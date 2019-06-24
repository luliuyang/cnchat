package cn.chat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Scanner;

/**
 * @author lulu
 * @date 2019-06-22 16:12
 */
public class BClient {
    private String name;

    public BClient(String name) {
        this.name = name;
    }

    public static void main(String[] args) throws IOException {
        System.out.println("请输入你的名字：");
        Scanner in=new Scanner(System.in);
        AClient nioClient = new AClient(in.nextLine());
        nioClient.start();
    }

    public void start() throws IOException {
        /**
         * 连接服务器端
         */
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1", 8000));
        socketChannel.configureBlocking(false);
        /**
         * 接收服务器端响应，新开一个线程，专门负责接收服务器端的响应数据
         */
        Selector selector = Selector.open();
        socketChannel.register(selector, SelectionKey.OP_READ);
        new Thread(new NioClientHandler(selector)).start();
        /**
         *向服务器端发送数据
         */
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            String request = scanner.nextLine();
            if (request != null && request.length() > 0) {
                /**
                 * 发送这个数据
                 */
                socketChannel.write(Charset.forName("UTF-8").encode(this.name+":"+request));

            }
        }
    }
}
