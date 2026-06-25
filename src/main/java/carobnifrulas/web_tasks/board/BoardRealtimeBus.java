package carobnifrulas.web_tasks.board;

import com.vaadin.flow.shared.Registration;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Lightweight in-memory realtime event bus for BoardView refreshes.
 *
 * This is intentionally similar to CardRealtimeBus, but it works at board level:
 * one event refreshes every open BoardView for the same boardId, including views
 * opened by other users/sessions.
 *
 * Vaadin @Push is required for other clients to receive updates immediately.
 */
public final class BoardRealtimeBus {

    public enum ChangeType {
        CARD_CHANGED,
        CARD_MOVED,
        LABELS,
        CHECKLIST,
        ALL
    }

    private static final Map<Long, Map<String, Consumer<ChangeType>>> LISTENERS = new ConcurrentHashMap<>();

    private BoardRealtimeBus() {
    }

    public static Registration register(Long boardId, Consumer<ChangeType> listener) {
        String token = UUID.randomUUID().toString();
        LISTENERS.computeIfAbsent(boardId, id -> new ConcurrentHashMap<>()).put(token, listener);

        return () -> {
            Map<String, Consumer<ChangeType>> boardListeners = LISTENERS.get(boardId);
            if (boardListeners != null) {
                boardListeners.remove(token);
                if (boardListeners.isEmpty()) {
                    LISTENERS.remove(boardId);
                }
            }
        };
    }

    public static void publish(Long boardId, ChangeType changeType) {
        if (boardId == null) {
            return;
        }

        Map<String, Consumer<ChangeType>> boardListeners = LISTENERS.get(boardId);
        if (boardListeners == null || boardListeners.isEmpty()) {
            return;
        }

        for (Consumer<ChangeType> listener : boardListeners.values()) {
            try {
                listener.accept(changeType == null ? ChangeType.ALL : changeType);
            } catch (Exception ignored) {
            }
        }
    }
}
