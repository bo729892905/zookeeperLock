package com.rxb.zookeeper;

import com.rxb.zookeeper.lock.ZKLock;
import org.I0Itec.zkclient.ZkClient;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Created by ren.xiaobo on 2018/4/23.
 */
public class LockTest {

    private static ZkClient getZkClient() {
        String connectionString = "39.105.24.60:2181,39.105.24.60:3181,39.105.24.60:4181";
        int sessionTimeout = 30000;
        int connectionTimeout = 30000;
        ZkClient zkClient = null;
        try {
            zkClient = new ZkClient(connectionString, sessionTimeout, connectionTimeout);
        } catch (Exception e) {
            System.out.println("连接服务器失败！");
        }
        return zkClient;
    }

    public static void main(String[] args) {
        AtomicInteger integer = new AtomicInteger(0);
        Stream.generate(integer::getAndIncrement).limit(100).forEach(i -> {
            int k = i;
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            new Thread(() -> {
                try {
                    ZKLock zklock = new ZKLock(getZkClient(), "/zk/timeout");
                    System.out.println(k + " lock");

                    zklock.lock();
                    Thread.sleep(2000);
                    zklock.unLock();

                    System.out.println(k + " lock end");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        });
    }
}
