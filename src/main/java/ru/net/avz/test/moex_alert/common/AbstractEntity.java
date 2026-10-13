package ru.net.avz.test.moex_alert.common;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.UUID;

@MappedSuperclass
public abstract class AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Getter
    private UUID id;

    @CreatedDate
    @Column(updatable = false)
    @NotNull
    @Getter
    private LocalDateTime createdAt;

    @Version
    @LastModifiedDate
    @Column
    @NotNull
    @Getter
    private LocalDateTime updatedAt;
}
