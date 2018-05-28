package com.github.stoploss.core;

import com.github.stoploss.core.enums.Action;
import com.github.stoploss.core.models.Market;
import com.github.stoploss.core.models.MarketMonitor;
import com.github.stoploss.core.utils.Utils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class CurrenciesManager {

    private Map<String, MarketMonitor> marketsMonitor = new HashMap<> ();

    public CurrenciesManager()
    {
        //TODO will be configuration
        String percentage = "0.05";
        String amount = "0.01";

        MarketMonitor ltc = new MarketMonitor ( Action.SELL, new BigDecimal (amount), new BigDecimal ( percentage ) );
        this.marketsMonitor.put ( "USDT-LTC", ltc );

        MarketMonitor dash = new MarketMonitor ( Action.SELL, new BigDecimal (amount), new BigDecimal ( percentage ) );
        this.marketsMonitor.put ( "USDT-DASH", dash );

        MarketMonitor eth = new MarketMonitor ( Action.SELL, new BigDecimal (amount), new BigDecimal ( percentage ) );
        this.marketsMonitor.put ( "USDT-ETH", eth );

        MarketMonitor btc = new MarketMonitor ( Action.SELL, new BigDecimal (amount), new BigDecimal ( percentage ) );
        this.marketsMonitor.put ( "USDT-BTC", btc );

    }

    public Map<String, MarketMonitor> getMarketsMonitor()
    {
        return new HashMap<> ( marketsMonitor );
    }

    public MarketMonitor getMarketMonitor(String marketName)
    {
        return marketsMonitor.get ( marketName );
    }

    public void initThreshold(String marketName, BigDecimal currentPrice)
    {
        Action action = marketsMonitor.get ( marketName ).getAction ();
        BigDecimal percentage = marketsMonitor.get ( marketName ).getPercentage ();

        BigDecimal threshold = Utils.calculateThreshold ( action, currentPrice, percentage );
        marketsMonitor.get ( marketName ).setCurrentThreshold ( threshold );
    }
}
