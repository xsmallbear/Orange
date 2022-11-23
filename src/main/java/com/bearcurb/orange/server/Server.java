package com.bearcurb.orange.server;

import com.bearcurb.orange.protocol.handle.OrangeProtocolCodec;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.util.List;

public class Server implements IServer {

  private String host;
  private int port;
  private Channel serverChannel;
  private ServerHandle serverHandler = new ServerHandle();
  private EventLoopGroup bossGroup = new NioEventLoopGroup();
  private EventLoopGroup workerGroup = new NioEventLoopGroup();
  private ServerBootstrap serverBootstrap = new ServerBootstrap();

  public Server(String host, int port) {
    this.host = host;
    this.port = port;
    init();
  }

  public void addService(String serviceName, IService handle) {
    ServiceManager.getInstance().registerService(serviceName, handle);
  }

  public void addIntercept(List<String> excludes, IIntercept intercept) {
    ServiceManager.getInstance().registerIntercept(excludes, intercept);
  }

  public void init() {
    serverBootstrap.group(bossGroup, workerGroup)
      .channel(NioServerSocketChannel.class)
      .childHandler(new ChannelInitializer<SocketChannel>() {
        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
          ChannelPipeline pipeline = ch.pipeline();
//          pipeline.addLast(new IdleStateHandler(8, 0, 0, TimeUnit.SECONDS));
//          pipeline.addLast(new HeartBeatServerHandler());
          pipeline.addLast(new LineBasedFrameDecoder(1024));
          pipeline.addLast(new StringDecoder());
          pipeline.addLast(new StringEncoder());
          pipeline.addLast(new OrangeProtocolCodec());
          pipeline.addLast(serverHandler);
        }
      });
  }

  @Override
  public void start() throws InterruptedException {
    ChannelFuture future = serverBootstrap.bind(host, port).sync();
    serverChannel = future.channel();
  }

  @Override
  public void stop() throws InterruptedException {
    if (serverChannel != null) {
      serverChannel.close();
      serverChannel = null;
      bossGroup.shutdownGracefully().sync();
      workerGroup.shutdownGracefully().sync();
    }
  }
}
