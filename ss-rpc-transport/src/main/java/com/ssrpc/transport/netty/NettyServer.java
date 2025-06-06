package com.ssrpc.transport.netty;

import com.ssrpc.registry.ServiceInvokerRegistry;
import com.ssrpc.transport.config.NetworkConfig;
import com.ssrpc.transport.exception.TransportException;
import com.ssrpc.transport.api.RpcServer;
import com.ssrpc.transport.codec.SimpleRpcCodec;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基于Netty的RPC服务端实现.
 * 
 * 使用Netty NIO框架提供高性能的网络服务
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class NettyServer implements RpcServer {
    
    private static final Logger log = LoggerFactory.getLogger(NettyServer.class);
    
    private final ServiceInvokerRegistry serviceRegistry;
    private final NetworkConfig config;
    private ServerBootstrap bootstrap;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel channel;
    private volatile boolean running = false;
    private int port = -1;
    
    public NettyServer(ServiceInvokerRegistry serviceRegistry) {
        this(serviceRegistry, new NetworkConfig());
    }
    
    public NettyServer(ServiceInvokerRegistry serviceRegistry, NetworkConfig config) {
        this.serviceRegistry = serviceRegistry;
        this.config = config;
        this.config.validate();
    }
    
    @Override
    public synchronized void start(int port) throws TransportException {
        if (running) {
            log.warn("Server is already running on port {}", this.port);
            return;
        }
        
        try {
            // 创建EventLoop组
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup(config.getWorkerThreads());
            
            // 配置服务端
            bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, config.isKeepAlive())
                    .childOption(ChannelOption.TCP_NODELAY, config.isTcpNoDelay())
                    .childOption(ChannelOption.SO_RCVBUF, config.getReceiveBufferSize())
                    .childOption(ChannelOption.SO_SNDBUF, config.getSendBufferSize())
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            
                            // 添加编解码器
                            pipeline.addLast(new SimpleRpcCodec());
                            
                            // 添加业务处理器
                            pipeline.addLast(new NettyServerHandler(serviceRegistry));
                        }
                    });
            
            // 配置内存分配器
            if (config.isUsePooledAllocator()) {
                bootstrap.childOption(ChannelOption.ALLOCATOR, io.netty.buffer.PooledByteBufAllocator.DEFAULT);
            }
            
            // 绑定端口并启动服务
            ChannelFuture future = bootstrap.bind(port).sync();
            channel = future.channel();
            this.port = port;
            running = true;
            
            log.info("Netty server started on port {}", port);
            
        } catch (Exception e) {
            log.error("Failed to start server on port {}", port, e);
            shutdown();
            throw new TransportException("Failed to start server on port " + port, e);
        }
    }
    
    @Override
    public synchronized void shutdown() {
        if (!running) {
            return;
        }
        
        try {
            running = false;
            
            if (channel != null) {
                channel.close().sync();
            }
            
            if (workerGroup != null) {
                workerGroup.shutdownGracefully();
            }
            
            if (bossGroup != null) {
                bossGroup.shutdownGracefully();
            }
            
            log.info("Netty server shutdown completed");
            
        } catch (Exception e) {
            log.error("Error during server shutdown", e);
        } finally {
            port = -1;
        }
    }
    
    @Override
    public boolean isRunning() {
        return running;
    }
    
    @Override
    public int getPort() {
        return port;
    }
} 