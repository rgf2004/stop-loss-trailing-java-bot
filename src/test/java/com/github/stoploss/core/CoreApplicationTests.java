package com.github.stoploss.core;

import com.github.xabgesagtx.bots.TelegramBotAutoConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.generics.LongPollingBot;
import org.telegram.telegrambots.generics.WebhookBot;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CoreApplicationTests {

    @MockBean
    TelegramBotAutoConfiguration telegramBotAutoConfiguration;

    @MockBean
    ApplicationRunnerBean applicationRunnerBean;

    @Before
    public void setup() {
        try {
            doNothing ().when ( telegramBotAutoConfiguration ).start ();

            doNothing ().when ( applicationRunnerBean ).run ( isA ( ApplicationArguments.class ) );

        } catch (Exception e) {
            e.printStackTrace ();
        }


    }

    @Test
    public void contextLoads() {
    }

}
