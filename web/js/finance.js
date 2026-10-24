import { api } from './api.js';
import { showToast } from './students.js';

const TYPES = {
  FEE_PAYMENT: 'Fee collection',
  SALARY: 'Salary payment',
  EXPENSE: 'Expense',
  DONATION: 'Donation',
  REFUND: 'Refund',
};

let selectedType = '';

export function initFinance() {
  const container = document.getElementById('view-finance');
  if (!container) return;
  selectedType = '';
  renderLayout(container);
  loadFinance(container);
}

function renderLayout(container) {
  container.innerHTML = `
    <header class="editorial-topbar">
      <div>
        <p class="eyebrow">Stage 4 &bull; Finance & Fees</p>
        <h2 class="hero-heading">Finance & Fees</h2>
        <p class="subheading">Live school balances, fee collection, payroll, and recorded transactions.</p>
      </div>
      <button id="refresh-finance-btn" class="btn-refresh" type="button">Sync Data</button>
    </header>
    <div id="finance-alert" class="finance-alert" role="status" style="display:none"></div>
    <section id="finance-summary" class="finance-summary" aria-label="Financial summary"></section>
    <section class="panel finance-form-panel">
      <div class="panel-header-row">
        <div><h3 class="panel-title-editorial">Record a Transaction</h3><p class="panel-subtitle">Changes are validated by FinanceService and saved to CSV.</p></div>
      </div>
      <form id="finance-form" class="finance-form">
        <div class="form-group">
          <label for="finance-type">Transaction type</label>
          <select id="finance-type" class="form-input" required>
            <option value="FEE_PAYMENT">Collect student fee</option>
            <option value="SALARY">Pay teacher salary</option>
            <option value="EXPENSE">Add expense</option>
            <option value="DONATION">Record donation</option>
          </select>
        </div>
        <div id="finance-student-field" class="form-group finance-wide"><label for="finance-student">Student</label><select id="finance-student" class="form-input" required></select></div>
        <div id="finance-teacher-field" class="form-group finance-wide" hidden><label for="finance-teacher">Teacher</label><select id="finance-teacher" class="form-input"></select></div>
        <div id="finance-category-field" class="form-group" hidden><label for="finance-category">Expense category</label><select id="finance-category" class="form-input"><option>ELECTRICITY</option><option>WATER</option><option>MAINTENANCE</option><option>LABORATORY</option><option>SPORTS</option><option>EVENTS</option><option>TEACHING_MATERIALS</option><option>TRANSPORT</option><option>OTHER</option></select></div>
        <div id="finance-purpose-field" class="form-group finance-wide" hidden><label for="finance-purpose">Purpose</label><input id="finance-purpose" class="form-input" maxlength="100" /></div>
        <div id="finance-donor-field" class="form-group finance-wide" hidden><label for="finance-donor">Donor name</label><input id="finance-donor" class="form-input" maxlength="100" /></div>
        <div id="finance-amount-field" class="form-group"><label for="finance-amount">Amount (₹)</label><input id="finance-amount" class="form-input" type="number" min="0.01" step="0.01" required /></div>
        <div class="finance-submit"><button id="finance-submit-btn" class="btn btn-action-primary" type="submit">Save Transaction</button></div>
      </form>
    </section>
    <section class="panel finance-ledger-panel">
      <div class="panel-header-row">
        <div><h3 class="panel-title-editorial">Transaction Ledger</h3><p class="panel-subtitle">Newest entries appear first.</p></div>
        <select id="finance-filter" class="filter-select" aria-label="Filter transactions by type">
          <option value="">All transactions</option>
          ${Object.entries(TYPES).map(([value, label]) => `<option value="${value}">${label}</option>`).join('')}
        </select>
      </div>
      <div id="finance-ledger"><div class="loading-state">Loading transactions...</div></div>
    </section>`;

  document.getElementById('refresh-finance-btn').onclick = () => loadFinance(container);
  document.getElementById('finance-filter').onchange = (event) => {
    selectedType = event.target.value;
    loadLedger();
  };
  document.getElementById('finance-type').onchange = updateFields;
  document.getElementById('finance-teacher').onchange = setSalaryAmount;
  document.getElementById('finance-form').onsubmit = submitTransaction;
  updateFields();
}

async function loadFinance(container) {
  const summary = document.getElementById('finance-summary');
  const ledger = document.getElementById('finance-ledger');
  summary.innerHTML = '<div class="loading-state">Loading financial summary...</div>';
  ledger.innerHTML = '<div class="loading-state">Loading transactions...</div>';
  try {
    const [stats, transactions, students, teachers] = await Promise.all([
      api.getDashboardStats(), api.getTransactions(),
      api.getStudents('', 'ACTIVE'), api.getTeachers('', 'true'),
    ]);
    if (!container.isConnected) return;
    renderSummary(summary, stats);
    fillPeople(students, teachers);
    renderLedger(ledger, filterTransactions(transactions));
  } catch (error) {
    summary.innerHTML = '';
    ledger.innerHTML = `<div class="empty-state">Unable to load finance data: ${escapeHtml(error.message)}</div>`;
    setAlert(error.message, true);
  }
}

async function loadLedger() {
  const ledger = document.getElementById('finance-ledger');
  if (!ledger) return;
  ledger.innerHTML = '<div class="loading-state">Loading transactions...</div>';
  try {
    renderLedger(ledger, await api.getTransactions(selectedType));
  } catch (error) {
    ledger.innerHTML = `<div class="empty-state">Unable to load transactions: ${escapeHtml(error.message)}</div>`;
  }
}

function renderSummary(container, stats) {
  const metrics = [
    ['Available balance', stats.balance, 'panel-midnight'],
    ['Total income', stats.totalIncome, 'panel-moss'],
    ['Total expenses', stats.totalExpenses, 'panel-rosy'],
    ['Pending student fees', stats.pendingFees, 'panel-darkgrid'],
    ['Fees collected', stats.totalFeesCollected, 'panel-moss'],
    ['Salaries paid', stats.totalSalaryPaid, 'panel-midnight'],
    ['Other expenses', stats.totalOtherExpenses, 'panel-rosy'],
    ['Donations', stats.totalDonations, 'panel-darkgrid'],
  ];
  container.innerHTML = metrics.map(([label, amount, tone]) => `
    <article class="panel finance-metric ${tone}"><span class="finance-metric-label">${label}</span>
      <strong>${money(amount)}</strong></article>`).join('');
}

function fillPeople(students, teachers) {
  const studentSelect = document.getElementById('finance-student');
  const teacherSelect = document.getElementById('finance-teacher');
  studentSelect.innerHTML = students.map(s => `<option value="${s.id}">${escapeHtml(s.name)} (#${s.id}) — remaining ${money(s.remainingFees)}</option>`).join('') || '<option value="">No active students</option>';
  teacherSelect.innerHTML = teachers.map(t => `<option value="${t.id}" data-salary="${Number(t.monthlySalary)}">${escapeHtml(t.name)} (#${t.id}) — ${money(t.monthlySalary)}/month</option>`).join('') || '<option value="">No active teachers</option>';
  setSalaryAmount();
}

function updateFields() {
  const type = document.getElementById('finance-type').value;
  document.getElementById('finance-student-field').hidden = type !== 'FEE_PAYMENT';
  document.getElementById('finance-teacher-field').hidden = type !== 'SALARY';
  document.getElementById('finance-category-field').hidden = type !== 'EXPENSE';
  document.getElementById('finance-purpose-field').hidden = type !== 'EXPENSE';
  document.getElementById('finance-donor-field').hidden = type !== 'DONATION';
  document.getElementById('finance-purpose').required = type === 'EXPENSE';
  document.getElementById('finance-donor').required = type === 'DONATION';
  document.getElementById('finance-student').required = type === 'FEE_PAYMENT';
  document.getElementById('finance-teacher').required = type === 'SALARY';
  document.getElementById('finance-amount').readOnly = type === 'SALARY';
  setSalaryAmount();
}

function setSalaryAmount() {
  if (document.getElementById('finance-type')?.value !== 'SALARY') return;
  const selected = document.getElementById('finance-teacher')?.selectedOptions[0];
  document.getElementById('finance-amount').value = selected?.dataset.salary || '';
}

async function submitTransaction(event) {
  event.preventDefault();
  const type = document.getElementById('finance-type').value;
  const amount = Number(document.getElementById('finance-amount').value);
  if (type !== 'SALARY' && (!Number.isFinite(amount) || amount <= 0)) {
    setAlert('Enter an amount greater than zero.', true);
    return;
  }
  const payload = { type };
  if (type !== 'SALARY') payload.amount = amount;
  if (type === 'FEE_PAYMENT') payload.studentId = Number(document.getElementById('finance-student').value);
  if (type === 'SALARY') payload.teacherId = Number(document.getElementById('finance-teacher').value);
  if (type === 'EXPENSE') {
    payload.category = document.getElementById('finance-category').value;
    payload.purpose = document.getElementById('finance-purpose').value.trim();
    if (!payload.purpose) return setAlert('Enter an expense purpose.', true);
  }
  if (type === 'DONATION') {
    payload.donor = document.getElementById('finance-donor').value.trim();
    if (!payload.donor) return setAlert('Enter the donor name.', true);
  }
  const button = document.getElementById('finance-submit-btn');
  button.disabled = true;
  try {
    await api.recordTransaction(payload);
    setAlert(`${TYPES[type]} recorded successfully.`, false);
    showToast(`${TYPES[type]} saved to the ledger.`);
    document.getElementById('finance-purpose').value = '';
    document.getElementById('finance-donor').value = '';
    await loadFinance(document.getElementById('view-finance'));
  } catch (error) {
    setAlert(error.message || 'Could not record transaction.', true);
  } finally {
    button.disabled = false;
  }
}

function filterTransactions(items) {
  return selectedType ? items.filter(item => item.type === selectedType) : items;
}

function renderLedger(container, transactions) {
  if (!transactions.length) {
    container.innerHTML = '<div class="empty-state">No transactions match this filter.</div>';
    return;
  }
  container.innerHTML = `<div class="table-container"><table class="editorial-table"><thead><tr><th>ID</th><th>Type</th><th>Description</th><th>Amount</th><th>Recorded</th></tr></thead><tbody>
    ${[...transactions].reverse().map(tx => `<tr><td class="tx-id">#${tx.id}</td><td>${TYPES[tx.type] || escapeHtml(tx.type)}</td><td>${escapeHtml(tx.description)}</td><td class="numeric-cell ${['FEE_PAYMENT', 'DONATION'].includes(tx.type) ? 'income-text' : 'expense-text'}">${['FEE_PAYMENT', 'DONATION'].includes(tx.type) ? '+' : '−'}${money(tx.amount)}</td><td>${formatDate(tx.timestamp)}</td></tr>`).join('')}
  </tbody></table></div>`;
}

function setAlert(message, isError) {
  const alert = document.getElementById('finance-alert');
  if (!alert) return;
  alert.textContent = message;
  alert.className = `finance-alert ${isError ? 'finance-alert-error' : 'finance-alert-success'}`;
  alert.style.display = 'block';
}

function money(value) {
  const amount = Number(value || 0);
  return `₹${amount.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

function formatDate(value) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? escapeHtml(value) : date.toLocaleString('en-IN');
}

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' }[char]));
}
