package com.senderman.lastkatkabot.TempObjects;

import com.annimon.tgbotsmodule.api.methods.Methods;
import com.senderman.lastkatkabot.LastkatkaBotHandler;
import com.senderman.lastkatkabot.Services;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class BullsAndCowsGame {
    private final int LENGTH = 4;
    private final Set<Integer> messagesToDelete;
    private final Set<Integer> checkedNumbers;
    private int attempts;
    private int answer;
    private int[] answerArray;
    private long chatId;
    private LastkatkaBotHandler handler;

    public BullsAndCowsGame(LastkatkaBotHandler handler, long chatId) {
        this.handler = handler;
        this.chatId = chatId;
        attempts = 10;
        messagesToDelete = new HashSet<>();
        checkedNumbers = new HashSet<>();
        handler.sendMessage(chatId, "Генерируем число...");
        answer = generateRandom();
        answerArray = split(answer);
        handler.sendMessage(chatId, "Число загадано!\n" +
                "Отправляйте в чат ваши варианты, они должны состоять только из 4 неповторяющихся чисел!\n" +
                "Правила игры - /bnchelp");
    }

    public void check(Message message) {

        messagesToDelete.add(message.getMessageId());

        var number = Integer.parseInt(message.getText());

        if (hasRepeatingDigits(split(number))) {
            messagesToDelete.add(
                    handler.sendMessage(chatId, "Загаданное число не может содержать повторяющиеся числа!")
                            .getMessageId());
            return;
        }

        var results = calculate(split(number));

        if (checkedNumbers.contains(number)) {
            messagesToDelete.add(
                    handler.sendMessage(chatId, String.format("%1$04d - уже проверяли! %2$dБ %3$dК",
                            number, results[0], results[1])).getMessageId());
            return;
        }

        attempts--;
        if (results[0] == 4) { // win
            handler.sendMessage(chatId, String.format("%1$s выиграл за %2$d попыток! %3$04d - правильный ответ!",
                    message.getFrom().getFirstName(), 10 - attempts, answer));
            Services.db().incBNCWin(message.getFrom().getId());
            for (int messageId : messagesToDelete) {
                Methods.deleteMessage(chatId, messageId).call(handler);
            }
            handler.bullsAndCowsGames.remove(chatId);
            return;

        }

        if (attempts != 0) {
            messagesToDelete.add(handler.sendMessage(chatId, String.format("%4$04d: %1$dБ %2$dК, попыток: %3$d\n",
                    results[0], results[1], attempts, number))
                    .getMessageId());
            checkedNumbers.add(number);
        } else { // lose
            handler.sendMessage(chatId, String.format("Вы проиграли! Ответ: %1$04d", answer));
            for (int messageId : messagesToDelete) {
                Methods.deleteMessage(chatId, messageId).call(handler);
            }
            handler.bullsAndCowsGames.remove(chatId);
        }
    }

    private int generateRandom() {
        int random;
        do {
            random = ThreadLocalRandom.current().nextInt(123, 10000);
        } while (hasRepeatingDigits(split(random)));
        return random;
    }

    // int to int[] :)
    private int[] split(int num) {
        int[] result = new int[LENGTH];
        for (int i = 0; i < LENGTH; i++) {
            var t = num % 10;
            result[LENGTH - 1 - i] = t;
            num /= 10;
        }
        return result;
    }

    // check that array contains only unique numbers
    private boolean hasRepeatingDigits(int[] array) {
        for (int i = 0; i < array.length; i++) {
            for (int j = i + 1; j < array.length; j++) {
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
        for (int i = 0; i < LENGTH; i++) {
            if (answerArray[i] == player[i]) {
                bulls++;
            } else {
                for (int j = 0; j < LENGTH; j++) {
                    if (player[i] == answerArray[j] && player[j] != answerArray[j]) {
                        cows++;
                    }
                }
            }
        }
        return new int[]{bulls, cows};
    }
}