package com.datdevops.hamariadb.entity;



import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "system_alerts")
public class SystemAlert {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SystemAlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertSeverity severity;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(precision = 10, scale = 2)
    private BigDecimal metricValue;

    @Column(precision = 10, scale = 2)
    private BigDecimal thresholdValue;

    @Column(nullable = false)
    private LocalDateTime triggeredAt = LocalDateTime.now();

    private LocalDateTime resolvedAt;

    @Column(nullable = false)
    private Boolean isResolved = false;
}

