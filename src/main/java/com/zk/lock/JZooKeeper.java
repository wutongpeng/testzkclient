package com.zk.lock;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import com.Config;

public class JZooKeeper extends ZooKeeper{
	public static String connectString;
	public static int sessionTimeout;
	public static int lockTimeout;
	public static int lockWaitTime;
	
	private JZooKeeper(Watcher watch)
			throws IOException {
		super(connectString, sessionTimeout, watch);
	}
	
	private JZooKeeper(Watcher watch,
			boolean canBeReadOnly) throws IOException {
		super(connectString,sessionTimeout,watch,canBeReadOnly);
	}

	private JZooKeeper(Watcher watch,
			long sessionId, byte[] sessionPasswd) throws IOException {
		super(connectString, sessionTimeout, watch,sessionId,  sessionPasswd);
	}

	private JZooKeeper(Watcher watch,
			long sessionId, byte[] sessionPasswd, boolean canBeReadOnly)
			throws IOException {
		super( connectString,sessionTimeout, watch,sessionId,sessionPasswd,canBeReadOnly);
	}
	
	public static JZooKeeper getConnection(JWatcher watcher) throws Exception{
		JZooKeeper zk = null;
		if(initZookeeperServer()){
			CountDownLatch connectedLatch = new CountDownLatch(1);
			watcher.setConnectedLatch(connectedLatch);
			zk =  new JZooKeeper(watcher);
			waitUntilConnected(connectedLatch,zk);
		}
		return zk;
	}
	
	public static JZooKeeper getConnection(JWatcher watcher,
			boolean canBeReadOnly) throws Exception{
		JZooKeeper zk = null;
		if(initZookeeperServer()){
			CountDownLatch connectedLatch = new CountDownLatch(1);
			watcher.setConnectedLatch(connectedLatch);
			zk =  new JZooKeeper(watcher,canBeReadOnly);
			waitUntilConnected(connectedLatch,zk);
		}
		return zk;
	}
	
	public static JZooKeeper getConnection(JWatcher watcher,
			long sessionId, byte[] sessionPasswd) throws Exception{
		JZooKeeper zk = null;
		if(initZookeeperServer()){
			CountDownLatch connectedLatch = new CountDownLatch(1);
			watcher.setConnectedLatch(connectedLatch);
			zk =  new JZooKeeper(watcher,sessionId,sessionPasswd);
			waitUntilConnected(connectedLatch,zk);
		}
		return zk;
	}
	
	public static JZooKeeper getConnection(JWatcher watcher,
			long sessionId, byte[] sessionPasswd, boolean canBeReadOnly) throws Exception{
		JZooKeeper zk = null;
		if(initZookeeperServer()){
			CountDownLatch connectedLatch = new CountDownLatch(1);
			watcher.setConnectedLatch(connectedLatch);
			zk =  new JZooKeeper(watcher,sessionId,sessionPasswd,canBeReadOnly);
			waitUntilConnected(connectedLatch,zk);
		}
		return zk;
	}
	
	public static boolean close(JZooKeeper zk) {
        if (zk != null) {
            if (States.CLOSED != zk.getState()) {
                try {
                    zk.close();
                } catch (InterruptedException e) {
                    return false;
                }
            }
        }
        return true;
    }
	
	private static void waitUntilConnected(CountDownLatch connectedLatch,ZooKeeper zk) {
        if (States.CONNECTING == zk.getState()) {
            try {
                connectedLatch.await();
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
    }
	
	/**
	 * 初始化zk的地址列表和超时时间
	 * @return
	 */
	private static boolean initZookeeperServer() {
		if(null != JZooKeeper.connectString && !"".equals(JZooKeeper.connectString)){
			return true;
		}
	   
		String hosts = null;
	    String timeout = null;
	    String lockTime = null;
	    String waitTime = null;
	   
		hosts = Config.connectString;
		timeout = "50000";
		lockTime = "60";
		waitTime = "20";
	    	
	    
	    
	    if("".equals(hosts.toString().trim())){
	    	return false;
	    }else{
	    	JZooKeeper.connectString = hosts.toString();
	    	
		    try{
		    	JZooKeeper.sessionTimeout = Integer.parseInt(timeout);
		    }catch(Exception ex){
		    	//默认ZK心跳时间50000
		    	JZooKeeper.sessionTimeout = 50000;
		    }
		    
		    try{
		    	JZooKeeper.lockTimeout = Integer.parseInt(lockTime);
		    }catch(Exception ex){
		    	//通过ZK加锁的时间默认最长60秒，60秒之后自动删锁
		    	JZooKeeper.lockTimeout = 60;
		    }
		    
		    try{
		    	JZooKeeper.lockWaitTime = Integer.parseInt(waitTime);
		    }catch(Exception ex){
		    	//等待锁的最长时间，如果超过时间，返回fasle
		    	JZooKeeper.sessionTimeout = 20;
		    }
	    }
	    return true; 
  }
	
}
