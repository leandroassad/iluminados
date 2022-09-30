package com.iluminados;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.jpos.iso.AsciiHexInterpreter;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

import com.iluminados.iso8583.ISO8583MessageMap;
import com.iluminados.iso8583.NSUManager;
import com.iluminados.util.BCD;
import com.iluminados.util.Util;

public class Iluminados {

	public Iluminados() {
	}
	
	protected DateFormat dateFmt = new SimpleDateFormat("MMdd");
	protected DateFormat timeFmt = new SimpleDateFormat("hhmmss");
	
	Calendar now = Calendar.getInstance();
	
	
	public void testeNSU() {
		System.out.println("NSU Atual: " + NSUManager.getInstance().getNextNSU());
	}
	
	/**
	 * Metodo para empacotar uma mensagem ISO8583 a partir dos campos setados
	 */
	public void testePackISO8583() {
		try {
			// Instancia um objet ISOMsg e seta o Packager da Valecard para ela
			ISOMsg msg = new ISOMsg();
			msg.setPackager(ISO8583MessageMap.getISO8583MessageMap().getPackager());
			
			msg.setMTI("0800");					// MTI 0800 para administrativas
			msg.set(3, "990000");
			msg.set(11, String.valueOf(NSUManager.getInstance().getNextNSU()));
			msg.set(12, timeFmt.format(now.getTime()));
			msg.set(13, dateFmt.format(now.getTime()));
			msg.set(24, "5");
			msg.set(41, "BW000017");
			msg.set(42, "BW000017");
			
			byte[] isoBytes = msg.pack();		// Empacota e devolve os bytes da mensagem
			
			System.out.println("Mensagem ISO RAW = " + BCD.BCDtoString(isoBytes));
			logISO8583Message(msg);
			
		} catch (ISOException e) {
			e.printStackTrace();
		}
	}
	
	public void testMSBLSB() {
		int x = 1234;
		byte b[] = new byte[2];
		
		Util.intToByte(x, b, Util.MSB_LSB_ORDER);
		System.out.println("Saida: Inteiro: 1234 =" + String.format("[%04X]", x) + " em MSB_LSB = " + String.format("[%02X][%02X]", b[0], b[1]));
		Util.intToByte(x, b, Util.LSB_MSB_ORDER);
		System.out.println("Saida: Inteiro: 1234 =" + String.format("[%04X]", x) + " em LSB_MSB = " + String.format("[%02X][%02X]", b[0], b[1]));
	}
	
	public void testeUnpackISO8583(String message) {
		try {
		// Converter a string de mensagem para bytes
		// Este passo não é feito quando a msg chega do TX
		AsciiHexInterpreter aih = AsciiHexInterpreter.INSTANCE;
		byte [] messageBytes = aih.uninterpret(message.getBytes(), 0, message.length()/2);
		
		// Criar uma ISOMsg com o packager da Valecard
		ISOMsg msg = new ISOMsg();
		msg.setPackager(ISO8583MessageMap.getISO8583MessageMap().getPackager());
		
		// Desempacotar a mensagem (bytes)
		msg.unpack(messageBytes);

		// Imprimir o resultado
		System.out.println("ISO8583 Message RAW = " + BCD.BCDtoString(messageBytes));
		logISO8583Message(msg);
		
		} catch (ISOException e) {
			e.printStackTrace();
		}
	}
	
	public void testeComunicacao() {
		
	}
	
	public void logISO8583Message(ISOMsg msg) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		msg.dump(ps, "\t");
		System.out.println(new String(baos.toByteArray()));
	}
}
