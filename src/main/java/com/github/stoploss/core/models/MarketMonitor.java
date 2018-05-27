package com.github.stoploss.core.models;

import com.github.stoploss.core.enums.Action;
import com.github.stoploss.core.utils.Utils;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@ToString
public class MarketMonitor {
    private Market market;
    private BigDecimal currentThreshold;
    private BigDecimal currentPrice;
    private Action action;
    private String currentUUId;

    public MarketMonitor (BigDecimal currentPrice, Market market) {
        this.setAction ( Action.SELL );
        this.setCurrentPrice ( currentPrice );
        this.setMarket ( market );
        this.setCurrentThreshold ( Utils.calculateThreshold ( Action.SELL, currentPrice, market.getPercentage () ) );
    }
}
