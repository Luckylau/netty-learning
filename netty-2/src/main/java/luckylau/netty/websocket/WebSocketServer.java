package luckylau.netty.websocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.CharsetUtil;

import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @Author luckylau
 * @Date 2019/9/4
 */
public class WebSocketServer {

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
                Channel ch = b.bind(port).sync().channel();
                ch.closeFuture().sync();

            } finally {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        }

        public void start() throws Exception {
            System.out.println("Web socket server started at port " + port + '.');
            run(port);
        }

        private class ChildChannelHandler extends ChannelInitializer<SocketChannel> {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast("http-codec",
                        new HttpServerCodec());
                pipeline.addLast("aggregator",
                        new HttpObjectAggregator(65536));
                ch.pipeline().addLast("http-chunked",
                        new ChunkedWriteHandler());
                pipeline.addLast("handler",
                        new WebSocketServerHandler());

            }

        }

        static class WebSocketServerHandler extends SimpleChannelInboundHandler<Object> {

            private WebSocketServerHandshaker handshaker;

            @Override
            public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
                ctx.flush();
            }

            private void handleHttpRequest(ChannelHandlerContext ctx,
                                           FullHttpRequest req) throws Exception {

                // 如果HTTP解码失败，返回HTTP异常
                if (!req.decoderResult().isSuccess()
                        || (!"websocket".equals(req.headers().get("Upgrade")))) {
                    sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1,
                            BAD_REQUEST));
                    return;
                }

                // 构造握手响应返回，本机测试
                WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                        "ws://localhost:8080/websocket", null, false);
                handshaker = wsFactory.newHandshaker(req);
                if (handshaker == null) {
                    WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
                } else {
                    handshaker.handshake(ctx.channel(), req);
                }
            }

            private void handleWebSocketFrame(ChannelHandlerContext ctx,
                                              WebSocketFrame frame) {

                // 判断是否是关闭链路的指令
                if (frame instanceof CloseWebSocketFrame) {
                    handshaker.close(ctx.channel(),
                            (CloseWebSocketFrame) frame.retain());
                    return;
                }
                // 判断是否是Ping消息
                if (frame instanceof PingWebSocketFrame) {
                    ctx.channel().write(
                            new PongWebSocketFrame(frame.content().retain()));
                    return;
                }
                // 本例程仅支持文本消息，不支持二进制消息
                if (!(frame instanceof TextWebSocketFrame)) {
                    throw new UnsupportedOperationException(String.format(
                            "%s frame types not supported", frame.getClass().getName()));
                }

                // 返回应答消息
                String request = ((TextWebSocketFrame) frame).text();
                System.out.printf("%s received %s%n", ctx.channel(), request);
                ctx.channel().write(
                        new TextWebSocketFrame(request
                                + " , 欢迎使用Netty WebSocket服务，现在时刻："
                                + new java.util.Date().toString()));
            }

            private void sendHttpResponse(ChannelHandlerContext ctx,
                                          FullHttpRequest req, FullHttpResponse res) {
                // 返回应答给客户端
                if (res.status().code() != 200) {
                    ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(),
                            CharsetUtil.UTF_8);
                    res.content().writeBytes(buf);
                    buf.release();
                    setContentLength(res, res.content().readableBytes());
                }

                // 如果是非Keep-Alive，关闭连接
                ChannelFuture f = ctx.channel().writeAndFlush(res);
                if (!HttpUtil.isKeepAlive(req) || res.status().code() != 200) {
                    f.addListener(ChannelFutureListener.CLOSE);
                }
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
                    throws Exception {
                cause.printStackTrace();
                ctx.close();
            }

            @Override
            protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
                // 传统的HTTP接入
                if (msg instanceof FullHttpRequest) {
                    handleHttpRequest(ctx, (FullHttpRequest) msg);
                }
                // WebSocket接入
                else if (msg instanceof WebSocketFrame) {
                    handleWebSocketFrame(ctx, (WebSocketFrame) msg);
                }
            }
        }
    }
}
