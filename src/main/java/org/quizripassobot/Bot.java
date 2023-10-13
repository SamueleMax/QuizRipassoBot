package org.quizripassobot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Bot extends TelegramLongPollingBot {
    enum Status {
        NORMAL,
        RECEIVING_QUIZ,
        RECEIVING_NEXT_ANSWER,
    }

    Status status = Status.NORMAL;
    int quizLength;
    int correctAnswers;
    int currentQuizPosition;
    static QuizQuestion[] currentQuizQuestions;

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
        if (update.hasMessage()) {
            Message msg = update.getMessage();
            User user = msg.getFrom();
            long id = user.getId();
            String msgText = msg.getText();
            if (msg.isCommand()) {
                switch (msgText) {
                    case "/start":
                        sendText(id, "Benvenuto! Cosa vuoi fare?");
                        break;
                    case "/stop":
                        endQuiz(id);
                        break;
                    case "/newquiz":
                        sendText(id, "Questa funzionalità non è ancora stata implementata");
                        break;
                    case "/loadquiz":
                        status = Status.RECEIVING_QUIZ;
                        sendText(id, "Carica un file di quiz compatibile");
                        break;
                    default:
                        sendText(id, "Comando sconosciuto.");
                }
            } else {
                switch (status) {
                    case RECEIVING_QUIZ:
                        status = Status.NORMAL;
                        QuizQuestion[] quizQuestions = QuizQuestion.parseQuiz(msgText);
                        if (quizQuestions == null) {
                            sendText(id, "Il file che hai caricato non è nel formato corretto");
                        } else {
                            startQuiz(id, quizQuestions);
                        }
                        break;
                    case RECEIVING_NEXT_ANSWER:
                        answerReceived(id, msgText);
                        break;
                }
            }
        } else if (update.hasCallbackQuery()) {
            var query = update.getCallbackQuery();
            String data = query.getData();
            long id = query.getFrom().getId();
            switch (data) {
                case "stop":
                    endQuiz(id);
                    break;
                case "loadquiz":
                    status = Status.RECEIVING_QUIZ;
                    sendText(id, "Carica un file di quiz compatibile");
                    break;
            }
        }
    }

    public void sendText(Long who, String what) {
        var keyboard = getKeyboard();
        SendMessage sm = new SendMessage();
        sm.setChatId(who.toString());
        sm.setText(what);
        sm.setReplyMarkup(keyboard);
        try {
            execute(sm);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    void startQuiz(Long userId, QuizQuestion[] quizQuestions) {
        quizLength = quizQuestions.length;
        correctAnswers = 0;
        currentQuizPosition = 0;
        currentQuizQuestions = quizQuestions;
        sendText(userId, String.format("Il quiz è cominciato! Ci sono %s domande. Buona fortuna!", quizLength));
            nextQuestion(userId);
    }

    void nextQuestion(long userId) {
        if (currentQuizPosition >= quizLength) {
            endQuiz(userId);
        } else {
            sendText(userId, currentQuizQuestions[currentQuizPosition].question);
            status = Status.RECEIVING_NEXT_ANSWER;
        }
    }

    void answerReceived(long userId, String answer) {
        QuizQuestion currentQuizQuestion = currentQuizQuestions[currentQuizPosition];
        if (currentQuizQuestion.checkAnswer(answer)) {
            sendText(userId, "Risposta corretta!");
            correctAnswers++;
        } else {
            if (currentQuizQuestion.answers.length > 1) {
                sendText(userId, String.format("Risposta sbagliata. Le risposte corrette erano %s.", currentQuizQuestion.getAnswers()));
            } else {
                sendText(userId, String.format("Risposta sbagliata. La risposta corretta era %s.", currentQuizQuestion.answers[0]));
            }
        }
        currentQuizPosition++;
        nextQuestion(userId);
    }

    void endQuiz(long userId) {
        if (status == Status.NORMAL) {
            sendText(userId, "Non c'è nessun quiz da terminare.");
            return;
        }
        sendText(userId, String.format("Il quiz è terminato. Hai risposto correttamente a %s domande su %s.", correctAnswers, quizLength));
        status = Status.NORMAL;
    }

    InlineKeyboardMarkup getKeyboard() {
        var keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboardRows = new ArrayList<>();
        switch (status) {
            case NORMAL:
                var loadQuizButton = new InlineKeyboardButton();
                loadQuizButton.setText("Load quiz");
                loadQuizButton.setCallbackData("loadquiz");
                keyboardRows.add(new ArrayList<>(List.of(loadQuizButton)));
                break;
            case RECEIVING_QUIZ:
                var stopButton = new InlineKeyboardButton();
                stopButton.setText("Stop");
                stopButton.setCallbackData("stop");
                keyboardRows.add(new ArrayList<>(List.of(stopButton)));
                break;
            case RECEIVING_NEXT_ANSWER:
                stopButton = new InlineKeyboardButton();
                stopButton.setText("Stop");
                stopButton.setCallbackData("stop");
                keyboardRows.add(new ArrayList<>(List.of(stopButton)));
                break;
        }
        keyboard.setKeyboard(keyboardRows);
        return keyboard;
    }

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        Bot bot = new Bot();
        botsApi.registerBot(bot);
    }
}
