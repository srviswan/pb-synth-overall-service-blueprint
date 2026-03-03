package com.pbsynth.tradecapture.repo;

import com.pbsynth.tradecapture.domain.IngestionRecord;
import com.pbsynth.tradecapture.domain.IngestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IngestionRecordRepository extends JpaRepository<IngestionRecord, String> {
    Optional<IngestionRecord> findByIdempotencyKey(String idempotencyKey);
    List<IngestionRecord> findTop100ByStatusOrderByCreatedAtAsc(IngestionStatus status);
}
