package carobnifrulas.web_tasks.notification;

import carobnifrulas.web_tasks.card.Card;
import carobnifrulas.web_tasks.user.User;
import carobnifrulas.web_tasks.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notifications;
    private final UserRepository users;

    public NotificationService(NotificationRepository notifications,
                               UserRepository users) {
        this.notifications = notifications;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        requireUser(userId);
        return notifications.countByUserIdAndReadFalse(userId);
    }

    @Transactional(readOnly = true)
    public List<NotificationEntity> listLatest(Long userId) {
        requireUser(userId);
        return notifications.findTop10ByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<NotificationEntity> listLatest(Long userId, int limit) {
        requireUser(userId);

        if (limit <= 10) {
            return notifications.findTop10ByUserIdOrderByCreatedAtDesc(userId);
        }
        return notifications.findTop20ByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        NotificationEntity n = notifications.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new IllegalStateException("Notifikacija nije pronađena."));

        if (!n.isRead()) {
            n.setRead(true);
            n.setReadAt(Instant.now());
            notifications.save(n);
            UserNotificationBus.publish(userId);
        }
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        requireUser(userId);

        List<NotificationEntity> all = notifications.findTop20ByUserIdOrderByCreatedAtDesc(userId);
        boolean changed = false;

        for (NotificationEntity n : all) {
            if (!n.isRead()) {
                n.setRead(true);
                n.setReadAt(Instant.now());
                changed = true;
            }
        }

        if (changed) {
            notifications.saveAll(all);
            UserNotificationBus.publish(userId);
        }
    }

    @Transactional
    public void createTaskAssignedNotification(User recipient,
                                               User actor,
                                               Card card) {
        if (recipient == null || card == null) {
            return;
        }

        if (actor != null && recipient.getId().equals(actor.getId())) {
            return;
        }

        NotificationEntity n = new NotificationEntity();
        n.setUserId(recipient.getId());
        n.setType(NotificationType.TASK_ASSIGNED);
        n.setTitle("Dodijeljen vam je novi task");
        n.setMessage(buildAssignedMessage(actor, card));
        n.setBoardId(card.getBoardId());
        n.setCardId(card.getId());
        n.setCreatedBy(actor != null ? actor.getId() : null);
        n.setRead(false);

        notifications.save(n);
        UserNotificationBus.publish(recipient.getId());
    }

    @Transactional
    public void createTaskReassignedNotification(User recipient,
                                                 User actor,
                                                 Card card) {
        if (recipient == null || card == null) {
            return;
        }

        if (actor != null && recipient.getId().equals(actor.getId())) {
            return;
        }

        NotificationEntity n = new NotificationEntity();
        n.setUserId(recipient.getId());
        n.setType(NotificationType.TASK_REASSIGNED);
        n.setTitle("Dodijeljen vam je task");
        n.setMessage(buildAssignedMessage(actor, card));
        n.setBoardId(card.getBoardId());
        n.setCardId(card.getId());
        n.setCreatedBy(actor != null ? actor.getId() : null);
        n.setRead(false);

        notifications.save(n);
        UserNotificationBus.publish(recipient.getId());
    }

    private String buildAssignedMessage(User actor, Card card) {
        String actorName = (actor == null || actor.getFullName() == null || actor.getFullName().isBlank())
                ? "Nepoznat korisnik"
                : actor.getFullName();

        String title = (card.getTitle() == null || card.getTitle().isBlank())
                ? "(bez naslova)"
                : card.getTitle();

        return actorName + " vam je dodijelio task: " + title;
    }

    private User requireUser(Long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User ne postoji: " + userId));
    }
}