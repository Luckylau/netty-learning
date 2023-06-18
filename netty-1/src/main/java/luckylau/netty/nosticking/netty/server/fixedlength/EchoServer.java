package luckylau.netty.nosticking.netty.server.fixedlength;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * @Author luckylau
 * @Date 2019/9/2
 */
public class EchoServer {

    public static void main(String[] args) throws Exception {
        int port = 6666;
        NoStickingNettyEchoServer noStickingNettyEchoServer = new NoStickingNettyEchoServer(port);
        noStickingNettyEchoServer.start();

    }

    static class NoStickingNettyEchoServer {
        private int port;

        public NoStickingNettyEchoServer(int port) {
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
            System.out.println("The no sticking netty fixed length server is start in port : " + port);
            bind(port);
        }

        private class ChildChannelHandler extends ChannelInitializer<SocketChannel> {
            @Override
            protected void initChannel(SocketChannel arg0) throws Exception {
                arg0.pipeline().addLast(
                        new FixedLengthFrameDecoder(20));
                arg0.pipeline().addLast(new StringDecoder());
                arg0.pipeline().addLast(new EchoServerHandler());
            }

        }

        class EchoServerHandler extends ChannelInboundHandlerAdapter {

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg)
                    throws Exception {
                System.out.println("Receive client : [" + msg + "]");
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
