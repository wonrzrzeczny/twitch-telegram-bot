package pl.edu.mimuw.kk406184.twitchtelegrambot;

// Stores information about last query
public class TTBCursorInfo {
	
	private String searchLang, searchGame, searchUser, cursor;
	
	public TTBCursorInfo(String searchLang, String searchGame, String searchUser, String cursor) {
		this.searchLang = searchLang;
		this.searchGame = searchGame;
		this.searchUser = searchUser;
		this.cursor = cursor;
	}
	
	public String getLang() {
		return searchLang;
	}
	
	public String getGame() {
		return searchGame;
	}
	
	public String getUser() {
		return searchUser;
	}
	
	public String getCursor() {
		return cursor;
	}
}
