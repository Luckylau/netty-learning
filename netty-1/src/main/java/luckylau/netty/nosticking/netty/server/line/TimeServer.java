package luckylau.netty.nosticking.netty.server.line;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

/**
 * @Author luckylau
 * @Date 2019/8/30
 */
public class TimeServer {
    public static void main(String[] args) throws Exception {
        int port = 6666;
        NoStickingNettyTimeServer noStickingNettyTimeServer = new NoStickingNettyTimeServer(port);
        noStickingNettyTimeServer.start();

    }

    static class NoStickingNettyTimeServer {
        private int port;

        public NoStickingNettyTimeServer(int port) {
            this.port = port;
        }

        private void bind(int port) throws Exception {
            EventLoopGroup bossGroup = new NioEventLoopGroup();
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            try {
                ServerBootstrap b = new ServerBootstrap();
                b.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .option(ChannelOption.SO_BACKLOG, 1024)
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
            System.out.println("The no sticking netty line server is start in port : " + port);
            bind(port);
        }

        private class ChildChannelHandler extends ChannelInitializer<SocketChannel> {
            @Override
            protected void initChannel(SocketChannel arg0) throws Exception {
                arg0.pipeline().addLast(new LineBasedFrameDecoder(1024));
                arg0.pipeline().addLast(new StringDecoder());
                arg0.pipeline().addLast(new TimeServerHandler());
            }

        }

        class TimeServerHandler extends ChannelInboundHandlerAdapter {
            private int counter;

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg)
                    throws Exception {
                String body = (String) msg;
                System.out.println("The time server receive order : " + body
                        + " ; the counter is : " + ++counter);
                String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body) ? new java.util.Date(
                        System.currentTimeMillis()).toString() : "BAD ORDER";
                currentTime = currentTime + System.getProperty("line.separator");
                ByteBuf resp = Unpooled.copiedBuffer(currentTime.getBytes());
                ctx.writeAndFlush(resp);
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
