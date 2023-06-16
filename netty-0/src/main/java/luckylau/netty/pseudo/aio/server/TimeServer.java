package luckylau.netty.pseudo.aio.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static luckylau.netty.utils.TimeUtils.getCurrentTime;

/**
 * 伪异步I/O模型图
 *
 * @Author luckylau
 * @Date 2019/8/26
 */
public class TimeServer {
    /**
     * 创建IO任务线程池
     */
    private static TimeServerHandlerExecutePool singleExecutor = new TimeServerHandlerExecutePool(
            50, 10000);

    public static void main(String[] args) throws IOException {
        int port = 6666;
        ServerSocket server = null;
        try {
            server = new ServerSocket(port);
            System.out.println("The pio time server is start in port : " + port);
            Socket socket = null;
            while (true) {
                socket = server.accept();
                //复用线程池
                singleExecutor.execute(new TimeServerHandler(socket));

            }
        } finally {
            if (server != null) {
                System.out.println("The pio time server close");
                server.close();
            }
        }

    }

    static class TimeServerHandlerExecutePool {
        private ExecutorService executor;

        public TimeServerHandlerExecutePool(int maxPoolSize, int queueSize) {
            executor = new ThreadPoolExecutor(Runtime.getRuntime()
                    .availableProcessors(), maxPoolSize, 120L, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<Runnable>(queueSize));
        }

        public void execute(java.lang.Runnable task) {
            executor.execute(task);
        }
    }

    static class TimeServerHandler implements Runnable {

        private Socket socket;

        public TimeServerHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            BufferedReader in = null;
            PrintWriter out = null;
            try {
                in = new BufferedReader(new InputStreamReader(
                        this.socket.getInputStream()));
                out = new PrintWriter(this.socket.getOutputStream(), true);

                String currentTime;
                String body;
                while (true) {
                    body = in.readLine();
                    if (body == null) {
                        break;
                    }
                    System.out.println("The time server receive order : " + body);
                    currentTime = "ack it,  " + getCurrentTime();
                    out.println(currentTime);
                }

            } catch (Exception e) {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                if (out != null) {
                    out.close();
                }
                if (this.socket != null) {
                    try {
                        this.socket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
    }
}
