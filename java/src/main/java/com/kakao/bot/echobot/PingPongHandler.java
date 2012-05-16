package com.kakao.bot.echobot;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.Timer;
import org.jboss.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PingPongHandler extends SimpleChannelUpstreamHandler
{
  private static final Logger LOG = LoggerFactory.getLogger(PingPongHandler.class);
  private static final long INIT_TIME = -1;

  final ClientBootstrap bootstrap;
  private final Timer timer;
  private final int pingInterval;
  private long pingTime = INIT_TIME;
  private long pongTime = INIT_TIME;

  public PingPongHandler(ClientBootstrap bootstrap, Timer timer, int interval) {
    this.bootstrap = bootstrap;
    this.timer = timer;
    this.pingInterval = interval;
  }

  private InetSocketAddress getRemoteAddress() {
    return (InetSocketAddress) bootstrap.getOption("remoteAddress");
  }


  public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
      throws Exception
  {
    Object obj = e.getMessage();

    BSONObject pong = null;
    if ( !(obj instanceof BSONObject) ) {
      LOG.trace("obj isn't type of DomainObject, obj:{}",obj);
      ctx.sendUpstream(e);
      return;
    }
    pong = (BSONObject)obj;
    
    // Pong Type 인지 체크함
    if ( !pong.get("type").equals("pong") ) {
      LOG.trace("obj isn't type of Pong, obj:{}",obj);
      ctx.sendUpstream(e);
      return;
    }
    
    // Pong에 대해서는 response를 줄 필요가 없음
    pongTime = (Long) pong.get("time");
    
    LOG.trace("Pong replied, pong:{}, pingTime:", pong, pingTime);
  }

  @Override
  public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
    LOG.info("Connected to: {}",getRemoteAddress());
    ctx.sendUpstream(e);
    if ( pingInterval > 0 ) {
      timer.newTimeout( new PingPongSchedule (e), pingInterval, TimeUnit.MILLISECONDS);
    }
  }
  
  private void resetTime () {
    this.pingTime = INIT_TIME;
    this.pongTime = INIT_TIME;
  }
  
  
  class PingPongSchedule implements TimerTask {
    final ChannelStateEvent e;
    PingPongSchedule (ChannelStateEvent e) {
      this.e = e;
    }
    public void run(Timeout timeout) {
      
      /**
       * 이전 ping시간과 pong시간을 체크
      * - ping과 pong이 다르면 응답을 안준 케이스임  => 재접속
       */
      if ( pingTime != pongTime ) {
         LOG.warn("ping/pong failed, ping:"+pingTime+", pong:"+pongTime+" ===> doing closed()");
         e.getChannel().close();
         
         resetTime ();
         return;
      }
      
      pingTime = System.currentTimeMillis();
      BSONObject ping = toPing(pingTime);
      e.getChannel().write(ping);
      LOG.debug("[send] ping:{}"+ping);
      
      if ( pingInterval > 0 ) {
        timer.newTimeout( new PingPongSchedule (e), pingInterval, TimeUnit.MILLISECONDS);
      }
    }
    
    private final BSONObject toPing (long pingTime) {
      BSONObject bson = new BasicBSONObject ();
      bson.put("type", "ping");
      bson.put("time", pingTime);
      return bson;
    }
  }



}
