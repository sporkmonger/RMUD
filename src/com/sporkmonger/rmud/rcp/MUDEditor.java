package com.sporkmonger.rmud.rcp;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.part.EditorPart;

import com.sporkmonger.rmud.Connection;
import com.sporkmonger.rmud.ITelnetBridge;
import com.sporkmonger.rmud.ITelnetListener;
import com.sporkmonger.rmud.Script;
import com.sporkmonger.rmud.Server;
import com.sporkmonger.rmud.TelnetEvent;
import com.sporkmonger.rmud.TelnetManager;
import com.sporkmonger.rmud.swt.custom.ANSIStyledContent;

public class MUDEditor extends EditorPart implements IJobChangeListener, ITelnetBridge, KeyListener {
	public static final String ID = "com.sporkmonger.rmud.mud.editor";

	private Server server;
	private Connection connection = null;
	private TelnetManager telnetManager = null;
	private MessageConsole messageConsole = null;
	private MessageConsole networkConsole = null;
	private MessageConsoleStream consoleOutputStream = null;
	private MessageConsoleStream consoleErrorStream = null;
	private MessageConsoleStream consoleRemoteNetworkStream = null;
	private MessageConsoleStream consoleLocalNetworkStream = null;
	private Script script = null;
	private StyledText ansiText = null;
	private Text commandInputField = null;
	private Color foreground = null;
	private Color background = null;
	private Font font = null;
	private ANSIStyledContent ansiStyledContent = null;
	private ArrayList<ITelnetListener> telnetListeners = new ArrayList<ITelnetListener>();
	private int commandIndex = 0;
	private ArrayList<String> commandHistory = new ArrayList<String>();
	
	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		final ITelnetBridge bridge = this;
		setSite(site);
		setInput(input);
		if (input instanceof MUDEditorInput) {
			this.server = ((MUDEditorInput)input).getServer();
			setPartName(this.server.getName());
		} else {
			throw new RuntimeException("Invalid editor input.");
		}
		this.connection = new Connection(this.server);
		
		this.messageConsole = new MessageConsole(server.getName() + " Console", null);
		this.networkConsole = new MessageConsole(server.getName() + " Network", null);
		
		this.consoleOutputStream = this.messageConsole.newMessageStream();
		this.consoleErrorStream = this.messageConsole.newMessageStream();
		this.consoleErrorStream.setColor(getEditorSite().getShell().getDisplay().getSystemColor(SWT.COLOR_RED));
		
		this.consoleRemoteNetworkStream = this.networkConsole.newMessageStream();
		this.consoleLocalNetworkStream = this.networkConsole.newMessageStream();
		this.consoleLocalNetworkStream.setColor(getEditorSite().getShell().getDisplay().getSystemColor(SWT.COLOR_BLUE));
		
		ConsolePlugin.getDefault().getConsoleManager().addConsoles(
			new IConsole[] { this.networkConsole, this.messageConsole }
		);

		Job job = new Job("Connecting to " + server.getHost() + ":" + server.getPort()) {
		    @Override
			public boolean belongsTo(Object family) {
		    	return family.equals("connect");
			}
			
		    @Override
		    protected IStatus run(IProgressMonitor monitor) {
		        // Start up the connection here.
	        	try {
	        		monitor.beginTask("Initializing script...", 100);
			        script = Script.loadScript(server.getHost(), bridge);
					monitor.worked(90);
	        		addTelnetListener(script);
	        		monitor.worked(10);
	        		monitor.beginTask("Establishing connection...", 100);
			        connection.connect();
	        		monitor.worked(100);
				} catch (Throwable e) {
					return new Status(
						Status.ERROR,
						"com.sporkmonger.rmud",
						"Could not connect: " + e.getMessage(),
						e
					);
				}

		        monitor.done();
		        return Status.OK_STATUS;
		    }
		};
		job.setUser(true);
		job.schedule();
		job.addJobChangeListener(this);
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(final Composite parent) {
		if (foreground == null) {
			foreground = new Color(parent.getDisplay(), 255, 255, 255);
		}
		if (background == null) {
			background = new Color(parent.getDisplay(), 40, 40, 40);
		}
		if (font == null) {
			font = new Font(parent.getDisplay(), "Monaco", 12, SWT.NORMAL);
		}
		
		SashForm sashForm = new SashForm(parent, SWT.VERTICAL);
		ansiText = new StyledText(sashForm, SWT.LEFT | SWT.MULTI | SWT.V_SCROLL);
		
		ansiStyledContent = new ANSIStyledContent(parent.getDisplay());
		ansiText.setBackground(background);
		ansiText.setForeground(foreground);
		ansiText.setFont(font);
		ansiText.setEditable(false);
		ansiText.setContent(ansiStyledContent);
		ansiText.addLineStyleListener(ansiStyledContent);
		ansiText.setMargins(8, 8, 8, 8);

		commandInputField = new Text(sashForm, SWT.LEFT);
		commandInputField.setBackground(background);
		commandInputField.setForeground(foreground);
		commandInputField.setText("");
		commandInputField.addKeyListener(this);
		
		sashForm.setWeights(new int[]{23, 1});
	}

	@Override
	public void setFocus() {
	}

	@Override
	public void dispose() {
		if (foreground != null) {
			foreground.dispose();
		}
		if (background != null) {
			background.dispose();
		}
		if (ansiStyledContent != null) {
			ansiStyledContent.dispose();
		}
		if (telnetManager != null) {
			try {
				telnetManager.close();
			} catch (IOException e) {
			}
		}
		if (messageConsole != null) {
			ConsolePlugin.getDefault().getConsoleManager().removeConsoles(			
				new IConsole[] { this.networkConsole, this.messageConsole }
			);
		}
		super.dispose();
	}
	
	protected void finalize() throws Throwable
	{
		dispose();
		super.finalize();
	}
	
	public void connected() {
	}
	
	// Job listener callbacks
	
	@Override
	public void aboutToRun(IJobChangeEvent event) {
	}

	@Override
	public void awake(IJobChangeEvent event) {
	}

	@Override
	public void done(IJobChangeEvent event) {
		if (event.getJob().belongsTo("connect")) {
			final ITelnetBridge telnetBridge = this;
			getEditorSite().getShell().getDisplay().syncExec(new Runnable() {
				public void run() {
					getEditorSite().getActionBars().getStatusLineManager().setMessage("Connected.");
				}
			});
			
			// It's kinda hard to sync these too...
			// Telnet manager can only be created after both of these exist and are live.
			if (ansiText != null && connection != null && connection.isConnected()) {
				telnetManager = new TelnetManager(telnetBridge, connection);
				connected();
			} else {
				(new Thread() {
					@Override
					public void run() {
						while (true) {
							if (ansiText != null && connection != null && connection.isConnected()) {
								telnetManager = new TelnetManager(telnetBridge, connection);
								connected();
								break;
							}
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
							}
						}
					}
				}).start();
			}
		}
	}

	@Override
	public void running(IJobChangeEvent event) {
	}

	@Override
	public void scheduled(IJobChangeEvent event) {
	}

	@Override
	public void sleeping(IJobChangeEvent event) {
	}

	@Override
	public void addTelnetListener(ITelnetListener listener) {
		telnetListeners.add(listener);
	}

	@Override
	public void sendToLocal(final String data) {
		getEditorSite().getShell().getDisplay().syncExec(new Runnable() {
			public void run() {
				if (!ansiText.isDisposed()) {
					ansiStyledContent.appendANSIText(data);
					// Don't scroll if we're looking at something else
					if (Math.abs(ansiText.getTopIndex() - ansiStyledContent.getLineCount()) < 100) {
						ansiText.setTopPixel(Integer.MAX_VALUE);
					}
				}
			}
		});
	}

	@Override
	public void sendToRemote(String data) {
		boolean disconnected = false;
		try {
			if (connection.getSocket().isConnected()) {
				connection.getSocket().getOutputStream().write(data.getBytes());
			} else {
				disconnected = true;
			}
		} catch (IOException e) {
			disconnected = true;
		}
		if (disconnected) {
			getEditorSite().getShell().getDisplay().syncExec(new Runnable() {
				public void run() {
					getEditorSite().getActionBars().getStatusLineManager().setMessage("Disconnected.");
				}
			});
		}
	}

	@Override
	public void receivedFromLocal(String data) {
		TelnetEvent event = new TelnetEvent(this, data);
		for (ITelnetListener listener : telnetListeners) {
			listener.readFromLocal(event);
		}
		getEditorSite().getShell().getDisplay().syncExec(new Runnable() {
			public void run() {
				if (!ansiText.isDisposed()) {
					// Don't scroll if we're looking at something else
					if (Math.abs(ansiText.getTopIndex() - ansiStyledContent.getLineCount()) < 100) {
						ansiText.setTopPixel(Integer.MAX_VALUE);
					}
				}
			}
		});
	}

	@Override
	public void receivedFromRemote(String data) {
		TelnetEvent event = new TelnetEvent(this, data);
		for (ITelnetListener listener : telnetListeners) {
			listener.readFromRemote(event);
		}
		getEditorSite().getShell().getDisplay().syncExec(new Runnable() {
			public void run() {
				if (!ansiText.isDisposed()) {
					// Don't scroll if we're looking at something else
					if (Math.abs(ansiText.getTopIndex() - ansiStyledContent.getLineCount()) < 100) {
						ansiText.setTopPixel(Integer.MAX_VALUE);
					}
				}
			}
		});
	}

	@Override
	public OutputStream getConsoleOutputStream() {
		if (this.consoleOutputStream != null) {
			return this.consoleOutputStream;
		} else {
			return null;
		}
	}


	@Override
	public OutputStream getConsoleErrorStream() {
		if (this.consoleErrorStream != null) {
			return this.consoleErrorStream;
		} else {
			return null;
		}
	}

	@Override
	public OutputStream getConsoleRemoteNetworkStream() {
		if (this.consoleRemoteNetworkStream != null) {
			return this.consoleRemoteNetworkStream;
		} else {
			return null;
		}
	}

	@Override
	public OutputStream getConsoleLocalNetworkStream() {
		if (this.consoleLocalNetworkStream != null) {
			return this.consoleLocalNetworkStream;
		} else {
			return null;
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.keyCode == SWT.ARROW_UP) {
			commandIndex -= 1;
			if (commandIndex < 0) {
				commandIndex = 0;
			}
			commandInputField.setText(commandHistory.get(commandIndex));
		} else if (e.keyCode == SWT.ARROW_DOWN) {
			commandIndex += 1;
			if (commandIndex >= commandHistory.size()) {
				commandIndex = commandHistory.size();
				commandInputField.setText("");
			} else {
				commandInputField.setText(commandHistory.get(commandIndex));
			}
		} else if (e.keyCode == 10 || e.keyCode == 13) {
			String command = commandInputField.getText();
			ansiStyledContent.appendANSIText("\u001b[4m" + command + "\u001b[24m\n");
			commandInputField.setText("");
			commandHistory.add(command);
			commandIndex += 1;
			receivedFromLocal(command.replaceAll("[\\r\\n]+", "") + "\n");
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}
}
