const DEFAULT_API_BASE_URL = "http://localhost:8080";

export const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL ?? DEFAULT_API_BASE_URL
).replace(/\/$/, "");

export const REST_API_URL = `${API_BASE_URL}/api`;
export const GRAPHQL_URL = `${API_BASE_URL}/graphql`;

export const getGroupNotificationsWebSocketUrl = (token: string): string => {
  const url = new URL(API_BASE_URL);
  const protocol = url.protocol === "https:" ? "wss:" : "ws:";
  return `${protocol}//${url.host}/ws/group-notifications?token=${encodeURIComponent(token)}`;
};
