package pl.edu.mimuw.kk406184.twitchtelegrambot;

import java.util.*;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;

// Parses sent message
public class TTBCommandProcessor implements Processor{
	
	private static final String CHAT_ID = "CamelTelegramChatId";
	
	@Override
	public void process(Exchange exchange) throws Exception {
		Message message = exchange.getMessage();
		exchange.setProperty(CHAT_ID, message.getHeader(CHAT_ID));
		message.removeHeader(CHAT_ID);
		
		String body = message.getBody(String.class);
		if (body.length() == 0 || body.charAt(0) != '?') {
			exchange.setProperty("type", "unknown");
			return;
		}
		else if (body.equals("?all")) {
			exchange.setProperty("type", "search");
			exchange.setProperty("streamsRequestParams", "first=5");
			return;
		}
		else if (body.equals("?next")) {
			TTBCursorInfo cursorInfo = TTBRoute.lastCursor.get(exchange.getProperty(CHAT_ID));
			if (cursorInfo == null) {
				exchange.setProperty("type", "error");
				exchange.setProperty("errorMessage", "You must make a query first.");
				return;
			}
			exchange.setProperty("type", "search");
			exchange.setProperty("streamsRequestParams", "first=5&after=" + cursorInfo.getCursor());
			if (cursorInfo.getGame() != null) {
				exchange.setProperty("searchGame", cursorInfo.getGame());
			}
			if (cursorInfo.getUser() != null) {
				exchange.setProperty("searchUser", cursorInfo.getUser());
			}
			if (cursorInfo.getLang() != null) {
				exchange.setProperty("searchLang", cursorInfo.getLang());
			}
			
			return;
		}
		else if (body.startsWith("?follow ") || body.startsWith("?unfollow ")) {
			
			int separator = body.indexOf(' ');
			exchange.setProperty("type", "webhook");
			exchange.setProperty("status", body.startsWith("?follow") ? "follow" : "unfollow");
			exchange.setProperty("user", body.substring(separator + 1).trim());
			
			return;
		}
		
		List<String> data = Arrays.asList(body.substring(1).split("\\?"));
		exchange.setProperty("type", "search");
		exchange.setProperty("streamsRequestParams", "first=5");
		for (String query : data) {
			int separatorIndex = query.indexOf(' ');

			String param = separatorIndex == -1 ? query : query.substring(0, separatorIndex);
			String value = separatorIndex == -1 ? " " : query.substring(separatorIndex + 1).trim();
			
			if (param.equals("user")) {
				exchange.setProperty("searchUser", value);
			}
			else if (param.equals("lang")) {
				exchange.setProperty("searchLang", value);
			}
			else if (param.equals("game")) {
				exchange.setProperty("searchGame", value);
			}
			else {
				exchange.setProperty("type", "unknown");
				return;
			}
		}
	}
}
