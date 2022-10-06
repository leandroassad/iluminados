package com.iluminados.iso8583.message;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

import com.iluminados.iso8583.ISO8583MessageMap;
import com.iluminados.iso8583.MessageDispatcher;
import com.iluminados.iso8583.NSUManager;
import com.iluminados.util.BCD;
import com.iluminados.util.Util;

public class AberturaMessage extends AbstractISO8583Message {
	static Logger log = Logger.getLogger("AberturaMessage");
	
	public static final String ADMINISTRATIVE_MTI = "0800";
	public static final String PROCESSING_CODE = "970000";
	public static final String DEFAULT_OPER_NII= "5";
	public static final String TABLES = "030000000400000008000000110000001200000013000000140000001500000016000000";	

	public AberturaMessage() {
		
	}
	
	public void processMessage() {
		String commType = "4";
		String serialNumber = getPinpadSerialNumber();
		
		MessageDispatcher dispatcher = MessageDispatcher.getInstance();		
		
		
		// Prepara os campos 3, 11, 12, 13, 24, 41, 42 e 61
		try {
			dispatcher.connect();
			
			ISOMsg requestMsg = new ISOMsg();
			requestMsg.setPackager(ISO8583MessageMap.getISO8583MessageMap().getPackager());
			
			requestMsg.setMTI(ADMINISTRATIVE_MTI);
			requestMsg.set(3, PROCESSING_CODE);
			requestMsg.set(11, String.valueOf(NSUManager.getInstance().getNextNSU()));
			requestMsg.set(12, getTimeAsString());
			requestMsg.set(13, getDateAsString());
			requestMsg.set(24, DEFAULT_OPER_NII);
			requestMsg.set(41, "BW000032");
			requestMsg.set(42, "BW000032");
			requestMsg.set(48, "INGMove24160080009");
			requestMsg.set(60, serialNumber.getBytes());
			SubFieldFormatter sf61 = new SubFieldFormatter();
			sf61.addField("62", serialNumber);
			sf61.addField("64", commType);			
			requestMsg.set(61,sf61.pack());
			
			SubFieldFormatter sf63 = new SubFieldFormatter();
			sf63.addField("96", TABLES);
			requestMsg.set(63, sf63.pack());

			
			byte [] requestBytes = requestMsg.pack();
			
			//log.debug("Request = " + BCD.BCDtoString(requestBytes));
			logISO8583Message(requestMsg);
			
			byte [] responseBytes = dispatcher.dispatch(requestBytes);


			if (responseBytes != null) {
				ISOMsg respMsg = new ISOMsg();
				respMsg.setPackager(ISO8583MessageMap.getISO8583MessageMap().getPackager());
				
				respMsg.unpack(responseBytes);
				logISO8583Message(respMsg);
				
				setResponse(respMsg);
				
				if (respMsg.getString(39).equals("00")) {
					byte[] bit63Bytes = respMsg.getBytes(63);
					//log.info("BIT63: " + Util.prepareBytesForPrinting(bit63Bytes));
						
					SubFieldFormatter sf63Resp = new SubFieldFormatter();
					sf63Resp.setBytes(bit63Bytes);
					
					List<SubField> sfList = sf63Resp.unpack();
					for (SubField sf : sfList) {
						byte [] data = sf.getData();
						switch (sf.getId()) {
						case "FS":
							log.info("** DE63.FS = " + new String(data));
							break;
						case "16":
							log.info("** DE63.16 = " + new String(data));
							break;
						case "44":
							log.info("** DE63.44 = " + Util.prepareBytesForPrinting(sf.getData()));
							long dataLen = sf.getData().length;
							int index = 0;
							while (index < dataLen) {
								int idAcquirer = data[index++];
								char vinculada = (char)data[index++];
								String nroLogico = new String(data, index, 8); index += 8;
								String codEstab = new String(data, index, 15); index += 15;
								byte [] wk = new byte[16];
								System.arraycopy(data, index, wk, 0, 16); index += 16;
								
								log.info("Acquirer ID: " + idAcquirer);
								log.info("Vinculada EC? " + vinculada);
								log.info("Numero Logico: " + nroLogico);
								log.info("Codigo Estabelecimento: [" + codEstab + "]");
								log.info("WorkingKey: [" + Util.prepareBytesForPrinting(wk) + "]");
								log.info("*********************************************************");
							}
							break;
						}

					}
					
					log.info("Aplicação Aberta...");
				}
				else {					
					log.info("Aplicação Não Aberta...");
				}
			}
			else {
				log.info("Aplicação Não Aberta...");
			}
		}
		catch (ISOException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {		
			dispatcher.disconnect();
		}		
	}
}
