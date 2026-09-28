package ru.net.avz.test.moex_alert.tickers;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface TickerRepository extends JpaRepository<TickerEntity, UUID> {

    Optional<TickerEntity> findByName(String name);
}
