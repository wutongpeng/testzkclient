package com.zkclient.testzkclient;

import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.serialize.SerializableSerializer;

import com.Config;

public class CreateSession {

	public static void main(String[] args) {
		ZkClient zc = new ZkClient(Config.connectString,10000,10000,new SerializableSerializer());
		System.out.println("conneted ok!");
	}
	
}
