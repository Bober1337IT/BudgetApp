declare global {
  interface Window {
    __APP_CONFIG__?: {
      apiBaseUrl?: string;
    };
  }
}

const DEFAULT_API_BASE_URL = "http://localhost:8080";

const resolveApiBaseUrl = (): string => {
  const runtimeUrl = window.__APP_CONFIG__?.apiBaseUrl?.trim();
  if (runtimeUrl) {
    return runtimeUrl.replace(/\/$/, "");
  }

  const buildTimeUrl = import.meta.env.VITE_API_BASE_URL?.trim();
  if (buildTimeUrl) {
    return buildTimeUrl.replace(/\/$/, "");
  }

  return DEFAULT_API_BASE_URL;
};

export const API_BASE_URL = resolveApiBaseUrl();

export const REST_API_URL = `${API_BASE_URL}/api`;
export const GRAPHQL_URL = `${API_BASE_URL}/graphql`;

export const getGroupNotificationsWebSocketUrl = (token: string): string => {
  const url = new URL(API_BASE_URL);
  const protocol = url.protocol === "https:" ? "wss:" : "ws:";
  return `${protocol}//${url.host}/ws/group-notifications?token=${encodeURIComponent(token)}`;
};
