package com.pp.smarthealth.repository;

import com.pp.smarthealth.model.ReportImportDraft;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface ReportImportDraftRepository extends JpaRepository<ReportImportDraft,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from ReportImportDraft d where d.id = :id")
    Optional<ReportImportDraft> lockById(@Param("id") Long id);
}
