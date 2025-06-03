package com.ssrpc.core.config;

import lombok.Data;

import java.util.concurrent.TimeUnit;

/**
 * 网络配置类.
 * 
 * 包含基于Netty的网络通信层所有配置参数，涵盖性能调优、连接管理、
 * 线程池配置、缓冲区设置、心跳检测等各个方面的专业配置
 * 
 * @author chenzhang
 * @since 1.0.0
 */
@Data
public class NetworkConfig {
    
    // ===== 服务端核心配置 =====
    
    /**
     * 服务端监听端口
     * 
     * 设置RPC服务监听的网络端口，范围1-65535
     * 建议使用非特权端口(>1024)以避免权限问题
     */
    private int serverPort = 8080;
    
    /**
     * Boss线程数 - Netty主Reactor线程池大小
     * 
     * Boss线程负责处理新连接的Accept操作，在典型的单端口服务中，
     * 一个Boss线程就足够了。多Boss线程主要用于多端口监听场景。
     * 
     * 性能建议：
     * - 单端口服务：设置为1
     * - 多端口服务：每个端口1个线程
     * - 过多的Boss线程会导致不必要的上下文切换开销
     */
    private int bossThreads = 1;
    
    /**
     * Worker线程数 - Netty从Reactor线程池大小
     * 
     * Worker线程负责处理已建立连接的I/O操作，是Netty性能的关键参数。
     * 这些线程执行Channel的读写操作和ChannelHandler的业务逻辑。
     * 
     * 性能建议：
     * - CPU密集型：CPU核数
     * - I/O密集型：CPU核数 * 2
     * - 高并发场景：CPU核数 * 2 至 CPU核数 * 4
     * - 避免设置过大，会增加线程切换开销和内存消耗
     * 
     * 注意：ChannelHandler中的重操作应该异步化到业务线程池
     */
    private int workerThreads = Runtime.getRuntime().availableProcessors() * 2;
    
    /**
     * 业务线程池大小
     * 
     * 专门处理RPC业务逻辑的线程池大小，将计算密集型任务从I/O线程中分离，
     * 避免阻塞Netty的EventLoop，保证网络I/O的高效处理。
     * 
     * 性能建议：
     * - 计算密集型业务：CPU核数 * 1.5
     * - I/O密集型业务：CPU核数 * 4-8
     * - 混合型业务：200-500（根据业务特性调整）
     * - 考虑业务处理时间和QPS来确定合适的大小
     */
    private int businessThreads = 200;
    
    /**
     * 业务线程池类型
     * 
     * 不同类型的线程池适用于不同的业务场景：
     * - FIXED: 固定大小，适合稳定负载
     * - CACHED: 缓存线程池，适合突发流量
     * - SCHEDULED: 调度线程池，适合定时任务
     * - CUSTOM: 自定义线程池，适合特殊需求
     */
    private ThreadPoolType businessThreadPoolType = ThreadPoolType.CACHED;
    
    /**
     * 服务端Socket积压队列大小 (SO_BACKLOG)
     * 
     * 控制服务端Socket监听队列的大小，即等待Accept的连接数上限。
     * 当连接请求速率超过Accept速率时，新连接会在此队列中等待。
     * 
     * 性能影响：
     * - 设置过小：高并发时新连接可能被拒绝(Connection refused)
     * - 设置过大：占用更多内存，可能导致连接超时
     * 
     * 建议值：
     * - 低并发场景：128-512
     * - 高并发场景：1024-4096
     * - 超高并发：根据系统承载能力调整，配合监控观察拒绝率
     */
    private int serverBacklog = 1024;
    
    /**
     * 服务端地址重用选项 (SO_REUSEADDR)
     * 
     * 允许在服务重启时快速重绑定端口，避免"Address already in use"错误。
     * 在TCP连接处于TIME_WAIT状态时仍然可以重新绑定端口。
     * 
     * 建议：生产环境建议开启，开发调试时必须开启
     */
    private boolean serverReuseAddress = true;
    
    // ===== 客户端连接配置 =====
    
    /**
     * 客户端连接超时时间（毫秒）
     * 
     * TCP三次握手完成的最大等待时间，超时后连接尝试失败。
     * 
     * 性能建议：
     * - 内网环境：1000-3000ms
     * - 外网环境：3000-5000ms  
     * - 不稳定网络：5000-10000ms
     * - 设置过小可能导致网络抖动时连接失败
     * - 设置过大会影响故障快速发现和切换
     */
    private int connectTimeout = 5000;
    
    /**
     * RPC请求超时时间（毫秒）
     * 
     * 单个RPC调用从发送到接收响应的最大等待时间，
     * 包括网络传输时间和服务端处理时间。
     * 
     * 性能建议：
     * - 快速查询服务：1000-3000ms
     * - 一般业务服务：5000-10000ms
     * - 复杂计算服务：10000-30000ms
     * - 需要根据业务SLA和服务响应时间特性调整
     */
    private long requestTimeout = 10000;
    
    /**
     * 请求重试次数
     * 
     * RPC调用失败后的最大重试次数，用于处理网络抖动和临时故障。
     * 
     * 注意事项：
     * - 重试会增加系统负载和延迟
     * - 需要确保服务端接口的幂等性
     * - 建议配合熔断器使用，避免雪崩效应
     */
    private int maxRetryTimes = 3;
    
    /**
     * 重试间隔时间（毫秒）
     * 
     * 两次重试之间的等待时间，避免重试风暴。
     * 可以考虑实现指数退避策略来优化重试机制。
     */
    private long retryInterval = 1000;
    
    // ===== TCP Socket优化配置 =====
    
    /**
     * TCP_NODELAY选项
     * 
     * 禁用Nagle算法，立即发送数据包而不等待缓冲区填满。
     * Nagle算法会延迟小包发送以提高网络利用率，但会增加延迟。
     * 
     * 性能影响：
     * - 启用：降低延迟，适合实时性要求高的RPC调用
     * - 禁用：可能增加网络包数量，但通常对RPC影响不大
     * 
     * 建议：RPC场景下建议启用以获得更好的响应时间
     */
    private boolean tcpNoDelay = true;
    
    /**
     * SO_KEEPALIVE选项
     * 
     * 启用TCP层面的连接保活机制，定期发送保活探针检测连接状态。
     * 
     * 保活参数（系统级别，需要OS支持）：
     * - tcp_keepalive_time: 开始发送保活探针的空闲时间（通常2小时）
     * - tcp_keepalive_intvl: 保活探针发送间隔（通常75秒）
     * - tcp_keepalive_probes: 探针发送次数（通常9次）
     * 
     * 注意：应用层心跳通常比TCP保活更及时和可控
     */
    private boolean keepAlive = true;
    
    /**
     * Socket接收缓冲区大小 (SO_RCVBUF)
     * 
     * 控制TCP接收窗口大小，影响网络吞吐量性能。
     * 
     * 性能调优：
     * - 带宽延迟积：BDP = 带宽 × RTT
     * - 高带宽网络：适当增大以提升吞吐量
     * - 低延迟网络：使用默认值即可
     * 
     * 建议值：
     * - 千兆局域网：64KB-256KB
     * - 万兆网络：256KB-1MB
     * - 广域网：根据BDP计算调整
     */
    private int receiveBufferSize = 65536;
    
    /**
     * Socket发送缓冲区大小 (SO_SNDBUF)
     * 
     * 控制TCP发送窗口大小，影响发送性能和内存使用。
     * 
     * 性能考虑：
     * - 与接收缓冲区配合优化网络吞吐量
     * - 过大会占用更多内存，过小会限制发送性能
     * - 应该与应用消息大小和网络特性匹配
     */
    private int sendBufferSize = 65536;
    
    // ===== 心跳检测与连接管理 =====
    
    /**
     * 是否启用应用层心跳检测
     * 
     * 应用层心跳相比TCP保活具有更好的实时性和可控性，
     * 能够及时发现连接异常并进行故障切换。
     * 
     * 建议：生产环境建议启用，特别是长连接场景
     */
    private boolean heartbeatEnabled = true;
    
    /**
     * 心跳发送间隔（秒）
     * 
     * 客户端主动发送心跳包的时间间隔。
     * 
     * 性能平衡：
     * - 间隔过短：增加网络开销和CPU消耗
     * - 间隔过长：故障发现延迟，影响可用性
     * 
     * 建议值：
     * - 内网环境：10-30秒
     * - 外网环境：30-60秒
     * - 高可用场景：5-15秒
     */
    private int heartbeatInterval = 30;
    
    /**
     * 心跳超时时间（秒）
     * 
     * 心跳请求发送后等待响应的最大时间，超时未收到响应则认为连接异常。
     * 
     * 超时处理：
     * - 客户端：关闭连接并重新建立
     * - 服务端：关闭连接释放资源
     * 
     * 性能考虑：
     * - 设置过短：网络抖动时误判连接异常
     * - 设置过长：故障发现延迟，影响切换速度
     * 
     * 建议值：
     * - 内网环境：5-15秒 (心跳间隔的 1/2 到 1/3)
     * - 外网环境：15-30秒 (心跳间隔的 1/2 到 1/1)
     * - 通常设置为心跳间隔的 1/2，既能快速发现故障又避免误判
     */
    private int heartbeatTimeout = 15;
    
    /**
     * 读空闲超时时间（秒）
     * 
     * 连接在指定时间内没有读取到数据触发空闲事件。
     * 服务端可以基于此判断客户端是否异常，决定是否关闭连接。
     * 
     * 建议设置为心跳间隔的2-3倍，允许网络抖动和处理延迟
     */
    private int readerIdleTime = 60;
    
    /**
     * 写空闲超时时间（秒）
     * 
     * 连接在指定时间内没有写入数据触发空闲事件。
     * 客户端可以基于此主动发送心跳包保持连接活跃。
     * 
     * 通常设置为0（禁用）或者与心跳间隔相同
     */
    private int writerIdleTime = 0;
    
    /**
     * 读写空闲超时时间（秒）
     * 
     * 连接在指定时间内既没有读也没有写触发空闲事件。
     * 
     * 一般情况下使用读空闲或写空闲即可，此选项较少使用
     */
    private int allIdleTime = 0;
    
    // ===== 连接池与复用优化 =====
    
    /**
     * 是否启用客户端连接池
     * 
     * 连接池可以复用TCP连接，减少连接建立开销，
     * 特别适合短连接频繁的场景。
     * 
     * 优势：
     * - 减少TCP三次握手开销
     * - 降低TIME_WAIT状态连接数
     * - 提升并发处理能力
     * 
     * 劣势：
     * - 增加连接管理复杂度
     * - 需要处理连接有效性检查
     */
    private boolean connectionPoolEnabled = true;
    
    /**
     * 每个服务地址的最大连接数
     * 
     * 限制单个客户端到特定服务器的连接数量，
     * 防止连接数过多导致服务器负载过高。
     * 
     * 设置考虑：
     * - 服务器承载能力
     * - 客户端并发请求量
     * - 网络带宽和延迟
     * 
     * 建议：5-20个连接通常足够，超高并发时可适当增加
     */
    private int maxConnectionsPerAddress = 10;
    
    /**
     * 连接池最大空闲时间（秒）
     * 
     * 连接在池中保持空闲的最大时间，超时后关闭连接释放资源。
     * 
     * 平衡点：
     * - 过短：频繁创建连接，失去连接池意义
     * - 过长：占用资源，可能连接已经失效
     * 
     * 建议：5-30分钟，根据业务访问模式调整
     */
    private int connectionMaxIdleTime = 300;
    
    /**
     * 连接健康检查间隔（秒）
     * 
     * 定期检查连接池中连接的健康状态，清理失效连接。
     * 
     * 检查方式可以包括：
     * - 连接状态检查
     * - 发送轻量级探测请求
     * - 基于空闲时间判断
     */
    private int connectionHealthCheckInterval = 30;
    
    // ===== Netty性能优化配置 =====
    
    /**
     * 是否启用直接内存 (Direct Buffer)
     * 
     * 直接内存分配在JVM堆外，避免JVM GC影响，
     * 在网络I/O操作中有更好的性能表现。
     * 
     * 优势：
     * - 减少内存拷贝次数
     * - 避免GC压力
     * - 提升I/O性能
     * 
     * 劣势：
     * - 内存管理复杂，容易出现内存泄漏
     * - 调试困难
     * 
     * 建议：生产环境开启，开发调试时可以关闭
     */
    private boolean useDirectBuffer = true;
    
    /**
     * 是否启用Netty内存池 (PooledByteBufAllocator)
     * 
     * 内存池可以重复利用ByteBuf，减少对象创建和GC压力，
     * 特别在高并发场景下能显著提升性能。
     * 
     * 性能提升：
     * - 减少内存分配和回收开销
     * - 降低GC频率和耗时
     * - 提高内存利用率
     * 
     * 建议：生产环境强烈建议开启
     */
    private boolean usePooledAllocator = true;
    
    /**
     * EventLoop的I/O时间比例 (IoRatio)
     * 
     * 控制EventLoop线程在I/O操作和非I/O任务之间的时间分配比例。
     * 
     * 参数含义：
     * - 50: I/O和非I/O任务各占50%时间
     * - 70: I/O占70%，非I/O占30%时间
     * - 100: 优先处理I/O，非I/O任务在I/O空闲时处理
     * 
     * 调优建议：
     * - I/O密集型：70-90
     * - CPU密集型：30-50
     * - 混合型：50-70
     */
    private int ioRatio = 70;
    
    /**
     * 批处理大小
     * 
     * 在某些场景下批量处理请求可以提升吞吐量，
     * 但会增加延迟。需要在吞吐量和延迟之间找到平衡。
     */
    private int batchSize = 100;
    
    /**
     * 批处理超时时间（毫秒）
     * 
     * 批量处理的最大等待时间，防止因等待凑够批次
     * 而导致请求延迟过高。
     */
    private int batchTimeout = 10;
    
    // ===== 流量控制与保护 =====
    
    /**
     * 是否启用流量控制
     * 
     * 保护服务器免受过载，维持系统稳定性。
     * 可以结合限流算法（令牌桶、漏桶等）实现。
     */
    private boolean flowControlEnabled = false;
    
    /**
     * 最大并发请求数
     * 
     * 系统同时处理的最大请求数量，超过此限制的请求
     * 可以选择排队等待或直接拒绝。
     */
    private int maxConcurrentRequests = 1000;
    
    /**
     * 每秒最大请求数 (QPS限制)
     * 
     * 限制系统的请求处理速率，防止突发流量冲击。
     * 可以结合滑动窗口算法实现平滑限流。
     */
    private int maxRequestsPerSecond = 10000;
    
    // ===== SSL/TLS安全配置 =====
    
    /**
     * 是否启用SSL/TLS加密
     * 
     * 提供传输层安全保护，适用于敏感数据传输场景。
     * 启用后会增加CPU开销和连接建立时间。
     */
    private boolean sslEnabled = false;
    
    /**
     * SSL私钥文件路径
     * 
     * 服务端SSL证书对应的私钥文件路径，支持PKCS#1和PKCS#8格式。
     * 私钥用于SSL握手过程中的身份验证和密钥交换。
     * 
     * 安全注意事项：
     * - 私钥文件权限应设置为仅服务账户可读(600)
     * - 建议使用硬件安全模块(HSM)保护生产环境私钥
     * - 定期轮换密钥对以降低安全风险
     */
    private String sslKeyPath;
    
    /**
     * SSL证书文件路径
     * 
     * 服务端SSL证书文件路径，包含公钥和证书链信息。
     * 证书用于向客户端证明服务器身份的合法性。
     * 
     * 证书要求：
     * - 必须是有效的X.509证书
     * - 建议包含完整证书链到根CA
     * - 域名或IP必须与证书CN/SAN字段匹配
     * - 注意证书有效期，及时更新避免过期
     */
    private String sslCertPath;
    
    /**
     * SSL私钥密码
     * 
     * 私钥文件的解密密码，用于加载加密的私钥文件。
     * 
     * 安全建议：
     * - 使用强密码策略
     * - 避免在配置文件中明文存储
     * - 考虑使用密钥管理服务(KMS)
     * - 生产环境建议使用无密码的私钥文件
     */
    private String sslKeyPassword;
    
    // ===== 构造方法和工厂方法 =====
    
    /**
     * 默认构造方法
     * 
     * 使用默认配置初始化NetworkConfig，适合大多数开发和测试场景。
     * 默认配置在性能和资源使用之间取得平衡。
     */
    public NetworkConfig() {
        // 使用字段默认值
    }
    
    /**
     * 创建默认配置实例
     * 
     * 提供适用于一般生产环境的默认配置，在稳定性、性能和资源消耗
     * 之间取得平衡，适合中等负载的业务场景。
     * 
     * @return 默认配置实例
     */
    public static NetworkConfig defaultConfig() {
        return new NetworkConfig();
    }
    
    /**
     * 创建高性能配置实例
     * 
     * 针对高并发、高吞吐量场景优化的配置，通过增加线程数、
     * 优化缓冲区大小、启用性能优化选项来提升处理能力。
     * 
     * 适用场景：
     * - 高QPS服务
     * - 大数据量传输
     * - 对延迟要求不极致但需要高吞吐的场景
     * 
     * 注意：会消耗更多CPU和内存资源
     * 
     * @return 高性能配置实例
     */
    public static NetworkConfig highPerformanceConfig() {
        NetworkConfig config = new NetworkConfig();
        config.setWorkerThreads(Runtime.getRuntime().availableProcessors() * 4);
        config.setBusinessThreads(500);
        config.setServerBacklog(4096);
        config.setReceiveBufferSize(262144); // 256KB
        config.setSendBufferSize(262144);    // 256KB
        config.setUseDirectBuffer(true);
        config.setUsePooledAllocator(true);
        config.setIoRatio(80);
        config.setBatchSize(200);
        config.setHeartbeatInterval(20);     // 较快的心跳
        config.setHeartbeatTimeout(10);      // 较快的故障检测
        return config;
    }
    
    /**
     * 创建低延迟配置实例
     * 
     * 针对低延迟要求的实时系统优化的配置，通过减少批处理、
     * 优化网络参数、调整线程策略来最小化响应时间。
     * 
     * 适用场景：
     * - 实时交易系统
     * - 在线游戏
     * - 实时通信应用
     * - 对响应时间要求极高的服务
     * 
     * 权衡：可能会牺牲一定的吞吐量来换取更低的延迟
     * 
     * @return 低延迟配置实例
     */
    public static NetworkConfig lowLatencyConfig() {
        NetworkConfig config = new NetworkConfig();
        config.setWorkerThreads(Runtime.getRuntime().availableProcessors());
        config.setBusinessThreads(100);
        config.setTcpNoDelay(true);
        config.setBatchSize(1);      // 禁用批处理
        config.setBatchTimeout(1);   // 最小批处理超时
        config.setIoRatio(90);       // 优先处理I/O
        config.setConnectTimeout(1000);
        config.setRequestTimeout(3000);
        config.setHeartbeatInterval(10);     // 更频繁的心跳检测
        config.setHeartbeatTimeout(5);       // 快速故障检测
        return config;
    }
    
    /**
     * 配置参数验证
     * 
     * 验证所有配置参数的合法性和一致性，确保配置的有效性。
     * 在服务启动前调用此方法可以及早发现配置问题。
     * 
     * 验证内容包括：
     * - 端口范围有效性
     * - 线程数合理性
     * - 超时时间逻辑性
     * - 缓冲区大小合理性
     * - SSL配置完整性
     * 
     * @throws IllegalArgumentException 当配置参数不合法时抛出
     */
    public void validate() {
        if (serverPort <= 0 || serverPort > 65535) {
            throw new IllegalArgumentException("Server port must be between 1 and 65535");
        }
        
        if (bossThreads <= 0) {
            throw new IllegalArgumentException("Boss threads must be positive");
        }
        
        if (workerThreads <= 0) {
            throw new IllegalArgumentException("Worker threads must be positive");
        }
        
        if (businessThreads <= 0) {
            throw new IllegalArgumentException("Business threads must be positive");
        }
        
        if (connectTimeout <= 0) {
            throw new IllegalArgumentException("Connect timeout must be positive");
        }
        
        if (requestTimeout <= 0) {
            throw new IllegalArgumentException("Request timeout must be positive");
        }
        
        if (heartbeatEnabled && heartbeatInterval <= 0) {
            throw new IllegalArgumentException("Heartbeat interval must be positive when heartbeat is enabled");
        }
        
        if (heartbeatEnabled && heartbeatTimeout <= 0) {
            throw new IllegalArgumentException("Heartbeat timeout must be positive when heartbeat is enabled");
        }
        
        if (heartbeatEnabled && heartbeatTimeout >= heartbeatInterval) {
            throw new IllegalArgumentException("Heartbeat timeout should be less than heartbeat interval");
        }
        
        if (heartbeatEnabled && readerIdleTime > 0 && readerIdleTime <= heartbeatInterval) {
            throw new IllegalArgumentException("Reader idle time should be greater than heartbeat interval");
        }
        
        if (sslEnabled) {
            if (sslKeyPath == null || sslKeyPath.trim().isEmpty()) {
                throw new IllegalArgumentException("SSL key path is required when SSL is enabled");
            }
            if (sslCertPath == null || sslCertPath.trim().isEmpty()) {
                throw new IllegalArgumentException("SSL cert path is required when SSL is enabled");
            }
        }
    }
    
    // ===== 便利方法 =====
    
    /**
     * 获取心跳间隔时间（毫秒）
     * 
     * @return 心跳间隔毫秒数
     */
    public long getHeartbeatIntervalMillis() {
        return heartbeatInterval * 1000L;
    }
    
    /**
     * 获取心跳超时时间（毫秒）
     * 
     * @return 心跳超时毫秒数
     */
    public long getHeartbeatTimeoutMillis() {
        return heartbeatTimeout * 1000L;
    }
    
    /**
     * 获取连接超时时间（毫秒）
     * 
     * @return 连接超时毫秒数
     */
    public long getConnectTimeoutMillis() {
        return connectTimeout;
    }
    
    /**
     * 业务线程池类型枚举
     * 
     * 定义不同类型的线程池策略，每种类型适用于不同的业务场景：
     * 
     * - FIXED: 固定大小线程池，线程数恒定，适合负载稳定的场景
     * - CACHED: 缓存线程池，动态调整线程数，适合负载波动的场景  
     * - SCHEDULED: 定时调度线程池，支持延迟和周期性任务
     * - CUSTOM: 自定义线程池，允许完全定制线程池行为
     */
    public enum ThreadPoolType {
        
        /**
         * 固定大小线程池 (FixedThreadPool)
         * 
         * 特点：
         * - 线程数量固定不变
         * - 任务队列无界（LinkedBlockingQueue）
         * - 线程复用，无创建销毁开销
         * 
         * 适用场景：
         * - 负载相对稳定的服务
         * - 可以预估并发量的业务
         * - 需要控制资源消耗的环境
         * 
         * 注意：队列无界可能导致内存问题
         */
        FIXED,
        
        /**
         * 缓存线程池 (CachedThreadPool)
         * 
         * 特点：
         * - 线程数量动态调整(0 ~ Integer.MAX_VALUE)
         * - 空闲线程60秒后回收
         * - 使用SynchronousQueue，直接传递任务
         * 
         * 适用场景：
         * - 负载波动较大的服务
         * - 大量短时间异步任务
         * - 峰值并发不可预估的业务
         * 
         * 注意：极端情况下可能创建大量线程
         */
        CACHED,
        
        /**
         * 定时调度线程池 (ScheduledThreadPool)
         * 
         * 特点：
         * - 支持延迟执行和周期性执行
         * - 核心线程数固定，最大线程数无限
         * - 使用DelayedWorkQueue优化延迟任务
         * 
         * 适用场景：
         * - 需要定时任务的业务
         * - 延迟处理逻辑
         * - 周期性监控和清理任务
         */
        SCHEDULED,
        
        /**
         * 自定义线程池 (Custom ThreadPool)
         * 
         * 特点：
         * - 完全自定义线程池参数
         * - 可以指定队列类型和大小
         * - 可以自定义拒绝策略和线程工厂
         * 
         * 适用场景：
         * - 有特殊性能要求的业务
         * - 需要精确控制线程池行为
         * - 标准线程池无法满足需求的场景
         */
        CUSTOM
    }
    
    @Override
    public String toString() {
        return "NetworkConfig{" +
                "serverPort=" + serverPort +
                ", bossThreads=" + bossThreads +
                ", workerThreads=" + workerThreads +
                ", businessThreads=" + businessThreads +
                ", connectTimeout=" + connectTimeout +
                ", requestTimeout=" + requestTimeout +
                ", heartbeatEnabled=" + heartbeatEnabled +
                ", heartbeatInterval=" + heartbeatInterval +
                ", heartbeatTimeout=" + heartbeatTimeout +
                ", connectionPoolEnabled=" + connectionPoolEnabled +
                ", maxConnectionsPerAddress=" + maxConnectionsPerAddress +
                '}';
    }
} 