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

// ---------- RabbitMQ sources ----------
const FANOUT_EXCHANGE = 'notification_fanout_1';
const DIRECT_QUEUE    = 'realfrontchat';

const WS_PORT_NUM = Number(WS_PORT);

// Map userId -> Set<WebSocket>
const clients = new Map();

// ---------- DEDUP CACHE (Option B) -------------------------------------------
const SEEN      = new Set();  // 🔹 NEW — keeps recent message keys
const MAX_SEEN  = 500;        // 🔹 NEW — trim size

// -----------------------------------------------------------------------------
// Helper: push payload to every open WebSocket in a Set<WebSocket>
const push = (conns, payload) => {
  for (const ws of conns) {
    if (ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify(payload));
    }
  }
};

// Common handler for any RabbitMQ message we receive.
function handleMessage(msg, channel) {
  if (!msg) return;

  let data;
  try { data = JSON.parse(msg.content.toString()); }
  catch (err) {
    console.error('[RabbitMQ] Invalid JSON:', err.message);
    return channel.ack(msg);
  }

  // ---------- DEDUP STEP -----------------------------------------------------
  const dedupKey = `${data.event_id}|${data.user_id}|${data.timestamp}`; // 🔹 NEW
  if (SEEN.has(dedupKey)) {                              // 🔹 NEW
    return channel.ack(msg);                             // 🔹 NEW (skip duplicate)
  }                                                      // 🔹 NEW
  SEEN.add(dedupKey);                                    // 🔹 NEW
  if (SEEN.size > MAX_SEEN) {                            // 🔹 NEW
    SEEN.delete(SEEN.values().next().value);             // 🔹 NEW (trim oldest)
  }                                                      // 🔹 NEW
  // --------------------------------------------------------------------------

  const eventId     = Number(data.event_id);
  const initiatorId = Number(data.user_id);
  const timestamp   = data.timestamp;

  const participants = (data.participants || []).map(p => ({
    id:   Number(p.user_id),
    name: p.user_name,
  })); // includes sender

  console.log(
    `[RabbitMQ] Dispatching "${data.type}" for event #${eventId}\n` +
    `  From user: ${initiatorId} at ${timestamp}\n` +
    `  To participants: ${participants.map(p => p.id).join(', ')}`
  );

  // Push to every online participant (including sender)
  participants.forEach(p => {
    const conns  = clients.get(p.id);
    const online = conns && conns.size > 0;
    if (online) {
      push(conns, {
        type:       data.type,
        event_id:   eventId,
        event_name: data.event_name,
        user_id:    initiatorId,
        user_name:  data.user_name,
        message:    data.message,
        timestamp,
      });
    }
    console.log(`  → user ${p.id}: ${online ? 'sent' : 'offline'}`);
  });

  channel.ack(msg);
}

// ---------- WebSocket server ----------
const wss = new WebSocket.Server({ port: WS_PORT_NUM }, () => {
  console.log(`WebSocket server running on port ${WS_PORT_NUM}`);
});

wss.on('connection', (ws, req) => {
  const url   = new URL(req.url, `http://localhost:${WS_PORT_NUM}`);
  const token = url.searchParams.get('token');
  if (!token) return ws.close(4001, 'Authentication token required');

  let payload;
  try { payload = jwt.verify(token, JWT_SECRET); }
  catch { return ws.close(4002, 'Invalid or expired token'); }

  const userId = Number(payload.sub);
  if (!clients.has(userId)) clients.set(userId, new Set());
  clients.get(userId).add(ws);

  ws.on('close', () => {
    const set = clients.get(userId);
    if (set) {
      set.delete(ws);
      if (set.size === 0) clients.delete(userId);
    }
  });
});

// ---------- RabbitMQ consumers ----------
(async () => {
  try {
    const conn    = await amqp.connect(
      `amqp://${RABBITMQ_USERNAME}:${RABBITMQ_PASSWORD}@${RABBITMQ_HOST}:${RABBITMQ_PORT}`
    );
    const channel = await conn.createChannel();

    // 1) Fan-out exchange
    await channel.assertExchange(FANOUT_EXCHANGE, 'fanout', { durable: true });
    const { queue } = await channel.assertQueue('', { exclusive: true });
    await channel.bindQueue(queue, FANOUT_EXCHANGE, '');
    console.log(`Waiting for fanout messages on "${FANOUT_EXCHANGE}"`);
    channel.consume(queue, msg => handleMessage(msg, channel), { noAck: false });

    // 2) Direct queue
    await channel.assertQueue(DIRECT_QUEUE, { durable: true });
    console.log(`Waiting for direct messages on queue "${DIRECT_QUEUE}"`);
    channel.consume(DIRECT_QUEUE, msg => handleMessage(msg, channel), { noAck: false });

  } catch (err) {
    console.error('Error setting up RabbitMQ consumers:', err);
    process.exit(1);
  }
})();
