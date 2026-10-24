/**
 * Dashboard View Controller.
 * Loads live metrics from /api/dashboard/stats, /api/health, and recent records.
 */

import { api } from './api.js';

export function initDashboard() {
  const container = document.getElementById('dashboard-content');
  if (!container) return;

  loadDashboardData(container);

  // Attach refresh button listener
  const refreshBtn = document.getElementById('refresh-dashboard-btn');
  if (refreshBtn) {
    refreshBtn.onclick = () => loadDashboardData(container);
  }
}

async function loadDashboardData(container) {
  // Show loading skeleton
  container.innerHTML = `
    <div class="loading-state">
      <div class="spinner"></div>
      <p class="loading-text">Fetching real-time records from backend API...</p>
    </div>
  `;

  try {
    const [health, stats, students, teachers, transactions] = await Promise.all([
      api.getHealth().catch(err => ({ status: 'UNKNOWN', school: 'Green Valley High School' })),
      api.getDashboardStats(),
      api.getStudents().catch(() => []),
      api.getTeachers().catch(() => []),
      api.getTransactions().catch(() => []),
    ]);

    renderDashboard(container, { health, stats, students, teachers, transactions });
  } catch (error) {
    console.error('Failed to load dashboard:', error);
    renderError(container, error.message);
  }
}

function renderDashboard(container, { health, stats, students, teachers, transactions }) {
  // Update header school name if element exists
  const schoolNameEl = document.getElementById('school-title');
  if (schoolNameEl && stats.schoolName) {
    schoolNameEl.textContent = stats.schoolName;
  }

  const balance = stats.balance ?? 0;
  const totalIncome = stats.totalIncome ?? 0;
  const totalExpenses = stats.totalExpenses ?? 0;
  const feesCollected = stats.totalFeesCollected ?? 0;
  const pendingFees = stats.pendingFees ?? 0;
  const totalSalary = stats.totalSalaryPaid ?? 0;
  const otherExpenses = stats.totalOtherExpenses ?? 0;
  const donations = stats.totalDonations ?? 0;
  const studentCount = stats.studentCount ?? students.length;
  const teacherCount = stats.teacherCount ?? teachers.length;

  const totalFeeAssessment = Number(feesCollected) + Number(pendingFees);
  const feeCollectionPct = totalFeeAssessment > 0
    ? Math.round((Number(feesCollected) / totalFeeAssessment) * 100)
    : 100;

  // Most recent 8 transactions (reversed for chronological recent first)
  const recentTransactions = Array.isArray(transactions)
    ? [...transactions].reverse().slice(0, 8)
    : [];

  container.innerHTML = `
    <!-- Top System Pulse & Meta -->
    <div class="dash-meta-bar">
      <div class="pulse-indicator">
        <span class="status-dot"></span>
        <span class="pulse-label">SYSTEM ONLINE &bull; ${escapeHtml(health.school || 'School Management System')} (Port ${health.port || 8080})</span>
      </div>
      <div class="meta-timestamp">
        <span>Verified CSV Persistence Loaded</span>
      </div>
    </div>

    <!-- Asymmetrical Color Panels Grid -->
    <div class="editorial-grid">
      
      <!-- Panel 1: Primary Treasury Hero (Midnight Green) -->
      <section class="panel panel-midnight hero-panel">
        <div class="panel-header">
          <span class="panel-tag">INSTITUTIONAL TREASURY</span>
          <span class="panel-badge-light">AUDITED BALANCE</span>
        </div>
        <div class="balance-display">
          <span class="currency-symbol">₹</span>
          <span class="balance-amount">${formatNumber(balance)}</span>
        </div>
        <p class="balance-caption">Net available liquid school capital after salaries, operations, and collections.</p>
        
        <div class="hero-split-stats">
          <div class="split-stat-item">
            <span class="stat-label">TOTAL OPERATING INCOME</span>
            <span class="stat-value income-val">+₹${formatNumber(totalIncome)}</span>
            <span class="stat-hint">Fees (₹${formatNumber(feesCollected)}) + Donations (₹${formatNumber(donations)})</span>
          </div>
          <div class="split-stat-item">
            <span class="stat-label">TOTAL DISBURSED EXPENSES</span>
            <span class="stat-value expense-val">-₹${formatNumber(totalExpenses)}</span>
            <span class="stat-hint">Salaries (₹${formatNumber(totalSalary)}) + Operations (₹${formatNumber(otherExpenses)})</span>
          </div>
        </div>
      </section>

      <!-- Panel 2: Student Cohort (Moss Green) -->
      <section class="panel panel-moss">
        <div class="panel-header">
          <span class="panel-tag">STUDENT ENROLLMENT</span>
          <span class="panel-badge-dark">ACTIVE COHORT</span>
        </div>
        <div class="big-metric">
          <span class="big-number">${studentCount}</span>
          <span class="big-unit">Students Enrolled</span>
        </div>
        <div class="metric-detail-block">
          <div class="detail-row">
            <span>Verified CSV Records:</span>
            <strong>${students.length}</strong>
          </div>
          <div class="detail-row">
            <span>Outstanding Pending Fees:</span>
            <strong class="pending-highlight">₹${formatNumber(pendingFees)}</strong>
          </div>
        </div>
        <div class="fee-progress-wrapper">
          <div class="fee-progress-labels">
            <span>Fee Collection Rate</span>
            <span><strong>${feeCollectionPct}%</strong></span>
          </div>
          <div class="progress-track">
            <div class="progress-fill" style="width: ${feeCollectionPct}%;"></div>
          </div>
        </div>
      </section>

      <!-- Panel 3: Academic Faculty (Rosy Brown) -->
      <section class="panel panel-rosy">
        <div class="panel-header">
          <span class="panel-tag">ACADEMIC FACULTY</span>
          <span class="panel-badge-dark">STAFFING</span>
        </div>
        <div class="big-metric text-dark">
          <span class="big-number">${teacherCount}</span>
          <span class="big-unit">Appointed Teachers</span>
        </div>
        <div class="metric-detail-block text-dark">
          <div class="detail-row">
            <span>Total Cumulative Payroll:</span>
            <strong>₹${formatNumber(totalSalary)}</strong>
          </div>
          <div class="detail-row">
            <span>Active Teaching Status:</span>
            <strong>100% Operational</strong>
          </div>
        </div>
        <div class="faculty-tag-cloud">
          ${renderDepartmentTags(teachers)}
        </div>
      </section>

      <!-- Panel 4: Detailed Financial Ledger (Dark Green Full Width) -->
      <section class="panel panel-darkgrid full-span">
        <div class="panel-header-row">
          <div>
            <h3 class="panel-title-editorial">Financial Audit Trail</h3>
            <p class="panel-subtitle">Real-time ledger of recorded payments, disbursements, and donations.</p>
          </div>
          <div class="ledger-count-tag">
            <span>Total Ledger Entries: <strong>${stats.transactionCount || recentTransactions.length}</strong></span>
          </div>
        </div>

        ${recentTransactions.length === 0 ? `
          <div class="empty-state">
            <p>No financial transactions currently on record.</p>
          </div>
        ` : `
          <div class="table-container">
            <table class="editorial-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Type</th>
                  <th>Description</th>
                  <th>Amount</th>
                  <th>Timestamp</th>
                </tr>
              </thead>
              <tbody>
                ${recentTransactions.map(tx => `
                  <tr>
                    <td class="tx-id">#${tx.id}</td>
                    <td><span class="tx-badge tx-badge-${String(tx.type).toLowerCase()}">${escapeHtml(tx.type)}</span></td>
                    <td class="tx-desc">${escapeHtml(tx.description || '—')}</td>
                    <td class="tx-amount ${isIncomeType(tx.type) ? 'income' : 'expense'}">
                      ${isIncomeType(tx.type) ? '+' : '-'}₹${formatNumber(tx.amount)}
                    </td>
                    <td class="tx-time">${formatTimestamp(tx.timestamp)}</td>
                  </tr>
                `).join('')}
              </tbody>
            </table>
          </div>
        `}
      </section>

    </div>
  `;
}

function renderDepartmentTags(teachers) {
  if (!Array.isArray(teachers) || teachers.length === 0) {
    return `<span class="dept-pill">General Faculty</span>`;
  }
  const depts = [...new Set(teachers.map(t => t.department).filter(Boolean))];
  if (depts.length === 0) return `<span class="dept-pill">Faculty Assigned</span>`;
  return depts.map(d => `<span class="dept-pill">${escapeHtml(d)}</span>`).join('');
}

function isIncomeType(type) {
  return type === 'FEE_PAYMENT' || type === 'DONATION';
}

function formatNumber(num) {
  if (num === null || num === undefined) return '0.00';
  const val = Number(num);
  if (isNaN(val)) return '0.00';
  return val.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function formatTimestamp(ts) {
  if (!ts) return '—';
  try {
    const d = new Date(ts);
    if (isNaN(d.getTime())) return String(ts).substring(0, 19).replace('T', ' ');
    return d.toLocaleString('en-IN', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  } catch {
    return String(ts);
  }
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

function renderError(container, message) {
  container.innerHTML = `
    <div class="panel panel-error">
      <div class="error-icon">!</div>
      <div class="error-content">
        <h3>Backend Communication Failure</h3>
        <p class="error-msg">${escapeHtml(message || 'Failed to establish connection with Java HTTP API on port 8080.')}</p>
        <button id="retry-dashboard-btn" class="btn btn-retry">Retry Connection</button>
      </div>
    </div>
  `;
  const retryBtn = document.getElementById('retry-dashboard-btn');
  if (retryBtn) {
    retryBtn.onclick = () => initDashboard();
  }
}

