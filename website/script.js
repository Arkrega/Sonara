const CONFIG = {
    owner: "arkrega",
    repo: "sonara",
    defaultVersion: "1.0.0",
    defaultTag: "v1.0.0",
    defaultFilename: "Sonara.apk",
    defaultFallbackUrl: "https://github.com/arkrega/sonara/releases/latest/download/Sonara.apk"
};

function formatBytes(bytes, decimals = 1) {
    if (!bytes || bytes === 0) return "~45 MB";
    const k = 1024;
    const dm = decimals < 0 ? 0 : decimals;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
}

function formatDate(dateStr) {
    if (!dateStr) return "Latest Stable";
    try {
        const d = new Date(dateStr);
        return d.toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        });
    } catch {
        return "Latest Stable";
    }
}

async function fetchLatestRelease() {
    const apiUrl = `https://api.github.com/repos/${CONFIG.owner}/${CONFIG.repo}/releases/latest`;
    
    try {
        const response = await fetch(apiUrl, {
            headers: {
                'Accept': 'application/vnd.github.v3+json'
            }
        });

        if (!response.ok) {
            throw new Error(`GitHub API returned status ${response.status}`);
        }

        const data = await response.json();
        if (data.draft || data.prerelease) {
            throw new Error("Latest release is a draft or prerelease");
        }

        updateUiWithRelease(data);
    } catch (err) {
        useFallbackRelease();
    }
}

function updateUiWithRelease(data) {
    const tag = data.tag_name || CONFIG.defaultTag;
    const version = tag.replace(/^v/i, '');
    const publishedAt = formatDate(data.published_at);
    
    const assets = Array.isArray(data.assets) ? data.assets : [];
    const apkAsset = assets.find(a => a.name === CONFIG.defaultFilename) 
        || assets.find(a => a.name.endsWith('.apk')) 
        || null;
    const shaAsset = assets.find(a => a.name === 'SHA256SUMS.txt') || null;

    const downloadUrl = apkAsset ? apkAsset.browser_download_url : CONFIG.defaultFallbackUrl;
    const fileSize = apkAsset ? formatBytes(apkAsset.size) : "~45 MB";

    const heroVersionTag = document.getElementById('hero-version-tag');
    if (heroVersionTag) heroVersionTag.textContent = `Sonara Stable v${version}`;

    const releaseVersionTitle = document.getElementById('release-version-title');
    if (releaseVersionTitle) releaseVersionTitle.textContent = `Sonara v${version}`;

    const releaseDate = document.getElementById('release-date');
    if (releaseDate) releaseDate.textContent = `Published: ${publishedAt}`;

    const releaseFilename = document.getElementById('release-filename');
    if (releaseFilename && apkAsset) releaseFilename.textContent = apkAsset.name;

    const releaseSize = document.getElementById('release-size');
    if (releaseSize) releaseSize.textContent = fileSize;

    const heroDownloadBtn = document.getElementById('hero-download-btn');
    if (heroDownloadBtn) heroDownloadBtn.href = downloadUrl;

    const mainDownloadBtn = document.getElementById('main-download-button');
    if (mainDownloadBtn) mainDownloadBtn.href = downloadUrl;

    if (shaAsset) {
        fetchSha256Checksum(shaAsset.browser_download_url, apkAsset ? apkAsset.name : CONFIG.defaultFilename);
    }

    if (data.body && data.body.trim().length > 0) {
        const changelogBody = document.getElementById('changelog-body');
        if (changelogBody) {
            const rawBody = data.body.replace(/</g, "&lt;").replace(/>/g, "&gt;");
            changelogBody.innerHTML = `<pre style="white-space: pre-wrap; font-family: inherit; line-height: 1.6;">${rawBody}</pre>`;
        }
    }
}

async function fetchSha256Checksum(url, apkName) {
    try {
        const resp = await fetch(url);
        if (!resp.ok) return;
        const text = await resp.text();
        const line = text.split('\n').find(l => l.includes(apkName));
        if (line) {
            const hash = line.trim().split(/\s+/)[0];
            const checksumText = document.getElementById('checksum-text');
            if (checksumText && hash) {
                checksumText.textContent = hash;
            }
        }
    } catch {
    }
}

function useFallbackRelease() {
    const heroDownloadBtn = document.getElementById('hero-download-btn');
    if (heroDownloadBtn) heroDownloadBtn.href = CONFIG.defaultFallbackUrl;

    const mainDownloadBtn = document.getElementById('main-download-button');
    if (mainDownloadBtn) mainDownloadBtn.href = CONFIG.defaultFallbackUrl;
}

document.addEventListener('DOMContentLoaded', () => {
    fetchLatestRelease();
});
