package com.lyhistory.rce.fastjson;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TargetObjComplex implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public List<TargetObj> items=new ArrayList<TargetObj>();
	public String message;
}