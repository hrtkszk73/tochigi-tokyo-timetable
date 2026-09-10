// 栃木⇔東京 時刻表 — PWA app logic
// State is stored in localStorage.

const TAB_KEYS = ['next-tokyo', 'next-tochigi', 'timetable'];
const DEFAULT_TAB = 'next-tokyo';

const DIRECTION_KEYS = {
  ToTokyo: 'toTokyo',
  ToTochigi: 'toTochigi',
  TobuToAsakusa: 'tobuToAsakusa',
  TobuToTochigi: 'tobuToTochigi',
};

const LS = {
  tab: 'tt.selectedTab',
  timetableDir: 'tt.timetableDirection',
  tokyoMin: 'tt.minutesToTokyoStation',
  tochigiMin: 'tt.minutesToTochigiStation',
};

const DEFAULTS = {
  minutesToTokyoStation: 30,
  minutesToTochigiStation: 10,
};

// --- State ---
const state = {
  timetable: null,
  selectedTab: localStorage.getItem(LS.tab) || DEFAULT_TAB,
  timetableDirection: localStorage.getItem(LS.timetableDir) || DIRECTION_KEYS.ToTokyo,
  settings: {
    minutesToTokyoStation: parseInt(localStorage.getItem(LS.tokyoMin) ?? DEFAULTS.minutesToTokyoStation, 10),
    minutesToTochigiStation: parseInt(localStorage.getItem(LS.tochigiMin) ?? DEFAULTS.minutesToTochigiStation, 10),
  },
  tickTimer: null,
};

// --- Utilities ---
const pad2 = (n) => String(n).padStart(2, '0');

function parseHM(s) {
  if (!s || typeof s !== 'string') return null;
  const m = s.match(/^(\d{1,2}):(\d{2})$/);
  if (!m) return null;
  return { h: parseInt(m[1], 10), m: parseInt(m[2], 10) };
}

function fmtHM(t) {
  if (!t) return '—';
  return `${pad2(t.h)}:${pad2(t.m)}`;
}

function hmToMinutes(t) {
  return t.h * 60 + t.m;
}

function nowLocalTime(date = new Date()) {
  return { h: date.getHours(), m: date.getMinutes(), s: date.getSeconds() };
}

function addMinutes(t, min) {
  const total = t.h * 60 + t.m + min;
  return { h: Math.floor(total / 60) % 24, m: total % 60 };
}

function isAfterOrEqual(a, b) {
  if (a.h !== b.h) return a.h > b.h;
  return a.m >= b.m;
}

function minutesUntil(target, from) {
  return (target.h * 60 + target.m) - (from.h * 60 + from.m);
}

// --- Data helpers ---
function directionData(key) {
  return state.timetable[key];
}

function trainDeparture(train) {
  return parseHM(train.stopTimes[0]);
}

function trainArrival(train) {
  return parseHM(train.stopTimes[train.stopTimes.length - 1]);
}

function trainIsFullJourney(train) {
  return train.stopTimes.every((t) => t !== null);
}

function minutesToBoardingStationFor(directionKey) {
  switch (directionKey) {
    case DIRECTION_KEYS.ToTokyo: return state.settings.minutesToTochigiStation; // 栃木駅から乗る
    case DIRECTION_KEYS.ToTochigi: return state.settings.minutesToTokyoStation; // 東京駅から乗る
    default: return 0;
  }
}

// --- Rendering ---
const $content = document.getElementById('content');
const $tabs = document.getElementById('tabs');
const $settingsBtn = document.getElementById('settings-btn');
const $settingsDialog = document.getElementById('settings-dialog');

function el(tag, opts = {}, children = []) {
  const n = document.createElement(tag);
  if (opts.class) n.className = opts.class;
  if (opts.text != null) n.textContent = opts.text;
  if (opts.html != null) n.innerHTML = opts.html;
  if (opts.attrs) for (const [k, v] of Object.entries(opts.attrs)) n.setAttribute(k, v);
  if (opts.on) for (const [ev, fn] of Object.entries(opts.on)) n.addEventListener(ev, fn);
  for (const c of children) if (c != null) n.appendChild(c);
  return n;
}

function renderTabs() {
  const buttons = $tabs.querySelectorAll('.tab');
  buttons.forEach((btn) => {
    const isSelected = btn.dataset.tab === state.selectedTab;
    btn.setAttribute('aria-selected', String(isSelected));
    btn.tabIndex = isSelected ? 0 : -1;
  });
}

function renderContent() {
  $content.innerHTML = '';
  $content.classList.remove('no-padding');
  const now = nowLocalTime();

  if (state.selectedTab === 'next-tokyo') {
    renderNext($content, DIRECTION_KEYS.ToTokyo, now);
  } else if (state.selectedTab === 'next-tochigi') {
    renderNext($content, DIRECTION_KEYS.ToTochigi, now);
  } else if (state.selectedTab === 'timetable') {
    $content.classList.add('no-padding');
    renderTimetable($content, now);
  }
}

function renderNext(root, directionKey, now) {
  const direction = directionData(directionKey);
  const offsetMinutes = minutesToBoardingStationFor(directionKey);
  const cutoff = addMinutes({ h: now.h, m: now.m }, offsetMinutes);

  const upcoming = direction.trains
    .filter((t) => trainIsFullJourney(t))
    .filter((t) => {
      const dep = trainDeparture(t);
      return isAfterOrEqual(dep, cutoff);
    })
    .sort((a, b) => hmToMinutes(trainDeparture(a)) - hmToMinutes(trainDeparture(b)));

  const nextTrain = upcoming[0] || null;
  const following = upcoming.slice(1, 4);

  // Clock card
  root.appendChild(el('div', { class: 'card card--variant clock-card' }, [
    el('div', { class: 'clock', text: `現在 ${pad2(now.h)}:${pad2(now.m)}:${pad2(now.s)}` }),
    el('div', { class: 'info', text: `${direction.stations[0]}まで${offsetMinutes}分 → 到着予想 ${pad2(cutoff.h)}:${pad2(cutoff.m)} 以降の便を表示` }),
  ]));

  // Next train card or empty card
  if (nextTrain) {
    root.appendChild(renderNextCard(nextTrain, direction, now));
  } else {
    root.appendChild(el('div', { class: 'card' }, [
      el('h3', { text: '本日の運行は終了しています' }),
      el('div', { class: 'no-train', text: '翌朝の便は明日になってから表示されます。' }),
    ]));
  }

  if (following.length > 0) {
    root.appendChild(el('div', { class: 'section-title', text: '以降の便' }));
    for (const t of following) {
      root.appendChild(el('div', { class: 'card' }, [
        renderTimeline(t, direction, false),
      ]));
    }
  }

  root.appendChild(renderNote());
}

function renderNextCard(train, direction, now) {
  const dep = trainDeparture(train);
  const minsUntil = minutesUntil(dep, now);
  let countdown;
  if (minsUntil <= 0) countdown = 'まもなく発車';
  else if (minsUntil < 60) countdown = `あと ${minsUntil} 分`;
  else countdown = `あと ${Math.floor(minsUntil / 60)} 時間 ${minsUntil % 60} 分`;

  return el('div', { class: 'card card--primary next-card' }, [
    el('div', { class: 'label', text: '次の便' }),
    el('div', { class: 'big-row' }, [
      el('div', { class: 'big-time', text: fmtHM(dep) }),
      el('div', { class: 'countdown', text: countdown }),
    ]),
    renderTimeline(train, direction, true),
  ]);
}

function renderTimeline(train, direction, emphasize) {
  const wrap = el('div', { class: 'timeline' });
  train.stopTimes.forEach((raw, idx) => {
    const t = parseHM(raw);
    const isEndpoint = idx === 0 || idx === train.stopTimes.length - 1;
    const row = el('div', { class: `timeline-row${isEndpoint ? ' endpoint' : ''}` }, [
      el('div', { class: 'timeline-dot' }),
      el('div', { class: 'timeline-label', text: direction.stations[idx] }),
      el('div', { class: 'timeline-time', text: fmtHM(t) }),
    ]);
    wrap.appendChild(row);
    if (idx < train.stopTimes.length - 1 && idx < direction.segmentLabels.length) {
      wrap.appendChild(el('div', { class: 'timeline-segment' }, [
        el('div', { class: 'segment-bar' }),
        el('div', { class: 'segment-label', text: direction.segmentLabels[idx] }),
      ]));
    }
  });
  return wrap;
}

function renderNote() {
  const notes = state.timetable.notes;
  const validFrom = state.timetable.validFrom;
  return el('div', { class: 'card card--variant note-card' }, [
    el('h3', { text: `データについて (${validFrom} 時点)` }),
    el('ul', {}, notes.map((n) => el('li', { text: n }))),
  ]);
}

function renderTimetable(root, now) {
  const direction = directionData(state.timetableDirection);

  // Direction selector
  const selector = el('div', { class: 'direction-selector' });
  selector.appendChild(dirRow('新幹線', [
    [DIRECTION_KEYS.ToTokyo, '栃木→東京'],
    [DIRECTION_KEYS.ToTochigi, '東京→栃木'],
  ]));
  selector.appendChild(dirRow('特急', [
    [DIRECTION_KEYS.TobuToAsakusa, '栃木→浅草'],
    [DIRECTION_KEYS.TobuToTochigi, '浅草→栃木'],
  ]));
  root.appendChild(selector);

  // Header
  const table = el('div', { class: 'timetable-wrap' });
  const header = el('div', { class: 'timetable-header' });
  direction.stations.forEach((s) => header.appendChild(el('div', { text: s })));
  table.appendChild(header);

  // Compute highlight index (first departure >= now)
  const nowT = { h: now.h, m: now.m };
  const highlightIdx = direction.trains.findIndex((t) => {
    const dep = trainDeparture(t);
    return isAfterOrEqual(dep, nowT);
  });

  direction.trains.forEach((t, idx) => {
    const row = el('div', { class: `timetable-row${idx === highlightIdx ? ' highlight' : ''}` });
    t.stopTimes.forEach((raw) => {
      row.appendChild(el('div', { text: fmtHM(parseHM(raw)) }));
    });
    row.dataset.trainIndex = String(idx);
    table.appendChild(row);
  });
  root.appendChild(table);

  // Scroll highlighted row into view — 'start' + scroll-margin-top on the row
  // leaves the sticky header stack visible above.
  if (highlightIdx >= 0) {
    requestAnimationFrame(() => {
      const target = table.querySelector(`.timetable-row[data-train-index="${highlightIdx}"]`);
      if (target) target.scrollIntoView({ block: 'start', behavior: 'auto' });
    });
  }

  // Note card
  const noteWrap = el('div', { attrs: { style: 'padding: 16px;' } }, [renderNote()]);
  root.appendChild(noteWrap);
}

function dirRow(label, opts) {
  const seg = el('div', { class: 'segmented' });
  opts.forEach(([key, name]) => {
    const pressed = state.timetableDirection === key;
    seg.appendChild(el('button', {
      attrs: { type: 'button', 'aria-pressed': String(pressed) },
      text: name,
      on: { click: () => selectTimetableDirection(key) },
    }));
  });
  return el('div', { class: 'dir-row' }, [
    el('div', { class: 'dir-label', text: label }),
    seg,
  ]);
}

// --- Actions ---
function selectTab(tab) {
  if (!TAB_KEYS.includes(tab)) return;
  state.selectedTab = tab;
  localStorage.setItem(LS.tab, tab);
  renderTabs();
  renderContent();
}

function selectTimetableDirection(key) {
  state.timetableDirection = key;
  localStorage.setItem(LS.timetableDir, key);
  renderContent();
}

function openSettings() {
  document.getElementById('tokyo-minutes').value = state.settings.minutesToTokyoStation;
  document.getElementById('tochigi-minutes').value = state.settings.minutesToTochigiStation;
  document.getElementById('tokyo-minutes-value').textContent = state.settings.minutesToTokyoStation;
  document.getElementById('tochigi-minutes-value').textContent = state.settings.minutesToTochigiStation;
  $settingsDialog.showModal();
}

function saveSettings() {
  const tokyo = parseInt(document.getElementById('tokyo-minutes').value, 10);
  const tochigi = parseInt(document.getElementById('tochigi-minutes').value, 10);
  state.settings.minutesToTokyoStation = Math.max(0, Math.min(180, tokyo));
  state.settings.minutesToTochigiStation = Math.max(0, Math.min(180, tochigi));
  localStorage.setItem(LS.tokyoMin, String(state.settings.minutesToTokyoStation));
  localStorage.setItem(LS.tochigiMin, String(state.settings.minutesToTochigiStation));
  renderContent();
}

// --- Init ---
async function init() {
  $content.innerHTML = '<div class="loading">読み込み中…</div>';
  try {
    const res = await fetch('./timetable.json', { cache: 'no-cache' });
    if (!res.ok) throw new Error('failed to fetch timetable.json');
    state.timetable = await res.json();
  } catch (e) {
    $content.innerHTML = `<div class="loading">時刻表データの読み込みに失敗しました。<br>${e.message}</div>`;
    return;
  }

  // Tabs
  $tabs.querySelectorAll('.tab').forEach((btn) => {
    btn.addEventListener('click', () => selectTab(btn.dataset.tab));
  });

  // Settings
  $settingsBtn.addEventListener('click', openSettings);
  document.getElementById('settings-cancel').addEventListener('click', () => $settingsDialog.close('cancel'));
  document.getElementById('tokyo-minutes').addEventListener('input', (e) => {
    document.getElementById('tokyo-minutes-value').textContent = e.target.value;
  });
  document.getElementById('tochigi-minutes').addEventListener('input', (e) => {
    document.getElementById('tochigi-minutes-value').textContent = e.target.value;
  });
  $settingsDialog.addEventListener('close', () => {
    if ($settingsDialog.returnValue !== 'cancel') saveSettings();
  });

  renderTabs();
  renderContent();

  // Refresh every 15 seconds so the clock and "あと N 分" stay fresh.
  state.tickTimer = setInterval(renderContent, 15_000);

  // Also refresh when returning to the tab.
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') renderContent();
  });
}

init();
