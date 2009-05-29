package com.sporkmonger.rmud;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import org.eclipse.swt.custom.StyledText;

public class TelnetManager {
	private ITelnetBridge telnetBridge = null;
	private Connection connection = null;
	private Thread workerThread = null;
	
	private final static byte STATE_DATA  			= 0;
	private final static byte STATE_IAC				= 1;
	private final static byte STATE_IAC_SB			= 2;
	private final static byte STATE_IAC_WILL		= 3;
	private final static byte STATE_IAC_DO			= 4;
	private final static byte STATE_IAC_WONT		= 5;
	private final static byte STATE_IAC_DONT		= 6;
	private final static byte STATE_IAC_SBIAC		= 7;
	private final static byte STATE_IAC_SBDATA		= 8;
	private final static byte STATE_IAC_SBDATAIAC	= 9;

	private final static byte IAC  = (byte)255;
	private final static byte EOR  = (byte)239;
	private final static byte WILL = (byte)251;
	private final static byte WONT = (byte)252;
	private final static byte DO   = (byte)253;
	private final static byte DONT = (byte)254;
	private final static byte SB   = (byte)250;
	private final static byte SE   = (byte)240;
	private final static byte GA   = (byte)249;
	private final static byte DM   = (byte)242;
	
	private StringBuilder buffer;

	public TelnetManager(final ITelnetBridge telnetBridge, final Connection connection) {
		this.telnetBridge = telnetBridge;
		this.connection = connection;
		this.workerThread = new Thread("TelnetManager") {
			@Override
			public void run() {
				Socket socket = connection.getSocket();
				InputStream inputStream = null;
				int state = STATE_DATA;
				try {
					inputStream = socket.getInputStream();
					buffer = new StringBuilder();
					while(true) {
						if (connection == null || connection.isClosed()) {
							break;
						}
						int result = inputStream.read();
						if (result == -1) {
							break;
						}
						byte currentChar = (byte)result;
						// [255, 251, 25, 255, 251, 200, 255, 251, 86]
						switch (currentChar) {
						case IAC:
							state = STATE_IAC;
							break;
						case WILL:
							if (state == STATE_IAC) {
								state = STATE_IAC_WILL;
							} else {
								pushChar(WILL);
							}
							break;
						case WONT:
							if (state == STATE_IAC) {
								state = STATE_IAC_WONT;
							} else {
								pushChar(WONT);
							}
							break;
						case GA:
							if (state == STATE_IAC) {
								pushChar((byte)10);
								flushBuffer();
								state = STATE_DATA;
							} else {
								pushChar(GA);
							}
							break;
						case DM:
							if (state == STATE_IAC) {
								state = STATE_DATA;
							} else {
								pushChar(DM);
							}
							break;
						case EOR:
							if (state == STATE_IAC) {
								state = STATE_DATA;
							} else {
								pushChar(EOR);
							}
							break;
						case 10:
							pushChar((byte)10);
							flushBuffer();
							break;
						default:
							if (state == STATE_DATA) {
								pushChar(currentChar);
							} else {
								// We pretty much don't care, screw the telnet protocol
								state = STATE_DATA;
							}
							break;
						}
					}
					close();
				} catch (IOException e) {
				}
			}
		};
		this.workerThread.start();
	}
	
	public void close() throws IOException {
		flushBuffer();
		if (connection != null && connection.isConnected()) {
			connection.getSocket().close();
		}
	}
	
	private void resetBuffer() {
		buffer = new StringBuilder();
	}
	
	private void flushBuffer() {
		telnetBridge.receivedFromRemote(buffer.toString());
		resetBuffer();
	}
	
	private void pushChar(byte currentChar) {
		buffer.append((char)currentChar);
	}
}
