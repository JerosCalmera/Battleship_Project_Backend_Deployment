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

    boolean resetting = false;

    List<String> storedPlayers = new ArrayList<>();

    // Restart a players room and ships, deletes any leftover lobby, deletes the room the player is the last player removed from the room, it will store players for reset if the function is already running, deletes computer players when no longer needed
    public void resetPlayer(String playerName) {
        Player player = playerRepository.findByNameContaining(playerName.substring(1, 6));
        Lobby lobby = new Lobby();
        lobby = lobbyRepository.findLobbySingleRoom(player.getRoomNumber());
        if (lobby != null) {
            lobbyRepository.delete(lobby);
        }
        if (resetting) {
            storedPlayers.add(playerName);
            return;
        }
        resetting = true;
        if (storedPlayers.contains(playerName)) {
            storedPlayers.remove(playerName);
        }
        shipRepository.deleteAllCoOrdsByPlayerId(player.getId());
        Room room = roomRepository.findRoomByPlayersName(player.getName());
        if (room == null) {
            resetting = false;
            return;
        }
        webSocketMessageSender.sendMessage("/topic/hidden", new Chat(room.getRoomNumber() + player.getName() + "Player left"));
        boolean playerPresent = roomRepository.existsByPlayersName(player.getName());
        if (playerPresent) {
            player.setRoom(null);
            room.removePlayerFromRoom(player);
        }
        player.setUnReady();
        player.setShips(null);
        player.setRoomNumber(null);
        playerRepository.save(player);
        roomRepository.save(room);
        if (room.getPlayers() == null || room.getPlayers().isEmpty() || room.getPlayers().size() == 1) {
            roomRepository.delete(room);
        }
        if (player.isComputer()) {
            playerRepository.delete(player);
        }
        resetting = false;
        if (!storedPlayers.isEmpty()) {
            resetPlayer(storedPlayers.get(0));
        }
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
                if (!playerLeader.isComputer()) {
                    webSocketMessageSender.sendMessage("/topic/leaderBoard", new Chat("Level (" + playerLeader.getLevel() + "): " + playerLeader.getName()));
                    total++;
                }
            } else {
                break;
            }
        }
    }

    // Logic to ensure that both players are ready to start the game
    public void matchStart(String playerName) throws InterruptedException {
        if (!playerName.contains("Computer")) {
            playerName = playerName.substring(1, playerName.length() - 1);
        }
        Player activePlayer = playerRepository.findByNameContaining(playerName);
        activePlayer.setReady();
        playerRepository.save(activePlayer);
        Room activeRoom = roomRepository.findRoomByPlayersName(playerName);
        if (activeRoom.getPlayersReady() == 0) {
            activeRoom.setPlayersReady(1);
            roomRepository.save(activeRoom);
        } else if (activeRoom.getPlayersReady() == 1) {
            activeRoom.setPlayersReady(2);
            roomRepository.save(activeRoom);
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
            webSocketMessageSender.sendMessage("/topic/chat", new Chat(ChatToken.generateChatToken() + room.getRoomNumber() + player.getName() + " has won the coin flip and goes first!"));
            ;
        } else {
            if (Objects.equals(playerList.get(0).getName(), player.getName())) {
                playerToNotSelect = playerList.get(0);
                playerToSelect = playerList.get(1);
            } else {
                playerToNotSelect = playerList.get(1);
                playerToSelect = playerList.get(0);
            }
            {
                if (playerToSelect.isComputer()) {
                    webSocketMessageSender.sendMessage("/topic/chat", new Chat(ChatToken.generateChatToken() + room.getRoomNumber() + "The computer has has won the coin flip and goes first!"));
                    webSocketMessageSender.sendMessage("/topic/turn", new Chat(room.getRoomNumber() + playerToSelect.getName()));
                    shooting.computerShoot(player.getName());
                } else {
                    webSocketMessageSender.sendMessage("/topic/chat", new Chat(ChatToken.generateChatToken() + room.getRoomNumber() + playerToSelect.getName() + " has won the coin flip and goes first!"));
                    webSocketMessageSender.sendMessage("/topic/turn", new Chat(room.getRoomNumber() + playerToSelect.getName()));
                }
            }
        }
        webSocketMessageSender.sendMessage("/topic/chat", new Chat(ChatToken.generateChatToken() + room.getRoomNumber() + "All ships placed! Match Start!"));
        ;
    }

    // Handles the room number submission from the frontend and decides if it is an existing room or a new one, creates a lobby for validating room information before its creation
    public void handlePassword(String roomNumber) throws InterruptedException {
        String roomNumberFound = roomNumber.substring(1, 5);
        String playerName = roomNumber.substring(5, 10);
        Player player = playerRepository.findByNameContaining(playerName);
        if (roomRepository.findByRoomNumber(roomNumberFound) != null) {
            webSocketMessageSender.sendMessage("/topic/globalChat", new Chat(ChatToken.generateChatToken() + "Admin: That room already exists, please choose another room number"));
            return;
        }
        if (!lobbyRepository.findLobbyRoomExists(roomNumberFound)) {
            Lobby roomToSave = new Lobby(roomNumberFound);
            roomToSave.setSaved(true);
            lobbyRepository.save(roomToSave);
            Thread.sleep(1000);
            player.setRoomNumber(roomNumberFound);
            playerRepository.save(player);
            webSocketMessageSender.sendMessage("/topic/connect", new Greeting("Server: Room saved!"));
            webSocketMessageSender.sendMessage("/topic/hidden", new Hidden(roomNumberFound + "Server: Room saved!"));
        } else {
            if (Objects.equals(player.getRoomNumber(), roomNumberFound)) {
                webSocketMessageSender.sendMessage("/topic/globalChat", new Chat(ChatToken.generateChatToken() + "Admin: " + player.getName() + " You have rejoined the lobby for your room"));
                Thread.sleep(1000);
                webSocketMessageSender.sendMessage("/topic/hidden", new Hidden(roomNumberFound + "Server: Room saved!"));
                return;
            }
            Lobby roomToValidate = lobbyRepository.findLobbySingleRoom(roomNumberFound);
            roomToValidate.setValidated(true);
            lobbyRepository.save(roomToValidate);
            player.setRoomNumber(roomNumberFound);
            playerRepository.save(player);
        }
        Lobby roomToCheck = lobbyRepository.findLobbySingleRoom(roomNumberFound);
        if (roomToCheck.isSaved() && roomToCheck.isValidated()) {
            Thread.sleep(1000);
            Lobby lobbyRoomToDelete = lobbyRepository.findLobbySingleRoom(player.getRoomNumber());
            lobbyRepository.delete(lobbyRoomToDelete);
            Room addRoom = new Room(roomNumberFound);
            addRoom.setRoomNumber(roomNumberFound);
            roomRepository.save(addRoom);
            Thread.sleep(50);
            List<Player> playersNotInRoom = new ArrayList<>();
            playersNotInRoom = playerRepository.findByStoredRoomNumber(roomNumberFound);
            for (Player newPlayer : playersNotInRoom) {
                newPlayer.setRoom(addRoom);
                shipRepository.deleteAllCoOrdsByPlayerId((newPlayer.getId()));
                playerRepository.save(newPlayer);
                addRoom.addPlayerToRoom(newPlayer);
                roomRepository.save(addRoom);
            }
            Player playerDetails1 = playerRepository.findByNameContaining(playersNotInRoom.get(1).getName());
            Player playerDetails2 = playerRepository.findByNameContaining(playersNotInRoom.get(0).getName());
            webSocketMessageSender.sendMessage("/topic/connect", new Greeting("Server: Rooms synced"));
            webSocketMessageSender.sendMessage("/topic/hidden", new Hidden(roomNumberFound + "Server: Room synced!"));
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
                webSocketMessageSender.sendMessage("/topic/nameValidated", new Chat("Invalid"));
            } else {
                webSocketMessageSender.sendMessage("/topic/globalChat", new Chat(ChatToken.generateChatToken()  + "Admin: Hello to our new player " + playerName.getName() + " your profile has been saved!"));
                webSocketMessageSender.sendMessage("/topic/nameValidated", new Chat(playerName.getName()));
                Player player = new Player(playerName.getName());
                playerRepository.save(player);
            }
        } else {
            Player player = playerRepository.findByNameContaining(playerName.getName());
            if (!playerName.getName().contains("Computer")) {
                    if (player.playerIsBanned()) {
                        webSocketMessageSender.sendMessage("/topic/globalChat", new Chat(ChatToken.generateChatToken() + "Admin: Sorry that player is banned!"));
                        webSocketMessageSender.sendMessage("/topic/nameValidated", new Chat("Banned"));
                        return;
                    }
                webSocketMessageSender.sendMessage("/topic/globalChat", new Chat(ChatToken.generateChatToken() + "Admin: Welcome back " + playerName.getName() + "!"));
                webSocketMessageSender.sendMessage("/topic/nameValidated", new Chat(playerName.getName()));
            }else
            {webSocketMessageSender.sendMessage("/topic/globalChat", new Chat(ChatToken.generateChatToken() + "Admin: A Game against the Computer has been selected"));}
        }
    }

    // Logic for a computer player to prepare itself for the game
    public void computerMatchStart(String startComputerGame) throws InterruptedException {
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
        handlePassword( "C" + startComputerGame.substring(1, 5) + computerPlayerCreated.getName());
        Thread.sleep(50);
        handlePassword ("C" + startComputerGame.substring(1, 5) + startComputerGame.substring(5, 10));
        Thread.sleep(50);
        Thread placeShipsThread = new Thread(() -> {
            try {
                placing.computerPlaceShips(computerPlayerCreated.getName());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        placeShipsThread.start();
        placeShipsThread.join();
        matchStart(computerPlayerCreated.getName());
        webSocketMessageSender.sendMessage("/topic/chat", new Chat(ChatToken.generateChatToken() + startComputerGame.substring(1, 5) + "Admin: Computer player ready"));
    }
}

