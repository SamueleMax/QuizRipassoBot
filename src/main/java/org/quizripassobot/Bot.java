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
        RECEIVING_NEXT_ANSWER
    }

    long userId;
    Status status = Status.NORMAL;
    int quizLength = -1;
    int currentQuizPosition = -1;
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
                    endQuiz();
                    break;
                case "/newquiz":
                    sendText(id, "Questo comando non è ancora stato implementato :(");
                    break;
                case "/loadquiz":
                    sendText(id, "Carica un file di quiz compatibile");
                    status = Status.RECEIVING_QUIZ;
                    break;
                default:
                    sendText(id, "Comando sconosciuto");
            }
        } else {
            switch (status) {
                case RECEIVING_QUIZ:
                    QuizQuestion[] quizQuestions = QuizQuestion.parseQuiz(msgText);
                    if (quizQuestions == null) {
                        sendText(id, "Il file che hai caricato non è nel formato corretto");
                    } else {
                        userId = id;
                        startQuiz(quizQuestions);
                    }
                    status = Status.NORMAL;
                    break;
                case RECEIVING_NEXT_ANSWER:
                    answerReceived(msgText);
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

    void startQuiz(QuizQuestion[] quizQuestions) {
        quizLength = quizQuestions.length;
        currentQuizPosition = 0;
        currentQuizQuestions = quizQuestions;
        sendText(userId, "Il quiz è cominciato! Ci sono " + quizLength + " domande. Buona fortuna!");
        nextQuestion();
    }

    void endQuiz() {
        sendText(userId, "Il quiz è terminato!");
        quizLength = -1;
        currentQuizPosition = -1;
        currentQuizQuestions = null;
    }

    void nextQuestion() {
        if (currentQuizPosition >= quizLength) {
            endQuiz();
        } else {
            sendText(userId, currentQuizQuestions[currentQuizPosition].question);
            status = Status.RECEIVING_NEXT_ANSWER;
        }
    }

    void answerReceived(String answer) {
        QuizQuestion currentQuizQuestion = currentQuizQuestions[currentQuizPosition];
        if (currentQuizQuestion.checkAnswer(answer)) {
            sendText(userId, "Risposta corretta!");
        } else {
            sendText(userId, "Risposta sbagliata. La risposta corretta era: " + currentQuizQuestion.answer);
        }
        status = Status.NORMAL;
        currentQuizPosition++;
        nextQuestion();
    }

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        Bot bot = new Bot();
        botsApi.registerBot(bot);
    }
}
