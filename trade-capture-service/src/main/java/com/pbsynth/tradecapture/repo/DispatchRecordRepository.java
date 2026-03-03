package com.pbsynth.tradecapture.repo;

import com.pbsynth.tradecapture.domain.DispatchRecord;
import com.pbsynth.tradecapture.domain.DispatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface DispatchRecordRepository extends JpaRepository<DispatchRecord, String> {
    List<DispatchRecord> findTop500ByStatusAndNextAttemptAtBeforeOrderByCreatedAtAsc(DispatchStatus status, Instant nextAttemptAt);

    List<DispatchRecord> findByIngestionIdOrderByCreatedAtAsc(String ingestionId);

    long countByIngestionIdAndStatus(String ingestionId, DispatchStatus status);

    boolean existsByIngestionIdAndDestinationId(String ingestionId, String destinationId);
}
