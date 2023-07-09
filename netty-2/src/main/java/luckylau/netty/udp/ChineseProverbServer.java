package luckylau.netty.udp;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.CharsetUtil;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @Author luckylau
 * @Date 2019/9/5
 */
public class ChineseProverbServer {

    public static void main(String[] args) throws Exception {
        int port = 6666;
        NettyServer nettyServer = new NettyServer(port);
        nettyServer.start();

    }

    static class NettyServer {
        private int port;

        private NettyServer(int port) {
            this.port = port;
        }

        private void run(final int port) throws Exception {
            EventLoopGroup group = new NioEventLoopGroup();
            try {
                Bootstrap b = new Bootstrap();
                b.group(group).channel(NioDatagramChannel.class)
                        .option(ChannelOption.SO_BROADCAST, true)
                        .handler(new ChineseProverbServerHandler());
                b.bind(port).sync().channel().closeFuture().await();
            } finally {
                group.shutdownGracefully();
            }
        }

        private void start() throws Exception {
            System.out.println("Chinese Proverb Server started at port " + port + '.');
            run(port);
        }


        class ChineseProverbServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {

            /**
             * 谚语列表
             */
            private final String[] DICTIONARY = {"只要功夫深，铁棒磨成针。",
                    "旧时王谢堂前燕，飞入寻常百姓家。", "洛阳亲友如相问，一片冰心在玉壶。", "一寸光阴一寸金，寸金难买寸光阴。",
                    "老骥伏枥，志在千里。烈士暮年，壮心不已!"};

            private String nextQuote() {
                int quoteId = ThreadLocalRandom.current().nextInt(DICTIONARY.length);
                return DICTIONARY[quoteId];
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
                    throws Exception {
                ctx.close();
                cause.printStackTrace();
            }

            @Override
            protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
                String req = packet.content().toString(CharsetUtil.UTF_8);
                System.out.println(req);
                if ("谚语字典查询?".equals(req)) {
                    ctx.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(
                            "谚语查询结果: " + nextQuote(), CharsetUtil.UTF_8), packet
                            .sender()));
                }
            }
        }
    }
}
