import { api } from './api.js';
import { showToast } from './students.js';

export function initAttendance() {
  const container = document.getElementById('view-attendance');
  if (!container) return;
  container.innerHTML = `
    <header class="editorial-topbar">
      <div><p class="eyebrow">Daily Roll Call</p><h2 class="hero-heading">Student Attendance</h2>
        <p class="subheading">Record and review attendance by date. Records are saved with the school data.</p></div>
      <div class="attendance-date-control"><label for="attendance-date">Attendance date</label>
        <input id="attendance-date" class="form-input" type="date" value="${localToday()}" max="${localToday()}" /></div>
    </header>
    <div id="attendance-alert" class="finance-alert" role="status" style="display:none"></div>
    <section id="attendance-summary" class="attendance-summary"></section>
    <section class="panel finance-ledger-panel">
      <div class="panel-header-row"><div><h3 class="panel-title-editorial">Class Register</h3>
        <p class="panel-subtitle">Daily status and each student’s recorded attendance rate.</p></div></div>
      <div id="attendance-roster"><div class="loading-state">Loading class register...</div></div>
    </section>`;
  document.getElementById('attendance-date').onchange = loadAttendance;
  loadAttendance();
}

async function loadAttendance() {
  const date = document.getElementById('attendance-date').value;
  const roster = document.getElementById('attendance-roster');
  if (!date) return;
  roster.innerHTML = '<div class="loading-state">Loading class register...</div>';
  try {
    const students = await api.getAttendance(date);
    renderAttendance(students, date);
  } catch (error) {
    roster.innerHTML = `<div class="empty-state">Could not load attendance: ${escapeHtml(error.message)}</div>`;
  }
}

function renderAttendance(students, date) {
  const roster = document.getElementById('attendance-roster');
  const summary = document.getElementById('attendance-summary');
  const counts = { PRESENT: 0, ABSENT: 0, LATE: 0, EXCUSED: 0, UNRECORDED: 0 };
  students.forEach(student => counts[student.status]++);
  const recorded = students.length - counts.UNRECORDED;
  summary.innerHTML = `
    <article class="panel attendance-count"><span>Present</span><strong>${counts.PRESENT}</strong></article>
    <article class="panel attendance-count"><span>Absent</span><strong>${counts.ABSENT}</strong></article>
    <article class="panel attendance-count"><span>Late / Excused</span><strong>${counts.LATE + counts.EXCUSED}</strong></article>
    <article class="panel attendance-count"><span>Not recorded</span><strong>${counts.UNRECORDED}</strong></article>`;
  if (!students.length) {
    roster.innerHTML = '<div class="empty-state">No active students are available for roll call.</div>';
    return;
  }
  roster.innerHTML = `<div class="attendance-progress">Recorded ${recorded} of ${students.length} active students</div>
    <div class="table-container"><table class="editorial-table"><thead><tr><th>Student</th><th>Class</th><th>Attendance rate</th><th>Status for ${escapeHtml(date)}</th><th>Action</th></tr></thead><tbody>
    ${students.map(student => {
      const done = student.status !== 'UNRECORDED';
      return `<tr><td><strong>${escapeHtml(student.studentName)}</strong> <span class="tx-id">#${student.studentId}</span></td>
        <td>${escapeHtml(student.grade)}-${escapeHtml(student.section)}</td>
        <td>${Number(student.attendancePercentage).toFixed(1)}%</td>
        <td>${done ? `<span class="status-pill status-${student.status.toLowerCase()}">${student.status}</span>` : `<div class="attendance-picker">
          <button type="button" class="attendance-picker-trigger" aria-expanded="false" aria-label="Choose attendance for ${escapeHtml(student.studentName)}">Present</button>
          <div class="attendance-picker-menu" role="listbox" hidden>
            ${[['PRESENT','Present'],['ABSENT','Absent'],['LATE','Late'],['EXCUSED','Excused']].map(([value,label]) => `<button type="button" class="attendance-option" role="option" aria-selected="${value === 'PRESENT'}" data-value="${value}">${label}</button>`).join('')}
          </div>
          <select class="attendance-status attendance-status-native" tabindex="-1" aria-hidden="true"><option value="PRESENT" selected>Present</option><option value="ABSENT">Absent</option><option value="LATE">Late</option><option value="EXCUSED">Excused</option></select>
        </div>`}</td>
        <td>${done ? '<span class="attendance-saved">Recorded</span>' : `<button class="btn-action btn-action-primary attendance-save" data-student-id="${student.studentId}">Save</button>`}</td></tr>`;
    }).join('')}</tbody></table></div>`;
  roster.querySelectorAll('.attendance-save').forEach(button => {
    button.onclick = () => saveAttendance(button, date);
  });
  roster.querySelectorAll('.attendance-picker').forEach(picker => {
    const trigger = picker.querySelector('.attendance-picker-trigger');
    const menu = picker.querySelector('.attendance-picker-menu');
    const select = picker.querySelector('.attendance-status');
    trigger.onclick = () => {
      menu.hidden = !menu.hidden;
      trigger.setAttribute('aria-expanded', String(!menu.hidden));
    };
    picker.querySelectorAll('.attendance-option').forEach(option => {
      option.onclick = () => {
        select.value = option.dataset.value;
        trigger.textContent = option.textContent;
        menu.hidden = true;
        trigger.setAttribute('aria-expanded', 'false');
        picker.querySelectorAll('.attendance-option').forEach(item => item.setAttribute('aria-selected', String(item === option)));
      };
    });
  });
}

async function saveAttendance(button, date) {
  const row = button.closest('tr');
  const status = row.querySelector('.attendance-status').value;
  button.disabled = true;
  try {
    await api.markAttendance({ studentId: Number(button.dataset.studentId), date, status });
    showToast(`Attendance marked ${status.toLowerCase()}.`);
    await loadAttendance();
  } catch (error) {
    const alert = document.getElementById('attendance-alert');
    alert.textContent = error.message || 'Unable to save attendance.';
    alert.className = 'finance-alert finance-alert-error';
    alert.style.display = 'block';
    button.disabled = false;
  }
}

function localToday() {
  const now = new Date();
  now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
  return now.toISOString().slice(0, 10);
}

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' }[char]));
}
