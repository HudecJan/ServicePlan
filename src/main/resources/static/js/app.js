const API = '';

// State
let currentYear = new Date().getFullYear();
let currentMonth = new Date().getMonth() + 1;
let doctors = [];
let selectedDoctorId = null;
let currentSchedule = null;

const MONTHS = ['', 'Január', 'Február', 'Marec', 'Apríl', 'Máj', 'Jún',
    'Júl', 'August', 'September', 'Október', 'November', 'December'];
const DAYS = ['Po', 'Ut', 'St', 'Št', 'Pi', 'So', 'Ne'];

document.addEventListener('DOMContentLoaded', async () => {
    await loadDoctors();
    updateMonthLabel();
    renderPrefCalendar();
    loadAllPreferences();

    document.getElementById('prevMonth').addEventListener('click', () => changeMonth(-1));
    document.getElementById('nextMonth').addEventListener('click', () => changeMonth(1));
    document.getElementById('generateBtn').addEventListener('click', generateSchedule);
    document.getElementById('exportBtn').addEventListener('click', exportCSV);
    document.getElementById('publishBtn').addEventListener('click', publishSchedule);
    document.getElementById('closeModal').addEventListener('click', closeModal);
    document.getElementById('removePref').addEventListener('click', removePreference);
    document.getElementById('extraPlus').addEventListener('click', () => changeExtra(1));
    document.getElementById('extraMinus').addEventListener('click', () => changeExtra(-1));
    document.getElementById('addDoctorBtn').addEventListener('click', () => openDoctorModal(null));
    document.getElementById('saveDoctorBtn').addEventListener('click', saveDoctor);
    document.getElementById('closeDoctorModal').addEventListener('click', closeDoctorModal);
    document.getElementById('doctorModal').addEventListener('click', (e) => {
        if (e.target.id === 'doctorModal') closeDoctorModal();
    });
    document.getElementById('doctorSelect').addEventListener('change', (e) => {
        selectedDoctorId = parseInt(e.target.value);
        renderPrefCalendar();
        updateExtraButton();
    });

    document.querySelectorAll('.tab').forEach(tab => {
        tab.addEventListener('click', () => switchTab(tab.dataset.tab));
    });

    document.querySelectorAll('.pref-btn').forEach(btn => {
        btn.addEventListener('click', () => savePreference(btn.dataset.pref));
    });

    document.getElementById('prefModal').addEventListener('click', (e) => {
        if (e.target.id === 'prefModal') closeModal();
    });
});

// API
async function api(path, options = {}) {
    const res = await fetch(API + path, {
        headers: { 'Content-Type': 'application/json' },
        ...options
    });
    if (!res.ok) {
        const err = await res.json().catch(() => ({ message: res.statusText }));
        throw new Error(err.message || 'API Error');
    }
    if (res.status === 204) return null;
    return res.json();
}

// Doctors
async function loadDoctors() {
    doctors = await api('/api/doctors');
    const select = document.getElementById('doctorSelect');
    select.innerHTML = doctors.map(d =>
        `<option value="${d.id}">${d.firstName} ${d.lastName}</option>`
    ).join('');
    if (doctors.length > 0) {
        selectedDoctorId = doctors[0].id;
    }
    updateExtraButton();
}

function updateExtraButton() {
    const doc = doctors.find(d => d.id === selectedDoctorId);
    const countEl = document.getElementById('extraCount');
    const count = doc ? doc.extraShifts : 0;
    countEl.textContent = count > 0 ? `⭐ ${count} extra` : '0 extra';
    countEl.classList.toggle('has-extra', count > 0);
}

async function changeExtra(delta) {
    if (!selectedDoctorId) return;
    const doc = doctors.find(d => d.id === selectedDoctorId);
    if (!doc) return;
    const newCount = Math.max(0, doc.extraShifts + delta);
    try {
        const updated = await api(`/api/doctors/${selectedDoctorId}/extra-shifts`, {
            method: 'PUT',
            body: JSON.stringify({ extraShifts: newCount })
        });
        const idx = doctors.findIndex(d => d.id === selectedDoctorId);
        if (idx >= 0) doctors[idx] = updated;
        updateExtraButton();
        loadAllPreferences();
    } catch (e) {
        alert('Chyba: ' + e.message);
    }
}

// Month nav
function changeMonth(delta) {
    currentMonth += delta;
    if (currentMonth > 12) { currentMonth = 1; currentYear++; }
    if (currentMonth < 1) { currentMonth = 12; currentYear--; }
    updateMonthLabel();
    renderPrefCalendar();
    loadAllPreferences();
    if (document.getElementById('scheduleTab').classList.contains('active')) {
        loadSchedule();
    }
}

function updateMonthLabel() {
    document.getElementById('currentMonth').textContent = `${MONTHS[currentMonth]} ${currentYear}`;
}

function switchTab(tabName) {
    document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(t => t.classList.remove('active'));
    document.querySelector(`[data-tab="${tabName}"]`).classList.add('active');
    document.getElementById(`${tabName}Tab`).classList.add('active');
    if (tabName === 'schedule') loadSchedule();
    if (tabName === 'doctors') renderDoctorsList();
}

// ============ PREFERENCES ============

let currentPreferences = {};

async function renderPrefCalendar() {
    if (!selectedDoctorId) return;

    try {
        const prefs = await api(`/api/doctors/${selectedDoctorId}/preferences?year=${currentYear}&month=${currentMonth}`);
        currentPreferences = {};
        prefs.forEach(p => { currentPreferences[p.date] = p; });
    } catch (e) {
        currentPreferences = {};
    }

    const container = document.getElementById('prefCalendar');
    container.innerHTML = buildCalendarHTML(currentYear, currentMonth, (date, isWeekend) => {
        const pref = currentPreferences[date];
        let badges = '';
        if (pref) {
            const cls = pref.preferenceType === 'WANT_ON_CALL' ? 'want' : 'cannot';
            const label = pref.preferenceType === 'WANT_ON_CALL' ? '✅ Chcem' : '❌ Nemôžem';
            badges = `<div class="pref-badge ${cls}">${label}</div>`;
            if (pref.note) {
                badges += `<div style="font-size:0.7em;color:#718096;margin-top:2px">${pref.note}</div>`;
            }
        }
        return badges;
    }, (date) => openPrefModal(date));
}

let modalDate = null;

function openPrefModal(date) {
    modalDate = date;
    const d = new Date(date);
    document.getElementById('modalTitle').textContent =
        `${d.getDate()}. ${MONTHS[d.getMonth() + 1]} ${d.getFullYear()}`;

    const pref = currentPreferences[date];
    document.querySelectorAll('.pref-btn').forEach(btn => btn.classList.remove('selected'));
    document.getElementById('prefNote').value = '';
    document.getElementById('removePref').style.display = 'none';

    if (pref) {
        const activeBtn = document.querySelector(`.pref-btn[data-pref="${pref.preferenceType}"]`);
        if (activeBtn) activeBtn.classList.add('selected');
        document.getElementById('prefNote').value = pref.note || '';
        document.getElementById('removePref').style.display = 'inline-block';
    }

    document.getElementById('prefModal').style.display = 'flex';
}

function closeModal() {
    document.getElementById('prefModal').style.display = 'none';
    modalDate = null;
}

async function savePreference(prefType) {
    if (!modalDate || !selectedDoctorId) return;
    try {
        await api(`/api/doctors/${selectedDoctorId}/preferences`, {
            method: 'POST',
            body: JSON.stringify({
                date: modalDate,
                preferenceType: prefType,
                note: document.getElementById('prefNote').value || null
            })
        });
        closeModal();
        renderPrefCalendar();
        loadAllPreferences();
    } catch (e) {
        alert('Chyba: ' + e.message);
    }
}

async function removePreference() {
    if (!modalDate || !selectedDoctorId) return;
    const pref = currentPreferences[modalDate];
    if (!pref) return;
    try {
        await api(`/api/doctors/${selectedDoctorId}/preferences/${pref.id}`, { method: 'DELETE' });
        closeModal();
        renderPrefCalendar();
        loadAllPreferences();
    } catch (e) {
        alert('Chyba: ' + e.message);
    }
}

// All preferences overview
async function loadAllPreferences() {
    try {
        const allPrefs = await api(`/api/preferences?year=${currentYear}&month=${currentMonth}`);
        renderAllPrefsTable(allPrefs);
    } catch (e) {
        document.getElementById('allPrefsGrid').innerHTML = '<p style="color:#a0aec0">Žiadne preferencie</p>';
    }
}

function renderAllPrefsTable(prefs) {
    const daysInMonth = new Date(currentYear, currentMonth, 0).getDate();
    const container = document.getElementById('allPrefsGrid');

    const byDoctor = {};
    prefs.forEach(p => {
        if (!byDoctor[p.doctorId]) byDoctor[p.doctorId] = { name: p.doctorName, prefs: {} };
        byDoctor[p.doctorId].prefs[p.date] = p.preferenceType;
    });

    let html = '<table class="prefs-table"><thead><tr><th class="doctor-name">Lekár</th><th>Extra</th>';
    for (let d = 1; d <= daysInMonth; d++) {
        const date = new Date(currentYear, currentMonth - 1, d);
        const isWe = date.getDay() === 0 || date.getDay() === 6;
        html += `<th${isWe ? ' style="color:#e53e3e"' : ''}>${d}</th>`;
    }
    html += '</tr></thead><tbody>';

    doctors.forEach(doc => {
        const docPrefs = byDoctor[doc.id];
        const extraIcon = doc.extraShifts > 0 ? `⭐${doc.extraShifts}` : '';
        html += `<tr><td class="doctor-name">${doc.firstName} ${doc.lastName}</td>`;
        html += `<td style="text-align:center">${extraIcon}</td>`;
        for (let d = 1; d <= daysInMonth; d++) {
            const dateStr = `${currentYear}-${String(currentMonth).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
            const date = new Date(currentYear, currentMonth - 1, d);
            const isWe = date.getDay() === 0 || date.getDay() === 6;
            let cls = isWe ? 'weekend-cell' : '';
            let content = '';

            if (docPrefs && docPrefs.prefs[dateStr]) {
                const pt = docPrefs.prefs[dateStr];
                if (pt === 'WANT_ON_CALL') { cls = 'want-cell'; content = '✅'; }
                else if (pt === 'CANNOT_ON_CALL') { cls = 'cannot-cell'; content = '❌'; }
            }
            html += `<td class="${cls}">${content}</td>`;
        }
        html += '</tr>';
    });

    html += '</tbody></table>';
    container.innerHTML = html;
}

// ============ SCHEDULE ============

async function loadSchedule() {
    try {
        currentSchedule = await api(`/api/schedules?year=${currentYear}&month=${currentMonth}`);
        renderScheduleCalendar();
        showScheduleStatus();
        loadSummary();
    } catch (e) {
        currentSchedule = null;
        document.getElementById('schedCalendar').innerHTML =
            '<div style="text-align:center;padding:40px;color:#a0aec0;font-size:1.1em">Žiadny rozvrh pre tento mesiac. Kliknite na "Vygenerovať rozvrh".</div>';
        document.getElementById('scheduleStatus').style.display = 'none';
        document.getElementById('summarySection').style.display = 'none';
        document.getElementById('exportBtn').style.display = 'none';
        document.getElementById('publishBtn').style.display = 'none';
    }
}

async function generateSchedule() {
    const btn = document.getElementById('generateBtn');
    btn.classList.add('loading');
    btn.textContent = '⏳ Kontrolujem konflikty...';

    try {
        // Step 1: Check for conflicts
        const conflicts = await api(`/api/schedules/conflicts?year=${currentYear}&month=${currentMonth}`);

        if (conflicts && conflicts.length > 0) {
            btn.classList.remove('loading');
            btn.textContent = '⚡ Vygenerovať rozvrh';
            // Show conflict resolution modal
            showConflictModal(conflicts);
            return;
        }

        // No conflicts — generate directly
        await doGenerate({});
    } catch (e) {
        alert('Chyba pri generovaní: ' + e.message);
        btn.classList.remove('loading');
        btn.textContent = '⚡ Vygenerovať rozvrh';
    }
}

async function doGenerate(overrides) {
    const btn = document.getElementById('generateBtn');
    btn.classList.add('loading');
    btn.textContent = '⏳ Generujem...';

    try {
        currentSchedule = await api('/api/schedules/generate', {
            method: 'POST',
            body: JSON.stringify({ year: currentYear, month: currentMonth, overrides })
        });
        renderScheduleCalendar();
        showScheduleStatus();
        loadSummary();
    } catch (e) {
        alert('Chyba pri generovaní: ' + e.message);
    } finally {
        btn.classList.remove('loading');
        btn.textContent = '⚡ Vygenerovať rozvrh';
    }
}

function showConflictModal(conflicts) {
    const wantConflicts = conflicts.filter(c => c.type === 'WANT_CONFLICT');
    const shortages = conflicts.filter(c => c.type === 'SHORTAGE');

    let html = `
    <div id="conflictModal" class="modal" style="display:flex">
        <div class="modal-content" style="max-width:620px;max-height:85vh;overflow-y:auto;">
            <h3>⚠️ Konflikty pri generovaní rozvrhu</h3>`;

    if (wantConflicts.length > 0) {
        html += `<p style="color:#718096;margin-bottom:12px;font-weight:600;">Viacerí lekári chcú slúžiť v rovnaký deň — vyberte kto dostane prednosť:</p>`;

        wantConflicts.forEach((c, i) => {
            html += `
            <div class="conflict-item want-conflict">
                <div class="conflict-date">📋 ${new Date(c.date).getDate()}. ${MONTHS[new Date(c.date).getMonth() + 1]} (${c.dayName})${c.weekend ? ' 🏥' : ' 🌙'}</div>
                <div class="conflict-options">`;

            c.candidates.forEach(doc => {
                html += `
                    <label class="conflict-option">
                        <input type="radio" name="want_${i}" value="${doc.doctorId}" data-date="${c.date}">
                        <span class="conflict-doctor" style="border-color:${doc.color}">
                            <span class="conflict-dot" style="background:${doc.color}"></span>
                            ${doc.doctorName}
                            ${doc.note ? `<span class="conflict-note">(${doc.note})</span>` : ''}
                        </span>
                    </label>`;
            });
            html += `</div></div>`;
        });
    }

    if (shortages.length > 0) {
        html += `<p style="color:#e53e3e;margin:16px 0 12px;font-weight:600;">🚨 Nedostatok lekárov — vyberte koho priradiť nasilu:</p>`;

        shortages.forEach((c, i) => {
            const needed = c.slotsNeeded - c.availableCount;
            html += `
            <div class="conflict-item shortage-conflict">
                <div class="conflict-date">🚨 ${new Date(c.date).getDate()}. ${MONTHS[new Date(c.date).getMonth() + 1]} (${c.dayName})${c.weekend ? ' 🏥' : ' 🌙'}
                    <span style="font-size:0.8em;color:#e53e3e;font-weight:400"> — dostupných: ${c.availableCount}/${c.slotsNeeded}, treba vybrať ${needed}</span>
                </div>
                <div class="conflict-options">`;

            c.candidates.forEach(doc => {
                const cannotCls = doc.hasCannot ? ' cannot-doctor' : '';
                const inputType = needed > 1 ? 'checkbox' : 'radio';
                html += `
                    <label class="conflict-option">
                        <input type="${inputType}" name="shortage_${i}" value="${doc.doctorId}" data-date="${c.date}" data-type="shortage">
                        <span class="conflict-doctor${cannotCls}" style="border-color:${doc.color}">
                            <span class="conflict-dot" style="background:${doc.color}"></span>
                            ${doc.doctorName}
                            ${doc.hasCannot ? '<span class="conflict-cannot">❌ NEMÔŽE</span>' : '<span class="conflict-available">✅ môže</span>'}
                            ${doc.note ? `<span class="conflict-note">(${doc.note})</span>` : ''}
                        </span>
                    </label>`;
            });
            html += `</div></div>`;
        });
    }

    html += `
            <div class="modal-footer" style="margin-top:20px;">
                <button id="resolveConflictsBtn" class="btn btn-generate" style="padding:12px 32px;font-size:1em;">
                    ⚡ Vygenerovať s výberom
                </button>
                <button id="skipConflictsBtn" class="btn btn-secondary">
                    Nechať na algoritmus
                </button>
            </div>
        </div>
    </div>`;

    document.body.insertAdjacentHTML('beforeend', html);

    // Pre-select: for WANT conflicts select first, for shortages select available ones
    wantConflicts.forEach((c, i) => {
        const firstRadio = document.querySelector(`input[name="want_${i}"]`);
        if (firstRadio) firstRadio.checked = true;
    });
    shortages.forEach((c, i) => {
        // Pre-select available doctors (those without CANNOT)
        c.candidates.filter(d => !d.hasCannot).forEach(doc => {
            const input = document.querySelector(`input[name="shortage_${i}"][value="${doc.doctorId}"]`);
            if (input) input.checked = true;
        });
    });

    document.getElementById('resolveConflictsBtn').addEventListener('click', () => {
        const overrides = {};

        // Collect WANT conflict choices
        wantConflicts.forEach((c, i) => {
            const selected = document.querySelector(`input[name="want_${i}"]:checked`);
            if (selected) {
                overrides[selected.dataset.date] = parseInt(selected.value);
            }
        });

        // Collect SHORTAGE choices (first selected doctor becomes the forced one)
        shortages.forEach((c, i) => {
            const selected = document.querySelectorAll(`input[name="shortage_${i}"]:checked`);
            if (selected.length > 0) {
                // First selected is the on-call override
                overrides[selected[0].dataset.date] = parseInt(selected[0].value);
            }
        });

        document.getElementById('conflictModal').remove();
        doGenerate(overrides);
    });

    document.getElementById('skipConflictsBtn').addEventListener('click', () => {
        document.getElementById('conflictModal').remove();
        doGenerate({});
    });

    // Close on backdrop
    document.getElementById('conflictModal').addEventListener('click', (e) => {
        if (e.target.id === 'conflictModal') {
            document.getElementById('conflictModal').remove();
        }
    });
}

function renderDoctorLegend() {
    const container = document.getElementById('doctorLegend');
    container.innerHTML = doctors.map(d =>
        `<div class="doctor-legend-item">
            <div class="doctor-legend-dot" style="background:${d.color}"></div>
            ${d.firstName} ${d.lastName}
        </div>`
    ).join('');
}

function renderScheduleCalendar() {
    if (!currentSchedule) return;

    renderDoctorLegend();

    const assignmentsByDate = {};
    currentSchedule.assignments.forEach(a => {
        if (!assignmentsByDate[a.date]) assignmentsByDate[a.date] = [];
        assignmentsByDate[a.date].push(a);
    });

    const container = document.getElementById('schedCalendar');
    container.innerHTML = buildCalendarHTML(currentYear, currentMonth, (date, isWeekend) => {
        const dayAssignments = assignmentsByDate[date] || [];
        let html = '';
        let hasWarning = false;
        let warnings = [];

        dayAssignments.forEach(a => {
            let icon;
            switch (a.shiftType) {
                case 'ON_CALL_WEEKDAY_16H': icon = '🌙'; break;
                case 'ON_CALL_WEEKEND_24H': icon = '🏥'; break;
                case 'ASSISTANT_WEEKEND': icon = '☀️'; break;
                default: icon = '';
            }

            let forcedCls = '';
            if (a.forced) {
                forcedCls = ' forced';
                hasWarning = true;
                warnings.push(a.warning || 'Núdzové priradenie');
            }

            const color = a.doctorColor || '#667eea';
            const style = `background:${color}20; color:${color}; border-left:3px solid ${color};`;

            html += `<div class="shift-badge doctor-color${forcedCls}" style="${style}" title="${a.forced ? a.warning : a.shiftType}">${icon} ${a.doctorName}</div>`;
        });

        if (hasWarning) {
            html += `<div class="forced-warning" title="${warnings.join('\n')}">⚠️ ${warnings[0]}</div>`;
        }

        return html;
    }, null);
}

function showScheduleStatus() {
    if (!currentSchedule) return;
    const el = document.getElementById('scheduleStatus');
    el.style.display = 'block';

    if (currentSchedule.status === 'PUBLISHED') {
        el.className = 'schedule-status published';
        el.textContent = '✅ Rozvrh je publikovaný';
        document.getElementById('publishBtn').style.display = 'none';
    } else {
        el.className = 'schedule-status draft';
        el.textContent = '📝 Rozvrh je v stave DRAFT — môžete ho upraviť pred publikovaním';
        document.getElementById('publishBtn').style.display = 'inline-block';
    }
    document.getElementById('exportBtn').style.display = 'inline-block';
}

async function loadSummary() {
    if (!currentSchedule) return;
    try {
        const summary = await api(`/api/schedules/${currentSchedule.id}/summary`);
        renderSummary(summary);
    } catch (e) {
        console.error('Summary error:', e);
    }
}

function renderSummary(summary) {
    const section = document.getElementById('summarySection');
    section.style.display = 'block';

    let html = `<table class="summary-table">
        <thead><tr>
            <th>Lekár</th>
            <th>Extra</th>
            <th>Služby (prac. dni)</th>
            <th>Služby (víkendy)</th>
            <th>Príslužby (víkendy)</th>
            <th>Celkom služieb</th>
        </tr></thead><tbody>`;

    for (const [doctorId, count] of Object.entries(summary.doctorCounts)) {
        const doc = doctors.find(d => d.id === parseInt(doctorId));
        const extraIcon = doc && doc.extraShifts > 0 ? `⭐${doc.extraShifts}` : '';
        html += `<tr>
            <td style="text-align:left;font-weight:600">${count.doctorName}</td>
            <td>${extraIcon}</td>
            <td>${count.weekdayOnCall}</td>
            <td>${count.weekendOnCall}</td>
            <td>${count.weekendAssistant}</td>
            <td class="highlight">${count.totalOnCall}</td>
        </tr>`;
    }

    html += '</tbody></table>';

    if (summary.maxShifts - summary.minShifts > 2) {
        html += `<p style="color:#e53e3e;margin-top:10px;font-weight:600">
            ⚠️ Varovanie: Rozdiel medzi max (${summary.maxShifts}) a min (${summary.minShifts}) služieb je ${summary.maxShifts - summary.minShifts}</p>`;
    } else {
        html += `<p style="color:#48bb78;margin-top:10px;font-weight:600">
            ✅ Rozdelenie je spravodlivé (rozsah: ${summary.minShifts}–${summary.maxShifts} služieb)</p>`;
    }

    document.getElementById('summaryTable').innerHTML = html;
}

async function publishSchedule() {
    if (!currentSchedule) return;
    if (!confirm('Naozaj chcete publikovať rozvrh? Po publikovaní ho nebude možné upraviť.')) return;
    try {
        currentSchedule = await api(`/api/schedules/${currentSchedule.id}/publish`, { method: 'POST' });
        showScheduleStatus();
    } catch (e) {
        alert('Chyba: ' + e.message);
    }
}

// CSV Export
function exportCSV() {
    if (!currentSchedule) return;

    const daysInMonth = new Date(currentYear, currentMonth, 0).getDate();
    const assignmentsByDate = {};
    currentSchedule.assignments.forEach(a => {
        if (!assignmentsByDate[a.date]) assignmentsByDate[a.date] = [];
        assignmentsByDate[a.date].push(a);
    });

    let csv = 'Dátum;Deň;Typ služby;Lekár\n';

    for (let d = 1; d <= daysInMonth; d++) {
        const dateStr = `${currentYear}-${String(currentMonth).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
        const date = new Date(currentYear, currentMonth - 1, d);
        const dayName = ['Ne', 'Po', 'Ut', 'St', 'Št', 'Pi', 'So'][date.getDay()];
        const assignments = assignmentsByDate[dateStr] || [];

        if (assignments.length === 0) {
            csv += `${dateStr};${dayName};-;-\n`;
        } else {
            assignments.forEach(a => {
                let type;
                switch (a.shiftType) {
                    case 'ON_CALL_WEEKDAY_16H': type = 'Služba 16h'; break;
                    case 'ON_CALL_WEEKEND_24H': type = 'Služba 24h'; break;
                    case 'ASSISTANT_WEEKEND': type = 'Príslužba'; break;
                    default: type = a.shiftType;
                }
                csv += `${dateStr};${dayName};${type};${a.doctorName}\n`;
            });
        }
    }

    const BOM = '\uFEFF';
    const blob = new Blob([BOM + csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `sluzby_${currentYear}_${String(currentMonth).padStart(2, '0')}.csv`;
    link.click();
    URL.revokeObjectURL(url);
}

// ============ CALENDAR BUILDER ============

function buildCalendarHTML(year, month, contentBuilder, onClickHandler) {
    const firstDay = new Date(year, month - 1, 1);
    const daysInMonth = new Date(year, month, 0).getDate();
    let startDay = firstDay.getDay() - 1;
    if (startDay < 0) startDay = 6;

    const today = new Date();
    const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;

    let html = '<div class="calendar-grid">';
    DAYS.forEach(d => { html += `<div class="calendar-header">${d}</div>`; });

    for (let i = 0; i < startDay; i++) {
        html += '<div class="calendar-day empty"></div>';
    }

    for (let d = 1; d <= daysInMonth; d++) {
        const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
        const date = new Date(year, month - 1, d);
        const isWeekend = date.getDay() === 0 || date.getDay() === 6;
        const isToday = dateStr === todayStr;

        let classes = 'calendar-day';
        if (isWeekend) classes += ' weekend';
        if (isToday) classes += ' today';

        const content = contentBuilder(dateStr, isWeekend);

        html += `<div class="${classes}" data-date="${dateStr}">`;
        html += `<div class="day-number${isWeekend ? ' weekend-num' : ''}">${d}</div>`;
        html += content;
        html += '</div>';
    }

    const totalCells = startDay + daysInMonth;
    const remaining = totalCells % 7 === 0 ? 0 : 7 - (totalCells % 7);
    for (let i = 0; i < remaining; i++) {
        html += '<div class="calendar-day empty"></div>';
    }

    html += '</div>';

    if (onClickHandler) {
        setTimeout(() => {
            document.querySelectorAll('.calendar-day:not(.empty)').forEach(cell => {
                if (cell.closest('#prefCalendar')) {
                    cell.addEventListener('click', () => onClickHandler(cell.dataset.date));
                }
            });
        }, 0);
    }

    return html;
}

// ============ DOCTORS MANAGEMENT ============

let editingDoctorId = null;

function renderDoctorsList() {
    const container = document.getElementById('doctorsList');
    if (doctors.length === 0) {
        container.innerHTML = '<p style="color:#a0aec0;text-align:center;padding:20px">Žiadni lekári. Pridajte prvého lekára.</p>';
        return;
    }

    container.innerHTML = doctors.map(d => `
        <div class="doctor-card">
            <div class="doctor-color-bar" style="background:${d.color}"></div>
            <div class="doctor-info">
                <div class="name">${d.firstName} ${d.lastName}</div>
                <div class="email">${d.email}</div>
                <div class="badges">
                    ${d.extraShifts > 0 ? `<span class="doctor-badge extra">⭐ +${d.extraShifts} extra</span>` : ''}
                </div>
            </div>
            <div class="doctor-actions">
                <button class="btn btn-edit" onclick="openDoctorModal(${d.id})">✏️</button>
                <button class="btn btn-remove" onclick="removeDoctor(${d.id}, '${d.firstName} ${d.lastName}')">🗑</button>
            </div>
        </div>
    `).join('');
}

function openDoctorModal(doctorId) {
    editingDoctorId = doctorId;
    const modal = document.getElementById('doctorModal');

    if (doctorId) {
        const doc = doctors.find(d => d.id === doctorId);
        if (!doc) return;
        document.getElementById('doctorModalTitle').textContent = 'Upraviť lekára';
        document.getElementById('docFirstName').value = doc.firstName;
        document.getElementById('docLastName').value = doc.lastName;
        document.getElementById('docEmail').value = doc.email;
        document.getElementById('docColor').value = doc.color;
    } else {
        document.getElementById('doctorModalTitle').textContent = 'Nový lekár';
        document.getElementById('docFirstName').value = '';
        document.getElementById('docLastName').value = '';
        document.getElementById('docEmail').value = '';
        document.getElementById('docColor').value = '#667eea';
    }

    modal.style.display = 'flex';
}

function closeDoctorModal() {
    document.getElementById('doctorModal').style.display = 'none';
    editingDoctorId = null;
}

async function saveDoctor() {
    const firstName = document.getElementById('docFirstName').value.trim();
    const lastName = document.getElementById('docLastName').value.trim();
    const email = document.getElementById('docEmail').value.trim();
    const color = document.getElementById('docColor').value;

    if (!firstName || !lastName || !email) {
        alert('Vyplňte všetky povinné polia (meno, priezvisko, email).');
        return;
    }

    try {
        if (editingDoctorId) {
            await api(`/api/doctors/${editingDoctorId}`, {
                method: 'PUT',
                body: JSON.stringify({ firstName, lastName, email })
            });
            // Update color separately if changed
            const doc = doctors.find(d => d.id === editingDoctorId);
            if (doc && doc.color !== color) {
                await api(`/api/doctors/${editingDoctorId}/color`, {
                    method: 'PUT',
                    body: JSON.stringify({ color })
                });
            }
        } else {
            await api('/api/doctors', {
                method: 'POST',
                body: JSON.stringify({ firstName, lastName, email })
            });
        }
        closeDoctorModal();
        await loadDoctors();
        renderDoctorsList();
    } catch (e) {
        alert('Chyba: ' + e.message);
    }
}

async function removeDoctor(id, name) {
    if (!confirm(`Naozaj chcete deaktivovať lekára ${name}? Lekár nebude zahrnutý do budúcich rozvrhov.`)) return;
    try {
        await api(`/api/doctors/${id}`, { method: 'DELETE' });
        await loadDoctors();
        renderDoctorsList();
    } catch (e) {
        alert('Chyba: ' + e.message);
    }
}
