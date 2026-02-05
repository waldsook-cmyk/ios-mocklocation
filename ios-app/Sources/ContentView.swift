import SwiftUI
import MapKit

struct ContentView: View {
    @EnvironmentObject var vm: LocationViewModel
    @State private var region = MKCoordinateRegion(
        center: CLLocationCoordinate2D(latitude: 39.9042, longitude: 116.4074),
        span: MKCoordinateSpan(latitudeDelta: 0.05, longitudeDelta: 0.05)
    )
    @State private var searchText: String = ""
    @State private var showingShare = false
    
    var body: some View {
        VStack(spacing: 0) {
            header
            map
            controls
        }
        .onAppear {
            if let last = vm.lastLocation {
                region.center = last
            }
        }
        .sheet(isPresented: $vm.showingSearchResults) {
            SearchResultsView()
                .environmentObject(vm)
        }
        .sheet(isPresented: $vm.showingShareSheet) {
            if let fileURL = vm.exportedGPXURL {
                ShareSheet(activityItems: [fileURL])
            }
        }
    }
    
    private var header: some View {
        VStack(spacing: 8) {
            Text("虚拟定位（App内）")
                .font(.title2).bold()
            HStack {
                TextField("输入地名（支持搜索）", text: $vm.query)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                Button("搜索") {
                    Task { await vm.search() }
                }
                .buttonStyle(.borderedProminent)
            }
            .padding(.horizontal)
        }
        .padding(.vertical, 12)
        .background(Color(UIColor.secondarySystemBackground))
    }
    
    private var map: some View {
        Map(coordinateRegion: $region, annotationItems: vm.annotationItems) { item in
            MapMarker(coordinate: item.coordinate, tint: .red)
        }
        .onChange(of: vm.currentCoordinate) { value in
            if let c = value { region.center = c }
        }
    }
    
    private var controls: some View {
        VStack(spacing: 8) {
            HStack {
                Text("经度:")
                TextField("lon", text: $vm.lonText)
                    .keyboardType(.decimalPad)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                Text("纬度:")
                TextField("lat", text: $vm.latText)
                    .keyboardType(.decimalPad)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                Button("定位") { vm.applyTextCoords() }
                    .buttonStyle(.bordered)
            }
            .padding(.horizontal)
            
            ScrollView(.horizontal, showsIndicators: false) {
                HStack {
                    ForEach(Cities.presets) { city in
                        Button(city.name) {
                            vm.setCoordinate(city.coord)
                        }
                        .buttonStyle(.bordered)
                    }
                }
                .padding(.horizontal)
            }
            
            HStack {
                Button(vm.isMocking ? "停止模拟" : "启动模拟") {
                    vm.toggleMock()
                }
                .buttonStyle(.borderedProminent)
                
                Button("导出GPX") {
                    vm.exportGPX()
                }
                .buttonStyle(.bordered)
            }
            .padding(.bottom, 12)
        }
        .background(Color(UIColor.systemBackground))
    }
}

struct SearchResultsView: View {
    @EnvironmentObject var vm: LocationViewModel
    var body: some View {
        NavigationView {
            List(vm.searchResults, id: \.self) { r in
                VStack(alignment: .leading) {
                    Text(r.displayName)
                    Text("lat: \(r.lat), lon: \(r.lon)")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                .contentShape(Rectangle())
                .onTapGesture {
                    vm.selectSearchResult(r)
                }
            }
            .navigationTitle("搜索结果")
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("关闭") { vm.showingSearchResults = false } } }
        }
    }
}

struct ShareSheet: UIViewControllerRepresentable {
    let activityItems: [Any]
    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: activityItems, applicationActivities: nil)
    }
    func updateUIViewController(_ vc: UIActivityViewController, context: Context) {}
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View { ContentView().environmentObject(LocationViewModel()) }
}
