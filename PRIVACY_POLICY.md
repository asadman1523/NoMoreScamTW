# 隱私權政策 (Privacy Policy)

**生效日期：2024 年 1 月 1 日**

**NoMoreScamTW (麥騙)** (以下簡稱「本應用程式」) 由開發者 **Jack Wu** 獨立開發。我們非常重視您的隱私權，本政策旨在說明我們如何處理您的資料，特別是關於 Android 無障礙服務 (Accessibility Service) 的使用。

## 1. 非官方聲明與資料來源 (Non-Affiliation & Data Source)

*   **非官方聲明**：本應用程式為獨立開發之工具軟體，**不代表任何政府機關**。
*   **資料來源**：本應用程式使用的詐騙網站清單，來源為台灣政府資料開放平臺 (https://data.gov.tw/dataset/160123)，僅用於協助使用者識別潛在風險。

## 2. Android 無障礙服務使用聲明 (Accessibility Service API Disclosure)

本應用程式的 Android 版本需要使用 **AccessibilityService API (無障礙服務)**，我們在此做出醒目揭露與隱私承諾：

*   **使用目的**：此權限僅用於**「讀取瀏覽器網址列的 URL」**。這是為了在您瀏覽網頁時，即時將網址與本機端的詐騙資料庫進行比對，以提供防詐騙警示功能。
*   **資料處理方式**：
    *   **僅本地處理**：所有網址比對皆在您的手機內部 (Local) 完成。
    *   **不收集敏感個資**：我們**絕不會**透過此 API 讀取、收集、儲存或傳輸您的密碼、信用卡號、銀行帳號或其他任何敏感個人資訊。
    *   **不上傳伺服器**：您的瀏覽紀錄與網址資訊**不會**被上傳至任何外部伺服器進行分析或儲存。

## 3. 資料收集與使用 (Data Collection and Use)

本應用程式 (包含 iOS、Android 及 Chrome 擴充功能版本) **不會** 收集、儲存或傳送您的任何個人資料至開發者的伺服器。

*   **網址比對**：所有的比對作業皆在您的裝置本機端進行。
*   **資料庫更新**：本應用程式僅會定期連線至公開的資料來源 (如 GitHub CDN 或 政府開放資料平台) 下載最新的詐騙清單。此過程不涉及上傳使用者資料。

## 4. 權限說明 (Permissions)

為了提供防護功能，我們需要以下權限：

*   **Android - 顯示在其他應用程式上層 (Overlay)**：用於在偵測到詐騙網站時，立即在螢幕上顯示紅色的全螢幕警示視窗。
*   **Android - 無障礙服務 (Accessibility)**：用於偵測當前瀏覽網址 (如上述第 2 點詳述)。
*   **Chrome Extension - Tabs/WebNavigation**：用於偵測瀏覽器分頁的網址狀態。

## 5. 第三方服務 (Third-Party Services)

本應用程式未串接任何用於追蹤使用者行為或廣告投遞的第三方服務 (如 Google Analytics、Firebase Analytics 等)。我們致力於提供純淨、無追蹤的使用體驗。

## 6. 聯絡我們 (Contact)

若您對本隱私權政策有任何疑問，歡迎透過以下方式聯繫開發者：

*   **開發者**：Jack Wu
*   **Email**: dm3352andy@gmail.com
*   **GitHub Issue**: https://github.com/asadman1523/NoMoreScamTW/issues
