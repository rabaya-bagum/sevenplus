import AVFoundation

/// Voice coach: announces each stage. Ducks other audio (music keeps playing).
final class Coach {
    private let synth = AVSpeechSynthesizer()
    var enabled = true

    init() {
        try? AVAudioSession.sharedInstance().setCategory(.playback, options: [.duckOthers, .mixWithOthers])
    }

    func announce(_ phase: Phase, workout: Workout) {
        guard enabled else { return }
        switch phase {
        case .getReady:
            let first = ExerciseLibrary.exercise(workout.steps[0].exerciseId)
            say("Get ready. First up, \(first.name).")
        case .work(let i, _):
            let ex = ExerciseLibrary.exercise(workout.steps[i].exerciseId)
            say("\(ex.name). \(ex.cue)")
        case .rest(let i, _):
            let next = ExerciseLibrary.exercise(workout.steps[i + 1].exerciseId)
            say("Rest. Next, \(next.name).")
        case .finished:
            say("Workout complete. Great job!")
        }
    }

    private func say(_ text: String) {
        synth.stopSpeaking(at: .immediate)
        let u = AVSpeechUtterance(string: text)
        u.rate = AVSpeechUtteranceDefaultSpeechRate
        synth.speak(u)
    }
}
