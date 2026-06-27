package io.opentron.cli;

/**
 * Chat notification system for background events.
 */
public class ChatNotifications {
    private static java.util.List<Notification> notifications = new java.util.ArrayList<>();
    
    public static class Notification {
        public String type;      // info, warning, error, success
        public String message;
        public long timestamp;
        public boolean read;
    }

    public static void addNotification(String type, String message) {
        Notification notif = new Notification();
        notif.type = type;
        notif.message = message;
        notif.timestamp = System.currentTimeMillis();
        notif.read = false;
        notifications.add(notif);
    }

    public static java.util.List<Notification> getUnread() {
        java.util.List<Notification> unread = new java.util.ArrayList<>();
        for (Notification n : notifications) {
            if (!n.read) {
                unread.add(n);
            }
        }
        return unread;
    }

    public static void markAsRead(int index) {
        if (index >= 0 && index < notifications.size()) {
            notifications.get(index).read = true;
        }
    }

    public static void clearAll() {
        notifications.clear();
    }

    public static int getUnreadCount() {
        return (int) notifications.stream().filter(n -> !n.read).count();
    }

    public static String formatNotification(Notification n) {
        String emoji = "ℹ️";
        if ("warning".equals(n.type)) emoji = "⚠️";
        if ("error".equals(n.type)) emoji = "❌";
        if ("success".equals(n.type)) emoji = "✅";
        
        return String.format("%s [%s] %s", emoji, n.type, n.message);
    }
}
