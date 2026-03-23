const API = '/api/containers';
let currentContainer = null;

// ── Helpers ──
async function api(path, opts = {}) {
    const res = await fetch(path, opts);
    if (!res.ok && !opts.allowFail) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body.error || `Request failed (${res.status})`);
    }
    return res;
}

function toast(message, type = 'success') {
    const el = document.createElement('div');
    el.className = `toast ${type}`;
    el.textContent = message;
    document.body.appendChild(el);
    setTimeout(() => el.remove(), 3000);
}

function formatSize(bytes) {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

function fileIcon() {
    return `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"/>
        <polyline points="13 2 13 9 20 9"/>
    </svg>`;
}

function containerIcon() {
    return `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/>
    </svg>`;
}

// ── Containers ──
async function loadContainers() {
    try {
        const res = await api(API);
        const containers = await res.json();
        const list = document.getElementById('container-list');
        list.innerHTML = '';
        containers.forEach(name => {
            const li = document.createElement('li');
            li.innerHTML = `${containerIcon()} ${name}`;
            if (name === currentContainer) li.classList.add('active');
            li.onclick = () => selectContainer(name);
            list.appendChild(li);
        });
    } catch (e) {
        toast(e.message, 'error');
    }
}

async function selectContainer(name) {
    currentContainer = name;
    document.querySelectorAll('.container-list li').forEach(li => li.classList.remove('active'));
    document.querySelectorAll('.container-list li').forEach(li => {
        if (li.textContent.trim() === name) li.classList.add('active');
    });
    document.getElementById('empty-state').style.display = 'none';
    document.getElementById('container-view').style.display = 'flex';
    document.getElementById('container-view').style.flexDirection = 'column';
    document.getElementById('container-view').style.flex = '1';
    document.getElementById('current-container-name').textContent = name;
    await loadBlobs();
}

async function createContainer() {
    const input = document.getElementById('new-container-input');
    const name = input.value.trim().toLowerCase();
    if (!name) return;
    try {
        await api(`${API}/${name}`, { method: 'POST' });
        toast(`Container "${name}" created`);
        closeModal();
        await loadContainers();
        await selectContainer(name);
    } catch (e) {
        toast(e.message, 'error');
    }
}

async function deleteCurrentContainer() {
    if (!currentContainer) return;
    if (!confirm(`Delete container "${currentContainer}" and all its files?`)) return;
    try {
        await api(`${API}/${currentContainer}`, { method: 'DELETE' });
        toast(`Container "${currentContainer}" deleted`);
        currentContainer = null;
        document.getElementById('container-view').style.display = 'none';
        document.getElementById('empty-state').style.display = 'flex';
        await loadContainers();
    } catch (e) {
        toast(e.message, 'error');
    }
}

// ── Blobs ──
async function loadBlobs() {
    if (!currentContainer) return;
    try {
        const res = await api(`${API}/${currentContainer}/blobs`);
        const blobs = await res.json();
        const grid = document.getElementById('blob-list');
        const empty = document.getElementById('blobs-empty');

        if (blobs.length === 0) {
            grid.innerHTML = '';
            empty.style.display = 'flex';
            return;
        }
        empty.style.display = 'none';
        grid.innerHTML = blobs.map(b => `
            <div class="blob-card">
                <div class="blob-icon">${fileIcon()}</div>
                <div class="blob-name">${b.name}</div>
                <div class="blob-meta">${formatSize(b.size)}${b.lastModified ? ' · ' + new Date(b.lastModified).toLocaleDateString() : ''}</div>
                <div class="blob-actions">
                    <button class="btn" onclick="downloadBlob('${b.name}')">
                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="8 17 12 21 16 17"/><line x1="12" y1="12" x2="12" y2="21"/><path d="M20.88 18.09A5 5 0 0 0 18 9h-1.26A8 8 0 1 0 3 16.29"/></svg>
                        Download
                    </button>
                    <button class="btn btn-danger" onclick="deleteBlob('${b.name}')">Delete</button>
                </div>
            </div>
        `).join('');
    } catch (e) {
        toast(e.message, 'error');
    }
}

function downloadBlob(name) {
    window.open(`${API}/${currentContainer}/blobs/${encodeURIComponent(name)}`, '_blank');
}

async function deleteBlob(name) {
    if (!confirm(`Delete "${name}"?`)) return;
    try {
        await api(`${API}/${currentContainer}/blobs/${encodeURIComponent(name)}`, { method: 'DELETE' });
        toast(`"${name}" deleted`);
        await loadBlobs();
    } catch (e) {
        toast(e.message, 'error');
    }
}

// ── Upload ──
async function uploadFiles(files) {
    if (!currentContainer || files.length === 0) return;
    const progress = document.getElementById('upload-progress');
    const fill = document.getElementById('progress-fill');
    const text = document.getElementById('progress-text');
    progress.style.display = 'flex';

    for (let i = 0; i < files.length; i++) {
        const pct = Math.round(((i) / files.length) * 100);
        fill.style.width = pct + '%';
        text.textContent = `Uploading ${i + 1}/${files.length}: ${files[i].name}`;

        const form = new FormData();
        form.append('file', files[i]);
        try {
            await api(`${API}/${currentContainer}/blobs`, { method: 'POST', body: form });
        } catch (e) {
            toast(`Failed to upload ${files[i].name}: ${e.message}`, 'error');
        }
    }
    fill.style.width = '100%';
    text.textContent = 'Done!';
    setTimeout(() => { progress.style.display = 'none'; fill.style.width = '0%'; }, 1500);
    toast(`${files.length} file(s) uploaded`);
    await loadBlobs();
}

// ── Modal ──
function openModal() {
    document.getElementById('modal-overlay').style.display = 'flex';
    document.getElementById('new-container-input').value = '';
    setTimeout(() => document.getElementById('new-container-input').focus(), 100);
}
function closeModal() {
    document.getElementById('modal-overlay').style.display = 'none';
}

// ── Event Listeners ──
document.addEventListener('DOMContentLoaded', () => {
    loadContainers();

    document.getElementById('btn-new-container').onclick = openModal;
    document.getElementById('btn-cancel-modal').onclick = closeModal;
    document.getElementById('btn-confirm-modal').onclick = createContainer;
    document.getElementById('new-container-input').addEventListener('keydown', e => {
        if (e.key === 'Enter') createContainer();
        if (e.key === 'Escape') closeModal();
    });
    document.getElementById('modal-overlay').addEventListener('click', e => {
        if (e.target === e.currentTarget) closeModal();
    });

    document.getElementById('btn-delete-container').onclick = deleteCurrentContainer;
    document.getElementById('btn-upload').onclick = () => document.getElementById('file-input').click();
    document.getElementById('file-input').onchange = e => uploadFiles(e.target.files);

    // Drag and drop
    const content = document.querySelector('.content');
    const dropZone = document.getElementById('drop-zone');

    content.addEventListener('dragover', e => {
        e.preventDefault();
        if (currentContainer) dropZone.style.display = 'block';
    });
    content.addEventListener('dragleave', e => {
        if (!content.contains(e.relatedTarget)) dropZone.style.display = 'none';
    });
    content.addEventListener('drop', e => {
        e.preventDefault();
        dropZone.style.display = 'none';
        if (currentContainer && e.dataTransfer.files.length) {
            uploadFiles(e.dataTransfer.files);
        }
    });
    dropZone.addEventListener('dragover', e => {
        e.preventDefault();
        dropZone.classList.add('drag-over');
    });
    dropZone.addEventListener('dragleave', () => dropZone.classList.remove('drag-over'));
});
