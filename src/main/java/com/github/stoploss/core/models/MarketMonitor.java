package com.github.stoploss.core.models;

import com.github.stoploss.core.enums.Action;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@ToString
public class MarketMonitor {

    private Action action;
    private BigDecimal amount;
    private BigDecimal percentage;
    private BigDecimal currentPrice;
    private BigDecimal currentThreshold;
    private String currentUUId;

    public MarketMonitor(Action action, BigDecimal amount, BigDecimal percentage)
    {
        this.action = action;
        this.amount = amount;
        this.percentage = percentage;
        this.currentPrice = null;
        this.currentThreshold = null;
    }
}
