package com.jeroscalmera.battleship_project.repositories;

import com.jeroscalmera.battleship_project.models.Player;
import com.jeroscalmera.battleship_project.models.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    void deleteAll();
    @Query
    Room findByRoomNumber(String roomNumber);

    @Query
    Room findRoomIdByPlayersId(Long id);

    @Query
    Room findRoomByPlayersName(String name);

    @Query
    List<Room> findByPlayersName(String playerName);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Room r JOIN r.players p WHERE p.name = :name")
    boolean existsByPlayersName(@Param("name") String playerName);

}