package com.jeroscalmera.battleship_project.models;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;


@Entity
@Table(name = "players")
public class Player implements Comparable<Player>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "name")
    private String name;

    @Column(name = "player_is_computer")
    private boolean isComputer;

    @Column(name = "level")
    private int level;

    @Column(name = "stored_room_number")
    private String storedRoomNumber;

    @Column(name = "room")
    private String roomNumber;
    @OneToMany(mappedBy = "player")
    @Cascade(org.hibernate.annotations.CascadeType.REMOVE)
    @JsonIgnoreProperties({"player"})
    private List<Ship> ships;

    @ManyToOne
    @JoinColumn(name="room_id", nullable = true)
    @Cascade(org.hibernate.annotations.CascadeType.REMOVE)
    @JsonIgnoreProperties({"player"})
    private Room room;

    @Column(name = "ready")
    private boolean ready;

    @Column(name = "aiShot", length = 1000)
    private String aiShot;

    @Column(name = "aiConfirmedHitInitial")
    private String aiConfirmedHitInitial;

    @Column(name = "aiConfirmedHit")
    private String aiConfirmedHit;

    @Column(name = "aiHitCheck")
    private boolean aiHitCheck;

    @Column(name = "playerIsBanned")
    private boolean isBanned;

    public Player(String name) {
        this.name = name;
        this.isComputer = false;
        this.ready = false;
        this.aiHitCheck = false;
        this.aiShot = null;
        this.aiConfirmedHit = null;
        this.aiConfirmedHitInitial = null;
        this.isBanned = false;
    }

    public Player() {
    }

    @Override
    public int compareTo(Player player) {
        return Integer.compare(player.level, this.level);
    }
    public void setShips(List<Ship> ships) {
        this.ships = ships;
    }

    public boolean isComputer() {
        return isComputer;
    }

    public void setComputer(boolean computer) {
        isComputer = computer;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLevel() {
        return level;
    }

    public String getDetails() {
        return name + " Lvl:(" + this.getLevel() + ")";
    }

    public int levelUp(int value) {
        this.level += value;
        return this.level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void addShip(Ship ship) {
        this.ships.add(ship);
    }

    public void setReady() {this.ready = true;}

    public void setUnReady() {this.ready = false;}

    public String getAiShot() {
        return aiShot;
    }

    public void setAiShot(String aiShot) {
        this.aiShot = aiShot;
    }

    public boolean getAiHitCheck() {
        return aiHitCheck;
    }

    public void setAiHitCheck(boolean aiHitCheck) {
        this.aiHitCheck = aiHitCheck;
    }

    public String getAiConfirmedHit() {
        return aiConfirmedHit;
    }

    public void setAiConfirmedHit(String aiConfirmedHit) {
        this.aiConfirmedHit = aiConfirmedHit;
    }

    public String getRoomNumber() {
        return storedRoomNumber;
    }

    public void setRoomNumber(String storedRoomNumber) {
        this.storedRoomNumber = storedRoomNumber;
    }

    public String getAiConfirmedHitInitial() {
        return aiConfirmedHitInitial;
    }

    public void setAiConfirmedHitInitial(String aiConfirmedHitInitial) {
        this.aiConfirmedHitInitial = aiConfirmedHitInitial;
    }
    public boolean playerIsBanned() {
        return this.isBanned;
    }
}

