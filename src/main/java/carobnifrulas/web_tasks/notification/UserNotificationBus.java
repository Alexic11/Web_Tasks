package carobnifrulas.web_tasks.notification;

import com.vaadin.flow.shared.Registration;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class UserNotificationBus {

    private static final Map<Long, Map<String, Runnable>> LISTENERS = new ConcurrentHashMap<>();

    private UserNotificationBus() {
    }

    public static Registration register(Long userId, Runnable listener) {
        String token = UUID.randomUUID().toString();
        LISTENERS.computeIfAbsent(userId, id -> new ConcurrentHashMap<>()).put(token, listener);

        return () -> {
            Map<String, Runnable> userListeners = LISTENERS.get(userId);
            if (userListeners != null) {
                userListeners.remove(token);
                if (userListeners.isEmpty()) {
                    LISTENERS.remove(userId);
                }
            }
        };
    }

    public static void publish(Long userId) {
        Map<String, Runnable> userListeners = LISTENERS.get(userId);
        if (userListeners == null || userListeners.isEmpty()) {
            return;
        }

        for (Runnable listener : userListeners.values()) {
            try {
                listener.run();
            } catch (Exception ignored) {
            }
        }
    }
}