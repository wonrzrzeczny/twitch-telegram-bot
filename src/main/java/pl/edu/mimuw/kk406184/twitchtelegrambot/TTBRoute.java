package pl.edu.mimuw.kk406184.twitchtelegrambot;

import java.util.HashMap;
import java.util.Set;
import java.util.Map;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class TTBRoute extends RouteBuilder {

	static Map<String, TTBCursorInfo> lastCursor = new HashMap<>(); // information about last query per chat
	static Map<Integer, Set<Integer>> subscriptions = new HashMap<>(); // set of chats subscribed to twitch user
	
	private static final String CHAT_ID = "CamelTelegramChatId";
	private static final String PARSE_MODE = "CamelTelegramParseMode";
	private static final String TWITCH_TOKEN = "Client-ID";
	private static final String TWITCH_AUTH = "Authorization";
	private static final String unknownCommandString = "Unknown command :c\n\n"
			+ "Send *?all* to list all active streams\n"
			+ "*?lang <language code>* to list streams in given language (eg. _?lang en_)\n"
			+ "*?game <game name>* to list streams from given game (eg. _?game Minecraft_)\n"
			+ "*?user <user name>* to list streams by given user (eg. _?user ryukahr_)\n"
			+ "You can use *?lang*, *?game* and *?user* in one query (eg. _?game Super Mario Maker 2 ?lang ko_)\n"
			+ "After making a query, you can send *?next* to list more results\n"
			+ "\n"
			+ "Send *?follow <user>* / *?unfollow <user>* to start / stop receiving notifications when user starts streaming\n";
	
	@Override
	public void configure() throws Exception {
		
		// incoming telegram messages
		from("telegram:bots")
			.log("Incoming message: ${headers}")
			.process(new TTBCommandProcessor())
			.toD("direct:${exchangeProperty.type}");
		
		// unknown command
		from("direct:unknown")
			.setHeader(CHAT_ID, simple("${exchangeProperty." + CHAT_ID + "}"))
			.setHeader(PARSE_MODE, constant("MARKDOWN"))
			.setBody(constant(unknownCommandString))
			.to("telegram:bots");
		
		// error while processing command
		from("direct:error")
			.removeHeader(TWITCH_TOKEN)
			.removeHeader(TWITCH_AUTH)
			.setHeader(CHAT_ID, simple("${exchangeProperty." + CHAT_ID + "}"))
			.setHeader(PARSE_MODE, constant("MARKDOWN"))
			.setBody(simple("${exchangeProperty.errorMessage}"))
			.to("telegram:bots");
		
		// querying twitch api to search for streams
		restConfiguration().host("https://api.twitch.tv/");
		from("direct:search")
			.setHeader(TWITCH_TOKEN, simple("{{twitch.api.client-id}}"))
			.setHeader(TWITCH_AUTH, simple("Bearer {{twitch.api.auth}}"))
			.pipeline("direct:search_user", "direct:search_game", "direct:search_lang") // Resolve query parameters
			.toD("rest:get:helix/streams?${exchangeProperty.streamsRequestParams}") // Query to Twitch API for top streams
			.process(new TTBStreamsResponseProcessor()) // Parse response and prepare list of game ids to query
			.choice()
				.when(exchangeProperty("gamesRequestParams").isEqualTo("")) // no results
					.setProperty("errorMessage", constant("No results found :c"))
					.to("direct:error")
				.otherwise()
					.toD("rest:get:helix/games?${exchangeProperty.gamesRequestParams}") // Query to Twitch API for game names
					.process(new TTBGamesResponseProcessor()) // Get game names from response
					.process(new TTBChatResponseBuilder()) // Build response to send to telegram chat
					.removeHeader(TWITCH_TOKEN)
					.removeHeader(TWITCH_AUTH)
					.setHeader(CHAT_ID, simple("${exchangeProperty." + CHAT_ID + "}"))
					.to("telegram:bots");
		
		from("direct:search_lang")
			.choice().when(exchangeProperty("searchLang").isNotNull())
				.setProperty("streamsRequestParams",
						     simple("${exchangeProperty.streamsRequestParams}&language=${exchangeProperty.searchLang}"));
		
		from("direct:search_user")
			.choice().when(exchangeProperty("searchUser").isNotNull())
				.setProperty("streamsRequestParams",
							 simple("${exchangeProperty.streamsRequestParams}&user_login=${exchangeProperty.searchUser}"));
		
		from("direct:search_game")
			.choice().when(exchangeProperty("searchGame").isNotNull())
				.toD("rest:get:helix/games?name=${exchangeProperty.searchGame}")
				.process(new TTBGamesResponseProcessor())
				.setProperty("streamsRequestParams",
							 simple("${exchangeProperty.streamsRequestParams}&game_id=${body}"));
		
		// follow/unfollow command
		from("direct:webhook")
			.setHeader(TWITCH_TOKEN, simple("{{twitch.api.client-id}}"))
			.setHeader(TWITCH_AUTH, simple("Bearer {{twitch.api.auth}}"))
			.toD("rest:get:helix/users?login=${exchangeProperty.user}")
			.process(new TTBWebhookProcessor())
			.multicast().to("direct:webhook_reply", "direct:webhook_post");
		
		// replying to user
		from("direct:webhook_reply")
			.removeHeader(TWITCH_TOKEN)
			.removeHeader(TWITCH_AUTH)
			.setBody(simple("${exchangeProperty.chatResponse}"))
			.setHeader(CHAT_ID, simple("${exchangeProperty." + CHAT_ID + "}"))
			.to("telegram:bots");
		
		// sending subscription to twitch api
		from("direct:webhook_post")
			.choice().when(exchangeProperty("userId").isNotNull())
				.setHeader("content-type", constant("application/json"))
				.setBody(simple("{ \"hub.callback\":\"{{callback-address}}\", \"hub.mode\":\"subscribe\", \"hub.lease_seconds\":86400, \"hub.topic\":\"https://api.twitch.tv/helix/streams?user_id=${exchangeProperty.userId}\" }"))
				.toD("rest:post:helix/webhooks/hub");
		
		
		// callback server for twitch notifications
		from("jetty:http://0.0.0.0:8910/?httpMethodRestrict=POST")
			.process(new TTBNotificationProcessor())
			.log("Incoming notification from twitch: ${exchangeProperty.logBody}")
			.removeHeaders("*")
			.process(new TTBStreamsResponseProcessor())
			.choice().when(exchangeProperty("gamesRequestParams").isNotEqualTo(""))
				.setHeader(TWITCH_TOKEN, simple("{{twitch.api.client-id}}"))
				.setHeader(TWITCH_AUTH, simple("Bearer {{twitch.api.auth}}"))
				.toD("rest:get:helix/games?${exchangeProperty.gamesRequestParams}") // Query to Twitch API for game name
				.removeHeader(TWITCH_TOKEN)
				.removeHeader(TWITCH_AUTH)
				.process(new TTBGamesResponseProcessor()) // Get game name from response
				.process(new TTBChatNotificationBuilder()) // Build notification message to send to telegram chat
				.setProperty("messageBody", simple("${body}"))
				.split(exchangeProperty("recipients").tokenize("\\$"))
				.setProperty("recipient", simple("${body}"))
				.setBody(simple("${exchangeProperty.messageBody}"))
				.setHeader(CHAT_ID, simple("${exchangeProperty.recipient}"))
				.to("telegram:bots");
		
		from("jetty:http://0.0.0.0:8910/?httpMethodRestrict=GET")
			.setBody(simple("${header.hub.challenge}"));
		
		
		// refreshing subscriptions every 15 minutes
		from("timer://refreshSubscriptions?fixedRate=true&period=900000")
			.log("Refreshing twitch subscriptions")
			.process(new TTBWebhookList())
			.removeHeader("firedTime")
			.setHeader(TWITCH_TOKEN, simple("{{twitch.api.client-id}}"))
			.setHeader(TWITCH_AUTH, simple("Bearer {{twitch.api.auth}}"))
			.split(exchangeProperty("userIds").tokenize("\\$"))
			.setProperty("userId", simple("${body}"))
			.to("direct:webhook_post");
	}
}
