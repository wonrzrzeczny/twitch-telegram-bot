package pl.edu.mimuw.kk406184.twitchtelegrambot;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

// Parses twitch notification sent to the callback server and prepares list of subscribed telegram users
public class TTBNotificationProcessor implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		
		String response = exchange.getMessage().getBody(String.class);
		exchange.setProperty("logBody", response);
		exchange.getMessage().setBody(response);
		
		JSONParser parser = new JSONParser();
		JSONObject json = (JSONObject)parser.parse(response);
		JSONArray streams = (JSONArray)json.get("data");
		if (streams.size() == 0) {
			exchange.setProperty("recipients", "");
			return;
		}
		
		int userId = Integer.parseInt((String)((JSONObject)streams.get(0)).get("user_id"));
		
		if (TTBRoute.subscriptions.containsKey(userId)) {
			StringBuilder sb = new StringBuilder();
			for (Integer chatId : TTBRoute.subscriptions.get(userId)) {
				sb.append(chatId.toString());
				sb.append("$");
			}
			String recipients = sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "";
			exchange.setProperty("recipients", recipients);
		}
		else {
			exchange.setProperty("recipients", "");
		}
	}
}
