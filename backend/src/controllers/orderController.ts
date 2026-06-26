import { Response } from 'express';
import { AuthRequest } from '../middleware/auth';
import { getDb } from '../config/db';
import { broadcast } from '../services/websocket';

// Helper to fetch full order details (with items)
async function getOrderDetails(orderId: number, db: any) {
  const orderResult = await db.query(`
    SELECT o.*, u.username as waiter_name 
    FROM orders o
    JOIN users u ON o.waiter_id = u.id
    WHERE o.id = $1
  `, [orderId]);

  const order = orderResult.rows[0];
  if (!order) return null;

  const itemsResult = await db.query(`
    SELECT oi.*, p.name as product_name, c.name as category_name
    FROM order_items oi
    JOIN products p ON oi.product_id = p.id
    JOIN categories c ON p.category_id = c.id
    WHERE oi.order_id = $1
  `, [orderId]);

  return {
    ...order,
    is_archived: !!order.is_archived,
    items: itemsResult.rows
  };
}

export async function submitOrder(req: AuthRequest, res: Response) {
  try {
    const { table_id, zone, notes, items } = req.body;
    const waiterId = req.user?.id;

    if (!table_id || !zone || !waiterId || !items || !Array.isArray(items) || items.length === 0) {
      return res.status(400).json({ error: 'table_id, zone, and a non-empty items list are required' });
    }

    const db = getDb();

    // Check if this table already has an active order
    const activeOrder = await db.query(
      'SELECT id FROM orders WHERE table_id = $1 AND zone = $2 AND status = $3',
      [table_id, zone, 'active']
    );
    if (activeOrder.rows.length > 0) {
      return res.status(400).json({ error: `Table ${table_id} in zone ${zone} already has an active order` });
    }

    // Get a dedicated connection from the pool for transaction
    const client = await db.connect();

    try {
      await client.query('BEGIN');

      // 1. Create order record
      const orderResult = await client.query(
        'INSERT INTO orders (table_id, zone, status, waiter_id, notes) VALUES ($1, $2, $3, $4, $5) RETURNING id',
        [table_id, zone, 'active', waiterId, notes || null]
      );
      const orderId = orderResult.rows[0].id;

      let totalPrice = 0;

      // 2. Insert items and sum prices
      for (const item of items) {
        const { product_id, quantity, item_notes } = item;
        if (!product_id || !quantity || quantity <= 0) {
          throw new Error('Invalid product_id or quantity');
        }

        const productResult = await client.query('SELECT price, is_available FROM products WHERE id = $1', [product_id]);
        const product = productResult.rows[0];
        if (!product) {
          throw new Error(`Product with ID ${product_id} not found`);
        }
        if (!product.is_available) {
          throw new Error(`Product with ID ${product_id} is currently unavailable`);
        }

        const itemTotal = product.price * quantity;
        totalPrice += itemTotal;

        await client.query(
          'INSERT INTO order_items (order_id, product_id, quantity, price, notes) VALUES ($1, $2, $3, $4, $5)',
          [orderId, product_id, quantity, product.price, item_notes || null]
        );
      }

      // 3. Update order total price
      await client.query('UPDATE orders SET total_price = $1 WHERE id = $2', [totalPrice, orderId]);

      // 4. Log status change
      await client.query(
        'INSERT INTO order_status_logs (order_id, old_status, new_status, changed_by, notes) VALUES ($1, $2, $3, $4, $5)',
        [orderId, null, 'active', waiterId, 'Order submitted']
      );

      await client.query('COMMIT');
      client.release();

      // Fetch completed order structure
      const fullOrder = await getOrderDetails(orderId, db);

      // Broadcast new order and table status change
      broadcast('ORDER_SUBMITTED', fullOrder);
      broadcast('TABLE_STATUS_CHANGED', { table_id, zone, status: 'active', order_id: orderId });

      return res.status(201).json(fullOrder);
    } catch (err: any) {
      await client.query('ROLLBACK');
      client.release();
      return res.status(400).json({ error: err.message || 'Transaction failed' });
    }
  } catch (error) {
    console.error('Submit order error:', error);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

export async function modifyOrder(req: AuthRequest, res: Response) {
  try {
    const { id } = req.params;
    const { items, notes } = req.body;
    const waiterId = req.user?.id!;

    if (!items || !Array.isArray(items)) {
      return res.status(400).json({ error: 'Items list is required' });
    }

    const db = getDb();
    const orderResult = await db.query('SELECT * FROM orders WHERE id = $1', [id]);
    const order = orderResult.rows[0];

    if (!order) {
      return res.status(404).json({ error: 'Order not found' });
    }

    if (order.status !== 'active') {
      return res.status(400).json({ error: 'Only active orders can be modified' });
    }

    if (order.is_archived === true || order.is_archived === 1) {
      return res.status(400).json({ error: 'Order is archived and locked' });
    }

    const client = await db.connect();

    try {
      await client.query('BEGIN');

      // 1. Delete existing items
      await client.query('DELETE FROM order_items WHERE order_id = $1', [id]);

      let totalPrice = 0;

      // 2. Insert new items and sum prices
      for (const item of items) {
        const { product_id, quantity, item_notes } = item;
        if (!product_id || !quantity || quantity <= 0) {
          throw new Error('Invalid product_id or quantity');
        }

        const productResult = await client.query('SELECT price, is_available FROM products WHERE id = $1', [product_id]);
        const product = productResult.rows[0];
        if (!product) {
          throw new Error(`Product with ID ${product_id} not found`);
        }

        const itemTotal = product.price * quantity;
        totalPrice += itemTotal;

        await client.query(
          'INSERT INTO order_items (order_id, product_id, quantity, price, notes) VALUES ($1, $2, $3, $4, $5)',
          [id, product_id, quantity, product.price, item_notes || null]
        );
      }

      // 3. Update order price and notes
      await client.query(
        'UPDATE orders SET total_price = $1, notes = $2, updated_at = CURRENT_TIMESTAMP WHERE id = $3',
        [totalPrice, notes !== undefined ? notes : order.notes, id]
      );

      // 4. Log status log (as modified)
      await client.query(
        'INSERT INTO order_status_logs (order_id, old_status, new_status, changed_by, notes) VALUES ($1, $2, $3, $4, $5)',
        [id, 'active', 'active', waiterId, 'Order items modified']
      );

      await client.query('COMMIT');
      client.release();

      const fullOrder = await getOrderDetails(Number(id), db);
      broadcast('ORDER_MODIFIED', fullOrder);

      return res.status(200).json(fullOrder);
    } catch (err: any) {
      await client.query('ROLLBACK');
      client.release();
      return res.status(400).json({ error: err.message || 'Transaction failed' });
    }
  } catch (error) {
    console.error('Modify order error:', error);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

export async function cancelOrder(req: AuthRequest, res: Response) {
  try {
    const { id } = req.params;
    const { reason } = req.body;
    const userId = req.user?.id!;

    const db = getDb();
    const orderResult = await db.query('SELECT * FROM orders WHERE id = $1', [id]);
    const order = orderResult.rows[0];

    if (!order) {
      return res.status(404).json({ error: 'Order not found' });
    }

    if (order.status !== 'active') {
      return res.status(400).json({ error: 'Only active orders can be cancelled' });
    }

    if (order.is_archived === true || order.is_archived === 1) {
      return res.status(400).json({ error: 'Order is archived and locked' });
    }

    const client = await db.connect();

    try {
      await client.query('BEGIN');

      await client.query(
        "UPDATE orders SET status = $1, closed_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = $2",
        ['cancelled', id]
      );

      await client.query(
        'INSERT INTO order_status_logs (order_id, old_status, new_status, changed_by, notes) VALUES ($1, $2, $3, $4, $5)',
        [id, 'active', 'cancelled', userId, reason || 'Order cancelled']
      );

      await client.query('COMMIT');
      client.release();

      const fullOrder = await getOrderDetails(Number(id), db);
      
      broadcast('ORDER_MODIFIED', fullOrder);
      broadcast('TABLE_STATUS_CHANGED', { table_id: order.table_id, zone: order.zone, status: 'free', order_id: null });

      return res.status(200).json(fullOrder);
    } catch (err: any) {
      await client.query('ROLLBACK');
      client.release();
      return res.status(400).json({ error: err.message || 'Transaction failed' });
    }
  } catch (error) {
    console.error('Cancel order error:', error);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

export async function issueBill(req: AuthRequest, res: Response) {
  try {
    const { id } = req.params;
    const { payment_method } = req.body; // 'cash' or 'card'
    const userId = req.user?.id!;

    if (!payment_method || (payment_method !== 'cash' && payment_method !== 'card')) {
      return res.status(400).json({ error: 'Payment method must be cash or card' });
    }

    const db = getDb();
    const orderResult = await db.query('SELECT * FROM orders WHERE id = $1', [id]);
    const order = orderResult.rows[0];

    if (!order) {
      return res.status(404).json({ error: 'Order not found' });
    }

    if (order.status !== 'active') {
      return res.status(400).json({ error: 'Only active orders can be paid/billed' });
    }

    if (order.is_archived === true || order.is_archived === 1) {
      return res.status(400).json({ error: 'Order is archived and locked' });
    }

    const client = await db.connect();

    try {
      await client.query('BEGIN');

      await client.query(
        "UPDATE orders SET status = $1, payment_method = $2, closed_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = $3",
        ['paid', payment_method, id]
      );

      await client.query(
        'INSERT INTO order_status_logs (order_id, old_status, new_status, changed_by, notes) VALUES ($1, $2, $3, $4, $5)',
        [id, 'active', 'paid', userId, `Bill paid by ${payment_method}`]
      );

      await client.query('COMMIT');
      client.release();

      const fullOrder = await getOrderDetails(Number(id), db);

      broadcast('ORDER_MODIFIED', fullOrder);
      broadcast('TABLE_STATUS_CHANGED', { table_id: order.table_id, zone: order.zone, status: 'free', order_id: null });

      return res.status(200).json(fullOrder);
    } catch (err: any) {
      await client.query('ROLLBACK');
      client.release();
      return res.status(400).json({ error: err.message || 'Transaction failed' });
    }
  } catch (error) {
    console.error('Issue bill error:', error);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

export async function getActiveOrders(req: AuthRequest, res: Response) {
  try {
    const db = getDb();
    const activeOrders = await db.query("SELECT id FROM orders WHERE status = 'active'");
    
    const details = [];
    for (const row of activeOrders.rows) {
      const full = await getOrderDetails(row.id, db);
      if (full) details.push(full);
    }

    return res.status(200).json(details);
  } catch (error) {
    console.error('Error fetching active orders:', error);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

export async function getOrderHistory(req: AuthRequest, res: Response) {
  try {
    const { startDate, endDate, status, waiterId, isArchived } = req.query;
    const db = getDb();

    let query = `
      SELECT o.id FROM orders o
      WHERE 1=1
    `;
    const params: any[] = [];

    if (startDate) {
      params.push(startDate);
      query += ` AND o.created_at >= $${params.length}`;
    }
    if (endDate) {
      params.push(endDate);
      query += ` AND o.created_at <= $${params.length}`;
    }
    if (status) {
      params.push(status);
      query += ` AND o.status = $${params.length}`;
    }
    if (waiterId) {
      params.push(waiterId);
      query += ` AND o.waiter_id = $${params.length}`;
    }
    if (isArchived !== undefined) {
      params.push(isArchived === 'true');
      query += ` AND o.is_archived = $${params.length}`;
    }

    query += ' ORDER BY o.created_at DESC';

    const orderRows = await db.query(query, params);
    const details = [];
    
    for (const row of orderRows.rows) {
      const full = await getOrderDetails(row.id, db);
      if (full) details.push(full);
    }

    return res.status(200).json(details);
  } catch (error) {
    console.error('Error fetching order history:', error);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

// Get metrics / daily report
async function calculateClosureStats(db: any) {
  // Total closed today (status: paid or cancelled) that is NOT archived
  const activeClosureOrders = await db.query(`
    SELECT o.id, o.total_price, o.status, o.payment_method, o.waiter_id, u.username as waiter_name, o.closed_at
    FROM orders o
    JOIN users u ON o.waiter_id = u.id
    WHERE o.status IN ('paid', 'cancelled') AND o.is_archived = FALSE
  `);

  let totalRevenue = 0;
  let cashRevenue = 0;
  let cardRevenue = 0;
  let paidOrdersCount = 0;
  let cancelledOrdersCount = 0;

  const waiterStats: Record<string, { revenue: number, orders: number }> = {};
  const categoryStats: Record<string, { quantity: number, revenue: number }> = {};

  for (const ord of activeClosureOrders.rows) {
    if (ord.status === 'paid') {
      paidOrdersCount++;
      totalRevenue += ord.total_price;
      if (ord.payment_method === 'cash') cashRevenue += ord.total_price;
      if (ord.payment_method === 'card') cardRevenue += ord.total_price;

      // Waiter stats
      if (!waiterStats[ord.waiter_name]) {
        waiterStats[ord.waiter_name] = { revenue: 0, orders: 0 };
      }
      waiterStats[ord.waiter_name].revenue += ord.total_price;
      waiterStats[ord.waiter_name].orders += 1;

      // Category stats
      const items = await db.query(`
        SELECT oi.quantity, oi.price, c.name as category_name
        FROM order_items oi
        JOIN products p ON oi.product_id = p.id
        JOIN categories c ON p.category_id = c.id
        WHERE oi.order_id = $1
      `, [ord.id]);

      for (const it of items.rows) {
        const itemRev = it.quantity * it.price;
        if (!categoryStats[it.category_name]) {
          categoryStats[it.category_name] = { quantity: 0, revenue: 0 };
        }
        categoryStats[it.category_name].quantity += it.quantity;
        categoryStats[it.category_name].revenue += itemRev;
      }
    } else if (ord.status === 'cancelled') {
      cancelledOrdersCount++;
    }
  }

  return {
    totalRevenue,
    cashRevenue,
    cardRevenue,
    paidOrdersCount,
    cancelledOrdersCount,
    waiterStats,
    categoryStats,
    ordersCount: activeClosureOrders.rows.length
  };
}

export async function getDayClosureReport(req: AuthRequest, res: Response) {
  try {
    const db = getDb();
    const stats = await calculateClosureStats(db);
    return res.status(200).json(stats);
  } catch (error) {
    console.error('Error calculating day closure stats:', error);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

export async function performDayClosure(req: AuthRequest, res: Response) {
  try {
    const db = getDb();
    
    // Calculate final stats before locking
    const stats = await calculateClosureStats(db);

    // Lock them (archive)
    await db.query(
      "UPDATE orders SET is_archived = TRUE WHERE status IN ('paid', 'cancelled') AND is_archived = FALSE"
    );

    // Broadcast Day Closure event
    broadcast('DAY_CLOSED', stats);

    return res.status(200).json({
      message: 'Day closure completed and orders archived successfully',
      report: stats
    });
  } catch (error) {
    console.error('Error performing day closure:', error);
    return res.status(500).json({ error: 'Internal server error' });
  }
}
