import React, { useState, useEffect } from 'react';
import { 
  LayoutDashboard, 
  ShoppingBag, 
  Users as UsersIcon, 
  LogOut, 
  Plus, 
  Trash2, 
  Edit, 
  AlertCircle, 
  FileText, 
  CheckCircle2, 
  Shield, 
  Coins, 
  CreditCard,
  Sparkles,
  RefreshCw,
  Clock,
  MessageSquare
} from 'lucide-react';
import { api, connectWebSocket } from './services/api';

export default function App() {
  const [currentUser, setCurrentUser] = useState<any>(api.getCurrentUser());
  const [token, setToken] = useState<string | null>(localStorage.getItem('token'));
  
  // Auth Form
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [authError, setAuthError] = useState('');
  const [authLoading, setAuthLoading] = useState(false);

  // App Navigation
  const [activeTab, setActiveTab] = useState<'dashboard' | 'products' | 'users'>('dashboard');

  // Real-time Dashboard state
  const [activeOrders, setActiveOrders] = useState<any[]>([]);
  const [metrics, setMetrics] = useState({
    totalRevenue: 0,
    cashRevenue: 0,
    cardRevenue: 0,
    activeOrdersCount: 0,
    completedOrdersCount: 0,
  });
  const [wsNotification, setWsNotification] = useState<string | null>(null);

  // Selected Order for Details Modal
  const [selectedOrder, setSelectedOrder] = useState<any | null>(null);
  const [paymentModalOpen, setPaymentModalOpen] = useState(false);
  const [cancellationReason, setCancellationReason] = useState('');
  const [cancelModalOpen, setCancelModalOpen] = useState(false);

  // Day Closure states
  const [closureReport, setClosureReport] = useState<any | null>(null);
  const [closureModalOpen, setClosureModalOpen] = useState(false);

  // Products CRUD State
  const [products, setProducts] = useState<any[]>([]);
  const [categories, setCategories] = useState<any[]>([]);
  const [prodTab, setProdTab] = useState<'items' | 'categories'>('items');
  const [productFormOpen, setProductFormOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState<any | null>(null);
  const [productForm, setProductForm] = useState({ name: '', price: '', category_id: '', is_available: true });
  const [categoryFormOpen, setCategoryFormOpen] = useState(false);
  const [newCategoryName, setNewCategoryName] = useState('');

  // Users CRUD State
  const [usersList, setUsersList] = useState<any[]>([]);
  const [userFormOpen, setUserFormOpen] = useState(false);
  const [userForm, setUserForm] = useState({ username: '', password: '', role: 'waiter' });

  // Load Initial Data when authenticated
  useEffect(() => {
    if (token) {
      loadDashboardData();
      loadProductsAndCategories();
      loadUsersList();

      // Connect to Real-time WebSockets
      const disconnect = connectWebSocket((event, data) => {
        console.log(`WS Event: ${event}`, data);
        
        // Show notification banner
        if (event === 'ORDER_SUBMITTED') {
          setWsNotification(`Νέα παραγγελία καταχωρήθηκε για το τραπέζι ${data.table_id} (${data.zone})!`);
          setTimeout(() => setWsNotification(null), 4000);
        } else if (event === 'DAY_CLOSED') {
          setWsNotification(`Το κλείσιμο ημέρας ολοκληρώθηκε από το διαχειριστή.`);
          setTimeout(() => setWsNotification(null), 4000);
        }

        // Refresh dashboard data
        loadDashboardData();
      });

      return () => {
        disconnect();
      };
    }
  }, [token]);

  // Calculations for dashboard metrics
  const loadDashboardData = async () => {
    try {
      const active = await api.getActiveOrders();
      const history = await api.getOrderHistory({ isArchived: false });
      
      setActiveOrders(active);

      // Calc current day totals (from active or completed/paid/cancelled non-archived orders)
      let revenue = 0;
      let cash = 0;
      let card = 0;
      let completed = 0;

      history.forEach((ord: any) => {
        if (ord.status === 'paid') {
          completed++;
          revenue += ord.total_price;
          if (ord.payment_method === 'cash') cash += ord.total_price;
          if (ord.payment_method === 'card') card += ord.total_price;
        }
      });

      setMetrics({
        totalRevenue: revenue,
        cashRevenue: cash,
        cardRevenue: card,
        activeOrdersCount: active.length,
        completedOrdersCount: completed
      });
    } catch (err) {
      console.error('Error loading dashboard data:', err);
    }
  };

  const loadProductsAndCategories = async () => {
    try {
      const cats = await api.getCategories();
      const prods = await api.getProducts();
      setCategories(cats);
      setProducts(prods);
    } catch (err) {
      console.error('Error loading products/categories:', err);
    }
  };

  const loadUsersList = async () => {
    try {
      const users = await api.getUsers();
      setUsersList(users);
    } catch (err) {
      console.error('Error loading users:', err);
    }
  };

  // Auth Handlers
  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setAuthError('');
    setAuthLoading(true);

    try {
      const res = await api.login(username, password);
      if (res.user.role !== 'admin') {
        api.logout();
        setAuthError('Μόνο οι διαχειριστές (Admins) έχουν πρόσβαση στο Admin Panel.');
        setAuthLoading(false);
        return;
      }
      setCurrentUser(res.user);
      setToken(res.token);
      setUsername('');
      setPassword('');
    } catch (err: any) {
      setAuthError(err.message || 'Αποτυχία σύνδεσης. Ελέγξτε τα στοιχεία στα στοιχεία σας.');
    } finally {
      setAuthLoading(false);
    }
  };

  const handleLogout = () => {
    api.logout();
    setCurrentUser(null);
    setToken(null);
  };

  // CSV Export Utility
  const exportCSV = async () => {
    try {
      const orders = await api.getOrderHistory(); // Get all history
      if (orders.length === 0) {
        alert('Δεν υπάρχουν παραγγελίες για εξαγωγή.');
        return;
      }

      let csvContent = 'ID,Table,Zone,Waiter,Status,Total Price,Payment Method,Notes,Created At,Closed At\n';
      
      orders.forEach((ord: any) => {
        const waiter = ord.waiter_name || '';
        const notes = (ord.notes || '').replace(/"/g, '""');
        csvContent += `${ord.id},"${ord.table_id}","${ord.zone}","${waiter}","${ord.status}",${ord.total_price},"${ord.payment_method || ''}","${notes}","${ord.created_at}","${ord.closed_at || ''}"\n`;
      });

      // Handle Greek Characters in Excel (UTF-8 BOM)
      const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.setAttribute('href', url);
      link.setAttribute('download', `sales_history_${new Date().toISOString().slice(0,10)}.csv`);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    } catch (err) {
      alert('Σφάλμα κατά την εξαγωγή CSV.');
    }
  };

  // Day Closure Actions
  const openClosureReport = async () => {
    try {
      const report = await api.getDayClosureReport();
      setClosureReport(report);
      setClosureModalOpen(true);
    } catch (err) {
      alert('Σφάλμα κατά τον υπολογισμό του κλεισίματος.');
    }
  };

  const handlePerformClosure = async () => {
    if (confirm('Είστε σίγουροι ότι θέλετε να πραγματοποιήσετε κλείσιμο ημέρας; Όλες οι ολοκληρωμένες παραγγελίες θα αρχειοθετηθούν και θα κλειδώσουν.')) {
      try {
        await api.performDayClosure();
        setClosureModalOpen(false);
        alert('Το κλείσιμο ημέρας ολοκληρώθηκε επιτυχώς!');
        loadDashboardData();
      } catch (err) {
        alert('Σφάλμα κατά την ολοκλήρωση του κλεισίματος.');
      }
    }
  };

  // Order Management Actions
  const handlePayment = async (method: 'cash' | 'card') => {
    if (!selectedOrder) return;
    try {
      // Call issueBill endpoint
      await requestCall(`/orders/${selectedOrder.id}/bill`, 'POST', { payment_method: method });
      alert('Ο λογαριασμός εκδόθηκε με επιτυχία!');
      setPaymentModalOpen(false);
      setSelectedOrder(null);
      loadDashboardData();
    } catch (err: any) {
      alert(err.message || 'Αποτυχία έκδοσης λογαριασμού.');
    }
  };

  // We need a request helper for sub-methods since we want to be robust
  const requestCall = async (path: string, method: string, body?: any) => {
    const token = localStorage.getItem('token');
    const response = await fetch(`http://localhost:3000/api${path}`, {
      method,
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: body ? JSON.stringify(body) : undefined
    });
    if (!response.ok) {
      const err = await response.json();
      throw new Error(err.error || 'Request failed');
    }
    return response.json();
  };

  const handleCancelOrder = async () => {
    if (!selectedOrder) return;
    try {
      await requestCall(`/orders/${selectedOrder.id}/cancel`, 'POST', { reason: cancellationReason });
      alert('Η παραγγελία ακυρώθηκε.');
      setCancelModalOpen(false);
      setCancellationReason('');
      setSelectedOrder(null);
      loadDashboardData();
    } catch (err: any) {
      alert(err.message || 'Αποτυχία ακύρωσης.');
    }
  };

  // Products CRUD handlers
  const handleSaveProduct = async (e: React.FormEvent) => {
    e.preventDefault();
    const data = {
      name: productForm.name,
      price: parseFloat(productForm.price),
      category_id: parseInt(productForm.category_id),
      is_available: productForm.is_available
    };

    try {
      if (editingProduct) {
        await api.updateProduct(editingProduct.id, data);
        alert('Το προϊόν ενημερώθηκε.');
      } else {
        await api.createProduct(data);
        alert('Το προϊόν δημιουργήθηκε.');
      }
      setProductFormOpen(false);
      setEditingProduct(null);
      setProductForm({ name: '', price: '', category_id: '', is_available: true });
      loadProductsAndCategories();
    } catch (err: any) {
      alert(err.message || 'Σφάλμα κατά την αποθήκευση.');
    }
  };

  const handleEditProductClick = (prod: any) => {
    setEditingProduct(prod);
    setProductForm({
      name: prod.name,
      price: prod.price.toString(),
      category_id: prod.category_id.toString(),
      is_available: prod.is_available === 1 || prod.is_available === true
    });
    setProductFormOpen(true);
  };

  const handleDeleteProduct = async (id: number) => {
    if (confirm('Είστε σίγουροι ότι θέλετε να διαγράψετε αυτό το προϊόν;')) {
      try {
        await api.deleteProduct(id);
        alert('Το προϊόν διαγράφηκε.');
        loadProductsAndCategories();
      } catch (err: any) {
        alert(err.message || 'Αποτυχία διαγραφής προϊόντος.');
      }
    }
  };

  const handleSaveCategory = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newCategoryName) return;

    try {
      await api.createCategory(newCategoryName);
      alert('Η κατηγορία δημιουργήθηκε.');
      setCategoryFormOpen(false);
      setNewCategoryName('');
      loadProductsAndCategories();
    } catch (err: any) {
      alert(err.message || 'Σφάλμα κατά τη δημιουργία.');
    }
  };

  const handleDeleteCategory = async (id: number) => {
    if (confirm('Είστε σίγουροι ότι θέλετε να διαγράψετε αυτή την κατηγορία;')) {
      try {
        await api.deleteCategory(id);
        alert('Η κατηγορία διαγράφηκε.');
        loadProductsAndCategories();
      } catch (err: any) {
        alert(err.message || 'Αποτυχία διαγραφής κατηγορίας.');
      }
    }
  };

  // User management CRUD
  const handleSaveUser = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await api.createUser(userForm);
      alert('Ο χρήστης δημιουργήθηκε.');
      setUserFormOpen(false);
      setUserForm({ username: '', password: '', role: 'waiter' });
      loadUsersList();
    } catch (err: any) {
      alert(err.message || 'Σφάλμα κατά τη δημιουργία χρήστη.');
    }
  };

  const handleDeleteUser = async (id: number) => {
    if (confirm('Είστε σίγουροι ότι θέλετε να διαγράψετε αυτόν τον χρήστη;')) {
      try {
        await api.deleteUser(id);
        alert('Ο χρήστης διαγράφηκε.');
        loadUsersList();
      } catch (err: any) {
        alert(err.message || 'Αποτυχία διαγραφής χρήστη.');
      }
    }
  };

  // Formatting helpers
  const formatPrice = (val: number) => {
    return new Intl.NumberFormat('el-GR', { style: 'currency', currency: 'EUR' }).format(val);
  };

  // Auth screen layout
  if (!token) {
    return (
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '100vh', padding: '1.5rem' }}>
        <div className="modal-content glass animate-fade-in" style={{ maxWidth: '420px', padding: '2.5rem', borderRadius: 'var(--radius-lg)' }}>
          <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
            <div style={{ display: 'inline-flex', background: 'var(--accent-gradient)', padding: '0.85rem', borderRadius: 'var(--radius-md)', marginBottom: '1rem', boxShadow: 'var(--shadow-neon)' }}>
              <Shield size={32} color="#ffffff" />
            </div>
            <h1 style={{ fontSize: '1.75rem', fontWeight: 800, letterSpacing: '-0.02em', background: 'var(--accent-gradient)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
              Restaurant Admin
            </h1>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginTop: '0.35rem' }}>
              Είσοδος στο σύστημα διαχείρισης
            </p>
          </div>

          {authError && (
            <div className="glass-card" style={{ background: 'var(--danger-bg)', borderColor: 'rgba(239, 68, 68, 0.2)', padding: '0.85rem 1rem', display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.5rem' }}>
              <AlertCircle size={20} color="#f87171" style={{ flexShrink: 0 }} />
              <p style={{ fontSize: '0.85rem', color: '#fca5a5', fontWeight: 500 }}>{authError}</p>
            </div>
          )}

          <form onSubmit={handleLogin}>
            <div className="input-group">
              <label htmlFor="username">Όνομα χρήστη</label>
              <input 
                id="username"
                type="text" 
                className="input-field" 
                placeholder="π.χ. admin" 
                value={username}
                onChange={e => setUsername(e.target.value)}
                required
              />
            </div>
            <div className="input-group" style={{ marginBottom: '2rem' }}>
              <label htmlFor="password">Κωδικός πρόσβασης</label>
              <input 
                id="password"
                type="password" 
                className="input-field" 
                placeholder="••••••••" 
                value={password}
                onChange={e => setPassword(e.target.value)}
                required
              />
            </div>
            <button type="submit" className="btn btn-primary" style={{ width: '100%', padding: '0.95rem' }} disabled={authLoading}>
              {authLoading ? (
                <>
                  <RefreshCw className="animate-spin" size={18} />
                  <span>Σύνδεση...</span>
                </>
              ) : (
                <span>Είσοδος στο Panel</span>
              )}
            </button>
          </form>
        </div>
      </div>
    );
  }

  // Dashboard layout
  return (
    <div className="dashboard-container">
      {/* WS Alert Banner */}
      {wsNotification && (
        <div style={{
          position: 'fixed',
          top: '1.5rem',
          right: '1.5rem',
          zIndex: 1100,
          background: 'rgba(11, 15, 25, 0.95)',
          border: '1px solid var(--accent-cyan)',
          boxShadow: 'var(--shadow-neon-cyan)',
          padding: '1rem 1.5rem',
          borderRadius: 'var(--radius-md)',
          display: 'flex',
          alignItems: 'center',
          gap: '0.75rem',
          backdropFilter: 'blur(8px)',
          animation: 'fadeIn 0.3s cubic-bezier(0.16, 1, 0.3, 1) forwards'
        }}>
          <Sparkles size={20} color="var(--accent-cyan)" className="animate-pulse" />
          <p style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--text-primary)' }}>
            {wsNotification}
          </p>
        </div>
      )}

      {/* Sidebar */}
      <aside className="sidebar">
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', padding: '0.5rem 0.5rem 2rem 0.5rem' }}>
          <div style={{ background: 'var(--accent-gradient)', padding: '0.5rem', borderRadius: '8px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <Shield size={20} color="#ffffff" />
          </div>
          <div>
            <h2 style={{ fontSize: '1.15rem', fontWeight: 800, background: 'var(--accent-gradient)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
              RestoAdmin
            </h2>
            <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 600 }}>SYSTEM CONTROLLER</p>
          </div>
        </div>

        <nav style={{ flexGrow: 1 }}>
          <button 
            className={`btn nav-item ${activeTab === 'dashboard' ? 'active' : ''}`}
            onClick={() => setActiveTab('dashboard')}
            style={{ width: '100%', justifyContent: 'flex-start' }}
          >
            <LayoutDashboard size={18} />
            <span>Dashboard</span>
          </button>

          <button 
            className={`btn nav-item ${activeTab === 'products' ? 'active' : ''}`}
            onClick={() => setActiveTab('products')}
            style={{ width: '100%', justifyContent: 'flex-start' }}
          >
            <ShoppingBag size={18} />
            <span>Προϊόντα & Κατ.</span>
          </button>

          <button 
            className={`btn nav-item ${activeTab === 'users' ? 'active' : ''}`}
            onClick={() => setActiveTab('users')}
            style={{ width: '100%', justifyContent: 'flex-start' }}
          >
            <UsersIcon size={18} />
            <span>Σερβιτόροι / Χρήστες</span>
          </button>
        </nav>

        <div style={{ borderTop: '1px solid var(--border-glass)', paddingTop: '1rem', marginTop: 'auto' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', padding: '0.5rem', marginBottom: '1rem' }}>
            <div style={{ width: '8px', height: '8px', borderRadius: '50%', background: 'var(--success)', boxShadow: '0 0 10px var(--success)' }} />
            <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', fontWeight: 500 }}>
              {currentUser?.username} (Admin)
            </span>
          </div>
          <button 
            className="btn btn-secondary nav-item" 
            onClick={handleLogout}
            style={{ width: '100%', justifyContent: 'flex-start', color: '#fca5a5', border: 'none', background: 'rgba(239, 68, 68, 0.05)' }}
          >
            <LogOut size={18} />
            <span>Αποσύνδεση</span>
          </button>
        </div>
      </aside>

      {/* Main Panel Content */}
      <main className="main-content">
        {activeTab === 'dashboard' && (
          <div className="animate-fade-in">
            {/* Header */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
              <div>
                <h1 style={{ fontSize: '2rem', fontWeight: 800, letterSpacing: '-0.02em' }}>Dashboard</h1>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem' }}>Real-time επισκόπηση του καταστήματος</p>
              </div>
              <div style={{ display: 'flex', gap: '1rem' }}>
                <button className="btn btn-secondary" onClick={loadDashboardData}>
                  <RefreshCw size={16} />
                  <span>Ανανέωση</span>
                </button>
                <button className="btn btn-secondary" onClick={exportCSV}>
                  <FileText size={16} />
                  <span>Εξαγωγή CSV</span>
                </button>
                <button className="btn btn-primary" onClick={openClosureReport}>
                  <CheckCircle2 size={16} />
                  <span>Κλείσιμο Ημέρας</span>
                </button>
              </div>
            </div>

            {/* Metrics cards */}
            <div className="metrics-grid">
              <div className="glass-card">
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
                  <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>Τζίρος Ημέρας</span>
                  <div style={{ background: 'rgba(99, 102, 241, 0.1)', padding: '0.5rem', borderRadius: '8px' }}>
                    <LayoutDashboard size={20} color="var(--accent-indigo)" />
                  </div>
                </div>
                <h3 style={{ fontSize: '1.85rem', fontWeight: 800 }}>{formatPrice(metrics.totalRevenue)}</h3>
                <div style={{ display: 'flex', gap: '1rem', marginTop: '0.5rem', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                  <span>Μετρητά: {formatPrice(metrics.cashRevenue)}</span>
                  <span>Κάρτα: {formatPrice(metrics.cardRevenue)}</span>
                </div>
              </div>

              <div className="glass-card">
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
                  <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>Ενεργές Παραγγελίες</span>
                  <div style={{ background: 'rgba(6, 182, 212, 0.1)', padding: '0.5rem', borderRadius: '8px' }}>
                    <Clock size={20} color="var(--accent-cyan)" />
                  </div>
                </div>
                <h3 style={{ fontSize: '1.85rem', fontWeight: 800 }}>{metrics.activeOrdersCount}</h3>
                <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '0.5rem' }}>Σερβίρονται τώρα στα τραπέζια</p>
              </div>

              <div className="glass-card">
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
                  <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>Ολοκληρωμένες Παραγγελίες</span>
                  <div style={{ background: 'rgba(16, 185, 129, 0.1)', padding: '0.5rem', borderRadius: '8px' }}>
                    <CheckCircle2 size={20} color="var(--success)" />
                  </div>
                </div>
                <h3 style={{ fontSize: '1.85rem', fontWeight: 800 }}>{metrics.completedOrdersCount}</h3>
                <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '0.5rem' }}>Συνολικά σήμερα (non-archived)</p>
              </div>
            </div>

            {/* Active Orders List */}
            <div className="glass-card" style={{ padding: '2rem' }}>
              <h2 style={{ fontSize: '1.25rem', fontWeight: 800, marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <div style={{ width: '8px', height: '8px', borderRadius: '50%', background: 'var(--accent-cyan)', animation: 'pulse 1.5s infinite' }} />
                <span>Ενεργά Τραπέζια & Παραγγελίες</span>
              </h2>

              {activeOrders.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '3rem 1rem', color: 'var(--text-secondary)' }}>
                  <LayoutDashboard size={48} style={{ opacity: 0.2, marginBottom: '1rem' }} />
                  <p>Δεν υπάρχουν ενεργές παραγγελίες αυτή τη στιγμή.</p>
                </div>
              ) : (
                <div className="table-container" style={{ marginTop: '0' }}>
                  <table>
                    <thead>
                      <tr>
                        <th>Τραπέζι</th>
                        <th>Ζώνη</th>
                        <th>Σερβιτόρος</th>
                        <th>Σύνολο</th>
                        <th>Ώρα Καταχώρησης</th>
                        <th>Ενέργειες</th>
                      </tr>
                    </thead>
                    <tbody>
                      {activeOrders.map((ord) => (
                        <tr key={ord.id}>
                          <td style={{ fontWeight: 700, color: 'var(--accent-cyan)' }}>{ord.table_id}</td>
                          <td>
                            <span className="badge" style={{ background: 'rgba(255,255,255,0.05)', color: 'var(--text-primary)' }}>
                              {ord.zone}
                            </span>
                          </td>
                          <td>{ord.waiter_name}</td>
                          <td style={{ fontWeight: 600 }}>{formatPrice(ord.total_price)}</td>
                          <td style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                            {new Date(ord.created_at).toLocaleTimeString('el-GR', { hour: '2-digit', minute: '2-digit' })}
                          </td>
                          <td>
                            <button className="btn btn-secondary" style={{ padding: '0.4rem 0.8rem', fontSize: '0.8rem' }} onClick={() => setSelectedOrder(ord)}>
                              Λεπτομέρειες / Έκδοση
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        )}

        {activeTab === 'products' && (
          <div className="animate-fade-in">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
              <div>
                <h1 style={{ fontSize: '2rem', fontWeight: 800, letterSpacing: '-0.02em' }}>Προϊόντα & Κατηγορίες</h1>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem' }}>Διαχείριση καταλόγου, τιμών και διαθεσιμότητας</p>
              </div>
              <div>
                {prodTab === 'items' ? (
                  <button className="btn btn-primary" onClick={() => { setEditingProduct(null); setProductForm({ name: '', price: '', category_id: categories[0]?.id?.toString() || '', is_available: true }); setProductFormOpen(true); }}>
                    <Plus size={16} />
                    <span>Νέο Προϊόν</span>
                  </button>
                ) : (
                  <button className="btn btn-primary" onClick={() => setCategoryFormOpen(true)}>
                    <Plus size={16} />
                    <span>Νέα Κατηγορία</span>
                  </button>
                )}
              </div>
            </div>

            <div className="tabs">
              <button className={`tab ${prodTab === 'items' ? 'active' : ''}`} onClick={() => setProdTab('items')}>
                Προϊόντα
              </button>
              <button className={`tab ${prodTab === 'categories' ? 'active' : ''}`} onClick={() => setProdTab('categories')}>
                Κατηγορίες
              </button>
            </div>

            {prodTab === 'items' ? (
              <div className="glass-card">
                {products.length === 0 ? (
                  <p style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-secondary)' }}>Δεν βρέθηκαν προϊόντα.</p>
                ) : (
                  <div className="table-container" style={{ marginTop: 0 }}>
                    <table>
                      <thead>
                        <tr>
                          <th>Όνομα</th>
                          <th>Κατηγορία</th>
                          <th>Τιμή</th>
                          <th>Διαθεσιμότητα</th>
                          <th style={{ textAlign: 'right' }}>Ενέργειες</th>
                        </tr>
                      </thead>
                      <tbody>
                        {products.map((prod) => (
                          <tr key={prod.id}>
                            <td style={{ fontWeight: 600 }}>{prod.name}</td>
                            <td>{prod.category_name}</td>
                            <td style={{ fontWeight: 600, color: 'var(--accent-cyan)' }}>{formatPrice(prod.price)}</td>
                            <td>
                              {prod.is_available === 1 || prod.is_available === true ? (
                                <span className="badge badge-success">Διαθέσιμο</span>
                              ) : (
                                <span className="badge badge-danger">Εξαντλήθηκε</span>
                              )}
                            </td>
                            <td style={{ textAlign: 'right' }}>
                              <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
                                <button className="btn btn-secondary" style={{ padding: '0.4rem', borderRadius: '8px' }} onClick={() => handleEditProductClick(prod)}>
                                  <Edit size={14} />
                                </button>
                                <button className="btn btn-danger" style={{ padding: '0.4rem', borderRadius: '8px' }} onClick={() => handleDeleteProduct(prod.id)}>
                                  <Trash2 size={14} />
                                </button>
                              </div>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            ) : (
              <div className="glass-card" style={{ maxWidth: '600px' }}>
                <div className="table-container" style={{ marginTop: 0 }}>
                  <table>
                    <thead>
                      <tr>
                        <th>Όνομα Κατηγορίας</th>
                        <th style={{ textAlign: 'right' }}>Ενέργειες</th>
                      </tr>
                    </thead>
                    <tbody>
                      {categories.map((cat) => (
                        <tr key={cat.id}>
                          <td style={{ fontWeight: 600 }}>{cat.name}</td>
                          <td style={{ textAlign: 'right' }}>
                            <button className="btn btn-danger" style={{ padding: '0.4rem', borderRadius: '8px' }} onClick={() => handleDeleteCategory(cat.id)}>
                              <Trash2 size={14} />
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </div>
        )}

        {activeTab === 'users' && (
          <div className="animate-fade-in">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
              <div>
                <h1 style={{ fontSize: '2rem', fontWeight: 800, letterSpacing: '-0.02em' }}>Διαχείριση Χρηστών</h1>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem' }}>Δημιουργία και διαχείριση σερβιτόρων και διαχειριστών</p>
              </div>
              <button className="btn btn-primary" onClick={() => setUserFormOpen(true)}>
                <Plus size={16} />
                <span>Νέος Χρήστης</span>
              </button>
            </div>

            <div className="glass-card" style={{ maxWidth: '800px' }}>
              <div className="table-container" style={{ marginTop: 0 }}>
                <table>
                  <thead>
                    <tr>
                      <th>Όνομα Χρήστη</th>
                      <th>Ρόλος</th>
                      <th>Ημ. Δημιουργίας</th>
                      <th style={{ textAlign: 'right' }}>Ενέργειες</th>
                    </tr>
                  </thead>
                  <tbody>
                    {usersList.map((usr) => (
                      <tr key={usr.id}>
                        <td style={{ fontWeight: 600 }}>{usr.username}</td>
                        <td>
                          <span className={`badge ${usr.role === 'admin' ? 'badge-warning' : 'badge-success'}`}>
                            {usr.role === 'admin' ? 'Admin' : 'Waiter'}
                          </span>
                        </td>
                        <td style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                          {new Date(usr.created_at).toLocaleDateString('el-GR')}
                        </td>
                        <td style={{ textAlign: 'right' }}>
                          {usr.id !== currentUser.id && (
                            <button className="btn btn-danger" style={{ padding: '0.4rem', borderRadius: '8px' }} onClick={() => handleDeleteUser(usr.id)}>
                              <Trash2 size={14} />
                            </button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}
      </main>

      {/* Selected Order Detail Modal */}
      {selectedOrder && (
        <div className="modal-overlay" onClick={() => setSelectedOrder(null)}>
          <div className="modal-content glass animate-fade-in" onClick={e => e.stopPropagation()} style={{ maxWidth: '600px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', borderBottom: '1px solid var(--border-glass)', paddingBottom: '1rem' }}>
              <div>
                <h3 style={{ fontSize: '1.5rem', fontWeight: 800 }}>
                  Παραγγελία Τραπέζι {selectedOrder.table_id}
                </h3>
                <span className="badge" style={{ background: 'rgba(255,255,255,0.05)', color: 'var(--text-primary)', marginTop: '0.25rem' }}>
                  Ζώνη: {selectedOrder.zone}
                </span>
              </div>
              <span className="badge badge-success">ΕΝΕΡΓΗ</span>
            </div>

            <div style={{ marginBottom: '1.5rem', maxHeight: '250px', overflowY: 'auto' }}>
              <h4 style={{ fontSize: '0.9rem', fontWeight: 700, color: 'var(--text-secondary)', textTransform: 'uppercase', marginBottom: '0.5rem' }}>
                Προϊόντα Παραγγελίας
              </h4>
              {selectedOrder.items && selectedOrder.items.map((it: any) => (
                <div key={it.id} style={{ display: 'flex', justifyContent: 'space-between', padding: '0.75rem 0', borderBottom: '1px solid rgba(255,255,255,0.02)' }}>
                  <div>
                    <div style={{ fontWeight: 600 }}>{it.product_name} <span style={{ color: 'var(--accent-cyan)' }}>x{it.quantity}</span></div>
                    {it.notes && (
                      <div style={{ fontSize: '0.8rem', color: 'var(--warning)', display: 'flex', alignItems: 'center', gap: '0.25rem', marginTop: '0.25rem' }}>
                        <MessageSquare size={12} />
                        <span>{it.notes}</span>
                      </div>
                    )}
                  </div>
                  <div style={{ fontWeight: 600 }}>{formatPrice(it.price * it.quantity)}</div>
                </div>
              ))}
            </div>

            {selectedOrder.notes && (
              <div className="glass-card" style={{ background: 'rgba(255,255,255,0.01)', padding: '1rem', marginBottom: '1.5rem' }}>
                <h4 style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--text-secondary)', textTransform: 'uppercase', marginBottom: '0.25rem' }}>
                  Γενικές Σημειώσεις
                </h4>
                <p style={{ fontSize: '0.9rem' }}>{selectedOrder.notes}</p>
              </div>
            )}

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '1rem 0', borderTop: '1px solid var(--border-glass)', marginBottom: '1.5rem' }}>
              <span style={{ fontWeight: 700, color: 'var(--text-secondary)' }}>Σύνολο Παραγγελίας:</span>
              <span style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--accent-cyan)' }}>
                {formatPrice(selectedOrder.total_price)}
              </span>
            </div>

            <div style={{ display: 'flex', gap: '1rem', justifyContent: 'flex-end' }}>
              <button className="btn btn-secondary" onClick={() => setSelectedOrder(null)}>Κλείσιμο</button>
              <button className="btn btn-danger" onClick={() => { setCancelModalOpen(true); }}>Ακύρωση Παραγγελίας</button>
              <button className="btn btn-primary" onClick={() => { setPaymentModalOpen(true); }}>Έκδοση Λογαριασμού</button>
            </div>
          </div>
        </div>
      )}

      {/* Payment Selection Modal */}
      {paymentModalOpen && (
        <div className="modal-overlay" onClick={() => setPaymentModalOpen(false)}>
          <div className="modal-content glass animate-fade-in" onClick={e => e.stopPropagation()} style={{ maxWidth: '400px' }}>
            <h3 style={{ fontSize: '1.25rem', fontWeight: 800, marginBottom: '1rem' }}>Τρόπος Πληρωμής</h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginBottom: '1.5rem' }}>
              Επιλέξτε τον τρόπο πληρωμής για την ολοκλήρωση της παραγγελίας του τραπεζιού {selectedOrder?.table_id}.
            </p>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <button className="btn btn-primary" style={{ background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)', boxShadow: 'none' }} onClick={() => handlePayment('cash')}>
                <Coins size={18} />
                <span>Μετρητά ({formatPrice(selectedOrder?.total_price || 0)})</span>
              </button>
              <button className="btn btn-primary" style={{ background: 'linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%)', boxShadow: 'none' }} onClick={() => handlePayment('card')}>
                <CreditCard size={18} />
                <span>Κάρτα ({formatPrice(selectedOrder?.total_price || 0)})</span>
              </button>
              <button className="btn btn-secondary" onClick={() => setPaymentModalOpen(false)}>Ακύρωση</button>
            </div>
          </div>
        </div>
      )}

      {/* Cancellation Reason Modal */}
      {cancelModalOpen && (
        <div className="modal-overlay" onClick={() => setCancelModalOpen(false)}>
          <div className="modal-content glass animate-fade-in" onClick={e => e.stopPropagation()} style={{ maxWidth: '400px' }}>
            <h3 style={{ fontSize: '1.25rem', fontWeight: 800, marginBottom: '1rem' }}>Λόγος Ακύρωσης</h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginBottom: '1rem' }}>
              Παρακαλώ εισάγετε το λόγο ακύρωσης της παραγγελίας.
            </p>
            <div className="input-group">
              <input 
                type="text" 
                className="input-field" 
                placeholder="π.χ. Λάθος καταχώρηση" 
                value={cancellationReason}
                onChange={e => setCancellationReason(e.target.value)}
                required
              />
            </div>
            <div style={{ display: 'flex', gap: '1rem', justifyContent: 'flex-end', marginTop: '1.5rem' }}>
              <button className="btn btn-secondary" onClick={() => setCancelModalOpen(false)}>Πίσω</button>
              <button className="btn btn-danger" onClick={handleCancelOrder}>Ακύρωση Παραγγελίας</button>
            </div>
          </div>
        </div>
      )}

      {/* Day Closure Modal */}
      {closureModalOpen && closureReport && (
        <div className="modal-overlay" onClick={() => setClosureModalOpen(false)}>
          <div className="modal-content glass animate-fade-in" onClick={e => e.stopPropagation()} style={{ maxWidth: '550px' }}>
            <h3 style={{ fontSize: '1.5rem', fontWeight: 800, marginBottom: '1.5rem', borderBottom: '1px solid var(--border-glass)', paddingBottom: '0.5rem' }}>
              Αναφορά Κλεισίματος Ημέρας
            </h3>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1.5rem' }}>
              <div className="glass-card" style={{ padding: '1rem', background: 'rgba(255,255,255,0.01)' }}>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', fontWeight: 600 }}>ΣΥΝΟΛΙΚΟΣ ΤΖΙΡΟΣ</span>
                <h4 style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--success)' }}>{formatPrice(closureReport.totalRevenue)}</h4>
              </div>
              <div className="glass-card" style={{ padding: '1rem', background: 'rgba(255,255,255,0.01)' }}>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', fontWeight: 600 }}>ΣΥΝΟΛΟ ΠΑΡΑΓΓΕΛΙΩΝ</span>
                <h4 style={{ fontSize: '1.5rem', fontWeight: 800 }}>{closureReport.ordersCount}</h4>
              </div>
            </div>

            <div style={{ marginBottom: '1.5rem' }}>
              <h4 style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', fontWeight: 700, textTransform: 'uppercase', marginBottom: '0.5rem' }}>
                Ανάλυση Πληρωμών
              </h4>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.5rem 0', borderBottom: '1px solid rgba(255,255,255,0.02)' }}>
                <span>Μετρητά:</span>
                <span style={{ fontWeight: 600 }}>{formatPrice(closureReport.cashRevenue)}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.5rem 0', borderBottom: '1px solid rgba(255,255,255,0.02)' }}>
                <span>Κάρτες:</span>
                <span style={{ fontWeight: 600 }}>{formatPrice(closureReport.cardRevenue)}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.5rem 0', borderBottom: '1px solid rgba(255,255,255,0.02)', color: 'var(--danger)' }}>
                <span>Ακυρωμένες Παραγγελίες:</span>
                <span style={{ fontWeight: 600 }}>{closureReport.cancelledOrdersCount}</span>
              </div>
            </div>

            {Object.keys(closureReport.waiterStats).length > 0 && (
              <div style={{ marginBottom: '1.5rem' }}>
                <h4 style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', fontWeight: 700, textTransform: 'uppercase', marginBottom: '0.5rem' }}>
                  Πωλήσεις ανά Σερβιτόρο
                </h4>
                {Object.entries(closureReport.waiterStats).map(([name, stat]: any) => (
                  <div key={name} style={{ display: 'flex', justifyContent: 'space-between', padding: '0.5rem 0', borderBottom: '1px solid rgba(255,255,255,0.02)' }}>
                    <span>{name}:</span>
                    <span style={{ fontWeight: 600 }}>{formatPrice(stat.revenue)} ({stat.orders} παρ.)</span>
                  </div>
                ))}
              </div>
            )}

            <div style={{ display: 'flex', gap: '1rem', justifyContent: 'flex-end', borderTop: '1px solid var(--border-glass)', paddingTop: '1.5rem' }}>
              <button className="btn btn-secondary" onClick={() => setClosureModalOpen(false)}>Ακύρωση</button>
              <button className="btn btn-primary" onClick={handlePerformClosure}>Οριστικοποίηση Κλεισίματος</button>
            </div>
          </div>
        </div>
      )}

      {/* Product Form Modal */}
      {productFormOpen && (
        <div className="modal-overlay" onClick={() => setProductFormOpen(false)}>
          <div className="modal-content glass animate-fade-in" onClick={e => e.stopPropagation()}>
            <h3 style={{ fontSize: '1.25rem', fontWeight: 800, marginBottom: '1.5rem' }}>
              {editingProduct ? 'Επεξεργασία Προϊόντος' : 'Προσθήκη Νέου Προϊόντος'}
            </h3>
            <form onSubmit={handleSaveProduct}>
              <div className="input-group">
                <label>Όνομα προϊόντος</label>
                <input 
                  type="text" 
                  className="input-field" 
                  value={productForm.name} 
                  onChange={e => setProductForm({ ...productForm, name: e.target.value })}
                  required 
                />
              </div>

              <div className="input-group">
                <label>Τιμή (€)</label>
                <input 
                  type="number" 
                  step="0.01" 
                  className="input-field" 
                  value={productForm.price} 
                  onChange={e => setProductForm({ ...productForm, price: e.target.value })}
                  required 
                />
              </div>

              <div className="input-group">
                <label>Κατηγορία</label>
                <select 
                  className="input-field" 
                  value={productForm.category_id} 
                  onChange={e => setProductForm({ ...productForm, category_id: e.target.value })}
                  required
                >
                  <option value="">Επιλέξτε Κατηγορία</option>
                  {categories.map(c => (
                    <option key={c.id} value={c.id}>{c.name}</option>
                  ))}
                </select>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', margin: '1.5rem 0' }}>
                <input 
                  type="checkbox" 
                  id="isAvailable" 
                  checked={productForm.is_available} 
                  onChange={e => setProductForm({ ...productForm, is_available: e.target.checked })} 
                />
                <label htmlFor="isAvailable" style={{ fontWeight: 600, fontSize: '0.9rem', cursor: 'pointer' }}>
                  Διαθέσιμο για παραγγελία
                </label>
              </div>

              <div style={{ display: 'flex', gap: '1rem', justifyContent: 'flex-end', marginTop: '2rem' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setProductFormOpen(false)}>Ακύρωση</button>
                <button type="submit" className="btn btn-primary">Αποθήκευση</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Category Form Modal */}
      {categoryFormOpen && (
        <div className="modal-overlay" onClick={() => setCategoryFormOpen(false)}>
          <div className="modal-content glass animate-fade-in" onClick={e => e.stopPropagation()}>
            <h3 style={{ fontSize: '1.25rem', fontWeight: 800, marginBottom: '1.5rem' }}>Νέα Κατηγορία</h3>
            <form onSubmit={handleSaveCategory}>
              <div className="input-group">
                <label>Όνομα Κατηγορίας</label>
                <input 
                  type="text" 
                  className="input-field" 
                  placeholder="π.χ. Ορεκτικά"
                  value={newCategoryName} 
                  onChange={e => setNewCategoryName(e.target.value)}
                  required 
                />
              </div>

              <div style={{ display: 'flex', gap: '1rem', justifyContent: 'flex-end', marginTop: '2rem' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setCategoryFormOpen(false)}>Ακύρωση</button>
                <button type="submit" className="btn btn-primary">Προσθήκη</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* User Form Modal */}
      {userFormOpen && (
        <div className="modal-overlay" onClick={() => setUserFormOpen(false)}>
          <div className="modal-content glass animate-fade-in" onClick={e => e.stopPropagation()}>
            <h3 style={{ fontSize: '1.25rem', fontWeight: 800, marginBottom: '1.5rem' }}>Δημιουργία Νέου Χρήστη</h3>
            <form onSubmit={handleSaveUser}>
              <div className="input-group">
                <label>Όνομα χρήστη (Username)</label>
                <input 
                  type="text" 
                  className="input-field" 
                  value={userForm.username} 
                  onChange={e => setUserForm({ ...userForm, username: e.target.value })}
                  required 
                />
              </div>

              <div className="input-group">
                <label>Κωδικός πρόσβασης (Password)</label>
                <input 
                  type="password" 
                  className="input-field" 
                  value={userForm.password} 
                  onChange={e => setUserForm({ ...userForm, password: e.target.value })}
                  required 
                />
              </div>

              <div className="input-group">
                <label>Ρόλος συστήματος</label>
                <select 
                  className="input-field" 
                  value={userForm.role} 
                  onChange={e => setUserForm({ ...userForm, role: e.target.value })}
                  required
                >
                  <option value="waiter">Σερβιτόρος (Waiter)</option>
                  <option value="admin">Διαχειριστής (Admin)</option>
                </select>
              </div>

              <div style={{ display: 'flex', gap: '1rem', justifyContent: 'flex-end', marginTop: '2rem' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setUserFormOpen(false)}>Ακύρωση</button>
                <button type="submit" className="btn btn-primary">Δημιουργία</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
