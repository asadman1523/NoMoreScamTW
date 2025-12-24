// 模擬 chrome API
global.chrome = {
    storage: {
        local: {
            set: async (data) => {
                if (data.fraudDatabase) {
                    console.log('儲存設定: fraudDatabase 包含', Object.keys(data.fraudDatabase).length, '筆資料');
                } else {
                    console.log('儲存設定:', Object.keys(data));
                }
                global.mockStorage = { ...global.mockStorage, ...data };
            },
            get: async (keys) => {
                return global.mockStorage || {};
            }
        }
    },
    runtime: {
        onInstalled: { addListener: () => { } },
        onStartup: { addListener: (cb) => { global.startupCallback = cb; } },
        onMessage: { addListener: () => { } }
    },
    alarms: {
        create: (name, alarmInfo) => {
            console.log(`建立鬧鐘: ${name}`, alarmInfo);
            global.mockAlarm = { name, ...alarmInfo };
        },
        onAlarm: { addListener: (callback) => { global.alarmCallback = callback; } }
    },
    tabs: {
        onUpdated: { addListener: () => { } }
    }
};

// 模擬 fetch
global.fetch = async (url) => {
    console.log('正在擷取 URL:', url);
    // 回傳範列 CSV 內容
    const sampleCsv = `WEBSITE_NM,WEBURL,CNT,STA_SDATE,STA_EDATE
網站名稱,網址,件數,統計起始日期,統計結束日期
0857娛樂城,www.0857.games,1,2023/12/12,2023/12/18
TestFraud,test-fraud.com,1,2023/01/01,2023/01/02
WithWWW,www.bad-site.com,1,2023/01/01,2023/01/02`;

    return {
        text: async () => sampleCsv
    };
};

// 載入 background script 內容 (使用 eval 進行測試)
const fs = require('fs');
const path = require('path');
const backgroundCode = fs.readFileSync(path.join(__dirname, 'background.js'), 'utf8');

// 執行 background 程式碼
eval(backgroundCode);

async function runTest() {
    console.log('--- 開始測試 ---');

    // 1. 測試更新
    await updateDatabase();

    // 2. 測試檢查 URL
    console.log('正在檢查 www.0857.games...');
    let result = await checkUrl('https://www.0857.games/');
    console.log('結果:', result); // 應為 '0857娛樂城'

    console.log('正在檢查 test-fraud.com...');
    result = await checkUrl('http://test-fraud.com/login');
    console.log('結果:', result); // 應為 'TestFraud'

    console.log('正在檢查 bad-site.com (www 的根網域)...');
    result = await checkUrl('https://bad-site.com');
    console.log('bad-site.com 的結果:', result);

    console.log('正在檢查 www.bad-site.com...');
    result = await checkUrl('https://www.bad-site.com');
    console.log('www.bad-site.com 的結果:', result); // 應為 'WithWWW'

    console.log('正在檢查 google.com...');
    result = await checkUrl('https://google.com');
    console.log('結果:', result); // 應為 null

    // 3. 測試鬧鐘
    console.log('正在檢查鬧鐘...');
    // 觸發 onStartup 來排程鬧鐘
    if (global.startupCallback) await global.startupCallback();

    if (global.mockAlarm && global.mockAlarm.name === 'dailyUpdate') {
        console.log('鬧鐘已成功排程。');
        console.log('週期:', global.mockAlarm.periodInMinutes);
        const nextRun = new Date(global.mockAlarm.when);
        console.log('下次執行:', nextRun.toLocaleString());

        // 驗證大約是從現在開始的 24 小時後
        const now = Date.now();
        const diff = nextRun.getTime() - now;
        // 允許些微誤差 (例如 24 小時內的 5 秒誤差)
        const expectedDiff = 24 * 60 * 60 * 1000;

        if (Math.abs(diff - expectedDiff) < 5000) {
            console.log('鬧鐘時間正確 (大約從現在起 24 小時)。');
        } else {
            console.error('鬧鐘時間不正確。差異:', diff, '預期:', expectedDiff);
        }

        // 檢查 storage 中的 nextUpdateTime
        if (global.mockStorage && global.mockStorage.nextUpdateTime) {
            console.log('下次更新時間已儲存:', new Date(global.mockStorage.nextUpdateTime).toLocaleString());
        } else {
            console.error('下次更新時間未儲存。');
        }
    } else {
        console.error('鬧鐘未排程。');
    }

    // 4. 測試舊資料啟動 (模擬離線 > 24 小時)
    console.log('測試舊資料啟動 (模擬離線 > 24 小時)...');
    // 模擬上次更新是 25 小時前
    const twentyFiveHoursAgo = Date.now() - (25 * 60 * 60 * 1000);
    global.mockStorage = { ...global.mockStorage, lastUpdated: twentyFiveHoursAgo };

    // 觸發啟動
    if (global.startupCallback) {
        await global.startupCallback();
        // 檢查 updateDatabase 是否被呼叫 (lastUpdated 現在應該非常新)
        const now = Date.now();
        if (global.mockStorage.lastUpdated > twentyFiveHoursAgo && (now - global.mockStorage.lastUpdated) < 1000) {
            console.log('資料庫在啟動時正確更新。');
        } else {
            console.error('資料庫啟動時更新失敗。上次更新:', global.mockStorage.lastUpdated);
        }
    } else {
        console.error('未註冊啟動監聽器。');
    }
}

runTest();
