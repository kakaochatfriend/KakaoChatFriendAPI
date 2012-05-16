package com.kakao.bot.echobot;

import org.bson.BSON;
import org.bson.BSONObject;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BsonEncoder extends OneToOneEncoder {
  private static final Logger LOG = LoggerFactory.getLogger(BsonEncoder.class);

  @Override
  protected Object encode(ChannelHandlerContext ctx, Channel channel, Object obj) throws Exception {
    ChannelBuffer buffer =  null;
    
    // null 인경우에는 DownStream으로 전달이 안됨
    if ( !( obj instanceof BSONObject ) ) {
      LOG.warn ("obj isn't BSONObject, "+obj);
      return null;
    }
    BSONObject bsonOut = (BSONObject) obj;
    byte[] baOut = null;
    try {
      buffer = ChannelBuffers.dynamicBuffer();
      baOut = BSON.encode(bsonOut);
      buffer.writeBytes( baOut );
    }catch (Exception e) {
      LOG.warn("exception occurred by"+obj, e);
    }
    return buffer;
  }

}
