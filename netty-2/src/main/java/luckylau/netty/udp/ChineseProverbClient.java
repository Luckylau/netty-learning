package luckylau.netty.udp;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;

/**
 * @Author luckylau
 * @Date 2019/9/5
 */
public class ChineseProverbClient {

    public static void main(String[] args) throws Exception {
        int port = 6666;
        NettyClient client = new NettyClient(port);
        client.start();

    }

    static class NettyClient {
        private int port;

        public NettyClient(int port) {
            this.port = port;
        }

        public void start() throws Exception {
            run(port);
        }

        private void run(final int port) throws Exception {
            EventLoopGroup group = new NioEventLoopGroup();
            try {
                Bootstrap b = new Bootstrap();
                b.group(group).channel(NioDatagramChannel.class)
                        .option(ChannelOption.SO_BROADCAST, true)
                        .handler(new ChineseProverbClientHandler());
                Channel ch = b.bind(0).sync().channel();
                // 向网段内的所有机器广播UDP消息
                ch.writeAndFlush(
                        new DatagramPacket(Unpooled.copiedBuffer("谚语字典查询?",
                                CharsetUtil.UTF_8), new InetSocketAddress(
                                "255.255.255.255", port))).sync();
                if (!ch.closeFuture().await(15000)) {
                    System.out.println("查询超时!");
                }
            } finally {
                group.shutdownGracefully();
            }
        }

        private class ChineseProverbClientHandler extends
                SimpleChannelInboundHandler<DatagramPacket> {

            @Override
            public void messageReceived(ChannelHandlerContext ctx, DatagramPacket msg)
                    throws Exception {
                String response = msg.content().toString(CharsetUtil.UTF_8);
                if (response.startsWith("谚语查询结果: ")) {
                    System.out.println(response);
                    ctx.close();
                }
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
                    throws Exception {
                cause.printStackTrace();
                ctx.close();
            }
        }
    }
}
