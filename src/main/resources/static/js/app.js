// CarePlan — Elderly Medicine Tracker Frontend JavaScript

const API_BASE = '';

let currentPatientId = null;
let currentTab = 'today';

document.addEventListener('DOMContentLoaded', () => {
    initApp();
    startClock();
});

// Clock Ticker
function startClock() {
    const updateTime = () => {
        const now = new Date();
        document.getElementById('clockTime').textContent = now.toLocaleTimeString();
    };
    updateTime();
    setInterval(updateTime, 1000);
}

// Initial App Setup
async function initApp() {
    // Set default dates for missed history filter (last 30 days)
    const today = new Date().toISOString().split('T')[0];
    const past30 = new Date(Date.now() - 30 * 86400000).toISOString().split('T')[0];
    document.getElementById('missedStart').value = past30;
    document.getElementById('missedEnd').value = today;
    document.getElementById('todayDateStr').textContent = new Date().toLocaleDateString('en-US', {
        weekday: 'long', year: 'numeric', month: 'long', day: 'numeric'
    });

    await loadPatientsDropdown();
}

// Load Patients Dropdown
async function loadPatientsDropdown() {
    try {
        const res = await fetch(`${API_BASE}/patients`);
        const patients = await res.json();
        
        const select = document.getElementById('patientSelect');
        const schedSelect = document.getElementById('schedulePatientSelect');
        select.innerHTML = '';
        schedSelect.innerHTML = '';

        if (patients.length === 0) {
            select.innerHTML = '<option value="">No Patients Found (Click + Add Patient)</option>';
            showToast('No patients found. Please add a patient to start.', 'info');
            return;
        }

        patients.forEach(p => {
            select.innerHTML += `<option value="${p.id}">${p.name} (${p.age} yrs)</option>`;
            schedSelect.innerHTML += `<option value="${p.id}">${p.name} (${p.age} yrs)</option>`;
        });

        currentPatientId = patients[0].id;
        select.value = currentPatientId;

        // Load active view data
        await refreshAll();
        await loadMedicinesDropdown();
    } catch (err) {
        console.error('Error fetching patients:', err);
        showToast('Failed to connect to backend server', 'error');
    }
}

// Load Medicines Dropdown for Schedule Modal
async function loadMedicinesDropdown() {
    try {
        const res = await fetch(`${API_BASE}/medicines`);
        const medicines = await res.json();
        const select = document.getElementById('scheduleMedicineSelect');
        select.innerHTML = '';

        if (medicines.length === 0) {
            select.innerHTML = '<option value="">No Medicines Registered</option>';
            return;
        }

        medicines.forEach(m => {
            select.innerHTML += `<option value="${m.id}">${m.medicineName} (${m.dosage}) - ${m.frequency}</option>`;
        });
    } catch (err) {
        console.error('Error fetching medicines:', err);
    }
}

// Patient Selector Changed
async function onPatientChange() {
    const select = document.getElementById('patientSelect');
    currentPatientId = select.value;
    if (currentPatientId) {
        await refreshAll();
    }
}

// Global Refresh
async function refreshAll() {
    if (!currentPatientId) return;
    await Promise.all([
        loadDashboardStats(),
        loadTodayDoses(),
        loadOverdueAlerts()
    ]);
    if (currentTab === 'missed') loadMissedHistory();
    if (currentTab === 'patients') loadPatientsView();
    if (currentTab === 'medicines') loadMedicinesView();
    if (currentTab === 'schedules') loadSchedulesView();
}

// Load Dashboard Counts
async function loadDashboardStats() {
    try {
        const res = await fetch(`${API_BASE}/doselogs/dashboard?patientId=${currentPatientId}`);
        if (!res.ok) return;
        const data = await res.json();

        document.getElementById('statTotal').textContent = data.totalScheduled;
        document.getElementById('statTaken').textContent = data.takenCount;
        document.getElementById('statPending').textContent = data.pendingCount;
        document.getElementById('statOverdue').textContent = data.overdueCount;

        // Progress bar percentage
        const pct = data.totalScheduled > 0 ? Math.round((data.takenCount / data.totalScheduled) * 100) : 0;
        document.getElementById('takenProgressBar').style.width = `${pct}%`;

        // Update Overdue Badge
        const badgeCount = document.getElementById('overdueBadgeCount');
        const badge = document.getElementById('overdueBadge');
        const banner = document.getElementById('overdueBanner');

        badgeCount.textContent = `${data.overdueCount} Overdue`;

        if (data.overdueCount > 0) {
            badge.classList.remove('hidden');
            banner.classList.remove('hidden');
            document.getElementById('bannerMessage').textContent = `${data.overdueCount} dose(s) are overdue by more than 1 hour for ${data.patientName}!`;
        } else {
            banner.classList.add('hidden');
        }
    } catch (err) {
        console.error('Error fetching dashboard stats:', err);
    }
}

// Load Today's Expected Doses
async function loadTodayDoses() {
    try {
        const res = await fetch(`${API_BASE}/doselogs/today?patientId=${currentPatientId}`);
        if (!res.ok) return;
        const doses = await res.json();
        
        const container = document.getElementById('todayDosesGrid');
        container.innerHTML = '';

        if (doses.length === 0) {
            container.innerHTML = `
                <div class="empty-state" style="grid-column: 1/-1; text-align: center; padding: 3rem; color: var(--text-muted);">
                    <i class="fa-solid fa-calendar-xmark" style="font-size: 3rem; margin-bottom: 1rem; opacity: 0.5;"></i>
                    <h3>No Schedules Configured for Today</h3>
                    <p>Click <strong>+ Add Schedule</strong> to set up a daily medicine reminder.</p>
                </div>
            `;
            return;
        }

        doses.forEach(d => {
            const statusClass = getStatusClass(d.status);
            const isTaken = d.taken === true;
            
            container.innerHTML += `
                <div class="dose-card ${d.overdue ? 'overdue-border' : ''}">
                    <div class="dose-card-header">
                        <div class="dose-time-badge">
                            <i class="fa-regular fa-clock"></i> ${d.doseTime}
                        </div>
                        <span class="status-pill ${statusClass}">${d.status}</span>
                    </div>

                    <div class="dose-info">
                        <h3>${d.medicineName}</h3>
                        <div class="dose-meta">
                            <span><i class="fa-solid fa-prescription"></i> ${d.dosage}</span>
                            <span>•</span>
                            <span><i class="fa-solid fa-repeat"></i> ${d.frequency}</span>
                        </div>
                    </div>

                    <div class="dose-actions">
                        ${isTaken ? `
                            <button class="btn btn-sm btn-outline" style="width:100%; cursor: default; background-color: #ecfdf5; color: #10b981; border-color: #a7f3d0;">
                                <i class="fa-solid fa-check-circle"></i> Dose Taken
                            </button>
                        ` : `
                            <button class="btn btn-sm btn-success" style="flex:1" onclick="markDose(${d.scheduleId}, true)">
                                <i class="fa-solid fa-check"></i> Mark Taken
                            </button>
                            <button class="btn btn-sm btn-outline" style="flex:1; color: var(--danger);" onclick="markDose(${d.scheduleId}, false)">
                                <i class="fa-solid fa-xmark"></i> Mark Missed
                            </button>
                        `}
                    </div>
                </div>
            `;
        });
    } catch (err) {
        console.error('Error fetching today doses:', err);
    }
}

// Load Overdue Alerts (>1 Hour Late)
async function loadOverdueAlerts() {
    try {
        const res = await fetch(`${API_BASE}/doselogs/overdue?patientId=${currentPatientId}`);
        if (!res.ok) return;
        const alerts = await res.json();

        const container = document.getElementById('overdueAlertsGrid');
        container.innerHTML = '';

        if (alerts.length === 0) {
            container.innerHTML = `
                <div class="empty-state" style="grid-column: 1/-1; text-align: center; padding: 3rem; color: var(--text-muted);">
                    <i class="fa-solid fa-shield-heart" style="font-size: 3rem; color: var(--success); margin-bottom: 1rem;"></i>
                    <h3>No Overdue Doses!</h3>
                    <p>All scheduled medicine doses are up to date for this patient.</p>
                </div>
            `;
            return;
        }

        alerts.forEach(a => {
            container.innerHTML += `
                <div class="alert-card" style="border-left: 6px solid var(--danger);">
                    <div class="dose-card-header">
                        <div class="dose-time-badge" style="background-color: #fef2f2; color: var(--danger);">
                            <i class="fa-solid fa-triangle-exclamation"></i> Scheduled: ${a.doseTime}
                        </div>
                        <span class="status-pill status-overdue">OVERDUE BY ${a.minutesOverdue} MINS</span>
                    </div>

                    <div class="dose-info">
                        <h3 style="color: #991b1b">${a.medicineName} (${a.dosage})</h3>
                        <p style="font-size: 0.85rem; color: #7f1d1d; margin-top: 0.4rem;">${a.alertMessage}</p>
                    </div>

                    <div class="dose-actions">
                        <button class="btn btn-sm btn-danger" style="width: 100%" onclick="markDose(${a.scheduleId}, true)">
                            <i class="fa-solid fa-hand-holding-medical"></i> Give Dose Now & Mark Taken
                        </button>
                    </div>
                </div>
            `;
        });
    } catch (err) {
        console.error('Error fetching overdue alerts:', err);
    }
}

// Mark Dose as Taken or Missed
async function markDose(scheduleId, taken) {
    const today = new Date().toISOString().split('T')[0];
    const payload = {
        date: today,
        taken: taken,
        schedule: { id: scheduleId }
    };

    try {
        const res = await fetch(`${API_BASE}/doselogs`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const data = await res.json();

        if (res.ok) {
            showToast(taken ? 'Dose successfully marked as TAKEN!' : 'Dose logged as MISSED.', 'success');
            await refreshAll();
        } else {
            // Business Rule Violation (e.g. duplicate dose attempt for already TAKEN slot)
            showToast(data.message || 'Cannot log dose: Business rule restriction.', 'error');
        }
    } catch (err) {
        console.error('Error marking dose:', err);
        showToast('Network error while logging dose', 'error');
    }
}

// Load Missed History
async function loadMissedHistory() {
    const start = document.getElementById('missedStart').value;
    const end = document.getElementById('missedEnd').value;
    
    try {
        const res = await fetch(`${API_BASE}/doselogs/missed?patientId=${currentPatientId}&startDate=${start}&endDate=${end}`);
        if (!res.ok) return;
        const logs = await res.json();

        const tbody = document.getElementById('missedHistoryTbody');
        tbody.innerHTML = '';

        if (logs.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" style="text-align: center; color: var(--text-muted);">No missed doses recorded in selected date range.</td></tr>`;
            return;
        }

        logs.forEach(l => {
            tbody.innerHTML += `
                <tr>
                    <td><strong>${l.date}</strong></td>
                    <td>${l.schedule.medicine.medicineName}</td>
                    <td>${l.schedule.medicine.dosage}</td>
                    <td><span class="dose-time-badge">${l.schedule.doseTime}</span></td>
                    <td>${l.loggedAt ? new Date(l.loggedAt).toLocaleString() : '--'}</td>
                    <td>
                        <button class="btn btn-sm btn-success" onclick="markDose(${l.schedule.id}, true)">
                            Mark Taken Now
                        </button>
                    </td>
                </tr>
            `;
        });
    } catch (err) {
        console.error('Error fetching missed history:', err);
    }
}

// Load Patients View
async function loadPatientsView() {
    try {
        const res = await fetch(`${API_BASE}/patients`);
        const patients = await res.json();
        const container = document.getElementById('patientsGrid');
        container.innerHTML = '';

        patients.forEach(p => {
            container.innerHTML += `
                <div class="item-card">
                    <div style="display:flex; align-items:center; gap:1rem;">
                        <div style="width:48px; height:48px; border-radius:50%; background:var(--primary-light); color:var(--primary); display:flex; align-items:center; justify-content:center; font-size:1.3rem;">
                            <i class="fa-solid fa-user"></i>
                        </div>
                        <div>
                            <h3 style="font-size:1.1rem; font-weight:700;">${p.name}</h3>
                            <span style="font-size:0.85rem; color:var(--text-muted);">ID: #${p.id}</span>
                        </div>
                    </div>
                    <div style="font-size:0.9rem; color:var(--text-muted); display:flex; flex-direction:column; gap:0.4rem; margin-top:0.5rem;">
                        <span><i class="fa-solid fa-cake-candles"></i> Age: ${p.age} years old</span>
                        <span><i class="fa-solid fa-phone"></i> Contact: ${p.phone}</span>
                    </div>
                </div>
            `;
        });
    } catch (err) {
        console.error('Error loading patients view:', err);
    }
}

// Load Medicines View
async function loadMedicinesView() {
    try {
        const res = await fetch(`${API_BASE}/medicines`);
        const medicines = await res.json();
        const container = document.getElementById('medicinesGrid');
        container.innerHTML = '';

        medicines.forEach(m => {
            container.innerHTML += `
                <div class="item-card">
                    <div style="display:flex; align-items:center; gap:1rem;">
                        <div style="width:48px; height:48px; border-radius:12px; background:var(--secondary-light); color:var(--secondary); display:flex; align-items:center; justify-content:center; font-size:1.3rem;">
                            <i class="fa-solid fa-pills"></i>
                        </div>
                        <div>
                            <h3 style="font-size:1.1rem; font-weight:700;">${m.medicineName}</h3>
                            <span style="font-size:0.85rem; color:var(--text-muted);">ID: #${m.id}</span>
                        </div>
                    </div>
                    <div style="font-size:0.9rem; color:var(--text-muted); display:flex; flex-direction:column; gap:0.4rem; margin-top:0.5rem;">
                        <span><i class="fa-solid fa-prescription"></i> Dosage: <strong>${m.dosage}</strong></span>
                        <span><i class="fa-solid fa-rotate"></i> Frequency: ${m.frequency}</span>
                    </div>
                </div>
            `;
        });
    } catch (err) {
        console.error('Error loading medicines view:', err);
    }
}

// Load Master Schedules View
async function loadSchedulesView() {
    try {
        const res = await fetch(`${API_BASE}/schedules`);
        const schedules = await res.json();
        const container = document.getElementById('schedulesGrid');
        container.innerHTML = '';

        schedules.forEach(s => {
            container.innerHTML += `
                <div class="item-card">
                    <div style="display:flex; justify-content:space-between; align-items:center;">
                        <span class="dose-time-badge"><i class="fa-regular fa-clock"></i> ${s.doseTime}</span>
                        <span style="font-size:0.8rem; color:var(--text-muted);">Schedule #${s.id}</span>
                    </div>
                    <div style="margin-top:0.5rem;">
                        <h3 style="font-size:1.05rem; font-weight:700;">${s.medicine.medicineName} (${s.medicine.dosage})</h3>
                        <p style="font-size:0.85rem; color:var(--text-muted); margin-top:0.2rem;">Patient: <strong>${s.patient.name}</strong></p>
                    </div>
                </div>
            `;
        });
    } catch (err) {
        console.error('Error loading schedules view:', err);
    }
}

// Form Handlers
async function handlePatientSubmit(e) {
    e.preventDefault();
    const name = document.getElementById('patientName').value.trim();
    const age = parseInt(document.getElementById('patientAge').value);
    const phone = document.getElementById('patientPhone').value.trim();

    try {
        const res = await fetch(`${API_BASE}/patients`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, age, phone })
        });

        if (res.ok) {
            showToast('Patient created successfully!', 'success');
            closeModal('modalPatient');
            document.getElementById('formPatient').reset();
            await loadPatientsDropdown();
        } else {
            const err = await res.json();
            showToast(err.message || 'Failed to save patient', 'error');
        }
    } catch (err) {
        console.error('Error creating patient:', err);
    }
}

async function handleMedicineSubmit(e) {
    e.preventDefault();
    const medicineName = document.getElementById('medicineName').value.trim();
    const dosage = document.getElementById('medicineDosage').value.trim();
    const frequency = document.getElementById('medicineFrequency').value.trim();

    try {
        const res = await fetch(`${API_BASE}/medicines`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ medicineName, dosage, frequency })
        });

        if (res.ok) {
            showToast('Medicine added to inventory!', 'success');
            closeModal('modalMedicine');
            document.getElementById('formMedicine').reset();
            await loadMedicinesDropdown();
        } else {
            const err = await res.json();
            showToast(err.message || 'Failed to save medicine', 'error');
        }
    } catch (err) {
        console.error('Error creating medicine:', err);
    }
}

async function handleScheduleSubmit(e) {
    e.preventDefault();
    const patientId = document.getElementById('schedulePatientSelect').value;
    const medicineId = document.getElementById('scheduleMedicineSelect').value;
    const doseTime = document.getElementById('scheduleDoseTime').value;

    try {
        const res = await fetch(`${API_BASE}/schedules`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                doseTime: doseTime,
                patient: { id: parseInt(patientId) },
                medicine: { id: parseInt(medicineId) }
            })
        });

        if (res.ok) {
            showToast('Dose schedule created successfully!', 'success');
            closeModal('modalSchedule');
            document.getElementById('formSchedule').reset();
            await refreshAll();
        } else {
            const err = await res.json();
            showToast(err.message || 'Failed to create schedule', 'error');
        }
    } catch (err) {
        console.error('Error creating schedule:', err);
    }
}

// Tab Switching
function switchTab(tabName) {
    currentTab = tabName;
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    document.querySelectorAll('.tab-view').forEach(view => view.classList.add('hidden'));

    const activeBtn = document.getElementById(`tab-${tabName}`);
    const activeView = document.getElementById(`view-${tabName}`);

    if (activeBtn) activeBtn.classList.add('active');
    if (activeView) activeView.classList.remove('hidden');

    if (tabName === 'missed') loadMissedHistory();
    if (tabName === 'patients') loadPatientsView();
    if (tabName === 'medicines') loadMedicinesView();
    if (tabName === 'schedules') loadSchedulesView();
}

// Status Class Helper
function getStatusClass(status) {
    switch (status) {
        case 'TAKEN': return 'status-taken';
        case 'MISSED': return 'status-missed';
        case 'OVERDUE': return 'status-overdue';
        default: return 'status-pending';
    }
}

// Modal Helpers
function openModal(id) {
    document.getElementById(id).classList.remove('hidden');
}

function closeModal(id) {
    document.getElementById(id).classList.add('hidden');
}

// Toast Notifications
function showToast(message, type = 'info') {
    const container = document.getElementById('toastContainer');
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    
    let icon = 'fa-info-circle';
    if (type === 'success') icon = 'fa-circle-check';
    if (type === 'error') icon = 'fa-triangle-exclamation';

    toast.innerHTML = `<i class="fa-solid ${icon}"></i> <span>${message}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(10px)';
        toast.style.transition = 'all 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}
