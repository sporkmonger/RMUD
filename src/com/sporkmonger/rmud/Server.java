package com.sporkmonger.rmud;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FilenameFilter;

public class Server {
	private static Server[] servers = null;
	private String name = null;
	private String host = null;
	private int port = 0;

	public Server() {
	}

	public Server(String name, String host, int port) {
		this.setName(name);
		this.setHost(host);
		this.setPort(port);
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getHost() {
		return host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getPort() {
		return port;
	}
	
	public String toString() {
		return name;
	}
	
	public static Server[] load() throws IOException {
		if (servers == null) {
			File settingsDirectory = new File(Settings.getSettingsPath());
			String[] serverFiles = settingsDirectory.list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".srv");
				}
			});
			if (serverFiles.length == 0) {
				servers = new Server[0];
			} else {
				servers = new Server[serverFiles.length];
				for (int i = 0; i < serverFiles.length; i++) {
					File serverFile = new File(settingsDirectory.getPath(), serverFiles[i]);
					BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(serverFile)));
					Server server = new Server(reader.readLine(), reader.readLine(), Integer.parseInt(reader.readLine()));
					servers[i] = server;
				}
			}
		}
		return servers;
	}
	
	public void save() throws IOException {
		String filename = name.toLowerCase().replaceAll("\\W", "_") + ".srv";
		File serverFile = new File(Settings.getSettingsPath(), filename);
		FileOutputStream outputStream = null;
		try {
			serverFile.createNewFile();
			outputStream = new FileOutputStream(serverFile);
			outputStream.write((name + "\n").getBytes());
			outputStream.write((host + "\n").getBytes());
			outputStream.write((port + "\n").getBytes());
		} catch (IOException e) {
			throw e;
		} finally {
			if (outputStream != null) {
				outputStream.close();
			}
		}
	}
}
