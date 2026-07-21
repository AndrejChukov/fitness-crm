package ru.fitnesscrm.scheduling.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.fitnesscrm.scheduling.domain.Booking;
import ru.fitnesscrm.scheduling.domain.BookingStatus;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    long countByClassSessionIdAndStatus(@Param("classSessionId") Long classSessionId, @Param("status") BookingStatus status);

    List<Booking> findByStatus(BookingStatus bookingStatus);
}
