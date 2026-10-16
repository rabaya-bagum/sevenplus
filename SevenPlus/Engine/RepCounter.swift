import Foundation
import CoreGraphics

/// Counts reps from a joint angle with hysteresis: a rep is going below `down`
/// and coming back above `up`. Pure, so it is easy to test and tune.
struct RepCounter: Equatable {
    let down: Double
    let up: Double
    private(set) var count = 0
    private(set) var isDown = false
    /// Smooths jitter from the pose detector.
    private var smoothed: Double?
    private let smoothing = 0.5

    init(down: Double, up: Double) {
        precondition(down < up)
        self.down = down
        self.up = up
    }

    /// Feed one angle in degrees. Returns true when a rep completes.
    @discardableResult
    mutating func update(angle raw: Double) -> Bool {
        let angle = smoothed.map { $0 + smoothing * (raw - $0) } ?? raw
        smoothed = angle
        if !isDown && angle < down {
            isDown = true
        } else if isDown && angle > up {
            isDown = false
            count += 1
            return true
        }
        return false
    }

    mutating func reset() {
        count = 0
        isDown = false
        smoothed = nil
    }
}

/// Which joints to measure for each countable exercise.
enum RepProfile {
    enum Joint { case knee, elbow }

    struct Spec { let joint: Joint; let down: Double; let up: Double }

    static let byExercise: [String: Spec] = [
        "squat":       Spec(joint: .knee, down: 100, up: 160),
        "lunge":       Spec(joint: .knee, down: 110, up: 160),
        "push-up":     Spec(joint: .elbow, down: 95, up: 155),
        "triceps-dip": Spec(joint: .elbow, down: 100, up: 155),
        "calf-raise":  Spec(joint: .knee, down: 150, up: 172),   // rough; mostly a demo
    ]

    static func supports(_ exerciseId: String) -> Bool { byExercise[exerciseId] != nil }

    /// Angle at `b` formed by points a–b–c, in degrees (0…180).
    static func angle(_ a: CGPoint, _ b: CGPoint, _ c: CGPoint) -> Double {
        let v1 = CGVector(dx: a.x - b.x, dy: a.y - b.y)
        let v2 = CGVector(dx: c.x - b.x, dy: c.y - b.y)
        let dot = v1.dx * v2.dx + v1.dy * v2.dy
        let mags = hypot(v1.dx, v1.dy) * hypot(v2.dx, v2.dy)
        guard mags > 0 else { return 180 }
        return acos(max(-1, min(1, dot / mags))) * 180 / .pi
    }
}
