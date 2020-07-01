# Twitch Telegram Bot

A simple Telegram bot allowing to search for active Twitch streams, written in Java using Apache Camel framework. Project written as an assignment for Languages and tools for programming II course at MIM UW.

Screenshots
-----------

<img src="screen_help.png" alt="" width="400">
<img src="screen_all.png" alt="" width="400">
<img src="screen_params.png" alt="" width="400">
<img src="screen_user.png" alt="" width="400">
<img src="screen_next.png" alt="" width="400">
<img src="screen_follow.png" alt="" width="400">

Installation
------------

In application.properties:

* replace `camel.component.telegram.authorization-token` with your telegram bot token
* replace `twitch.api.client-id` with your twitch application client-id
* replace `twitch.api.auth` with twitch OAuth token
* replace `callback-address` with your host address

Use `maven clean install spring-boot:repackage` to create .jar file.