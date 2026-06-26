import { Router } from 'express';
import { login, getProfile } from '../controllers/authController';
import { getUsers, createUser, updateUser, deleteUser } from '../controllers/userController';
import {
  getCategories, createCategory, deleteCategory,
  getProducts, createProduct, updateProduct, deleteProduct
} from '../controllers/productController';
import {
  submitOrder, modifyOrder, cancelOrder, issueBill,
  getActiveOrders, getOrderHistory, getDayClosureReport, performDayClosure
} from '../controllers/orderController';
import { authenticateToken, authorizeRole } from '../middleware/auth';

const router = Router();

// --- Auth Routes ---
router.post('/auth/login', login);
router.get('/auth/profile', authenticateToken, getProfile);

// --- User Management (Admin Only) ---
router.get('/users', authenticateToken, authorizeRole('admin'), getUsers);
router.post('/users', authenticateToken, authorizeRole('admin'), createUser);
router.put('/users/:id', authenticateToken, authorizeRole('admin'), updateUser);
router.delete('/users/:id', authenticateToken, authorizeRole('admin'), deleteUser);

// --- Categories ---
router.get('/categories', authenticateToken, getCategories);
router.post('/categories', authenticateToken, authorizeRole('admin'), createCategory);
router.delete('/categories/:id', authenticateToken, authorizeRole('admin'), deleteCategory);

// --- Products ---
router.get('/products', authenticateToken, getProducts);
router.post('/products', authenticateToken, authorizeRole('admin'), createProduct);
router.put('/products/:id', authenticateToken, authorizeRole('admin'), updateProduct);
router.delete('/products/:id', authenticateToken, authorizeRole('admin'), deleteProduct);

// --- Orders ---
router.post('/orders', authenticateToken, submitOrder);
router.put('/orders/:id', authenticateToken, modifyOrder);
router.post('/orders/:id/cancel', authenticateToken, cancelOrder);
router.post('/orders/:id/bill', authenticateToken, issueBill);
router.get('/orders/active', authenticateToken, getActiveOrders);
router.get('/orders/history', authenticateToken, getOrderHistory);

// --- Day Closure (Admin Only) ---
router.get('/orders/closure-report', authenticateToken, authorizeRole('admin'), getDayClosureReport);
router.post('/orders/closure', authenticateToken, authorizeRole('admin'), performDayClosure);

export default router;
