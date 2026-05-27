package com.linkup.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<User> findTop20ByIdNotAndFullNameContainingIgnoreCaseOrIdNotAndEmailContainingIgnoreCase(
            Long id1, String fullName, Long id2, String email);
}
