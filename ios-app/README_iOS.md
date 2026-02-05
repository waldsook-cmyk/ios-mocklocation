# MockLocation iOS（方案A / 免费 Apple ID）

本目录包含一个可编译的 iOS SwiftUI 项目（无需服务器）：
- 地图显示（MapKit）
- 地名搜索（OpenStreetMap Nominatim）
- 快捷城市（北京/上海/广州/深圳）
- 经纬度手动输入
- 位置持久化（自动记住上次）
- 导出 GPX 并分享（可用于 Xcode 的模拟定位）

重要声明：iOS 在未越狱状态下无法全局修改系统定位，App 内“虚拟定位”仅在本应用中生效。若需让其它 App 看到模拟位置，需在 macOS + Xcode 调试会话中使用 GPX（仅调试期间有效）。

---

## 目录结构
```
ios-app/
├─ Sources/
│  ├─ MockLocationAppApp.swift
│  ├─ ContentView.swift
│  ├─ LocationViewModel.swift
│  ├─ NominatimAPI.swift
│  ├─ GPXExporter.swift
│  └─ Cities.swift
├─ Resources/
│  ├─ Info.plist
│  └─ Sample-Beijing.gpx
└─ project.yml  # XcodeGen 工程描述
```

---

## 从 Windows 构建 IPA（使用 GitHub Actions，免费）
你无需 macOS，本项目已内置 GitHub Actions 工作流，会在云端 macOS 上编译并生成 **未签名 IPA**（可用 AltStore/Sideloadly 进行签名安装）。

步骤：
1. 在 GitHub 创建一个新的私有仓库（建议私有）。
2. 将 `mock-location-ios` 整个文件夹推送到仓库（至少包含 `ios-app/` 和 `.github/workflows/ios-unsigned-ipa.yml`）。
3. 打开 GitHub → 仓库 → Actions → 选择工作流【iOS unsigned IPA build】→ Run workflow。
4. 等待 3-6 分钟，工作流完成后在 Artifacts 下载 `MockLocationApp-unsigned.ipa`。

提示：
- 若不熟悉 Git，可用 GitHub Desktop 直接把文件夹拖进去创建仓库并推送。
- 工作流脚本见 `.github/workflows/ios-unsigned-ipa.yml`。

---

## 在 iPhone 13 上安装（免费 Apple ID）
推荐两种工具（二选一）：

### 方案一：AltStore（推荐）
1. 在 Windows 安装 iTunes 与 iCloud（请从 Apple 官网安装版本）。
2. 安装 AltServer（https://altstore.io/），运行并用你的 Apple ID 登录。
3. 用数据线连接 iPhone → AltServer 左下角图标 → Install AltStore → 选择你的 iPhone。
4. iPhone：设置 → 通用 → VPN与设备管理 → 信任你的 Apple ID 开发者。
5. 打开 iPhone 上的 AltStore → My Apps → 左上角“＋”→ 选择刚下载的 `MockLocationApp-unsigned.ipa`。
6. AltStore 会用你的 Apple ID 进行签名并安装（有效期 7 天，到期在 AltStore 内一键续签）。

### 方案二：Sideloadly
1. 安装 Sideloadly（https://sideloadly.io/）。
2. 连接 iPhone，选择 `MockLocationApp-unsigned.ipa`，输入 Apple ID，按提示完成安装。
3. 首次安装后在 iPhone 设置中“信任开发者”。

---

## App 使用说明
1. 打开 App，允许定位权限（用于地图显示）。
2. 方式一：输入经纬度 → 点“定位”。
3. 方式二：点击快捷城市（北京/上海/广州/深圳）。
4. 方式三：输入地名 → 点“搜索” → 在结果中点选。
5. 点“导出GPX”→ 通过“文件”、“AirDrop”或其他方式分享 GPX。
   - 若你在 macOS + Xcode 调试：把 GPX 加到 Xcode Scheme 的位置模拟中，运行到设备后即可在调试期间让其它 App 看到该位置。

---

## Nominatim（地名搜索）注意
- Nominatim 要求提供可联系的 User-Agent。你可以修改：`Sources/NominatimAPI.swift` 中的
  ```swift
  req.setValue("MockLocationApp/1.0 (contact: example@example.com)", forHTTPHeaderField: "User-Agent")
  ```
  将 `example@example.com` 换成你的邮箱或网站，避免被限流。

---

## 常见问题
- Q：为什么这个 App 不能让所有 App 都看到我设置的位置？
  - A：iOS 安全机制所限，未越狱无法全局修改定位。可在 Xcode 调试期间通过 GPX 临时影响系统定位。
- Q：免费 Apple ID 为什么 7 天就过期？
  - A：苹果策略所限。你可在 AltStore 里一键续签，或购买付费开发者账号延长有效期和更便捷分发。
- Q：安装时提示无法验证？
  - A：请在 iPhone 设置 → 通用 → VPN与设备管理 → 信任对应的开发者证书，并保持手机与装包电脑在同一局域网（AltServer 续签需要）。

---

## 下一步（可选增强）
- 离线城市列表、收藏管理
- 自定义 GPX 轨迹导出（非单点）
- 地图样式切换（标准/卫星/混合）

如需我替你把仓库推送/配置 Actions，请把仓库地址告诉我（或我提供一个最简推送命令行）。
