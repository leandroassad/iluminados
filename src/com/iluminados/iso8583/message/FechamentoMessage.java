package com.iluminados.iso8583.message;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

import com.iluminados.iso8583.ISO8583MessageMap;
import com.iluminados.iso8583.MessageDispatcher;
import com.iluminados.iso8583.NSUManager;

import lombok.Getter;

public class FechamentoMessage extends AbstractISO8583Message {

	static Logger log = Logger.getLogger("FechamentoMessage");
	
	public static final String FECHAMENTO_MTI = "0500";
	public static final String PROCESSING_CODE = "920000";
	public static final String DEFAULT_OPER_NII= "5";

	@Getter int numLote;
	
	String senha;
	public FechamentoMessage(String senha) {
		this.senha = senha;
	}
	
	public void processMessage() {
		String serialNumber = getPinpadSerialNumber();
		String commType = "4";
		MessageDispatcher dispatcher = MessageDispatcher.getInstance();
				
		// Prepara os campos 3, 11, 12, 13, 24, 41, 42 e 61
		try {
			dispatcher.connect();

			SubFieldFormatter sf61 = new SubFieldFormatter();
			sf61.addField("61", serialNumber);
			sf61.addField("64", commType);
			sf61.addField("65", senha);
			sf61.addField("55", "00000000000000000000000000000000000000000000                                                                        000000000000000000000000000000000000000000000000000000000000000000000000");
			
			ISOMsg requestMsg = new ISOMsg();
			requestMsg.setPackager(ISO8583MessageMap.getISO8583MessageMap().getPackager());
			
			numLote = 1;
			requestMsg.setMTI(FECHAMENTO_MTI);
			requestMsg.set(3, PROCESSING_CODE);
			requestMsg.set(11, String.valueOf(NSUManager.getInstance().getNextNSU()));
			requestMsg.set(12, getTimeAsString());
			requestMsg.set(13, getDateAsString());
			requestMsg.set(24, DEFAULT_OPER_NII);
			requestMsg.set(41, "BW000032");
			requestMsg.set(42, "BW000032");
			requestMsg.set(48, "INGMove24160080009");
			requestMsg.set(60, String.valueOf(numLote));
			requestMsg.set(61, sf61.pack());
			
			byte [] requestBytes = requestMsg.pack();
			
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
					
					SubFieldFormatter sf63 = new SubFieldFormatter();
					sf63.setBytes(bit63Bytes);
					List<SubField> list = sf63.unpack();
					for (SubField sf : list) {
						byte [] data = sf.getData();
						switch (sf.getId()) {
						case "12":
							log.info("Dados Fechamento: [" + new String(data) + "]");
							int index = 0;
							String nroLogico = new String(data, index, 8);	index += 8;
							String codEstab = new String(data, index, 15); index += 15;
							String valorTotalRealizadas = new String(data, index, 12); index += 12;
							String qtdeTotalRealizadas = new String(data, index, 4); index += 4;
							String valorTotalCanceladas = new String(data, index, 12); index += 12;
							String qtdeTotalCanceladas = new String(data, index, 4); index += 4;
							
							log.info("Numero Logico: " + nroLogico);
							log.info("Codigo Estabelecimento: [" + codEstab + "]");
							log.info("Valor Total Realizadas e Não Canceladas: R$ " + formatAsAmount(Long.parseLong(valorTotalRealizadas)));
							log.info("Quantidade Total Realizadas e Nao Canceladas: " + qtdeTotalRealizadas);
							log.info("Valor Total Canceladas: R$ " + formatAsAmount(Long.parseLong(valorTotalCanceladas)));
							log.info("Quantidade Total Canceladas: " + qtdeTotalCanceladas);
							break;
						}
					}
				}
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
	
	protected String formatAsAmount(long value) {
		return String.format("%,.2f", (double)((double)value/100));
	}	
}
