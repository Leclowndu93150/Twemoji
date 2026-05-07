const API = '';
let token = null;
let session = {
    emojis: [],
    categories: [],
};
let allEmojis = [];
let filteredEmojis = [];
let dragPayload = null; // { char, sourceType: 'sidebar'|'category', sourceCatId? }

const ghost = document.getElementById('drag-ghost');
const searchEl = document.getElementById('search');
const gridEl = document.getElementById('emoji-grid');
const areaEl = document.getElementById('categories-area');
const addCatBtn = document.getElementById('add-cat-btn');
const exportBtn = document.getElementById('export-btn');
const packNameEl = document.getElementById('pack-name');
const iconBtn = document.getElementById('icon-btn');
const iconPicker = document.getElementById('icon-picker');
const iconPickerGrid = document.getElementById('icon-picker-grid');

let packIcon = '🎨';

async function init() {
    const params = new URLSearchParams(location.search);
    token = params.get('t');

    if (!token) {
        showEmpty('no session — use the discord bot to get a link');
        return;
    }

    try {
        const r = await fetch(`${API}/api/session/${token}`);
        if (!r.ok) throw new Error('bad session');
        const data = await r.json();
        allEmojis = data.emojis || [];
        filteredEmojis = allEmojis.slice();
        session.categories = (data.categories || []).map((c, i) => ({
            id: 'c' + i,
            name: c.name,
            emojis: allEmojis.filter(e => e.category === c.name).map(e => e.name),
        }));
    } catch (_) {
        showEmpty('session expired — run !twemoji again');
        return;
    }

    renderAll();
    searchEl.addEventListener('input', () => {
        const q = searchEl.value.toLowerCase();
        filteredEmojis = q
            ? allEmojis.filter(e => e.name.toLowerCase().includes(q))
            : allEmojis.slice();
        renderGrid();
    });

    addCatBtn.addEventListener('click', addCategory);
    exportBtn.addEventListener('click', doExport);

    iconBtn.addEventListener('click', e => {
        e.stopPropagation();
        const visible = iconPicker.style.display !== 'none';
        if (visible) {
            iconPicker.style.display = 'none';
        } else {
            renderIconPicker();
            iconPicker.style.display = 'flex';
        }
    });
    document.addEventListener('click', e => {
        if (!iconPicker.contains(e.target) && e.target !== iconBtn) {
            iconPicker.style.display = 'none';
        }
    });

    document.addEventListener('mousemove', onMouseMove);
    document.addEventListener('mouseup', onMouseUp);
}

function renderAll() {
    renderGrid();
    renderCategories();
}

function renderIconPicker() {
    iconPickerGrid.innerHTML = '';
    allEmojis.forEach(emoji => {
        const btn = document.createElement('button');
        btn.title = emoji.name;
        const img = document.createElement('img');
        img.src = emoji.url;
        img.alt = emoji.name;
        btn.appendChild(img);
        btn.addEventListener('click', () => {
            packIcon = emoji.name;
            iconBtn.innerHTML = '';
            const previewImg = document.createElement('img');
            previewImg.src = emoji.url;
            previewImg.style.width = '24px';
            previewImg.style.height = '24px';
            iconBtn.appendChild(previewImg);
            iconBtn.classList.add('has-icon');
            iconPicker.style.display = 'none';
        });
        iconPickerGrid.appendChild(btn);
    });
}

function renderGrid() {
    gridEl.innerHTML = '';
    const inCategory = new Set(session.categories.flatMap(c => c.emojis));
    filteredEmojis
        .filter(emoji => !inCategory.has(emoji.name))
        .forEach(emoji => {
            const btn = document.createElement('button');
            btn.className = 'emoji-btn';
            btn.title = emoji.name;
            const img = document.createElement('img');
            img.src = emoji.url;
            img.alt = emoji.name;
            img.draggable = false;
            btn.appendChild(img);
            btn.addEventListener('mousedown', e => startDrag(e, emoji.name, 'sidebar', null));
            gridEl.appendChild(btn);
        });
}

function renderCategories() {
    const existing = areaEl.querySelectorAll('.category-box');
    existing.forEach(el => el.remove());

    session.categories.forEach(cat => {
        const box = makeCategoryBox(cat);
        areaEl.insertBefore(box, addCatBtn);
    });
}

function makeCategoryBox(cat) {
    const box = document.createElement('div');
    box.className = 'category-box sketch-card';
    box.dataset.catId = cat.id;

    const header = document.createElement('div');
    header.className = 'category-header';

    const nameInput = document.createElement('input');
    nameInput.className = 'category-name';
    nameInput.value = cat.name;
    nameInput.addEventListener('change', () => {
        cat.name = nameInput.value.trim() || cat.name;
    });

    const count = document.createElement('span');
    count.className = 'category-count';
    count.textContent = `${cat.emojis.length} emoji${cat.emojis.length !== 1 ? 's' : ''}`;

    const delBtn = document.createElement('button');
    delBtn.className = 'cat-delete-btn';
    delBtn.textContent = '✕';
    delBtn.addEventListener('click', () => {
        session.categories = session.categories.filter(c => c.id !== cat.id);
        box.remove();
    });

    header.append(nameInput, count, delBtn);

    const body = document.createElement('div');
    body.className = 'category-body';

    cat.emojis.forEach(char => {
        body.appendChild(makeCatEmoji(char, cat));
    });

    const hint = document.createElement('div');
    hint.className = 'category-drop-hint';
    hint.textContent = 'drop here!';

    box.append(header, body, hint);

    box.addEventListener('dragover', e => { e.preventDefault(); box.classList.add('drag-over'); });
    box.addEventListener('dragleave', e => {
        if (!box.contains(e.relatedTarget)) box.classList.remove('drag-over');
    });
    box.addEventListener('mouseenter', () => {
        if (dragPayload) box.classList.add('drag-over');
    });
    box.addEventListener('mouseleave', () => box.classList.remove('drag-over'));

    return box;
}

function makeCatEmoji(name, cat) {
    const el = document.createElement('div');
    el.className = 'cat-emoji';
    el.dataset.name = name;
    el.title = name;

    const emoji = allEmojis.find(e => e.name === name);
    if (emoji) {
        const img = document.createElement('img');
        img.src = emoji.url;
        img.alt = name;
        img.draggable = false;
        el.appendChild(img);
    }

    const rm = document.createElement('button');
    rm.className = 'remove-emoji';
    rm.textContent = '×';
    rm.addEventListener('mousedown', e => { e.stopPropagation(); e.preventDefault(); });
    rm.addEventListener('click', e => {
        e.stopPropagation();
        cat.emojis = cat.emojis.filter(n => n !== name);
        el.remove();
        updateCategoryCount(cat);
        save();
        renderGrid();
    });

    el.appendChild(rm);
    el.addEventListener('mousedown', e => startDrag(e, name, 'category', cat.id));
    return el;
}

function updateCategoryCount(cat) {
    const box = areaEl.querySelector(`[data-cat-id="${cat.id}"]`);
    if (!box) return;
    const count = box.querySelector('.category-count');
    if (count) count.textContent = `${cat.emojis.length} emoji${cat.emojis.length !== 1 ? 's' : ''}`;
}

function addCategory() {
    const id = 'c' + Date.now();
    const cat = { id, name: 'new category', emojis: [] };
    session.categories.push(cat);
    const box = makeCategoryBox(cat);
    areaEl.insertBefore(box, addCatBtn);
    const input = box.querySelector('.category-name');
    input.focus();
    input.select();
}

const ZOOM = 1.5;

function startDrag(e, name, sourceType, sourceCatId) {
    e.preventDefault();
    dragPayload = { name, sourceType, sourceCatId };

    const emoji = allEmojis.find(em => em.name === name);
    ghost.innerHTML = '';
    if (emoji) {
        const img = document.createElement('img');
        img.src = emoji.url;
        img.style.width = '36px';
        img.style.height = '36px';
        ghost.appendChild(img);
    }
    ghost.style.display = 'block';
    ghost.style.left = (e.clientX / ZOOM) + 'px';
    ghost.style.top = (e.clientY / ZOOM) + 'px';

    if (sourceType === 'category') {
        const cat = session.categories.find(c => c.id === sourceCatId);
        if (cat) {
            cat.emojis = cat.emojis.filter(n => n !== name);
            const box = areaEl.querySelector(`[data-cat-id="${sourceCatId}"]`);
            if (box) {
                const body = box.querySelector('.category-body');
                body.querySelectorAll(`.cat-emoji[data-name="${CSS.escape(name)}"]`).forEach(el => el.remove());
                updateCategoryCount(cat);
            }
        }
    }
}

function onMouseMove(e) {
    if (!dragPayload) return;
    ghost.style.left = (e.clientX / ZOOM) + 'px';
    ghost.style.top = (e.clientY / ZOOM) + 'px';
}

function onMouseUp(e) {
    if (!dragPayload) return;
    ghost.style.display = 'none';

    const { name } = dragPayload;
    dragPayload = null;

    const target = e.target.closest('.category-box');
    if (target) {
        target.classList.remove('drag-over');
        const catId = target.dataset.catId;
        const cat = session.categories.find(c => c.id === catId);
        if (cat && !cat.emojis.includes(name)) {
            cat.emojis.push(name);
            const body = target.querySelector('.category-body');
            body.appendChild(makeCatEmoji(name, cat));
            updateCategoryCount(cat);
            save();
            renderGrid();
        }
    } else {
        renderGrid();
    }

    document.querySelectorAll('.category-box.drag-over').forEach(b => b.classList.remove('drag-over'));
}

async function save() {
    if (!token) return;
    const flatCats = session.categories.map(c => ({ name: c.name }));
    const flatEmojis = allEmojis.map(e => {
        const cat = session.categories.find(c => c.emojis.includes(e.name));
        return { ...e, category: cat ? cat.name : null };
    });
    allEmojis = flatEmojis;
    await fetch(`${API}/api/session/${token}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ categories: flatCats, emojis: flatEmojis }),
    }).catch(() => {});
}

function showEmpty(msg) {
    document.body.innerHTML = `<div style="height:100vh;display:flex;align-items:center;justify-content:center;font-family:'Patrick Hand',cursive;font-size:24px;color:#7556c9;text-align:center;padding:20px">${msg}</div>`;
}

async function doExport() {
    if (!token) return;
    await save();
    const packName = packNameEl.value.trim() || 'emoji_pack';
    const formatEl = document.getElementById('pack-format');
    const version = formatEl ? formatEl.value : '1.21.1';
    const url = `${API}/api/export/${token}?name=${encodeURIComponent(packName)}&version=${encodeURIComponent(version)}`;
    const a = document.createElement('a');
    a.href = url;
    a.download = packName + '.zip';
    a.click();
}

init();
