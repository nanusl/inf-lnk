package com.ly.fn.inf.lnk.remoting.netty;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月19日 下午10:28:53
 */
public class NettyServerConfigurator implements Cloneable {
    private int listenPort = 8888;
    private int serverWorkerThreads = 10;
    private int serverSelectorThreads = 5;
    private int serverChannelMaxIdleTimeSeconds = 120;
    private int serverSocketSndBufSize = Integer.getInteger(NettySystemConfigurator.IO_REMOTING_SOCKET_SNDBUF_SIZE, 65535);
    private int serverSocketRcvBufSize = Integer.getInteger(NettySystemConfigurator.IO_REMOTING_SOCKET_RCVBUF_SIZE, 65535);
    private boolean serverPooledByteBufAllocatorEnable = true;
    private int defaultServerWorkerProcessorThreads = 10;
    private int defaultServerExecutorThreads = 8;
    
    /**
     * make make install
     * ../glibc-2.10.1/configure \ --prefix=/usr \ --with-headers=/usr/include \
     * --host=x86_64-linux-gnu \ --build=x86_64-pc-linux-gnu \ --without-gd
     */
    private boolean useEpollNativeSelector = false;

    public int getListenPort() {
        return listenPort;
    }

    public void setListenPort(int listenPort) {
        this.listenPort = listenPort;
    }

    public int getDefaultServerExecutorThreads() {
        return defaultServerExecutorThreads;
    }

    public void setDefaultServerExecutorThreads(int defaultServerExecutorThreads) {
        this.defaultServerExecutorThreads = defaultServerExecutorThreads;
    }

    public int getDefaultServerWorkerProcessorThreads() {
        return defaultServerWorkerProcessorThreads;
    }

    public void setDefaultServerWorkerProcessorThreads(int defaultServerWorkerProcessorThreads) {
        this.defaultServerWorkerProcessorThreads = defaultServerWorkerProcessorThreads;
    }

    public int getServerWorkerThreads() {
        return serverWorkerThreads;
    }

    public void setServerWorkerThreads(int serverWorkerThreads) {
        this.serverWorkerThreads = serverWorkerThreads;
    }

    public int getServerSelectorThreads() {
        return serverSelectorThreads;
    }

    public void setServerSelectorThreads(int serverSelectorThreads) {
        this.serverSelectorThreads = serverSelectorThreads;
    }

    public int getServerChannelMaxIdleTimeSeconds() {
        return serverChannelMaxIdleTimeSeconds;
    }

    public void setServerChannelMaxIdleTimeSeconds(int serverChannelMaxIdleTimeSeconds) {
        this.serverChannelMaxIdleTimeSeconds = serverChannelMaxIdleTimeSeconds;
    }

    public int getServerSocketSndBufSize() {
        return serverSocketSndBufSize;
    }

    public void setServerSocketSndBufSize(int serverSocketSndBufSize) {
        this.serverSocketSndBufSize = serverSocketSndBufSize;
    }

    public int getServerSocketRcvBufSize() {
        return serverSocketRcvBufSize;
    }

    public void setServerSocketRcvBufSize(int serverSocketRcvBufSize) {
        this.serverSocketRcvBufSize = serverSocketRcvBufSize;
    }

    public boolean isServerPooledByteBufAllocatorEnable() {
        return serverPooledByteBufAllocatorEnable;
    }

    public void setServerPooledByteBufAllocatorEnable(boolean serverPooledByteBufAllocatorEnable) {
        this.serverPooledByteBufAllocatorEnable = serverPooledByteBufAllocatorEnable;
    }

    public boolean isUseEpollNativeSelector() {
        return useEpollNativeSelector;
    }

    public void setUseEpollNativeSelector(boolean useEpollNativeSelector) {
        this.useEpollNativeSelector = useEpollNativeSelector;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return (NettyServerConfigurator) super.clone();
    }
}
