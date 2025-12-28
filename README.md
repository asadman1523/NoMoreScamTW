# NoMoreScamTW

**NoMoreScamTW** 是一個開源的反詐騙專案，旨在保護台灣使用者免受惡意與詐騙網站的侵害。本專案包含 Chrome 瀏覽器擴充功能與 Android 應用程式。

## 📂 專案結構

本儲存庫 (Repository) 採用 Monorepo 結構，包含以下兩個子專案：

### 1. 🌐 [Chrome Extension (瀏覽器擴充功能)](./extension/)
*   **目錄**: [`extension/`](./extension/)
*   **描述**: 適用於 Google Chrome、Microsoft Edge 等 Chromium 核心瀏覽器。當瀏覽到詐騙網站時，會顯示全螢幕紅色警示。
*   [查看詳細說明](./extension/README.md)

### 2. 📱 [Android App (行動應用程式)](./android/)
*   **目錄**: [`android/`](./android/)
*   **描述**: 適用於 Android 手機。利用無障礙服務 (Accessibility Service) 監控手機瀏覽器，當偵測到詐騙網址時，會顯示懸浮警示橫幅。
*   [查看詳細說明](./android/README.md)

## 🤝 貢獻與授權

歡迎提交 Pull Request 或 Issue 來協助改進本專案。

*   **資料來源**: [政府資料開放平臺 - 詐騙網站清單](https://data.gov.tw/dataset/160055)
*   **授權**: MIT License
