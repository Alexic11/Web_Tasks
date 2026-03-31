package carobnifrulas.web_tasks.card;

import com.vaadin.flow.shared.Registration;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class CardRealtimeBus {

    public enum ChangeType {
        COMMENTS,
        ATTACHMENTS,
        ACTIVITY,
        ALL
    }

    private static final Map<Long, Map<String, Consumer<ChangeType>>> LISTENERS = new ConcurrentHashMap<>();

    private CardRealtimeBus() {
    }

    public static Registration register(Long cardId, Consumer<ChangeType> listener) {
        String token = UUID.randomUUID().toString();
        LISTENERS.computeIfAbsent(cardId, id -> new ConcurrentHashMap<>()).put(token, listener);

        return () -> {
            Map<String, Consumer<ChangeType>> cardListeners = LISTENERS.get(cardId);
            if (cardListeners != null) {
                cardListeners.remove(token);
                if (cardListeners.isEmpty()) {
                    LISTENERS.remove(cardId);
                }
            }
        };
    }

    public static void publish(Long cardId, ChangeType changeType) {
        Map<String, Consumer<ChangeType>> cardListeners = LISTENERS.get(cardId);
        if (cardListeners == null || cardListeners.isEmpty()) {
            return;
        }

        for (Consumer<ChangeType> listener : cardListeners.values()) {
            try {
                listener.accept(changeType);
            } catch (Exception ignored) {
            }
        }
    }
}