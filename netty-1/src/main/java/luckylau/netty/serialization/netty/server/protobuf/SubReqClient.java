/*
package luckylau.netty.serialization.netty.server.protobuf;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import luckylau.netty.serialization.netty.server.pojo.SubscribeReq;

*/
/**
 * @Author luckylau
 * @Date 2019/9/2
 * <p>
 * Creates a client-side handler.
 *//*

public class SubReqClient {
    public static void main(String[] args) throws Exception{
        int port =6666;
        NettyClient client = new NettyClient("127.0.0.1", port);
        client.start();

    }

    static class NettyClient {
        private int port ;
        private String host;

        public NettyClient(String host, int port) {
            this.port = port;
            this.host = host;
        }

        public void start() throws Exception{
            connect(host, port);
        }

        private void connect(String host, int port) throws Exception{
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
                                        new ProtobufVarint32FrameDecoder());
                                ch.pipeline().addLast(
                                        new ProtobufDecoder(
                                                SubscribeRespProto.SubscribeResp
                                                        .getDefaultInstance()));
                                ch.pipeline().addLast(
                                        new ProtobufVarint32LengthFieldPrepender());
                                ch.pipeline().addLast(new ProtobufEncoder());
                                ch.pipeline().addLast(new SubReqClientHandler());
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

        private class SubReqClientHandler extends ChannelHandlerAdapter {

            */
/**
 * Creates a client-side handler.
 *//*

            public SubReqClientHandler() {
            }

            @Override
            public void channelActive(ChannelHandlerContext ctx) {
                for (int i = 0; i < 10; i++) {
                    ctx.write(subReq(i));
                }
                ctx.flush();
            }

            private SubscribeReq subReq(int i) {
                SubscribeReq req = new SubscribeReq();
                req.setAddress("北京市海淀区");
                req.setEmail("laujunbupt0913@163.com");
                req.setProductName("Netty-Learn");
                req.setSubReqID(i);
                req.setUserName("Luckylau");
                return req;
            }


            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg)
                    throws Exception {
                System.out.println("Receive server response : [" + msg + "]");

            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                // 释放资源
                System.out.println("Unexpected exception from downstream : "
                        + cause.getMessage());
                ctx.close();
            }

            @Override
            public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
                ctx.flush();
            }


        }


    }

}
*/
