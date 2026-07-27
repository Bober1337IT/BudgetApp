// src/api/axiosClient.ts
import axios from "axios";
import { REST_API_URL } from "../config/apiConfig";

const axiosClient = axios.create({
  baseURL: REST_API_URL,
});

// Opcjonalnie dodaj interceptor do automatycznego dołączania tokena JWT
axiosClient.interceptors.request.use((config) => {
  const token = localStorage.getItem("accessToken");
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default axiosClient;
