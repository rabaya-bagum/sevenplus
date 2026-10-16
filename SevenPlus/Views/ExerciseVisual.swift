import SwiftUI
import AVFoundation
import UIKit

/// Bundled exercise media, named by exercise id (rendered by tools/animation):
///   <id>.mp4 / <id>-dark.mp4              looping clip for light / dark mode
///   <id>-thumb.jpg / <id>-dark-thumb.jpg  still for lists
enum ExerciseMedia {
    static func clipURL(_ id: String, dark: Bool, bundle: Bundle = .main) -> URL? {
        if dark, let url = bundle.url(forResource: id + "-dark", withExtension: "mp4") { return url }
        return bundle.url(forResource: id, withExtension: "mp4")
    }

    static func thumbnail(_ id: String, dark: Bool, bundle: Bundle = .main) -> UIImage? {
        let name = dark ? id + "-dark-thumb" : id + "-thumb"
        let url = bundle.url(forResource: name, withExtension: "jpg") ?? bundle.url(forResource: id + "-thumb", withExtension: "jpg")
        return url.flatMap { UIImage(contentsOfFile: $0.path) }
    }
}

/// The 3D character performing an exercise. Falls back to the SF Symbol if no clip is bundled.
struct ExerciseVisual: View {
    let exercise: Exercise
    var playing = true
    var tint: Color = .accentColor

    @Environment(\.colorScheme) private var scheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        Group {
            if let url = ExerciseMedia.clipURL(exercise.id, dark: scheme == .dark) {
                LoopingVideo(url: url, playing: playing && !reduceMotion)
                    .id(url)                                   // new player when light/dark switches
                    .aspectRatio(1, contentMode: .fit)
                    .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            } else {
                Image(systemName: exercise.symbol)
                    .font(.system(size: 96))
                    .foregroundStyle(tint)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .accessibilityHidden(true)       // the exercise name and cue are read out instead
    }
}

/// Small still for lists (no video players in scrolling lists).
struct ExerciseThumbnail: View {
    let exercise: Exercise
    var size: CGFloat = 40
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        Group {
            if let image = ExerciseMedia.thumbnail(exercise.id, dark: scheme == .dark) {
                Image(uiImage: image).resizable().scaledToFill()
            } else {
                Image(systemName: exercise.symbol).font(.title2).foregroundStyle(.tint)
            }
        }
        .frame(width: size, height: size)
        .clipShape(RoundedRectangle(cornerRadius: size * 0.22, style: .continuous))
        .accessibilityHidden(true)
    }
}

/// Silent, seamlessly looping video. The clips have no audio track, so music keeps playing.
struct LoopingVideo: UIViewRepresentable {
    let url: URL
    let playing: Bool

    func makeUIView(context: Context) -> VideoSurface {
        let view = VideoSurface()
        view.load(url)
        view.setPlaying(playing)
        return view
    }

    func updateUIView(_ view: VideoSurface, context: Context) {
        view.setPlaying(playing)
    }

    static func dismantleUIView(_ view: VideoSurface, coordinator: ()) {
        view.tearDown()
    }

    final class VideoSurface: UIView {
        override class var layerClass: AnyClass { AVPlayerLayer.self }
        private var playerLayer: AVPlayerLayer { layer as! AVPlayerLayer }
        private var player: AVQueuePlayer?
        private var looper: AVPlayerLooper?

        func load(_ url: URL) {
            let player = AVQueuePlayer()
            player.isMuted = true
            player.preventsDisplaySleepDuringVideoPlayback = false
            looper = AVPlayerLooper(player: player, templateItem: AVPlayerItem(url: url))
            playerLayer.player = player
            playerLayer.videoGravity = .resizeAspect
            backgroundColor = .clear
            self.player = player
        }

        func setPlaying(_ on: Bool) {
            if on { player?.play() } else { player?.pause() }
        }

        func tearDown() {
            player?.pause()
            looper?.disableLooping()
            playerLayer.player = nil
            player = nil
            looper = nil
        }
    }
}
