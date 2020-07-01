package pl.edu.mimuw.kk406184.twitchtelegrambot;

import java.util.HashSet;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.telegram.model.OutgoingTextMessage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

// Processes follow/unfollow command
public class TTBWebhookProcessor implements Processor {

	private static final String CHAT_ID = "CamelTelegramChatId";
	
	@Override
	public void process(Exchange exchange) throws Exception {
		String response = exchange.getMessage().getBody(String.class);
		
		JSONParser parser = new JSONParser();
		JSONObject json = (JSONObject)parser.parse(response);
		JSONArray users = (JSONArray)json.get("data");
		
		if (users.size() == 0) {
			String text = "User *" + exchange.getProperty("user") + "* not found :c";
			exchange.setProperty("chatResponse", new OutgoingTextMessage(text, "Markdown", true, null));
			return;
		}
		
		JSONObject user = (JSONObject)users.get(0);
		int userId = Integer.parseInt((String)user.get("id"));
		int chatId = Integer.parseInt(exchange.getProperty(CHAT_ID, String.class));
		
		String text;
		if (exchange.getProperty("status").equals("follow")) {
			if (!TTBRoute.subscriptions.containsKey(userId)) {
				TTBRoute.subscriptions.put(userId, new HashSet<Integer>());
			}
			TTBRoute.subscriptions.get(userId).add(chatId);
			exchange.setProperty("userId", userId);
			text = "You are now following *" + exchange.getProperty("user") + "*!";
		}
		else {
			if (!TTBRoute.subscriptions.containsKey(userId) || !TTBRoute.subscriptions.get(userId).contains(chatId)) {
				text = "You can't unfollow someone you're not following :v";
			}
			else {
				TTBRoute.subscriptions.get(userId).remove(chatId);
				if (TTBRoute.subscriptions.get(userId).isEmpty()) {
					TTBRoute.subscriptions.remove(userId);
				}
				text = "You are no longer following *" + exchange.getProperty("user") + "* :c";
			}
		}

		exchange.setProperty("chatResponse", new OutgoingTextMessage(text, "Markdown", true, null));
	}
}
