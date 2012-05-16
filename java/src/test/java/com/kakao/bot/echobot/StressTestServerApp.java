/**
 * 
 */
package com.kakao.bot.echobot;

import static org.jboss.netty.channel.Channels.pipeline;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author hypermin
 *
 */
public class StressTestServerApp
{
  private static final Logger LOG = LoggerFactory.getLogger(StressTestServerApp.class);
  private static int port = 11111;
  private static int requestPerSeconds  = 10000;
  private static int testTime = 20;
  
  public static void main(String[] args) throws Exception {
    LOG.info("StressTestServer start");
    if (args.length > 1) {
      port = Integer.parseInt(args[0]);
      LOG.info("port:{}",port);
    }
    if ( args.length == 3) {
      requestPerSeconds = Integer.parseInt(args[1]);
      testTime = Integer.parseInt(args[2]);
      LOG.info("rps:{}, testTime:{}",requestPerSeconds, testTime);
    }
    startServer (port, requestPerSeconds, testTime);
  }
  
  private static void startServer (final int port, final int rps, final int tt) {
    LOG.info("port:{}, rps:{}, testTime:{}",new Object[]{port, rps, tt});
    
    // Configure the server.
    ServerBootstrap bootstrap = new ServerBootstrap(
        new NioServerSocketChannelFactory(
            Executors.newCachedThreadPool(),
            Executors.newCachedThreadPool()));

    // Set up the pipeline factory.
    bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
      public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = pipeline();
        pipeline.addLast("bsonDecoder",   new BsonDecoder());     // 
        pipeline.addLast("bsonEncoder",   new BsonEncoder());
        pipeline.addLast("handler",       new StressTestServerHandler( rps, tt ));
        return pipeline;
      }
    });

    // Bind and start to accept incoming connections.
    InetSocketAddress address = new InetSocketAddress( port ); 
    bootstrap.bind(address);
    System.out.println("bind. address:"+address);
    
  }
}
