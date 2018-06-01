package com.github.stoploss.core.exchange;

import com.github.stoploss.core.enums.Action;
import de.elbatya.cryptocoins.bittrexclient.BittrexClient;
import de.elbatya.cryptocoins.bittrexclient.api.model.common.ApiResult;
import de.elbatya.cryptocoins.bittrexclient.api.model.common.BittrexApiException;
import de.elbatya.cryptocoins.bittrexclient.api.model.marketapi.OpenOrder;
import de.elbatya.cryptocoins.bittrexclient.api.model.marketapi.OrderCreated;
import de.elbatya.cryptocoins.bittrexclient.config.ApiCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class BittrexExchange {

    private static final Logger LOGGER = LoggerFactory.getLogger ( BittrexExchange.class );

    @Value("${application.bittrex.api}")
    private String bittrexApiKey;

    @Value("${application.bittrex.secret}")
    private String bittrexSecret;

    private BittrexClient bittrexClient;

    @PostConstruct
    public void init() {
        // Ceate ApiCredentials with ApiKey and Secret from bittrex.com
        ApiCredentials credentials = new ApiCredentials ( bittrexApiKey, bittrexSecret );

        // Create a BittrexClient with the ApiCredentials
        bittrexClient = new BittrexClient ( credentials );
    }

    public BigDecimal getCurrentPrice(String marketName) throws IllegalArgumentException{
        try {
            return bittrexClient.getPublicApi ().getTicker ( marketName ).unwrap ().getLast ();
        } catch (BittrexApiException e) {
            LOGGER.error ( "Error While Getting Ticker", e );
            throw new IllegalArgumentException("Invalid Market");
        }
    }

    public String placeOrder(Action action, String marketName, BigDecimal price, BigDecimal amount) {
        ApiResult<OrderCreated> order = null;
        switch (action) {
            case SELL:
                order = bittrexClient.getMarketApi ().sellLimit ( marketName, amount, price );
                break;
            case BUY:
                order = bittrexClient.getMarketApi ().buyLimit ( marketName, amount, price );
                break;
        }

        try {
            String uuid = order.unwrap ().getUuid ();
            LOGGER.info ( String.format ( "%10s New %s Order has been created successfully %s", marketName, action, uuid ) );
            return uuid;
        } catch (BittrexApiException e) {
            LOGGER.error ( "Order couldn't been created ", e );
            return null;
        }
    }

    public boolean isOrderOpen(String uuid) {
        List<String> openOrders = bittrexClient.getMarketApi ().getOpenOrders ().unwrap ().stream ().map ( OpenOrder::getOrderUuid ).filter ( orderUuid -> orderUuid.equalsIgnoreCase ( uuid ) ).collect ( Collectors.toList () );

        return (openOrders != null && openOrders.size () > 0);
    }
}
