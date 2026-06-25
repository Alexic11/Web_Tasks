package carobnifrulas.web_tasks.card;

import com.vaadin.flow.shared.Registration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Lightweight in-memory presence bus for TaskDialog.
 *
 * It tracks which users currently have the same task open and pushes the
 * current snapshot to all open TaskDialog instances for that card.
 *
 * Notes:
 * - This is intentionally in-memory. It is perfect for a single application
 *   instance. If the app is later deployed as multiple backend instances,
 *   replace this with Redis/pub-sub or a database-backed heartbeat.
 * - Vaadin @Push is required for other browser sessions to see presence changes
 *   immediately.
 */
public final class CardPresenceBus {

    public enum PresenceMode {
        VIEWING,
        EDITING
    }

    public static final class PresenceUser {
        private final Long userId;
        private final String displayName;
        private final String email;
        private final PresenceMode mode;
        private final Instant joinedAt;

        public PresenceUser(Long userId, String displayName, String email, PresenceMode mode, Instant joinedAt) {
            this.userId = userId;
            this.displayName = displayName;
            this.email = email;
            this.mode = mode == null ? PresenceMode.VIEWING : mode;
            this.joinedAt = joinedAt == null ? Instant.now() : joinedAt;
        }

        public Long getUserId() {
            return userId;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getEmail() {
            return email;
        }

        public PresenceMode getMode() {
            return mode;
        }

        public Instant getJoinedAt() {
            return joinedAt;
        }
    }

    private static final class PresenceEntry {
        private final String token;
        private final Long userId;
        private final String displayName;
        private final String email;
        private final PresenceMode mode;
        private final Instant joinedAt;

        private PresenceEntry(String token,
                              Long userId,
                              String displayName,
                              String email,
                              PresenceMode mode) {
            this.token = token;
            this.userId = userId;
            this.displayName = displayName;
            this.email = email;
            this.mode = mode == null ? PresenceMode.VIEWING : mode;
            this.joinedAt = Instant.now();
        }
    }

    private static final Map<Long, Map<String, PresenceEntry>> PRESENCE = new ConcurrentHashMap<>();
    private static final Map<Long, Map<String, Consumer<List<PresenceUser>>>> LISTENERS = new ConcurrentHashMap<>();

    private CardPresenceBus() {
    }

    public static Registration register(Long cardId,
                                        Long userId,
                                        String displayName,
                                        String email,
                                        PresenceMode mode,
                                        Consumer<List<PresenceUser>> listener) {
        if (cardId == null) {
            return () -> {};
        }

        String token = UUID.randomUUID().toString();

        PRESENCE.computeIfAbsent(cardId, id -> new ConcurrentHashMap<>())
                .put(token, new PresenceEntry(token, userId, displayName, email, mode));

        if (listener != null) {
            LISTENERS.computeIfAbsent(cardId, id -> new ConcurrentHashMap<>())
                    .put(token, listener);
        }

        publish(cardId);

        return () -> {
            remove(cardId, token);
        };
    }

    public static List<PresenceUser> snapshot(Long cardId) {
        if (cardId == null) {
            return List.of();
        }

        Map<String, PresenceEntry> entries = PRESENCE.get(cardId);
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }

        Map<String, PresenceUser> collapsed = new LinkedHashMap<>();

        entries.values().stream()
                .sorted(Comparator.comparing(e -> e.joinedAt))
                .forEach(e -> {
                    String key = e.userId != null ? "ID:" + e.userId : "EMAIL:" + nullSafe(e.email);
                    PresenceUser existing = collapsed.get(key);

                    if (existing == null) {
                        collapsed.put(key, toPresenceUser(e));
                        return;
                    }

                    // If the same user has multiple tabs open, prefer EDITING over VIEWING.
                    if (existing.getMode() != PresenceMode.EDITING && e.mode == PresenceMode.EDITING) {
                        collapsed.put(key, toPresenceUser(e));
                    }
                });

        return new ArrayList<>(collapsed.values());
    }

    private static void remove(Long cardId, String token) {
        Map<String, PresenceEntry> presenceForCard = PRESENCE.get(cardId);
        if (presenceForCard != null) {
            presenceForCard.remove(token);
            if (presenceForCard.isEmpty()) {
                PRESENCE.remove(cardId);
            }
        }

        Map<String, Consumer<List<PresenceUser>>> listenersForCard = LISTENERS.get(cardId);
        if (listenersForCard != null) {
            listenersForCard.remove(token);
            if (listenersForCard.isEmpty()) {
                LISTENERS.remove(cardId);
            }
        }

        publish(cardId);
    }

    private static void publish(Long cardId) {
        Map<String, Consumer<List<PresenceUser>>> listenersForCard = LISTENERS.get(cardId);
        if (listenersForCard == null || listenersForCard.isEmpty()) {
            return;
        }

        List<PresenceUser> snapshot = snapshot(cardId);

        for (Consumer<List<PresenceUser>> listener : listenersForCard.values()) {
            try {
                listener.accept(snapshot);
            } catch (Exception ignored) {
            }
        }
    }

    private static PresenceUser toPresenceUser(PresenceEntry e) {
        return new PresenceUser(e.userId, e.displayName, e.email, e.mode, e.joinedAt);
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
