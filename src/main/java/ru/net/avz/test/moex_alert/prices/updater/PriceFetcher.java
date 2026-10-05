package ru.net.avz.test.moex_alert.prices.updater;

import ru.net.avz.test.moex_alert.prices.PriceEntity;
import ru.net.avz.test.moex_alert.prices.exceptions.PriceFetchException;

import java.time.Duration;
import java.util.List;

/**
 * интеграция с источником цен
 */
public interface PriceFetcher {

    /**
     * @return EMPTY - загрузка/обновление не было выполнено, иначе - множество загруженных цен
     */
    List<PriceEntity> loadAndSavePrices()
            throws PriceFetchException;

    /**
     * @return EMPTY - загрузка/обновление не было выполнено, иначе - множество загруженных цен
     */
    List<PriceEntity> loadAndSavePrices(
            Duration timeout)
            throws PriceFetchException;

}
