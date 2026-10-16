import Foundation
import StoreKit

/// What the free tier includes. Pure rules so they are easy to test and change.
enum Gate {
    static let freeLengths: Set<Int> = [5, 7]
    static let freeCustomWorkouts = 3
    /// Offer the paywall once, right after this many completed workouts.
    static let upsellAfterCompleted = 3

    static func canUse(length: Int, premium: Bool) -> Bool { premium || freeLengths.contains(length) }

    static func canCreateCustom(existing: Int, premium: Bool) -> Bool { premium || existing < freeCustomWorkouts }

    static func shouldUpsell(completedCount: Int, premium: Bool, alreadyShown: Bool) -> Bool {
        !premium && !alreadyShown && completedCount == upsellAfterCompleted
    }
}

/// StoreKit 2 subscriptions: loads products, buys, restores, and keeps
/// `isPremium` in sync with the user's current entitlements.
@MainActor
final class PremiumStore: ObservableObject {
    static let productIDs = ["com.sevenplus.premium.monthly", "com.sevenplus.premium.yearly"]

    @Published private(set) var products: [Product] = []
    @Published private(set) var purchasedIDs: Set<String> = []
    @Published var lastError: String?
    #if DEBUG
    /// Lets you test premium screens without a purchase (Profile → Developer).
    @Published var debugUnlock = UserDefaults.standard.bool(forKey: "debugUnlock") {
        didSet { UserDefaults.standard.set(debugUnlock, forKey: "debugUnlock") }
    }
    #endif

    var isPremium: Bool {
        #if DEBUG
        if debugUnlock { return true }
        #endif
        return !purchasedIDs.isEmpty
    }

    var upsellShown: Bool {
        get { UserDefaults.standard.bool(forKey: "upsellShown") }
        set { UserDefaults.standard.set(newValue, forKey: "upsellShown") }
    }

    private var updates: Task<Void, Never>?

    init() {
        updates = Task { [weak self] in
            for await result in Transaction.updates {
                if case .verified(let t) = result { await t.finish() }
                await self?.refreshEntitlements()
            }
        }
        Task { await load() }
    }

    // MARK: - Upsell after the 3rd completed workout

    @Published var showPaywall = false
    private var upsellPending = false

    /// Call when a workout completes; the paywall waits until the player closes.
    func workoutCompleted(totalCompleted: Int) {
        if Gate.shouldUpsell(completedCount: totalCompleted, premium: isPremium, alreadyShown: upsellShown) {
            upsellPending = true
        }
    }

    /// Call when the player is dismissed.
    func presentPendingUpsell() {
        guard upsellPending else { return }
        upsellPending = false
        upsellShown = true
        showPaywall = true
    }

    func load() async {
        do {
            products = try await Product.products(for: Self.productIDs)
                .sorted { $0.price < $1.price }
        } catch {
            lastError = "Couldn't load prices. Check your connection."
        }
        await refreshEntitlements()
    }

    func refreshEntitlements() async {
        var ids: Set<String> = []
        for await result in Transaction.currentEntitlements {
            if case .verified(let t) = result, t.revocationDate == nil { ids.insert(t.productID) }
        }
        purchasedIDs = ids
    }

    /// Returns true when the purchase went through.
    @discardableResult
    func buy(_ product: Product) async -> Bool {
        do {
            switch try await product.purchase() {
            case .success(let verification):
                guard case .verified(let t) = verification else {
                    lastError = "The purchase couldn't be verified."
                    return false
                }
                await t.finish()
                await refreshEntitlements()
                return true
            case .pending, .userCancelled:
                return false
            @unknown default:
                return false
            }
        } catch {
            lastError = error.localizedDescription
            return false
        }
    }

    func restore() async {
        do { try await AppStore.sync() } catch { lastError = "Restore failed: \(error.localizedDescription)" }
        await refreshEntitlements()
    }
}
