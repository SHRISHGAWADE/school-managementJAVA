/**
 * Teacher Management View Controller.
 * Handles faculty roster, real-time search, hiring modal, credential updates,
 * soft-deactivation / reactivation, and CSV synchronization.
 */

import { api } from './api.js';
import { showToast } from './students.js';

let allTeachers = [];
let activeSearchQuery = '';
let activeStatusFilter = '';

export function initTeachers() {
  const container = document.getElementById('view-teachers');
  if (!container) return;

  renderTeachersLayout(container);
  loadTeachers();
}

function renderTeachersLayout(container) {
  container.innerHTML = `
    <!-- Top Editorial Header -->
    <div class="module-header-row">
      <div>
        <p class="eyebrow">Academic Faculty & Appointments</p>
        <h2 class="hero-heading">Faculty Directory</h2>
        <p class="subheading">Active faculty appointments, department assignments, and monthly payroll records.</p>
      </div>
      <button id="open-hire-modal-btn" class="btn btn-action-primary">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="btn-icon">
          <path stroke-linecap="round" stroke-linejoin="round" d="M12 4v16m8-8H4"/>
        </svg>
        <span>Hire New Teacher</span>
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
          id="teacher-search-input" 
          placeholder="Search by teacher name or department..." 
          class="search-input"
          value="${escapeHtml(activeSearchQuery)}"
        />
        ${activeSearchQuery ? `<button id="clear-teacher-search-btn" class="clear-btn">&times;</button>` : ''}
      </div>

      <div class="filter-group">
        <label for="teacher-status-select" class="filter-label">Status:</label>
        <select id="teacher-status-select" class="filter-select">
          <option value="" ${activeStatusFilter === '' ? 'selected' : ''}>All Faculty</option>
          <option value="true" ${activeStatusFilter === 'true' ? 'selected' : ''}>Active Only</option>
          <option value="false" ${activeStatusFilter === 'false' ? 'selected' : ''}>Inactive Only</option>
        </select>
      </div>

      <div class="toolbar-stats" id="teacher-count-badge">
        Loading count...
      </div>
    </div>

    <!-- Interactive Data Table Container -->
    <div class="panel panel-ledger" id="teachers-table-container">
      <div class="loading-state">
        <div class="spinner"></div>
        <p class="loading-text">Loading teacher records from verified CSV database...</p>
      </div>
    </div>

    <!-- Hire / Edit Teacher Modal -->
    <div id="teacher-modal" class="modal-backdrop" style="display: none;">
      <div class="modal-dialog">
        <div class="modal-header">
          <h3 id="teacher-modal-title" class="modal-title">Hire New Faculty Member</h3>
          <button id="close-teacher-modal-btn" class="modal-close">&times;</button>
        </div>
        <form id="teacher-form" class="modal-form">
          <input type="hidden" id="teacher-id-field" value="" />

          <div class="form-alert" id="teacher-form-alert" style="display: none;"></div>

          <div class="form-row">
            <div class="form-group">
              <label for="teacher-name">Full Faculty Name <span class="req">*</span></label>
              <input type="text" id="teacher-name" class="form-input" placeholder="e.g. Dr. Maya Rao" required />
            </div>
            <div class="form-group">
              <label for="teacher-id-num">Teacher ID (Optional / Auto)</label>
              <input type="number" id="teacher-id-num" class="form-input" placeholder="Auto" min="1" />
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label for="teacher-dept">Academic Department <span class="req">*</span></label>
              <input type="text" id="teacher-dept" class="form-input" placeholder="e.g. Science, Mathematics, Languages" required />
            </div>
            <div class="form-group">
              <label for="teacher-salary">Monthly Salary (₹) <span class="req">*</span></label>
              <input type="number" id="teacher-salary" class="form-input" placeholder="e.g. 28000" min="0" step="500" required />
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label for="teacher-email">Official Email</label>
              <input type="email" id="teacher-email" class="form-input" placeholder="teacher@example.com" />
            </div>
            <div class="form-group">
              <label for="teacher-phone">Contact Phone</label>
              <input type="tel" id="teacher-phone" class="form-input" placeholder="e.g. 9000000010" />
            </div>
          </div>

          <div class="modal-footer">
            <button type="button" id="cancel-teacher-modal-btn" class="btn btn-secondary">Cancel</button>
            <button type="submit" id="save-teacher-btn" class="btn btn-action-primary">Save Appointment</button>
          </div>
        </form>
      </div>
    </div>
  `;

  bindTeacherEvents();
}

function bindTeacherEvents() {
  const searchInput = document.getElementById('teacher-search-input');
  if (searchInput) {
    let debounceTimer;
    searchInput.addEventListener('input', (e) => {
      clearTimeout(debounceTimer);
      activeSearchQuery = e.target.value.trim();
      debounceTimer = setTimeout(() => {
        loadTeachers();
      }, 250);
    });
  }

  const clearBtn = document.getElementById('clear-teacher-search-btn');
  if (clearBtn) {
    clearBtn.addEventListener('click', () => {
      activeSearchQuery = '';
      if (searchInput) searchInput.value = '';
      loadTeachers();
    });
  }

  const statusSelect = document.getElementById('teacher-status-select');
  if (statusSelect) {
    statusSelect.addEventListener('change', (e) => {
      activeStatusFilter = e.target.value;
      loadTeachers();
    });
  }

  const openModalBtn = document.getElementById('open-hire-modal-btn');
  if (openModalBtn) {
    openModalBtn.addEventListener('click', () => openTeacherModal());
  }

  const closeModalBtn = document.getElementById('close-teacher-modal-btn');
  const cancelModalBtn = document.getElementById('cancel-teacher-modal-btn');
  if (closeModalBtn) closeModalBtn.addEventListener('click', closeTeacherModal);
  if (cancelModalBtn) cancelModalBtn.addEventListener('click', closeTeacherModal);

  const teacherForm = document.getElementById('teacher-form');
  if (teacherForm) {
    teacherForm.addEventListener('submit', handleTeacherFormSubmit);
  }
}

async function loadTeachers() {
  const tableContainer = document.getElementById('teachers-table-container');
  const countBadge = document.getElementById('teacher-count-badge');
  if (!tableContainer) return;

  try {
    const teachers = await api.getTeachers(activeSearchQuery, activeStatusFilter);
    allTeachers = Array.isArray(teachers) ? teachers : [];

    if (countBadge) {
      const activeCount = allTeachers.filter(t => t.active).length;
      countBadge.innerHTML = `Showing <strong>${allTeachers.length}</strong> teachers (<strong>${activeCount}</strong> Active)`;
    }

    renderTeacherTable(tableContainer, allTeachers);
  } catch (err) {
    console.error('Error fetching teachers:', err);
    tableContainer.innerHTML = `
      <div class="empty-state">
        <p class="error-msg">Failed to retrieve teacher records: ${escapeHtml(err.message)}</p>
        <button id="retry-teachers-btn" class="btn btn-secondary" style="margin-top: 10px;">Retry</button>
      </div>
    `;
    const retryBtn = document.getElementById('retry-teachers-btn');
    if (retryBtn) retryBtn.onclick = () => loadTeachers();
  }
}

function renderTeacherTable(container, teachers) {
  if (teachers.length === 0) {
    container.innerHTML = `
      <div class="empty-state">
        <p>No teachers found matching your criteria.</p>
        ${activeSearchQuery || activeStatusFilter ? `
          <button id="reset-teacher-filter-btn" class="btn btn-secondary" style="margin-top: 12px;">Reset Filters</button>
        ` : ''}
      </div>
    `;
    const resetBtn = document.getElementById('reset-teacher-filter-btn');
    if (resetBtn) {
      resetBtn.onclick = () => {
        activeSearchQuery = '';
        activeStatusFilter = '';
        const searchInput = document.getElementById('teacher-search-input');
        const statusSelect = document.getElementById('teacher-status-select');
        if (searchInput) searchInput.value = '';
        if (statusSelect) statusSelect.value = '';
        loadTeachers();
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
            <th>Faculty Name</th>
            <th>Department</th>
            <th>Contact</th>
            <th style="text-align: right;">Monthly Salary</th>
            <th>Assigned Subjects</th>
            <th>Status</th>
            <th style="text-align: center;">Actions</th>
          </tr>
        </thead>
        <tbody>
          ${teachers.map(t => `
            <tr data-teacher-id="${t.id}">
              <td class="tx-id">#${t.id}</td>
              <td><span class="entity-name">${escapeHtml(t.name)}</span></td>
              <td><span class="dept-pill">${escapeHtml(t.department || 'General')}</span></td>
              <td>
                <div class="contact-block">
                  <span>${escapeHtml(t.email || '—')}</span>
                  <small>${escapeHtml(t.phone || '—')}</small>
                </div>
              </td>
              <td class="numeric-cell">₹${formatNum(t.monthlySalary)}</td>
              <td>
                <span class="subject-ids-tag">
                  ${Array.isArray(t.subjectIds) && t.subjectIds.length > 0 
                    ? t.subjectIds.map(id => `#${id}`).join(', ') 
                    : 'Unassigned'}
                </span>
              </td>
              <td>
                <span class="status-pill status-${t.active ? 'active' : 'inactive'}">
                  ${t.active ? 'ACTIVE' : 'INACTIVE'}
                </span>
              </td>
              <td style="text-align: center;">
                <div class="action-btn-group">
                  <button class="btn-action edit-teacher-btn" data-id="${t.id}" title="Edit Faculty Profile">Edit</button>
                  ${t.active ? `
                    <button class="btn-action btn-danger deactivate-teacher-btn" data-id="${t.id}" title="Deactivate Faculty">Deactivate</button>
                  ` : `
                    <button class="btn-action btn-success activate-teacher-btn" data-id="${t.id}" title="Reactivate Faculty">Activate</button>
                  `}
                </div>
              </td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    </div>
  `;

  // Attach action buttons
  container.querySelectorAll('.edit-teacher-btn').forEach(btn => {
    btn.onclick = () => {
      const id = parseInt(btn.dataset.id, 10);
      const teacher = allTeachers.find(t => t.id === id);
      if (teacher) openTeacherModal(teacher);
    };
  });

  container.querySelectorAll('.deactivate-teacher-btn').forEach(btn => {
    btn.onclick = () => handleDeactivateTeacher(parseInt(btn.dataset.id, 10));
  });

  container.querySelectorAll('.activate-teacher-btn').forEach(btn => {
    btn.onclick = () => handleActivateTeacher(parseInt(btn.dataset.id, 10));
  });
}

function openTeacherModal(teacher = null) {
  const modal = document.getElementById('teacher-modal');
  const title = document.getElementById('teacher-modal-title');
  const alert = document.getElementById('teacher-form-alert');
  const idField = document.getElementById('teacher-id-field');
  const idNumField = document.getElementById('teacher-id-num');
  const nameField = document.getElementById('teacher-name');
  const deptField = document.getElementById('teacher-dept');
  const salaryField = document.getElementById('teacher-salary');
  const emailField = document.getElementById('teacher-email');
  const phoneField = document.getElementById('teacher-phone');

  if (alert) alert.style.display = 'none';

  if (teacher) {
    title.textContent = `Edit Faculty: ${teacher.name} (#${teacher.id})`;
    idField.value = teacher.id;
    if (idNumField) {
      idNumField.value = teacher.id;
      idNumField.disabled = true;
    }
    nameField.value = teacher.name;
    deptField.value = teacher.department || '';
    salaryField.value = teacher.monthlySalary || 0;
    emailField.value = teacher.email || '';
    phoneField.value = teacher.phone || '';
  } else {
    title.textContent = 'Hire New Faculty Member';
    idField.value = '';
    if (idNumField) {
      idNumField.value = '';
      idNumField.disabled = false;
    }
    nameField.value = '';
    deptField.value = '';
    salaryField.value = '25000';
    emailField.value = '';
    phoneField.value = '';
  }

  modal.style.display = 'flex';
}

function closeTeacherModal() {
  const modal = document.getElementById('teacher-modal');
  if (modal) modal.style.display = 'none';
}

async function handleTeacherFormSubmit(e) {
  e.preventDefault();
  const alert = document.getElementById('teacher-form-alert');
  const idField = document.getElementById('teacher-id-field');
  const isEdit = Boolean(idField.value);

  const payload = {
    name: document.getElementById('teacher-name').value.trim(),
    department: document.getElementById('teacher-dept').value.trim(),
    monthlySalary: parseFloat(document.getElementById('teacher-salary').value) || 0,
    email: document.getElementById('teacher-email').value.trim(),
    phone: document.getElementById('teacher-phone').value.trim(),
  };

  if (!isEdit) {
    const customId = parseInt(document.getElementById('teacher-id-num').value, 10);
    if (!isNaN(customId) && customId > 0) {
      payload.id = customId;
    }
  }

  // Client Validation
  if (!payload.name) {
    showFormError(alert, 'Teacher name is required.');
    return;
  }
  if (!payload.department) {
    showFormError(alert, 'Department is required.');
    return;
  }
  if (payload.monthlySalary < 0) {
    showFormError(alert, 'Monthly salary cannot be negative.');
    return;
  }
  if (payload.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(payload.email)) {
    showFormError(alert, 'Invalid email format.');
    return;
  }

  const saveBtn = document.getElementById('save-teacher-btn');
  saveBtn.disabled = true;
  saveBtn.textContent = 'Saving...';

  try {
    if (isEdit) {
      await api.updateTeacher(idField.value, payload);
      showToast(`Teacher #${idField.value} profile updated successfully!`);
    } else {
      const created = await api.hireTeacher(payload);
      showToast(`Teacher ${created.name} (#${created.id}) appointed and saved to CSV!`);
    }
    closeTeacherModal();
    loadTeachers();
  } catch (err) {
    console.error('Save teacher failed:', err);
    showFormError(alert, err.message || 'Server rejected the request.');
  } finally {
    saveBtn.disabled = false;
    saveBtn.textContent = 'Save Appointment';
  }
}

async function handleDeactivateTeacher(id) {
  const teacher = allTeachers.find(t => t.id === id);
  const name = teacher ? teacher.name : `#${id}`;
  if (!confirm(`Are you sure you want to deactivate faculty member ${name}? This sets status to inactive and persists to CSV.`)) {
    return;
  }

  try {
    await api.deactivateTeacher(id);
    showToast(`Teacher ${name} has been deactivated.`, 'warning');
    loadTeachers();
  } catch (err) {
    alert(`Failed to deactivate: ${err.message}`);
  }
}

async function handleActivateTeacher(id) {
  try {
    await api.activateTeacher(id);
    showToast(`Teacher #${id} restored to ACTIVE status!`);
    loadTeachers();
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

