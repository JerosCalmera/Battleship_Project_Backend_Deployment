package com.jeroscalmera.battleship_project.gameLogic;

import com.jeroscalmera.battleship_project.models.BugReport;
import com.jeroscalmera.battleship_project.models.Lobby;
import com.jeroscalmera.battleship_project.models.Player;
import com.jeroscalmera.battleship_project.models.Room;
import com.jeroscalmera.battleship_project.repositories.*;
import com.jeroscalmera.battleship_project.websocket.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PlayerAndRoom {
    private PlayerRepository playerRepository;
    private RoomRepository roomRepository;
    private ShipRepository shipRepository;
    private BugreportRepository bugreportRepository;

    private LobbyRepository lobbyRepository;
    private WebSocketMessageSender webSocketMessageSender;
    private static final List<Player> playersNotInRoom = new ArrayList<>();
    private Placing placing;
    private Shooting shooting;

    public PlayerAndRoom(LobbyRepository lobbyRepository, PlayerRepository playerRepository, RoomRepository roomRepository, ShipRepository shipRepository, BugreportRepository bugreportRepository, WebSocketMessageSender webSocketMessageSender, Placing placing, Shooting shooting) {
        this.playerRepository = playerRepository;
        this.roomRepository = roomRepository;
        this.shipRepository = shipRepository;
        this.bugreportRepository = bugreportRepository;
        this.lobbyRepository = lobbyRepository;
        this.webSocketMessageSender = webSocketMessageSender;
        this.placing = placing;
        this.shooting = shooting;
    }

    // Restart a players room and ships, deletes the room the player is the last player removed from the room
    public void resetPlayer(String playerName) {
        System.out.println("Player name to delete from room: " + playerName.substring(1, 6));
        Player player = playerRepository.findByNameContaining(playerName.substring(1, 6));
        shipRepository.deleteAllCoOrdsByPlayerId(player.getId());
        Room room = roomRepository.findRoomByPlayersName(player.getName());
        boolean playerPresent = roomRepository.existsByPlayersName(player.getName());
        if (playerPresent)
            {room.removePlayerFromRoom(player);
            System.out.println("deleting player: " + player.getName());
            roomRepository.save(room);}
        if (room.getPlayers() == null || room.getPlayers().isEmpty() || room.getPlayers().size() == 1)
            {roomRepository.delete(room);
                System.out.println("Deleting room: " + room.getRoomNumber());}
        else {
            System.out.println("Players remaining in room: " + room.getPlayers().size());}
        player.setUnReady();
        player.setShips(null);
        player.setRoom(null);
        playerRepository.save(player);
        roomRepository.save(room);
        System.out.println("Player reset done");
    }

    // Saves a bugreport to the database
    public void bugReport(BugReport bugReport) {
        bugreportRepository.save(bugReport);
    }

    // Submits a players starting ships to the frontend
    public void submitStartStats(Player name) {
        Player player = playerRepository.findByNameContaining(name.getName());
        List<String> allCoOrds = playerRepository.findAllCoOrdsByPlayerName(name.getName());
        String converted = String.join("", allCoOrds);
        webSocketMessageSender.sendMessage("/topic/gameData", new GameData(player.getRoom().getRoomNumber() + name.getName() + converted));
    }

    // Sends the top ten players to the leaderboard display on the frontend
    public void leaderBoard(String trigger) throws InterruptedException {
        List<Player> leaderboard = playerRepository.findAll();
        Collections.sort(leaderboard, Comparator.comparingInt(Player::getLevel).reversed());
        int total = 0;
        for (Player playerLeader : leaderboard) {
            if (total < 10) {
                if (!playerLeader.isComputer()){
                webSocketMessageSender.sendMessage("/topic/leaderBoard", new Chat("Level (" + playerLeader.getLevel() + ") " + playerLeader.getName()));
                total++;}
            } else {
                break;
            }
        }
    }

    // Logic to ensure that both players are ready to start the game
    public void matchStart(String playerName) throws InterruptedException {
        if (!playerName.contains("Computer")) {
            playerName = playerName.substring(1, playerName.length() -1);
        }
        Player activePlayer = playerRepository.findByNameContaining(playerName);
        activePlayer.setReady();
        playerRepository.save(activePlayer);
        Room activeRoom = roomRepository.findRoomByPlayersName(playerName);
        if (activeRoom.getPlayersReady() == 0) {
            activeRoom.setPlayersReady(1);
            roomRepository.save(activeRoom);
        }
        else if (activeRoom.getPlayersReady() == 1) {
            activeRoom.setPlayersReady(2);
            roomRepository.save(activeRoom);
            Lobby lobbyRoomToDelete = lobbyRepository.findLobbySingleRoom(activeRoom.getRoomNumber());
            coinFlip(playerName);
        }
    }

    // Logic to randomly select which player starts first
    public void coinFlip(String playerName) throws InterruptedException {
        Random random = new Random();
        Room room = roomRepository.findRoomByPlayersName(playerName);
        Lobby lobby = lobbyRepository.findLobbySingleRoom(room.getRoomNumber());
        List<Player> playerList = room.getPlayers();
        Player playerToNotSelect = new Player(playerName);
        Player playerToSelect = new Player(playerName);
        Player player = playerRepository.findByNameContaining(playerName);
        int coin = random.nextInt(2) + 1;
        if (coin == 1) {
            webSocketMessageSender.sendMessage("/topic/turn", new Chat(room.getRoomNumber() + player.getName()));
            if (player.isComputer()) {
                shooting.computerShoot(player.getName());
            }
            webSocketMessageSender.sendMessage("/topic/chat", new Chat( ChatToken.generateChatToken() + room.getRoomNumber() + player.getName() + " has won the coin flip and goes first!"));
            lobbyRepository.delete(lobby);
        }
        else {
            if (Objects.equals(playerList.get(0).getName(), player.getName())) {
                playerToNotSelect = playerList.get(0);
                playerToSelect = playerList.get(1);
        } else {
            playerToNotSelect = playerList.get(1);
            playerToSelect = playerList.get(0);}
        {
        if (playerToSelect.isComputer()) {
            webSocketMessageSender.sendMessage("/topic/chat", new Chat( ChatToken.generateChatToken() + room.getRoomNumber() + "The computer has has won the coin flip and goes first!"));
            webSocketMessageSender.sendMessage("/topic/turn", new Chat(room.getRoomNumber() + playerToSelect.getName()));
            shooting.computerShoot(player.getName());
        }
        else {
            webSocketMessageSender.sendMessage("/topic/chat", new Chat( ChatToken.generateChatToken() + room.getRoomNumber() + playerToSelect.getName() + " has won the coin flip and goes first!"));
            webSocketMessageSender.sendMessage("/topic/turn", new Chat(room.getRoomNumber() + playerToSelect.getName()));
        }}}
        webSocketMessageSender.sendMessage("/topic/chat", new Chat(ChatToken.generateChatToken()+ room.getRoomNumber() + "All ships placed! Match Start!"));
        lobbyRepository.delete(lobby);
    }

    // Handles the room number submission from the frontend and decides if it is an existing room or a new one
    public void handlePassword(String roomNumber) throws InterruptedException {
        roomNumber = roomNumber.substring(1, roomNumber.length() - 1);
            if
            (!lobbyRepository.findLobbyRoomExists(roomNumber)) {
                Lobby roomToSave = new Lobby(roomNumber);
                roomToSave.setSaved(true);
                lobbyRepository.save(roomToSave);
                webSocketMessageSender.sendMessage("/topic/connect", new Greeting("Server: Room saved!"));
                webSocketMessageSender.sendMessage("/topic/hidden", new Hidden(roomNumber + "Server: Room saved!"));
            } else {
                Lobby roomToValidate = lobbyRepository.findLobbySingleRoom(roomNumber);
                roomToValidate.setValidated(true);
                lobbyRepository.save(roomToValidate);
            }
            Lobby roomToCheck = lobbyRepository.findLobbySingleRoom(roomNumber);
            if (roomToCheck.isSaved() && roomToCheck.isValidated()) {
                webSocketMessageSender.sendMessage("/topic/connect", new Greeting("Server: Rooms synced"));
                webSocketMessageSender.sendMessage("/topic/hidden", new Hidden(roomNumber + "Server: Room synced!"));
                Room addRoom = new Room(roomNumber);
                addRoom.setRoomNumber(roomNumber.substring(0, 4));
                roomRepository.save(addRoom);
                Thread.sleep(50);
                for (Player newPlayer : playersNotInRoom) {
                    Player playerToFind = playerRepository.findByName(newPlayer.getName());
                    if (playerToFind != null) {
                        playerToFind.setRoom(addRoom);
                        shipRepository.deleteAllCoOrdsByPlayerId((playerToFind.getId()));
                        playerRepository.save(playerToFind);

                    } else {
                        newPlayer.setRoom(addRoom);
                        shipRepository.deleteAllCoOrdsByPlayerId((newPlayer.getId()));
                        playerRepository.save(newPlayer);
                    }
                    addRoom.addPlayerToRoom(newPlayer);
                }
                Player playerDetails1 = playerRepository.findByNameContaining(playersNotInRoom.get(1).getName());
                Player playerDetails2 = playerRepository.findByNameContaining(playersNotInRoom.get(0).getName());
                webSocketMessageSender.sendMessage("/topic/playerData1", new Hidden(addRoom.getRoomNumber() + playerDetails1.getDetails()));
                webSocketMessageSender.sendMessage("/topic/playerData2", new Hidden(addRoom.getRoomNumber() + playerDetails2.getDetails()));
                roomRepository.save(addRoom);
                playerRepository.save(playerDetails1);
                playerRepository.save(playerDetails2);
                playersNotInRoom.clear();
            }
        }

        // Handles a players name entry from the frontend
    public void handleNewPlayer(Player playerName) throws InterruptedException {
        Thread.sleep(100);
        List<String> players = playerRepository.findName();
        if (!players.contains(playerName.getName())) {
            if (players.stream().anyMatch(name -> name.startsWith(playerName.getName().substring(0, 4)))) {
                webSocketMessageSender.sendMessage("/topic/globalChat", new Chat(ChatToken.generateChatToken() + "Admin: Sorry, " + playerName.getName() + " is too similar to an existing username!"));
            } else {
                String name = playerName.getName();
                Player player = new Player(name);
                playersNotInRoom.add(player);
                webSocketMessageSender.sendMessage("/topic/globalChat", new Chat(ChatToken.generateChatToken()  + "Admin: Hello to our new player " + playerName.getName() + " your profile has been saved!"));
                webSocketMessageSender.sendMessage("/topic/nameValidated", new Chat(playerName.getName()));
            }
        } else {
            if (!playerName.getName().contains("Computer")) {
                webSocketMessageSender.sendMessage("/topic/globalChat", new Chat(ChatToken.generateChatToken() + "Admin: Welcome back " + playerName.getName() + "!"));
                webSocketMessageSender.sendMessage("/topic/nameValidated", new Chat(playerName.getName()));
            }else
            {webSocketMessageSender.sendMessage("/topic/globalChat", new Chat(ChatToken.generateChatToken() + "Admin: A Game against the Computer has been selected"));}
            String name = playerName.getName();
            Player player = new Player(name);
            playersNotInRoom.add(player);
        }
    }

    // Logic for a computer player to prepare itself for the game
    public void computerMatchStart(String roomNumber) throws InterruptedException {
        Random random = new Random();
        int rando = random.nextInt(10000);
        String randomNumber = String.format("%05d", rando);
        String ident = randomNumber;
        Player computerPlayerCreated = new Player();
        computerPlayerCreated.setName(ident + "Computer");
        computerPlayerCreated.setComputer(true);
        playerRepository.save(computerPlayerCreated);
        handleNewPlayer(computerPlayerCreated);
        Thread.sleep(50);
        handlePassword(roomNumber);
        Thread.sleep(50);
        handlePassword(roomNumber);
        Thread.sleep(50);;
        Thread placeShipsThread = new Thread(() -> {
            try {
                placing.computerPlaceShips(computerPlayerCreated);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        placeShipsThread.start();
        placeShipsThread.join();
        matchStart(computerPlayerCreated.getName());
        webSocketMessageSender.sendMessage("/topic/chat", new Chat(ChatToken.generateChatToken() + roomNumber.substring(1, roomNumber.length() - 1) + "Admin: Computer player ready"));
    }
}

