package ru.fitnesscrm.scheduling.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.fitnesscrm.scheduling.domain.Booking;
import ru.fitnesscrm.scheduling.domain.BookingStatus;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    long countByClassSessionIdAndStatus(Long classSessionId, BookingStatus status);
}
