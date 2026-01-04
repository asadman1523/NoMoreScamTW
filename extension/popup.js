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
    document.getElementById('appName').textContent = chrome.i18n.getMessage('appName');
    document.getElementById('settingsTitle').textContent = chrome.i18n.getMessage('settingsTitle');
    document.getElementById('settingLabel').textContent = chrome.i18n.getMessage('showLeaveBtnLabel');

    // Set Version
    const manifest = chrome.runtime.getManifest();
    document.getElementById('version').textContent = `v${manifest.version}`;

    chrome.storage.local.get(['showLeaveBtn'], (result) => {
        // Default to false (unchecked) if undefined
        showLeaveBtn.checked = result.showLeaveBtn || false;
    });

    showLeaveBtn.addEventListener('change', (e) => {
        chrome.storage.local.set({ showLeaveBtn: e.target.checked });
    });

    async function updateStatus() {
        const { fraudDatabase, lastUpdated, nextUpdateTime, termsAccepted, totalEntries } = await chrome.storage.local.get(['fraudDatabase', 'lastUpdated', 'nextUpdateTime', 'termsAccepted', 'totalEntries']);

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
            const nextDate = nextUpdateTime ? new Date(nextUpdateTime).toLocaleString() : '...';
            const count = totalEntries || Object.keys(fraudDatabase).length;

            // Check cooldown (5 minutes = 300000 ms)
            const now = Date.now();
            const timeDiff = now - lastUpdated;
            const cooldown = 5 * 60 * 1000;

            if (timeDiff < cooldown) {
                const remainingMinutes = Math.ceil((cooldown - timeDiff) / 60000);
                updateBtn.disabled = true;
                updateBtn.textContent = `... (${remainingMinutes})`;
            } else {
                updateBtn.disabled = false;
                updateBtn.textContent = chrome.i18n.getMessage('btnUpdate') || '手動更新資料庫';
            }

            statusDiv.innerHTML = `
        <strong>${chrome.i18n.getMessage('databaseStatus')}</strong><br>
        ${chrome.i18n.getMessage('lastUpdatedLabel')} ${lastDate}<br>
        ${chrome.i18n.getMessage('nextUpdateLabel')} ${nextDate}<br>
        ${chrome.i18n.getMessage('totalRecordsLabel')} ${count}
      `;
        } else {
            statusDiv.textContent = '...';
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
