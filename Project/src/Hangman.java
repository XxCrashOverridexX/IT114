import java.util.ArrayList;
import java.util.List;

import utils.Debug;



class Hangman {
	private static String secretWord;
	public static List<String> wordAsChar = new ArrayList<String>();
	public static List<String> playerWord = new ArrayList<String>();
	
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
		Debug.log(secretWord);
		Debug.log(wordAsChar);
		Debug.log(playerWord);
		
		
			}
	
	public static void sendToPlayer(){
		String outputToPlayer;
		outputToPlayer = playerWord.toString();
		outputToPlayer.replace("[", "");
		outputToPlayer.replace(",", "");		
		outputToPlayer.replace("]", "");
				
		Debug.log(outputToPlayer);
		Room.serverBroadcast(outputToPlayer);
		
	}

}
	



	

