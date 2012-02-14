package com.kakao.bot.echobot;

import org.bson.BSON;
import org.bson.BSONObject;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

public class BsonEncoder extends OneToOneEncoder {

  @Override
  protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
    if (msg instanceof byte[]) {
      ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
      buffer.writeBytes((byte[]) msg);
      
      return buffer;
    } else if (msg instanceof BSONObject) {
      System.out.println("encode:BSONObject");
      ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
      buffer.writeBytes(BSON.encode((BSONObject) msg));
      
      return buffer;
    } else {
      System.out.println(this.getClass().getName() + " : unknown type." + msg);
    }
    return null;
  }

}
