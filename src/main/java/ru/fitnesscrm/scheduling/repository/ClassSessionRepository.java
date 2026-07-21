package ru.fitnesscrm.scheduling.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.fitnesscrm.scheduling.domain.ClassSession;

import java.time.Instant;
import java.util.List;

public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {

    @Query("""
            SELECT cs FROM ClassSession cs
            WHERE cs.trainerId = :trainerId
              AND cs.startTime < :endTime
              AND cs.endTime > :startTime
            """)
    List<ClassSession> findTrainerConflicts(
            @Param("trainerId") Long trainerId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );

    @Query("""
            SELECT cs FROM ClassSession cs
            WHERE cs.facilityId = :facilityId
              AND cs.startTime < :endTime
              AND cs.endTime > :startTime
            """)
    List<ClassSession> findFacilityConflicts(
            @Param("facilityId") Long facilityId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );

    List<ClassSession> findByEndTimeBefore(Instant time);
}
