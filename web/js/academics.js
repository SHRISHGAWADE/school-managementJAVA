import { api } from './api.js';
import { showToast } from './students.js';

let years = [];
let academicClasses = [];

export function initAcademics() {
  const container = document.getElementById('view-academics');
  if (!container) return;
  container.innerHTML = `
    <header class="editorial-topbar"><div><p class="eyebrow">Academic Planning</p>
      <h2 class="hero-heading">Academic Years & Class Structure</h2>
      <p class="subheading">Define school year dates and the grade sections offered in each year.</p></div>
      <button id="academics-refresh" class="btn-refresh" type="button">Sync Data</button></header>
    <div id="academics-alert" class="finance-alert" role="status" style="display:none"></div>
    <div class="academics-forms">
      <section class="panel academics-form-panel academic-year-card"><h3 class="panel-title-editorial">Create Academic Year</h3>
        <p class="panel-subtitle">The end date must follow the start date.</p>
        <form id="academic-year-form" class="academic-form">
          <label>Year name<input class="form-input" name="name" required maxlength="40" placeholder="e.g. 2026-27" /></label>
          <label>Start date<input class="form-input" name="startDate" type="date" required /></label>
          <label>End date<input class="form-input" name="endDate" type="date" required /></label>
          <button class="btn btn-action-primary" type="submit">Save Year</button>
        </form>
      </section>
      <section class="panel academics-form-panel academic-class-card"><h3 class="panel-title-editorial">Add Class / Section</h3>
        <p class="panel-subtitle">Create the year before adding its class sections.</p>
        <form id="academic-class-form" class="academic-form">
          <label>Academic year<select class="form-input" name="academicYear" required></select></label>
          <label>Grade / class<input class="form-input" name="grade" required maxlength="40" placeholder="e.g. Grade 10" /></label>
          <label>Section<input class="form-input" name="section" required maxlength="20" placeholder="e.g. A" /></label>
          <button class="btn btn-action-primary" type="submit">Save Class</button>
        </form>
      </section>
    </div>
    <section class="panel finance-ledger-panel academics-register">
      <div class="panel-header-row"><div><h3 class="panel-title-editorial">Academic Years</h3>
        <p class="panel-subtitle">Saved school year date ranges.</p></div></div>
      <div id="academic-years-list"></div>
    </section>
    <section class="panel finance-ledger-panel academics-register">
      <div class="panel-header-row"><div><h3 class="panel-title-editorial">Class & Section Register</h3>
        <p class="panel-subtitle">Classes grouped by academic year.</p></div>
        <select id="academic-class-filter" class="filter-select" aria-label="Filter classes by academic year"></select></div>
      <div id="academic-classes-list"></div>
    </section>`;
  document.getElementById('academics-refresh').onclick = loadData;
  document.getElementById('academic-year-form').onsubmit = createYear;
  document.getElementById('academic-class-form').onsubmit = createClass;
  document.getElementById('academic-class-filter').onchange = renderClasses;
  loadData();
}

async function loadData() {
  try {
    [years, academicClasses] = await Promise.all([api.getAcademicYears(), api.getAcademicClasses()]);
    const orderedYears = [...years].sort((a, b) => a.startDate.localeCompare(b.startDate));
    const yearSelect = document.querySelector('#academic-class-form [name="academicYear"]');
    yearSelect.innerHTML = orderedYears.map(year => `<option value="${escapeHtml(year.name)}">${escapeHtml(year.name)}</option>`).join('')
      || '<option value="">Create an academic year first</option>';
    yearSelect.disabled = years.length === 0;
    const filter = document.getElementById('academic-class-filter');
    const previous = filter.value;
    filter.innerHTML = `<option value="">All years</option>${orderedYears.map(year => `<option value="${escapeHtml(year.name)}">${escapeHtml(year.name)}</option>`).join('')}`;
    if ([...filter.options].some(option => option.value === previous)) filter.value = previous;
    renderYears();
    renderClasses();
  } catch (error) { setAlert(error.message || 'Could not load academic data.', true); }
}

async function createYear(event) {
  event.preventDefault();
  const data = Object.fromEntries(new FormData(event.currentTarget));
  try {
    await api.createAcademicYear(data);
    event.currentTarget.reset();
    setAlert('Academic year saved.', false);
    showToast('Academic year added.');
    await loadData();
  } catch (error) { setAlert(error.message || 'Could not save academic year.', true); }
}

async function createClass(event) {
  event.preventDefault();
  const data = Object.fromEntries(new FormData(event.currentTarget));
  try {
    await api.createAcademicClass(data);
    event.currentTarget.reset();
    setAlert('Class section saved.', false);
    showToast('Class section added.');
    await loadData();
  } catch (error) { setAlert(error.message || 'Could not save class section.', true); }
}

function renderYears() {
  const container = document.getElementById('academic-years-list');
  if (!years.length) return container.innerHTML = '<div class="empty-state">No academic years have been created.</div>';
  container.innerHTML = `<div class="table-container"><table class="editorial-table"><thead><tr><th>Academic year</th><th>Starts</th><th>Ends</th></tr></thead><tbody>
    ${[...years].sort((a, b) => a.startDate.localeCompare(b.startDate)).map(year => `<tr><td><strong>${escapeHtml(year.name)}</strong></td><td>${escapeHtml(year.startDate)}</td><td>${escapeHtml(year.endDate)}</td></tr>`).join('')}
    </tbody></table></div>`;
}

function renderClasses() {
  const container = document.getElementById('academic-classes-list');
  if (!container) return;
  const year = document.getElementById('academic-class-filter').value;
  const rows = academicClasses.filter(item => !year || item.academicYear === year)
    .sort((a, b) => a.academicYear.localeCompare(b.academicYear) || a.grade.localeCompare(b.grade) || a.section.localeCompare(b.section));
  if (!rows.length) return container.innerHTML = '<div class="empty-state">No class sections match this selection.</div>';
  container.innerHTML = `<div class="table-container"><table class="editorial-table"><thead><tr><th>Academic year</th><th>Grade / class</th><th>Section</th></tr></thead><tbody>
    ${rows.map(item => `<tr><td>${escapeHtml(item.academicYear)}</td><td>${escapeHtml(item.grade)}</td><td>${escapeHtml(item.section)}</td></tr>`).join('')}
    </tbody></table></div>`;
}

function setAlert(message, isError) {
  const alert = document.getElementById('academics-alert');
  alert.textContent = message;
  alert.className = `finance-alert ${isError ? 'finance-alert-error' : 'finance-alert-success'}`;
  alert.style.display = 'block';
}

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' }[char]));
}
