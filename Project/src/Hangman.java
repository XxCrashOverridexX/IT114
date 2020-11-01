import utils.Debug;



class Hangman {
	private static String secretWord;
	public static int len;
	

	
	public static void startGame() {
		// First server picks a word and makes its reference
		secretWord = Words.Selector();
		len = secretWord.length();
		Character[] wordAsChar = new Character[len];
		for (int i=0; i< len; i++) {
			wordAsChar[i] = secretWord.charAt(i);
			Debug.hiddenLog(wordAsChar[i]);
		}
		
		//Convert char array to blank spaces to player
		Character[] playerWord = new Character[len];
		char uscore = '_';
		for (int i=0; i< len; i++) {//Re-setup array to give to player
			playerWord[i] = secretWord.charAt(i);
		}
		for (int i=0; i<len;i++) {
			if (playerWord[i] == ' ') { //don't turn spaces into underscores
				continue;
			}else {
				playerWord[i] = uscore;
			}//else
		}//for
		
		
	}
	public static void sendGameText() {

		
	}
}


	

