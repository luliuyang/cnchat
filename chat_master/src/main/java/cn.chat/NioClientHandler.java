package cn.chat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

/**
 * 客户端线程类，专门用于接收服务器端发送过来的信息
 *
 * @author lulu
 * @date 2019-06-22 15:15
 */
public class NioClientHandler extends Thread {
    private Selector selector;

    public NioClientHandler(Selector selector) {
        this.selector = selector;
    }
    @Override
    public void run() {
        try {
            while (true) {
                int readyChannels = selector.select();
                if (readyChannels == 0) {
                    continue;
                }
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();
                    iterator.remove();
                    if (selectionKey.isReadable()) {
                        readHandler(selectionKey, selector);
                    }
                }
            }
        } catch (IOException e) {

            e.printStackTrace();
        }

    }
    /**
     * 可读事件处理器，把客户端写过来的东西打印出来
     *
     * @param selectionKey
     * @param selector
     * @throws IOException
     */
    private void readHandler(SelectionKey selectionKey, Selector selector) throws IOException {
        /**
         * 从selectionKey中获取到已经就绪的channel
         */
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        /**
         * 创建buffer,用于在通道里读写数据
         */
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        /**
         * 读到的东西放在这里
         */
        String response = "";
        while (socketChannel.read(byteBuffer) > 0) {
            /**
             * 切换buffer为输出数据做好准备
             */
            byteBuffer.flip();
            /**
             * 统一编码格式
             */
            response += Charset.forName("UTF-8").decode(byteBuffer);
            /**
             * 做好装入数据的准备
             */
//            byteBuffer.clear();

        }
        socketChannel.register(selector, SelectionKey.OP_READ);
        if (response.length() > 0) {
            System.out.println(response);
        }
    }
}
