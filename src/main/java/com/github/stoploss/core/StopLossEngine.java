package com.github.stoploss.core;

import com.github.stoploss.core.enums.Action;
import com.github.stoploss.core.exchange.BittrexExchange;
import com.github.stoploss.core.models.MarketMonitor;
import com.github.stoploss.core.telegram.TelegramBot;
import com.github.stoploss.core.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
class StopLossEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger ( StopLossEngine.class );

    @Value("${application.sleep.interval}")
    private long sleepTime;

    @Autowired
    private TelegramBot telegramBot;

    @Autowired
    private BittrexExchange bittrex;

    @Autowired
    private CurrenciesManager currenciesManager;

    void startEngine() throws InterruptedException {

        while (true) {

            Map<String, BigDecimal> currentPrices = currenciesManager.getMarketsMonitor ().keySet ().stream ().map ( this::getPriceByMarketName ).collect ( Collectors.toMap ( Map.Entry::getKey, Map.Entry::getValue ) );

            currentPrices.forEach ( this::processNewPrice );

            Thread.sleep ( 1000 * sleepTime );
        }
    }

    private Map.Entry<String, BigDecimal> getPriceByMarketName(String marketName) {
        BigDecimal currentPrice = bittrex.getCurrentPrice ( marketName );
        return new AbstractMap.SimpleEntry<> ( marketName, currentPrice );
    }

    private void processNewPrice(String marketName, BigDecimal currentPrice) {

        MarketMonitor marketMonitor = currenciesManager.getMarketMonitor ( marketName );

        if (marketMonitor.getCurrentPrice () == null) { // first time

            currenciesManager.initThreshold ( marketName, currentPrice );

            LOGGER.info ( String.format ( "%10s First Time - Action %s with current last %05.8f, current threshold %05.8f", marketName, marketMonitor.getAction (), currentPrice, marketMonitor.getCurrentThreshold () ) );
        } else {

            currenciesManager.setCurrentPrice ( marketName, currentPrice );

            if (marketMonitor.getCurrentUUId () == null) {

                switch (marketMonitor.getAction ()) {
                    case SELL:
                        if (currentPrice.compareTo ( marketMonitor.getCurrentThreshold () ) <= 0) { // Sell case
                            currenciesManager.setCurrentUUId ( marketName, createOrder ( marketMonitor.getAction (), marketName, currentPrice, marketMonitor.getAmount () ) );
                        } else {
                            BigDecimal newSellThreshold = Utils.calculateThreshold ( Action.SELL, currentPrice, marketMonitor.getPercentage () );
                            if (currentPrice.compareTo ( newSellThreshold ) > 0 && newSellThreshold.compareTo ( marketMonitor.getCurrentThreshold () ) > 0) {
                                LOGGER.info ( String.format ( "%10s - Action %s New Stop Loss will be applied %05.8f", marketName, marketMonitor.getAction () ,  newSellThreshold ) );
                                currenciesManager.setThreshold ( marketName, newSellThreshold );
                            } else {
                                LOGGER.info ( String.format ( "%10s - Action %s Still current configuration valid with current last %05.8f, current threshold %05.8f", marketName, marketMonitor.getAction (), currentPrice, marketMonitor.getCurrentThreshold () ) );
                            }
                        }
                        break;
                    case BUY:

                        if (currentPrice.compareTo ( marketMonitor.getCurrentThreshold () ) >= 0) { // Buy case
                            currenciesManager.setCurrentUUId ( marketName, createOrder ( marketMonitor.getAction (), marketName, currentPrice, marketMonitor.getAmount () ) );
                        } else {
                            BigDecimal newBuyThreshold = Utils.calculateThreshold ( Action.BUY, currentPrice, marketMonitor.getPercentage () );
                            if (currentPrice.compareTo ( newBuyThreshold ) < 0 && newBuyThreshold.compareTo ( marketMonitor.getCurrentThreshold () ) < 0) {
                                LOGGER.info ( String.format ( "%10s - Action %s New Stop Loss will be applied %05.8f", marketName, marketMonitor.getAction () ,  newBuyThreshold ) );
                                currenciesManager.setThreshold ( marketName, newBuyThreshold );
                            } else {
                                LOGGER.info ( String.format ( "%10s - Action %s Still current configuration valid with current last %05.8f, current threshold %05.8f", marketName, marketMonitor.getAction (), currentPrice, marketMonitor.getCurrentThreshold () ) );
                            }
                        }
                        break;
                }
            } else {
                checkingOpenOrder ( marketName, currentPrice );
            }
        }
    }

    private void checkingOpenOrder(String marketName, BigDecimal currentPrice) {
        LOGGER.info ( String.format ( "%10s Current %s Order is in progress ... Checking it...", marketName, currenciesManager.getMarketMonitor ( marketName ).getAction () ) );

        if (bittrex.isOrderOpen ( currenciesManager.getMarketMonitor ( marketName ).getCurrentUUId () )) {
            LOGGER.info ( String.format ( "%10s Current %s Order is still open...", marketName, currenciesManager.getMarketMonitor ( marketName ).getAction () ) );
        } else {
            LOGGER.info ( String.format ( "%10s Current %s Order has been finished", marketName, currenciesManager.getMarketMonitor ( marketName ).getAction () ) );
            currenciesManager.setCurrentUUId ( marketName, null );
            currenciesManager.inverseAction ( marketName );
            BigDecimal newThreshold = Utils.calculateThreshold ( currenciesManager.getMarketMonitor ( marketName ).getAction (), currentPrice, currenciesManager.getMarketMonitor ( marketName ).getPercentage () );
            currenciesManager.setThreshold ( marketName, newThreshold );
            LOGGER.info ( String.format ( "%10s New Stop Loss After order will be applied %05.8f", marketName, newThreshold ) );
        }
    }

    private String createOrder(Action action, String marketName, BigDecimal price, BigDecimal amount) {
        LOGGER.info ( String.format ( "%10s New %s Order Request will be applied at price %05.8f, amount %05.8f", marketName, action, price, amount ) );
        telegramBot.sendCustomTextMessage ( String.format ( "%10s New %s Order Request will be applied at price %05.8f, amount %05.8f", marketName, action, price, amount ) );
        return bittrex.placeOrder ( action, marketName, price, amount );
    }
}
