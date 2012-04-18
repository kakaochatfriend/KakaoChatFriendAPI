package com.kakao.bot.echobot;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bson.BSON;
import org.bson.BSONObject;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.util.JSON;

public class BotHandler extends SimpleChannelUpstreamHandler {
	static BSONObject loginPkt;
	
	BotProtocol protocol;
	final ClientBootstrap bootstrap;
	
	Logger logger = LoggerFactory.getLogger(BotHandler.class);
	
	Class[] paramTypes = new Class[] { Channel.class, BSONObject.class };
	
	
	BotHandler(final ClientBootstrap bootstrap, BotProtocol protocol, String id, String pass) {
		this.bootstrap = bootstrap;
		this.protocol = protocol;
		loginPkt = (BSONObject)JSON.parse("{ \"type\":\"login\", \"id\":\"" + id + "\", \"pass\":\"" + pass + "\" } ");
	}
	
	@Override
	public void messageReceived(ChannelHandlerContext ctx, final MessageEvent e) {
		// logic insert 
		Object obj = e.getMessage();
		if (obj instanceof byte[]) {
			BSONObject bsonIn = BSON.decode((byte[]) obj);

			logger.debug("BsonIn = " + bsonIn);

			String type = (String) bsonIn.get("type");
			if (type == null) {
				// error
				logger.debug("null");
			} else {
				
				Method method;
				try {
					method = BotProtocol.class.getMethod("handle_"+type, paramTypes);
					method.invoke(protocol, ctx.getChannel(), bsonIn);
				} catch (SecurityException ex) {
					ex.printStackTrace();
				} catch (NoSuchMethodException e1) {
					e1.printStackTrace();
					logger.info("method type not exist" + type);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		} else {
			logger.debug("unknown " + obj.toString());
		}
	}
	
	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) {
		logger.debug("channel Closed. reconnect.");
		try { Thread.sleep(5000); } catch (Exception ex) { logger.debug(ex.getMessage()); }
		bootstrap.connect();
	}

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
		logger.debug("connected.");
		ctx.getChannel().write(loginPkt);
	}
	
	@Override
	public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
			logger.debug("channelDisconnected");
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
		e.getChannel().close();
	}
}
