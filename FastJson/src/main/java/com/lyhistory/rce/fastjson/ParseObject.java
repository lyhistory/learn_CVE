package com.lyhistory.rce.fastjson;

import java.io.Serializable;
import java.util.List;

public class ParseObject extends Object
implements Serializable{
	private String oper;
	private List<String> topics;
	public String getOper() {
		return oper;
	}
	public void setOper(String oper) {
		this.oper = oper;
	}
	public List<String> getTopics() {
		return topics;
	}
	public void setTopics(List<String> topics) {
		this.topics = topics;
	}
	
}
