chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
    if (request.action === 'showWarning') {
        showWarning(request.fraudName);
    }
});

function showWarning(fraudName) {
    // Check if already shown
    if (document.getElementById('fraud-guard-overlay')) return;

    const overlay = document.createElement('div');
    overlay.id = 'fraud-guard-overlay';

    overlay.innerHTML = `
    <div id="fraud-guard-modal">
      <div id="fraud-guard-icon">⚠️</div>
      <div id="fraud-guard-title">警告：疑似詐騙網站</div>
      <div id="fraud-guard-message">
        您正在瀏覽的網站已被政府列為詐騙網站。請立即離開以保護您的財產安全。
      </div>
      <div id="fraud-guard-details">
        詐騙類型/名稱：${fraudName}
      </div>
      <button id="fraud-guard-button">立即離開 (回到 Google)</button>
      <button id="fraud-guard-ignore">我了解風險，繼續瀏覽</button>
    </div>
  `;

    document.body.appendChild(overlay);

    // Prevent scrolling
    document.body.style.overflow = 'hidden';

    document.getElementById('fraud-guard-button').addEventListener('click', () => {
        window.location.href = 'https://www.google.com';
    });

    document.getElementById('fraud-guard-ignore').addEventListener('click', () => {
        document.body.removeChild(overlay);
        document.body.style.overflow = '';
    });
}
