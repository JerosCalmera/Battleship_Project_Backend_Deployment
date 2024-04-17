package com.jeroscalmera.battleship_project.gameLogic;

import java.util.Random;

public class ChatToken {
    public ChatToken() {
    }
    // Generates a random chat token to be attached to messages to ensure lagged messages are not repeated
    public static String generateChatToken() {
        Random random = new Random();
        int token = random.nextInt(1000);
        return String.format("%04d", token);
    }
}
