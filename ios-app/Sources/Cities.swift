import Foundation
import CoreLocation

struct City: Identifiable {
    let id = UUID()
    let name: String
    let coord: CLLocationCoordinate2D
}

enum Cities {
    static let presets: [City] = [
        City(name: "北京", coord: .init(latitude: 39.9042, longitude: 116.4074)),
        City(name: "上海", coord: .init(latitude: 31.2304, longitude: 121.4737)),
        City(name: "广州", coord: .init(latitude: 23.1291, longitude: 113.2644)),
        City(name: "深圳", coord: .init(latitude: 22.5431, longitude: 114.0579)),
    ]
}
