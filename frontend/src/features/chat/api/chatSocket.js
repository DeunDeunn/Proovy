import { Client, ReconnectionTimeMode } from "@stomp/stompjs";

const WS_URL = process.env.NEXT_PUBLIC_WS_URL;
const PERSONAL_ERROR_QUEUE = "/queue/errors";
const roomTopic = (chatRoomId) => `/topic/chats/rooms/${chatRoomId}`;

let client = null;
let onError = null;
let onDisconnect = null;
let onConnected = null;
let activeRoomSubscription = null;

const subscribeActiveRoom = () => {
  if (!activeRoomSubscription) return;

  const { chatRoomId, onMessage } = activeRoomSubscription;
  activeRoomSubscription.subscription = client.subscribe(roomTopic(chatRoomId), (message) => {
    onMessage(JSON.parse(message.body));
  });
};

const createClient = () =>
  new Client({
    brokerURL: WS_URL,
    reconnectDelay: 1000,
    maxReconnectDelay: 30000,
    reconnectTimeMode: ReconnectionTimeMode.EXPONENTIAL,
    onConnect: () => {
      client.subscribe(PERSONAL_ERROR_QUEUE, (message) => {
        onError?.(JSON.parse(message.body));
      });
      subscribeActiveRoom();
      onConnected?.();
    },
    onWebSocketClose: () => {
      onDisconnect?.();
    },
    onWebSocketError: () => {
      onDisconnect?.();
    },
  });

export const connectSocket = ({
  onError: onErrorHandler,
  onDisconnect: onDisconnectHandler,
  onConnected: onConnectedHandler,
} = {}) => {
  onError = onErrorHandler ?? null;
  onDisconnect = onDisconnectHandler ?? null;
  onConnected = onConnectedHandler ?? null;

  if (!client) {
    client = createClient();
  }
  if (!client.active) {
    client.activate();
  }

  return client;
};

export const disconnectSocket = () => {
  client?.deactivate();
};

export const getSocketClient = () => client;

export const subscribeRoom = (chatRoomId, onMessage) => {
  activeRoomSubscription = { chatRoomId, onMessage, subscription: null };

  if (client?.connected) {
    subscribeActiveRoom();
  }
};

export const unsubscribeRoom = () => {
  activeRoomSubscription?.subscription?.unsubscribe();
  activeRoomSubscription = null;
};

export const publishMessage = (chatRoomId, payload) => {
  client?.publish({
    destination: `/app/chats/rooms/${chatRoomId}/messages`,
    body: JSON.stringify(payload),
  });
};
