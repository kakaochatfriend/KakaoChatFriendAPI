package com.kakao.bot.echobot;

import java.net.InetSocketAddress;
import java.util.HashMap;

import org.bson.BSON;
import org.bson.BSONObject;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import com.mongodb.util.JSON;

public class EchoHandler extends SimpleChannelUpstreamHandler {
	public static final BSONObject loginPkt = (BSONObject)JSON.parse("{ \"type\":\"login\", \"id\":\"testtest\", \"pass\":\"testtest\" } ");
	
	final HashMap<String, BotHandler> handler;
	final ClientBootstrap bootstrap;
	
	EchoHandler(final ClientBootstrap bootstrap, final HashMap<String, BotHandler> handler) {
		this.bootstrap = bootstrap;
		this.handler = handler;
	}
	
	@Override
	public void messageReceived(ChannelHandlerContext ctx, final MessageEvent e) {
		// logic insert 
		
		Object obj = e.getMessage();
		if (obj instanceof byte[]) {
			BSONObject bsonIn = BSON.decode((byte[]) obj);

			System.out.println("BsonIn = " + bsonIn);

			String type = (String) bsonIn.get("type");
			if (type == null) {
				// error
				System.out.println("null");
			} else {
				Object handle = this.handler.get(type);
				if (handle != null && handle instanceof BotHandler) {
					((BotHandler)handle).request(ctx.getChannel(), bsonIn);
				} else {
					System.out.println("NOT HANDLED : "+bsonIn);
				}
			}
		} else {
			System.out.println(obj);
		}
	}
	
	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) {
		System.out.println("channel Closed. reconnect.");
		try { Thread.sleep(5000); } catch (Exception ex) { ex.printStackTrace(); }
		bootstrap.connect();
	}

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
		System.out.println("connected.");
		ctx.getChannel().write(loginPkt);
	}
	
	@Override
	public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
			System.out.println("channelDisconnected");
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
		e.getChannel().close();
	}
}
