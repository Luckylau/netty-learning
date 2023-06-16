package luckylau.netty.nio.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * NIO编程
 *
 * @Author luckylau
 * @Date 2019/8/26
 */
public class TimeClient {
    private static ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static void main(String[] args) {
        int port = 6666;
        executorService.execute(new TimeClientHandler("127.0.0.1", port));
    }

    static class TimeClientHandler implements Runnable {

        private String host;
        private int port;

        private Selector selector;
        private SocketChannel socketChannel;

        private volatile boolean stop;

        public TimeClientHandler(String host, int port) {
            this.host = host == null || host.isEmpty() ? "127.0.0.1" : host;
            this.port = port;
            try {
                selector = Selector.open();
                socketChannel = SocketChannel.open();
                socketChannel.configureBlocking(false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        @Override
        public void run() {
            try {
                doConnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (!stop) {
                try {
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
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // 多路复用器关闭后，所有注册在上面的Channel和Pipe等资源都会被自动去注册并关闭，所以不需要重复释放资源
            if (selector != null)
                try {
                    selector.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

        }

        private void handleInput(SelectionKey key) throws IOException {

            if (key.isValid()) {
                // 判断是否连接成功
                SocketChannel sc = (SocketChannel) key.channel();
                if (key.isConnectable()) {
                    if (sc.finishConnect()) {
                        sc.register(selector, SelectionKey.OP_READ);
                        doWrite(sc);
                    } else {
                        // 连接失败，进程退出
                        System.exit(1);
                    }
                }

                if (key.isReadable()) {
                    ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                    int readBytes = sc.read(readBuffer);
                    if (readBytes > 0) {
                        readBuffer.flip();
                        byte[] bytes = new byte[readBuffer.remaining()];
                        readBuffer.get(bytes);
                        String body = new String(bytes, "UTF-8");
                        System.out.println("Now is : " + body);
                        this.stop = true;
                    } else if (readBytes < 0) {
                        // 对端链路关闭
                        key.cancel();
                        sc.close();
                    }
                }
            }

        }

        private void doConnect() throws IOException {
            // 如果直接连接成功，则注册到多路复用器上，发送请求消息，读应答
            if (socketChannel.connect(new InetSocketAddress(host, port))) {
                socketChannel.register(selector, SelectionKey.OP_READ);
                doWrite(socketChannel);
            } else {
                socketChannel.register(selector, SelectionKey.OP_CONNECT);
            }
        }

        private void doWrite(SocketChannel sc) throws IOException {
            byte[] req = "query time order".getBytes();
            ByteBuffer writeBuffer = ByteBuffer.allocate(req.length);
            writeBuffer.put(req);
            writeBuffer.flip();
            sc.write(writeBuffer);
        }
    }
}
