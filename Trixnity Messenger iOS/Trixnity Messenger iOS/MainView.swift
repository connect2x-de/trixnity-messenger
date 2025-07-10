import Foundation
import SwiftUI
import TrixnityMessengerUI

struct MainView: UIViewControllerRepresentable {
    let lifecycle: Lifecycle

    func makeUIViewController(context: Context) -> UIViewController {
        return MainControllerKt.MainViewController(lifecycle: lifecycle)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}
