import { Client } from "@stomp/stompjs";

const WS_URL = process.env.NEXT_PUBLIC_WS_URL;
const PERSONAL_ERROR_QUEUE = "/queue/errors";

let client = null;
let onError = null;

const createClient = () =>
  new Client({
    brokerURL: WS_URL,
    reconnectDelay: 5000,
    onConnect: () => {
      client.subscribe(PERSONAL_ERROR_QUEUE, (message) => {
        onError?.(JSON.parse(message.body));
      });
    },
  });

export const connectSocket = ({ onError: onErrorHandler } = {}) => {
  onError = onErrorHandler ?? null;

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
