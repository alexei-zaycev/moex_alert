package ru.net.avz.test.moex_alert.prices;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PriceRepository
        extends JpaRepository<PriceEntity, UUID> {

}
