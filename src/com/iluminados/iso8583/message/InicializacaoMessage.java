package com.iluminados.iso8583.message;

import java.io.IOException;
import java.util.logging.Logger;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

import com.iluminados.iso8583.ISO8583MessageMap;
import com.iluminados.iso8583.MessageDispatcher;
import com.iluminados.iso8583.NSUManager;
import com.iluminados.util.BCD;

public class InicializacaoMessage extends AbstractISO8583Message {
	public static final Logger log = Logger.getLogger("Inicializacao");
	
	/*
	 * O Processing code 950000 é usado na primeira e na ultima msg
	 */
	public static final String PROCESSING_CODE = "950000";
	

	public static final String TABLES = "0300000004000000050000000600000008000000110000001200000013000000140000001500000016000000";	
	
	public InicializacaoMessage() {
		super();
	}
	
	public void processMessage() {
		String commType = "4";
		boolean initSuccess = true;
		
		String serialNumber = getPinpadSerialNumber();
		
		String processingCode = PROCESSING_CODE;	// ProcessingCode Inicial sendo setado para iniciar a baixa de tabelas
		MessageDispatcher dispatcher = MessageDispatcher.getInstance();
		try {
			
			dispatcher.connect();
			ISOMsg requestMsg = new ISOMsg();
			requestMsg.setPackager(ISO8583MessageMap.getISO8583MessageMap().getPackager());

			// Prepara os campos 3, 11, 12, 13, 24, 48, 60,61 e 63
			requestMsg.setMTI(ADMINISTRATIVE_MTI);
			requestMsg.set(24, DEFAULT_OPER_NII);
			requestMsg.set(48,"INGMove24160080009");
			requestMsg.set(60, serialNumber.getBytes());
			
			SubFieldFormatter sf61 = new SubFieldFormatter();
			sf61.addField("62", serialNumber);
			sf61.addField("64", commType);			
			requestMsg.set(61,sf61.pack());
			
			SubFieldFormatter sf63 = new SubFieldFormatter();
			sf63.addField("96", TABLES);
			requestMsg.set(63, sf63.pack());

			while (true) {			
				
				requestMsg.set(3, processingCode);				
				requestMsg.set(11, String.valueOf(NSUManager.getInstance().getNextNSU()));
				requestMsg.set(12, getTimeAsString());
				requestMsg.set(13, getDateAsString());
				
				logISO8583Message(requestMsg);
				
				byte [] request = requestMsg.pack();
				
				byte [] responseBytes = dispatcher.dispatch(request);
				
				if (responseBytes == null) break;
				
				ISOMsg respMsg = new ISOMsg();
				respMsg.setPackager(ISO8583MessageMap.getISO8583MessageMap().getPackager());
				
				respMsg.unpack(responseBytes);
				logISO8583Message(respMsg);
				
				setResponse(respMsg);
				
				if (respMsg.getString(39).equals("00")) {	// Requisição aprovada
					processingCode = respMsg.getString(3);	// Obtem o ProcessingCode, para a próxima requisição de tabela
					//log.info("Proximo ProssessingCode: " + processingCode);					
	
					if (processingCode.equals(PROCESSING_CODE)) {	// fim das tabelas
						break;
					}
	
					if (!respMsg.hasField(63)) continue;		// Nao deve acontecer, mas, just in case.
	
					byte[] bit63Bytes = respMsg.getBytes(63);
					byte [] bit63TamBytes = new byte[2];					
					
					// extrai o tamanho dos dados do registro
					bit63TamBytes[0] = bit63Bytes[0];
					bit63TamBytes[1] = bit63Bytes[1];
					
					int bit63Tam = (int)BCD.BCDToDecimal(bit63TamBytes);
										
					String bit63 = new String(bit63Bytes, 2, bit63Tam-2);
					
					String tableId = bit63.substring(0, 2);
					int tableDataLen = Integer.parseInt(bit63.substring(2, 5));
					String tableVersion = bit63.substring(5, 11);
					
					int dataLen = bit63Tam-11;		// Tamanho total - tableId (2) - tableDataLen (3) - tableVersion (6)
					byte[] tableData = new byte[dataLen];
					System.arraycopy(bit63Bytes, 13, tableData, 0, dataLen);
					
					log.info("TableId: " + tableId);
					log.info("dataLen: " + dataLen + "(Tamanho dos dados depois do tableVersion, no formato tableOperation tableData ... tableOperation tableData)");
					log.info("tableDataLen: " + tableDataLen + " (Tamanho de todas as tabelas juntas menos o tableOperation com 2 bytes, para cada tabela)");
					
//					TableParser parser = tableMap.getTableParser(tableId);
//					if (parser != null) {
//						parser.parseTable(tableVersion, tableData, tableDataLen);
//					}
//					else {
//						log.info("Tabela [" + tableId + "] não mapeada - Ignorando dados.");
//					}					
				}
				else {
					initSuccess = false;
					break;
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
}
