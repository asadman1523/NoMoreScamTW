const DATA_URL = 'https://opdadm.moi.gov.tw/api/v1/no-auth/resource/api/dataset/033197D4-70F4-45EB-9FB8-6D83532B999A/resource/D24B474A-9239-44CA-8177-56D7859A31F6/download';

// 擷取並更新詐騙資料庫
async function updateDatabase() {
    try {
        console.log('Fetching fraud database...');
        const response = await fetch(DATA_URL);
        const text = await response.text();
        const lines = text.split(/\r?\n/);

        // 跳過標頭 (根據範例跳過前 2 行)
        // 範例:
        // WEBSITE_NM,WEBURL,CNT,STA_SDATE,STA_EDATE
        // 網站名稱,網址,件數,統計起始日期,統計結束日期
        const data = {};

        // 從索引 2 開始
        for (let i = 2; i < lines.length; i++) {
            const line = lines[i].trim();
            if (!line) continue;

            // 簡單的 CSV 分割 (假設範例中的欄位沒有逗號)
            // 如果欄位中有逗號，可能需要更好的解析器。
            // 但對於 URL 和名稱，通常是安全的。
            const parts = line.split(',');
            if (parts.length >= 2) {
                const name = parts[0].trim();
                let url = parts[1].trim();

                // 正規化 URL: 移除 http://, https://, www. 前綴 (如果需要)，或保持原樣。
                // 清單中有 'www.0857.games', 'tw11st.com'。
                // 我們應該直接儲存清單中的 hostname，
                // 也許也儲存一個沒有 'www.' 的版本？
                // 目前先儲存清單中的原始網域。
                // 移除協定 (如果存在)
                url = url.replace(/^https?:\/\//, '');
                // 移除結尾斜線
                url = url.replace(/\/$/, '');

                if (url) {
                    data[url] = name;
                }
            }
        }

        await chrome.storage.local.set({ fraudDatabase: data, lastUpdated: Date.now() });
        // 確保下次更新已排程並顯示
        scheduleDailyUpdate();
        console.log(`Database updated. Loaded ${Object.keys(data).length} entries.`);

    } catch (error) {
        console.error('Failed to update database:', error);
    }
}

// 檢查 URL 是否在資料庫中
async function checkUrl(url) {
    try {
        const { fraudDatabase } = await chrome.storage.local.get('fraudDatabase');
        if (!fraudDatabase) return null;

        const urlObj = new URL(url);
        const hostname = urlObj.hostname;

        // 檢查完全符合
        if (fraudDatabase[hostname]) {
            return fraudDatabase[hostname];
        }

        // 如果適用，檢查沒有 'www.' 的版本
        if (hostname.startsWith('www.')) {
            const rootDomain = hostname.slice(4);
            if (fraudDatabase[rootDomain]) {
                return fraudDatabase[rootDomain];
            }
        }

        // 如果沒有 'www.'，檢查有的版本
        if (!hostname.startsWith('www.')) {
            const wwwDomain = 'www.' + hostname;
            if (fraudDatabase[wwwDomain]) {
                return fraudDatabase[wwwDomain];
            }
        }

        // 另外檢查清單中是否有 'www.' 但用戶訪問的是根網域
        // 例如：清單有 'www.example.com'，用戶訪問 'example.com'
        // 若沒有雙重儲存，這很難有效率地查詢。
        // 目前假設清單是權威的。

        return null;
    } catch (e) {
        // 無效的 URL
        return null;
    }
}

// 根據上次更新時間排程每日更新
function scheduleDailyUpdate(lastUpdatedTime) {
    const baseTime = lastUpdatedTime || Date.now();
    const nextRun = baseTime + 24 * 60 * 60 * 1000;

    chrome.alarms.create('dailyUpdate', {
        when: nextRun,
        periodInMinutes: 1440 // 24 小時
    });
    chrome.storage.local.set({ nextUpdateTime: nextRun });
}

// 事件監聽器
chrome.runtime.onInstalled.addListener(() => {
    updateDatabase();
});

chrome.runtime.onStartup.addListener(async () => {
    const { lastUpdated } = await chrome.storage.local.get('lastUpdated');
    const now = Date.now();

    // 如果從未更新或距離上次更新超過 24 小時
    if (!lastUpdated || (now - lastUpdated) >= 24 * 60 * 60 * 1000) {
        console.log('Startup: Database outdated, updating...');
        await updateDatabase();
    } else {
        console.log('Startup: Database is fresh. Ensuring schedule is set.');
        scheduleDailyUpdate(lastUpdated);
    }
});

chrome.alarms.onAlarm.addListener((alarm) => {
    if (alarm.name === 'dailyUpdate') {
        console.log('Running daily update...');
        updateDatabase();
    }
});

chrome.tabs.onUpdated.addListener(async (tabId, changeInfo, tab) => {
    if (changeInfo.status === 'complete' && tab.url) {
        const fraudName = await checkUrl(tab.url);
        if (fraudName) {
            console.log(`Fraud detected: ${tab.url} (${fraudName})`);
            chrome.tabs.sendMessage(tabId, {
                action: 'showWarning',
                fraudName: fraudName
            }).catch(() => {
                // Content script 可能尚未準備好或未注入某些頁面 (例如 chrome://)
            });
        }
    }
});

// popup 的訊息監聽器
chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
    if (request.action === 'forceUpdate') {
        updateDatabase().then(() => sendResponse({ success: true }));
        return true; // 保持通道開啟
    }
});
