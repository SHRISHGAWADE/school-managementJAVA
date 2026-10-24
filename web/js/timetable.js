import { api } from './api.js';
import { showToast } from './students.js';

const days = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
let entries = [];
let editingId = null;

export function initTimetable() {
  const container = document.getElementById('view-timetable');
  if (!container) return;
  editingId = null;
  container.innerHTML = `
    <header class="editorial-topbar"><div><p class="eyebrow">Academic Operations</p>
      <h2 class="hero-heading">Timetable Management</h2>
      <p class="subheading">Plan class periods and avoid teacher, class, and room overlaps.</p></div>
      <button id="timetable-refresh" class="btn-refresh" type="button">Sync Data</button></header>
    <div id="timetable-alert" class="finance-alert" role="status" style="display:none"></div>
    <section class="panel timetable-form-panel">
      <div class="panel-header-row"><div><h3 class="panel-title-editorial" id="timetable-form-title">Add Timetable Entry</h3>
        <p class="panel-subtitle">End time must be later than start time. Conflicts are checked before saving.</p></div></div>
      <form id="timetable-form" class="timetable-form">
        <label>Grade / class<input class="form-input" name="grade" required maxlength="40" placeholder="e.g. Grade 10" /></label>
        <label>Section<input class="form-input" name="section" required maxlength="20" placeholder="e.g. A" /></label>
        <label>Day<select class="form-input" name="day" required>${days.map(day => `<option value="${day}">${day[0] + day.slice(1).toLowerCase()}</option>`).join('')}</select></label>
        <label>Subject<select class="form-input" name="subjectId" required></select></label>
        <label>Teacher<select class="form-input" name="teacherId" required></select></label>
        <label>Start time<input class="form-input" name="startTime" type="time" required /></label>
        <label>End time<input class="form-input" name="endTime" type="time" required /></label>
        <label>Room<input class="form-input" name="room" maxlength="40" placeholder="Room / lab" /></label>
        <div class="timetable-form-actions"><button id="timetable-save" class="btn btn-action-primary" type="submit">Save Entry</button>
          <button id="timetable-cancel" class="btn btn-secondary" type="button" hidden>Cancel Edit</button></div>
      </form>
    </section>
    <section class="panel finance-ledger-panel timetable-register">
      <div class="panel-header-row"><div><h3 class="panel-title-editorial">Weekly Schedule</h3>
        <p class="panel-subtitle">Entries are organized by day and start time.</p></div>
        <label class="timetable-day-filter">Show day<select id="timetable-day-filter" class="filter-select"><option value="">All days</option>${days.map(day => `<option value="${day}">${day[0] + day.slice(1).toLowerCase()}</option>`).join('')}</select></label></div>
      <div id="timetable-list"><div class="loading-state">Loading timetable...</div></div>
    </section>`;
  document.getElementById('timetable-refresh').onclick = loadData;
  document.getElementById('timetable-form').onsubmit = saveEntry;
  document.getElementById('timetable-cancel').onclick = resetForm;
  document.getElementById('timetable-day-filter').onchange = renderSchedule;
  loadData();
}

async function loadData() {
  try {
    const [schedule, subjects, teachers] = await Promise.all([
      api.getTimetable(), api.getSubjects(), api.getTeachers('', 'true'),
    ]);
    entries = schedule;
    const form = document.getElementById('timetable-form');
    form.elements.subjectId.innerHTML = subjects.map(s => `<option value="${s.id}">${escapeHtml(s.name)}</option>`).join('');
    form.elements.teacherId.innerHTML = teachers.map(t => `<option value="${t.id}">${escapeHtml(t.name)}</option>`).join('');
    renderSchedule();
    if (!subjects.length || !teachers.length) {
      setAlert('Add subjects and active teachers before creating timetable entries.', true);
    }
  } catch (error) {
    document.getElementById('timetable-list').innerHTML = `<div class="empty-state">Could not load timetable: ${escapeHtml(error.message)}</div>`;
  }
}

async function saveEntry(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const data = Object.fromEntries(new FormData(form));
  data.subjectId = Number(data.subjectId);
  data.teacherId = Number(data.teacherId);
  const wasEditing = editingId !== null;
  try {
    if (editingId === null) await api.createTimetableEntry(data);
    else await api.updateTimetableEntry(editingId, data);
    resetForm();
    setAlert(wasEditing ? 'Timetable entry updated.' : 'Timetable entry saved.', false);
    showToast('Timetable saved.');
    await loadData();
  } catch (error) { setAlert(error.message || 'Unable to save timetable entry.', true); }
}

function renderSchedule() {
  const container = document.getElementById('timetable-list');
  if (!container) return;
  const filter = document.getElementById('timetable-day-filter').value;
  const visible = entries.map((entry, index) => ({ ...entry, id: entry.id ?? index }))
    .filter(entry => !filter || entry.day === filter)
    .sort((a, b) => days.indexOf(a.day) - days.indexOf(b.day) || a.startTime.localeCompare(b.startTime));
  if (!visible.length) {
    container.innerHTML = '<div class="empty-state">No timetable entries for this selection.</div>';
    return;
  }
  const groups = days.filter(day => visible.some(entry => entry.day === day));
  container.innerHTML = groups.map(day => `<section class="timetable-day-group"><h4>${day[0] + day.slice(1).toLowerCase()}</h4>
    <div class="table-container"><table class="editorial-table"><thead><tr><th>Time</th><th>Class</th><th>Subject</th><th>Teacher</th><th>Room</th><th>Actions</th></tr></thead><tbody>
    ${visible.filter(entry => entry.day === day).map(entry => `<tr><td>${escapeHtml(entry.startTime)}–${escapeHtml(entry.endTime)}</td>
      <td>${escapeHtml(entry.grade)}-${escapeHtml(entry.section)}</td><td>${escapeHtml(entry.subjectName)}</td>
      <td>${escapeHtml(entry.teacherName)}</td><td>${escapeHtml(entry.room || '—')}</td>
      <td><div class="action-btn-group"><button class="btn-action timetable-edit" data-id="${entry.id}">Edit</button><button class="btn-action btn-danger timetable-delete" data-id="${entry.id}">Delete</button></div></td></tr>`).join('')}
    </tbody></table></div></section>`).join('');
  container.querySelectorAll('.timetable-edit').forEach(button => button.onclick = () => editEntry(Number(button.dataset.id)));
  container.querySelectorAll('.timetable-delete').forEach(button => button.onclick = () => deleteEntry(Number(button.dataset.id)));
}

function editEntry(id) {
  const entry = entries.find((item, index) => (item.id ?? index) === id);
  if (!entry) return;
  editingId = id;
  const form = document.getElementById('timetable-form');
  ['grade', 'section', 'day', 'subjectId', 'teacherId', 'startTime', 'endTime', 'room']
    .forEach(key => form.elements[key].value = entry[key] ?? '');
  document.getElementById('timetable-form-title').textContent = 'Edit Timetable Entry';
  document.getElementById('timetable-save').textContent = 'Update Entry';
  document.getElementById('timetable-cancel').hidden = false;
  form.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

async function deleteEntry(id) {
  if (!confirm('Delete this timetable entry?')) return;
  try {
    await api.deleteTimetableEntry(id);
    resetForm();
    showToast('Timetable entry deleted.', 'warning');
    await loadData();
  } catch (error) { setAlert(error.message || 'Unable to delete timetable entry.', true); }
}

function resetForm() {
  editingId = null;
  const form = document.getElementById('timetable-form');
  if (!form) return;
  form.reset();
  document.getElementById('timetable-form-title').textContent = 'Add Timetable Entry';
  document.getElementById('timetable-save').textContent = 'Save Entry';
  document.getElementById('timetable-cancel').hidden = true;
}

function setAlert(message, isError) {
  const alert = document.getElementById('timetable-alert');
  alert.textContent = message;
  alert.className = `finance-alert ${isError ? 'finance-alert-error' : 'finance-alert-success'}`;
  alert.style.display = 'block';
}

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' }[char]));
}
