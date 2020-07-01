package pl.edu.mimuw.kk406184.twitchtelegrambot;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

// Prepares list of subscribed topics on twitch webhook api
// Used in subscription refreshing route
public class TTBWebhookList implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		
		StringBuilder sb = new StringBuilder();
		for (Integer userId : TTBRoute.subscriptions.keySet()) {
			sb.append(userId);
			sb.append("$");
		}
		String data = sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "";
		exchange.setProperty("userIds", data);
	}
}
