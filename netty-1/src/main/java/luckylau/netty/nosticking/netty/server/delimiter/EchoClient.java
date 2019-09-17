package luckylau.netty.nosticking.netty.server.delimiter;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

/**
 * @Author luckylau
 * @Date 2019/9/2
 */
public class EchoClient {

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
                                ByteBuf delimiter = Unpooled.copiedBuffer("$_"
                                        .getBytes());
                                ch.pipeline().addLast(
                                        new DelimiterBasedFrameDecoder(1024,
                                                delimiter));
                                ch.pipeline().addLast(new StringDecoder());
                                ch.pipeline().addLast(new EchoClientHandler());
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

        private class EchoClientHandler extends ChannelHandlerAdapter {
            static final String ECHO_REQ = "Hi, luckylau. Welcome to Netty.$_";
            private int counter;

            /**
             * Creates a client-side handler.
             */
            public EchoClientHandler() {
            }

            @Override
            public void channelActive(ChannelHandlerContext ctx) {
                for (int i = 0; i < 20; i++) {
                    ctx.writeAndFlush(Unpooled.copiedBuffer(ECHO_REQ.getBytes()));
                }
            }

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg)
                    throws Exception {
                System.out.println("This is " + ++counter + " times receive server : ["
                        + msg + "]");
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
