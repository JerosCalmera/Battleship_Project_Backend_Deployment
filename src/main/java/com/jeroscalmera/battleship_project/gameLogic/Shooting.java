package com.jeroscalmera.battleship_project.gameLogic;

import com.jeroscalmera.battleship_project.models.Player;
import com.jeroscalmera.battleship_project.models.Room;
import com.jeroscalmera.battleship_project.models.Ship;
import com.jeroscalmera.battleship_project.repositories.PlayerRepository;
import com.jeroscalmera.battleship_project.repositories.RoomRepository;
import com.jeroscalmera.battleship_project.repositories.ShipRepository;
import com.jeroscalmera.battleship_project.websocket.Chat;
import com.jeroscalmera.battleship_project.websocket.Hidden;
import com.jeroscalmera.battleship_project.websocket.WebSocketMessageSender;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;

@Service
public class Shooting {
    private PlayerRepository playerRepository;
    private ShipRepository shipRepository;
    private WebSocketMessageSender webSocketMessageSender;
    private RoomRepository roomRepository;
    private List<String> coOrdLetters = new ArrayList<>();
    private List<String> coOrdNumbers = new ArrayList<>();
    private List<String> lost = new ArrayList<>();
    private Placing placing;
    public Shooting(PlayerRepository playerRepository, ShipRepository shipRepository, WebSocketMessageSender webSocketMessageSender, RoomRepository roomRepository, Placing placing) {
        this.playerRepository = playerRepository;
        this.shipRepository = shipRepository;
        this.webSocketMessageSender = webSocketMessageSender;
        this.roomRepository = roomRepository;
        this.placing = placing;
    }

    // Checks if a player is in the game lost list and if it is a computer player
    public void computerCheck(String string) throws InterruptedException {
        Player playerToCheck;
        playerToCheck = playerRepository.findByNameContaining(string.substring(1, 5));
        if (lost.contains(playerToCheck.getName())) {
            lost.remove(playerToCheck.getName());
            return;
        }
        if (playerToCheck.isComputer()) {
            computerShoot(playerToCheck.getName());
        }
    }

    // Logic to handle shooting at an enemy ship
    public void shootAtShip(String input) throws InterruptedException {
        String target = input.trim();
        String aimPoint = target.substring(0, 2);
        aimPoint = aimPoint.trim();
        Player selectedPlayer = playerRepository.findByNameContaining(target.substring(2, 6));
        Player selectedPlayer2 = playerRepository.findByNameContaining(target.substring(6, 10));
        List<String> shipList = playerRepository.findAllCoOrdsByPlayerName(selectedPlayer.getName());
        String converted = String.join("", shipList);
        if (converted.contains(aimPoint)) {
            if (!selectedPlayer2.isComputer()) {
                webSocketMessageSender.sendMessage("/topic/gameInfo", new Chat(selectedPlayer.getRoom().getRoomNumber() + selectedPlayer2.getName() + " Hit!"));
            } else {
                webSocketMessageSender.sendMessage("/topic/gameInfo", new Chat(selectedPlayer.getRoom().getRoomNumber() + "The Computer Hit!"));
                if (selectedPlayer2.getAiConfirmedHitInitial() == null) {
                    selectedPlayer2.setAiConfirmedHitInitial(aimPoint);
                }
                selectedPlayer2.setAiConfirmedHit(aimPoint);
                selectedPlayer2.setAiHitCheck(true);
                playerRepository.save(selectedPlayer2);
            }
            webSocketMessageSender.sendMessage("/topic/turn", new Hidden(selectedPlayer.getRoom().getRoomNumber() + selectedPlayer.getName()));
            webSocketMessageSender.sendMessage("/topic/enemyDamage", new Chat(selectedPlayer.getRoom().getRoomNumber() + aimPoint + selectedPlayer.getName()));
            Long shipID = shipRepository.findShipIdsByPlayerAndCoOrdsContainingPair(selectedPlayer.getId(), aimPoint);
            Optional<Ship> shipToUpdate = shipRepository.findById(shipID);
            Ship ship = shipToUpdate.get();
            String shipHealth = ship.getCoOrds();
            String newShipHealth = shipHealth.replace(aimPoint, "XX");
            ship.setCoOrds(newShipHealth);
            shipRepository.save(ship);
            enumerateShips(selectedPlayer.getId(), selectedPlayer2.getId());
            computerCheck(selectedPlayer.getName());
        } else {
            if (!selectedPlayer2.isComputer()) {
                webSocketMessageSender.sendMessage("/topic/gameInfo", new Chat(selectedPlayer.getRoom().getRoomNumber() + selectedPlayer2.getName() + " Missed!"));
            } else {
                webSocketMessageSender.sendMessage("/topic/gameInfo", new Chat(selectedPlayer.getRoom().getRoomNumber() + "The Computer Missed!"));
                selectedPlayer2.setAiConfirmedHit("");
                selectedPlayer2.setAiHitCheck(false);
                playerRepository.save(selectedPlayer2);
            }
            webSocketMessageSender.sendMessage("/topic/turn", new Hidden(selectedPlayer.getRoom().getRoomNumber() + selectedPlayer.getName()));
            webSocketMessageSender.sendMessage("/topic/miss", new Hidden(selectedPlayer.getRoom().getRoomNumber() + selectedPlayer.getName() + aimPoint));
            computerCheck(selectedPlayer.getName());
        }
    }

    // Checks the damage state of a players ships
    @Transactional
    public void enumerateShips(Long id, Long id2) throws InterruptedException {
        boolean allShipsDestroyed = true;
        Player playerToCheck = playerRepository.findPlayerById(id);
        Player playerToCheck2 = playerRepository.findPlayerById(id2);
        String playerName = playerToCheck.getName();
        if (playerToCheck.isComputer()) {
            playerName = "Computer";
        }
        Ship ship = new Ship();
        List<Ship> shipToModify = shipRepository.findAllShipsByPlayerId(id);
        for (Ship shipToCheck : shipToModify)
            if (Objects.equals(shipToCheck.getShipDamage(), "XXXXXXXXXX")) {
                shipToCheck.setShipDamage("Destroyed");
                shipRepository.save(shipToCheck);
                webSocketMessageSender.sendMessage("/topic/chat", new Chat(ChatToken.generateChatToken() + playerToCheck.getRoom().getRoomNumber() + playerName + ": You destroyed my Carrier!"));
                if (!playerName.contains("Computer") && playerToCheck2.getName().contains("Computer")) {
                    playerToCheck2.setAiHitCheck(false);
                    playerToCheck2.setAiConfirmedHit(null);
                    playerToCheck2.setAiConfirmedHitInitial(null);
                    playerRepository.save(playerToCheck);
                    playerRepository.save(playerToCheck2);
                }
            } else if (Objects.equals(shipToCheck.getShipDamage(), "XXXXXXXX")) {
                shipToCheck.setShipDamage("Destroyed");
                shipRepository.save(shipToCheck);
                webSocketMessageSender.sendMessage("/topic/chat", new Chat(ChatToken.generateChatToken() + playerToCheck.getRoom().getRoomNumber() + playerName + ": You destroyed my Battleship!"));
                if (!playerName.contains("Computer") && playerToCheck2.getName().contains("Computer")) {
                    playerToCheck2.setAiHitCheck(false);
                    playerToCheck2.setAiConfirmedHit(null);
                    playerToCheck2.setAiConfirmedHitInitial(null);
                    playerRepository.save(playerToCheck);
                    playerRepository.save(playerToCheck2);
                }
            } else if (Objects.equals(shipToCheck.getShipDamage(), "XXXXXX")) {
                shipToCheck.setShipDamage("Destroyed");
                shipRepository.save(shipToCheck);
                webSocketMessageSender.sendMessage("/topic/chat", new Chat(ChatToken.generateChatToken() + playerToCheck.getRoom().getRoomNumber() + playerName + ": You destroyed my Cruiser!"));
                if (!playerName.contains("Computer") && playerToCheck2.getName().contains("Computer")) {
                    playerToCheck2.setAiHitCheck(false);
                    playerToCheck2.setAiConfirmedHit(null);
                    playerToCheck2.setAiConfirmedHitInitial(null);
                    playerRepository.save(playerToCheck);
                    playerRepository.save(playerToCheck2);
                }
            } else if (Objects.equals(shipToCheck.getShipDamage(), "XXXX")) {
                shipToCheck.setShipDamage("Destroyed");
                shipRepository.save(shipToCheck);
                webSocketMessageSender.sendMessage("/topic/chat", new Chat(ChatToken.generateChatToken() + playerToCheck.getRoom().getRoomNumber() + playerName + ": You destroyed my Destroyer!"));
                if (!playerName.contains("Computer") && playerToCheck2.getName().contains("Computer")) {
                    playerToCheck2.setAiHitCheck(false);
                    playerToCheck2.setAiConfirmedHit(null);
                    playerToCheck2.setAiConfirmedHitInitial(null);
                    playerRepository.save(playerToCheck);
                    playerRepository.save(playerToCheck2);
                }
            }
        for (Ship shipToCheck : shipToModify)
            if (!Objects.equals(shipToCheck.getShipDamage(), "Destroyed")) {
                allShipsDestroyed = false;
            }

        if (allShipsDestroyed) {
            webSocketMessageSender.sendMessage("/topic/chat", new Chat(ChatToken.generateChatToken() + playerToCheck.getRoom().getRoomNumber() + playerToCheck.getName() + " has had all their starships destroyed! And is defeated!"));
            lost.add(playerToCheck.getName());
            Room roomToCheck = new Room();
            Room roomId = roomRepository.findRoomIdByPlayersId(playerToCheck.getId());
            List<Player> players = playerRepository.findPlayersByRoomId(roomId.getId());
            String winnerName = "";
            for (Player winner : players) {
                if (!Objects.equals(winner.getName(), playerToCheck.getName())) {
                    if (winner.getName().contains("Computer")) {
                        winnerName = "Computer";
                    } else {
                        winnerName = winner.getName();
                    }
                    winner.setLevel(winner.levelUp(1));
                    winner.setAiConfirmedHit(null);
                    winner.setAiConfirmedHitInitial(null);
                    winner.setAiShot(null);
                    winner.setAiHitCheck(false);
                    playerRepository.save(winner);
                    webSocketMessageSender.sendMessage("/topic/chat", new Chat(ChatToken.generateChatToken() + playerToCheck.getRoom().getRoomNumber() + winnerName + " is the Winner!"));
                    webSocketMessageSender.sendMessage("/topic/winner", new Chat(playerToCheck.getRoom().getRoomNumber() + winnerName));
                    Thread.sleep(50);
                    webSocketMessageSender.sendMessage("/topic/gameInfo", new Chat(playerToCheck.getRoom().getRoomNumber() + winnerName + " Wins!"));
                }
            }
            }
        }

    // Generates a random coordinate
    public String generateRandomCoOrd() {
        Random random = new Random();
        int randomIndex = random.nextInt(10);
        coOrdLetters = Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H", "I", "J");
        coOrdNumbers = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
        String startLetter = coOrdLetters.get(randomIndex);
        randomIndex = random.nextInt(10);
        String startNumber = coOrdNumbers.get(randomIndex);
        return startLetter + startNumber;
    }

    // Logic to handle a computer player shooting
    @Transactional
    public void computerShoot(String playerName) throws InterruptedException {
        Thread.sleep(1000);
        List<Room> playersInRoom = roomRepository.findByPlayersName(playerName);
        List<Player> players = new ArrayList<>();
        for (Room room : playersInRoom) {
            players = room.getPlayers();
        }
        Player humanPlayer;
        Player computerPlayer;
        if (players.get(0).isComputer()) {
            humanPlayer = players.get(1);
            computerPlayer = players.get(0);
        } else {
            humanPlayer = players.get(0);
            computerPlayer = players.get(1);}

        if (computerPlayer.getAiShot() == null) {
            computerPlayer.setAiShot("");
        }

        String shoot = generateRandomCoOrd();

            while (computerPlayer.getAiShot().contains(shoot)) {
                if (computerPlayer.getAiConfirmedHitInitial() != null && !computerPlayer.getAiHitCheck()) {
                    shoot = (placing.generateStartingRandomCoOrds(computerPlayer.getAiConfirmedHitInitial(), true));
                    System.out.println("Miss but ship not destroyed");
                } else if (computerPlayer.getAiHitCheck()) {
                    shoot = (placing.generateStartingRandomCoOrds(computerPlayer.getAiConfirmedHit(), true));
                    System.out.println("Confirmed hit, attempting to find ship");
                } else {
                    shoot = generateRandomCoOrd();
                    computerPlayer.setAiConfirmedHitInitial("");
                    System.out.println("Failed to find a valid square to shoot, ending loop");
                }
                if (!computerPlayer.getAiShot().contains(shoot)) {
                    System.out.println("loop break");
                    break;
                }
            }

        computerPlayer.setAiShot(computerPlayer.getAiShot() + shoot);
        playerRepository.save(computerPlayer);
        shootAtShip(shoot + humanPlayer.getName().substring(0, 4) + computerPlayer.getName().substring(0, 4));
    }
}