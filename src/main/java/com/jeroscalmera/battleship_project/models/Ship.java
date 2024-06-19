package com.jeroscalmera.battleship_project.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;

@Entity
@Table(name = "ships")
public class Ship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name="player_id", nullable = true)
    @JsonIgnoreProperties({"ship"})
    private Player player;
    @Column(name = "name")
    private String name;
    @Column(name = "max_size")
    private int maxSize;
    @Column(name = "coOrds")
    private String coOrds;
    public Ship(String name, int maxSize, String coOrds) {
        this.name = name;
        this.maxSize = maxSize;
        this.coOrds = coOrds;
    }
    public void setPlayer(Player player) {
        this.player = player;
    }
    public Player getPlayer() {
        return player;
    }
    public Ship(){
    }
    public String getName() {
        return name;
    }
    public String getCoOrds() {
        return coOrds;
    }
    public void setShipDamage(String damage) {
        this.coOrds = damage;
    }
    public String getShipDamage() {
        return coOrds;
    }
    public void setCoOrds(String coOrds) {
        this.coOrds = coOrds;
    }
    public void setDamage(String newCoOrd) {
        if (this.coOrds.length() < this.maxSize)
        {setCoOrds(getCoOrds() + newCoOrd);}
    }
}
