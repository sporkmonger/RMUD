package com.sporkmonger.rmud.swt.custom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.LineStyleListener;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledTextContent;
import org.eclipse.swt.custom.TextChangeListener;
import org.eclipse.swt.custom.TextChangedEvent;
import org.eclipse.swt.custom.TextChangingEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

public class ANSIStyledContent implements StyledTextContent, LineStyleListener {
	private Display display = null;
	private Color[] colorTable = new Color[20];
	private Pattern controlSequencePattern = Pattern.compile("((\u009B|\u001B\\[)(\\d+;)*(\\d+)?[mABCDEFGHJKSTfnsu])");

	private ArrayList<TextChangeListener> listeners = new ArrayList<TextChangeListener>();

	private StringBuffer content = new StringBuffer();
	private String[] lines = null;

	private ArrayList<StyleRange> styles = new ArrayList<StyleRange>();
	private StyleRange inheritStyle = null;

	public ANSIStyledContent(Display display) {
		this.display = display;

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
		ArrayList<StyleRange> applicableStyles = new ArrayList<StyleRange>();
		int lineEnd = event.lineOffset + event.lineText.length();
		for (StyleRange style : styles) {
			if (style.start >= event.lineOffset && style.start < lineEnd) {
				applicableStyles.add(style);
			}
		}
		StyleRange[] lineStyles = applicableStyles.toArray(new StyleRange[applicableStyles.size()]);
		event.styles = lineStyles;
	}

	@Override
	public void addTextChangeListener(TextChangeListener listener) {
		listeners.add(listener);
	}

	@Override
	public int getCharCount() {
		return content.length();
	}

	private String[] getLines() {
		if (lines == null) {
			if (content.length() == 0) {
				lines = new String[]{""};
			} else {
				ArrayList<String> lineList = new ArrayList<String>();
				int lineStart = 0; 
				for (int i = 0; i < content.length(); i++) {
					if (content.charAt(i) == '\n') {
						lineList.add(content.substring(lineStart, i));
						lineStart = i + 1;
					}
				}
				lines = lineList.toArray(new String[lineList.size()]);
			}
		}
		return lines;
	}
	
	@Override
	public String getLine(int lineIndex) {
		String rawLine = null;
		String[] lines = this.getLines();
		if (lineIndex > lines.length) {
			rawLine = "";
		} else {
			rawLine = lines[lineIndex];
		}
		String result = "";
		if (rawLine.length() > 0) {
			if (rawLine.charAt(rawLine.length() - 1) == '\n') {
				result = rawLine.substring(0, rawLine.length() - 1);
			} else {
				result = rawLine;
			}
		}
		return result;
	}

	@Override
	public int getLineAtOffset(int offset) {
		StringBuilder buffer = new StringBuilder();
		String[] lines = getLines();
		for (int i = 0; i < lines.length; i++) {
			buffer.append(lines + "\n");
			if (buffer.length() > offset) {
				return i;
			}
		}
		return (lines.length - 1);
	}

	@Override
	public int getLineCount() {
		int lineCount = 0;
		int charCount = content.length();
		if (charCount == 0) {
			return 1;
		} else {
			for (int i = 0; i < charCount; i++) {
				if (content.charAt(i) == '\n') {
					lineCount += 1;
				} else if (i == (charCount - 1)) {
					lineCount += 1;
				}
			}
			return lineCount;
		}
	}

	@Override
	public String getLineDelimiter() {
		return "\n";
	}

	@Override
	public int getOffsetAtLine(int lineIndex) {
		StringBuilder buffer = new StringBuilder();
		String[] lines = getLines();
		for (int i = 0; i < lineIndex; i++) {
			buffer.append(lines[i] + "\n");
		}
		return buffer.length();
	}

	@Override
	public String getTextRange(int start, int length) {
		return content.substring(start, start + length);
	}

	@Override
	public void removeTextChangeListener(TextChangeListener listener) {
		listeners.remove(listener);
	}

	@Override
	public void replaceTextRange(int start, int replaceLength, String text) {
		content.replace(start, start + replaceLength, text);
	}

	/**
	 * This method sets plain text.
	 * @param text
	 */
	@Override
	public void setText(String text) {
		content = new StringBuffer();
		content.append(text);
		lines = null;
		for (TextChangeListener listener : listeners) {
			TextChangedEvent event = new TextChangedEvent(this);
			listener.textSet(event);
		}
	}

	/**
	 * This method appends plain text.
	 * @param text
	 */
	public void appendText(String text) {
		for (TextChangeListener listener : listeners) {
			TextChangingEvent event = new TextChangingEvent(this);
			event.newText = text;
			event.newCharCount = text.length();
			event.newLineCount = text.split("\n").length;
			event.replaceCharCount = 0;
			event.replaceLineCount = 0;
			event.start = content.length();
			listener.textChanging(event);
		}
		content.append(text);
		lines = null;
		for (TextChangeListener listener : listeners) {
			TextChangedEvent event = new TextChangedEvent(this);
			listener.textChanged(event);
		}
	}

	/**
	 * This method sets ANSI text.
	 * @param text
	 */
	public void appendANSIText(String text) {
		ArrayList<StyleRange> newStyles = new ArrayList<StyleRange>();

		// Multiple carriage returns in a row should be changed to a single
		// carriage return.  Otherwise it confuses the widget.
		// Buffer contains all unprocessed ANSI control sequences
		StringBuilder buffer = new StringBuilder(text.replaceAll("\r+", ""));

		// The text that has already been processed
		// All indices must be calculated from the offset + remainder length
		// Remainder does not contain any ANSI control sequences
		StringBuilder remainder = new StringBuilder();
		
		int offset = content.length();
		StyleRange firstStyle = null;
		StyleRange lastStyle = inheritStyle;
		
		while (buffer.length() > 0) {
			byte[] bytes = buffer.toString().getBytes();
			if (bytes[0] == 27 || bytes[0] == 155) {
				Matcher matcher = controlSequencePattern.matcher(buffer);
				if (matcher.find() && matcher.start() == 0) {
					String controlSequence = matcher.group();
					// Remove the control sequence from the buffer
					buffer.replace(0, controlSequence.length(), "");
					// Look for the next control sequence, the current sequence stops there
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
						if (firstStyle == null) {
							firstStyle = newStyleRange;
						}
						if (length > 0) {
							newStyles.add(newStyleRange);
							inheritStyle = newStyleRange;
						}
					} else {
						System.err.println("Unhandled ANSI sequence type: " + sequenceType);
					}
				} else {
					// First character wasn't the beginning of a control sequence
					// Move it into the remainder
					remainder.append(buffer.charAt(0));
					buffer.deleteCharAt(0);
				}
			} else {
				remainder.append(buffer.charAt(0));
				buffer.deleteCharAt(0);
			}
		}
		
		Collections.sort(newStyles, new Comparator<StyleRange>() {
			@Override
			public int compare(StyleRange rangeOne, StyleRange rangeTwo) {
				return rangeOne.start - rangeTwo.start;
			}
		});
		
		for (TextChangeListener listener : listeners) {
			TextChangingEvent event = new TextChangingEvent(this);
			event.newText = remainder.toString();
			event.newCharCount = remainder.length();
			if (remainder.toString().equals("\n")) {
				event.newLineCount = 1;
			} else {
				event.newLineCount = remainder.toString().split("\n").length;
			}
			event.replaceCharCount = 0;
			if (content.length() == 0 || content.charAt(content.length() - 1) == '\n') {
				event.replaceLineCount = 0;
			} else {
				event.replaceLineCount = 1;
			}
			event.start = content.length();
			listener.textChanging(event);
		}
		content.append(remainder.toString());
		lines = null;
		if (lastStyle != null) {
			styles.add(buildCarryOverStyleRange(offset, offset + remainder.length(), lastStyle, firstStyle));
		}
		styles.addAll(newStyles);
		for (TextChangeListener listener : listeners) {
			TextChangedEvent event = new TextChangedEvent(this);
			listener.textChanged(event);
		}
	}
	
	private StyleRange buildCarryOverStyleRange(int start, int end, StyleRange lastStyle, StyleRange firstStyle) {
		if (lastStyle != null) {
			StyleRange carryOverStyle = new StyleRange();
			carryOverStyle.start = start;
			if (firstStyle == null) {
				carryOverStyle.length = end - carryOverStyle.start;
			} else {
				carryOverStyle.length = firstStyle.start - carryOverStyle.start;
			}
			if (carryOverStyle.length > 0) {
				if (lastStyle.fontStyle != SWT.NORMAL ||
						lastStyle.foreground != null ||
						lastStyle.background != null ||
						lastStyle.underline != false) {
					carryOverStyle.fontStyle = lastStyle.fontStyle;
					carryOverStyle.foreground = lastStyle.foreground;
					carryOverStyle.background = lastStyle.background;
					carryOverStyle.underline = lastStyle.underline;
					carryOverStyle.underlineStyle = lastStyle.underlineStyle;
					carryOverStyle.underlineColor = lastStyle.underlineColor;
				}
			}
			return carryOverStyle;
		} else {
			return null;
		}
	}
	
	private StyleRange buildStyleRange(int start, int length, int[] codes) {
		StyleRange newStyleRange = new StyleRange();
		newStyleRange.start = start;
		newStyleRange.length = length;
		
		if (inheritStyle != null) {
			// Inherit from previous style
			newStyleRange.foreground = inheritStyle.foreground;
			newStyleRange.background = inheritStyle.background;
			newStyleRange.fontStyle = inheritStyle.fontStyle;
			newStyleRange.underline = inheritStyle.underline;
			newStyleRange.underlineStyle = inheritStyle.underlineStyle;
		}
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
		display = null;
		for (int i = 0; i < 20; i++) {
			if (colorTable[i] != null) {
				colorTable[i].dispose();
			}
		}
	}
}
