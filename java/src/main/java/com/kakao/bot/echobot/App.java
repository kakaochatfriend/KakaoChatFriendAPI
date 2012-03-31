package com.kakao.bot.echobot;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import org.bson.BSON;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ChannelState;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import static org.jboss.netty.channel.Channels.pipeline;

import com.mongodb.util.JSON;

/**
 * EchoBot
 */
public class App {

	public static String host = "127.0.0.1";
	public static int port = 8080;

	private static NioClientSocketChannelFactory clientSocketChannel = new NioClientSocketChannelFactory(
				Executors.newCachedThreadPool(),
				Executors.newCachedThreadPool());

	private static ClientBootstrap bootstrap = new ClientBootstrap(clientSocketChannel);

	public static final BSONObject loginPkt = (BSONObject) JSON
			.parse("{ \"type\":\"login\", \"id\":\"testtest\", \"pass\":\"testtest\" } ");

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

		bootstrap.setOption("connectTimeoutMillis", 1000);
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline pipeline = pipeline();
				pipeline.addLast("decoder", new BsonDecoder());
				pipeline.addLast("encoder", new BsonEncoder());
				pipeline.addLast("handler", new EchoHandler());
				return pipeline;
			}
		});

		ChannelFuture future = bootstrap.connect(new InetSocketAddress(
				host, port));
		future.awaitUninterruptibly();

	}

	public static class EchoHandler extends SimpleChannelUpstreamHandler {

		@Override
		public void messageReceived(ChannelHandlerContext ctx,
				final MessageEvent e) {
			Object obj = e.getMessage();
			if (obj instanceof byte[]) {
				BSONObject bsonIn = BSON.decode((byte[]) obj);

				System.out.println("BsonIn = " + bsonIn);

				String type = (String) bsonIn.get("type");
				if (type == null) {
					// error
					System.out.println("null");
				} else if (type.equals("request")) {
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

					System.out.println("OUT => " + bsonOut);

					e.getChannel().write(BSON.encode(bsonOut));
					} catch (Exception e1) {
						e1.printStackTrace();
					}

				} else if (type.equals("login")) {
					System.out.println("login ");
				} else if (type.equals("result")) {
					System.out.println("result ");
				} else if (type.equals("add")) {
					long user_key = (Long) bsonIn.get("user_key");
					long room_key = (Long) bsonIn.get("room_key");
					int msgId = (Integer) bsonIn.get("msg_id");

					BSONObject bOut = new BasicBSONObject();
					bOut.put("type", "response");
					bOut.put("room_key", room_key);
					bOut.put("user_key", user_key);
					bOut.put("msg_id", msgId);
					bOut.put("message", "방가방가!\n에코봇에 친구추가를 하다늬 ㄲㄲㄲ");

					e.getChannel().write(BSON.encode(bOut));
				} else if (type.equals("ping")) {
					BSONObject bOut = new BasicBSONObject();
					bOut.put("type", "pong");
					bOut.put("time", (Long)bsonIn.get("time"));
					e.getChannel().write(BSON.encode(bOut));
				}
			} else {
				System.out.println(obj);
			}
		}

		@Override
		public void channelConnected(ChannelHandlerContext ctx,
				ChannelStateEvent e) {
			System.out.println("connected.");
			ctx.getChannel().write(loginPkt);
		}
		
		@Override
		public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
			System.out.println("connection disconnected.");
			if( e.getChannel().isOpen() )
				e.getChannel().close();
		}

		@Override
		public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) {
			System.out.println("Retry 1sec later");
			try{ Thread.sleep(1000); } catch(Exception ex) { }
			bootstrap.connect(new InetSocketAddress(host, port));
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
			System.out.println(e.getCause());
			e.getChannel().close();
		}

	}
}
