package com.ssrpc.transport.codec.spi;

import com.ssrpc.core.spi.ExtensionLoader;
import com.ssrpc.serialization.Serializer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 协议序列化器工厂
 * 
 * 负责创建和管理协议序列化器，将核心序列化模块适配到传输层协议需求
 * 使用适配器模式解决接口差异问题
 *
 * @author chenzhang
 * @since 1.0.0
 */
public class SerializerFactory {
    
    private static final Map<Byte, ProtocolSerializer> PROTOCOL_SERIALIZER_MAP = new ConcurrentHashMap<>();
    private static final Map<String, ProtocolSerializer> SERIALIZER_MAP_BY_NAME = new ConcurrentHashMap<>();
    
    // 序列化器类型映射：协议类型 -> 核心序列化器名称
    private static final Map<Byte, String> TYPE_MAPPING = new ConcurrentHashMap<>();
    
    static {
        // 初始化类型映射
        TYPE_MAPPING.put((byte) 1, "jdk");    // JDK 序列化
        TYPE_MAPPING.put((byte) 2, "json");   // JSON 序列化
        // 注意：kryo 和 protobuf 序列化器暂未实现，待后续添加
        // TYPE_MAPPING.put((byte) 3, "kryo");   // Kryo 序列化（待实现）
        // TYPE_MAPPING.put((byte) 4, "protobuf"); // Protobuf 序列化（待实现）
        
        // 初始化协议序列化器
        initializeProtocolSerializers();
    }
    
    /**
     * 初始化协议序列化器
     */
    private static void initializeProtocolSerializers() {
        ExtensionLoader<Serializer> extensionLoader = ExtensionLoader.getExtensionLoader(Serializer.class);
        
        for (Map.Entry<Byte, String> entry : TYPE_MAPPING.entrySet()) {
            byte protocolType = entry.getKey();
            String serializerName = entry.getValue();
            
            try {
                Serializer coreSerializer = extensionLoader.getExtension(serializerName);
                if (coreSerializer != null) {
                    ProtocolSerializer protocolSerializer = new ProtocolSerializerAdapter(coreSerializer, protocolType);
                    register(protocolSerializer);
                }
            } catch (Exception e) {
                // 序列化器不存在或加载失败，跳过
                System.err.println("Failed to load serializer: " + serializerName + ", error: " + e.getMessage());
            }
        }
    }
    
    /**
     * 注册协议序列化器
     *
     * @param serializer 协议序列化器实例
     */
    public static void register(ProtocolSerializer serializer) {
        PROTOCOL_SERIALIZER_MAP.put(serializer.getType(), serializer);
        SERIALIZER_MAP_BY_NAME.put(serializer.getClass().getSimpleName(), serializer);
    }
    
    /**
     * 根据类型获取协议序列化器
     *
     * @param type 序列化器类型
     * @return 协议序列化器实例
     */
    public static ProtocolSerializer getSerializer(byte type) {
        ProtocolSerializer serializer = PROTOCOL_SERIALIZER_MAP.get(type);
        if (serializer == null) {
            throw new IllegalArgumentException("Protocol serializer not found for type: " + type);
        }
        return serializer;
    }
    
    /**
     * 根据名称获取协议序列化器
     *
     * @param name 序列化器名称
     * @return 协议序列化器实例
     */
    public static ProtocolSerializer getSerializerByName(String name) {
        ProtocolSerializer serializer = SERIALIZER_MAP_BY_NAME.get(name);
        if (serializer == null) {
            throw new IllegalArgumentException("Protocol serializer not found for name: " + name);
        }
        return serializer;
    }
    
    /**
     * 获取默认协议序列化器（JSON）
     *
     * @return 默认协议序列化器实例
     */
    public static ProtocolSerializer getDefaultSerializer() {
        return getSerializer((byte) 2); // 默认使用JSON序列化
    }
    
    /**
     * 检查是否支持指定类型的序列化器
     *
     * @param type 序列化器类型
     * @return true表示支持，false表示不支持
     */
    public static boolean isSupported(byte type) {
        return PROTOCOL_SERIALIZER_MAP.containsKey(type);
    }
} 