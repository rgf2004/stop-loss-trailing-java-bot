package com.github.stoploss.core.telegram;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
class TelegramCommand {

    private Command command;
    private List<String> arguments = new ArrayList<> (  );

    TelegramCommand(Command command){
        this.command = command;
    }
}
