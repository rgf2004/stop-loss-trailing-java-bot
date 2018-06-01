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
import org.springframework.util.StringUtils;

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

            Map<String, BigDecimal> currentPrices = currenciesManager.getMarketsMonitor ().keySet ().stream ().map ( this::getPriceByMarketName ).filter ( out -> out != null ).collect ( Collectors.toMap ( Map.Entry::getKey, Map.Entry::getValue ) );

            currentPrices.forEach ( this::processNewPrice );

            Thread.sleep ( 1000 * sleepTime );
        }
    }

    private Map.Entry<String, BigDecimal> getPriceByMarketName(String marketName) {
        try {
            BigDecimal currentPrice = bittrex.getCurrentPrice ( marketName );
            return new AbstractMap.SimpleEntry<> ( marketName, currentPrice );
        } catch (IllegalArgumentException e)
        {
            telegramBot.sendCustomTextMessage ( String.format ( "Invalid Market %10s and will be removed ", marketName ) );
            currenciesManager.removeCurrency ( marketName );
            return null;
        }
    }

    private void processNewPrice(String marketName, BigDecimal currentPrice) {

        if (currenciesManager.getCurrentPrice (marketName) == null) { // first time

            currenciesManager.initThreshold ( marketName, currentPrice );

            LOGGER.info ( String.format ( "%10s First Time - Action %s with current last %05.8f, current threshold %05.8f", marketName, currenciesManager.getAction (marketName), currentPrice, currenciesManager.getCurrentThreshold (marketName) ) );
        } else {

            currenciesManager.setCurrentPrice ( marketName, currentPrice );

            if (currenciesManager.getCurrentUUId (marketName) == null) {

                switch (currenciesManager.getAction (marketName)) {
                    case SELL:
                        if (currentPrice.compareTo ( currenciesManager.getCurrentThreshold (marketName) ) <= 0) { // Sell case
                            currenciesManager.setCurrentUUId ( marketName, createOrder ( currenciesManager.getAction (marketName), marketName, currentPrice, currenciesManager.getAmount (marketName) ) );
                        } else {
                            BigDecimal newSellThreshold = Utils.calculateThreshold ( Action.SELL, currentPrice, currenciesManager.getPercentage (marketName) );
                            if (currentPrice.compareTo ( newSellThreshold ) > 0 && newSellThreshold.compareTo ( currenciesManager.getCurrentThreshold (marketName) ) > 0) {
                                LOGGER.info ( String.format ( "%10s - Action %s New Stop Loss will be applied %05.8f", marketName, currenciesManager.getAction (marketName) ,  newSellThreshold ) );
                                currenciesManager.setCurrentThreshold ( marketName, newSellThreshold );
                            } else {
                                LOGGER.info ( String.format ( "%10s - Action %s Still current configuration valid with current last %05.8f, current threshold %05.8f", marketName, currenciesManager.getAction (marketName), currentPrice, currenciesManager.getCurrentThreshold (marketName) ) );
                            }
                        }
                        break;
                    case BUY:

                        if (currentPrice.compareTo ( currenciesManager.getCurrentThreshold (marketName) ) >= 0) { // Buy case
                            currenciesManager.setCurrentUUId ( marketName, createOrder ( currenciesManager.getAction (marketName), marketName, currentPrice, currenciesManager.getAmount (marketName) ) );
                        } else {
                            BigDecimal newBuyThreshold = Utils.calculateThreshold ( Action.BUY, currentPrice, currenciesManager.getPercentage (marketName) );
                            if (currentPrice.compareTo ( newBuyThreshold ) < 0 && newBuyThreshold.compareTo ( currenciesManager.getCurrentThreshold (marketName) ) < 0) {
                                LOGGER.info ( String.format ( "%10s - Action %s New Stop Loss will be applied %05.8f", marketName, currenciesManager.getAction (marketName) ,  newBuyThreshold ) );
                                currenciesManager.setCurrentThreshold ( marketName, newBuyThreshold );
                            } else {
                                LOGGER.info ( String.format ( "%10s - Action %s Still current configuration valid with current last %05.8f, current threshold %05.8f", marketName, currenciesManager.getAction (marketName), currentPrice, currenciesManager.getCurrentThreshold (marketName) ) );
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
            currenciesManager.setCurrentThreshold ( marketName, newThreshold );
            LOGGER.info ( String.format ( "%10s New Stop Loss After order will be applied %05.8f", marketName, newThreshold ) );
        }
    }

    private String createOrder(Action action, String marketName, BigDecimal price, BigDecimal amount) {
        LOGGER.info ( String.format ( "%10s New %s Order Request will be applied at price %05.8f, amount %05.8f", marketName, action, price, amount ) );
        String uuid =  bittrex.placeOrder ( action, marketName, price, amount );
        if (uuid != null)
        {
            telegramBot.sendCustomTextMessage ( String.format ( "%10s New %s Order Request has been applied at price %05.8f, amount %05.8f", marketName, action, price, amount ) );
        }
        else
        {
            telegramBot.sendCustomTextMessage ( String.format ( "%10s New %s Order Request has been failed PLEASE take an action", marketName, action) );
        }
        return uuid;
    }
}
