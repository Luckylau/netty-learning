package luckylau.netty.serialization.netty.server.marshalling;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import luckylau.netty.serialization.netty.server.pojo.SubscribeReq;
import luckylau.netty.serialization.netty.server.pojo.SubscribeResp;

/**
 * @Author luckylau
 * @Date 2019/9/2
 */
public class SubReqServer {
    public static void main(String[] args) throws Exception {
        int port = 6666;
        NettyServer nettyServer = new NettyServer(port);
        nettyServer.start();

    }

    static class NettyServer {
        private int port;

        public NettyServer(int port) {
            this.port = port;
        }

        private void bind(int port) throws Exception {
            EventLoopGroup bossGroup = new NioEventLoopGroup();
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            try {
                ServerBootstrap b = new ServerBootstrap();
                b.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .option(ChannelOption.SO_BACKLOG, 100)
                        .handler(new LoggingHandler(LogLevel.INFO))
                        .childHandler(new ChildChannelHandler());
                // 绑定端口，同步等待成功
                ChannelFuture f = b.bind(port).sync();

                f.channel().closeFuture().sync();

            } finally {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        }

        public void start() throws Exception {
            System.out.println("The SubReqServer is start in port : " + port);
            bind(port);
        }

        private class ChildChannelHandler extends ChannelInitializer<SocketChannel> {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(
                        MarshallingCodeCFactory
                                .buildMarshallingDecoder());
                ch.pipeline().addLast(
                        MarshallingCodeCFactory
                                .buildMarshallingEncoder());
                ch.pipeline().addLast(new SubReqServerHandler());
            }

        }

        class SubReqServerHandler extends ChannelInboundHandlerAdapter {

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg)
                    throws Exception {
                SubscribeReq req = (SubscribeReq) msg;
                if ("Luckylau".equalsIgnoreCase(req.getUserName())) {
                    System.out.println("Service accept client req : ["
                            + req.toString() + "]");
                    ctx.writeAndFlush(resp(req.getSubReqID()));
                }
            }

            private SubscribeResp resp(int subReqID) {
                SubscribeResp resp = new SubscribeResp();
                resp.setSubReqID(subReqID);
                resp.setRespCode(0);
                resp.setDesc("Thank you !");
                return resp;
            }

            @Override
            public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
                ctx.flush();
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                ctx.close();
            }
        }

    }
}
