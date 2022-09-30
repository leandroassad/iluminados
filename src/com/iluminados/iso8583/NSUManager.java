package com.iluminados.iso8583;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

public class NSUManager {
	static final Logger log = Logger.getLogger("NSUManager"); 
	public static final int MAX_NSU = 999999;
	
	int currentNSU = 1;
	String nsuStore = null;
	
	protected static NSUManager nsuManager= null; 
	protected NSUManager () {
		nsuStore = "data/nsustore.dat";
		
		File f = new File(nsuStore);
		if (f.exists()) {
			try (FileInputStream in = new FileInputStream(f)) {
				currentNSU = in.read();
			} catch (IOException e) {
				log.info("Falha abrindo NSU");
			}
		}
		else {
			try (FileOutputStream out = new FileOutputStream(f)) {
				currentNSU = 1;
				out.write(currentNSU);
			} catch (IOException e) {
				log.info("Falha gravando NSU");
			}
			
		}
	}
	
	public static NSUManager getInstance() {
		if (nsuManager == null) {
			nsuManager = new NSUManager();
		}
		
		return nsuManager;
	}
	
	public void storeCurrentNSU() {
		try (FileOutputStream out = new FileOutputStream(new File(nsuStore))) {
				out.write(currentNSU);
		}
		catch (IOException e) {
		}
		
	}
	
	public int getNextNSU() {
		int retNSU = currentNSU;
		
		currentNSU = (currentNSU == MAX_NSU) ? 1 : ++currentNSU;
		storeCurrentNSU();
		
		return retNSU;
	}
	
	public int getCurrentNSU() {
		return currentNSU;
	}
}
