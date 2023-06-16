package luckylau.netty.aio.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AIO编程
 *
 * @Author luckylau
 * @Date 2019/8/26
 */
public class TimeServer {
    private static ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static void main(String[] args) {
        int port = 6666;
        executorService.execute(new AsyncTimeServerHandler(port));

    }

    static class AsyncTimeServerHandler implements Runnable {

        private int port;

        private CountDownLatch latch;
        private AsynchronousServerSocketChannel asynchronousServerSocketChannel;

        public AsyncTimeServerHandler(int port) {
            this.port = port;
            try {
                asynchronousServerSocketChannel = AsynchronousServerSocketChannel.open();
                asynchronousServerSocketChannel.bind(new InetSocketAddress(port));
                System.out.println("The time server is start in port : " + port);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {

            latch = new CountDownLatch(1);
            doAccept();
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private void doAccept() {
            asynchronousServerSocketChannel.accept(this,
                    new AcceptCompletionHandler());
        }
    }

    static class AcceptCompletionHandler implements
            CompletionHandler<AsynchronousSocketChannel, AsyncTimeServerHandler>

    {

        @Override
        public void completed(AsynchronousSocketChannel result,
                              AsyncTimeServerHandler attachment) {
            attachment.asynchronousServerSocketChannel.accept(attachment, this);
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            result.read(buffer, buffer, new ReadCompletionHandler(result));
        }

        @Override
        public void failed(Throwable exc, AsyncTimeServerHandler attachment) {
            attachment.latch.countDown();
            exc.printStackTrace();
        }

    }

    static class ReadCompletionHandler implements
            CompletionHandler<Integer, ByteBuffer> {

        private AsynchronousSocketChannel channel;

        public ReadCompletionHandler(AsynchronousSocketChannel channel) {
            if (this.channel == null) {
                this.channel = channel;
            }
        }

        @Override
        public void completed(Integer result, ByteBuffer attachment) {
            attachment.flip();
            byte[] body = new byte[attachment.remaining()];
            attachment.get(body);
            String req = new String(body, StandardCharsets.UTF_8);
            System.out.println("The time server receive order : " + req);
            String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(req) ? new java.util.Date(
                    System.currentTimeMillis()).toString() : "BAD ORDER";
            doWrite(currentTime);
        }

        private void doWrite(String currentTime) {
            if (currentTime != null && currentTime.trim().length() > 0) {
                byte[] bytes = (currentTime).getBytes();
                ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
                writeBuffer.put(bytes);
                writeBuffer.flip();
                channel.write(writeBuffer, writeBuffer,
                        new CompletionHandler<Integer, ByteBuffer>() {
                            @Override
                            public void completed(Integer result, ByteBuffer buffer) {
                                // 如果没有发送完成，继续发送
                                if (buffer.hasRemaining())
                                    channel.write(buffer, buffer, this);
                            }

                            @Override
                            public void failed(Throwable exc, ByteBuffer attachment) {
                                try {
                                    channel.close();
                                } catch (IOException e) {
                                }
                            }
                        });
            }
        }

        @Override
        public void failed(Throwable exc, ByteBuffer attachment) {
            try {
                this.channel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


}
