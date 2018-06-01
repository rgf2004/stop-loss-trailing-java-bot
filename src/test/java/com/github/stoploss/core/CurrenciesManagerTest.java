package com.github.stoploss.core;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;

@RunWith(SpringRunner.class)
public class CurrenciesManagerTest {

    //TODO make it autowiring
    CurrenciesManager currenciesManager = new CurrenciesManager ();

    @Test
    public void testAddNewCurrency() {
        String marketName = "USDT-LTC";
        String amount = "0.05";
        String percentage = "0.05";
        currenciesManager.addCurrency ( marketName, amount, percentage );
        Assert.assertEquals ( 1, currenciesManager.getMarketsMonitor ().size () );
        Assert.assertTrue ( currenciesManager.getAmount ( marketName ).compareTo ( new BigDecimal ( amount ) ) == 0 );
        Assert.assertTrue ( currenciesManager.getPercentage ( marketName ).compareTo ( new BigDecimal ( percentage ) ) == 0 );
    }

    @Test
    public void testAddInvalidCurrency() throws Exception {
        String marketName = "USDT-LTC";
        String amount = "invalid amount";
        String percentage = "invalid percentage";
        try {
            currenciesManager.addCurrency ( marketName, amount, percentage );
            throw new Exception ( "Invalid behaviour" );
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testUpdateExistingCurrency() {
        String marketName = "USDT-LTC";
        String amount = "0.05";
        String percentage = "0.05";
        currenciesManager.addCurrency ( marketName, amount, percentage );

        amount = "0.06";
        percentage = "0.07";
        currenciesManager.addCurrency ( marketName, amount, percentage );
        Assert.assertEquals ( 1, currenciesManager.getMarketsMonitor ().size () );
        Assert.assertTrue ( currenciesManager.getAmount ( marketName ).compareTo ( new BigDecimal ( amount ) ) == 0 );
        Assert.assertTrue ( currenciesManager.getPercentage ( marketName ).compareTo ( new BigDecimal ( percentage ) ) == 0 );
    }

    @Test
    public void testAddAndUpdateExistingCurrency() {
        String marketName = "USDT-LTC";
        String amount = "0.05";
        String percentage = "0.05";
        currenciesManager.addCurrency ( marketName, amount, percentage );

        marketName = "USDT-BTC";
        amount = "0.06";
        percentage = "0.07";
        currenciesManager.addCurrency ( marketName, amount, percentage );
        Assert.assertEquals ( 2, currenciesManager.getMarketsMonitor ().size () );
    }

}