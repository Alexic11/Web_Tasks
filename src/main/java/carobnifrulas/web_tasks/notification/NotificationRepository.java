package carobnifrulas.web_tasks.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    long countByUserIdAndReadFalse(Long userId);

    List<NotificationEntity> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);

    List<NotificationEntity> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<NotificationEntity> findByIdAndUserId(Long id, Long userId);
}