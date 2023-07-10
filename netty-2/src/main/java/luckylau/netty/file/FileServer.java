package luckylau.netty.file;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;

import java.io.File;
import java.io.RandomAccessFile;

import static io.netty.handler.codec.http.HttpConstants.CR;

/**
 * @Author luckylau
 * @Date 2019/9/5
 */
public class FileServer {
    public static void main(String[] args) throws Exception {
        int port = 8080;
        NettyServer nettyServer = new NettyServer(port);
        nettyServer.start();

    }

    static class NettyServer {
        private int port;

        public NettyServer(int port) {
            this.port = port;
        }

        private void run(final int port) throws Exception {
            EventLoopGroup bossGroup = new NioEventLoopGroup();
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            try {
                ServerBootstrap b = new ServerBootstrap();
                b.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new ChildChannelHandler());
                // 绑定端口，同步等待成功
                ChannelFuture future = b.bind(port).sync();
                future.channel().closeFuture().sync();

            } finally {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        }

        public void start() throws Exception {
            System.out.println("The FileServer is start in port : " + port);
            run(port);
        }

        private class ChildChannelHandler extends ChannelInitializer<SocketChannel> {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(
                        new StringEncoder(CharsetUtil.UTF_8),
                        new LineBasedFrameDecoder(1024),
                        new StringDecoder(CharsetUtil.UTF_8),
                        new FileServerHandler());
            }

        }

        static class FileServerHandler extends SimpleChannelInboundHandler<String> {

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
                    throws Exception {
                cause.printStackTrace();
                ctx.close();
            }

            @Override
            protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
                File file = new File(msg);
                if (file.exists()) {
                    if (!file.isFile()) {
                        ctx.writeAndFlush("Not a file : " + file + CR);
                        return;
                    }
                    ctx.write(file + " " + file.length() + CR);
                    RandomAccessFile randomAccessFile = new RandomAccessFile(msg, "r");
                    FileRegion region = new DefaultFileRegion(
                            randomAccessFile.getChannel(), 0, randomAccessFile.length());
                    ctx.write(region);
                    ctx.writeAndFlush(CR);
                    randomAccessFile.close();
                } else {
                    ctx.writeAndFlush("File not found: " + file + CR);
                }
            }
        }
    }
}
