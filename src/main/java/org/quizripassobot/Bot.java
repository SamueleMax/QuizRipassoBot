package org.quizripassobot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Bot extends TelegramLongPollingBot {
    @Override
    public String getBotUsername() {
        return Env.botName;
    }

    @Override
    public String getBotToken() {
        return Env.botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        var msg = update.getMessage();
        var user = msg.getFrom();
        var id = user.getId();
        var msgText = msg.getText();

        if (msg.isCommand()) {
            switch (msgText) {
                case "/newquiz":
                    sendText(id, "Questo comando non è ancora stato implementato :(");
                    break;
                case "/loadquiz":
                    sendText(id, "Carica un file di quiz compatibile");
                    i++;
                    sendText(id, String.valueOf(i));
                    break;
                default:
                    sendText(id, "Comando sconosciuto");
            }
        }
    }

    public void sendText(Long who, String what) {
        SendMessage sm = new SendMessage();
        sm.setChatId(who.toString());
        sm.setText(what);
        try {
            execute(sm);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        Bot bot = new Bot();
        botsApi.registerBot(bot);
    }
}
