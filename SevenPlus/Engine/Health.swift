import Foundation
import HealthKit

/// Calorie estimate for bodyweight HIIT. Pure so it can be tested.
enum Calories {
    /// Compendium of Physical Activities: vigorous calisthenics ≈ 8 MET.
    static let met = 8.0
    static let defaultWeightKg = 70.0

    /// kcal = MET × body weight (kg) × hours of active work (rest time excluded).
    static func estimate(activeSeconds: Int, weightKg: Double?) -> Int {
        let kg = weightKg ?? defaultWeightKg
        return Int((met * kg * Double(activeSeconds) / 3600).rounded())
    }

    /// Seconds spent working (not resting or getting ready) in a session.
    static func activeSeconds(workout: Workout, elapsed: Int, getReady: Int = 5) -> Int {
        var t = max(0, elapsed - getReady), active = 0
        for step in workout.steps {
            let w = min(t, step.workSeconds)
            active += w
            t -= w
            guard t > 0 else { break }
            t = max(0, t - step.restSeconds)
        }
        return active
    }
}

/// Saves finished workouts to Apple Health when the user turns it on.
@MainActor
final class HealthManager: ObservableObject {
    @Published var enabled = UserDefaults.standard.bool(forKey: "healthEnabled") {
        didSet { UserDefaults.standard.set(enabled, forKey: "healthEnabled") }
    }
    @Published private(set) var lastError: String?
    /// Fetched when a workout starts so finishing never waits on Health.
    private(set) var cachedWeightKg: Double?

    func refreshWeight() async {
        guard enabled else { return }
        cachedWeightKg = await latestWeightKg()
    }

    private let store = HKHealthStore()
    var isAvailable: Bool { HKHealthStore.isHealthDataAvailable() }

    private var shareTypes: Set<HKSampleType> {
        [HKObjectType.workoutType(), HKQuantityType(.activeEnergyBurned)]
    }
    private var readTypes: Set<HKObjectType> { [HKQuantityType(.bodyMass)] }

    /// Turn on: ask permission first. Health never tells apps whether read access
    /// was granted, so we only check that the request itself went through.
    func setEnabled(_ on: Bool) async {
        guard on else { enabled = false; return }
        guard isAvailable else { lastError = "Apple Health isn't available on this device."; return }
        do {
            try await store.requestAuthorization(toShare: shareTypes, read: readTypes)
            enabled = store.authorizationStatus(for: HKObjectType.workoutType()) == .sharingAuthorized
            if !enabled { lastError = "Allow SevenPlus to write workouts in the Health app → Sharing." }
        } catch {
            lastError = error.localizedDescription
        }
    }

    /// Most recent body weight in kg, if the user shared it.
    func latestWeightKg() async -> Double? {
        guard isAvailable else { return nil }
        return await withCheckedContinuation { cont in
            let query = HKSampleQuery(sampleType: HKQuantityType(.bodyMass), predicate: nil, limit: 1,
                                      sortDescriptors: [NSSortDescriptor(key: HKSampleSortIdentifierEndDate, ascending: false)]) { _, samples, _ in
                let kg = (samples?.first as? HKQuantitySample)?.quantity.doubleValue(for: .gramUnit(with: .kilo))
                cont.resume(returning: kg)
            }
            store.execute(query)
        }
    }

    /// Writes one HIIT workout with its active calories.
    func save(start: Date, end: Date, kcal: Int) async {
        guard enabled, isAvailable, end > start else { return }
        let config = HKWorkoutConfiguration()
        config.activityType = .highIntensityIntervalTraining
        config.locationType = .indoor
        let builder = HKWorkoutBuilder(healthStore: store, configuration: config, device: .local())
        do {
            try await builder.beginCollection(at: start)
            if kcal > 0 {
                let sample = HKQuantitySample(type: HKQuantityType(.activeEnergyBurned),
                                              quantity: HKQuantity(unit: .kilocalorie(), doubleValue: Double(kcal)),
                                              start: start, end: end)
                try await builder.addSamples([sample])
            }
            try await builder.endCollection(at: end)
            _ = try await builder.finishWorkout()
        } catch {
            lastError = "Couldn't save to Health: \(error.localizedDescription)"
        }
    }
}
