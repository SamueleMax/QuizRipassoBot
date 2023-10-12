package org.quizripassobot;

import java.util.ArrayList;

public class QuizQuestion {
    String question;
    String[] answers;
    boolean answerCorrect = false;

    QuizQuestion(String question, String[] answers) {
        this.question = question;
        this.answers = answers;
    }

    boolean checkAnswer(String answer) {
        for (String correctAnswer : this.answers) {
            if (correctAnswer.equalsIgnoreCase(answer)) {
                answerCorrect = true;
                return true;
            }
        }
        return false;
    }

    String getAnswers() {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < answers.length; i++) {
            String answer = answers[i];
            str.append("\"");
            str.append(answer);
            str.append("\"");
            if (i < answers.length - 1) {
                // This if is needed, so we avoid adding ", " at the end of the last answer
                str.append(", ");
            }
        }
        return str.toString();
    }

    static QuizQuestion[] parseQuiz(String quizText) {
        // Parse a quiz file, line by line
        // Quiz file syntax:
        // <Question>: <Answer1>
        // <Question>: <Answer1> | <Answer2> (both answers are correct)
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
            String answerString = splitLine[1];
            String[] answers = answerString.split(" \\| ");

            quizQuestions.add(new QuizQuestion(question, answers));
        }
        return quizQuestions.toArray(new QuizQuestion[0]);
    }
}
