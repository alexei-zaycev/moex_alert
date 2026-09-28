package ru.net.avz.test.moex_alert.tickers;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.net.avz.test.moex_alert.tickers.dto.TickerCreateDto;
import ru.net.avz.test.moex_alert.tickers.dto.TickerPatchDto;
import ru.net.avz.test.moex_alert.tickers.dto.TickerUpdateDto;
import ru.net.avz.test.moex_alert.tickers.exceptions.TickerAlreadyExistsException;
import ru.net.avz.test.moex_alert.tickers.exceptions.TickerNotFoundException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TickerService {

    private final TickerRepository tickerRepository;

    public List<TickerEntity> findAll() {
        return tickerRepository.findAll();
    }

    public Page<TickerEntity> findAll(Pageable pageable) {
        return tickerRepository.findAll(pageable);
    }

    public TickerEntity findByNameOrThrow(
            String tickerName
    ) {
        return tickerRepository.findByName(tickerName)
                .orElseThrow(() -> new TickerNotFoundException(tickerName));
    }

    @Transactional
    public TickerEntity create(
            TickerCreateDto tickerNew
    ) {
        Optional<TickerEntity> tickerCurrent = tickerRepository.findByName(tickerNew.name());
        if (tickerCurrent.isPresent()) {
            throw new TickerAlreadyExistsException(tickerNew.name());
        } else {
            return tickerRepository.saveAndFlush(tickerNew.toBuilder().build());
        }
    }

    @Transactional
    public TickerEntity update(
            String tickerName,
            TickerUpdateDto tickerNew
    ) {
        TickerEntity tickerCurrent = findByNameOrThrow(tickerName);
        return tickerRepository.saveAndFlush(tickerNew.applyTo(tickerCurrent).build());
    }

    @Transactional
    public TickerEntity patch(
            String tickerName,
            TickerPatchDto tickerPatch
    ) {
        TickerEntity tickerCurrent = findByNameOrThrow(tickerName);
        return tickerRepository.saveAndFlush(tickerPatch.applyTo(tickerCurrent).build());
    }

    @Transactional
    public void delete(
            String tickerName
    ) {
        TickerEntity tickerCurrent = findByNameOrThrow(tickerName);
        tickerRepository.deleteById(tickerCurrent.getId());
    }
}
