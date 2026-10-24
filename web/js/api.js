/**
 * Reusable, zero-dependency API Client for School Management System.
 * Connects directly to the backend HttpApiServer on the same origin.
 */

async function request(endpoint, options = {}) {
  try {
    const response = await fetch(endpoint, {
      headers: {
        'Accept': 'application/json',
        ...options.headers,
      },
      ...options,
    });

    const contentType = response.headers.get('content-type') || '';
    let data = null;
    if (contentType.includes('application/json')) {
      data = await response.json();
    } else {
      const text = await response.text();
      try {
        data = JSON.parse(text);
      } catch {
        data = { message: text };
      }
    }

    if (!response.ok) {
      const errorMsg = (data && data.message) || `HTTP ${response.status}: ${response.statusText}`;
      const err = new Error(errorMsg);
      err.status = response.status;
      err.data = data;
      throw err;
    }

    return data;
  } catch (error) {
    console.error(`API Request failed for [${endpoint}]:`, error);
    throw error;
  }
}

export const api = {
  getHealth: () => request('/api/health'),
  getDashboardStats: () => request('/api/dashboard/stats'),
  getStudents: (query = '', status = '') => {
    const params = new URLSearchParams();
    if (query) params.set('q', query);
    if (status) params.set('status', status);
    const qs = params.toString() ? `?${params.toString()}` : '';
    return request(`/api/students${qs}`);
  },
  getStudentById: (id) => request(`/api/students/${id}`),
  enrollStudent: (data) =>
    request('/api/students', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    }),
  updateStudent: (id, data) =>
    request(`/api/students/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    }),
  deactivateStudent: (id) =>
    request(`/api/students/${id}`, {
      method: 'DELETE',
    }),
  activateStudent: (id) =>
    request(`/api/students/${id}/activate`, {
      method: 'PATCH',
    }),

  getTeachers: (query = '', active = '') => {
    const params = new URLSearchParams();
    if (query) params.set('q', query);
    if (active !== '') params.set('active', active);
    const qs = params.toString() ? `?${params.toString()}` : '';
    return request(`/api/teachers${qs}`);
  },
  getTeacherById: (id) => request(`/api/teachers/${id}`),
  hireTeacher: (data) =>
    request('/api/teachers', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    }),
  updateTeacher: (id, data) =>
    request(`/api/teachers/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    }),
  deactivateTeacher: (id) =>
    request(`/api/teachers/${id}`, {
      method: 'DELETE',
    }),
  activateTeacher: (id) =>
    request(`/api/teachers/${id}/activate`, {
      method: 'PATCH',
    }),

  getTransactions: (type = '') => {
    const q = type ? `?type=${encodeURIComponent(type)}` : '';
    return request(`/api/finance/transactions${q}`);
  },
  recordTransaction: (data) =>
    request('/api/finance/transactions', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    }),
  getAttendance: (date) => request(`/api/attendance?date=${encodeURIComponent(date)}`),
  markAttendance: (data) =>
    request('/api/attendance', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    }),
  getExams: () => request('/api/academics/exams'),
  createExam: (data) => request('/api/academics/exams', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data),
  }),
  getSubjects: () => request('/api/academics/subjects'),
  createSubject: (data) => request('/api/academics/subjects', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data),
  }),
  getResults: (filters = {}) => {
    const params = new URLSearchParams();
    Object.entries(filters).forEach(([key, value]) => { if (value) params.set(key, value); });
    return request(`/api/results${params.toString() ? `?${params}` : ''}`);
  },
  saveResult: (data) => request('/api/results', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data),
  }),
  getTimetable: () => request('/api/timetable'),
  createTimetableEntry: (data) => request('/api/timetable', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data),
  }),
  updateTimetableEntry: (id, data) => request(`/api/timetable/${id}`, {
    method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data),
  }),
  deleteTimetableEntry: (id) => request(`/api/timetable/${id}`, { method: 'DELETE' }),
  getAcademicYears: () => request('/api/academics/years'),
  createAcademicYear: (data) => request('/api/academics/years', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data),
  }),
  getAcademicClasses: () => request('/api/academics/classes'),
  createAcademicClass: (data) => request('/api/academics/classes', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data),
  }),
};

