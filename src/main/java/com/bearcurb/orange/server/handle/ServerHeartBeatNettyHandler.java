package com.bearcurb.orange.server.handle;

import com.bearcurb.orange.common.protocol.Procotol;
import com.bearcurb.orange.server.util.ServerProtocolGenerator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class ServerHeartBeatNettyHandler extends SimpleChannelInboundHandler<Procotol> {
  private int idleReaderTriggernumber = 0;

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Procotol msg) throws Exception {
    System.out.println(msg.getEvent());
    if (msg.getEvent() != Procotol.EventType.HEART) {
      ctx.fireChannelRead(msg);
      return;
    }
    idleReaderTriggernumber = 0;
    //收到的是心跳包
    //发送心跳回复
    Procotol protocol = ServerProtocolGenerator.getSimpleResultProtocol();
    protocol.setEvent(Procotol.EventType.HEART);
    System.out.println("心跳");
    ctx.writeAndFlush(protocol);
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt instanceof IdleStateEvent) {
      if (((IdleStateEvent) evt).state() == IdleState.READER_IDLE) {
        // long time no request
        idleReaderTriggernumber++;
        if (idleReaderTriggernumber > 3) {
          System.out.println("超时三次 断开");
          ctx.close();
        }
      } else {
        super.userEventTriggered(ctx, evt);
      }
    }
  }
}