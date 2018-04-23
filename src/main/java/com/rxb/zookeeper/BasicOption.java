package com.rxb.zookeeper;

import org.I0Itec.zkclient.ZkClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Created by ren.xiaobo on 2018/4/23.
 */
public class BasicOption {
    private ZkClient zk;

    @Before
    public void connection() {
        String connectionString = "39.105.24.60:2181,39.105.24.60:3181,39.105.24.60:4181";
        int sessionTimeout = 30000;
        int connectionTimeout = 30000;
        try {
            zk = new ZkClient (connectionString, sessionTimeout,connectionTimeout);
        } catch (Exception e) {
            System.out.println("连接服务器失败！");
        }

    }

    @Test
    public void test() {
        String path = "/test";

        try {
            //创建节点
            if (!zk.exists(path )) {
                zk.createPersistent(path,"test");
                System.out.println("create node " + path + " success!");
            } else {
                System.out.println("this path is exists!");
            }

            //获取节点
            String data=zk.readData(path);
            System.out.println("get data :" + data);


            //修改节点
            zk.writeData(path,"test_modify");
            data=zk.readData(path);
            System.out.println("get data :" + data);

            //创建子节点
            zk.createPersistent(path+ "/child1");
            zk.createPersistent(path+ "/child2");

            //获取子节点
            List<String> children = zk.getChildren(path);
            System.out.println("遍历子节点 ======>");
            children.forEach(System.out::println);
            System.out.println();

            //删除节点
            zk.delete(path + "/child1");
            zk.delete(path + "/child2");
            zk.delete(path);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @After
    public void close() {
        try {
            if (zk != null) {
                zk.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
