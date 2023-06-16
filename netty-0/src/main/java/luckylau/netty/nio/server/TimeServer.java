package luckylau.netty.nio.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static luckylau.netty.utils.TimeUtils.getCurrentTime;

/**
 * NIO编程
 *
 * @Author luckylau
 * @Date 2019/8/26
 */
public class TimeServer {
    private static ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static void main(String[] args) {
        int port = 6666;
        MultiplexerTimeServer timeServer = new MultiplexerTimeServer(port);
        executorService.execute(timeServer);
    }

    static class MultiplexerTimeServer implements Runnable {

        private Selector selector;

        private ServerSocketChannel servChannel;

        private volatile boolean stop;

        public MultiplexerTimeServer(int port) {
            try {
                selector = Selector.open();
                //打开 ServerSocketChannel
                servChannel = ServerSocketChannel.open();
                servChannel.configureBlocking(false);
                //绑定监听地址  InetSocketAddress
                servChannel.socket().bind(new InetSocketAddress(port), 1024);
                servChannel.register(selector, SelectionKey.OP_ACCEPT);
                System.out.println("The nio server is start in port : " + port);
            } catch (IOException e) {
                e.printStackTrace();

            }
        }

        @Override
        public void run() {
            while (!stop) {
                try {
                    //每隔1s会被唤醒
                    selector.select(TimeUnit.SECONDS.toMillis(1));
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> it = selectedKeys.iterator();
                    while (it.hasNext()) {
                        SelectionKey key = it.next();
                        it.remove();
                        try {
                            handleInput(key);
                        } catch (Exception e) {
                            if (key != null) {
                                key.cancel();
                                if (key.channel() != null) {
                                    key.channel().close();
                                }
                            }
                        }
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }

            // 多路复用器关闭后，所有注册在上面的Channel和Pipe等资源都会被自动去注册并关闭，所以不需要重复释放资源
            if (selector != null) {
                try {
                    selector.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void handleInput(SelectionKey key) throws IOException {

            if (key.isValid()) {
                // 处理新接入的请求消息
                if (key.isAcceptable()) {
                    // 接受一个新链接
                    ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                    SocketChannel sc = ssc.accept();
                    sc.configureBlocking(false);
                    // 注册监听读操作
                    sc.register(selector, SelectionKey.OP_READ);
                }

                if (key.isReadable()) {
                    // 读取数据
                    SocketChannel sc = (SocketChannel) key.channel();
                    ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                    int readBytes = sc.read(readBuffer);
                    if (readBytes > 0) {
                        //将缓冲区当前的limit设置为position，position为0，用于后续对缓存区的读取操作
                        readBuffer.flip();
                        byte[] bytes = new byte[readBuffer.remaining()];
                        readBuffer.get(bytes);
                        String body = new String(bytes, StandardCharsets.UTF_8);
                        System.out.println("The time server receive order : "
                                + body);
                        String currentTime = "ack it, " + getCurrentTime();
                        doWrite(sc, currentTime);
                    } else if (readBytes < 0) {
                        // 对端链路关闭
                        key.cancel();
                        sc.close();
                    }
                }
            }
        }

        private void doWrite(SocketChannel channel, String response)
                throws IOException {
            if (response != null && response.trim().length() > 0) {
                byte[] bytes = response.getBytes();
                ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
                writeBuffer.put(bytes);
                writeBuffer.flip();
                channel.write(writeBuffer);
            }
        }

        public void stop() {
            this.stop = true;
        }
    }
}
