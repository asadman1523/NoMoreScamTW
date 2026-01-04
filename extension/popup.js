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
        const { lastUpdated, nextUpdateTime, termsAccepted, totalEntries } = await chrome.storage.local.get(['lastUpdated', 'nextUpdateTime', 'termsAccepted', 'totalEntries']);

        // Set Default Button Text if nothing else happens
        updateBtn.textContent = chrome.i18n.getMessage('btnUpdate') || 'Update Database';

        if (!termsAccepted) {
            statusDiv.innerHTML = `
                <div style="color: #d93025; margin-bottom: 10px;">${chrome.i18n.getMessage('termsNotAccepted') || '⚠️ Disclaimer not accepted'}</div>
                <button id="openTermsBtn" style="background:#4285f4; color:white; border:none; padding:8px 15px; border-radius:4px; cursor:pointer;">${chrome.i18n.getMessage('btnOpenTerms') || 'Open Terms'}</button>
            `;
            document.getElementById('openTermsBtn').addEventListener('click', () => {
                chrome.tabs.create({ url: 'welcome.html' });
            });
            updateBtn.disabled = true;
            return;
        }

        if (lastUpdated) {
            const lastDate = new Date(lastUpdated).toLocaleString();
            const nextDate = nextUpdateTime ? new Date(nextUpdateTime).toLocaleString() : '...';
            const count = totalEntries || 0;

            // Check cooldown (5 minutes = 300000 ms)
            const now = Date.now();
            const timeDiff = now - lastUpdated;
            const cooldown = 5 * 60 * 1000;

            if (timeDiff < cooldown) {
                const remainingMinutes = Math.ceil((cooldown - timeDiff) / 60000);
                updateBtn.disabled = true;
                updateBtn.textContent = `${chrome.i18n.getMessage('cooldownLabel') || 'Cooldown'} (${remainingMinutes})`;
            } else {
                updateBtn.disabled = false;
                updateBtn.textContent = chrome.i18n.getMessage('btnUpdate') || 'Update Database';
            }

            statusDiv.innerHTML = `
        <strong>${chrome.i18n.getMessage('databaseStatus') || 'Status: Online'}</strong><br>
        ${chrome.i18n.getMessage('lastUpdatedLabel') || 'Last Updated:'} ${lastDate}<br>
        ${chrome.i18n.getMessage('nextUpdateLabel') || 'Next Update:'} ${nextDate}<br>
        ${chrome.i18n.getMessage('totalRecordsLabel') || 'Total Records:'} ${count}
      `;
        } else {
            const { lastError } = await chrome.storage.local.get('lastError');
            if (lastError) {
                statusDiv.innerHTML = `
                    <div style="color: #d93025; margin-bottom: 5px;">${chrome.i18n.getMessage('dbInitializing') || 'Initializing...'}</div>
                    <div style="font-size: 11px; color: #999;">Error: ${lastError}</div>
                `;
            } else {
                statusDiv.textContent = chrome.i18n.getMessage('dbInitializing') || 'Initializing...';
            }
            updateBtn.disabled = false;
            updateBtn.textContent = chrome.i18n.getMessage('btnUpdate') || 'Update Database';
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
