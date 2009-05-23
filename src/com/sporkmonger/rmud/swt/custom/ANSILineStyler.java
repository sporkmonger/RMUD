package com.sporkmonger.rmud.swt.custom;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.LineStyleListener;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

public class ANSILineStyler implements LineStyleListener, VerifyListener {
	private StyledText styledText = null;
	private Display display = null;
	
	private Pattern controlSequencePattern = Pattern.compile("((\u009B|\u001B\\[)(\\d+;)*(\\d+)?[mABCDEFGHJKSTfnsu])");
	private StyleRange lastStyle = null;
	private ArrayList<StyleRange> queuedStyles = new ArrayList<StyleRange>();
	private Color[] colorTable = new Color[20];
	
	public ANSILineStyler(StyledText styledText) {
		this.styledText = styledText;
		this.display = this.styledText.getDisplay();
		
		// normal colors
		this.colorTable[0] = new Color(this.display, 0, 0, 0);       // black
		this.colorTable[1] = new Color(this.display, 204, 0, 0);     // red
		this.colorTable[2] = new Color(this.display, 0, 204, 0);     // green
		this.colorTable[3] = new Color(this.display, 204, 204, 0);   // yellow
		this.colorTable[4] = new Color(this.display, 0, 0, 204);     // blue
		this.colorTable[5] = new Color(this.display, 204, 0, 204);   // magenta
		this.colorTable[6] = new Color(this.display, 0, 204, 204);   // cyan
		this.colorTable[7] = new Color(this.display, 192, 192, 192); // white
		this.colorTable[8] = null; // not used
		this.colorTable[9] = null; // not used
		
		// bright colors
		this.colorTable[10] = new Color(this.display, 128, 128, 128); // black
		this.colorTable[11] = new Color(this.display, 255, 0, 0);     // red
		this.colorTable[12] = new Color(this.display, 0, 255, 0);     // green
		this.colorTable[13] = new Color(this.display, 255, 255, 0);   // yellow
		this.colorTable[14] = new Color(this.display, 0, 0, 255);     // blue
		this.colorTable[15] = new Color(this.display, 255, 0, 255);   // magenta
		this.colorTable[16] = new Color(this.display, 0, 255, 255);   // cyan
		this.colorTable[17] = new Color(this.display, 255, 255, 255); // white
		this.colorTable[18] = null; // not used
		this.colorTable[19] = null; // not used
	}
	
	@Override
	public void lineGetStyle(LineStyleEvent event) {
		int start = event.lineOffset;
		int end = event.lineOffset + event.lineText.length();
		StyleRange firstStyle = null;
		ArrayList<StyleRange> applicableStyles = new ArrayList<StyleRange>();
		for (int i = 0; i < queuedStyles.size(); i++) {
			StyleRange currentStyle = queuedStyles.get(i);
			if (currentStyle.start >= start && currentStyle.start <= end) {
				applicableStyles.add(currentStyle);
				queuedStyles.remove(i);
				if (firstStyle == null || currentStyle.start < firstStyle.start) {
					firstStyle = currentStyle;
				}
				i--;
			}
		}
		if (lastStyle != null) {
			StyleRange initialStyle = new StyleRange();
			initialStyle.start = start;
			if (firstStyle == null) {
				initialStyle.length = end - start;				
			} else {
				initialStyle.length = firstStyle.start - start - 1;
			}
			if (initialStyle.length > 0) {
				if (lastStyle.fontStyle != SWT.NORMAL ||
						lastStyle.foreground != null ||
						lastStyle.background != null ||
						lastStyle.underline != false) {
					initialStyle.fontStyle = lastStyle.fontStyle;
					initialStyle.foreground = lastStyle.foreground;
					initialStyle.background = lastStyle.background;
					initialStyle.underline = lastStyle.underline;
					initialStyle.underlineStyle = lastStyle.underlineStyle;
					initialStyle.underlineColor = lastStyle.underlineColor;
					applicableStyles.add(initialStyle);
					lastStyle = initialStyle;
				}
			}
		}
		StyleRange[] styles = new StyleRange[applicableStyles.size()];
		for (int i = 0; i < applicableStyles.size(); i++) {
			styles[i] = applicableStyles.get(i);
		}
		event.styles = styles;
	}
	
	@Override
	public void verifyText(VerifyEvent event) {
		StringBuilder buffer = new StringBuilder(event.text);
		StringBuilder remainder = new StringBuilder();
		int offset = event.start;
		while (buffer.length() > 0) {
			byte[] bytes = buffer.toString().getBytes();
			if (bytes[0] == 27 || bytes[0] == 155) {
				Matcher matcher = controlSequencePattern.matcher(buffer);
				if (matcher.find() && matcher.start() == 0) {
					String controlSequence = matcher.group();
					buffer.replace(0, controlSequence.length(), "");
					matcher = controlSequencePattern.matcher(buffer);
					int start = offset + remainder.length();
					int length = 0;
					if (matcher.find()) {
						length = matcher.start();
					} else {
						length = buffer.length();
					}
					char sequenceType = parseSequenceType(controlSequence);
					int[] codes = parseSequenceCodes(controlSequence);
					if (sequenceType == 'm') {
						StyleRange newStyleRange = buildStyleRange(start, length, codes);
						queuedStyles.add(newStyleRange);
						lastStyle = newStyleRange;
					} else {
						System.err.println("Unhandled ANSI sequence type: " + sequenceType);
					}
				} else {
					remainder.append(buffer.charAt(0));
					buffer.deleteCharAt(0);
				}
			} else {
				remainder.append(buffer.charAt(0));
				buffer.deleteCharAt(0);
			}
		}
		event.text = remainder.toString();
	}

	private StyleRange buildStyleRange(int start, int length, int[] codes) {
		StyleRange newStyleRange = new StyleRange();
		newStyleRange.start = start;
		newStyleRange.length = length;
		for (int i = 0; i < codes.length; i++) {
			Color tempColor = null;
			switch (codes[i]) {
			case 0:
				newStyleRange.foreground = null;
				newStyleRange.background = null;
				newStyleRange.fontStyle = SWT.NORMAL;
				newStyleRange.underline = false;
				newStyleRange.underlineStyle = SWT.UNDERLINE_SINGLE;
				break;
			case 1:
				newStyleRange.fontStyle = SWT.BOLD;
				break;
			case 2:
				// It's actually supposed to be faint, but there's no way to display that
				newStyleRange.fontStyle = SWT.NORMAL;
				break;
			case 3:
				newStyleRange.fontStyle = SWT.ITALIC;
				break;
			case 4:
				newStyleRange.underline = true;
				newStyleRange.underlineStyle = SWT.UNDERLINE_SINGLE;
				break;
			case 7:
				// Swap foreground and background
				tempColor = newStyleRange.foreground; 
				newStyleRange.foreground = newStyleRange.background;
				newStyleRange.background = tempColor;
				break;
			case 21:
				newStyleRange.underline = true;
				newStyleRange.underlineStyle = SWT.UNDERLINE_DOUBLE;
				break;
			case 22:
				newStyleRange.fontStyle = SWT.NORMAL;
				break;
			case 24:
				newStyleRange.underline = false;
				newStyleRange.underlineStyle = 0;
				break;
			case 27:
				// Technically, this should just unset reversed foreground, but we're
				// just going to reverse again
				tempColor = newStyleRange.foreground; 
				newStyleRange.foreground = newStyleRange.background;
				newStyleRange.background = tempColor;
				break;
			default:
				if (codes[i] >= 30 && codes[i] < 40) {
					newStyleRange.foreground = colorTable[codes[i] - 30];
				} else if (codes[i] >= 40 && codes[i] < 50) {
					newStyleRange.background = colorTable[codes[i] - 40];
				} else if (codes[i] >= 90 && codes[i] < 100) {
					newStyleRange.foreground = colorTable[codes[i] - 90 + 10];
				} else if (codes[i] >= 100 && codes[i] < 110) {
					newStyleRange.background = colorTable[codes[i] - 100 + 10];
				}
				break;
			}
		}
		return newStyleRange;
	}
	
	private int[] parseSequenceCodes(String controlSequence) {
		String codeSequence = null;
		if (controlSequence.charAt(0) == '\u009B') {
			codeSequence = controlSequence.substring(1, controlSequence.length() - 1);
		} else if (controlSequence.charAt(0) == '\u001B') {
			codeSequence = controlSequence.substring(2, controlSequence.length() - 1);
		} else {
			throw new RuntimeException("Invalid ANSI control sequence: '" + controlSequence + "'");
		}
		String[] codeStrings = codeSequence.toString().split(";");
		if (codeStrings.length == 0) {
			return new int[] {0};
		} else {
			int[] codes = new int[codeStrings.length];
			for (int i = 0; i < codeStrings.length; i++) {
				if (codeStrings[i].equals("")) {
					codes[i] = 0;
				} else {
					codes[i] = Integer.parseInt(codeStrings[i]);
				}
			}
			return codes;
		}
	}

	private char parseSequenceType(String controlSequence) {
		return controlSequence.charAt(controlSequence.length() - 1);
	}

	public void dispose() {
		styledText = null;
		display = null;
		for (int i = 0; i < 20; i++) {
			if (colorTable[i] != null) {
				colorTable[i].dispose();
			}
		}
	}
}
