import Foundation
import MapKit

struct AnnotationItem: Identifiable {
    let id = UUID()
    let coordinate: CLLocationCoordinate2D
}

struct NominatimResult: Decodable, Hashable {
    let displayName: String
    let lat: String
    let lon: String
    
    enum CodingKeys: String, CodingKey {
        case displayName = "display_name"
        case lat, lon
    }
}

@MainActor
final class LocationViewModel: ObservableObject {
    @Published var query: String = ""
    @Published var searchResults: [NominatimResult] = []
    @Published var showingSearchResults: Bool = false
    
    @Published var currentCoordinate: CLLocationCoordinate2D?
    @Published var annotationItems: [AnnotationItem] = []
    
    @Published var latText: String = ""
    @Published var lonText: String = ""
    
    @Published var isMocking: Bool = false
    
    @Published var showingShareSheet: Bool = false
    var exportedGPXURL: URL?
    
    private let lastLatKey = "last_lat"
    private let lastLonKey = "last_lon"
    
    var lastLocation: CLLocationCoordinate2D? {
        let lat = UserDefaults.standard.double(forKey: lastLatKey)
        let lon = UserDefaults.standard.double(forKey: lastLonKey)
        if lat == 0 && lon == 0 { return nil }
        return CLLocationCoordinate2D(latitude: lat, longitude: lon)
    }
    
    func setCoordinate(_ coord: CLLocationCoordinate2D) {
        currentCoordinate = coord
        annotationItems = [AnnotationItem(coordinate: coord)]
        latText = String(format: "%.6f", coord.latitude)
        lonText = String(format: "%.6f", coord.longitude)
        UserDefaults.standard.set(coord.latitude, forKey: lastLatKey)
        UserDefaults.standard.set(coord.longitude, forKey: lastLonKey)
    }
    
    func applyTextCoords() {
        guard let lat = Double(latText), let lon = Double(lonText) else { return }
        setCoordinate(CLLocationCoordinate2D(latitude: lat, longitude: lon))
    }
    
    func toggleMock() {
        isMocking.toggle()
        // 说明：iOS 不支持未越狱全局改定位；此处仅在 App 内模拟坐标
    }
    
    func search() async {
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !q.isEmpty else { return }
        do {
            let results = try await NominatimAPI.search(query: q)
            self.searchResults = results
            self.showingSearchResults = true
        } catch {
            print("Search error: \(error)")
        }
    }
    
    func selectSearchResult(_ r: NominatimResult) {
        showingSearchResults = false
        if let lat = Double(r.lat), let lon = Double(r.lon) {
            setCoordinate(CLLocationCoordinate2D(latitude: lat, longitude: lon))
        }
    }
    
    func exportGPX() {
        guard let coord = currentCoordinate else { return }
        do {
            let url = try GPXExporter.exportWaypoint(name: "MockPoint", coordinate: coord)
            exportedGPXURL = url
            showingShareSheet = true
        } catch {
            print("Export GPX error: \(error)")
        }
    }
}
