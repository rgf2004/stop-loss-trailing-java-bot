package com.github.stoploss.core.utils;

import com.github.stoploss.core.enums.Action;

import java.math.BigDecimal;

public class Utils {

    public static BigDecimal calculateThreshold(Action action, BigDecimal currentPrice, BigDecimal percentage) {
        BigDecimal result = null;
        switch (action) {
            case SELL:
                result = currentPrice .subtract ( currentPrice.multiply ( percentage ) );
                break;
            case BUY:
                result = currentPrice.add ( currentPrice.multiply ( percentage ) );
                break;
        }
        return result;
    }

}
