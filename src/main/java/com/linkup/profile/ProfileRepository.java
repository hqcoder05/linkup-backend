package com.linkup.profile;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
    @EntityGraph(attributePaths = "user")
    Optional<Profile> findByUserId(Long userId);

    @EntityGraph(attributePaths = "user")
    @Query("select p from Profile p where p.user.id in :userIds")
    List<Profile> findByUserIds(@Param("userIds") List<Long> userIds);
}
