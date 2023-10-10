package org.quizripassobot;

import java.util.ArrayList;

public class QuizQuestion {
    String question;
    String answer;
    boolean answerCorrect = false;

    QuizQuestion(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }

    boolean checkAnswer(String answer) {
        if (this.answer.equalsIgnoreCase(answer)) {
            answerCorrect = true;
            return true;
        }
        return false;
    }

    static QuizQuestion[] parseQuiz(String quizText) {
        // Parse a quiz file, line by line
        // Quiz file syntax: <Question>: <Answer>
        String[] quizLines = quizText.strip().split("\n");
        ArrayList<QuizQuestion> quizQuestions = new ArrayList<>();
        for (String line : quizLines) {
            line = line.strip();
            String[] splitLine = line.split(": ");
            if (splitLine.length != 2) {
                // Error in quiz syntax
                return null;
            }
            String question = splitLine[0];
            String answer = splitLine[1];
            quizQuestions.add(new QuizQuestion(question, answer));
        }
        return quizQuestions.toArray(new QuizQuestion[0]);
    }
}
