package com.gt.codinnator.domain.ai.repository;

import com.gt.codinnator.domain.ai.entity.AiTestReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface AiTestReportRepository extends JpaRepository<AiTestReport, Long> {

    List<AiTestReport> findByUserIdOrderByCreatedAtDesc(Long userId);

    // ✅ 내가 참여한 방들의 report를 한번에 가져오기 (roomId 목록 기반)
    List<AiTestReport> findByRoomIdInOrderByCreatedAtDesc(Collection<Long> roomIds);
}
