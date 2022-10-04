package com.iluminados.iso8583.message;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Logger;

import org.jpos.iso.ISOMsg;

public abstract class AbstractISO8583Message implements ISO8583Message {
	static final Logger log = Logger.getLogger("ISO8583");
	
	protected DateFormat dateFmt = new SimpleDateFormat("MMdd");
	protected DateFormat timeFmt = new SimpleDateFormat("hhmmss");
	
	Calendar now = Calendar.getInstance();
	AbstractISO8583Message() {
		response = null;
	}
	
	public String getDateAsString() {
		return dateFmt.format(now.getTime());
	}
	
	public String getTimeAsString() {
		return timeFmt.format(now.getTime());
	}
	
	protected ISOMsg response;
	
	@Override
	public void setResponse(ISOMsg response) {
		this.response = response;
	}
	
	@Override 
	public ISOMsg getResponse() {
		return response;
	}
	
	@Override
	public String getResponseCode() {
		return (response != null) ? response.getString(39) : null;
	}
	
	public void logISO8583Message(ISOMsg msg) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		msg.dump(ps, "\t");
		log.info(new String(baos.toByteArray()));
	}
	
	public String getPinpadSerialNumber() {		
		return "19203PP31041338";
	}
}
