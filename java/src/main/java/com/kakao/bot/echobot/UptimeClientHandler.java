package com.kakao.bot.echobot;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.timeout.ReadTimeoutException;
import org.jboss.netty.handler.timeout.WriteTimeoutException;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.Timer;
import org.jboss.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UptimeClientHandler extends SimpleChannelUpstreamHandler
{
  private static final Logger LOG = LoggerFactory.getLogger(UptimeClientHandler.class);
  final ClientBootstrap bootstrap;
  private final Timer timer;
  private final int reconnectionDelay;
  private long startTime = -1;

  public UptimeClientHandler(ClientBootstrap bootstrap, Timer timer, int reconnectionDelay) {
    this.bootstrap = bootstrap;
    this.timer = timer;
    this.reconnectionDelay = reconnectionDelay;
  }

  InetSocketAddress getRemoteAddress() {
    return (InetSocketAddress) bootstrap.getOption("remoteAddress");
  }

  @Override
  public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
    if ( LOG.isInfoEnabled() ) {
      LOG.info(getMsgHead()+"Disconnected from: {}", getRemoteAddress());
    }
    ctx.sendUpstream(e);
  }

  @Override
  public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) {
    if (LOG.isInfoEnabled()) {
      LOG.info(getMsgHead()+"Sleeping for: {} ms", reconnectionDelay);
    }

    timer.newTimeout( new TimerTask() {
      public void run(Timeout timeout) throws Exception {
        LOG.warn(getMsgHead()+"Reconnecting to: {}, reconnection delay:{}",getRemoteAddress(), reconnectionDelay);
        bootstrap.connect();
      }
    }, reconnectionDelay, TimeUnit.MILLISECONDS);
    ctx.sendUpstream(e);
  }

  @Override
  public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
    if (startTime < 0) {
      startTime = System.currentTimeMillis();
    }

    if (LOG.isInfoEnabled()) {
      LOG.info(getMsgHead()+"Connected to: {}",getRemoteAddress());
    }
    ctx.sendUpstream(e);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
    Throwable cause = e.getCause();
    LOG.warn("ctx:"+ctx+", Unexpected exception from downstream.", cause);

    if (cause instanceof ConnectException) {
      startTime = -1;
      LOG.warn(getMsgHead()+"Failed to connect: {}", cause.getMessage());
    }
    if (cause instanceof ReadTimeoutException) {
    // The connection was OK but there was no traffic for last period.
      LOG.warn(getMsgHead()+"Disconnecting due to no inbound traffic, {}",cause.getMessage());
    } else if ( cause instanceof WriteTimeoutException) {
      LOG.warn(getMsgHead()+"Disconnecting due to no outbound traffic, {}", cause.getMessage());
    } else {
      LOG.warn(getMsgHead()+"Exception occurred!!", cause);
      
    }
    e.getChannel().close();
  }

  private String getMsgHead () {
    if ( startTime < 0 ) {
      return "[SERVER IS DOWN]";
    } else {
      return String.format("[UPTIME: %5ds]", (System.currentTimeMillis() - startTime) / 1000);
    }
  }
}
