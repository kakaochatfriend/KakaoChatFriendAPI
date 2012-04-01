package com.kakao.bot.echobot;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;

import org.bson.BSON;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import static org.jboss.netty.channel.Channels.pipeline;

import com.mongodb.util.JSON;

/**
 * EchoBot
 */
public class App {

	public static String host = "localhost";
	public static int port = 8080;

	private static NioClientSocketChannelFactory clientSocketChannel = new NioClientSocketChannelFactory(
				Executors.newCachedThreadPool(),
				Executors.newCachedThreadPool());

	public static void main(String[] args) {

		if (args.length > 1) {
			host = args[0];
			System.out.println(host);
			port = Integer.parseInt(args[1]);
			System.out.println(port);
		}
		createClient();

	}

	public static void createClient() {
		final ClientBootstrap bootstrap = new ClientBootstrap(clientSocketChannel);
		final HashMap<String, BotHandler> hm = new HashMap<String, BotHandler>();
		hm.put("request", new RequestHandler());
		hm.put("ping", new PingPongHandler());
		hm.put("add", new LoginHandler());

		bootstrap.setOption("connectTimeoutMillis", 1000);
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline pipeline = pipeline();
				pipeline.addLast("decoder", new BsonDecoder());
				pipeline.addLast("encoder", new BsonEncoder());
				pipeline.addLast("handler", new EchoHandler(bootstrap, hm));
				return pipeline;
			}
		});
		
		bootstrap.setOption("remoteAddress", new InetSocketAddress(host,port));
		bootstrap.connect();
	}

	public static class RequestHandler implements BotHandler {
		public void request(Channel channel, final BSONObject bsonIn) {
			// logic insert
			try {
				long user_key = (Long) bsonIn.get("user_key");
				long room_key = (Long) bsonIn.get("room_key");
				int msgId = (Integer) bsonIn.get("msg_id");

				String message = (String) bsonIn.get("message");

				BSONObject bsonOut = new BasicBSONObject();
				bsonOut.put("type", "response");
				bsonOut.put("room_key", room_key);
				bsonOut.put("user_key", user_key);
				bsonOut.put("msg_id", msgId);
				bsonOut.put("message", "1:" + message);
				List msgs = new ArrayList();
				msgs.add("2:" + message);
				msgs.add("3:" + (new StringBuffer(message)).reverse().toString());

				System.out.println("OUT => " + bsonOut);

				channel.write(BSON.encode(bsonOut));
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}
	
	public static class LoginHandler implements BotHandler {
		public void request(Channel channel, final BSONObject bsonIn) {
			// logic insert
			long userKey = (Long) bsonIn.get("user_key");
			System.out.println(String.format("LOGIN user(%d) ", userKey));
		}
	}

	public static class AddedHandler implements BotHandler {
		public void request(Channel channel, final BSONObject bsonIn) {
			
			long user_key = (Long) bsonIn.get("user_key");
			long room_key = (Long) bsonIn.get("room_key");
			int msgId = (Integer) bsonIn.get("msg_id");

			BSONObject bOut = new BasicBSONObject();
			bOut.put("type", "response");
			bOut.put("room_key", room_key);
			bOut.put("user_key", user_key);
			bOut.put("msg_id", msgId);
			bOut.put("message", "방가방가!\n에코봇에 친구추가를 하다늬 ㄲㄲㄲ");

			channel.write(BSON.encode(bOut));
		}
	}	
	
	public static class PingPongHandler implements BotHandler {
		public void request(Channel channel, final BSONObject bsonIn) {
			BSONObject bOut = new BasicBSONObject();
			bOut.put("type", "pong");
			bOut.put("time", (Long)bsonIn.get("time"));
			channel.write(BSON.encode(bOut));
		}
	}	
}
