import Foundation

enum NominatimAPI {
    static func search(query: String) async throws -> [NominatimResult] {
        let q = query.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? query
        let url = URL(string: "https://nominatim.openstreetmap.org/search?format=json&q=\(q)&limit=10&addressdetails=0")!
        var req = URLRequest(url: url)
        req.httpMethod = "GET"
        req.setValue("MockLocationApp/1.0 (contact: example@example.com)", forHTTPHeaderField: "User-Agent")
        req.setValue("application/json", forHTTPHeaderField: "Accept")
        let (data, resp) = try await URLSession.shared.data(for: req)
        guard let http = resp as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            throw NSError(domain: "Nominatim", code: 1, userInfo: [NSLocalizedDescriptionKey: "请求失败"])
        }
        let decoder = JSONDecoder()
        decoder.keyDecodingStrategy = .convertFromSnakeCase
        do {
            return try decoder.decode([NominatimResult].self, from: data)
        } catch {
            throw NSError(domain: "Nominatim", code: 2, userInfo: [NSLocalizedDescriptionKey: "解析失败: \(error.localizedDescription)"])
        }
    }
}
