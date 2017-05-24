package com.testzk.lock;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Main {

	public static void main(String[] args) throws Exception {
		ZooKeeperLock zkLock = new ZooKeeperLock();
		// 加锁处理
		boolean b = zkLock.lock("lock", true);
		if(b){
			System.out.println("加锁成功-"+getTime());
			Thread.sleep(5000);
			zkLock.releaseLock();
			System.out.println("释放锁成功-"+getTime());
		}else{
			System.out.println("加锁失败-"+getTime());
		}
	}
	
	public static String getTime(){
		 String format = "yyyy-MM-dd HH:mm:ss";
	     SimpleDateFormat sf = new SimpleDateFormat(format);
	     return sf.format(new Date());
	}

}
