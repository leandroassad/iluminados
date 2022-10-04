package com.iluminados.iso8583;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Logger;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import com.iluminados.util.BCD;
import com.iluminados.util.ByteUtil;
import com.iluminados.util.Util;

import lombok.Getter;
import lombok.Setter;

public class MessageDispatcher {
	public static final Logger log = Logger.getLogger("MessageDispatcher");
	
	public static final byte STX = 0x02;
	public static final byte ETX = 0x03;
	public static final byte CR = 0x0D;

	public static final byte[] TPDU = { 0x68, 0x00, 0x00, 0x00, 0x00 };
	
	public static final String VALECARD_HOSTNAME = "201.16.207.146";
	public static final int VALECARD_PORT = 20004;
	public static final boolean IS_SSL = true;
	public static final int CONN_TIMEOUT = 30;
	public static final int RESP_TIMEOUT = 30;
	
	@Getter @Setter private String hostname;
	@Getter @Setter private int port;
	@Getter @Setter private boolean ssl = false;
	private int connTimeout;
	private int respTimeout;
	
	
	
	public static MessageDispatcher dispatcher = null;
	protected MessageDispatcher() {
		hostname = VALECARD_HOSTNAME;
		port = VALECARD_PORT;
		ssl = IS_SSL;
		connTimeout = CONN_TIMEOUT;
		respTimeout = RESP_TIMEOUT;
	}
	
	public static MessageDispatcher getInstance() {
		if (dispatcher == null) {
			dispatcher = new MessageDispatcher();
		}
		return dispatcher;
	}
	
	public static MessageDispatcher getInstanceSimple() {
		if (dispatcher == null) {
			dispatcher = new MessageDispatcher();
		}
		
		return dispatcher;
	}
	
	Socket sock = null;
	DataOutputStream out = null;
	DataInputStream in = null;
	
	public void connect() throws IOException  {	
		log.info("Conectando em " + hostname + ":" + port);
		if (ssl) {
			log.info("Conexao SSL");
			SSLSocketFactory sslsocketfactory = (SSLSocketFactory)SSLSocketFactory.getDefault();
			sock = (SSLSocket) sslsocketfactory.createSocket(hostname, port);
		}
		else {
			sock = new Socket(hostname, port);
		}

		if (connTimeout > 0) {
			log.info("Setando Timeout em " + connTimeout + " segundos");
			sock.setSoTimeout(connTimeout*1000);
		}

		out = new DataOutputStream(sock.getOutputStream());
		in = new DataInputStream(sock.getInputStream());
		
		log.info("Conexão com sucesso");
	}
	
	public void disconnect() {
		log.info("Desconectando....");
		if (sock != null) {
			try {
				sock.close();
			} catch (IOException e) {}
		}
	}
	
	public byte[] dispatch(byte[] requestBytes) throws IOException {
		return dispatch(requestBytes, true);
	}
	
	/**
	 * Empacota a msg e manda para o Host
	 *  
	 * @param requestBytes
	 * @return
	 * @throws IOException
	 */
	public byte[] dispatch(byte[] requestBytes, boolean waitForResponse) throws IOException {
		
		// empacota a mensagem
		byte [] fullRequest = pack(requestBytes);
		
		log.info("Request completo ao TX = " + BCD.BCDtoString(fullRequest));
		
		log.info("Enviando requisição");
		out.write(fullRequest);
		log.info("Enviado com sucesso. Aguardando resposta");
		
		if (!waitForResponse) return null;
		
		// Le os dois primeiros bytes, para saber qto ler depois
		byte [] lenBytes = new byte[2];
		int n  = in.read(lenBytes);
	
		log.info("Número de Bytes de tamanho recebidos: " + n + " Bytes = " + Integer.toHexString(lenBytes[0]&0xFF) + " " + Integer.toHexString(lenBytes[1]&0xFF));
		
		if (n == -1) {
			log.info("Falha recebendo bytes do host");
			return null;
		}
		
		// Le o restante
		int len = Util.byteToInt(lenBytes, Util.LSB_MSB_ORDER);	
		log.info("Numero de bytes a ler do TX: " + len);
		byte [] recv = new byte[len];
		int nRead = Util.readBytes(sock, recv);
		
		log.info("Response recebido do TX = " + BCD.BCDtoString(recv));
		
		byte[] responseBytes = unpack(recv, len);
		
		return responseBytes;
	}
	
	/**
	 * Empacota a mensagem ISO com o formato: TAM1 STX TAM2 TPDU MESSAGE ETX CRC CR
	 * @param requesBytes Mensagem ISO original
	 * @return Mensagem empacotada com headers e CRC
	 */
	protected byte[] pack(byte [] requestBytes) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		byte [] request= insertTrailingBytes(requestBytes);
		
		// tam1 = STX + tam2 + tpdu + request + ETX + CRC + 0D
		int tam1 = 1 + 3 + 5 + request.length + 1 + 2 + 1;

		baos.write(TPDU);
		baos.write(request);
		baos.write(ETX);
				
		byte crc = calcCRC(baos.toByteArray());		
		
		byte nibble1 = (byte)((crc >> 4) & 0x0F);
		byte nibble2 = (byte)(crc& 0x0F);
		
		baos.write(ByteUtil.byteToAscii(nibble1));
		baos.write(ByteUtil.byteToAscii(nibble2));
		baos.write(CR);
		
		byte [] tam1Bytes = new byte[2];
		Util.intToByte(tam1, tam1Bytes, Util.LSB_MSB_ORDER);
		
		int tam2 = 5 + request.length;
		byte [] tam2Bytes = new byte[2];
		Util.intToByte(tam2, tam2Bytes, Util.MSB_LSB_ORDER);
		String tam2Str = Integer.toHexString(tam2Bytes[0]&0xFF) + String.format("%02X", new Integer(tam2Bytes[1]&0xFF));

		ByteArrayOutputStream requestStream = new ByteArrayOutputStream();
		requestStream.write(tam1Bytes);
		requestStream.write(STX);
		requestStream.write(tam2Str.getBytes());
		requestStream.write(baos.toByteArray());

		return requestStream.toByteArray();
	}
	
	/**
	 * Efetua o desempacotamento da mensagem , devolvendo somente os bytes da mensagem ISO
	 * @param responseBytes Mensagem de resposta completa, sem os dois bytes iniciais de tamanho
	 * @param responseLen Tamanho da mensagem
	 * @return
	 */
	protected byte [] unpack(byte [] responseBytes, int responseLen) {
		byte [] isoBytes = null;
		// Trata os bytes e remove os 1F's da msg original
		if (responseBytes[0] == STX) {
			byte [] tam2Bytes = new byte[2];
			int tam2;
			
			// Extrai o tamanho do PDU + Mensagem
			tam2Bytes[0] = ByteUtil.asciiToByte(responseBytes[1]);
			tam2Bytes[1] = ByteUtil.asciiToByte(responseBytes[2]);
			tam2Bytes[1] <<= 4;
			tam2Bytes[1] |= ByteUtil.asciiToByte(responseBytes[3]);			
			
			tam2 = Util.byteToInt(tam2Bytes, Util.MSB_LSB_ORDER);
			
			// Extrai o CRC recebido
			byte messageCRC = ByteUtil.asciiToByte(responseBytes[responseLen-3]);
			messageCRC <<= 4;
			messageCRC |= ByteUtil.asciiToByte(responseBytes[responseLen-2]);
			
			// Calcula o CRC
			byte crc = calcCRC(responseBytes, 4, tam2+1);	// + 1 para incluir o ETX
			
			if (crc != messageCRC) {
				log.info("Falha de recebimento - CRC Incorreto");
				return null;
			}
			
			byte[] tpdu = new byte[5];
			System.arraycopy(responseBytes, 4, tpdu, 0, 5);
			
			// Mensagem ISO, pronta para ser adequadamente tratada, removendo-se os trailings
			int recvMsgLen =  tam2 - 5;	//  tam2 - tpduLen
			byte [] receivedMessage = new byte[recvMsgLen];
			System.arraycopy(responseBytes, 9, receivedMessage, 0, recvMsgLen);
			
			isoBytes = removeTrailingBytes(receivedMessage);
		}
		
		return isoBytes;
	}
	
	/**
	 * Calcula o CRC da msg, que é o XOR dos bytes TPDU, MSG e ETX
	 * @param data
	 * @return
	 */
	protected byte calcCRC(byte [] data) {
		return calcCRC(data, 0, data.length);
	}
	
	/**
	 * Calcula o CRC da msg, que é o XOR dos bytes TPDU, MSG e ETX
	 * @param data
	 * @param offset
	 * @param len
	 * @return
	 */
	protected byte calcCRC(byte [] data, int offset, int len) {
		byte crc = 0x00;
		
		for (int i=offset; i<(offset+len); i++) {
			crc ^= (data[i]&0xFF);
		}
		
		return crc;
		
	}
	
	private boolean belongsToSpecialBytes(byte b) {		
		return (b == 0x02 || b == 0x03 || b == 0x0D || b == 0x8D | b == 0x10 || b == 0x52 || b == 0x1F);
	}
	
	/**
	 * Faz um tratamento dos bytes da msg, tratando especialmente alguns bytes como 02, 03, 0D, etc
	 * De acordo com a documentação da Valecard, capitulo 4.2 - Empacotamento das Mensagens 
	 * @param request
	 * @return
	 */
	protected byte [] insertTrailingBytes(byte [] request) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		log.info("Request ISO Antes do tratamento = " + BCD.BCDtoString(request));
		
		for (int i=0; i<request.length; i++) {
			if (belongsToSpecialBytes(request[i])) {
				byte nibble1 = (byte)((request[i] >> 4) & 0x0F);
				byte nibble2 = (byte)(request[i]& 0x0F);
				
				baos.write(0x1F);
				baos.write(ByteUtil.byteToAscii(nibble1));
				baos.write(ByteUtil.byteToAscii(nibble2));
			}
			else baos.write(request[i]);
		}
		
		byte[] returnBytes = baos.toByteArray();
		log.info("Request ISO Depois do tratamento = " + BCD.BCDtoString(returnBytes));
		
		return returnBytes;
	}
	
	/**
	 * Remove os bytes especiais inseridos na msg para empacotamento
	 * @param request
	 * @return
	 */
	protected byte [] removeTrailingBytes(byte [] request) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		int index = 0;
		while (index < request.length) {
			if (request[index] == 0x1F) {
				index++;
				byte b = ByteUtil.asciiToByte(request[index]);
				b <<= 4;
				index++;
				b |= ByteUtil.asciiToByte(request[index]);
				baos.write(b);
			}
			else {
				baos.write(request[index]);
			}
			index++;
		}
		
		return baos.toByteArray();		
	}
}
