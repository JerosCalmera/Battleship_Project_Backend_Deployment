package com.jeroscalmera.battleship_project.models;


import javax.persistence.*;

@Entity
@Table(name = "lobby")
public class Lobby {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "lobby_room")
    private String lobbyRoom;

    @Column(name = "IsSaved")
    private boolean isSaved;

    @Column(name = "IsValidated")
    private boolean isValidated;

    public Lobby(String lobbyRoom) {
        this.lobbyRoom = lobbyRoom;
    }

    public Lobby() {
    }

    public boolean isSaved() {
        return isSaved;
    }

    public void setSaved(boolean saved) {
        isSaved = saved;
    }

    public boolean isValidated() {
        return isValidated;
    }

    public void setValidated(boolean validated) {
        isValidated = validated;
    }
}