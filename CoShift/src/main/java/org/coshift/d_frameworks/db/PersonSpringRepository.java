package org.coshift.d_frameworks.db;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersonSpringRepository extends JpaRepository<PersonJpaEntity, Long> {

    Optional<PersonJpaEntity> findByNicknameIgnoreCase(String nickname);
}