import { useEffect } from "react";
import { toast } from "react-toastify";
import { getGroupNotificationsWebSocketUrl } from "../../config/apiConfig";
import { useAuth } from "../../context/AuthContext";

interface GroupNotification {
  type: "GROUP_EXPENSE_ADDED";
  groupId: number | string;
  groupName: string;
  title: string;
  amount: number;
  userShare: number;
  createdByEmail: string;
  message: string;
}

const GroupNotificationsListener = () => {
  const { isAuthenticated } = useAuth();

  useEffect(() => {
    if (!isAuthenticated) return;

    const token = localStorage.getItem("accessToken");
    if (!token) return;

    const socket = new WebSocket(getGroupNotificationsWebSocketUrl(token));

    socket.onmessage = (event) => {
      try {
        const notification = JSON.parse(event.data) as GroupNotification;
        if (notification.type === "GROUP_EXPENSE_ADDED") {
          toast.info(notification.message);
        }
      } catch (error) {
        console.error("Nie udało się obsłużyć komunikatu grupowego:", error);
      }
    };

    socket.onerror = (error) => {
      console.error("Błąd połączenia WebSocket z komunikatami grupowymi:", error);
    };

    return () => {
      socket.close();
    };
  }, [isAuthenticated]);

  return null;
};

export default GroupNotificationsListener;
