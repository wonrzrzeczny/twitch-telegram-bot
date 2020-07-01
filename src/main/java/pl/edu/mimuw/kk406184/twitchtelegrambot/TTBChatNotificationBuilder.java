package pl.edu.mimuw.kk406184.twitchtelegrambot;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.telegram.model.OutgoingTextMessage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

// Parses helix/streams response (provided in "streamsResponseBody" property) and builds notification chat message
// Used in callback server route
public class TTBChatNotificationBuilder implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		JSONParser parser = new JSONParser();
		
		JSONObject response = (JSONObject)parser.parse(exchange.getProperty("streamsResponseBody", String.class));
		JSONObject gameNames = (JSONObject)parser.parse(exchange.getProperty("gamesResponseMap", String.class));
		
		StringBuilder sb = new StringBuilder();
		
		JSONArray streams = (JSONArray)response.get("data");
		JSONObject stream = (JSONObject)streams.get(0);
		String streamTitle = stream.get("title").toString();
		String streamDisplayName = stream.get("user_name").toString();
		String thumbnailUrl = stream.get("thumbnail_url").toString();
		String streamUserName = thumbnailUrl.substring(52).split("-")[0]; // twitch API bug
		String streamUrl = "twitch.tv/" + streamUserName;
		String streamGameId = stream.get("game_id").toString();
		String streamGame = gameNames.get(streamGameId).toString();
		String streamViewers = stream.get("viewer_count").toString();
		sb.append("*" + streamDisplayName + "* started streaming " + streamGame + "!\n");
		sb.append("[" + streamTitle.replaceAll("\\[|\\]", "") + "](" + streamUrl + ")\n"); //square brackets break markdown links
		sb.append("Viewers: " + streamViewers + "\n");
		
		exchange.getMessage().setBody(new OutgoingTextMessage(sb.toString(), "Markdown", true, null));
	}
}
