package luckylau.netty.basic.netty.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.nio.charset.StandardCharsets;

import static luckylau.netty.utils.TimeUtils.getCurrentTime;

/**
 * @Author luckylau
 * @Date 2019/8/28
 */
public class TimeServer {

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
                        .option(ChannelOption.SO_BACKLOG, 1024)
                        .childHandler(new ChildChannelHandler());

                ChannelFuture f = b.bind(port).sync();

                f.channel().closeFuture().sync();

            } finally {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        }

        public void start() throws Exception {
            System.out.println("The netty basic server is start in port : " + port);
            bind(port);
        }

        private class ChildChannelHandler extends ChannelInitializer<SocketChannel> {
            @Override
            protected void initChannel(SocketChannel arg0) throws Exception {
                arg0.pipeline().addLast(new TimeServerHandler());
            }

        }

        class TimeServerHandler extends ChannelHandlerAdapter {

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg)
                    throws Exception {
                ByteBuf buf = (ByteBuf) msg;
                byte[] req = new byte[buf.readableBytes()];
                buf.readBytes(req);
                String body = new String(req, StandardCharsets.UTF_8);
                System.out.println("The time netty basic server receive order : " + body);
                String currentTime = "ack it, " + getCurrentTime();
                ByteBuf resp = Unpooled.copiedBuffer(currentTime.getBytes());
                ctx.write(resp);
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
