/**
 * Main Application Bootstrapper & Sidebar Navigation Router.
 */

import { initDashboard } from './dashboard.js';
import { initStudents } from './students.js';
import { initTeachers } from './teachers.js';
import { initFinance } from './finance.js';
import { initAttendance } from './attendance.js';
import { initResults } from './results.js';
import { initTimetable } from './timetable.js';
import { initAcademics } from './academics.js';

document.addEventListener('DOMContentLoaded', () => {
  setupNavigation();
  // Start on Dashboard
  switchView('dashboard');
});

const MODULE_META = {
  finance: {
    title: 'Institutional Finance & Fees',
    stage: 'Stage 4',
    description: 'Student fee collection, automated receipt issuance, operational expense vouchers, and donation ledgers.',
    icon: 'M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z'
  },
  academics: {
    title: 'Academic Years & Class Structure',
    stage: 'Stage 5',
    description: 'Creation of formal academic school years, grade section definition, curriculum subjects, and exam periods.',
    icon: 'M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10'
  },
  attendance: {
    title: 'Student & Staff Attendance Tracking',
    stage: 'Stage 5',
    description: 'Daily roll call, duplicate-prevention date validation, absence logging, and attendance percentage calculation.',
    icon: 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4'
  },
  results: {
    title: 'Examination & Grading Governance',
    stage: 'Stage 5',
    description: 'Marks entry per subject, boundary validation, percentage calculation, and automated A+/A/B/C/F grade cards.',
    icon: 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z'
  },
  timetable: {
    title: 'Curriculum Schedule & Conflict Detection',
    stage: 'Stage 5',
    description: 'Weekly period planner with teacher double-booking prevention and room allocation checks.',
    icon: 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z'
  }
};

function setupNavigation() {
  const navButtons = document.querySelectorAll('.nav-btn');
  navButtons.forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.preventDefault();
      const view = btn.dataset.view;
      if (view) {
        switchView(view);
      }
    });
  });
}

function switchView(viewName) {
  // Update sidebar active highlights
  document.querySelectorAll('.nav-btn').forEach(btn => {
    if (btn.dataset.view === viewName) {
      btn.classList.add('active');
    } else {
      btn.classList.remove('active');
    }
  });

  const dashboardView = document.getElementById('view-dashboard');
  const studentsView = document.getElementById('view-students');
  const teachersView = document.getElementById('view-teachers');
  const financeView = document.getElementById('view-finance');
  const attendanceView = document.getElementById('view-attendance');
  const resultsView = document.getElementById('view-results');
  const timetableView = document.getElementById('view-timetable');
  const academicsView = document.getElementById('view-academics');
  const placeholderView = document.getElementById('view-placeholder');

  // Hide all views first
  if (dashboardView) dashboardView.style.display = 'none';
  if (studentsView) studentsView.style.display = 'none';
  if (teachersView) teachersView.style.display = 'none';
  if (financeView) financeView.style.display = 'none';
  if (attendanceView) attendanceView.style.display = 'none';
  if (resultsView) resultsView.style.display = 'none';
  if (timetableView) timetableView.style.display = 'none';
  if (academicsView) academicsView.style.display = 'none';
  if (placeholderView) placeholderView.style.display = 'none';

  if (viewName === 'dashboard') {
    if (dashboardView) dashboardView.style.display = 'block';
    initDashboard();
  } else if (viewName === 'students') {
    if (studentsView) studentsView.style.display = 'block';
    initStudents();
  } else if (viewName === 'teachers') {
    if (teachersView) teachersView.style.display = 'block';
    initTeachers();
  } else if (viewName === 'finance') {
    if (financeView) financeView.style.display = 'block';
    initFinance();
  } else if (viewName === 'attendance') {
    if (attendanceView) attendanceView.style.display = 'block';
    initAttendance();
  } else if (viewName === 'results') {
    if (resultsView) resultsView.style.display = 'block';
    initResults();
  } else if (viewName === 'timetable') {
    if (timetableView) timetableView.style.display = 'block';
    initTimetable();
  } else if (viewName === 'academics') {
    if (academicsView) academicsView.style.display = 'block';
    initAcademics();
  } else {
    if (placeholderView) {
      placeholderView.style.display = 'block';
      renderTemporaryView(placeholderView, viewName);
    }
  }
}

function renderTemporaryView(container, viewName) {
  const meta = MODULE_META[viewName] || {
    title: `${capitalize(viewName)} Module`,
    stage: 'Next Phase',
    description: 'This operational section will be connected to its dedicated API endpoints in the scheduled stage.',
    icon: 'M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z'
  };

  container.innerHTML = `
    <div class="panel panel-placeholder">
      <div class="placeholder-crest">
        <svg viewBox="0 0 24 24" class="crest-svg" fill="none" stroke="currentColor" stroke-width="1.8">
          <path stroke-linecap="round" stroke-linejoin="round" d="${meta.icon}" />
        </svg>
      </div>
      <div class="placeholder-badge">${meta.stage} &bull; Scheduled Implementation</div>
      <h2 class="placeholder-title">${meta.title}</h2>
      <p class="placeholder-desc">${meta.description}</p>
      
      <div class="placeholder-info-box">
        <h4>Operational Scope in Upcoming Release:</h4>
        <ul>
          <li>Direct integration with verified <code>sms.service</code> business rules.</li>
          <li>Synchronized CSV persistence handling file I/O safely.</li>
          <li>Responsive editorial table with live search and filtering.</li>
        </ul>
      </div>

      <button id="back-to-dash-btn" class="btn btn-return">
        &larr; Return to Live Dashboard
      </button>
    </div>
  `;

  const backBtn = document.getElementById('back-to-dash-btn');
  if (backBtn) {
    backBtn.onclick = () => switchView('dashboard');
  }
}

function capitalize(s) {
  if (!s) return '';
  return s.charAt(0).toUpperCase() + s.slice(1);
}

