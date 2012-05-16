package com.kakao.bot.echobot;

import org.bson.BSON;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BsonDecoder extends FrameDecoder {

  private static final Logger LOG = LoggerFactory.getLogger(BsonDecoder.class);
  private static final int MAXDATASIZE = 10*1024;     // 10KB가 넘으면 illegal data로 인식함
  

  @Override
  protected Object decode(ChannelHandlerContext channelhandlercontext,
      Channel channel, ChannelBuffer channelbuffer) throws Exception {
    try {
      byte[] bytes = decodeToByteArray (channelhandlercontext, channel, channelbuffer);
      
      // bytes가 null 이면 이것은 illegal 접근으로 판단함, UpStream으로 data를 전달하지 않음
      if ( bytes == null ) {
        return null;
      }
      return BSON.decode(bytes);
    } catch (Exception e) {
      LOG.warn("exception occurred during decoding process", e);
      return null;
    }
  }
  
  protected byte[] decodeToByteArray (ChannelHandlerContext channelhandlercontext,
      Channel channel, ChannelBuffer channelbuffer) {

    int readable = channelbuffer.readableBytes();
    // channelbuffer에 readable size에 대한 validation check
    if (readable < 5) {
      LOG.warn("readable byte is too small!!, must be over 5 bytes, readable:"+readable);
      return null;
    }

    /**
     * BSON은 ByteOrder가 little-endian임, java는 big-endian이기 때문에 변환할 필요가 있음
     *  http://bsonspec.org/#/specification
     */
    channelbuffer.markReaderIndex();
    int size = Integer.reverseBytes(channelbuffer.getInt(channelbuffer.readerIndex()));
    // size가 0보다 작음              => overflow 된 경우임
    // size가 maxHeapSize보다 큰 경우 => flooding 상태
    if (size < 0 || size > MAXDATASIZE) {
      LOG.warn("size invalid!! size:"+size+", max:"+MAXDATASIZE + ", readable:"+readable);
      return null;
    }

    // size가 readable 보다 큰 경우!!
    if (size > readable) {
      // The whole bytes were not received yet - return null
      // This method will be invoked again when more packets are
      // received and appended to the buffer.
      
      // Reset to the marked position to read the length field again
      // next time
      channelbuffer.resetReaderIndex();
      return null;
    }
    
    byte[] bt = new byte[size];
    channelbuffer.readBytes(bt);
    return bt;
  }
}
