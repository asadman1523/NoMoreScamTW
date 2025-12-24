document.addEventListener('DOMContentLoaded', async () => {
    const statusDiv = document.getElementById('status');
    const updateBtn = document.getElementById('updateBtn');

    // Load initial status
    updateStatus();

    updateBtn.addEventListener('click', () => {
        updateBtn.disabled = true;
        updateBtn.textContent = '更新中...';

        chrome.runtime.sendMessage({ action: 'forceUpdate' }, (response) => {
            updateBtn.disabled = false;
            updateBtn.textContent = '更新資料庫';

            if (response && response.success) {
                updateStatus();
            } else {
                statusDiv.textContent = '更新失敗，請檢查網路連線。';
            }
        });
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
            return;
        }

        if (fraudDatabase && lastUpdated) {
            const lastDate = new Date(lastUpdated).toLocaleString();
            const nextDate = nextUpdateTime ? new Date(nextUpdateTime).toLocaleString() : '初始化中...';
            const count = Object.keys(fraudDatabase).length;

            statusDiv.innerHTML = `
        <strong>資料庫狀態：正常</strong><br>
        最後更新：${lastDate}<br>
        下次更新：${nextDate}<br>
        網站數量：${count}
      `;
        } else {
            statusDiv.textContent = '資料庫尚未初始化。';
        }
    }
});
