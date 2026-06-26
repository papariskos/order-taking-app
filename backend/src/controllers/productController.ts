import { Response } from 'express';
import { AuthRequest } from '../middleware/auth';
import { getDb } from '../config/db';

// --- Categories ---

export async function getCategories(req: AuthRequest, res: Response) {
  try {
    const db = getDb();
    const result = await db.query('SELECT * FROM categories ORDER BY name ASC');
    return res.status(200).json(result.rows);
  } catch (error) {
    console.error('Error fetching categories:', error);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

export async function createCategory(req: AuthRequest, res: Response) {
  try {
    const { name } = req.body;
    if (!name) {
      return res.status(400).json({ error: 'Category name is required' });
    }

    const db = getDb();
    const existing = await db.query('SELECT id FROM categories WHERE name = $1', [name]);
    if (existing.rows.length > 0) {
      return res.status(400).json({ error: 'Category name already exists' });
    }

    const result = await db.query('INSERT INTO categories (name) VALUES ($1) RETURNING id', [name]);
    return res.status(201).json({ id: result.rows[0].id, name });
  } catch (error) {
    console.error('Error creating category:', error);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

export async function deleteCategory(req: AuthRequest, res: Response) {
  try {
    const { id } = req.params;
    const db = getDb();

    // Check if products are tied to this category
    const linkedProducts = await db.query('SELECT COUNT(*) as count FROM products WHERE category_id = $1', [id]);
    if (linkedProducts.rows.length > 0 && parseInt(linkedProducts.rows[0].count) > 0) {
      return res.status(400).json({ error: 'Cannot delete category containing products. Reassign or delete products first.' });
    }

    const result = await db.query('DELETE FROM categories WHERE id = $1', [id]);
    if (result.rowCount === 0) {
      return res.status(404).json({ error: 'Category not found' });
    }

    return res.status(200).json({ message: 'Category deleted successfully' });
  } catch (error) {
    console.error('Error deleting category:', error);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

// --- Products ---

export async function getProducts(req: AuthRequest, res: Response) {
  try {
    const db = getDb();
    const result = await db.query(`
      SELECT p.*, c.name as category_name 
      FROM products p 
      JOIN categories c ON p.category_id = c.id
      ORDER BY c.name ASC, p.name ASC
    `);
    return res.status(200).json(result.rows);
  } catch (error) {
    console.error('Error fetching products:', error);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

export async function createProduct(req: AuthRequest, res: Response) {
  try {
    const { name, price, category_id, is_available } = req.body;
    if (!name || price === undefined || category_id === undefined) {
      return res.status(400).json({ error: 'Name, price, and category_id are required' });
    }

    const db = getDb();

    // Check category exists
    const category = await db.query('SELECT id FROM categories WHERE id = $1', [category_id]);
    if (category.rows.length === 0) {
      return res.status(400).json({ error: 'Invalid category_id' });
    }

    const result = await db.query(
      'INSERT INTO products (name, price, category_id, is_available) VALUES ($1, $2, $3, $4) RETURNING id',
      [
        name,
        price,
        category_id,
        is_available !== undefined ? !!is_available : true
      ]
    );

    return res.status(201).json({
      id: result.rows[0].id,
      name,
      price,
      category_id,
      is_available: is_available !== undefined ? !!is_available : true
    });
  } catch (error) {
    console.error('Error creating product:', error);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

export async function updateProduct(req: AuthRequest, res: Response) {
  try {
    const { id } = req.params;
    const { name, price, category_id, is_available } = req.body;

    const db = getDb();
    const product = await db.query('SELECT id FROM products WHERE id = $1', [id]);
    if (product.rows.length === 0) {
      return res.status(404).json({ error: 'Product not found' });
    }

    if (category_id !== undefined) {
      const category = await db.query('SELECT id FROM categories WHERE id = $1', [category_id]);
      if (category.rows.length === 0) {
        return res.status(400).json({ error: 'Invalid category_id' });
      }
    }

    await db.query(
      `UPDATE products 
       SET name = COALESCE($1::varchar, name),
           price = COALESCE($2::double precision, price),
           category_id = COALESCE($3::integer, category_id),
           is_available = COALESCE($4::boolean, is_available)
       WHERE id = $5`,
      [
        name !== undefined ? name : null,
        price !== undefined ? price : null,
        category_id !== undefined ? category_id : null,
        is_available !== undefined ? !!is_available : null,
        id
      ]
    );

    return res.status(200).json({ message: 'Product updated successfully' });
  } catch (error) {
    console.error('Error updating product:', error);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

export async function deleteProduct(req: AuthRequest, res: Response) {
  try {
    const { id } = req.params;
    const db = getDb();

    // Check if the product has been ordered
    const orderItems = await db.query('SELECT COUNT(*) as count FROM order_items WHERE product_id = $1', [id]);
    if (orderItems.rows.length > 0 && parseInt(orderItems.rows[0].count) > 0) {
      return res.status(400).json({ error: 'Cannot delete product because it has been ordered in history. Set is_available to false instead.' });
    }

    const result = await db.query('DELETE FROM products WHERE id = $1', [id]);
    if (result.rowCount === 0) {
      return res.status(404).json({ error: 'Product not found' });
    }

    return res.status(200).json({ message: 'Product deleted successfully' });
  } catch (error) {
    console.error('Error deleting product:', error);
    return res.status(500).json({ error: 'Internal server error' });
  }
}
