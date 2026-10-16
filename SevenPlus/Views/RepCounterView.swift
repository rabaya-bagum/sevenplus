import SwiftUI
import AVFoundation

/// Small camera window with the live rep count.
struct RepCounterView: View {
    @ObservedObject var camera: PoseCamera

    var body: some View {
        HStack(spacing: 12) {
            CameraPreview(session: camera.session)
                .frame(width: 90, height: 120)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(camera.seesBody ? Color.green : Color.secondary, lineWidth: 2))
                .accessibilityHidden(true)
            VStack(alignment: .leading) {
                Text("\(camera.reps)").font(.system(size: 44, weight: .bold, design: .rounded)).monospacedDigit()
                Text(camera.permissionDenied ? "Camera access is off in Settings"
                     : camera.seesBody ? "reps" : "Step back so your whole body is in view")
                    .font(.caption).foregroundStyle(.secondary)
            }
            .accessibilityElement(children: .combine)
            .accessibilityLabel("\(camera.reps) reps counted")
        }
    }
}

struct CameraPreview: UIViewRepresentable {
    let session: AVCaptureSession

    final class PreviewView: UIView {
        override class var layerClass: AnyClass { AVCaptureVideoPreviewLayer.self }
        var previewLayer: AVCaptureVideoPreviewLayer { layer as! AVCaptureVideoPreviewLayer }
    }

    func makeUIView(context: Context) -> PreviewView {
        let v = PreviewView()
        v.previewLayer.session = session
        v.previewLayer.videoGravity = .resizeAspectFill
        return v
    }

    func updateUIView(_ uiView: PreviewView, context: Context) {}
}
