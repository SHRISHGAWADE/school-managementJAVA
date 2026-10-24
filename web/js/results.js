import { api } from './api.js';
import { showToast } from './students.js';

let students = [];
let exams = [];
let subjects = [];
let results = [];

export function initResults() {
  const container = document.getElementById('view-results');
  if (!container) return;
  container.innerHTML = `
    <header class="editorial-topbar">
      <div><p class="eyebrow">Assessment & Academic Progress</p><h2 class="hero-heading">Exam Results & Grades</h2>
        <p class="subheading">Enter marks against each subject maximum and review saved student results.</p></div>
      <button id="results-refresh" class="btn-refresh" type="button">Sync Data</button>
    </header>
    <div id="results-alert" class="finance-alert" role="status" style="display:none"></div>
    <div class="results-setup-grid">
      <details class="panel results-setup"><summary>Add an exam</summary>
        <form id="exam-form" class="results-setup-form">
          <label>Exam name<input class="form-input" name="name" required maxlength="80" placeholder="e.g. Midterm Examination" /></label>
          <label>Exam date<input class="form-input" name="examDate" type="date" required /></label>
          <button class="btn btn-action-primary" type="submit">Save Exam</button>
        </form>
      </details>
      <details class="panel results-setup"><summary>Add a subject</summary>
        <form id="subject-form" class="results-setup-form">
          <label>Subject name<input class="form-input" name="name" required maxlength="80" placeholder="e.g. Mathematics" /></label>
          <label>Maximum marks<input class="form-input" name="maxMarks" type="number" min="1" step="1" required /></label>
          <button class="btn btn-action-primary" type="submit">Save Subject</button>
        </form>
      </details>
    </div>
    <section class="panel results-entry-panel">
      <div class="panel-header-row"><div><h3 class="panel-title-editorial">Enter or Update Marks</h3>
        <p class="panel-subtitle">Saving the same student, exam, and subject updates that result.</p></div></div>
      <form id="result-form" class="results-entry-form">
        <label>Student<select id="result-student" class="form-input" required></select></label>
        <label>Exam<select id="result-exam" class="form-input" required></select></label>
        <label>Subject<select id="result-subject" class="form-input" required></select></label>
        <label>Marks obtained<input id="result-marks" class="form-input" type="number" min="0" step="1" required /></label>
        <div class="results-max-hint" id="result-max-hint"></div>
        <button class="btn btn-action-primary" id="result-save-btn" type="submit">Save Result</button>
      </form>
    </section>
    <section class="panel finance-ledger-panel results-ledger-panel">
      <div class="panel-header-row"><div><h3 class="panel-title-editorial">Results Register</h3>
        <p class="panel-subtitle">Filter saved marks by exam, student, or subject.</p></div></div>
      <div class="results-filters">
        <label>Exam<select id="results-filter-exam" class="filter-select"><option value="">All exams</option></select></label>
        <label>Student<select id="results-filter-student" class="filter-select"><option value="">All students</option></select></label>
        <label>Subject<select id="results-filter-subject" class="filter-select"><option value="">All subjects</option></select></label>
      </div>
      <div id="results-table"><div class="loading-state">Loading results...</div></div>
    </section>`;
  document.getElementById('results-refresh').onclick = loadData;
  document.getElementById('exam-form').onsubmit = createExam;
  document.getElementById('subject-form').onsubmit = createSubject;
  document.getElementById('result-form').onsubmit = saveResult;
  ['result-student', 'result-exam', 'result-subject'].forEach(id => {
    document.getElementById(id).onchange = updateExistingMarks;
  });
  ['results-filter-exam', 'results-filter-student', 'results-filter-subject'].forEach(id => {
    document.getElementById(id).onchange = loadTable;
  });
  loadData();
}

async function loadData() {
  try {
    [students, exams, subjects, results] = await Promise.all([
      api.getStudents(), api.getExams(), api.getSubjects(), api.getResults(),
    ]);
    fillSelect('result-student', students, 'id', item => `${item.name} (#${item.id})`);
    fillSelect('result-exam', exams, 'id', item => `${item.name} — ${item.examDate}`);
    fillSelect('result-subject', subjects, 'id', item => `${item.name} (${item.maxMarks} marks)`);
    fillSelect('results-filter-exam', exams, 'id', item => item.name, 'All exams');
    fillSelect('results-filter-student', students, 'id', item => item.name, 'All students');
    fillSelect('results-filter-subject', subjects, 'id', item => item.name, 'All subjects');
    updateExistingMarks();
    renderTable();
    if (!exams.length || !subjects.length) {
      setAlert('Add at least one exam and one subject before entering results.', false);
    }
  } catch (error) {
    document.getElementById('results-table').innerHTML = `<div class="empty-state">Could not load results data: ${escapeHtml(error.message)}</div>`;
  }
}

function fillSelect(id, items, valueKey, label, placeholder = null) {
  const select = document.getElementById(id);
  const first = placeholder === null ? '' : `<option value="">${placeholder}</option>`;
  select.innerHTML = first + items.map(item => `<option value="${item[valueKey]}">${escapeHtml(label(item))}</option>`).join('');
  select.disabled = items.length === 0 && placeholder === null;
}

function updateExistingMarks() {
  const studentId = Number(document.getElementById('result-student').value);
  const examId = Number(document.getElementById('result-exam').value);
  const subjectId = Number(document.getElementById('result-subject').value);
  const subject = subjects.find(item => item.id === subjectId);
  document.getElementById('result-marks').max = subject?.maxMarks || '';
  document.getElementById('result-max-hint').textContent = subject ? `Maximum: ${subject.maxMarks} marks` : 'Select a subject';
  const existing = results.find(item => item.studentId === studentId && item.examId === examId && item.subjectId === subjectId);
  document.getElementById('result-marks').value = existing ? existing.marks : '';
  document.getElementById('result-save-btn').textContent = existing ? 'Update Result' : 'Save Result';
}

async function createExam(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const data = Object.fromEntries(new FormData(form));
  try {
    await api.createExam(data);
    form.reset();
    setAlert('Exam saved.', false);
    showToast('Exam added.');
    await loadData();
  } catch (error) { setAlert(error.message, true); }
}

async function createSubject(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const data = Object.fromEntries(new FormData(form));
  data.maxMarks = Number(data.maxMarks);
  try {
    await api.createSubject(data);
    form.reset();
    setAlert('Subject saved.', false);
    showToast('Subject added.');
    await loadData();
  } catch (error) { setAlert(error.message, true); }
}

async function saveResult(event) {
  event.preventDefault();
  const studentId = Number(document.getElementById('result-student').value);
  const examId = Number(document.getElementById('result-exam').value);
  const subjectId = Number(document.getElementById('result-subject').value);
  const marks = Number(document.getElementById('result-marks').value);
  const subject = subjects.find(item => item.id === subjectId);
  if (!studentId || !examId || !subject || !Number.isInteger(marks) || marks < 0 || marks > subject.maxMarks) {
    setAlert(`Enter whole-number marks from 0 to ${subject?.maxMarks ?? 'the subject maximum'}.`, true);
    return;
  }
  try {
    await api.saveResult({ studentId, examId, subjectId, marks });
    setAlert('Result saved successfully.', false);
    showToast('Exam result saved.');
    await loadData();
  } catch (error) { setAlert(error.message, true); }
}

async function loadTable() {
  const filters = {
    examId: document.getElementById('results-filter-exam').value,
    studentId: document.getElementById('results-filter-student').value,
    subjectId: document.getElementById('results-filter-subject').value,
  };
  try {
    results = await api.getResults(filters);
    renderTable();
  } catch (error) { setAlert(error.message, true); }
}

function renderTable() {
  const container = document.getElementById('results-table');
  if (!results.length) {
    container.innerHTML = '<div class="empty-state">No results match the selected filters.</div>';
    return;
  }
  container.innerHTML = `<div class="table-container"><table class="editorial-table"><thead><tr>
    <th>Student</th><th>Exam</th><th>Subject</th><th>Marks</th><th>Percentage</th><th>Grade</th>
    </tr></thead><tbody>${results.map(result => `<tr>
      <td><strong>${escapeHtml(result.studentName)}</strong> <span class="tx-id">#${result.studentId}</span></td>
      <td>${escapeHtml(result.examName)}<small class="result-subline">${escapeHtml(result.examDate)}</small></td>
      <td>${escapeHtml(result.subjectName)}</td>
      <td class="numeric-cell">${result.marks} / ${result.maxMarks}</td>
      <td>${Number(result.percentage).toFixed(1)}%</td>
      <td><span class="status-pill result-grade">${escapeHtml(result.grade)}</span></td>
    </tr>`).join('')}</tbody></table></div>`;
}

function setAlert(message, isError) {
  const alert = document.getElementById('results-alert');
  if (!alert) return;
  alert.textContent = message;
  alert.className = `finance-alert ${isError ? 'finance-alert-error' : 'finance-alert-success'}`;
  alert.style.display = 'block';
}

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' }[char]));
}
