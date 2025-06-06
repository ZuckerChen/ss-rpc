package com.ssrpc.core.spi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * SPI扩展加载器.
 * 
 * 负责加载和管理SPI扩展实现
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class ExtensionLoader<T> {
    
    private static final Logger log = LoggerFactory.getLogger(ExtensionLoader.class);
    
    /**
     * SPI扩展目录
     */
    private static final String SPI_DIRECTORY = "META-INF/services/";
    private static final String SPI_INTERNAL_DIRECTORY = "META-INF/ss-rpc/";
    
    /**
     * 扩展加载器缓存
     */
    private static final ConcurrentMap<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<>();
    
    /**
     * 扩展实例缓存
     */
    private static final ConcurrentMap<Class<?>, Object> EXTENSION_INSTANCES = new ConcurrentHashMap<>();
    
    /**
     * 扩展接口类型
     */
    private final Class<T> type;
    
    /**
     * 扩展实现类缓存
     */
    private final Map<String, Class<?>> extensionClasses = new ConcurrentHashMap<>();
    
    /**
     * 扩展实例缓存
     */
    private final Map<String, T> cachedInstances = new ConcurrentHashMap<>();
    
    /**
     * 默认扩展名称
     */
    private String defaultExtension;
    
    /**
     * 私有构造器
     */
    private ExtensionLoader(Class<T> type) {
        this.type = type;
        SPI spi = type.getAnnotation(SPI.class);
        if (spi != null) {
            this.defaultExtension = spi.value();
        }
    }
    
    /**
     * 获取扩展加载器
     * 
     * @param type 扩展接口类型
     * @param <T> 扩展接口类型
     * @return 扩展加载器
     */
    @SuppressWarnings("unchecked")
    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("Extension type == null");
        }
        if (!type.isInterface()) {
            throw new IllegalArgumentException("Extension type (" + type + ") is not an interface!");
        }
        if (!type.isAnnotationPresent(SPI.class)) {
            throw new IllegalArgumentException("Extension type (" + type + 
                ") is not an extension, because it is NOT annotated with @SPI!");
        }
        
        ExtensionLoader<T> loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        if (loader == null) {
            EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader<>(type));
            loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        }
        return loader;
    }
    
    /**
     * 获取扩展实例
     * 
     * @param name 扩展名称
     * @return 扩展实例
     */
    public T getExtension(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Extension name == null");
        }
        
        // 检查缓存
        T instance = cachedInstances.get(name);
        if (instance == null) {
            synchronized (cachedInstances) {
                instance = cachedInstances.get(name);
                if (instance == null) {
                    instance = createExtension(name);
                    cachedInstances.put(name, instance);
                }
            }
        }
        return instance;
    }
    
    /**
     * 获取默认扩展实例
     * 
     * @return 默认扩展实例
     */
    public T getDefaultExtension() {
        if (defaultExtension == null || defaultExtension.isEmpty()) {
            throw new IllegalStateException("No default extension defined for " + type.getName());
        }
        return getExtension(defaultExtension);
    }
    
    /**
     * 获取所有扩展名称
     * 
     * @return 扩展名称集合
     */
    public Set<String> getSupportedExtensions() {
        loadExtensionClasses();
        return Collections.unmodifiableSet(extensionClasses.keySet());
    }
    
    /**
     * 检查是否存在指定扩展
     * 
     * @param name 扩展名称
     * @return true表示存在，false表示不存在
     */
    public boolean hasExtension(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        loadExtensionClasses();
        return extensionClasses.containsKey(name);
    }
    
    /**
     * 创建扩展实例
     */
    @SuppressWarnings("unchecked")
    private T createExtension(String name) {
        Class<?> clazz = getExtensionClass(name);
        if (clazz == null) {
            throw new IllegalStateException("No such extension " + type.getName() + " by name " + name);
        }
        
        try {
            T instance = (T) EXTENSION_INSTANCES.get(clazz);
            if (instance == null) {
                EXTENSION_INSTANCES.putIfAbsent(clazz, clazz.newInstance());
                instance = (T) EXTENSION_INSTANCES.get(clazz);
            }
            return instance;
        } catch (Exception e) {
            throw new IllegalStateException("Extension instance (name: " + name + ", class: " +
                type + ") couldn't be instantiated: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取扩展类
     */
    private Class<?> getExtensionClass(String name) {
        if (type == null) {
            throw new IllegalArgumentException("Extension type == null");
        }
        if (name == null) {
            throw new IllegalArgumentException("Extension name == null");
        }
        loadExtensionClasses();
        return extensionClasses.get(name);
    }
    
    /**
     * 加载扩展类
     */
    private void loadExtensionClasses() {
        if (!extensionClasses.isEmpty()) {
            return;
        }
        
        synchronized (extensionClasses) {
            if (!extensionClasses.isEmpty()) {
                return;
            }
            
            try {
                loadDirectory(extensionClasses, SPI_DIRECTORY);
                loadDirectory(extensionClasses, SPI_INTERNAL_DIRECTORY);
            } catch (Exception e) {
                log.error("Failed to load extension classes for " + type.getName(), e);
            }
        }
    }
    
    /**
     * 从指定目录加载扩展
     */
    private void loadDirectory(Map<String, Class<?>> extensionClasses, String dir) {
        String fileName = dir + type.getName();
        try {
            Enumeration<URL> urls = ExtensionLoader.class.getClassLoader().getResources(fileName);
            if (urls != null) {
                while (urls.hasMoreElements()) {
                    URL resourceURL = urls.nextElement();
                    loadResource(extensionClasses, resourceURL);
                }
            }
        } catch (Throwable t) {
            log.error("Exception occurred when loading extension class (interface: " +
                type + ", description file: " + fileName + ").", t);
        }
    }
    
    /**
     * 加载资源文件
     */
    private void loadResource(Map<String, Class<?>> extensionClasses, URL resourceURL) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resourceURL.openStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.length() > 0 && !line.startsWith("#")) {
                    try {
                        String name = null;
                        int i = line.indexOf('=');
                        if (i > 0) {
                            name = line.substring(0, i).trim();
                            line = line.substring(i + 1).trim();
                        }
                        if (line.length() > 0) {
                            loadClass(extensionClasses, resourceURL, Class.forName(line, true, 
                                ExtensionLoader.class.getClassLoader()), name);
                        }
                    } catch (Throwable t) {
                        log.error("Failed to load extension class (interface: " + type +
                            ", class line: " + line + ") in " + resourceURL + ", cause: " + t.getMessage(), t);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Exception occurred when loading extension class (interface: " +
                type + ", class file: " + resourceURL + ") in " + resourceURL, e);
        }
    }
    
    /**
     * 加载扩展类
     */
    private void loadClass(Map<String, Class<?>> extensionClasses, URL resourceURL, Class<?> clazz, String name) {
        if (!type.isAssignableFrom(clazz)) {
            throw new IllegalStateException("Error occurred when loading extension class (interface: " +
                type + ", class line: " + clazz.getName() + "), class "
                + clazz.getName() + " is not subtype of interface.");
        }
        
        if (name == null || name.isEmpty()) {
            name = findAnnotationName(clazz);
            if (name == null || name.isEmpty()) {
                throw new IllegalStateException("No such extension name for the class " + clazz.getName() + " in the config " + resourceURL);
            }
        }
        
        String[] names = name.split(",");
        for (String n : names) {
            if (!extensionClasses.containsKey(n)) {
                extensionClasses.put(n, clazz);
            }
        }
    }
    
    /**
     * 从注解中查找扩展名称
     */
    private String findAnnotationName(Class<?> clazz) {
        // 可以通过其他注解或约定来确定扩展名称
        return clazz.getSimpleName().toLowerCase();
    }
    
    /**
     * 获取扩展接口类型
     * 
     * @return 扩展接口类型
     */
    public Class<T> getType() {
        return type;
    }
} 