package com.zk.lock;

import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.Watcher.Event.KeeperState;

public class JWatcher implements Watcher {
	public CountDownLatch connectedLatch;
	@Override
	public void process(WatchedEvent event){
        try {
            // 连接建立时, 打开latch, 唤醒wait在该latch上的线程
            if (event.getState() == KeeperState.SyncConnected) {
            	System.out.println("连接成功！");
            	connectedLatch.countDown();
            }
            //发生了删除事件
            if (event.getType() == EventType.NodeDeleted){
            	delete();
            }
            //发生了新增加
            if (event.getType() == EventType.NodeCreated){
            	add();
            }
            //发生了更新
            if (event.getType() == EventType.NodeDataChanged){
            	update();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
	/**
	 * 
	 */
	public void delete(){
		System.out.println("执行了删除！");
	}
	
	public void add(){
		System.out.println("执行了新增加！");
	}
	
	public void update(){
		System.out.println("执行了更新！");
	}
	
	public CountDownLatch getConnectedLatch() {
		return connectedLatch;
	}
	public void setConnectedLatch(CountDownLatch connectedLatch) {
		this.connectedLatch = connectedLatch;
	}

}
