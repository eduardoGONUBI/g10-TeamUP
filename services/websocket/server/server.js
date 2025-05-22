require('dotenv').config();
const amqp      = require('amqplib');
const WebSocket = require('ws');
const jwt       = require('jsonwebtoken');

const {
  RABBITMQ_USERNAME,
  RABBITMQ_PASSWORD,
  RABBITMQ_HOST,
  RABBITMQ_PORT,
  JWT_SECRET,
  WS_PORT = 8080
} = process.env;

// Fanout exchange we want to consume
const EXCHANGE    = 'notification_fanout_1';
const WS_PORT_NUM = parseInt(WS_PORT, 10);

// Map userId -> Set of ws connections
const clients = new Map();

const wss = new WebSocket.Server({ port: WS_PORT_NUM }, () => {
  console.log(`WebSocket server running on port ${WS_PORT_NUM}`);
});

wss.on('connection', (ws, req) => {
  // Properly extract the token from the URL
  const url = new URL(req.url, `http://localhost:${WS_PORT_NUM}`);
  const token = url.searchParams.get('token');
  if (!token) return ws.close(4001, 'Authentication token required');

  let payload;
  try {
    payload = jwt.verify(token, JWT_SECRET);
  } catch {
    return ws.close(4002, 'Invalid or expired token');
  }

  const userId = Number(payload.sub);
  if (!clients.has(userId)) {
    clients.set(userId, new Set());
  }
  clients.get(userId).add(ws);

  ws.on('close', () => {
    const set = clients.get(userId);
    if (set) {
      set.delete(ws);
      if (set.size === 0) clients.delete(userId);
    }
  });
});

(async () => {
  try {
    // 1) connect & channel
    const conn    = await amqp.connect(
      `amqp://${RABBITMQ_USERNAME}:${RABBITMQ_PASSWORD}@${RABBITMQ_HOST}:${RABBITMQ_PORT}`
    );
    const channel = await conn.createChannel();

    // 2) declare the fanout exchange
    await channel.assertExchange(EXCHANGE, 'fanout', { durable: true });

    // 3) create a private, exclusive queue
    const { queue } = await channel.assertQueue('', { exclusive: true });

    // 4) bind it to our fanout exchange
    await channel.bindQueue(queue, EXCHANGE, '');

    console.log(`Waiting for messages in exchange "${EXCHANGE}", queue "${queue}"`);

    // 5) consume
    channel.consume(
      queue,
      msg => {
        if (!msg) return;

        const raw = msg.content.toString();
        console.log('[RabbitMQ] Raw:', raw);

        let data;
        try {
          data = JSON.parse(raw);
        } catch (err) {
          console.error('[RabbitMQ] Invalid JSON:', err.message);
          channel.ack(msg);
          return;
        }

        const eventId     = Number(data.event_id);
        const initiatorId = Number(data.user_id);
        const timestamp   = data.timestamp;
        const recipients  = (data.participants || [])
          .map(p => ({ id: Number(p.user_id), name: p.user_name }))
          .filter(p => p.id !== initiatorId);

        console.log(
          `[RabbitMQ] Dispatching "${data.type}" event "${data.event_name}" (#${eventId})\n` +
          `  From user: ${initiatorId} at ${timestamp}\n` +
          `  To recipients: ${recipients.map(r => r.id).join(', ')}`
        );

        // Fan out over WebSocket to each online participant
        recipients.forEach(r => {
          const conns = clients.get(r.id);
          let status = conns && conns.size > 0 ? 'sent' : 'offline';

          if (conns) {
            for (const clientWs of conns) {
              if (clientWs.readyState === WebSocket.OPEN) {
                try {
                  clientWs.send(JSON.stringify({
                    type:       data.type,
                    event_id:   eventId,
                    event_name: data.event_name,
                    user_id:    initiatorId,
                    user_name:  data.user_name,
                    message:    data.message,
                    timestamp:  timestamp
                  }));
                } catch (sendErr) {
                  console.error(`Failed to send to user ${r.id}:`, sendErr);
                }
              }
            }
          }

          console.log(`  → user ${r.id}: ${status}`);
        });

        channel.ack(msg);
      },
      { noAck: false }
    );
  } catch (err) {
    console.error('Error connecting to RabbitMQ or setting up consumer:', err);
    process.exit(1);
  }
})();
