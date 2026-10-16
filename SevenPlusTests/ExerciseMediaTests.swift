import XCTest
@testable import SevenPlus

/// Every exercise must ship with its 3D clip (light and dark) and list thumbnails.
/// Clips are rendered by tools/animation/render_all.py into SevenPlus/Resources/ExerciseClips.
final class ExerciseMediaTests: XCTestCase {
    func testEveryExerciseHasClipsAndThumbnails() {
        for ex in ExerciseLibrary.all {
            let light = ExerciseMedia.clipURL(ex.id, dark: false)
            let dark = ExerciseMedia.clipURL(ex.id, dark: true)
            XCTAssertNotNil(light, "\(ex.id).mp4 missing")
            XCTAssertNotEqual(dark, light, "\(ex.id)-dark.mp4 missing")
            XCTAssertNotNil(ExerciseMedia.thumbnail(ex.id, dark: false), "\(ex.id)-thumb.jpg missing")
            XCTAssertNotNil(ExerciseMedia.thumbnail(ex.id, dark: true), "\(ex.id)-dark-thumb.jpg missing")
        }
    }

    func testUnknownExerciseFallsBackToIcon() {
        XCTAssertNil(ExerciseMedia.clipURL("not-an-exercise", dark: false))
        XCTAssertNil(ExerciseMedia.thumbnail("not-an-exercise", dark: true))
    }
}
