package com.kakao.bot.echobot;

import org.bson.BSON;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.jboss.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BotProtocol {
	Logger logger = LoggerFactory.getLogger(BotProtocol.class);

	// must be implemented
	public void handle_ping(Channel channel, BSONObject e) {
		BSONObject out = new BasicBSONObject();
		out.put("type", "pong");
		out.put("time", (Long) e.get("time"));
		channel.write(BSON.encode(out));
	}

	// must be implemented
	public void handle_request(Channel channel, BSONObject e) {
	}

	public void handle_add(Channel channel, BSONObject e) {
		logger.debug("LOGIN user(%d) ", (Long) e.get("user_key"));
	}

	public void handle_block(Channel channel, BSONObject e) {
		logger.debug("BLOCKED user(%d) ", (Long) e.get("user_key"));
	}

	public void handle_leave(Channel channel, BSONObject e) {
		logger.debug("LEAVE user(%d) ", (Long) e.get("user_key"));
	}

	public void handle_result(Channel channel, BSONObject e) {
		logger.debug("Result : " + e);
	}
}
