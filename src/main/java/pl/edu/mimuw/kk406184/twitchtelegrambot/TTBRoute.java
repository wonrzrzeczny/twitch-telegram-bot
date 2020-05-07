package pl.edu.mimuw.kk406184.twitchtelegrambot;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class TTBRoute extends RouteBuilder {

	static Map<String, TTBCursorInfo> lastCursor = new HashMap<>(); // information about last query per chat
	
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
			+ "After making a query, you can send *?next* to list more results\n";
	
	@Override
	public void configure() throws Exception {
		from("telegram:bots")
			.log("Incoming message: ${body}")
			.process(new TTBCommandProcessor())
			.toD("direct:${exchangeProperty.type}");
		
		from("direct:unknown")
			.setHeader(CHAT_ID, simple("${exchangeProperty." + CHAT_ID + "}"))
			.setHeader(PARSE_MODE, constant("MARKDOWN"))
			.setBody(constant(unknownCommandString))
			.to("telegram:bots");
		
		from("direct:error")
			.removeHeader(TWITCH_TOKEN)
			.removeHeader(TWITCH_AUTH)
			.setHeader(CHAT_ID, simple("${exchangeProperty." + CHAT_ID + "}"))
			.setHeader(PARSE_MODE, constant("MARKDOWN"))
			.setBody(simple("${exchangeProperty.errorMessage}"))
			.to("telegram:bots");
		
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
	}
}
 