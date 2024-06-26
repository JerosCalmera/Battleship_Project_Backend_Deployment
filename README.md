# Capstone Project - Multiplayer Battleship

Brief:  
------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
To solo develop a multiplayer Battleship board game, playable in the browser between two people.
Players can play a game of battleship via websocket, each player can see their board and the enemies, game plays in the same fashion as the board game, two boards will be visible to each player, their own with the ships they placed and the opponents.

Tech Stack: 
------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
Database - A PostgreSQL database used to store game data (game board info, player names, games won etc.)  
Frontend - TypeScript with React  
Backend - Java with Spring Boot  
Messaging - STOMP over WebSocket  is used for frontend and backend communication  

Features:
------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
- The game can be played multiplayer between two players or single player against a computer controlled opponent
- Players have a name they can set, if a player is returning, they can access their saved user data within the database that has saved name and level
- Players can see how many games they have won in total (players gain a level for every game won)
- Players connect to a "room" designated by a manually typed or randomly generated four number code that both players must enter
- A chat feature allows players to communicate with each other, and also allows the "Admin" to send messages via the backend if needed, chat messages are unique to each room
- Players can randomly place ships on the game board if they do not wish to manually place them
- A Leaderboard shows top players and their levels
- Players can submit bug reports or messages the developer, these are saved into the database for later review
- Various lag handling features such as the use of loading screens and unique chat keys designed to ensure repeated messages are not sent in the event of a poor connection
- The game will inform the other player when their opponent has left the game using the reset option
- When a player uses the reset function, the backend will purge data no longer needed from the database

Development over time:
------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
This project started as a two week final capstone project in my time in CodeClan, at the point of deadline it was a simple two player game. Since graduating I have expanded on the project to allow for more than one game to be played at once and also for a single player to play against a computer opponent if they wish. Various visual upgrades and a large amount of general improvements to the code to fix bugs and improve the users quality of life (such as the ability to send bug reports and be informed when background loading is going on) have been added in to improve the experience.

The code has been commented for clarity.

This game is currently hosted online on Heroku.

https://solar-fury-v1-c6d0bb48da3c.herokuapp.com/ (Frontend)

The backend and frontend exist as separate applications that communicate via websocket.

![1](https://github.com/JerosCalmera/Battleship_Project_Frontend_Deployment/assets/136751073/3df47f39-3e66-4eec-9cca-8fbc39e26c52)
![2](https://github.com/JerosCalmera/Battleship_Project_Frontend_Deployment/assets/136751073/bd26dc96-bb0a-460f-8ce2-74c090e671a0)
![3](https://github.com/JerosCalmera/Battleship_Project_Frontend_Deployment/assets/136751073/00eac204-b08f-43e6-ad6c-ae1932b76c88)
![4](https://github.com/JerosCalmera/Battleship_Project_Frontend_Deployment/assets/136751073/29d00f53-a31e-445e-a58c-ed6788866f76)
![twoplayer](https://github.com/JerosCalmera/Battleship_Project_Frontend_Deployment/assets/136751073/43ea60ff-dbba-4390-922c-8791c22a209c)



