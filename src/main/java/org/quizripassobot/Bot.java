package org.quizripassobot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

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
        var msg = update.getMessage();
        var user = msg.getFrom();
        var id = user.getId();
        var msgText = msg.getText();

        if (msg.isCommand()) {
            switch (msgText) {
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

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        Bot bot = new Bot();
        botsApi.registerBot(bot);
    }
}
