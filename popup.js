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
        const { fraudDatabase, lastUpdated, nextUpdateTime } = await chrome.storage.local.get(['fraudDatabase', 'lastUpdated', 'nextUpdateTime']);

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
