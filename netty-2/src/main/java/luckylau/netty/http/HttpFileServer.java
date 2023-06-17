package luckylau.netty.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedFile;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.CharsetUtil;
import luckylau.netty.utils.IpUtil;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @Author luckylau
 * @Date 2019/9/2
 */
public class HttpFileServer {

    public static void main(String[] args) throws Exception {
        int port = 8080;
        String url = "/netty-2";
        NettyServer nettyServer = new NettyServer(port, url);
        nettyServer.start();

    }

    static class NettyServer {
        private int port;

        private String url;

        public NettyServer(int port, String url) {
            this.port = port;
            this.url = url;
        }

        private void run(final int port, final String url) throws Exception {
            EventLoopGroup bossGroup = new NioEventLoopGroup();
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            try {
                ServerBootstrap b = new ServerBootstrap();
                b.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new ChildChannelHandler());
                // 绑定端口，同步等待成功
                ChannelFuture future = b.bind(IpUtil.getLocalIP(), port).sync();
                System.out.println("HTTP文件目录服务器启动，网址是 : http://" + IpUtil.getLocalIP() + ":"
                        + port + url);
                future.channel().closeFuture().sync();

            } finally {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        }

        public void start() throws Exception {
            System.out.println("The HttpFileServer is start in port : " + port);
            run(port, url);
        }

        private class ChildChannelHandler extends ChannelInitializer<SocketChannel> {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast("http-decoder",
                        new HttpRequestDecoder());
                //将多个消息转换为单一的FullHttpRequest或者FullHttpResponse
                ch.pipeline().addLast("http-aggregator",
                        new HttpObjectAggregator(65536));
                ch.pipeline().addLast("http-encoder",
                        new HttpResponseEncoder());
                //支持异步发送大的码流，但不占用过多的内存，防止发送java内存溢出错误
                ch.pipeline().addLast("http-chunked",
                        new ChunkedWriteHandler());
                ch.pipeline().addLast("fileServerHandler",
                        new HttpFileServerHandler(url));

            }

        }

        class HttpFileServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

            private final String url;
            private final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");
            private final Pattern ALLOWED_FILE_NAME = Pattern
                    .compile("[A-Za-z0-9][-_A-Za-z0-9\\.]*");

            public HttpFileServerHandler(String url) {
                this.url = url;
            }

            @Override
            public void messageReceived(ChannelHandlerContext ctx,
                                        FullHttpRequest request) throws Exception {
                if (!request.getDecoderResult().isSuccess()) {
                    sendError(ctx, BAD_REQUEST);
                    return;
                }
                if (request.getMethod() != GET) {
                    sendError(ctx, METHOD_NOT_ALLOWED);
                    return;
                }
                final String uri = request.getUri();
                final String path = sanitizeUri(uri);
                if (path == null) {
                    sendError(ctx, FORBIDDEN);
                    return;
                }
                File file = new File(path);
                if (file.isHidden() || !file.exists()) {
                    sendError(ctx, NOT_FOUND);
                    return;
                }
                if (file.isDirectory()) {
                    if (uri.endsWith("/")) {
                        sendListing(ctx, file);
                    } else {
                        sendRedirect(ctx, uri + '/');
                    }
                    return;
                }
                if (!file.isFile()) {
                    sendError(ctx, FORBIDDEN);
                    return;
                }
                RandomAccessFile randomAccessFile = null;
                try {
                    // 以只读的方式打开文件
                    randomAccessFile = new RandomAccessFile(file, "r");
                } catch (FileNotFoundException fnfe) {
                    sendError(ctx, NOT_FOUND);
                    return;
                }
                long fileLength = randomAccessFile.length();
                HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
                setContentLength(response, fileLength);
                setContentTypeHeader(response, file);
                if (isKeepAlive(request)) {
                    response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
                }
                ctx.write(response);
                ChannelFuture sendFileFuture;
                sendFileFuture = ctx.write(new ChunkedFile(randomAccessFile, 0,
                        fileLength, 8192), ctx.newProgressivePromise());
                sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
                    @Override
                    public void operationProgressed(ChannelProgressiveFuture future,
                                                    long progress, long total) {
                        if (total < 0) {
                            // total unknown
                            System.err.println("Transfer progress: " + progress);
                        } else {
                            System.err.println("Transfer progress: " + progress + " / "
                                    + total);
                        }
                    }

                    @Override
                    public void operationComplete(ChannelProgressiveFuture future)
                            throws Exception {
                        System.out.println("Transfer complete.");
                    }
                });
                ChannelFuture lastContentFuture = ctx
                        .writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
                if (!isKeepAlive(request)) {
                    lastContentFuture.addListener(ChannelFutureListener.CLOSE);
                }
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
                    throws Exception {
                cause.printStackTrace();
                if (ctx.channel().isActive()) {
                    sendError(ctx, INTERNAL_SERVER_ERROR);
                }
            }

            private String sanitizeUri(String uri) {
                try {
                    uri = URLDecoder.decode(uri, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    try {
                        uri = URLDecoder.decode(uri, "ISO-8859-1");
                    } catch (UnsupportedEncodingException e1) {
                        throw new Error();
                    }
                }
                if (!uri.startsWith(url)) {
                    return null;
                }
                if (!uri.startsWith("/")) {
                    return null;
                }
                uri = uri.replace('/', File.separatorChar);
                if (uri.contains(File.separator + '.')
                        || uri.contains('.' + File.separator) || uri.startsWith(".")
                        || uri.endsWith(".") || INSECURE_URI.matcher(uri).matches()) {
                    return null;
                }
                return System.getProperty("user.dir") + uri;
            }

            private void sendListing(ChannelHandlerContext ctx, File dir) {
                FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);
                response.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");
                StringBuilder buf = new StringBuilder();
                String dirPath = dir.getPath();
                buf.append("<!DOCTYPE html>\r\n");
                buf.append("<html><head><title>");
                buf.append(dirPath);
                buf.append(" 目录：");
                buf.append("</title></head><body>\r\n");
                buf.append("<h3>");
                buf.append(dirPath).append(" 目录：");
                buf.append("</h3>\r\n");
                buf.append("<ul>");
                buf.append("<li>链接：<a href=\"../\">..</a></li>\r\n");
                for (File f : dir.listFiles()) {
                    if (f.isHidden() || !f.canRead()) {
                        continue;
                    }
                    String name = f.getName();
                    if (!ALLOWED_FILE_NAME.matcher(name).matches()) {
                        continue;
                    }
                    buf.append("<li>链接：<a href=\"");
                    buf.append(name);
                    buf.append("\">");
                    buf.append(name);
                    buf.append("</a></li>\r\n");
                }
                buf.append("</ul></body></html>\r\n");
                ByteBuf buffer = Unpooled.copiedBuffer(buf, CharsetUtil.UTF_8);
                response.content().writeBytes(buffer);
                buffer.release();
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            }

            private void sendRedirect(ChannelHandlerContext ctx, String newUri) {
                FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, FOUND);
                response.headers().set(LOCATION, newUri);
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            }

            private void sendError(ChannelHandlerContext ctx,
                                   HttpResponseStatus status) {
                FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1,
                        status, Unpooled.copiedBuffer("Failure: " + status.toString()
                        + "\r\n", CharsetUtil.UTF_8));
                response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            }

            private void setContentTypeHeader(HttpResponse response, File file) {
                MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
                response.headers().set(CONTENT_TYPE,
                        mimeTypesMap.getContentType(file.getPath()));
            }


        }
    }
}
