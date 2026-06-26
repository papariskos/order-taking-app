import { Pool } from 'pg';
import bcrypt from 'bcryptjs';

let pool: Pool;

export function getDb() {
  if (!pool) {
    const connectionString = process.env.DATABASE_URL || 'postgresql://postgres:postgres@localhost:5432/restaurant';
    console.log('Initializing PostgreSQL Pool with connection string...');
    pool = new Pool({
      connectionString,
      ssl: connectionString.includes('supabase.co') ? { rejectUnauthorized: false } : false
    });
  }
  return pool;
}

export async function initDb() {
  const db = getDb();

  // Create Users table
  await db.query(`
    CREATE TABLE IF NOT EXISTS users (
      id SERIAL PRIMARY KEY,
      username VARCHAR(255) UNIQUE NOT NULL,
      password VARCHAR(255) NOT NULL,
      role VARCHAR(50) NOT NULL,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
  `);

  // Create Categories table
  await db.query(`
    CREATE TABLE IF NOT EXISTS categories (
      id SERIAL PRIMARY KEY,
      name VARCHAR(255) UNIQUE NOT NULL,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
  `);

  // Create Products table
  await db.query(`
    CREATE TABLE IF NOT EXISTS products (
      id SERIAL PRIMARY KEY,
      name VARCHAR(255) NOT NULL,
      price DOUBLE PRECISION NOT NULL,
      category_id INTEGER NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
      is_available BOOLEAN DEFAULT TRUE,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
  `);

  // Create Orders table
  await db.query(`
    CREATE TABLE IF NOT EXISTS orders (
      id SERIAL PRIMARY KEY,
      table_id VARCHAR(50) NOT NULL,
      zone VARCHAR(100) NOT NULL,
      status VARCHAR(50) NOT NULL,
      waiter_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
      total_price DOUBLE PRECISION DEFAULT 0.0,
      payment_method VARCHAR(50),
      notes TEXT,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      closed_at TIMESTAMP,
      is_archived BOOLEAN DEFAULT FALSE
    )
  `);

  // Create Order Items table
  await db.query(`
    CREATE TABLE IF NOT EXISTS order_items (
      id SERIAL PRIMARY KEY,
      order_id INTEGER NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
      product_id INTEGER NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
      quantity INTEGER NOT NULL,
      price DOUBLE PRECISION NOT NULL,
      notes TEXT
    )
  `);

  // Create Order Status Logs table
  await db.query(`
    CREATE TABLE IF NOT EXISTS order_status_logs (
      id SERIAL PRIMARY KEY,
      order_id INTEGER NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
      old_status VARCHAR(50),
      new_status VARCHAR(50) NOT NULL,
      changed_by INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
      notes TEXT,
      timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
  `);

  // Seed default admin user
  const userCheck = await db.query('SELECT COUNT(*) as count FROM users WHERE role = $1', ['admin']);
  if (parseInt(userCheck.rows[0].count) === 0) {
    const adminPasswordHash = await bcrypt.hash('adminpassword', 10);
    await db.query(
      'INSERT INTO users (username, password, role) VALUES ($1, $2, $3)',
      ['admin', adminPasswordHash, 'admin']
    );

    const waiterPasswordHash = await bcrypt.hash('waiterpassword', 10);
    await db.query(
      'INSERT INTO users (username, password, role) VALUES ($1, $2, $3)',
      ['waiter1', waiterPasswordHash, 'waiter']
    );
    console.log('Seeded default users: admin/adminpassword and waiter1/waiterpassword');
  }

  // Seed categories
  const categoryCheck = await db.query('SELECT COUNT(*) as count FROM categories');
  if (parseInt(categoryCheck.rows[0].count) === 0) {
    const categories = ['Καφέδες', 'Αναψυκτικά', 'Φαγητά', 'Γλυκά', 'Ποτά'];
    for (const cat of categories) {
      await db.query('INSERT INTO categories (name) VALUES ($1) ON CONFLICT DO NOTHING', [cat]);
    }
    console.log('Seeded categories');

    // Seed products
    const catMap = await db.query('SELECT id, name FROM categories');
    const findCatId = (name: string) => catMap.rows.find(c => c.name === name)?.id || 1;

    const products = [
      { name: 'Espresso', price: 2.0, category_id: findCatId('Καφέδες') },
      { name: 'Cappuccino', price: 2.5, category_id: findCatId('Καφέδες') },
      { name: 'Φραπέ', price: 2.0, category_id: findCatId('Καφέδες') },
      { name: 'Freddo Espresso', price: 2.5, category_id: findCatId('Καφέδες') },
      { name: 'Freddo Cappuccino', price: 3.0, category_id: findCatId('Καφέδες') },
      { name: 'Coca Cola 330ml', price: 2.0, category_id: findCatId('Αναψυκτικά') },
      { name: 'Πορτοκαλάδα 330ml', price: 2.0, category_id: findCatId('Αναψυκτικά') },
      { name: 'Νερό 500ml', price: 0.5, category_id: findCatId('Αναψυκτικά') },
      { name: 'Club Sandwich', price: 6.0, category_id: findCatId('Φαγητά') },
      { name: 'Pizza Margarita', price: 8.5, category_id: findCatId('Φαγητά') },
      { name: 'Burger Special', price: 9.0, category_id: findCatId('Φαγητά') },
      { name: 'Σαλάτα Χωριάτικη', price: 6.5, category_id: findCatId('Φαγητά') },
      { name: 'Σουφλέ Σοκολάτας', price: 5.0, category_id: findCatId('Γλυκά') },
      { name: 'Waffle Special', price: 6.0, category_id: findCatId('Γλυκά') },
      { name: 'Παγωτό 1 Μπάλα', price: 2.0, category_id: findCatId('Γλυκά') },
      { name: 'Μπύρα Alfa 500ml', price: 3.5, category_id: findCatId('Ποτά') },
      { name: 'Κρασί Ποτήρι', price: 4.0, category_id: findCatId('Ποτά') },
      { name: 'Mojito', price: 7.5, category_id: findCatId('Ποτά') }
    ];

    for (const prod of products) {
      await db.query(
        'INSERT INTO products (name, price, category_id, is_available) VALUES ($1, $2, $3, true)',
        [prod.name, prod.price, prod.category_id]
      );
    }
    console.log('Seeded products');
  }
}
