package com.iluminados.util;

public class BCD {

    public static byte[] DecimalToBCD(long num) {
    	return DecimalToBCD(num, 2);
    }
    
    public static byte[] DecimalToBCD(long num, int numBytes) {
        int digits = 0;

        long temp = num;
        while (temp != 0) {
            digits++;
            temp /= 10;
        }

        int byteLen = digits % 2 == 0 ? digits / 2 : (digits + 1) / 2;
        
        // Para garantir que tera num bytes no minimo
        if (byteLen < numBytes) byteLen = numBytes;

        byte bcd[] = new byte[byteLen];

        for (int i = 0; i < digits; i++) {
            byte tmp = (byte) (num % 10);

            if (i % 2 == 0) {
                bcd[i / 2] = tmp;
            } else {
                bcd[i / 2] |= (byte) (tmp << 4);
            }

            num /= 10;
        }

        for (int i = 0; i < byteLen / 2; i++) {
            byte tmp = bcd[i];
            bcd[i] = bcd[byteLen - i - 1];
            bcd[byteLen - i - 1] = tmp;
        }

        return bcd;
    }

    public static long BCDToDecimal(byte[] bcd) {
        return Long.valueOf(BCD.BCDtoString(bcd));
    }
    
    public static long BCDToDecimal(byte bcd) {
    	return Long.valueOf(BCD.BCDtoString(bcd));
    }
    
    public static String BCDtoString(byte bcd) {
        StringBuffer sb = new StringBuffer();
        
        byte high = (byte) (bcd & 0xf0);
        high >>>= (byte) 4;    
        high = (byte) (high & 0x0f);
        byte low = (byte) (bcd & 0x0f);
        
        sb.append(high);
        sb.append(low);
        
        return sb.toString();
    }
    
    public static String BCDtoString(byte[] bcd) {
        StringBuffer sb = new StringBuffer();
    
        for (int i = 0; i < bcd.length; i++) {
            sb.append(BCDtoString(bcd[i]));
        }
    
        return sb.toString();
    }
    
    public static void main(String[] args) {
        System.out.println("Testing DecimalToBCD:");
        BCD.testForValue(1L,        "00000001");
        BCD.testForValue(11L,       "00010001");
        BCD.testForValue(111L,      "0000000100010001");
        BCD.testForValue(1111L,     "0001000100010001");
        BCD.testForValue(11111L,    "000000010001000100010001");
        BCD.testForValue(42,        "01000010");
        BCD.testForValue(112233L,   "000100010010001000110011");
        BCD.testForValue(12345L,    "000000010010001101000101");
        
        System.out.println("\nTesting two way conversion using DecimalToBCD and back using BCDToDecimal:");
        BCD.testForValue(1L);
        BCD.testForValue(11L);
        BCD.testForValue(111L);
        BCD.testForValue(1111L);
        BCD.testForValue(11111L);
        BCD.testForValue(12983283L);
        BCD.testForValue(9832098349L);
        
        byte [] bv = BCD.DecimalToBCD(3);
        System.out.println("3 byte len=" + bv.length);
        System.out.println(Integer.toHexString(bv[0]&0xFF) + " " + Integer.toHexString(bv[1]&0xFF));
        String value = BCD.byteArrayToBinaryString(BCD.DecimalToBCD(3));
        System.out.println(value);
    }
    
    private static void testForValue(long val, String expected) {
        String binaryString = BCD.byteArrayToBinaryString(BCD.DecimalToBCD(val));
        System.out.print(String.format("Testing: %10d -> %30s %4s\n", val, binaryString, binaryString.equals(expected) ? "[OK]" : "[FAIL]"));
    }
    
    private static void testForValue(long val) {
        long newVal = BCD.BCDToDecimal(BCD.DecimalToBCD(val));
        
        System.out.print(String.format("Testing: %10d -> %30d %4s\n", val, newVal, newVal == val ? "[OK]" : "[FAIL]"));
    }
    
    public static String byteArrayToBinaryString(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for (byte i : bytes) {
            String byteInBinary = String.format("%8s", Integer.toBinaryString(i)).replace(' ', '0');
            sb.append(byteInBinary);
        }
        return sb.toString();
    }
}