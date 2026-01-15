package com.example.closestv2.domain.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<MemberRoot, Long> {
    Optional<MemberRoot> findByMemberInfoUserEmail(String email);

    boolean existsByMemberInfoUserEmail(String email);
}
