import Foundation
import CoreLocation

enum GPXExporter {
    static func exportWaypoint(name: String, coordinate: CLLocationCoordinate2D) throws -> URL {
        let gpx = """
        <?xml version=\"1.0\" encoding=\"UTF-8\"?>
        <gpx version=\"1.1\" creator=\"MockLocationApp\" xmlns=\"http://www.topografix.com/GPX/1/1\">
          <wpt lat=\"\(coordinate.latitude)\" lon=\"\(coordinate.longitude)\">
            <name>\(name)</name>
          </wpt>
        </gpx>
        """.trimmingCharacters(in: .whitespacesAndNewlines)
        
        let dir = FileManager.default.temporaryDirectory
        let url = dir.appendingPathComponent("mock_location_\(Int(Date().timeIntervalSince1970)).gpx")
        try gpx.data(using: .utf8)?.write(to: url)
        return url
    }
}
