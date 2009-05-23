package com.sporkmonger.rmud;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.UUID;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.runtime.builtin.IRubyObject;

import com.sporkmonger.util.HydraLoader;

public class Script implements ITelnetListener {
	private String host = null;
	private String scriptBody = null;
	private File scriptFile = null;
	private static String preface = null;
	private Ruby runtime = null;
	private String uuid = null;
	private ITelnetBridge bridge = null;
	private static HashMap<String, Script> scriptRegistry = new HashMap<String, Script>();
	
	public Script(ITelnetBridge bridge) {
		this.bridge = bridge;
		uuid = UUID.randomUUID().toString();
		scriptRegistry.put(uuid, this);
	}
	
	public static Script getScriptInstance(String uuid) {
		return scriptRegistry.get(uuid);
	}
	
	public static Script loadScript(String host, ITelnetBridge bridge) throws Throwable {
		File settingsDirectory = new File(Settings.getSettingsPath());
		File scriptFile = new File(settingsDirectory, host + ".rb");
		Script scriptForHost = new Script(bridge);
		scriptForHost.setHost(host);
		scriptForHost.setScriptFile(scriptFile);
		if (scriptFile.exists()) {
			StringBuilder body = new StringBuilder();
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(scriptFile)));
			String line = null;
			while ((line = reader.readLine()) != null) {
				body.append(line + "\n");
			}
			scriptForHost.setScriptBody(body.toString());
		} else {
			scriptForHost.setScriptBody("");
			try {
				scriptFile.createNewFile();
			} catch (IOException e) {
			}
		}
		if (preface == null) {
			InputStream prefaceStream = Settings.getPrefaceURL().openStream();
			if (prefaceStream == null) {
				throw new RuntimeException("Could not obtain input stream for script preface.");
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(prefaceStream));
			StringBuilder body = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				body.append(line + "\n");
			}
			preface = body.toString();
		}
		scriptForHost.initializeRuntime();
		return scriptForHost;
	}

	public void setScriptFile(File scriptFile) {
		this.scriptFile = scriptFile;
	}

	public File getScriptFile() {
		return scriptFile;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getHost() {
		return host;
	}

	public void setScriptBody(String scriptBody) {
		this.scriptBody = scriptBody;
	}

	public String getScriptBody() {
		return scriptBody;
	}
	
	public ITelnetBridge getBridge() {
		return bridge;
	}
	
	protected void initializeRuntime() throws Throwable {
		if (runtime == null) {
			RubyInstanceConfig config = new RubyInstanceConfig();
			config.setJRubyHome(Settings.getJRubyHome());
			config.setCurrentDirectory(Settings.getSettingsPath());
			config.setError(new PrintStream(getBridge().getConsoleErrorStream()));
			config.setOutput(new PrintStream(getBridge().getConsoleOutputStream()));
			
			ClassLoader jrubyLoader = config.getLoader();
			ClassLoader rmudLoader = this.getClass().getClassLoader();
			
			// JRuby's classloader must be the parent, or resources
			// can't be loaded correctly within the runtime.
			// Overriding getResource inexplicably breaks everything.
			// Fortunately, we don't need to access resources from
			// the plugin's classloader.  Unsatisfying, but sometimes
			// that's just how things go.
			HydraLoader hydraLoader = new HydraLoader(jrubyLoader);
			hydraLoader.addLoader(rmudLoader);
			
			config.setLoader(hydraLoader);
			
			runtime = Ruby.newInstance(config);
			runtime.evalScriptlet("SCRIPT_UUID = \"" + uuid + "\"\n");
			runtime.evalScriptlet("$:.unshift(\"" + Settings.getSettingsPath() + "\")\n");
			runtime.executeScript(preface, Settings.getPrefaceURL().getPath());
			runtime.executeScript(scriptBody, scriptFile.getPath());
		} else {
			throw new RuntimeException("Runtime already initialized.");
		}
	}

	@Override
	public void readFromLocal(TelnetEvent event) {
		if (runtime == null) {
			throw new RuntimeException("Runtime has not been initialized.");
		}
		RubyString rubyData = RubyString.newString(runtime, event.getData());
		runtime.getClass("IOProcessor").callMethod(runtime.getCurrentContext(), "process_from_local", rubyData);
	}

	@Override
	public void readFromRemote(TelnetEvent event) {
		if (runtime == null) {
			throw new RuntimeException("Runtime has not been initialized.");
		}
		RubyString rubyData = RubyString.newString(runtime, event.getData());
		runtime.getClass("IOProcessor").callMethod(runtime.getCurrentContext(), "process_from_remote", rubyData);
	}
}
