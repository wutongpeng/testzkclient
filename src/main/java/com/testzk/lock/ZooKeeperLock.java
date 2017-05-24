package com.testzk.lock;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;

public class ZooKeeperLock {

	private JZooKeeper zk = null;

	private String waitPath;

	private String thisPath;

	private String key;

	private final CountDownLatch lockAcquiredSignal = new CountDownLatch(1);

	public final static String SHAREDLOCK = "sharedLock";

	public final static String EXCLUSIVELOCK = "exclusivelock";

	public ZooKeeperLock() {

	}

	public boolean lock(String key,boolean isWait) throws Exception {
		return lock(key, isWait, ZooKeeperLock.EXCLUSIVELOCK);
	}

	/**
	 * 加锁方法
	 * @param key 加锁的数据的主键值
	 * @param isWait 是否需要等待锁资源
	 * @param lockType 锁类型
	 * @return 返回boolean类型
	 * @throws Exception
	 */
	public boolean lock(String key,boolean isWait, String lockType) throws Exception {
		if (lockType.equals(ZooKeeperLock.EXCLUSIVELOCK)) {
			return exclusiveLock(key, isWait);
		} else {
			return false;
		}
	}

	/**
	 * 
	 * @param key
	 * @param isWait
	 * @param lockType
	 * @return
	 * @throws Exception
	 */
	private boolean exclusiveLock( String key,
			boolean isWait) throws Exception {
		boolean lockSuccess = false;
		lockSuccess = doLock( key, isWait);
		// 如果没有获取到锁，需要等待锁资源
		if (!lockSuccess && isWait) {
			// 等待锁最长时间为JZooKeeper.lockWaitTime秒
			lockSuccess = lockAcquiredSignal.await(JZooKeeper.lockWaitTime,
					TimeUnit.SECONDS);
		}
		return lockSuccess;
	}

	private boolean doLock( String key, boolean isWait) {
		try {
			this.key = key;
			// 先创建当前路径
			createPath();
			// 检查是否获取到了锁
			return checkGetLock(isWait);
		} catch (Exception ex) {

		}
		return false;
	}

	public void releaseLock() throws InterruptedException, KeeperException {
		JZooKeeper.close(zk);
	}

	private void createPath() throws Exception {
		try {
			zk = JZooKeeper.getConnection(new lockWatcher());
			// 首先创建xtbh
			if (zk.exists("/zookeeper" , true) == null) {
				String tablepath = zk.create("/zookeeper", null,
						Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
				System.out.println(tablepath);
			}
			thisPath = zk.create("/zookeeper" + "/" + key,
					getCurrentDate().getBytes(), Ids.OPEN_ACL_UNSAFE,
					CreateMode.EPHEMERAL_SEQUENTIAL);
		} catch (Exception ex) {
			throw ex;
		}
	}

	private boolean checkGetLock(boolean isWait) throws Exception {
		List<String> childrenNodes = zk.getChildren("/zookeeper",
				false);

		List<String> OneData = getOneDataPath(childrenNodes, key);

		if (OneData.size() == 0) {
			throw new Exception("节点不存在了！");
		}
		// 列表中只有一个子节点, 那肯定就是thisPath, 说明client获得锁
		if (childrenNodes.size() == 1) {
			getLockSuccess();
			return true;
		} else {
			String thisNode = thisPath.substring(("/zookeeper").length() + 1);
			// 排序
			Collections.sort(OneData);
			int index = OneData.indexOf(thisNode);
			if (index == -1) {
				throw new Exception("节点不存在了！");
			} else if (index == 0) {
				// inddx == 0, 说明thisNode在列表中最小, 当前client获得锁
				getLockSuccess();
				return true;
			} else {
				// 获得排名比thisPath前1位的节点
				String waitLockTime = null;
				String fristLockTime = null;
				waitPath = "/zookeeper/"
						+ OneData.get(index - 1);
				// 在waitPath上注册监听器, 当waitPath被删除时, zookeeper会回调监听器的process方法
				if (isWait) {
					waitLockTime = new String(zk.getData(waitPath, true,
							new Stat()));
				}
				// 如果加锁的数据时间超过了限制，需要将其删除
				String fristPath = "/zookeeper/" 
						+ OneData.get(0);
				if (waitPath.equals(fristPath)) {
					fristLockTime = waitLockTime;
				} else {
					fristLockTime = new String(zk.getData(fristPath, null,
							new Stat()));
				}
				if (calcSeconds(fristLockTime, getCurrentDate()) > JZooKeeper.lockTimeout) {
					zk.delete(fristPath, -1);
				}
				return false;
			}
		}
	}

	private List<String> getOneDataPath(List<String> list, String key) {
		List<String> oneDataList = new ArrayList<String>();
		for (String ele : list) {
			if (ele.indexOf(key) >= 0) {
				oneDataList.add(ele);
			}
		}
		return oneDataList;
	}

	private void getLockSuccess() {
		lockAcquiredSignal.countDown();
	}

	private String getCurrentDate() {
		// 下面取服务器计算机的当前时间
		SimpleDateFormat Fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return Fmt.format(new Date());
	}

	class lockWatcher extends JWatcher {
		@Override
		public void process(WatchedEvent event) {
			try {
				// 连接建立时, 打开latch, 唤醒wait在该latch上的线程
				if (event.getState() == KeeperState.SyncConnected) {
					System.out.println("连接成功！");
					connectedLatch.countDown();
				}
				// 发生了删除事件
				if (event.getType() == EventType.NodeDeleted
						&& event.getPath().equals(waitPath)) {
					// 检查是否获取到了锁
					checkGetLock(true);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
     *   得到传入时间字符串的相差秒数
     *   @param   startDate   开始时间
     *   @param   endDate   结束时间
     *   @return   long   相差秒数
     *   @throws   ParseException
     * 
     */
    public static long calcSeconds(String startDate, String endDate) throws
        ParseException {
      String format = "yyyy-MM-dd HH:mm:ss";
      SimpleDateFormat sf = new SimpleDateFormat(format);
      Date sDate = null;
      Date eDate = null;

      sDate = sf.parse(startDate);
      eDate = sf.parse(endDate);
      Calendar c = Calendar.getInstance();

      c.setTime(sDate);
      long ls = c.getTimeInMillis();
      c.setTime(eDate);
      long le = c.getTimeInMillis();
      //计算秒
      return (int) ( (le - ls) / 1000);
    }
}
