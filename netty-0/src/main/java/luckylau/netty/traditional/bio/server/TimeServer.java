package luckylau.netty.traditional.bio.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static luckylau.netty.utils.TimeUtils.getCurrentTime;

/**
 * @Author luckylau
 * @Date 2019/8/26
 */
public class TimeServer {
    private static ExecutorService executorService = Executors.newCachedThreadPool();

    public static void main(String[] args) throws IOException {
        int port = 6666;
        ServerSocket server = null;
        try {
            server = new ServerSocket(port);
            System.out.println("The bio time server is start in port : " + port);
            Socket socket = null;
            while (true) {
                //当没有客户端连接时，会阻塞在这里
                socket = server.accept();
                //每当有一个客户端连接时候，都会创建一个线程进行处理
                executorService.execute(new TimeServerHandler(socket));

            }
        } finally {
            if (server != null) {
                System.out.println("The bio time server close.");
                server.close();
            }
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
