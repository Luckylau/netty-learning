package luckylau.netty.basic.netty.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.nio.charset.StandardCharsets;

/**
 * @Author luckylau
 * @Date 2019/8/28
 */
public class TimeClient {

    public static void main(String[] args) throws Exception {
        int port = 6666;
        NettyClient client = new NettyClient("127.0.0.1", port);
        client.start();

    }

    static class NettyClient {
        private int port;
        private String host;

        public NettyClient(String host, int port) {
            this.port = port;
            this.host = host;
        }

        public void start() throws Exception {
            connect(host, port);
        }

        private void connect(String host, int port) throws Exception {
            EventLoopGroup group = new NioEventLoopGroup();
            try {
                Bootstrap b = new Bootstrap();
                b.group(group).channel(NioSocketChannel.class)
                        .option(ChannelOption.TCP_NODELAY, true)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            public void initChannel(SocketChannel ch)
                                    throws Exception {
                                ch.pipeline().addLast(new TimeClientHandler());
                            }
                        });

                // 发起异步连接操作
                ChannelFuture f = b.connect(host, port).sync();

                // 当代客户端链路关闭
                f.channel().closeFuture().sync();
            } finally {
                // 优雅退出，释放NIO线程组
                group.shutdownGracefully();
            }
        }

        private class TimeClientHandler extends ChannelHandlerAdapter {

            private final ByteBuf firstMessage;

            /**
             * Creates a client-side handler.
             */
            public TimeClientHandler() {
                byte[] req = "query time order".getBytes();
                firstMessage = Unpooled.buffer(req.length);
                firstMessage.writeBytes(req);

            }

            @Override
            public void channelActive(ChannelHandlerContext ctx) {
                ctx.writeAndFlush(firstMessage);
            }

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg)
                    throws Exception {
                ByteBuf buf = (ByteBuf) msg;
                byte[] req = new byte[buf.readableBytes()];
                buf.readBytes(req);
                String body = new String(req, StandardCharsets.UTF_8);
                System.out.println("Now is : " + body);
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                // 释放资源
                System.out.println("Unexpected exception from downstream : "
                        + cause.getMessage());
                ctx.close();
            }
        }


    }

}
