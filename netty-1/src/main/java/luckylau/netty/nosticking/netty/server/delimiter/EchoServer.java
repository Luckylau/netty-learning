package luckylau.netty.nosticking.netty.server.delimiter;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
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
            System.out.println("The no Sticking Netty Echo Server is start in port : " + port);
            bind(port);
        }

        private class ChildChannelHandler extends ChannelInitializer<SocketChannel> {
            @Override
            protected void initChannel(SocketChannel arg0) throws Exception {
                ByteBuf delimiter = Unpooled.copiedBuffer("$_"
                        .getBytes());
                arg0.pipeline().addLast(
                        new DelimiterBasedFrameDecoder(1024,
                                delimiter));
                arg0.pipeline().addLast(new StringDecoder());
                arg0.pipeline().addLast(new EchoServerHandler());
            }

        }

        class EchoServerHandler extends ChannelHandlerAdapter {
            private int counter;

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg)
                    throws Exception {
                String body = (String) msg;
                System.out.println("This is " + ++counter + " times receive client : ["
                        + body + "]");
                body += "$_";
                ByteBuf echo = Unpooled.copiedBuffer(body.getBytes());
                ctx.writeAndFlush(echo);
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
