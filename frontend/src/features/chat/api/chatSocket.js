import { Client, ReconnectionTimeMode } from "@stomp/stompjs";

const WS_URL = process.env.NEXT_PUBLIC_WS_URL;
// 개인 큐는 서버의 convertAndSendToUser가 세션별 목적지로 라우팅해주도록 /user 접두사로 구독해야 한다.
const PERSONAL_ERROR_QUEUE = "/user/queue/errors";
const PERSONAL_ROOM_UPDATE_QUEUE = "/user/queue/chat-room-updates";
const roomTopic = (chatRoomId) => `/topic/chats/rooms/${chatRoomId}`;

let client = null;
let onError = null;
let onDisconnect = null;
let onConnected = null;
let onRoomUpdated = null;
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
      client.subscribe(PERSONAL_ROOM_UPDATE_QUEUE, (message) => {
        onRoomUpdated?.(JSON.parse(message.body));
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

// 여러 화면이 동시에 소켓을 쓴다 (앱 전역 실시간 동기화 + 지금 열어본 채팅방).
// 한쪽이 넘기지 않은 핸들러까지 매번 덮어써버리면 다른 쪽 핸들러가 사라지므로,
// 이번 호출에서 실제로 넘어온 핸들러만 갱신한다.
export const connectSocket = ({
  onError: onErrorHandler,
  onDisconnect: onDisconnectHandler,
  onConnected: onConnectedHandler,
  onRoomUpdated: onRoomUpdatedHandler,
} = {}) => {
  if (onErrorHandler !== undefined) onError = onErrorHandler;
  if (onDisconnectHandler !== undefined) onDisconnect = onDisconnectHandler;
  if (onConnectedHandler !== undefined) onConnected = onConnectedHandler;
  if (onRoomUpdatedHandler !== undefined) onRoomUpdated = onRoomUpdatedHandler;

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
