const CONFIG_URL = 'https://cdn.jsdelivr.net/gh/asadman1523/NoMoreScamTW@main/server_config.json';
const DEFAULT_DATA_URL = 'https://opdadm.moi.gov.tw/api/v1/no-auth/resource/api/dataset/033197D4-70F4-45EB-9FB8-6D83532B999A/resource/D24B474A-9239-44CA-8177-56D7859A31F6/download';

// In-memory cache for the database
let cachedDatabase = null;

// 擷取並更新詐騙資料庫
async function updateDatabase() {
    try {
        console.log('Fetching remote config...');
        let dataUrl = DEFAULT_DATA_URL;

        try {
            const configResponse = await fetch(CONFIG_URL);
            if (configResponse.ok) {
                const config = await configResponse.json();
                if (config.csv_url) {
                    dataUrl = config.csv_url;
                }
            }
        } catch (configError) {
            console.warn('Failed to fetch config, using default:', configError);
        }

        console.log(`Fetching fraud database from: ${dataUrl}`);
        const response = await fetch(dataUrl);
        const text = await response.text();
        const lines = text.split(/\r?\n/);

        // CSV Header: WEBSITE_NM,WEBURL,CNT,STA_SDATE,STA_EDATE
        const data = {};

        // 從索引 2 開始 (跳過標頭)
        for (let i = 2; i < lines.length; i++) {
            const line = lines[i].trim();
            if (!line) continue;

            // Regex to split by comma, ignoring commas inside quotes
            const parts = line.split(/,(?=(?:(?:[^"]*"){2})*[^"]*$)/);

            if (parts.length >= 2) {
                // Remove potential quotes around fields
                const cleanParts = parts.map(p => p.trim().replace(/^"|"$/g, ''));

                const name = cleanParts[0];
                let rawUrl = cleanParts[1];
                const count = cleanParts[2] || '0';
                const startDate = cleanParts[3] || '';
                const endDate = cleanParts[4] || '';

                if (!rawUrl) continue;

                // 正規化 URL
                let url = rawUrl.replace(/^https?:\/\//, '');
                url = url.replace(/\/$/, '');

                if (url) {
                    data[url] = {
                        name: name,
                        url: rawUrl,
                        count: count,
                        startDate: startDate,
                        endDate: endDate
                    };
                }
            }
        }

        await chrome.storage.local.set({ fraudDatabase: data, lastUpdated: Date.now() });
        cachedDatabase = data; // Update cache

        // 確保下次更新已排程並顯示
        scheduleDailyUpdate();
        console.log(`Database updated. Loaded ${Object.keys(data).length} entries.`);
        return { success: true, count: Object.keys(data).length };

    } catch (error) {
        console.error('Failed to update database:', error);
        return { success: false, error: error.message || 'Unknown error' };
    }
}

// 檢查 URL 是否在資料庫中
async function checkUrl(url) {
    try {
        // Use cache if available, otherwise load from storage
        if (!cachedDatabase) {
            const { fraudDatabase } = await chrome.storage.local.get('fraudDatabase');
            cachedDatabase = fraudDatabase || {};
        }

        if (!cachedDatabase || Object.keys(cachedDatabase).length === 0) return null;

        const urlObj = new URL(url);
        const hostname = urlObj.hostname;

        // Helper to check domain against DB
        const check = (domain) => {
            return cachedDatabase[domain] || null;
        };

        // 1. Check exact match
        let result = check(hostname);
        if (result) return result;

        // 2. Check w/o 'www.' if present
        if (hostname.startsWith('www.')) {
            result = check(hostname.slice(4));
            if (result) return result;
        }

        // 3. Check w/ 'www.' if missing
        if (!hostname.startsWith('www.')) {
            result = check('www.' + hostname);
            if (result) return result;
        }

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
chrome.runtime.onInstalled.addListener(async () => {
    updateDatabase();

    // Check acceptance
    const { termsAccepted } = await chrome.storage.local.get('termsAccepted');
    if (!termsAccepted) {
        chrome.tabs.create({ url: 'welcome.html' });
    }
});

chrome.runtime.onStartup.addListener(async () => {
    // Populate cache on startup
    const { fraudDatabase, lastUpdated, termsAccepted } = await chrome.storage.local.get(['fraudDatabase', 'lastUpdated', 'termsAccepted']);

    if (!termsAccepted) {
        // Optional: Open welcome page every startup if not accepted?
        // Let's just rely on onInstalled or user clicking extension icon.
        // Actually, for better visibility:
        chrome.tabs.create({ url: 'welcome.html' });
    }

    if (fraudDatabase) {
        cachedDatabase = fraudDatabase;
    }

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
        // Enforce Terms: Check if user agreed
        // Since this hotpath runs often, we should cache termsAccepted too, but storage.get is fast enough for now locally.
        const { termsAccepted } = await chrome.storage.local.get('termsAccepted');

        if (!termsAccepted) {
            return; // Do not protect if not agreed
        }

        const fraudInfo = await checkUrl(tab.url);
        if (fraudInfo) {
            console.log(`Fraud detected: ${tab.url}`, fraudInfo);
            chrome.tabs.sendMessage(tabId, {
                action: 'showWarning',
                fraudInfo: fraudInfo
            }).catch(() => {
                // Content script 可能尚未準備好或未注入某些頁面 (例如 chrome://)
            });
        }
    }
});

// popup 的訊息監聽器
chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
    if (request.action === 'updateDatabase' || request.action === 'forceUpdate') {
        updateDatabase()
            .then((result) => {
                sendResponse(result);
            })
            .catch((err) => {
                console.error('updateDatabase threw error:', err);
                sendResponse({ success: false, error: err.message });
            });

        return true; // 保持通道開啟
    }
});
