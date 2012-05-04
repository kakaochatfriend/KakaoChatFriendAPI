package com.kakao.bot.selectdb;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

public class BsonDecoder extends FrameDecoder {

  @Override
  protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer)
      throws Exception {
    int readable = buffer.readableBytes();
    if (readable < 5) {
      return null;
    }
    
    buffer.markReaderIndex();
    int size = Integer.reverseBytes(buffer.getInt(buffer.readerIndex()));
    if (size < 0 || size > 100000) {
      channel.close();
      return null;
    }
    
    if (size > readable) {
      buffer.resetReaderIndex();
      return null;
    }
    
    byte[] bt = new byte[size];
    buffer.readBytes(bt);

    return bt;
  }

}
