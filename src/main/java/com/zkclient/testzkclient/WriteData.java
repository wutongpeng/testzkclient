package com.zkclient.testzkclient;

import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.serialize.SerializableSerializer;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

import com.Config;

public class WriteData {

	public static void main(String[] args) {
		ZkClient zc = new ZkClient(Config.connectString,10000,10000,new SerializableSerializer());
		System.out.println("conneted ok!");
		
		
		User u = new User();
		u.setId(2);
		u.setName("test2");
		zc.writeData("/node_5", u, 1);
		
	}
	
}
