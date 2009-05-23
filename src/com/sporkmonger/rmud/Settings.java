package com.sporkmonger.rmud;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

public class Settings {
	public static String getSettingsPath() throws FileNotFoundException {
		String settingsPath = "~/.rmud/";
		String osName = System.getProperty("os.name", "");
		if (osName.equals("Mac OS X")) {
			settingsPath = "~/Library/Application Support/RMUD/";
		} else if (osName.contains("Windows")) {
			settingsPath = ".";
		}
		if (settingsPath.startsWith("~")) {
			settingsPath = settingsPath.replace("~", System.getProperty("user.home"));
		}
		File settingsDirectory = null;
		try {
			settingsDirectory = (new File(settingsPath)).getCanonicalFile();
		} catch (IOException e) {
			settingsDirectory = (new File(settingsPath)).getAbsoluteFile();		
		}
		if (!settingsDirectory.exists()) {
			if (!settingsDirectory.mkdir()) {
				throw new FileNotFoundException("Could not create " + settingsDirectory.getPath());
			}
		}
		return settingsDirectory.getPath();
	}
	
	public static String getJRubyHome() throws FileNotFoundException {
		String settingsPath = getSettingsPath();
		File jrubyDirectory = null;
		try {
			jrubyDirectory = (new File(settingsPath, "jruby.home")).getCanonicalFile();
		} catch (IOException e) {
			jrubyDirectory = (new File(settingsPath, "jruby.home")).getAbsoluteFile();		
		}
		if (!jrubyDirectory.exists()) {
			if (!jrubyDirectory.mkdir()) {
				throw new FileNotFoundException("Could not create " + jrubyDirectory.getPath());
			}
		}
		return jrubyDirectory.getPath();
	}
	
	public static URL getPrefaceURL() throws IOException {
		Bundle rmudBundle = Platform.getBundle("com.sporkmonger.rmud");
		return FileLocator.toFileURL(rmudBundle.getEntry("/src/com/sporkmonger/rmud/Preface.rb"));
	}
}
