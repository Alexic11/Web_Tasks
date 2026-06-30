package carobnifrulas.web_tasks.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    long countByUserIdAndReadFalse(Long userId);

    List<NotificationEntity> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);

    List<NotificationEntity> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE notifications
               SET is_read = true,
                   read_at = :readAt
             WHERE user_id = :userId
               AND is_read = false
            """, nativeQuery = true)
    int markAllUnreadAsRead(@Param("userId") Long userId,
                            @Param("readAt") Instant readAt);

    Optional<NotificationEntity> findByIdAndUserId(Long id, Long userId);
}
