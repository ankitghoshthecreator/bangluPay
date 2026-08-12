package engine.nexus.repository;

import engine.nexus.model.UserAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserAuthRepository extends JpaRepository<UserAuth, UUID> {
    Optional<UserAuth> findByUsername(String username);
    Optional<UserAuth> findByCustomerId(UUID customerId);
}
