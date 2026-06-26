import { Server } from 'http';
import { WebSocketServer, WebSocket } from 'ws';
import jwt from 'jsonwebtoken';
import { JWT_SECRET } from '../middleware/auth';

interface Client {
  ws: WebSocket;
  userId?: number;
  role?: string;
  username?: string;
}

let wss: WebSocketServer;
const clients: Set<Client> = new Set();

export function initWebSocketServer(server: Server) {
  wss = new WebSocketServer({ noServer: true });

  server.on('upgrade', (request, socket, head) => {
    // Parse URL to find token
    const url = new URL(request.url || '', `http://${request.headers.host}`);
    const token = url.searchParams.get('token');

    if (!token) {
      socket.write('HTTP/1.1 401 Unauthorized\r\n\r\n');
      socket.destroy();
      return;
    }

    jwt.verify(token, JWT_SECRET, (err, decoded: any) => {
      if (err) {
        socket.write('HTTP/1.1 403 Forbidden\r\n\r\n');
        socket.destroy();
        return;
      }

      wss.handleUpgrade(request, socket, head, (ws) => {
        wss.emit('connection', ws, decoded);
      });
    });
  });

  wss.on('connection', (ws: WebSocket, user: any) => {
    const client: Client = {
      ws,
      userId: user.id,
      role: user.role,
      username: user.username
    };
    
    clients.add(client);
    console.log(`WebSocket client connected: ${user.username} (${user.role})`);

    // Send a welcome event
    ws.send(JSON.stringify({
      event: 'WELCOME',
      data: { message: `Connected as ${user.username}` }
    }));

    ws.on('message', (message: string) => {
      try {
        const payload = JSON.parse(message);
        console.log(`Received WebSocket message from ${user.username}:`, payload);
        
        // Handle client-initiated events here if needed.
        // For example, ping/pong or subscribing to specific zones.
      } catch (err) {
        console.error('Error handling WebSocket message:', err);
      }
    });

    ws.on('close', () => {
      clients.delete(client);
      console.log(`WebSocket client disconnected: ${user.username}`);
    });

    ws.on('error', (err) => {
      console.error(`WebSocket error for ${user.username}:`, err);
      clients.delete(client);
    });
  });
}

export function broadcast(event: string, data: any) {
  if (!wss) return;
  const message = JSON.stringify({ event, data });
  
  clients.forEach((client) => {
    if (client.ws.readyState === WebSocket.OPEN) {
      client.ws.send(message);
    }
  });
  console.log(`Broadcasted event: ${event}`);
}
