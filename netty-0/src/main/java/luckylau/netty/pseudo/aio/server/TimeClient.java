package luckylau.netty.pseudo.aio.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * 伪异步I/O模型图
 *
 * @Author luckylau
 * @Date 2019/8/26
 */
public class TimeClient {

    public static void main(String[] args) throws IOException {
        int port = 6666;
        Socket socket = null;
        BufferedReader in = null;
        PrintWriter out = null;
        try {
            socket = new Socket("127.0.0.1", port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            out.println("query time order");
            String resp = in.readLine();
            System.out.println("The time client receive from server : " + resp);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }

            if (socket != null) {
                socket.close();
            }
        }
    }
}
