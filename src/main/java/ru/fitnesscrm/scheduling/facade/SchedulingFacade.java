package ru.fitnesscrm.scheduling.facade;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import ru.fitnesscrm.scheduling.domain.BookingStatus;
import ru.fitnesscrm.scheduling.domain.ClassSession;
import ru.fitnesscrm.scheduling.repository.BookingRepository;
import ru.fitnesscrm.scheduling.repository.ClassSessionRepository;

import java.time.Instant;
import java.util.List;

@Component
@AllArgsConstructor
public class SchedulingFacade {

    private final BookingRepository bookingRepository;
    private final ClassSessionRepository classSessionRepository;

    public List<ClassSession> getEndedClassSessions(Instant time) {
        return classSessionRepository.findByEndTimeBefore(time);
    }

    public long countAttendedBookingsBySessionId(Long sessionId) {
        return bookingRepository.countByClassSessionIdAndStatus(sessionId, BookingStatus.ATTENDED);
    }

}
