package com.lyhistory.rce.fastjson;

import java.io.Serializable;
import java.util.List;

public class TargetObj {
	public String ccy;
	public long NetOptionValue;
	public long SpanInterComCredit;
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "NetOptionValue="+String.valueOf(NetOptionValue)+" SpanInterComCredit="+String.valueOf(SpanInterComCredit);
	}
}

