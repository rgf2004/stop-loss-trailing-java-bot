package com.github.stoploss.core.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private static final Logger LOGGER = LoggerFactory.getLogger (TelegramBot.class);

    @Value ( "${application.telegram.channelid}" )
    long chatId;

    @Value ( "${application.telegram.token}" )
    String telegramBotTokcen;

    @Value ( "${application.telegram.user}" )
    String telegramBotUserName;

    @Override
    public void onUpdateReceived(Update update) {

    }

    @Override
    public String getBotUsername() {
        return telegramBotUserName;
    }

    @Override
    public String getBotToken() {
        return telegramBotTokcen;
    }

    public void sendCustomTextMessage(String msg)
    {
        LOGGER.info ( "Sending Message [{}]", msg );
        SendMessage message = new SendMessage (  );
        message.setChatId ( chatId );
        message.setText ( msg );
        try {
            execute ( message );
        } catch (TelegramApiException e) {
            LOGGER.error ( "Error Occurred", e );
        }

    }
}
