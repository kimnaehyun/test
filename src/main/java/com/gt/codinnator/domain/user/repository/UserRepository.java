package com.gt.codinnator.domain.user.repository;

import com.gt.codinnator.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // Github 고유 ID로 기존 가입 여부 확인
    Optional<User> findByGitId(String gitId);
}