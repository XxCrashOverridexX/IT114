package server;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import client.ClientUI;
import client.User;





class Hangman {
	private final static Logger log = Logger.getLogger(Hangman.class.getName());
	private static String secretWord;
	public static List<String> wordAsChar = new ArrayList<String>();
	public static List<String> playerWord = new ArrayList<String>();
	public static boolean checkerFlag = false;
	
	public static void startGame() { //sets initial variables for game
		// First server picks a word and makes its reference
		secretWord = Words.Selector();
		wordAsChar = List.of(secretWord.split(""));
		for (int i=0;i<wordAsChar.size(); i++) {
			playerWord.add(wordAsChar.get(i));
		}
		for (int i=0; i<playerWord.size();i++) {
			
			if (playerWord.get(i) != "  ") {
				playerWord.set(i, "_");
			}
			
		}
		log.log(Level.INFO,secretWord);
		
		
			}
	
	public static void setOrder() {
		//List<User> gameOrder = new ArrayList<User>();
		
	}
	
	public static void checkLetterGuess(String guess) {
		for (int i=0;i<playerWord.size();i++) {
			if (wordAsChar.get(i) == guess) {
				playerWord.set(i, wordAsChar.get(i));
				Room.serverBroadcast("Correct!");
			}
		}
		sendToPlayer();
		
	}
	
	public static void sendToPlayer(){
		String outputToPlayer;
		outputToPlayer = playerWord.toString();
		outputToPlayer.replace("[", "");
		outputToPlayer.replace(",", "");		
		outputToPlayer.replace("]", "");
				
		
		Room.serverBroadcast(outputToPlayer);
		
	}

}
	



	

