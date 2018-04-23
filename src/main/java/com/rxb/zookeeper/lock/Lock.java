package com.rxb.zookeeper.lock;

/**
 * Created by ren.xiaobo on 2018/4/23.
 */
public interface Lock {
    void lock() throws Exception;

    void unLock();
}
