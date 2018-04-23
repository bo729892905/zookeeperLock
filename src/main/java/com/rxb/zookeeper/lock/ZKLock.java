package com.rxb.zookeeper.lock;

import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Created by ren.xiaobo on 2018/4/23.
 */
public class ZKLock implements Lock {
    /**
     * slf4j日志
     */
    private Logger logger = LoggerFactory.getLogger(getClass());
    /**
     * zk的客户端
     */
    private ZkClient zkClient;
    /**
     * 分布式锁在zk的根节点
     */
    private String rootpath;
    /**
     * 创建顺序非持久化节点的路径
     */
    private String current;
    /**
     * 当前节点的前一个节点，用于订阅其删除事件
     */
    private String previous;
    /**
     * 路径斜杠的常量
     */
    private final String SEPARATOR = "/";

    public ZKLock(ZkClient zkClient, String rootpath) {
        this.zkClient = zkClient;
        if (rootpath == null) throw new IllegalArgumentException("param rootPath cant't be null!");
        if (!rootpath.startsWith(SEPARATOR)) {
            rootpath = SEPARATOR + rootpath;
        }
        if (rootpath.endsWith(SEPARATOR)) {
            rootpath = rootpath.substring(0, rootpath.length() - 1);
        }
        this.rootpath = rootpath;

        /* 如果rootpath不存在，则创建rootpath，高并发情况下会被其他线程创建,需要捕捉创建异常 */
        if (!zkClient.exists(rootpath)) {
            try {
                zkClient.createPersistent(rootpath, true);
            } catch (ZkNodeExistsException e) {
                logger.warn("znode has been created by other thead or processes!");
            }
        }
    }

    @Override
    public void lock() throws Exception {
        /* 创建zk的非持久化顺序节点，返回当前的绝对路径 */
        current = zkClient.createEphemeralSequential(rootpath + SEPARATOR, "");
        logger.info("created current path is {}", current);
        /* 获取当前的顺序 */
        String currentSub = current.substring(current.lastIndexOf(SEPARATOR) + 1);
        /* 获取根节点下所有的子节点 */
        List<String> children = zkClient.getChildren(rootpath);
        /* 子节点排序 */
        children.sort(Comparator.comparingLong(Long::parseLong));
        /* 获取最小的一个，为最先创建的节点 */
        String min = children.get(0);
        if (!currentSub.equals(min)) {
            logger.info("this node should wait for other earlier one completed!");
            for (int i = 0; i < children.size(); i++) {
                if (currentSub.equals(children.get(i))) {
                    previous = children.get(i - 1);
                }
            }
            /*一般情况不会有这种现象，除非人为操作zk*/
            if (previous == null)
                throw new RuntimeException("current node has no previous,current path is " + rootpath);
            previous = rootpath + SEPARATOR + previous;

            /*订阅前节点的删除信息，线程进入等待*/
            waitUntilLock();
        } else {
            logger.info("current node {} lock success,thread is {}", current, Thread.currentThread().getName());
        }
    }

    private void waitUntilLock() throws Exception {
        /*同步计数器*/
        CountDownLatch countDownLatch = new CountDownLatch(1);
        LockZKDataListener listener = new LockZKDataListener(countDownLatch, zkClient, previous);
        zkClient.subscribeDataChanges(previous,listener);
        /*高并发下必须判断是否存在前节点否则就会造成死锁*/
        if (!zkClient.exists(previous)) {
            logger.info("{} lock success,current thread is {}", current, Thread.currentThread().getName());
            listener.handleDataDeleted("");
        }else {
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                logger.error("wait InterruptedException", e);
            }
        }
    }

    @Override
    public void unLock() {
        /*删除当前节点，下一个节点订阅当前事件后获取锁*/
        zkClient.delete(current);
        logger.info("{} unlock success,current thread is {}", current, Thread.currentThread().getName());
    }
}
