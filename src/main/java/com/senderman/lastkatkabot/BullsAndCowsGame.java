package com.senderman.lastkatkabot;

import java.util.concurrent.ThreadLocalRandom;

class BullsAndCowsGame {
    private final int MAX_COUNT = 4;
    private int attempts;
    private int answer;
    private int[] answerArray;
    private long chatId;
    private LastkatkaBotHandler handler;


    BullsAndCowsGame(LastkatkaBotHandler handler, long chatId) {
        this.handler = handler;
        this.chatId = chatId;
        attempts = 10;
        handler.sendMessage(chatId, "Генерируем число...");
        answer = generateRandom();
        answerArray = split(answer);
        handler.sendMessage(chatId, "Число загадано!\n" +
                "Отправляйте в чат ваши варианты, они должны состоять только из 4 неповторяющихся чисел!");
    }

    void check(int number) {

        if (attempts == 1) {
            handler.sendMessage(chatId, "Вы проиграли! Ответ: " + answer);
            handler.bullsAndCowsGames.remove(chatId);
            return;
        }
        if (matchFound(split(number))) {
            handler.sendMessage(chatId, "Загаданное число не может содержать повторяющиеся числа!");
            return;
        }
        int[] results = calculate(split(number));
        if (results[0] == 4) {
            handler.sendMessage(chatId, "Вы выиграли! " + number + " - правильный ответ!");
            handler.bullsAndCowsGames.remove(chatId);

        } else {
            attempts--;
            handler.sendMessage(chatId, String.format(" %4$d: Быков: %1$d, коров: %2$d, попыток: %3$d\n",
                    results[0], results[1], attempts, number));
        }
    }

    private int generateRandom() {
        int random;
        do {
            random = ThreadLocalRandom.current().nextInt(1000, 10000);
        } while (matchFound(split(random)));
        return random;
    }

    // int to int[] :)
    private int[] split(int num) {
        int[] result = new int[MAX_COUNT];
        for (int i = 0; i < MAX_COUNT; i++) {
            int t = num % 10;
            result[MAX_COUNT - 1 - i] = t;
            num /= 10;
        }
        return result;
    }

    // check that array contains only unique numbers
    private boolean matchFound(int[] array) {
        for (int i = 0; i < MAX_COUNT; i++) {
            for (int j = i + 1; j < MAX_COUNT; j++) {
                if (array[i] == array[j]) {
                    return true;
                }
            }
        }
        return false;
    }

    //calculate bulls and cows
    private int[] calculate(int[] player) {
        int bulls = 0, cows = 0;
        for (int i = 0; i < MAX_COUNT; i++) {
            if (answerArray[i] == player[i]) {
                bulls++;
            } else {
                for (int j = 0; j < MAX_COUNT; j++) {
                    if (player[i] == answerArray[j] && player[j] != answerArray[j]) {
                        cows++;
                    }
                }
            }
        }
        return new int[]{bulls, cows};
    }
}
