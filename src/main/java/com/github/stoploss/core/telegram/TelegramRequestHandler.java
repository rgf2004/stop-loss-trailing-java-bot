package com.github.stoploss.core.telegram;

import com.github.stoploss.core.CurrenciesManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
class TelegramRequestHandler {

    private static final String STATUS = "/status";
    private static final String ADD = "/add";

    @Autowired
    private CurrenciesManager currenciesManager;

    List<String> handleRequest(String message)
    {
        TelegramCommand command = null;
        try {
            command = generateCommand ( message );

            switch (command.getCommand ()) {
                case STATUS:
                    return currenciesManager
                            .getMarketsMonitor ()
                            .entrySet ()
                            .stream ()
                            .map ( currenciesManager::buildResponseMessage )
                            .collect ( Collectors.toList () );
                case ADD_CURRENCY:
                    currenciesManager.addCurrency (
                            command.getArguments ().get ( 0 ),
                            command.getArguments ().get ( 1 ),
                            command.getArguments ().get ( 2 ) );
                    return Collections.singletonList ( "Currency added successfully" );
            }

            return null;
        }
        catch (IllegalArgumentException e) {
            return Collections.singletonList ( e.getMessage () );
        }
    }

    private TelegramCommand generateCommand(String command) throws IllegalArgumentException
    {
        if (StringUtils.isEmpty ( command ))
            throw new IllegalArgumentException ( "Command Can not be empty" );

        if (STATUS.equalsIgnoreCase ( command )) {
            return new TelegramCommand (Command.STATUS);
        }
        else if ( command.startsWith ( ADD )){
            TelegramCommand addCurrencyCommand = new TelegramCommand ( Command.ADD_CURRENCY );
            String[] arguments = command.split ( " " );
            if (arguments.length != 4)
                throw  new IllegalArgumentException ( "Invalid Add command - usage : /add <currency-name> <amount> <percentage>" );

            addCurrencyCommand.getArguments ().add ( arguments[1] );
            addCurrencyCommand.getArguments ().add ( arguments[2] );
            addCurrencyCommand.getArguments ().add ( arguments[3] );

            return addCurrencyCommand;
        }
        else {
            throw new IllegalArgumentException ( "Invalid command - supported commands /status /add" );
        }
    }
}
