package com.iluminados.iso8583;

import java.io.InputStream;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOPackager;
import org.jpos.iso.packager.GenericPackager;

public class ISO8583MessageMap {

	protected static ISO8583MessageMap map = null;
	protected ISO8583MessageMap() {
	}
	
	String isoMapFilename;
	public static ISO8583MessageMap getISO8583MessageMap() {
		if (map == null) {
			map = new ISO8583MessageMap();
			map.setMap("/com/iluminados/iso8583/ValecardMessageMap.xml");
		}
		
		return map;
	}
			
	public InputStream getMap() {
		return this.getClass().getResourceAsStream(isoMapFilename);
	}
	
	public void setMap(String isoMapFilename) {
		this.isoMapFilename = isoMapFilename;
	}
	
	ISOPackager packager = null; 
	public ISOPackager getPackager() throws ISOException {
		if (packager == null)
			packager = new GenericPackager(getMap());
		
		return packager;
	}
}
