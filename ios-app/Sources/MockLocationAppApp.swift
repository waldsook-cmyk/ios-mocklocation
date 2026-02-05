import SwiftUI

@main
struct MockLocationAppApp: App {
    @StateObject private var vm = LocationViewModel()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(vm)
        }
    }
}
