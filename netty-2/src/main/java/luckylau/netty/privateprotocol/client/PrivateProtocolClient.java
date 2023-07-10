package luckylau.netty.privateprotocol.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import luckylau.netty.privateprotocol.NettyConstant;
import luckylau.netty.privateprotocol.NettyMessageDecoder;
import luckylau.netty.privateprotocol.NettyMessageEncoder;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @Author luckylau
 * @Date 2019/9/6
 */
public class PrivateProtocolClient {
    public static void main(String[] args) throws Exception {
        NettyClient client = new NettyClient(NettyConstant.PORT, NettyConstant.REMOTEIP);
        client.start();

    }

    static class NettyClient {
        private final int port;

        private final String host;


        private final ScheduledExecutorService executor = Executors
                .newScheduledThreadPool(1);


        public NettyClient(int port, String host) {
            this.port = port;
            this.host = host;
        }

        public void start() throws Exception {
            connect(port, host);
        }

        public void connect(int port, String host) throws Exception {

            // 配置客户端NIO线程组
            EventLoopGroup group = new NioEventLoopGroup();
            try {
                Bootstrap b = new Bootstrap();
                b.group(group).channel(NioSocketChannel.class)
                        .option(ChannelOption.TCP_NODELAY, true)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            public void initChannel(SocketChannel ch)
                                    throws Exception {
                                ch.pipeline().addLast(
                                        new NettyMessageDecoder(1024 * 1024, 4, 4));
                                ch.pipeline().addLast("MessageEncoder",
                                        new NettyMessageEncoder());
                                ch.pipeline().addLast("readTimeoutHandler",
                                        new ReadTimeoutHandler(50));
                                ch.pipeline().addLast("LoginAuthHandler",
                                        new LoginAuthReqHandler());
                                ch.pipeline().addLast("HeartBeatHandler",
                                        new HeartBeatReqHandler());
                            }
                        });
                // 发起异步连接操作
                ChannelFuture future = b.connect(
                        new InetSocketAddress(host, port),
                        new InetSocketAddress(NettyConstant.LOCALIP,
                                NettyConstant.LOCAL_PORT)).sync();
                future.channel().closeFuture().sync();
            } finally {
                // 所有资源释放完成之后，清空资源，再次发起重连操作
                executor.execute(() -> {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                        try {
                            // 发起重连操作
                            connect(NettyConstant.PORT, NettyConstant.REMOTEIP);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }
}
