import Foundation
import SwiftUI
import TrixnityMessengerUI

struct MainView: UIViewControllerRepresentable {
    let lifecycle: LifecycleRegistry

    func makeUIViewController(context: Context) -> UIViewController {
        return MainControllerKt.MainViewController(lifecycle: lifecycle).second!
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}
