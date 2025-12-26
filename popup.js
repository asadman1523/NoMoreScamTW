document.addEventListener('DOMContentLoaded', async () => {
    const statusDiv = document.getElementById('status');
    const updateBtn = document.getElementById('updateBtn');

    // Load initial status
    updateStatus();

    document.getElementById('updateBtn').addEventListener('click', () => {
        chrome.runtime.sendMessage({ action: 'updateDatabase' }, (response) => {
            if (chrome.runtime.lastError) {
                alert('通訊錯誤：' + chrome.runtime.lastError.message);
                return;
            }
            if (response && response.success) {
                updateStatus();
                // alert('資料庫更新成功！\n共 ' + response.count + ' 筆資料。');
            } else {
                const errorMsg = response && response.error ? response.error : '未知錯誤';
                // Use JSON.stringify for object errors to make them readable
                const debugInfo = typeof errorMsg === 'object' ? JSON.stringify(errorMsg) : errorMsg;
                alert('更新失敗：' + debugInfo);
            }
        });
    });

    // Initialize Setting
    const showLeaveBtn = document.getElementById('showLeaveBtn');

    // Set text dynamically to prevent flash
    document.getElementById('settingsTitle').textContent = '設定';
    document.getElementById('settingLabel').textContent = '顯示「立即離開」按鈕';

    chrome.storage.local.get(['showLeaveBtn'], (result) => {
        // Default to false (unchecked) if undefined
        showLeaveBtn.checked = result.showLeaveBtn || false;
    });

    showLeaveBtn.addEventListener('change', (e) => {
        chrome.storage.local.set({ showLeaveBtn: e.target.checked });
    });

    async function updateStatus() {
        const { fraudDatabase, lastUpdated, nextUpdateTime, termsAccepted } = await chrome.storage.local.get(['fraudDatabase', 'lastUpdated', 'nextUpdateTime', 'termsAccepted']);

        if (!termsAccepted) {
            statusDiv.innerHTML = `
                <div style="color: #d93025; margin-bottom: 10px;">⚠️ 尚未同意免責聲明</div>
                <button id="openTermsBtn" style="background:#4285f4; color:white; border:none; padding:5px 10px; border-radius:4px; cursor:pointer;">開啟條款頁面</button>
            `;
            document.getElementById('openTermsBtn').addEventListener('click', () => {
                chrome.tabs.create({ url: 'welcome.html' });
            });
            updateBtn.disabled = true; // Cannot update if terms not accepted
            return;
        }

        if (fraudDatabase && lastUpdated) {
            const lastDate = new Date(lastUpdated).toLocaleString();
            const nextDate = nextUpdateTime ? new Date(nextUpdateTime).toLocaleString() : '初始化中...';
            const count = Object.keys(fraudDatabase).length;

            // Check cooldown (5 minutes = 300000 ms)
            const now = Date.now();
            const timeDiff = now - lastUpdated;
            const cooldown = 5 * 60 * 1000;

            if (timeDiff < cooldown) {
                const remainingMinutes = Math.ceil((cooldown - timeDiff) / 60000);
                updateBtn.disabled = true;
                updateBtn.textContent = `更新冷卻中 (${remainingMinutes} 分鐘)`;
                updateBtn.style.backgroundColor = '#ccc';
                updateBtn.style.cursor = 'not-allowed';
            } else {
                updateBtn.disabled = false;
                updateBtn.textContent = '手動更新資料庫';
                updateBtn.style.backgroundColor = '#2196F3';
                updateBtn.style.cursor = 'pointer';
            }

            statusDiv.innerHTML = `
        <strong>資料庫狀態：正常</strong><br>
        最後更新：${lastDate}<br>
        下次更新：${nextDate}<br>
        網站數量：${count}
      `;
        } else {
            statusDiv.textContent = '資料庫尚未初始化。';
            updateBtn.disabled = false;
        }
    }
    // Footer Links Handlers
    document.getElementById('githubLink').addEventListener('click', (e) => {
        e.preventDefault();
        chrome.tabs.create({ url: 'https://github.com/asadman1523/NoMoreScamTW' });
    });

    document.getElementById('opayBtn').addEventListener('click', (e) => {
        e.preventDefault();
        chrome.tabs.create({ url: 'https://p.opay.tw/qRHBb' });
    });

    document.getElementById('paypalBtn').addEventListener('click', (e) => {
        e.preventDefault();
        chrome.tabs.create({ url: 'https://www.paypal.com/ncp/payment/C6KHB3JZYZU5Y' });
    });
});
