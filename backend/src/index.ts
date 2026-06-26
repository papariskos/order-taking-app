import dotenv from 'dotenv';
dotenv.config();

import express from 'express';
import http from 'http';
import cors from 'cors';
import { getDb, initDb } from './config/db';
import apiRouter from './routes/api';
import { initWebSocketServer } from './services/websocket';

const app = express();
const port = process.env.PORT || 3000;

// Enable CORS for frontend web admin panel and local emulator/devices
app.use(cors());
app.use(express.json());

// Main HTTP router
app.use('/api', apiRouter);

const server = http.createServer(app);

// Initialize WebSockets
initWebSocketServer(server);

// Boot database and start listening
async function bootstrap() {
  try {
    console.log('Connecting to PostgreSQL database...');
    getDb();
    await initDb();
    console.log('PostgreSQL Database initialized and seeded successfully.');

    server.listen(port, () => {
      console.log(`Backend server is running on http://localhost:${port}`);
      console.log(`WebSocket server initialized on ws://localhost:${port}`);
    });
  } catch (error) {
    console.error('Failed to initialize PostgreSQL database:', error);
    process.exit(1);
  }
}

bootstrap();
