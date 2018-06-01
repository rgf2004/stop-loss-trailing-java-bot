package com.github.stoploss.core;

import com.github.stoploss.core.enums.Action;
import com.github.stoploss.core.models.MarketMonitor;
import com.github.stoploss.core.utils.Utils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class CurrenciesManager {

    private Map<String, MarketMonitor> marketsMonitor = new HashMap<> ();

    public CurrenciesManager() {
    }

    public void addCurrency(String marketNameString, String amountString, String percentageString) throws IllegalArgumentException
    {
        BigDecimal amount = null;
        BigDecimal percentage = null;

        String marketName = marketNameString.toUpperCase ();

        try
        {
            amount = new BigDecimal ( amountString );
            percentage = new BigDecimal ( percentageString );
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException ( "value of amount|percentage is invalid" );
        }

        if (this.marketsMonitor.containsKey ( marketName ))
        {
            if ( getPercentage ( marketName ).compareTo ( percentage ) != 0)
                setCurrentPrice ( marketName, null ); // this is mandatory to recalculate the threshold

            setAmount ( marketName, amount );
            setPercentage ( marketName, percentage );
        }
        else
        {
            //TODO there should be check on valid market name
            //TODO action also should be parameter and not be default sell
            MarketMonitor market = new MarketMonitor ( Action.SELL, amount, percentage );
            this.marketsMonitor.put ( marketName , market );
        }
    }

    MarketMonitor getMarketMonitor(String marketName) {
        return marketsMonitor.get ( marketName );
    }

    void initThreshold(String marketName, BigDecimal currentPrice) {
        Action action = marketsMonitor.get ( marketName ).getAction ();
        BigDecimal percentage = marketsMonitor.get ( marketName ).getPercentage ();

        BigDecimal threshold = Utils.calculateThreshold ( action, currentPrice, percentage );
        marketsMonitor.get ( marketName ).setCurrentThreshold ( threshold );
        marketsMonitor.get ( marketName ).setCurrentPrice ( currentPrice );
    }

    public String buildResponseMessage(Map.Entry<String, MarketMonitor> entry) {
        return String.format ( "Currency %s, Amount %.8f, Action %s, Price %.8f, Threshold %.8f",
                entry.getKey (),
                entry.getValue ().getAmount (),
                entry.getValue ().getAction (),
                entry.getValue ().getCurrentPrice (),
                entry.getValue ().getCurrentThreshold () );
    }

    //TODO should be find a better way to set variables
    Action getAction(String marketName) {
        return marketsMonitor.get(marketName).getAction ();
    }

    void inverseAction(String marketName) {
        if (marketsMonitor.get ( marketName ).getAction () == Action.SELL)
            marketsMonitor.get ( marketName ).setAction ( Action.BUY );
        else
            marketsMonitor.get ( marketName ).setAction ( Action.SELL );
    }

    BigDecimal getAmount (String marketName) {
        return marketsMonitor.get ( marketName ).getAmount ();
    }

    void setAmount (String marketName, BigDecimal amount){
        marketsMonitor.get ( marketName ).setAmount ( amount );
    }

    BigDecimal getPercentage(String marketName) {
        return marketsMonitor.get ( marketName ).getPercentage ();
    }

    void setPercentage(String marketName, BigDecimal percentage) {
        marketsMonitor.get ( marketName ).setPercentage ( percentage );
    }

    BigDecimal getCurrentPrice(String marketName){
        return marketsMonitor.get ( marketName ).getCurrentPrice ();
    }

    void setCurrentPrice(String marketName, BigDecimal currentPrice) {
        marketsMonitor.get ( marketName ).setCurrentPrice ( currentPrice );
    }

    BigDecimal getCurrentThreshold(String marketName){
        return marketsMonitor.get ( marketName ).getCurrentThreshold ();
    }

    void setCurrentThreshold(String marketName, BigDecimal threshold) {
        marketsMonitor.get ( marketName ).setCurrentThreshold ( threshold );
    }

    String getCurrentUUId(String marketName){
        return marketsMonitor.get ( marketName ).getCurrentUUId ();
    }

    void setCurrentUUId(String marketName, String uuid) {
        marketsMonitor.get ( marketName ).setCurrentUUId ( uuid );
    }

    public Map<String, MarketMonitor> getMarketsMonitor() {
        return new HashMap<> ( marketsMonitor );
    }

}
