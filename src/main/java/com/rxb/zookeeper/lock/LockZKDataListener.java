package com.rxb.zookeeper.lock;

import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;

import java.util.concurrent.CountDownLatch;

/**
 * zk的监听器
 *
 * Created by ren.xiaobo on 2018/4/23.
 */
public class LockZKDataListener implements IZkDataListener {
    private CountDownLatch countDownLatch;

    private ZkClient zkClient;

    private String previous;

    public LockZKDataListener(CountDownLatch countDownLatch, ZkClient zkClient, String previous) {
        this.countDownLatch = countDownLatch;
        this.zkClient = zkClient;
        this.previous = previous;
    }

    @Override
    public void handleDataChange(String dataPath, Object data) throws Exception {
    }

    @Override
    public void handleDataDeleted(String dataPath) throws Exception {
        countDownLatch.countDown();
        zkClient.unsubscribeDataChanges(previous,this);
    }
}
