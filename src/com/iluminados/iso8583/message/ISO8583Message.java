package com.iluminados.iso8583.message;

import org.jpos.iso.ISOMsg;

public interface ISO8583Message {
	public static final String ADMINISTRATIVE_MTI = "0800";
	public static final String DEFAULT_OPER_NII= "5";
	
	public ISOMsg getResponse();
	public void setResponse(ISOMsg response);
	public String getResponseCode();	

}
