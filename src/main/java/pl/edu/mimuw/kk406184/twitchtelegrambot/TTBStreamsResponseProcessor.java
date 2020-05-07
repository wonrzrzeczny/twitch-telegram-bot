package pl.edu.mimuw.kk406184.twitchtelegrambot;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

// Parses helix/streams response from twitch api
// Prepares list of game ids to query to helix/games in order to get game names
// Saves information about query in TTBRoute.lastCursor for ?next command to use
public class TTBStreamsResponseProcessor implements Processor {

	private static final String CHAT_ID = "CamelTelegramChatId";
	
	@Override
	public void process(Exchange exchange) throws Exception {
		String response = exchange.getMessage().getBody(String.class);
		exchange.setProperty("streamsResponseBody", response);
		
		StringBuilder gamesQueryParams = new StringBuilder();
		
		JSONParser parser = new JSONParser();
		JSONObject json = (JSONObject)parser.parse(response);
		JSONArray streams = (JSONArray)json.get("data");

		for (Object _stream : streams) {
			JSONObject stream = (JSONObject)_stream;
			String game_id = stream.get("game_id").toString();
			
			if (gamesQueryParams.length() > 0)
				gamesQueryParams.append("&");
			gamesQueryParams.append("id=" + game_id);
		}
		
		exchange.getMessage().setBody("");
		exchange.setProperty("gamesRequestParams", gamesQueryParams.toString());
		
		if (gamesQueryParams.toString().equals("")) { // no results found
			TTBRoute.lastCursor.remove(exchange.getProperty(CHAT_ID, String.class));
		}
		else {
			JSONObject pagination = (JSONObject)json.get("pagination");
			if (pagination != null && pagination.get("cursor") != null) {
				String cursor = pagination.get("cursor").toString();
				TTBRoute.lastCursor.put(exchange.getProperty(CHAT_ID, String.class), 
						new TTBCursorInfo(exchange.getProperty("searchLang", String.class), 
								exchange.getProperty("searchGame", String.class), 
								exchange.getProperty("searchUser", String.class), 
								cursor));
			}
			else { // no pagination info provided
				TTBRoute.lastCursor.remove(exchange.getProperty(CHAT_ID, String.class));
			}
		}
	}
}
