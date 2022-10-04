package com.iluminados.iso8583.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.iluminados.util.BCD;

public class SubFieldFormatter {

	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	public SubFieldFormatter() {		
	}
	
	public byte[] pack() {
		return baos.toByteArray();
	}

	public List<SubField> unpack() {
		ArrayList<SubField> list = new ArrayList();
		byte[] rawData = baos.toByteArray();
		byte[] lenBytes = new byte[2];
		byte[] idBytes = new byte[2];
		
		int index = 0;
		while (index < rawData.length) {
			SubField sf = new SubField();
			lenBytes[0] = rawData[index++];
			lenBytes[1] = rawData[index++];
			sf.setLen(BCD.BCDToDecimal(lenBytes));
			sf.setId(new String(rawData, index, 2));
			index += 2;
			byte[] dataBytes = new byte[(int)sf.getLen()-2];
			System.arraycopy(rawData, index, dataBytes, 0, (int)sf.getLen()-2);
			sf.setData(dataBytes);			
			index += (sf.getLen()-2);
			
			list.add(sf);
		}
		
		return list;
	}
	
	public void setBytes(byte[] subFieldData) throws IOException {
		baos.reset();
		baos.write(subFieldData);
	}
	
	public void addField(String id, String data) throws IOException {
		byte [] sfLenBytes = BCD.DecimalToBCD(id.length() + data.length() );
		
		baos.write(sfLenBytes);
		baos.write(id.getBytes());
		baos.write(data.getBytes());		
	}
	
	public void addField(String id, int data, int len) throws IOException {
		
		byte [] dataBytes = BCD.DecimalToBCD(data, len);
		byte [] sfLenBytes = BCD.DecimalToBCD(id.length() + len );
		
		baos.write(sfLenBytes);
		baos.write(id.getBytes());
		baos.write(dataBytes);
	}
	
	public void addField(String id, byte[] data) throws IOException {
		addField(id, data, data.length);
	}
	
	public void addField(String id, byte[] data, int len) throws IOException {
		
		byte [] sfLenBytes = BCD.DecimalToBCD(id.length() + len );
		
		baos.write(sfLenBytes);
		baos.write(id.getBytes());
		baos.write(data);
	}
	
	public void clear() {
		baos.reset();
	}
	
}
