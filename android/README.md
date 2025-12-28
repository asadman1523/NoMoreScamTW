# NoMoreScamTW - 台灣詐騙網站偵測 (Android)

**NoMoreScamTW** 是一個 Android 應用程式，移植自同名的 Chrome 擴充功能。它旨在保護使用者免受詐騙網站的侵害。透過整合政府開放資料平台，當您在手機瀏覽器中誤入可疑的詐騙網站時，App 會立即彈出醒目的紅色警示橫幅，協助您保護個人資料與財產安全。

## ✨ 主要功能

*   **全域即時防護**：利用 `AccessibilityService` 監測瀏覽器網址列，當偵測到詐騙網址時，直接在瀏覽器上方顯示紅色懸浮警示 (Overlay)。
*   **每日自動更新**：透過 `WorkManager` 每 24 小時自動從內政部警政署下載最新的詐騙網站清單。
*   **離線比對**：所有網址比對皆在**本機端 (Local)** 進行，您的瀏覽紀錄**絕不會**被上傳至任何伺服器，確保隱私安全。
*   **手動查詢**：可在 App 內輸入網址查詢是否在黑名單中。
*   **快速分享檢查**：支援 Android 系統分享功能，可直接將網址從瀏覽器「分享」給 NoMoreScamTW 進行快速檢查。
*   **流暢的使用者體驗**：包含完整的權限引導流程、WebP 動畫教學、以及左右滑動的頁面切換效果。

## 🛠️ 技術架構 (Tech Stack)

本專案採用 **Kotlin** 編寫，並遵循現代 Android 開發標準：

*   **語言**: Kotlin
*   **架構模式**: MVVM (Repository Pattern)
*   **本地資料庫**: [Room Database](https://developer.android.com/training/data-storage/room) - 用於高效儲存與查詢數萬筆詐騙網址資料。
*   **網路請求**: [Retrofit](https://square.github.io/retrofit/) - 用於下載政府開放資料 (CSV)。
*   **背景任務**: [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) - 處理每日背景自動更新。
*   **核心服務**:
    *   **AccessibilityService**: 用於取得當前前台應用程式 (瀏覽器) 的網址內容。
    *   **System Alert Window**: 用於顯示最上層的紅色警示橫幅。
*   **非同步處理**: Coroutines & Flow.

## 📂 專案結構

```
com.jackwu.nomorescamtw
├── data/               # 資料層
│   ├── AppDatabase.kt  # Room 資料庫設定
│   ├── FraudDao.kt     # 資料庫存取介面 (DAO)
│   └── FraudSite.kt    # 資料實體 (Entity)
├── network/            # 網路層
│   └── FraudApiService.kt # Retrofit API 定義
├── repository/         # 倉庫層
│   └── FraudRepository.kt # 核心邏輯：下載 CSV、解析資料、網址比對演算法
├── service/            # 服務
│   └── ScamDetectionService.kt # AccessibilityService 實作，負責監控瀏覽器與顯示 Overlay
├── worker/             # 背景工作
│   └── UpdateDatabaseWorker.kt # 每日自動更新的 Worker
└── [Activities]        # UI 層
    ├── MainActivity.kt                # 主畫面 (狀態顯示、手動檢查)
    ├── DisclaimerActivity.kt          # 免責聲明 (入口點)
    ├── PermissionOverlayActivity.kt   # 懸浮視窗權限引導
    └── PermissionAccessibilityActivity.kt # 無障礙服務權限引導
```

## 🔒 關於權限

本應用程式需要以下敏感權限才能正常運作，我們承諾僅用於詐騙偵測用途：

1.  **顯示在其他應用程式上層 (SYSTEM_ALERT_WINDOW)**：
    *   **用途**：當偵測到危險時，強制在瀏覽器畫面上方顯示紅色警示橫幅。
2.  **無障礙服務 (BIND_ACCESSIBILITY_SERVICE)**：
    *   **用途**：讀取特定瀏覽器 (Chrome, Edge, Firefox 等) 的網址列內容，以便與本地詐騙資料庫進行比對。
    *   **隱私**：我們**只會讀取網址**，不會讀取網頁內容或輸入的密碼，且比對過程完全在手機內部完成。

## 🚀 如何開始

1.  Clone 此專案到本地。
2.  使用 Android Studio 開啟專案。
3.  等待 Gradle Sync 完成。
4.  連接 Android 裝置或模擬器，執行 `Run 'app'`。
5.  依照 App 內的引導流程授權即可開始使用。

## 📊 資料來源

*   **政府資料開放平臺**：[詐騙網站清單](https://data.gov.tw/dataset/160055)
*   資料提供單位：內政部警政署刑事警察局

## 📄 授權

MIT License

---
*Original concept inspired by the NoMoreScamTW Chrome Extension.*
