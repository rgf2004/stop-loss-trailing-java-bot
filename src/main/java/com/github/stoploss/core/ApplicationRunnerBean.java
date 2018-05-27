package com.github.stoploss.core;

import com.github.stoploss.core.telegram.TelegramBot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ApplicationRunnerBean implements ApplicationRunner {

    @Autowired
    private TelegramBot telegramBot;

    @Autowired
    private StopLossEngine stopLossEngine;

    @Override
    public void run(ApplicationArguments args) throws Exception {

        telegramBot.sendCustomTextMessage ( "Stop-loss application has been started now" );

        stopLossEngine.startEngine ();

    }
}
