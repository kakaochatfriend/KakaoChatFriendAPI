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
			Executors.newCachedThreadPool(), Executors.newCachedThreadPool());

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
		final ClientBootstrap bootstrap = new ClientBootstrap(
				clientSocketChannel);
		final BotProtocol botImpl = new EchoBotProtocolImpl();

		bootstrap.setOption("connectTimeoutMillis", 1000);
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline pipeline = pipeline();
				pipeline.addLast("decoder", new BsonDecoder());
				pipeline.addLast("encoder", new BsonEncoder());
				pipeline.addLast("handler", new BotHandler(bootstrap, botImpl,
						"testtest", "testtest"));
				return pipeline;
			}
		});

		bootstrap.setOption("remoteAddress", new InetSocketAddress(host, port));
		bootstrap.connect();
	}

	public static class EchoBotProtocolImpl extends BotProtocol {
		@Override
		public void handle_request(Channel channel, BSONObject bsonIn) {
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
				msgs.add("3:"
						+ (new StringBuffer(message)).reverse().toString());

				logger.debug("OUT => " + bsonOut);

				channel.write(BSON.encode(bsonOut));
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}

		@Override
		public void handle_add(Channel channel, BSONObject e) {
			long userKey = (Long) e.get("user_key");
			long roomKey = (Long) e.get("room_key");
			int msgId = (Integer) e.get("msg_id");

			BSONObject bOut = new BasicBSONObject();
			bOut.put("type", "response");
			bOut.put("room_key", roomKey);
			bOut.put("user_key", userKey);
			bOut.put("msg_id", msgId);
			bOut.put("message", "thank you for add me.");

			channel.write(BSON.encode(bOut));

			logger.debug("LOGIN user(%d) ", userKey);
		}
	}
}
