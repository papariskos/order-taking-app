import { Response } from 'express';
import { AuthRequest } from '../middleware/auth';
import { getDb } from '../config/db';
import bcrypt from 'bcryptjs';

export async function getUsers(req: AuthRequest, res: Response) {
  try {
    const db = getDb();
    const result = await db.query('SELECT id, username, role, created_at FROM users');
    return res.status(200).json(result.rows);
  } catch (error) {
    console.error('Error fetching users:', error);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

export async function createUser(req: AuthRequest, res: Response) {
  try {
    const { username, password, role } = req.body;

    if (!username || !password || !role) {
      return res.status(400).json({ error: 'Username, password and role are required' });
    }

    if (role !== 'admin' && role !== 'waiter') {
      return res.status(400).json({ error: 'Invalid role. Must be admin or waiter' });
    }

    const db = getDb();

    // Check if user exists
    const existing = await db.query('SELECT id FROM users WHERE username = $1', [username]);
    if (existing.rows.length > 0) {
      return res.status(400).json({ error: 'Username already exists' });
    }

    const hashedPassword = await bcrypt.hash(password, 10);
    const result = await db.query(
      'INSERT INTO users (username, password, role) VALUES ($1, $2, $3) RETURNING id',
      [username, hashedPassword, role]
    );

    return res.status(201).json({
      message: 'User created successfully',
      user: {
        id: result.rows[0].id,
        username,
        role
      }
    });
  } catch (error) {
    console.error('Error creating user:', error);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

export async function updateUser(req: AuthRequest, res: Response) {
  try {
    const { id } = req.params;
    const { password, role } = req.body;

    const db = getDb();
    const checkResult = await db.query('SELECT id FROM users WHERE id = $1', [id]);

    if (checkResult.rows.length === 0) {
      return res.status(404).json({ error: 'User not found' });
    }

    if (role && role !== 'admin' && role !== 'waiter') {
      return res.status(400).json({ error: 'Invalid role. Must be admin or waiter' });
    }

    if (password) {
      const hashedPassword = await bcrypt.hash(password, 10);
      await db.query('UPDATE users SET password = $1 WHERE id = $2', [hashedPassword, id]);
    }

    if (role) {
      await db.query('UPDATE users SET role = $1 WHERE id = $2', [role, id]);
    }

    return res.status(200).json({ message: 'User updated successfully' });
  } catch (error) {
    console.error('Error updating user:', error);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

export async function deleteUser(req: AuthRequest, res: Response) {
  try {
    const { id } = req.params;
    const currentUserId = req.user?.id;

    if (Number(id) === currentUserId) {
      return res.status(400).json({ error: 'Cannot delete your own account' });
    }

    const db = getDb();
    const result = await db.query('DELETE FROM users WHERE id = $1', [id]);

    if (result.rowCount === 0) {
      return res.status(404).json({ error: 'User not found' });
    }

    return res.status(200).json({ message: 'User deleted successfully' });
  } catch (error) {
    console.error('Error deleting user:', error);
    return res.status(500).json({ error: 'Internal server error' });
  }
}
