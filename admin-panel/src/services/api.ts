const API_BASE = 'https://order-taking-app-production.up.railway.app/api';

function getHeaders() {
  const token = localStorage.getItem('token');
  return {
    'Content-Type': 'application/json',
    ...(token ? { 'Authorization': `Bearer ${token}` } : {})
  };
}

async function request(path: string, options: RequestInit = {}) {
  const url = `${API_BASE}${path}`;
  const response = await fetch(url, {
    ...options,
    headers: {
      ...getHeaders(),
      ...options.headers
    }
  });

  if (response.status === 401 || response.status === 403) {
    // Auth error: logout and refresh
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    if (!window.location.pathname.includes('/login') && window.location.pathname !== '/') {
      window.location.reload();
    }
  }

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.error || `HTTP error ${response.status}`);
  }

  return response.json();
}

export const api = {
  // Auth
  async login(username: string, password: string) {
    const res = await request('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password })
    });
    localStorage.setItem('token', res.token);
    localStorage.setItem('user', JSON.stringify(res.user));
    return res;
  },

  logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  },

  getCurrentUser() {
    const userStr = localStorage.getItem('user');
    return userStr ? JSON.parse(userStr) : null;
  },

  // Users
  getUsers() {
    return request('/users');
  },
  createUser(userData: any) {
    return request('/users', {
      method: 'POST',
      body: JSON.stringify(userData)
    });
  },
  updateUser(id: number, userData: any) {
    return request(`/users/${id}`, {
      method: 'PUT',
      body: JSON.stringify(userData)
    });
  },
  deleteUser(id: number) {
    return request(`/users/${id}`, {
      method: 'DELETE'
    });
  },

  // Categories
  getCategories() {
    return request('/categories');
  },
  createCategory(name: string) {
    return request('/categories', {
      method: 'POST',
      body: JSON.stringify({ name })
    });
  },
  deleteCategory(id: number) {
    return request(`/categories/${id}`, {
      method: 'DELETE'
    });
  },

  // Products
  getProducts() {
    return request('/products');
  },
  createProduct(productData: any) {
    return request('/products', {
      method: 'POST',
      body: JSON.stringify(productData)
    });
  },
  updateProduct(id: number, productData: any) {
    return request(`/products/${id}`, {
      method: 'PUT',
      body: JSON.stringify(productData)
    });
  },
  deleteProduct(id: number) {
    return request(`/products/${id}`, {
      method: 'DELETE'
    });
  },

  // Orders
  getActiveOrders() {
    return request('/orders/active');
  },
  getOrderHistory(filters: { startDate?: string; endDate?: string; status?: string; waiterId?: number; isArchived?: boolean } = {}) {
    const params = new URLSearchParams();
    if (filters.startDate) params.append('startDate', filters.startDate);
    if (filters.endDate) params.append('endDate', filters.endDate);
    if (filters.status) params.append('status', filters.status);
    if (filters.waiterId) params.append('waiterId', filters.waiterId.toString());
    if (filters.isArchived !== undefined) params.append('isArchived', filters.isArchived.toString());

    return request(`/orders/history?${params.toString()}`);
  },

  // Day Closure
  getDayClosureReport() {
    return request('/orders/closure-report');
  },
  performDayClosure() {
    return request('/orders/closure', {
      method: 'POST'
    });
  }
};

// WebSocket Handler
export function connectWebSocket(onEvent: (event: string, data: any) => void): () => void {
  const token = localStorage.getItem('token');
  if (!token) return () => {};

  const ws = new WebSocket(`wss://order-taking-app-production.up.railway.app?token=${token}`);

  ws.onopen = () => {
    console.log('Admin WebSocket connected');
  };

  ws.onmessage = (message) => {
    try {
      const payload = JSON.parse(message.data);
      if (payload.event) {
        onEvent(payload.event, payload.data);
      }
    } catch (err) {
      console.error('Error parsing WS message:', err);
    }
  };

  ws.onclose = () => {
    console.log('Admin WebSocket disconnected. Reconnecting in 5s...');
    // Simple reconnect
    setTimeout(() => {
      if (localStorage.getItem('token')) {
        connectWebSocket(onEvent);
      }
    }, 5000);
  };

  ws.onerror = (error) => {
    console.error('WebSocket error:', error);
  };

  // Return a cleanup function
  return () => {
    ws.close();
  };
}
