const form = document.getElementById('shorten-form');
const urlInput = document.getElementById('url-input');
const submitBtn = document.getElementById('submit-btn');
const resultDiv = document.getElementById('result');
const shortUrlLink = document.getElementById('short-url');
const copyBtn = document.getElementById('copy-btn');
const errorDiv = document.getElementById('error');

form.addEventListener('submit', async (e) => {
    e.preventDefault();
    hideMessages();

    const url = urlInput.value.trim();
    if (!url) return;

    submitBtn.disabled = true;
    submitBtn.textContent = 'Shortening...';

    try {
        const res = await fetch('/api/shorten', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ url })
        });

        const data = await res.json();

        if (!res.ok) {
            showError(data.error || 'Something went wrong');
            return;
        }

        shortUrlLink.href = data.shortUrl;
        shortUrlLink.textContent = data.shortUrl;
        resultDiv.classList.remove('hidden');
    } catch (err) {
        showError('Network error — is the server running?');
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Shorten';
    }
});

copyBtn.addEventListener('click', () => {
    navigator.clipboard.writeText(shortUrlLink.href).then(() => {
        copyBtn.textContent = 'Copied!';
        setTimeout(() => { copyBtn.textContent = 'Copy'; }, 1500);
    });
});

function showError(msg) {
    errorDiv.textContent = msg;
    errorDiv.classList.remove('hidden');
}

function hideMessages() {
    resultDiv.classList.add('hidden');
    errorDiv.classList.add('hidden');
}
