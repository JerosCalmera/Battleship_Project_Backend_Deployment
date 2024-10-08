package com.jeroscalmera.battleship_project.repositories;

import com.jeroscalmera.battleship_project.models.Lobby;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LobbyRepository extends JpaRepository<Lobby, Long> {

    @Query("SELECT l FROM Lobby l WHERE l.lobbyRoom = :roomNumber")
    Lobby findLobbySingleRoom(@Param("roomNumber") String roomNumber);

    @Query("SELECT COUNT(l) > 0 FROM Lobby l WHERE l.lobbyRoom = :roomNumber")
    boolean findLobbyRoomExists(@Param("roomNumber") String roomNumber);

}
