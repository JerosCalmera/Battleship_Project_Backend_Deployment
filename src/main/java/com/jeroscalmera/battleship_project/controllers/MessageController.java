package com.jeroscalmera.battleship_project.controllers;

import com.jeroscalmera.battleship_project.gameLogic.ChatToken;
import com.jeroscalmera.battleship_project.gameLogic.Placing;
import com.jeroscalmera.battleship_project.gameLogic.PlayerAndRoom;
import com.jeroscalmera.battleship_project.gameLogic.Shooting;
import com.jeroscalmera.battleship_project.models.BugReport;
import com.jeroscalmera.battleship_project.models.Player;
import com.jeroscalmera.battleship_project.websocket.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class MessageController {


    private final SimpMessagingTemplate messagingTemplate;
    @Autowired
    public MessageController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Autowired
    private PlayerAndRoom playerAndRoom;
    @Autowired
    private Placing placing;
    @Autowired
    private Shooting shooting;
    @MessageMapping("/hello")
    @SendTo("/topic/connect")
    public Greeting greeting(Connection message, String name) throws Exception {
        System.out.println("Client connected: " + message);
        return new Greeting("Game server ready....");
    }

    @MessageMapping("/chat")
    @SendTo("/topic/chat")
    public Chat chat(Chat message) throws Exception {
        String chat = message.getContent();
        return new Chat(ChatToken.generateChatToken() + chat);
    }

    @MessageMapping("/globalChat")
    @SendTo("/topic/globalChat")
    public Chat globalChat(Chat message) throws Exception {
        String chat = message.getContent();
        return new Chat(ChatToken.generateChatToken() + chat);
    }
    @MessageMapping("/gameUpdate")
    public void gameUpdate(Player name) throws InterruptedException {
    }

    @MessageMapping("/nameValidated")
    public void nameValidated() throws InterruptedException {
    }
    @MessageMapping("/startup")
    public void startup(Player name) throws InterruptedException {
        playerAndRoom.submitStartStats(name);
    }
    @MessageMapping("/gameData")
    public void handleGameData(GameData gameData) throws InterruptedException {
        String target = gameData.getContent();
        shooting.shootAtShip(target);
    }

    @MessageMapping("/randomPlacement")
    public void handleRandomPlacement(String name) throws InterruptedException {
        placing.computerPlaceShips(name.substring(1, name.length() -1));
    }
    @MessageMapping("/restart")
    public void handleRestart(String playerName) {playerAndRoom.resetPlayer(playerName);
    }
    @MessageMapping("/room")
    public void handlePassword(String newRoom) throws InterruptedException {
        playerAndRoom.handlePassword(newRoom);
    }
    @MessageMapping("/name")
    @SendTo("/topic/name")
    public void handleName (Player player) throws InterruptedException {
        playerAndRoom.handleNewPlayer(player);
    }
    @MessageMapping("/placement")
    public void shipPlacement(String string) throws InterruptedException {
        placing.placeShip(string);
    }
    @MessageMapping("/placement2")
    public void secondaryPlacement(String string) throws InterruptedException {
    }
    @MessageMapping("/enemyDamage")
    public void enemyDamage(String string) {
    }
    @MessageMapping("/hidden")
    public Hidden hidden(String string) {
        return null;
    }

    @MessageMapping("/computer")
    public void computer(String startComputerGame) throws InterruptedException {
        playerAndRoom.computerMatchStart(startComputerGame);
    }

    @MessageMapping("/miss")
    public void miss(String string) {
    }
    @MessageMapping("/gameInfo")
    public void gameInfo(String string) {
    }

    @MessageMapping("/leaderBoard")
    public void leaderboard(String trigger) throws InterruptedException {
        playerAndRoom.leaderBoard(trigger);
    }

    @MessageMapping("/turn")
    public void turn(String playerName) throws InterruptedException {
        playerAndRoom.coinFlip(playerName);
    }

    @MessageMapping("/turnCheck")
    public void turnCheck(String playerName) throws InterruptedException {
    }

    @MessageMapping("/matchStart")
    public void matchStart(String playerName) throws InterruptedException {
        playerAndRoom.matchStart(playerName);
    }
    @MessageMapping("/bugReport")
    public void bugReport(BugReport bugReport) throws InterruptedException {
        playerAndRoom.bugReport(bugReport);
    }

}
