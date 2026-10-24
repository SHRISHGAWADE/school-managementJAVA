/**
 * Student Management View Controller.
 * Handles interactive roster, real-time search, enrollment modal, contact updates,
 * soft-deactivation / reactivation, and CSV synchronization.
 */

import { api } from './api.js';

let allStudents = [];
let activeSearchQuery = '';
let activeStatusFilter = '';

export function initStudents() {
  const container = document.getElementById('view-students');
  if (!container) return;

  renderStudentsLayout(container);
  loadStudents();
}

function renderStudentsLayout(container) {
  container.innerHTML = `
    <!-- Top Editorial Header -->
    <div class="module-header-row">
      <div>
        <p class="eyebrow">Cohort Governance & Rosters</p>
        <h2 class="hero-heading">Student Directory</h2>
        <p class="subheading">Active enrollments, academic section placements, and tuition fee ledgers.</p>
      </div>
      <button id="open-enroll-modal-btn" class="btn btn-action-primary">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="btn-icon">
          <path stroke-linecap="round" stroke-linejoin="round" d="M12 4v16m8-8H4"/>
        </svg>
        <span>Enroll New Student</span>
      </button>
    </div>

    <!-- Toolbar: Search & Filter -->
    <div class="panel panel-toolbar">
      <div class="search-box">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="search-icon">
          <circle cx="11" cy="11" r="8"/>
          <line x1="21" y1="21" x2="16.65" y2="16.65"/>
        </svg>
        <input 
          type="text" 
          id="student-search-input" 
          placeholder="Search by student name or admission number..." 
          class="search-input"
          value="${escapeHtml(activeSearchQuery)}"
        />
        ${activeSearchQuery ? `<button id="clear-search-btn" class="clear-btn">&times;</button>` : ''}
      </div>

      <div class="filter-group">
        <label for="student-status-select" class="filter-label">Status:</label>
        <select id="student-status-select" class="filter-select">
          <option value="" ${activeStatusFilter === '' ? 'selected' : ''}>All Cohorts</option>
          <option value="ACTIVE" ${activeStatusFilter === 'ACTIVE' ? 'selected' : ''}>Active Only</option>
          <option value="INACTIVE" ${activeStatusFilter === 'INACTIVE' ? 'selected' : ''}>Inactive Only</option>
        </select>
      </div>

      <div class="toolbar-stats" id="student-count-badge">
        Loading count...
      </div>
    </div>

    <!-- Interactive Data Table Container -->
    <div class="panel panel-ledger" id="students-table-container">
      <div class="loading-state">
        <div class="spinner"></div>
        <p class="loading-text">Loading student records from verified CSV database...</p>
      </div>
    </div>

    <!-- Enroll Student Modal -->
    <div id="student-modal" class="modal-backdrop" style="display: none;">
      <div class="modal-dialog">
        <div class="modal-header">
          <h3 id="modal-title" class="modal-title">Enroll New Student</h3>
          <button id="close-modal-btn" class="modal-close">&times;</button>
        </div>
        <form id="student-form" class="modal-form">
          <input type="hidden" id="student-id-field" value="" />

          <div class="form-alert" id="student-form-alert" style="display: none;"></div>

          <div class="form-row">
            <div class="form-group">
              <label for="form-admission-num">Admission Number <span class="req">*</span></label>
              <input type="text" id="form-admission-num" class="form-input" placeholder="e.g. ADM004" required />
            </div>
            <div class="form-group">
              <label for="form-id">Student ID (Optional / Auto-generated)</label>
              <input type="number" id="form-id" class="form-input" placeholder="Auto" min="1" />
            </div>
          </div>

          <div class="form-group">
            <label for="form-name">Full Student Name <span class="req">*</span></label>
            <input type="text" id="form-name" class="form-input" placeholder="e.g. Priya Sharma" required />
          </div>

          <div class="form-row">
            <div class="form-group">
              <label for="form-email">Email Address</label>
              <input type="email" id="form-email" class="form-input" placeholder="student@example.com" />
            </div>
            <div class="form-group">
              <label for="form-phone">Phone Number</label>
              <input type="tel" id="form-phone" class="form-input" placeholder="e.g. 9876543210" />
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label for="form-grade">Grade / Class <span class="req">*</span></label>
              <input type="text" id="form-grade" class="form-input" placeholder="e.g. Grade 10" required />
            </div>
            <div class="form-group">
              <label for="form-section">Section <span class="req">*</span></label>
              <input type="text" id="form-section" class="form-input" placeholder="e.g. A" required />
            </div>
          </div>

          <div class="form-row" id="fee-fields-row">
            <div class="form-group">
              <label for="form-total-fees">Total Annual Fees (₹) <span class="req">*</span></label>
              <input type="number" id="form-total-fees" class="form-input" placeholder="e.g. 30000" min="0" step="100" required />
            </div>
            <div class="form-group">
              <label for="form-admission-date">Admission Date</label>
              <input type="date" id="form-admission-date" class="form-input" />
            </div>
          </div>

          <div class="modal-footer">
            <button type="button" id="cancel-modal-btn" class="btn btn-secondary">Cancel</button>
            <button type="submit" id="save-student-btn" class="btn btn-action-primary">Save Student</button>
          </div>
        </form>
      </div>
    </div>
  `;

  bindEvents();
}

function bindEvents() {
  const searchInput = document.getElementById('student-search-input');
  if (searchInput) {
    let debounceTimer;
    searchInput.addEventListener('input', (e) => {
      clearTimeout(debounceTimer);
      activeSearchQuery = e.target.value.trim();
      debounceTimer = setTimeout(() => {
        loadStudents();
      }, 250);
    });
  }

  const clearBtn = document.getElementById('clear-search-btn');
  if (clearBtn) {
    clearBtn.addEventListener('click', () => {
      activeSearchQuery = '';
      if (searchInput) searchInput.value = '';
      loadStudents();
    });
  }

  const statusSelect = document.getElementById('student-status-select');
  if (statusSelect) {
    statusSelect.addEventListener('change', (e) => {
      activeStatusFilter = e.target.value;
      loadStudents();
    });
  }

  const openModalBtn = document.getElementById('open-enroll-modal-btn');
  if (openModalBtn) {
    openModalBtn.addEventListener('click', () => openStudentModal());
  }

  const closeModalBtn = document.getElementById('close-modal-btn');
  const cancelModalBtn = document.getElementById('cancel-modal-btn');
  if (closeModalBtn) closeModalBtn.addEventListener('click', closeStudentModal);
  if (cancelModalBtn) cancelModalBtn.addEventListener('click', closeStudentModal);

  const studentForm = document.getElementById('student-form');
  if (studentForm) {
    studentForm.addEventListener('submit', handleFormSubmit);
  }
}

async function loadStudents() {
  const tableContainer = document.getElementById('students-table-container');
  const countBadge = document.getElementById('student-count-badge');
  if (!tableContainer) return;

  try {
    const students = await api.getStudents(activeSearchQuery, activeStatusFilter);
    allStudents = Array.isArray(students) ? students : [];

    if (countBadge) {
      const activeCount = allStudents.filter(s => s.status === 'ACTIVE').length;
      countBadge.innerHTML = `Showing <strong>${allStudents.length}</strong> students (<strong>${activeCount}</strong> Active)`;
    }

    renderTable(tableContainer, allStudents);
  } catch (err) {
    console.error('Error fetching students:', err);
    tableContainer.innerHTML = `
      <div class="empty-state">
        <p class="error-msg">Failed to retrieve student records: ${escapeHtml(err.message)}</p>
        <button id="retry-students-btn" class="btn btn-secondary" style="margin-top: 10px;">Retry</button>
      </div>
    `;
    const retryBtn = document.getElementById('retry-students-btn');
    if (retryBtn) retryBtn.onclick = () => loadStudents();
  }
}

function renderTable(container, students) {
  if (students.length === 0) {
    container.innerHTML = `
      <div class="empty-state">
        <p>No students found matching your criteria.</p>
        ${activeSearchQuery || activeStatusFilter ? `
          <button id="reset-filter-btn" class="btn btn-secondary" style="margin-top: 12px;">Reset Filters</button>
        ` : ''}
      </div>
    `;
    const resetBtn = document.getElementById('reset-filter-btn');
    if (resetBtn) {
      resetBtn.onclick = () => {
        activeSearchQuery = '';
        activeStatusFilter = '';
        const searchInput = document.getElementById('student-search-input');
        const statusSelect = document.getElementById('student-status-select');
        if (searchInput) searchInput.value = '';
        if (statusSelect) statusSelect.value = '';
        loadStudents();
      };
    }
    return;
  }

  container.innerHTML = `
    <div class="table-container">
      <table class="editorial-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Adm No</th>
            <th>Name</th>
            <th>Contact</th>
            <th>Class / Section</th>
            <th style="text-align: right;">Total Fee</th>
            <th style="text-align: right;">Paid</th>
            <th style="text-align: right;">Remaining</th>
            <th>Status</th>
            <th style="text-align: center;">Actions</th>
          </tr>
        </thead>
        <tbody>
          ${students.map(s => `
            <tr data-student-id="${s.id}">
              <td class="tx-id">#${s.id}</td>
              <td><strong>${escapeHtml(s.admissionNumber)}</strong></td>
              <td>
                <span class="entity-name">${escapeHtml(s.name)}</span>
              </td>
              <td>
                <div class="contact-block">
                  <span>${escapeHtml(s.email || '—')}</span>
                  <small>${escapeHtml(s.phone || '—')}</small>
                </div>
              </td>
              <td><span class="badge badge-class">${escapeHtml(s.grade)} (${escapeHtml(s.section)})</span></td>
              <td class="numeric-cell">₹${formatNum(s.totalFees)}</td>
              <td class="numeric-cell income-text">₹${formatNum(s.feesPaid)}</td>
              <td class="numeric-cell ${Number(s.remainingFees) > 0 ? 'expense-text' : ''}">₹${formatNum(s.remainingFees)}</td>
              <td>
                <span class="status-pill status-${String(s.status).toLowerCase()}">
                  ${escapeHtml(s.status)}
                </span>
              </td>
              <td style="text-align: center;">
                <div class="action-btn-group">
                  <button class="btn-action edit-student-btn" data-id="${s.id}" title="Edit Student Details">Edit</button>
                  ${s.status === 'ACTIVE' ? `
                    <button class="btn-action btn-danger deactivate-student-btn" data-id="${s.id}" title="Deactivate Student">Deactivate</button>
                  ` : `
                    <button class="btn-action btn-success activate-student-btn" data-id="${s.id}" title="Reactivate Student">Activate</button>
                  `}
                </div>
              </td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    </div>
  `;

  // Attach table action event listeners
  container.querySelectorAll('.edit-student-btn').forEach(btn => {
    btn.onclick = () => {
      const id = parseInt(btn.dataset.id, 10);
      const student = allStudents.find(s => s.id === id);
      if (student) openStudentModal(student);
    };
  });

  container.querySelectorAll('.deactivate-student-btn').forEach(btn => {
    btn.onclick = () => handleDeactivate(parseInt(btn.dataset.id, 10));
  });

  container.querySelectorAll('.activate-student-btn').forEach(btn => {
    btn.onclick = () => handleActivate(parseInt(btn.dataset.id, 10));
  });
}

function openStudentModal(student = null) {
  const modal = document.getElementById('student-modal');
  const title = document.getElementById('modal-title');
  const alert = document.getElementById('student-form-alert');
  const idField = document.getElementById('student-id-field');
  const admField = document.getElementById('form-admission-num');
  const idNumField = document.getElementById('form-id');
  const nameField = document.getElementById('form-name');
  const emailField = document.getElementById('form-email');
  const phoneField = document.getElementById('form-phone');
  const gradeField = document.getElementById('form-grade');
  const sectionField = document.getElementById('form-section');
  const totalFeesField = document.getElementById('form-total-fees');
  const dateField = document.getElementById('form-admission-date');
  const feeRow = document.getElementById('fee-fields-row');

  if (alert) alert.style.display = 'none';

  if (student) {
    title.textContent = `Edit Student: ${student.name} (#${student.id})`;
    idField.value = student.id;
    admField.value = student.admissionNumber;
    admField.disabled = true; // Admission number is fixed in domain
    if (idNumField) {
      idNumField.value = student.id;
      idNumField.disabled = true;
    }
    nameField.value = student.name;
    emailField.value = student.email || '';
    phoneField.value = student.phone || '';
    gradeField.value = student.grade || '';
    sectionField.value = student.section || '';
    totalFeesField.value = student.totalFees;
    dateField.value = student.admissionDate || '';
    dateField.disabled = true;
  } else {
    title.textContent = 'Enroll New Student';
    idField.value = '';
    admField.value = '';
    admField.disabled = false;
    if (idNumField) {
      idNumField.value = '';
      idNumField.disabled = false;
    }
    nameField.value = '';
    emailField.value = '';
    phoneField.value = '';
    gradeField.value = 'Grade 10';
    sectionField.value = 'A';
    totalFeesField.value = '30000';
    dateField.value = new Date().toISOString().split('T')[0];
    dateField.disabled = false;
  }

  modal.style.display = 'flex';
}

function closeStudentModal() {
  const modal = document.getElementById('student-modal');
  if (modal) modal.style.display = 'none';
}

async function handleFormSubmit(e) {
  e.preventDefault();
  const alert = document.getElementById('student-form-alert');
  const idField = document.getElementById('student-id-field');
  const isEdit = Boolean(idField.value);

  const payload = {
    name: document.getElementById('form-name').value.trim(),
    email: document.getElementById('form-email').value.trim(),
    phone: document.getElementById('form-phone').value.trim(),
    grade: document.getElementById('form-grade').value.trim(),
    section: document.getElementById('form-section').value.trim(),
    totalFees: parseFloat(document.getElementById('form-total-fees').value) || 0,
  };

  if (!isEdit) {
    payload.admissionNumber = document.getElementById('form-admission-num').value.trim();
    const customId = parseInt(document.getElementById('form-id').value, 10);
    if (!isNaN(customId) && customId > 0) {
      payload.id = customId;
    }
    const admDate = document.getElementById('form-admission-date').value;
    if (admDate) payload.admissionDate = admDate;
  }

  // Client Validation
  if (!payload.name) {
    showFormError(alert, 'Student name is required.');
    return;
  }
  if (!isEdit && !payload.admissionNumber) {
    showFormError(alert, 'Admission number is required.');
    return;
  }
  if (!payload.grade || !payload.section) {
    showFormError(alert, 'Grade and Section are required.');
    return;
  }
  if (payload.totalFees < 0) {
    showFormError(alert, 'Total fees cannot be negative.');
    return;
  }
  if (payload.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(payload.email)) {
    showFormError(alert, 'Invalid email format.');
    return;
  }

  const saveBtn = document.getElementById('save-student-btn');
  saveBtn.disabled = true;
  saveBtn.textContent = 'Saving...';

  try {
    if (isEdit) {
      await api.updateStudent(idField.value, payload);
      showToast(`Student #${idField.value} updated successfully!`);
    } else {
      const created = await api.enrollStudent(payload);
      showToast(`Student ${created.name} (#${created.id}) enrolled and saved to CSV!`);
    }
    closeStudentModal();
    loadStudents();
  } catch (err) {
    console.error('Save failed:', err);
    showFormError(alert, err.message || 'Server rejected the request.');
  } finally {
    saveBtn.disabled = false;
    saveBtn.textContent = 'Save Student';
  }
}

async function handleDeactivate(id) {
  const student = allStudents.find(s => s.id === id);
  const name = student ? student.name : `#${id}`;
  if (!confirm(`Are you sure you want to deactivate student ${name}? This soft-removes the student and persists to CSV.`)) {
    return;
  }

  try {
    await api.deactivateStudent(id);
    showToast(`Student ${name} has been deactivated.`, 'warning');
    loadStudents();
  } catch (err) {
    alert(`Failed to deactivate: ${err.message}`);
  }
}

async function handleActivate(id) {
  try {
    await api.activateStudent(id);
    showToast(`Student #${id} reactivated to ACTIVE status!`);
    loadStudents();
  } catch (err) {
    alert(`Failed to reactivate: ${err.message}`);
  }
}

function showFormError(el, msg) {
  if (!el) return;
  el.textContent = msg;
  el.style.display = 'block';
}

function formatNum(n) {
  const v = Number(n);
  return isNaN(v) ? '0.00' : v.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

export function showToast(message, type = 'success') {
  let toastContainer = document.getElementById('toast-container');
  if (!toastContainer) {
    toastContainer = document.createElement('div');
    toastContainer.id = 'toast-container';
    toastContainer.className = 'toast-container';
    document.body.appendChild(toastContainer);
  }

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `
    <span class="toast-dot"></span>
    <span class="toast-msg">${escapeHtml(message)}</span>
  `;
  toastContainer.appendChild(toast);

  setTimeout(() => {
    toast.classList.add('fade-out');
    setTimeout(() => toast.remove(), 300);
  }, 3500);
}

