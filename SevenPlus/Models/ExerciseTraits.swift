import Foundation

/// Extra facts the workout builder needs, kept beside the library so the
/// exercise definitions stay readable.
struct ExerciseTraits {
    let area: BodyArea
    let stresses: Set<Limitation>   // which limitations this move is NOT safe for
    let swapOnly: Bool              // only used as a quiet-mode substitute

    static let byId: [String: ExerciseTraits] = [
        "jumping-jacks":    .init(area: .cardio, stresses: [.knees, .noJumping], swapOnly: false),
        "high-knees":       .init(area: .cardio, stresses: [.knees, .noJumping], swapOnly: false),
        "burpee":           .init(area: .cardio, stresses: [.knees, .wrists, .noJumping], swapOnly: false),
        "mountain-climber": .init(area: .cardio, stresses: [.wrists, .noJumping], swapOnly: false),
        "wall-sit":         .init(area: .lower, stresses: [.knees], swapOnly: false),
        "step-up":          .init(area: .lower, stresses: [.knees], swapOnly: false),
        "squat":            .init(area: .lower, stresses: [.knees], swapOnly: false),
        "lunge":            .init(area: .lower, stresses: [.knees], swapOnly: false),
        "glute-bridge":     .init(area: .lower, stresses: [], swapOnly: false),
        "calf-raise":       .init(area: .lower, stresses: [], swapOnly: false),
        "push-up":          .init(area: .upper, stresses: [.wrists], swapOnly: false),
        "triceps-dip":      .init(area: .upper, stresses: [.wrists], swapOnly: false),
        "push-up-rotation": .init(area: .upper, stresses: [.wrists], swapOnly: false),
        "crunch":           .init(area: .core, stresses: [.back], swapOnly: false),
        "plank":            .init(area: .core, stresses: [.wrists], swapOnly: false),
        "side-plank":       .init(area: .core, stresses: [.wrists], swapOnly: false),
        "bird-dog":         .init(area: .core, stresses: [.wrists], swapOnly: false),
        "superman":         .init(area: .core, stresses: [.back], swapOnly: false),
        "step-jacks":       .init(area: .cardio, stresses: [], swapOnly: true),
        "marching":         .init(area: .cardio, stresses: [], swapOnly: true),
        "step-back-burpee": .init(area: .cardio, stresses: [.wrists], swapOnly: true),
        "slow-climber":     .init(area: .cardio, stresses: [.wrists], swapOnly: true),
    ]

    static func of(_ id: String) -> ExerciseTraits { byId[id]! }
}
