package com.iluminados.iso8583.message;

import java.io.IOException;
import java.util.logging.Logger;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

import com.iluminados.iso8583.ISO8583MessageMap;
import com.iluminados.iso8583.MessageDispatcher;
import com.iluminados.iso8583.NSUManager;
import com.iluminados.util.BCD;

public class TesteComunicacaoMessage extends AbstractISO8583Message {
	
	static final Logger log = Logger.getLogger("TesteComunicacaoMessage");
	
	public static final String ADMINISTRATIVE_MTI = "0800";
	public static final String PROCESSING_CODE = "990000";
	public static final String DEFAULT_OPER_NII= "5";

	public TesteComunicacaoMessage() {
		
	}
	
	private byte[] generateISO8583Message() throws ISOException, IOException {
		String serialNumber = getPinpadSerialNumber();
		String commType = "4";
		
		// Prepara os campos 3, 11, 12, 13, 24, 41, 42 e 61
		
		ISOMsg requestMsg = new ISOMsg();
		requestMsg.setPackager(ISO8583MessageMap.getISO8583MessageMap().getPackager());
		
		requestMsg.setMTI(ADMINISTRATIVE_MTI);
		requestMsg.set(3, PROCESSING_CODE);
		requestMsg.set(11, String.valueOf(NSUManager.getInstance().getNextNSU()));
		requestMsg.set(12, getTimeAsString());
		requestMsg.set(13, getDateAsString());
		requestMsg.set(24, DEFAULT_OPER_NII);
		requestMsg.set(41, "BW000017");
		requestMsg.set(42, "BW000017");
		
		SubFieldFormatter sf61 = new SubFieldFormatter();
		sf61.addField("62", serialNumber);
		sf61.addField("64", commType);
					
		requestMsg.set(61,sf61.pack());
		
		logISO8583Message(requestMsg);
		
		byte[] requestBytes = requestMsg.pack();
		log.info("Request ISO Puro = " + BCD.BCDtoString(requestBytes));
		
		return requestBytes;
	}
	
	private byte[] sendISO8583MessageToTX(byte[] requestBytes) throws IOException {
		MessageDispatcher dispatcher = MessageDispatcher.getInstance();
		dispatcher.connect();
		byte [] responseBytes = dispatcher.dispatch(requestBytes);
		
		return responseBytes;
	}
	
	private void handleResponse(byte[] responseBytes) throws ISOException {
		ISOMsg respMsg = new ISOMsg();
		respMsg.setPackager(ISO8583MessageMap.getISO8583MessageMap().getPackager());
		
		log.info("Response = " + BCD.BCDtoString(responseBytes));
		respMsg.unpack(responseBytes);
		logISO8583Message(respMsg);
		
		String respCode = respMsg.getString(39);
		if (respCode.equals("00")) {
			log.info("Teste de comunicação com sucesso!!!!");
			log.info("NSUHost: " + respMsg.getString(37));
		}
		else {
			log.info("Teste de comunicacao deu erro. RespCode = " + respCode);
		}
		
		setResponse(respMsg);
	}
	
	public void processMessage() {
		
		try {	
			// Gera a mensagem ISO8583 para ser enviada ao TX
			byte[] requestBytes = generateISO8583Message();
			
			// Envia a mensagem ao TX e recebe a resposta, ou exception caso de erro ou timeout
			byte[] responseBytes = sendISO8583MessageToTX(requestBytes);
			
			// Trata a resposta
			if (responseBytes != null) {
				handleResponse(responseBytes);
			}
			else {
				log.info("Nao ha resposta..... Culpa do Nilson");
			}
		}
		catch (ISOException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			MessageDispatcher.getInstance().disconnect();
		}
	}
}
