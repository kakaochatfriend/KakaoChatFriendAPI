package com.kakao.bot.echobot;

import java.util.Random;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StressTestServerHandler extends SimpleChannelUpstreamHandler {
  private static final Logger LOG = LoggerFactory.getLogger(StressTestServerHandler.class);

  private final AtomicLong sendCnt = new AtomicLong ();
  private final AtomicLong recvCnt = new AtomicLong ();
  private final AtomicLong sendCntPS = new AtomicLong ();
  private final AtomicLong recvCntPS = new AtomicLong ();

  private final AtomicInteger executeTime = new AtomicInteger ();

  private final int requestPerSeconds;
  private final int testTime;
  private long start = 0;

  private boolean first = true;
  private static boolean useDetailLog = false;

  ScheduledExecutorService timer = Executors.newScheduledThreadPool(1);
  ScheduledFuture<?> timerFuture = null;


  public StressTestServerHandler (int requestPerSeconds, int testTime) {
    LOG.info("BotStressRandomTestHandler start, rps:"+requestPerSeconds+", time:"+testTime);
    this.requestPerSeconds = requestPerSeconds;
    this.testTime = testTime;
  }

  @Override
  public void messageReceived(ChannelHandlerContext ctx,
      final MessageEvent e) {
    Object obj = e.getMessage();
    BSONObject bsonIn = null;
    BSONObject bsonOut = null;
    do {
      if ( !( obj instanceof BSONObject) ) {
        LOG.info("obj isn't type of BSONObject, obj:"+obj);
        break;
      }
      bsonIn = (BSONObject)obj;
      if ( useDetailLog ) {
        LOG.info("BsonIn = " + bsonIn);
      }

      String type = (String) bsonIn.get("type");
      if (type == null) {
        LOG.info("null");
      } else if (type.equals("request")) {
        LOG.debug("request");
      } else if (type.equals("response")) {
        recvCnt.incrementAndGet();
        recvCntPS.incrementAndGet();
        sendResult (e);
      } else if (type.equals("login")) {
        LOG.debug("login ");
        sendResult (e);
        startRandomTest (e);
      } else if (type.equals("result")) {
        LOG.debug("result ");
      } else if (type.equals("add")) {
        LOG.debug("add");
        sendResult ( e );
      } else if ( type.equals("ping")) {
        LOG.debug("ping:{}", bsonIn);
        bsonOut = new BasicBSONObject();
        bsonOut.put("type", "pong");
        bsonOut.put("time", bsonIn.get("time"));
        e.getChannel().write(bsonOut);
      }
    } while(false);
  }

  private void sendResult (final MessageEvent e) {
    BSONObject bOut = new BasicBSONObject();
    bOut.put("type", "result");
    bOut.put("code", 200);
    bOut.put("msg_id", 0 );
    bOut.put("msg", "테스트" );
    if ( useDetailLog ) {
      LOG.info("BsonOut = " + bOut );
    }
    e.getChannel().write(bOut);
  }

  private void startRandomTest (final MessageEvent e) {
    if ( !first )  return;
    start = System.currentTimeMillis();
    first = false;

    final Random rand = new Random (System.currentTimeMillis());

    class MyWorker extends TimerTask {
      public void run() {
        try {
          if ( executeTime.getAndIncrement() >= testTime ) {
            LOG.info("[stop]"+getStatistics() );
            return;
          } 

          for ( int i = 0; i < requestPerSeconds; i++ ) {
            BSONObject bOut = new BasicBSONObject();
            int msgId = Math.abs( rand.nextInt() % 10000);
            bOut.put("type", "request");
            bOut.put("user_key", Math.abs( rand.nextLong() % 10000) );
            bOut.put("room_key", Math.abs( rand.nextLong() % 10000) );
            bOut.put("msg_id",   msgId );
            bOut.put("country_iso", "KR");
            bOut.put("message", getMessage(msgId) );
            if ( useDetailLog ) {
              LOG.info("BsonOut = " + bOut );
            }
            e.getChannel().write( bOut );
            sendCnt.incrementAndGet();
            sendCntPS.incrementAndGet();
          }
          LOG.info("[send]"+getStatistics() );
        } finally {
          sendCntPS.set(0);
          recvCntPS.set(0);
        }
      }
    };

    if (timerFuture != null) {
      //cancel execution of the future task (TimerPopTask())
      //If task is already running, do not interrupt it.
      timerFuture.cancel(false);
    }
    timerFuture = timer.scheduleAtFixedRate( new MyWorker(), 0, 1, TimeUnit.SECONDS);
  }

  private String getStatistics () {
    return "send:("+ sendCnt.intValue()+", "+sendCntPS.intValue()+"), receive:("+recvCnt.intValue()+", "+recvCntPS.intValue()+"), execute:("+executeTime.intValue()+"/"+ testTime  +"), ts:"+getDuration();
  }
  
  private String getMessage (int msgId) {
    return "테스트, msgId:"+msgId;
  }
  
  private long getDuration () {
    return System.currentTimeMillis() - start;
  }

  @Override
  public void channelConnected(ChannelHandlerContext ctx,
      ChannelStateEvent e) {
    LOG.info("connected.");
  }

  @Override
  public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
    LOG.info("connection closed.");
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
    e.getCause().printStackTrace();
    e.getChannel().close();
  }

}