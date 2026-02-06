package com.wakilfly.repository;

import com.wakilfly.model.Report;
import com.wakilfly.model.ReportStatus;
import com.wakilfly.model.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {

    Page<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    Page<Report> findByTypeAndStatusOrderByCreatedAtDesc(ReportType type, ReportStatus status, Pageable pageable);

    Page<Report> findByReporterIdOrderByCreatedAtDesc(UUID reporterId, Pageable pageable);

    Page<Report> findByTargetIdOrderByCreatedAtDesc(UUID targetId, Pageable pageable);

    long countByStatus(ReportStatus status);

    long countByTargetIdAndStatus(UUID targetId, ReportStatus status);

    boolean existsByReporterIdAndTargetIdAndType(UUID reporterId, UUID targetId, ReportType type);

    @Query("SELECT COUNT(r) FROM Report r WHERE r.status = 'PENDING'")
    long countPendingReports();

    @Query("SELECT r.type, COUNT(r) FROM Report r WHERE r.status = 'PENDING' GROUP BY r.type")
    Object[][] countPendingByType();
}
