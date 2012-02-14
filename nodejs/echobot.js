var sys = require("util"),
	bot = require("./kakao-bot.js"),
	log4js = require('log4js');

var logger = log4js.getLogger("echobot");


var KAKAO_BOT_HOST = "localhost",
	KAKAO_BOT_PORT = 11111,

	ECHOBOT_ID = "test",
	ECHOBOT_PASSWORD = "test";

var client = bot.createClient();

client.connect(KAKAO_BOT_HOST, KAKAO_BOT_PORT, ECHOBOT_ID, ECHOBOT_PASSWORD);

client.on("error", function(e) {
	logger.error("[ERROR] " + e);
	process.exit(-1);
});

client.on("add", function(user_key, room_key, msgId) {
	var greetingMessage = "반갑습니다";

	logger.info("Greeting to " + user_key + ", " + room_key + ": " + greetingMessage);
	client.sendMessage(user_key, room_key, msgId, greetingMessage);
});

client.on("message", function(user_key, room_key, msgId, countryIso, message) {
	logger.info("Message from " + user_key + ", " + room_key + ": " + message);
	client.sendMessage(user_key, room_key, msgId, message);
});

