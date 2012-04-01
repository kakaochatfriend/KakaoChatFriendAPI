package com.kakao.bot.echobot;

import org.bson.BSONObject;
import org.jboss.netty.channel.Channel;

public interface BotHandler {
	public void request(final Channel channel, final BSONObject e);
}
