package engine.nexus.repository;

import engine.nexus.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    List<Payment> findByFromAccountId(UUID accountId);
    List<Payment> findByToAccountId(UUID accountId);
    List<Payment> findByFromAccountIdOrToAccountId(UUID fromId, UUID toId);
    List<Payment> findByStatus(Payment.PaymentStatus status);
}
