package com.iluminados.iso8583.message;

import lombok.Data;

@Data
public class SubField {
	private long len;
	private String id;
	private byte[] data;
	
	public SubField() {
		
	}
	
	public SubField(int len, String id, byte[] data) {
		this.len = len;
		this.id = id;
		this.data = data;
	}
}
