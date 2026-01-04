// IndexedDB Helper
const DB_NAME = 'FraudDB';
const STORE_NAME = 'sites';
const DB_VERSION = 1;

function openDB() {
    return new Promise((resolve, reject) => {
        const request = indexedDB.open(DB_NAME, DB_VERSION);
        request.onupgradeneeded = (event) => {
            const db = event.target.result;
            if (!db.objectStoreNames.contains(STORE_NAME)) {
                db.createObjectStore(STORE_NAME);
            }
        };
        request.onsuccess = () => resolve(request.result);
        request.onerror = () => reject(request.error);
    });
}

async function saveToIndexedDB(data) {
    const db = await openDB();
    const tx = db.transaction(STORE_NAME, 'readwrite');
    const store = tx.objectStore(STORE_NAME);

    // Clear old data first
    await new Promise((resolve, reject) => {
        const clearReq = store.clear();
        clearReq.onsuccess = () => resolve();
        clearReq.onerror = () => reject(clearReq.error);
    });

    // Bulk add
    for (const [url, info] of Object.entries(data)) {
        store.put(info, url);
    }

    return new Promise((resolve, reject) => {
        tx.oncomplete = () => resolve();
        tx.onerror = () => reject(tx.error);
    });
}

async function getFromIndexedDB(url) {
    const db = await openDB();
    const tx = db.transaction(STORE_NAME, 'readonly');
    const store = tx.objectStore(STORE_NAME);
    const request = store.get(url);

    return new Promise((resolve, reject) => {
        request.onsuccess = () => resolve(request.result);
        request.onerror = () => reject(request.error);
    });
}

// 擷取並更新詐騙資料庫
async function updateDatabase() {
    try {
        console.log('Fetching remote config...');
        let govApiUrl = 'https://data.gov.tw/api/v2/rest/dataset/160055'; // Default

        try {
            const configResponse = await fetch(CONFIG_URL);
            if (configResponse.ok) {
                const config = await configResponse.json();
                if (config.fraud_api_url) {
                    govApiUrl = config.fraud_api_url;
                }
            }
        } catch (configError) {
            console.warn('Failed to fetch config, using default Gov API URL:', configError);
        }

        console.log(`Fetching metadata from Gov API: ${govApiUrl}`);
        const govResponse = await fetch(govApiUrl);
        if (!govResponse.ok) throw new Error('Gov API fetch failed');

        const govJson = await govResponse.json();
        const downloadUrl = govJson?.result?.distribution?.[0]?.resourceDownloadUrl;

        if (!downloadUrl) throw new Error('Could not find download URL in Gov API JSON');

        console.log(`Downloading CSV from: ${downloadUrl}`);
        const response = await fetch(downloadUrl);
        if (!response.ok) throw new Error('CSV download failed');

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

        const totalEntries = lines.length - 2;

        // Save metadata to storage.local
        await chrome.storage.local.set({
            lastUpdated: Date.now(),
            totalEntries: totalEntries,
            lastError: null // Clear error
        });

        // Save big data to IndexedDB
        await saveToIndexedDB(data);

        cachedDatabase = data; // Update cache

        // 確保下次更新已排程並顯示
        scheduleDailyUpdate();
        console.log(`Database updated. Loaded ${Object.keys(data).length} unique sites, ${totalEntries} total records.`);
        return { success: true, count: totalEntries };

    } catch (error) {
        console.error('Failed to update database:', error);
        // Log error to storage for debugging in popup
        chrome.storage.local.set({
            lastError: error.message || 'Unknown error',
            lastErrorTs: Date.now()
        });
        return { success: false, error: error.message || 'Unknown error' };
    }
}

// 檢查 URL 是否在資料庫中
async function checkUrl(url) {
    try {
        const urlObj = new URL(url);
        const hostname = urlObj.hostname;

        // Use cache if available
        if (cachedDatabase && Object.keys(cachedDatabase).length > 0) {
            const checkCache = (domain) => cachedDatabase[domain] || null;
            let res = checkCache(hostname);
            if (!res && hostname.startsWith('www.')) res = checkCache(hostname.slice(4));
            if (!res && !hostname.startsWith('www.')) res = checkCache('www.' + hostname);
            if (res) return res;
        }

        // Fallback or Initial check from IndexedDB
        const checkDB = async (domain) => {
            try {
                return await getFromIndexedDB(domain);
            } catch (e) { return null; }
        };

        // 1. Check exact match
        let result = await checkDB(hostname);
        if (result) return result;

        // 2. Check w/o 'www.' if present
        if (hostname.startsWith('www.')) {
            result = await checkDB(hostname.slice(4));
            if (result) return result;
        }

        // 3. Check w/ 'www.' if missing
        if (!hostname.startsWith('www.')) {
            result = await checkDB('www.' + hostname);
            if (result) return result;
        }

        return null;
    } catch (e) {
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
