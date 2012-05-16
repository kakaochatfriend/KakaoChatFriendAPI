package com.kakao.bot.echobot;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.Timer;
import org.jboss.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EchoBotHandler extends SimpleChannelUpstreamHandler{
	Logger LOG = LoggerFactory.getLogger(EchoBotHandler.class);
	private final ClientBootstrap bootstrap;
  private final Timer timer;
	private final String id;
	private final String pass;
	private final int delay;
	
	private static final Map<String, TYPE> map = new HashMap<String, TYPE> ();
	
	public enum TYPE{
	  ADD ("add"),
	  BLOCK ("block"),
	  LEAVE ("leave"),
	  REQUEST ("request"),
	  RESPONSE ("response"),
	  LOGIN("login"),
	  PING("ping"),
	  PONG("pong"),
	  RESULT ("result");
	  
	  private final String type;
	  private TYPE(String type) {
	    this.type = type;
	  }
	  private final String getType () {
	    return this.type;
	  }

	  public static TYPE parse (String type) {
	    if ( map.isEmpty() ) {
	      for ( TYPE item : values() ) {
	        map.put(item.getType(), item);
	      }
	    }
	    return map.get(type);
	  }
  }

	/**
	 * 
	 * @param bootstrap
	 * @param timer        delay용 timer
   * @param delay        echo를 보낼때 delay를 줄지 여부, 단위:ms
	 * @param id           login용 id
	 * @param pass         login용 pass
	 */
	public EchoBotHandler(final ClientBootstrap bootstrap, Timer timer, int delay, String id, String pass) {
    this.bootstrap = bootstrap;
    this.timer = timer;
    this.id = id;
    this.pass = pass;
    this.delay = delay;
  }
	
  private InetSocketAddress getRemoteAddress() {
    return (InetSocketAddress) bootstrap.getOption("remoteAddress");
  }

	@Override
	public void messageReceived(ChannelHandlerContext ctx, final MessageEvent e) {
	  Object obj = e.getMessage();
	  if ( !( obj instanceof BSONObject ) ) {
      LOG.info("obj isn't type of BSONObject, obj:{}", obj);
	    return;
	  }
	  
	  BSONObject bsonIn = (BSONObject)obj;
	  TYPE type = TYPE.parse((String) bsonIn.get("type"));
	  switch (type) {
	    case ADD:      handleAdd (type, e.getChannel(), bsonIn); break;
	    case BLOCK:    handleBlock (type, e.getChannel(), bsonIn); break;
	    case LEAVE:    handleLeave (type, e.getChannel(), bsonIn); break;
	    case REQUEST:  handleRequest(type, e.getChannel(), bsonIn); break;
	    case RESULT:   handleResult (type, e.getChannel(), bsonIn); break;
	    case PING:     handlePing (type, e.getChannel(), bsonIn); break;
	    case PONG:     handlePong (type, e.getChannel(), bsonIn); break;
	    default: LOG.warn("invalid type:{}, bsonIn:{}", type, bsonIn);
	  }
	}
	

  @Override
  public void channelConnected(ChannelHandlerContext ctx,ChannelStateEvent e) {
    LOG.debug("connected to {}", getRemoteAddress());
    BSONObject login = new BasicBSONObject ();
    login.put("type", "login");
    login.put("id", this.id);
    login.put("pass", this.pass);
    
    e.getChannel().write(login);
  }

  
	protected void handlePing(TYPE type, final Channel channel, BSONObject bsonIn) {
    LOG.debug("PING ts({}) ", (Long) bsonIn.get("time"));
		BSONObject out = new BasicBSONObject();
		out.put("type", "pong");
		out.put("time", (Long) bsonIn.get("time"));
		channel.write(out);
	}
	
  protected void handlePong(TYPE type, final Channel channel, BSONObject bsonIn) {
    LOG.debug("PONG ts({})", (Long) bsonIn.get("time"));
  }

  protected void handleRequest(TYPE type, final Channel channel, BSONObject bsonIn) {
    LOG.debug("REQUEST user(%d) ", (Long) bsonIn.get("user_key"));
    
    long  userKey = (Long) bsonIn.get("user_key");
    long  roomKey = (Long) bsonIn.get("room_key");
    int   msgId   = (Integer) bsonIn.get("msg_id");

    String message = (String) bsonIn.get("message");

    final BSONObject bsonOut = new BasicBSONObject();
    bsonOut.put("type", "response");
    bsonOut.put("room_key", roomKey);
    bsonOut.put("user_key", userKey);
    bsonOut.put("msg_id", msgId);
    bsonOut.put("message", "1:" + message);
    List<String> msgs = new ArrayList<String>();
    msgs.add("2:" + message);
    msgs.add("3:" +(new StringBuffer(message)).reverse().toString());

    if ( delay > 0 ) {
      timer.newTimeout( new TimerTask() {
        public void run(Timeout timeout) throws Exception {
          LOG.debug("OUT => " + bsonOut);
          channel.write(bsonOut);
        }
      }, delay, TimeUnit.MILLISECONDS);
    } else {
      LOG.debug("OUT => " + bsonOut);
      channel.write(bsonOut);
    }
	}

  protected void handleAdd(TYPE type, final Channel channel, BSONObject bsonIn) {
    LOG.debug("ADD user(%d) ", (Long) bsonIn.get("user_key"));

    long userKey = (Long) bsonIn.get("user_key");
    long roomKey = (Long) bsonIn.get("room_key");
    int msgId = (Integer) bsonIn.get("msg_id");
    BSONObject bOut = new BasicBSONObject();
    bOut.put("type", "response");
    bOut.put("room_key", roomKey);
    bOut.put("user_key", userKey);
    bOut.put("msg_id", msgId);
    bOut.put("message", "thank you for add me.");

    channel.write(bOut);
    LOG.debug("LOGIN user(%d) ", userKey);
  }

  protected void handleBlock(TYPE type, final Channel channel, BSONObject bsonIn) {
    LOG.debug("BLOCKED user(%d) ", (Long) bsonIn.get("user_key"));
  }

  protected void handleLeave(TYPE type, final Channel channel, BSONObject bsonIn) {
    LOG.debug("LEAVE user(%d) ", (Long) bsonIn.get("user_key"));
  }

  protected void handleResult(TYPE type, final Channel channel, BSONObject bsonIn) {
    LOG.debug("Result : " + bsonIn);
  }
}
