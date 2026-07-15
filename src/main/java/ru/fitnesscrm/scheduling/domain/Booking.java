package ru.fitnesscrm.scheduling.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ru.fitnesscrm.common.domain.TenantEntity;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "bookings")
public class Booking extends TenantEntity {

    @Column(name = "class_session_id", nullable = false)
    private Long classSessionId;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "booking_status")
    private BookingStatus status = BookingStatus.CONFIRMED;

    @Column(name = "booked_at", nullable = false)
    private Instant bookedAt = Instant.now();

    @Column(name = "cancelled_at")
    private Instant cancelledAt;
}
