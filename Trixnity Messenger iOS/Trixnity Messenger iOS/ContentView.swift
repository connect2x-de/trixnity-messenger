import SwiftUI
import TrixnityMessengerUI

struct ContentView: View {
    var body: some View {
        ComposeView().ignoresSafeArea(.all)
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
