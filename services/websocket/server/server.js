require('dotenv').config();    // le o ENV

//bibliotecas
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

// RabbitMQ sources
const FANOUT_EXCHANGE  = 'notification_fanout_1';  // fanout exchange
const CHAT_QUEUE       = 'realfrontchat';   // chat em tempo real
const NOTIFICATION_QUEUE = 'notification';  // notificaçoes

const WS_PORT_NUM = Number(WS_PORT);      // porta websocket

// Map userId -> Set<WebSocket>
const clients = new Map();

// cache para evitar mensagens duplicadas
const SEEN     = new Set();
const MAX_SEEN = 500;

// envia o payload para todos os websockets abertos
const push = (conns, payload) => {
  for (const ws of conns) {
    if (ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify(payload));
    }
  }
};

// hander para receber mensagems do rabbit
function handleMessage(msg, channel) {
  if (!msg) return;

  // parse do json
  let data;
  try {
    data = JSON.parse(msg.content.toString());
  } catch (err) {
    console.error('[RabbitMQ] Invalid JSON:', err.message);
    return channel.ack(msg);
  }

  // Passo de deduplicaçao: gera uma chave única para cada evento+utilizador+timestamp
  const dedupKey = `${data.event_id}|${data.user_id}|${data.timestamp}`;
  if (SEEN.has(dedupKey)) {
    return channel.ack(msg);    // descarta evento ja processado
  }
  SEEN.add(dedupKey);   // Se exceder o limite da cache, remove a entrada mais antiga
  if (SEEN.size > MAX_SEEN) {
    SEEN.delete(SEEN.values().next().value);
  }
  // extrai os dados principais
  const eventId     = Number(data.event_id);
  const initiatorId = Number(data.user_id);
  const timestamp   = data.timestamp;

  // constroi a lista de participantes
  const participants = (data.participants || []).map(p => ({
    id:   Number(p.user_id),
    name: p.user_name,
  }));

  console.log(  // log
    `[RabbitMQ] Dispatching "${data.type}" for event #${eventId}\n` +
    `  From user: ${initiatorId} at ${timestamp}\n` +
    `  To participants: ${participants.map(p => p.id).join(', ')}`
  );

  // envia a notificaçao para cada participante online
  participants.forEach(p => {

     if (p.id === initiatorId) return; // impede que o iniciador receba a sua notificaçao

    const conns  = clients.get(p.id);  //Obtem o conjunto de conexões WebSocket ativas para o utilizador p.id
    const online = conns && conns.size > 0; //Determina se o utilizador está online (tem pelo menos uma conexão aberta)
    // prepara e envia o payload 
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

//-------WebSocket server -----------------------------------------------
//cria o servidor websocket
const wss = new WebSocket.Server({ port: WS_PORT_NUM }, () => {
  console.log(`WebSocket server running on port ${WS_PORT_NUM}`);
});
// evento dispara sempre que um cliense se liga
wss.on('connection', (ws, req) => {
  //extrai o token
  const url   = new URL(req.url, `http://localhost:${WS_PORT_NUM}`);    
  const token = url.searchParams.get('token');

  if (!token) return ws.close(4001, 'Authentication token required'); // nao ha token

  // valida token
  let payload;
  try {
    payload = jwt.verify(token, JWT_SECRET);
  } catch {
    return ws.close(4002, 'Invalid or expired token');
  }

  // obtem o id do user a partir do token
  const userId = Number(payload.sub);

  // regista a ligaçao websocket no mapa dos clientes
  if (!clients.has(userId)) clients.set(userId, new Set());  // se for a primeira ligaçao cria um novo set
  clients.get(userId).add(ws);

  //Quando a ligação fechar, remove-a do Set e limpa o mapa se vazio
  ws.on('close', () => {
    const set = clients.get(userId);
    if (set) {
      set.delete(ws);
      if (set.size === 0) clients.delete(userId);
    }
  });
});

// ─── RabbitMQ consumers ──────────────────────────────────────────────────────
(async () => {
  // estabelece a ligaçao ao servidor rabbitmq usando oi env
  try {
    const conn    = await amqp.connect(
      `amqp://${RABBITMQ_USERNAME}:${RABBITMQ_PASSWORD}@${RABBITMQ_HOST}:${RABBITMQ_PORT}`
    );
    // cria o canal
    const channel = await conn.createChannel();

    // 1) Fan-out exchange
    await channel.assertExchange(FANOUT_EXCHANGE, 'fanout', { durable: true });
    const { queue: fanoutQueue } = await channel.assertQueue('', { exclusive: true });
    await channel.bindQueue(fanoutQueue, FANOUT_EXCHANGE, '');
    console.log(`Waiting for fanout messages on "${FANOUT_EXCHANGE}"`);
    channel.consume(fanoutQueue, msg => handleMessage(msg, channel), { noAck: false });

    // 2) Direct chat queue
    await channel.assertQueue(CHAT_QUEUE, { durable: true });
    console.log(`Waiting for direct messages on queue "${CHAT_QUEUE}"`);
    channel.consume(CHAT_QUEUE, msg => handleMessage(msg, channel), { noAck: false });

    // 3) Direct notification queue
    await channel.assertQueue(NOTIFICATION_QUEUE, { durable: true });
    console.log(`Waiting for direct messages on queue "${NOTIFICATION_QUEUE}"`);
    channel.consume(NOTIFICATION_QUEUE, msg => handleMessage(msg, channel), { noAck: false });

  } catch (err) {
    console.error('Error setting up RabbitMQ consumers:', err);
    process.exit(1);
  }
})();
