package com.github.stoploss.core;

import com.github.stoploss.core.enums.Action;
import com.github.stoploss.core.exchange.BittrexExchange;
import com.github.stoploss.core.models.Market;
import com.github.stoploss.core.models.MarketMonitor;
import com.github.stoploss.core.telegram.TelegramBot;
import com.github.stoploss.core.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
public class StopLossEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger ( StopLossEngine.class );

    @Value("${application.sleep.interval}")
    private long sleepTime;

    @Autowired
    private TelegramBot telegramBot;

    @Autowired
    private BittrexExchange bittrex;

    private Map<String, Market> markets = new HashMap<> ();

    private Map<String, MarketMonitor> marketsMonitor = new HashMap<> ();

    public StopLossEngine() {
        //TODO will be configuration
        String percentage = "0.05";
        Market btc = new Market ( new BigDecimal ( percentage ), new BigDecimal ( "0.01" ) );
        Market ltc = new Market ( new BigDecimal ( percentage ), new BigDecimal ( "0.5" ) );
        Market dash = new Market ( new BigDecimal ( percentage ), new BigDecimal ( "0.5" ) );
        Market eth = new Market ( new BigDecimal ( percentage ), new BigDecimal ( "0.1" ) );

        markets.put ( "USDT-BTC", btc );
        markets.put ( "USDT-LTC", ltc );
        markets.put ( "USDT-DASH", dash );
        markets.put ( "USDT-ETH", eth );
    }

    void startEngine() throws InterruptedException {

        while (true) {
            Map<String, BigDecimal> currentPrices = new HashMap<> ();

            markets.forEach ( (marketName, market) -> {
                BigDecimal currentPrice = bittrex.getCurrentPrice ( marketName );
                if (currentPrice != null)
                    currentPrices.put ( marketName, currentPrice );
            } );

            currentPrices.forEach ( this::processNewPrice );

            Thread.sleep ( 1000 * sleepTime );
        }
    }

    private void processNewPrice(String marketName, BigDecimal currentPrice) {

        if (!marketsMonitor.containsKey ( marketName )) { // first time

            MarketMonitor marketMonitor = new MarketMonitor ( currentPrice, markets.get ( marketName ) );

            LOGGER.info ( String.format ( "%10s First Time Market Action %s with current last %05.8f, current threshold %05.8f", marketName, marketMonitor.getAction (), currentPrice, marketMonitor.getCurrentThreshold () ) );

            marketsMonitor.put ( marketName, marketMonitor );

        } else {

            MarketMonitor marketMonitor = marketsMonitor.get ( marketName );

            if (marketMonitor.getCurrentUUId () == null) {

                switch (marketMonitor.getAction ()) {
                    case SELL:
                        if (currentPrice.compareTo ( marketMonitor.getCurrentThreshold () ) <= 0) { // Sell case
                            marketMonitor.setCurrentUUId ( createOrder ( marketMonitor.getAction (), marketName, currentPrice, marketMonitor.getMarket ().getAmount () ) );
                        } else {
                            BigDecimal newSellThreshold = Utils.calculateThreshold ( Action.SELL, currentPrice, marketMonitor.getMarket ().getPercentage () );
                            if (currentPrice.compareTo ( newSellThreshold ) > 0 && newSellThreshold.compareTo ( marketMonitor.getCurrentThreshold () ) > 0) {
                                LOGGER.info ( String.format ( "%10s New Stop Loss will be applied %05.8f", marketName, newSellThreshold ) );
                                marketMonitor.setCurrentThreshold ( newSellThreshold );
                            } else {
                                LOGGER.info ( String.format ( "%10s - Action %s Still current configuration valid with current last %05.8f, current threshold %05.8f", marketName, marketMonitor.getAction (), currentPrice, marketMonitor.getCurrentThreshold () ) );
                            }
                        }

                        break;
                    case BUY:

                        if (currentPrice.compareTo ( marketMonitor.getCurrentThreshold () ) >= 0) { // Buy case
                            LOGGER.info ( String.format ( "%10s Buy Request will be applied at price %05.8f", marketName, currentPrice ) );
                            marketMonitor.setCurrentThreshold ( Utils.calculateThreshold ( Action.SELL, currentPrice, marketMonitor.getMarket ().getPercentage () ) );
                            marketMonitor.setAction ( Action.SELL );
                        } else {
                            BigDecimal newBuyThreshold = Utils.calculateThreshold ( Action.BUY, currentPrice, marketMonitor.getMarket ().getPercentage () );
                            if (currentPrice.compareTo ( newBuyThreshold ) < 0 && newBuyThreshold.compareTo ( marketMonitor.getCurrentThreshold () ) < 0) {
                                LOGGER.info ( String.format ( "%10s New Stop Loss will be applied %05.8f", marketName, newBuyThreshold ) );
                                marketMonitor.setCurrentThreshold ( newBuyThreshold );
                            } else {
                                LOGGER.info ( String.format ( "%10s - Action %s Still current configuration valid with current last %05.8f, current threshold %05.8f", marketName, marketMonitor.getAction (), currentPrice, marketMonitor.getCurrentThreshold () ) );
                            }
                        }
                        break;
                }
            }
            else {
                checkingOpenOrder ( marketName, marketMonitor, currentPrice );
            }
        }
    }

    private void checkingOpenOrder(String marketName, MarketMonitor marketMonitor, BigDecimal currentPrice)
    {
        LOGGER.info ( String.format ( "%10s Current %s Order is in progress ... Checking it...", marketName, marketMonitor.getAction () ) );

        if (bittrex.isOrderOpen ( marketMonitor.getCurrentUUId () )) {
            LOGGER.info ( String.format ( "%10s Current %s Order is still open...", marketName, marketMonitor.getAction () ) );
        } else {
            LOGGER.info ( String.format ( "%10s Current %s Order has been finished", marketName, marketMonitor.getAction () ) );
            marketMonitor.setCurrentUUId ( null );

            if (marketMonitor.getAction () == Action.SELL)
                marketMonitor.setAction ( Action.BUY );
            else
                marketMonitor.setAction ( Action.SELL );

            BigDecimal newThreshold = Utils.calculateThreshold ( marketMonitor.getAction (), currentPrice, marketMonitor.getMarket ().getPercentage () );
            marketMonitor.setCurrentThreshold ( newThreshold );
            LOGGER.info ( String.format ( "%10s New Stop Loss After order will be applied %05.8f", marketName, newThreshold ) );
        }
    }

    private String createOrder(Action action, String marketName, BigDecimal price, BigDecimal amount) {
        LOGGER.info ( String.format ( "%10s New %s Order Request will be applied at price %05.8f, amount %05.8f", marketName, action, price, amount ) );
        telegramBot.sendCustomTextMessage ( String.format ( "%10s New %s Order Request will be applied at price %05.8f, amount %05.8f", marketName, action, price, amount ) );
        return bittrex.placeOrder ( action, marketName, price, amount );
    }
}
