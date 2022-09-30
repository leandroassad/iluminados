package com.iluminados.util;

import java.util.logging.Logger;

public class ByteUtil {
	static final Logger log = Logger.getLogger("ByteUtil");
	
	private static final String hexDigits = "0123456789ABCDEF";
	
	public static final int AS_HEX = 0;
	public static final int AS_INT = 1;
	
	/**
	 * Converts a byte array to int value
	 * @param b The byte array
	 * @return The converted value
	 */
	public static int byteArrayToInt(byte[] b)
	{
		int value = 0x0000;

		value = b[0];
		value = ((value << 8) & 0xFF00);
		value =  (value | (b[1]&0x00FF));
		
		return value;
	}

	/**
	 * Converts a byte array to short value
	 * @param b
	 * @return
	 */
	public static short byteArrayToShort(byte[] b)
	{
		short value = 0x0000;

		value = b[0];
		value = (short)((value << 8) & 0xFF00);
		value =  (short)(value | b[1]);

		return value;
	}

	
	/**
	 * Converts a byte array to a string printing each byte as Hex or Int
	 * @param b The byte array
	 * @param separator The separator between each int
	 * @param format AS_HEX or AS_INT (Default)
	 * @return
	 */
	public static String byteArrayToString(byte[] b, int nBytes, char separator, int format)
	{
		StringBuffer buffer = new StringBuffer();
		
		for (int i=0; i<nBytes; i++)
		{
			if (i > 0 && separator!=0) buffer.append(separator);
			buffer.append((format==AS_HEX)?String.format("%02X", new Integer(b[i]&0x00FF)):(short)(b[i]&0x00FF));
		}
		
		return buffer.toString();
	}
	
	public static void printBytes(String message, byte[] b, int len)
	{
		StringBuffer buffer = new StringBuffer();
		if (message != null) buffer.append(message).append('\n');
		for (int i=0; i<len; i++)
			buffer.append(Integer.toHexString(b[i]&0x00FF)).append(' ');
		
		buffer.append('\n');
		
		log.info(buffer.toString());
	}
	
	public static byte[] asByte(String hexa) throws IllegalArgumentException {

		if (hexa.length() % 2 != 0) {
			throw new IllegalArgumentException("String hexa invalida");
		}

		byte[] b = new byte[hexa.length() / 2];

		for (int i = 0; i < hexa.length(); i += 2) {
			b[i / 2] = (byte) ((hexDigits.indexOf(hexa.charAt(i)) << 4) | (hexDigits
					.indexOf(hexa.charAt(i + 1))));
		}
		return b;
	}
	
    public static byte[] intToBytes(int a) {
        byte[] ret = new byte[2];
        ret[0] = (byte) ((a >> 8) & 0xFF);
        ret[1] = (byte) (a & 0xFF);
        return ret;
    }
    
	/**
	 * Converte um byte 0-9 A-F em seu valor ascii 30-39 41-46
	 * @param b Byte a ser convertido
	 * @return byte convertido
	 */
	public static byte byteToAscii(byte b) {
		byte filler = 0x00;
		if (b >= 0x00 && b <= 0x09) filler =  0x30;
		else if (b >= 0x0A && b <= 0x0F) filler = 0x37;
		
		return (byte)(b + filler);
	}
	
	/**
	 * Converte um byte 30-39 41-46 em seu valor 0-9 A-F
	 * @param b Byte ascii a ser convertido
	 * @return byte convertido
	 */
	public static byte asciiToByte(byte b) {
		return (byte)((b>=0x30  && b<=0x39) ? (b-0x30) : (b-0x37));
	}
	
}
