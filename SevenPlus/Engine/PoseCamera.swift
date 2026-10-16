import AVFoundation
import Vision
import UIKit

/// Front camera + Vision body pose → joint angle → RepCounter.
/// Frames are processed on-device and never stored or sent anywhere.
final class PoseCamera: NSObject, ObservableObject {
    @Published private(set) var reps = 0
    @Published private(set) var seesBody = false
    @Published private(set) var permissionDenied = false

    let session = AVCaptureSession()
    private let queue = DispatchQueue(label: "sevenplus.pose")
    private var counter: RepCounter?
    private var spec: RepProfile.Spec?
    private var configured = false

    /// Start counting for an exercise (camera starts if needed).
    func start(exerciseId: String) {
        guard let s = RepProfile.byExercise[exerciseId] else { return }
        queue.async {
            self.spec = s
            self.counter = RepCounter(down: s.down, up: s.up)
        }
        DispatchQueue.main.async { self.reps = 0 }
        AVCaptureDevice.requestAccess(for: .video) { granted in
            DispatchQueue.main.async { self.permissionDenied = !granted }
            guard granted else { return }
            self.queue.async {
                self.configureIfNeeded()
                if !self.session.isRunning { self.session.startRunning() }
            }
        }
    }

    func stop() {
        queue.async {
            if self.session.isRunning { self.session.stopRunning() }
            self.counter = nil
        }
        DispatchQueue.main.async { self.seesBody = false }
    }

    private func configureIfNeeded() {
        guard !configured else { return }
        session.beginConfiguration()
        session.sessionPreset = .vga640x480
        if let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .front),
           let input = try? AVCaptureDeviceInput(device: device), session.canAddInput(input) {
            session.addInput(input)
        }
        let output = AVCaptureVideoDataOutput()
        output.alwaysDiscardsLateVideoFrames = true
        output.setSampleBufferDelegate(self, queue: queue)
        if session.canAddOutput(output) { session.addOutput(output) }
        session.commitConfiguration()
        configured = true
    }

    /// Picks the side of the body the camera sees best.
    private func jointAngle(_ obs: VNHumanBodyPoseObservation, joint: RepProfile.Joint) -> Double? {
        typealias J = VNHumanBodyPoseObservation.JointName
        let sides: [(J, J, J)] = joint == .knee
            ? [(.leftHip, .leftKnee, .leftAnkle), (.rightHip, .rightKnee, .rightAnkle)]
            : [(.leftShoulder, .leftElbow, .leftWrist), (.rightShoulder, .rightElbow, .rightWrist)]
        var best: (confidence: Float, angle: Double)?
        for (a, b, c) in sides {
            guard let pa = try? obs.recognizedPoint(a), let pb = try? obs.recognizedPoint(b),
                  let pc = try? obs.recognizedPoint(c) else { continue }
            let conf = min(pa.confidence, pb.confidence, pc.confidence)
            guard conf > 0.3 else { continue }
            let angle = RepProfile.angle(pa.location, pb.location, pc.location)
            if best == nil || conf > best!.confidence { best = (conf, angle) }
        }
        return best?.angle
    }
}

extension PoseCamera: AVCaptureVideoDataOutputSampleBufferDelegate {
    func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        guard let spec, counter != nil, let pixels = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }
        let request = VNDetectHumanBodyPoseRequest()
        try? VNImageRequestHandler(cvPixelBuffer: pixels, orientation: .leftMirrored).perform([request])
        guard let obs = request.results?.first, let angle = jointAngle(obs, joint: spec.joint) else {
            DispatchQueue.main.async { if self.seesBody { self.seesBody = false } }
            return
        }
        let didRep = counter!.update(angle: angle)
        let count = counter!.count
        DispatchQueue.main.async {
            if !self.seesBody { self.seesBody = true }
            if didRep { self.reps = count }
        }
    }
}
