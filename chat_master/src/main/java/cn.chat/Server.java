package cn.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

/**
 * @author lulu
 * @date 2019-06-22 14:22
 */
public class Server {
    /**
     * 基于sl4j的日志文件
     */
    private Logger logger= LoggerFactory.getLogger(this.getClass());

    /**
     * 主方法
     *
     * @param args
     */
    public static void main(String[] args) throws IOException {
        Server nioServer = new Server();
        nioServer.start();
    }

    /**
     * 开始服务器
     */
    public void start() throws IOException {
        /**
         * 1.创建Selector
         */
        Selector selector = Selector.open();
        /**
         * 2.通过ServerSocketChannel创建channel通道
         */
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        /**
         * 3，为channel通道绑定监听端口
         */
        serverSocketChannel.socket().bind(new InetSocketAddress(8000));
        /**
         * 4.设置channel为非阻塞模式
         */
        serverSocketChannel.configureBlocking(false);
        /**
         * 5.将channel注册到selector上，监听连接事件
         */
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        logger.info("服务器启动成功");
        /**
         * 6.循环等待新接入的连接
         */
        while (true) {
            /**
             * 获取可用的channel数量
             */
            //todo
            int readyChannels = selector.select();
            if (readyChannels == 0) {
                continue;
            }
            /**
             *获取可用的channel集合
             */
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                /**
                 * 取出实例，并移除
                 */
                SelectionKey selectionKey = iterator.next();
                iterator.remove();
                /**
                 * 7，根据就绪状态，调用对应方法，完成业务逻辑
                 */
                if (selectionKey.isAcceptable()) {
                    acceptHandler(serverSocketChannel, selector);
                }
                if (selectionKey.isReadable()) {
                    readHandler(selectionKey, selector);
                }
            }
        }

    }



    /**
     * 接入事件处理器，主要功能为创建socketChannel响应客户端的建立连接请求，同时在多路复用器上注册连接可读事件
     *
     * @param serverSocketChannel 服务器套接字信道
     * @param selector            多路复用器
     * @throws IOException
     */
    private void acceptHandler(ServerSocketChannel serverSocketChannel, Selector selector) throws IOException {
        /**
         * 创建连接通道
         */
        SocketChannel socketChannel = serverSocketChannel.accept();
        /**
         * 设置也是工作在非阻塞模式
         */
        socketChannel.configureBlocking(false);
        /**
         * 注册可读事件
         */
        socketChannel.register(selector, SelectionKey.OP_READ);
        /**
         * 通过通道，给个回应
         */
        logger.info("新客户端登陆");
        socketChannel.write(Charset.forName("UTF-8").encode("你与聊天室其他人都不是好友关系，注意隐私安全"));
    }

    /**
     * 可读事件处理器，把用户写过来的东西广播给所有在线用户
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
        String request = "";
        while (socketChannel.read(byteBuffer) > 0) {
            /**
             * 切换buffer为输出数据做好准备
             */
            byteBuffer.flip();
            /**
             * 统一编码格式
             */
            request += Charset.forName("UTF-8").decode(byteBuffer);
            /**
             * 做到装入数据的准备
             */
        }
        socketChannel.register(selector, SelectionKey.OP_READ);
        if (request.length() > 0) {
            /**
             * 广播给所有客户端用户
             */
            System.out.println(request);
            broadCast(selector, socketChannel, request);
        }
    }

    /**
     * 实现服务器的广播
     *
     * @param selector
     * @param sendPerson
     * @param msg
     * @throws IOException
     */
    private void broadCast(Selector selector, final SocketChannel sendPerson, String msg) throws IOException {
        /**
         * 获取到已经接入的客户端的channel,获取的是注册到的，不是就绪的
         */
        Set<SelectionKey> selectionKeys = selector.keys();
        /**
         * 循环向所有channel广播信息
         */
        Iterator<SelectionKey> iterator = selectionKeys.iterator();
        while (iterator.hasNext()) {
            SelectionKey SelectionKey = iterator.next();
//            iterator.remove();
            Channel channel = SelectionKey.channel();
            //首先发送是给除了发送者以外的其他人发的，其次要判断是不是一个socketChannel
            if (channel instanceof SocketChannel && !channel.equals(sendPerson)) {
                try {
                    ((SocketChannel) channel).write(Charset.forName("UTF-8").encode(msg));
                } catch (IOException e) {
                    logger.error(e.getMessage(),e);
                    e.printStackTrace();
                }
            }
        }
    }
}
