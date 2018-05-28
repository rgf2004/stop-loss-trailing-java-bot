package com.github.stoploss.core.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private static final Logger LOGGER = LoggerFactory.getLogger ( TelegramBot.class );

    @Value("${application.telegram.channelid}")
    private long chatId;

    @Value("${application.telegram.token}")
    private String telegramBotTokcen;

    @Value("${application.telegram.user}")
    private String telegramBotUserName;

    @Autowired
    private TelegramRequestHandler telegramRequestHandler;

    @Override
    public void onUpdateReceived(Update update) {

        LOGGER.info ( "Telegram Update Received : {} ", update );
        telegramRequestHandler
                .handleRequest ( update.getMessage ().getText () )
                .forEach ( this::sendCustomTextMessage );
            ;
    }

    @Override
    public String getBotUsername() {
        return telegramBotUserName;
    }

    @Override
    public String getBotToken() {
        return telegramBotTokcen;
    }

    public void sendCustomTextMessage(String msg) {
        LOGGER.info ( "Sending Message [{}]", msg );
        SendMessage message = new SendMessage ();
        message.setChatId ( chatId );
        message.setText ( msg );
        try {
            execute ( message );
        } catch (TelegramApiException e) {
            LOGGER.error ( "Error Occurred", e );
        }

    }
}
