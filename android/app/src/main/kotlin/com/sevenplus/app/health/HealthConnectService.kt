package com.sevenplus.app.health

import android.content.Context
import android.util.Log
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import com.sevenplus.app.contracts.HealthAvailability
import com.sevenplus.app.contracts.HealthService
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.reflect.KClass

/** Health Connect (iOS HealthManager). Every call is safe when Health Connect is missing or access was revoked. */
class HealthConnectService(context: Context) : HealthService {
    private val context = context.applicationContext

    override val availability: HealthAvailability
        get() = when (runCatching { HealthConnectClient.getSdkStatus(context) }.getOrDefault(HealthConnectClient.SDK_UNAVAILABLE)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthAvailability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthAvailability.NOT_INSTALLED
            else -> HealthAvailability.NOT_SUPPORTED
        }

    override val permissions: Set<String> = setOf(
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
    )

    override fun permissionContract(): ActivityResultContract<Set<String>, Set<String>> =
        PermissionController.createRequestPermissionResultContract()

    private fun client(): HealthConnectClient? =
        if (availability == HealthAvailability.AVAILABLE) runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull()
        else null

    override suspend fun hasPermissions(): Boolean = try {
        client()?.permissionController?.getGrantedPermissions()?.containsAll(permissions) ?: false
    } catch (e: Exception) {
        Log.w(TAG, "getGrantedPermissions failed", e); false
    }

    override suspend fun saveWorkout(startMillis: Long, endMillis: Long, kcal: Int) {
        val client = client() ?: return
        try {
            val start = Instant.ofEpochMilli(startMillis)
            val end = Instant.ofEpochMilli(maxOf(endMillis, startMillis + 1000))
            val rules = ZoneId.systemDefault().rules
            val startOffset = rules.getOffset(start)
            val endOffset = rules.getOffset(end)
            fun metadata() = Metadata.activelyRecorded(Device(type = Device.TYPE_PHONE))
            val records = mutableListOf<Record>(
                ExerciseSessionRecord(
                    startTime = start, startZoneOffset = startOffset,
                    endTime = end, endZoneOffset = endOffset,
                    metadata = metadata(),
                    exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING,
                    title = "SevenPlus",
                ),
            )
            if (kcal > 0) records += ActiveCaloriesBurnedRecord(
                startTime = start, startZoneOffset = startOffset,
                endTime = end, endZoneOffset = endOffset,
                energy = Energy.kilocalories(kcal.toDouble()),
                metadata = metadata(),
            )
            client.insertRecords(records)
        } catch (e: Exception) {
            Log.w(TAG, "saveWorkout failed", e)
        }
    }

    override suspend fun latestWeightKg(): Double? {
        val client = client() ?: return null
        return try {
            val now = Instant.now()
            client.readRecords(
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(now.minus(Duration.ofDays(365)), now),
                    ascendingOrder = false,
                    pageSize = 1,
                )
            ).records.maxByOrNull { it.time }?.weight?.inKilograms
        } catch (e: Exception) {
            Log.w(TAG, "latestWeightKg failed", e); null
        }
    }

    override suspend fun deleteSavedData(): Boolean {
        val client = client() ?: return false
        return try {
            deleteOwn(client, ExerciseSessionRecord::class)
            deleteOwn(client, ActiveCaloriesBurnedRecord::class)
            true
        } catch (e: Exception) {
            Log.w(TAG, "deleteSavedData failed", e); false
        }
    }

    private suspend fun <T : Record> deleteOwn(client: HealthConnectClient, type: KClass<T>) {
        val ids = mutableListOf<String>()
        var token: String? = null
        do {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = type,
                    timeRangeFilter = TimeRangeFilter.before(Instant.now().plus(Duration.ofDays(1))),
                    dataOriginFilter = setOf(DataOrigin(context.packageName)),
                    pageSize = 1000,
                    pageToken = token,
                )
            )
            ids += response.records.map { it.metadata.id }
            token = response.pageToken
        } while (token != null)
        ids.chunked(1000).forEach { client.deleteRecords(type, it, emptyList()) }
    }

    private companion object { const val TAG = "HealthConnectService" }
}
