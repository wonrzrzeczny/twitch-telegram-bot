package pl.edu.mimuw.kk406184.twitchtelegrambot;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;

// Parses helix/games response from twitch api.
// Stores map from game ids to names in gamesResponseMap property
// Stores id of first queried game in body
public class TTBGamesResponseProcessor implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		String response = exchange.getMessage().getBody(String.class);
		
		JSONParser parser = new JSONParser();
		JSONObject json = (JSONObject)parser.parse(response);
		JSONArray streams = (JSONArray)json.get("data");
		
		Map<String, String> gamesMap = new HashMap<>();
		
		for (int i = 0; i < streams.size(); i++) {
			JSONObject stream = (JSONObject)streams.get(i);
			String id = stream.get("id").toString();
			String name = stream.get("name").toString();
			gamesMap.put(id, name);
		}
		
		exchange.setProperty("gamesResponseMap", JSONValue.toJSONString(gamesMap));
		exchange.getMessage().setBody(gamesMap.isEmpty() ? "-1" : gamesMap.keySet().toArray()[0]);
	}

}
