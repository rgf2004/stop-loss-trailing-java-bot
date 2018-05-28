package com.github.stoploss.core.telegram;

import com.github.stoploss.core.CurrenciesManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
class TelegramRequestHandler {

    @Autowired
    private CurrenciesManager currenciesManager;

    List<String> handleRequest(String message)
    {
        //TODO in the future there will be actions against messages but for now any request will be replied by status
        return currenciesManager
                .getMarketsMonitor ()
                .entrySet ()
                .stream ()
                .map ( currenciesManager::buildResponseMessage )
                .collect( Collectors.toList());
    }
}
