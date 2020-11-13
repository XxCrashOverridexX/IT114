package server;


class GameLogic{
	
	
	public static void gameLogic(){
	Room.serverBroadcast("Welcome to Hangman");	
	Hangman.sendToPlayer();
	
	}	
}