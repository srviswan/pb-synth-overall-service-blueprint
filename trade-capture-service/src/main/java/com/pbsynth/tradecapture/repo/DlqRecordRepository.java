package com.pbsynth.tradecapture.repo;

import com.pbsynth.tradecapture.domain.DlqRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DlqRecordRepository extends JpaRepository<DlqRecord, String> {
}
