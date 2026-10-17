package com.sevenplus.app.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.sevenplus.app.BuildConfig
import com.sevenplus.app.contracts.PremiumService
import com.sevenplus.app.contracts.ProductInfo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Period

/**
 * Google Play subscriptions (iOS PremiumStore). Never throws when Play is missing; failures
 * land in [lastError]. [debugPremium] (e.g. settings.map { it.debugPremium }) is honored only
 * in debug builds.
 */
class PlayBillingPremiumService(
    context: Context,
    private val scope: CoroutineScope,
    debugPremium: StateFlow<Boolean>? = null,
) : PremiumService {
    private val entitled = MutableStateFlow(false)
    override val isPremium: StateFlow<Boolean> =
        combine(entitled, if (BuildConfig.DEBUG && debugPremium != null) debugPremium else flowOf(false)) { a, b -> a || b }
            .stateIn(scope, SharingStarted.Eagerly, false)
    private val _products = MutableStateFlow<List<ProductInfo>>(emptyList())
    override val products: StateFlow<List<ProductInfo>> = _products
    private val _lastError = MutableStateFlow<String?>(null)
    override val lastError: StateFlow<String?> = _lastError

    private var details: Map<String, ProductDetails> = emptyMap()
    private var pendingPurchase: CompletableDeferred<Boolean>? = null
    private val connectMutex = Mutex()

    private val listener = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> scope.launch {
                handle(purchases.orEmpty())
                pendingPurchase?.complete(purchases.orEmpty().any { it.purchaseState == Purchase.PurchaseState.PURCHASED })
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> pendingPurchase?.complete(false)
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> scope.launch {
                refreshPurchases(); pendingPurchase?.complete(entitled.value)
            }
            else -> {
                _lastError.value = result.debugMessage.ifEmpty { "The purchase didn't go through." }
                pendingPurchase?.complete(false)
            }
        }
    }

    private val client: BillingClient? = try {
        BillingClient.newBuilder(context.applicationContext)
            .setListener(listener)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .enableAutoServiceReconnection()
            .build()
    } catch (_: Throwable) { null }

    override fun start() {
        scope.launch { if (connect()) { loadProducts(); refreshPurchases() } }
    }

    /** Connects with exponential backoff (1s, 2s, 4s); false when Play stays unavailable. */
    private suspend fun connect(): Boolean = connectMutex.withLock {
        val c = client ?: return@withLock false.also { _lastError.value = "Google Play isn't available." }
        if (c.isReady) return@withLock true
        var wait = 1000L
        repeat(4) { attempt ->
            val result = CompletableDeferred<BillingResult>()
            try {
                c.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(r: BillingResult) { result.complete(r) }
                    override fun onBillingServiceDisconnected() {
                        result.complete(BillingResult.newBuilder()
                            .setResponseCode(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED).build())
                    }
                })
            } catch (_: Throwable) {
                result.complete(BillingResult.newBuilder()
                    .setResponseCode(BillingClient.BillingResponseCode.BILLING_UNAVAILABLE).build())
            }
            val r = result.await()
            if (r.responseCode == BillingClient.BillingResponseCode.OK) { _lastError.value = null; return@withLock true }
            if (r.responseCode == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE && attempt > 0) {
                _lastError.value = "Google Play billing isn't available on this device."
                return@withLock false
            }
            if (attempt < 3) { delay(wait); wait *= 2 }
        }
        _lastError.value = "Couldn't connect to Google Play."
        false
    }

    private suspend fun loadProducts() {
        val c = client ?: return
        val params = QueryProductDetailsParams.newBuilder().setProductList(PremiumService.PRODUCT_IDS.map {
            QueryProductDetailsParams.Product.newBuilder().setProductId(it).setProductType(BillingClient.ProductType.SUBS).build()
        }).build()
        val res = try { c.queryProductDetails(params) } catch (_: Throwable) { null }
        val list = res?.productDetailsList.orEmpty()
        if (res == null || res.billingResult.responseCode != BillingClient.BillingResponseCode.OK || list.isEmpty()) {
            _lastError.value = "Couldn't load prices. Check your connection."
            return
        }
        details = list.associateBy { it.productId }
        _products.value = PremiumService.PRODUCT_IDS.mapNotNull { id -> details[id]?.let(::toInfo) }
    }

    private fun toInfo(d: ProductDetails): ProductInfo? {
        val offers = d.subscriptionOfferDetails ?: return null
        val base = offers.firstOrNull { it.offerId == null } ?: offers.first()
        val basePhase = base.pricingPhases.pricingPhaseList.lastOrNull() ?: return null
        val trial = offers.asSequence().flatMap { it.pricingPhases.pricingPhaseList }
            .firstOrNull { it.priceAmountMicros == 0L }?.billingPeriod?.let(::days)
        return ProductInfo(
            id = d.productId,
            title = d.name.ifEmpty { d.title },
            price = basePhase.formattedPrice,
            period = if (basePhase.billingPeriod.contains("Y")) "year" else "month",
            freeTrialDays = trial,
        )
    }

    private fun days(iso: String): Int? = try { Period.parse(iso).let { it.years * 365 + it.months * 30 + it.days } } catch (_: Exception) { null }

    override suspend fun purchase(activity: Activity, productId: String): Boolean {
        val c = client ?: return false.also { _lastError.value = "Google Play isn't available." }
        if (!connect()) return false
        if (details.isEmpty()) loadProducts()
        val d = details[productId] ?: return false.also { _lastError.value = "Couldn't load prices. Check your connection." }
        val offer = d.subscriptionOfferDetails?.let { o -> o.firstOrNull { it.offerId != null } ?: o.firstOrNull() }
            ?: return false
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).setOfferToken(offer.offerToken).build()
        )).build()
        val deferred = CompletableDeferred<Boolean>()
        pendingPurchase = deferred
        val r = try { c.launchBillingFlow(activity, params) } catch (_: Throwable) { null }
        if (r?.responseCode != BillingClient.BillingResponseCode.OK) {
            pendingPurchase = null
            _lastError.value = r?.debugMessage?.ifEmpty { null } ?: "The purchase didn't go through."
            return false
        }
        return deferred.await().also { pendingPurchase = null }
    }

    override suspend fun restore() {
        if (!connect()) { _lastError.value = "Restore failed: Google Play isn't available."; return }
        refreshPurchases()
    }

    private suspend fun refreshPurchases() {
        val c = client ?: return
        val res = try {
            c.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build())
        } catch (_: Throwable) { return }
        if (res.billingResult.responseCode != BillingClient.BillingResponseCode.OK) return
        entitled.value = false
        handle(res.purchasesList)
    }

    private suspend fun handle(purchases: List<Purchase>) {
        val c = client ?: return
        var any = false
        for (p in purchases) {
            if (p.purchaseState != Purchase.PurchaseState.PURCHASED) continue
            if (p.products.none { it in PremiumService.PRODUCT_IDS }) continue
            any = true
            if (!p.isAcknowledged) {
                try {
                    c.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build())
                } catch (_: Throwable) { _lastError.value = "The purchase couldn't be verified." }
            }
        }
        if (any) entitled.value = true
    }
}
