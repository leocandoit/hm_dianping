package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 基于Redis的简单分布式锁实现
 * 
 * 功能特点：
 * 1. 互斥性：同一时刻只有一个线程能获取锁
 * 2. 防误删：只有加锁的线程才能释放锁
 * 3. 原子性：使用Lua脚本保证释放锁操作的原子性
 * 4. 超时释放：支持锁的自动过期，防止死锁
 */
public class SimpleRedisLock implements ILock {

    /**
     * 锁的名称，用于区分不同的业务锁
     */
    private String name;
    
    /**
     * Redis操作模板，用于执行Redis命令
     */
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 构造函数
     * @param name 锁的名称
     * @param stringRedisTemplate Redis操作模板
     */
    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * Redis锁的key前缀，用于统一管理锁相关的key
     */
    private static final String KEY_PREFIX = "lock:";
    
    /**
     * 线程标识前缀，每个JVM实例都有唯一的UUID前缀
     * 格式：UUID-线程ID，确保不同JVM中的线程标识不会冲突
     */
    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";
    
    /**
     * 释放锁的Lua脚本，静态加载保证性能
     * 使用Lua脚本的原因：保证"比较标识+删除锁"操作的原子性
     */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        // 设置Lua脚本文件位置（类路径下的unlock.lua文件）
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        // 设置脚本返回值类型为Long（Lua脚本中del命令返回删除的key数量）
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    /**
     * 尝试获取分布式锁（非阻塞）
     * 
     * @param timeoutSec 锁的超时时间（秒），超时后自动释放锁
     * @return true-获取锁成功，false-获取锁失败
     */
    @Override
    public boolean tryLock(long timeoutSec) {
        // 生成当前线程的唯一标识：UUID前缀 + 线程ID
        // 这样可以确保不同JVM、不同线程的标识都是唯一的
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        
        // 使用Redis的SET命令尝试获取锁
        // setIfAbsent相当于：SET key value NX EX timeout
        // NX：只有key不存在时才设置
        // EX：设置过期时间（秒）
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + name, threadId, timeoutSec, TimeUnit.SECONDS);
        
        // 避免自动拆箱时的空指针异常，使用Boolean.TRUE.equals()进行比较
        return Boolean.TRUE.equals(success);
    }

    /**
     * 释放分布式锁
     * 
     * 使用Lua脚本保证操作的原子性：
     * 1. 比较锁中存储的线程标识与当前线程标识是否一致
     * 2. 如果一致则删除锁，如果不一致则不做任何操作
     * 
     * 这样可以防止误删其他线程的锁
     */
    @Override
    public void unlock() {
        // 执行Lua脚本释放锁
        // 参数说明：
        // 1. UNLOCK_SCRIPT: 要执行的Lua脚本
        // 2. Collections.singletonList(KEY_PREFIX + name): 传递给脚本的KEYS数组，这里只有一个key
        // 3. ID_PREFIX + Thread.currentThread().getId(): 传递给脚本的ARGV数组，这里是当前线程标识
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX + name),
                ID_PREFIX + Thread.currentThread().getId());
    }
    /*
     * 旧版本的unlock方法实现（已废弃）
     * 
     * 问题：存在线程安全问题
     * 1. 获取锁标识
     * 2. 比较标识
     * 3. 删除锁
     * 
     * 这三个步骤不是原子操作，可能在步骤2和3之间，锁被其他线程获取，
     * 导致误删其他线程的锁。
     * 
     * 解决方案：使用Lua脚本保证操作的原子性
     */
    /*@Override
    public void unlock() {
        // 获取线程标示
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        // 获取锁中的标示
        String id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
        // 判断标示是否一致
        if(threadId.equals(id)) {
            // 释放锁
            stringRedisTemplate.delete(KEY_PREFIX + name);
        }
    }*/
}
