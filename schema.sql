-- ==============================================================
-- RESTAURANT ORDERING SYSTEM - SUPABASE SQL SCHEMA & SEED SCRIPT
-- Copy and paste this script directly into the Supabase SQL Editor
-- ==============================================================

-- Clean up existing tables (reverse order of dependencies)
DROP TABLE IF EXISTS order_status_logs CASCADE;
DROP TABLE IF EXISTS order_items CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS products CASCADE;
DROP TABLE IF EXISTS categories CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- 1. Users Table
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Categories Table
CREATE TABLE categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Products Table
CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price DOUBLE PRECISION NOT NULL,
    category_id INTEGER NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
    is_available BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. Orders Table
CREATE TABLE orders (
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
);

-- 5. Order Items Table
CREATE TABLE order_items (
    id SERIAL PRIMARY KEY,
    order_id INTEGER NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id INTEGER NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    quantity INTEGER NOT NULL,
    price DOUBLE PRECISION NOT NULL,
    notes TEXT
);

-- 6. Order Status Logs Table
CREATE TABLE order_status_logs (
    id SERIAL PRIMARY KEY,
    order_id INTEGER NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    old_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    changed_by INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    notes TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================
-- DATABASE SEEDING
-- ==============================================================

-- Seed default users:
-- admin / adminpassword (bcrypt hash)
-- waiter1 / waiterpassword (bcrypt hash)
INSERT INTO users (username, password, role) VALUES 
('admin', '$2a$10$J.61lo7BfYN2JYIpmo07O.9Ne5Kj8Hat21QIs5CtBYTcHUvTi3qWu', 'admin'),
('waiter1', '$2a$10$5oH.QwXyf97z8cVNtxV4wefn4kFjJid0MGFAkeOu2p6DcF58t/OuK', 'waiter')
ON CONFLICT DO NOTHING;

-- Seed categories
INSERT INTO categories (name) VALUES 
('Καφέδες'), 
('Αναψυκτικά'), 
('Φαγητά'), 
('Γλυκά'), 
('Ποτά')
ON CONFLICT DO NOTHING;

-- Seed products
-- We use subqueries to get category IDs dynamically
INSERT INTO products (name, price, category_id, is_available) VALUES 
('Espresso', 2.0, (SELECT id FROM categories WHERE name = 'Καφέδες'), true),
('Cappuccino', 2.5, (SELECT id FROM categories WHERE name = 'Καφέδες'), true),
('Φραπέ', 2.0, (SELECT id FROM categories WHERE name = 'Καφέδες'), true),
('Freddo Espresso', 2.5, (SELECT id FROM categories WHERE name = 'Καφέδες'), true),
('Freddo Cappuccino', 3.0, (SELECT id FROM categories WHERE name = 'Καφέδες'), true),
('Coca Cola 330ml', 2.0, (SELECT id FROM categories WHERE name = 'Αναψυκτικά'), true),
('Πορτοκαλάδα 330ml', 2.0, (SELECT id FROM categories WHERE name = 'Αναψυκτικά'), true),
('Νερό 500ml', 0.5, (SELECT id FROM categories WHERE name = 'Αναψυκτικά'), true),
('Club Sandwich', 6.0, (SELECT id FROM categories WHERE name = 'Φαγητά'), true),
('Pizza Margarita', 8.5, (SELECT id FROM categories WHERE name = 'Φαγητά'), true),
('Burger Special', 9.0, (SELECT id FROM categories WHERE name = 'Φαγητά'), true),
('Σαλάτα Χωριάτικη', 6.5, (SELECT id FROM categories WHERE name = 'Φαγητά'), true),
('Σουφλέ Σοκολάτας', 5.0, (SELECT id FROM categories WHERE name = 'Γλυκά'), true),
('Waffle Special', 6.0, (SELECT id FROM categories WHERE name = 'Γλυκά'), true),
('Παγωτό 1 Μπάλα', 2.0, (SELECT id FROM categories WHERE name = 'Γλυκά'), true),
('Μπύρα Alfa 500ml', 3.5, (SELECT id FROM categories WHERE name = 'Ποτά'), true),
('Κρασί Ποτήρι', 4.0, (SELECT id FROM categories WHERE name = 'Ποτά'), true),
('Mojito', 7.5, (SELECT id FROM categories WHERE name = 'Ποτά'), true);
