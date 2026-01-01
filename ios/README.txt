NoMoreScamTW iOS 擴充功能
========================

此資料夾包含 Safari 網頁擴充功能的原始碼。

若要在 iOS 裝置 (iPhone/iPad) 上執行：

1.  **前置需求**：
    *   一台執行 macOS 的 Mac 電腦。
    *   已安裝 Xcode (可從 Mac App Store 免費下載)。
    *   一個 Apple ID (免費開發者帳號即可進行本機測試)。

2.  **轉換步驟**：
    *   在 Mac 上開啟終端機 (Terminal)。
    *   執行以下指令：
        `xcrun safari-web-extension-converter /path/to/NoMoreScamTW`
        (請將 `/path/to/NoMoreScamTW` 替換為此目錄中包含 `manifest.json` 的實際資料夾路徑)。
    *   Xcode 將會啟動並提示您建立新專案。
    *   選擇 "Swift" 作為開發語言。
    *   專案驗證完成後，您可以選擇模擬器或已連接的實體裝置來執行應用程式。

3.  **注意事項**：
    *   `manifest.json` 基於 Manifest V3，Safari on iOS 15+ 皆有支援。
    *   支援 `background.js` (Service Worker)。
    *   詐騙資料庫更新邏輯會在背景執行。請注意，與 Chrome 桌面版相比，iOS Safari 對背景腳本有更嚴格的資源限制。
