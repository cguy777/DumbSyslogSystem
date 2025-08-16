package fibrous.syslog.dss;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class FiGUIConsole extends JPanel {
	
	public JTabbedPane tabbedPane;
	public JScrollPane logScroll;
	public JTextArea logOutput;
	
	public JSplitPane splitPane;
	public JScrollPane filterEditorScroll;
	public JTextArea filterEditor;
	public JScrollPane consoleOutputScroll;
	public JTextArea consoleOutput;
	
	public JLabel caret;
	public JTextField input;
	public JButton enterButton;
	public JPanel lowerPanel;
	
	public FiGUIConsole() {
		this.setLayout(new BorderLayout(5, 5));
		lowerPanel = new JPanel(new BorderLayout(5, 5));
		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		splitPane.setResizeWeight(0.7);
		tabbedPane = new JTabbedPane();
		this.add(splitPane);
		splitPane.setTopComponent(tabbedPane);
		
		//Syslog tab
		logOutput = new JTextArea();
		logOutput.setEditable(false);
		logOutput.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		logOutput.setVisible(true);
		logScroll = new JScrollPane(logOutput);
		logScroll.setVisible(true);
		tabbedPane.add("Syslogs", logScroll);
		
		//Filter editor
		filterEditor = new JTextArea();
		filterEditor.setEditable(true);
		filterEditor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		filterEditor.setVisible(true);
		filterEditorScroll = new JScrollPane(filterEditor);
		filterEditorScroll.setVisible(true);
		tabbedPane.add("Filters", filterEditorScroll);
		
		//Console output
		consoleOutput = new JTextArea();
		consoleOutput.setEditable(false);
		consoleOutput.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		consoleOutput.setBackground(Color.lightGray);
		consoleOutput.setVisible(true);
		consoleOutputScroll = new JScrollPane(consoleOutput);
		consoleOutputScroll.setVisible(true);
		splitPane.setBottomComponent(consoleOutputScroll);
		
		//Input area
		caret = new JLabel("Input: ");
		lowerPanel.add(caret, BorderLayout.WEST);
		input = new JTextField();
		input.setFocusable(true);
		input.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		input.setVisible(true);
		lowerPanel.add(input, BorderLayout.CENTER);
		enterButton = new JButton("Submit");
		lowerPanel.add(enterButton, BorderLayout.EAST);
		this.add(lowerPanel, BorderLayout.SOUTH);
	}
	
	public void clearLog() {
		logOutput.setText("");
	}
	
	public String getConsoleInputText() {
		return input.getText();
	}
	
	public void clearConsoleInput() {
		input.setText("");
	}
	
	public void clearConsole() {
		consoleOutput.setText("");
	}
	
	public void printToLog(String text) {
		logOutput.append(text);
		logOutput.setCaretPosition(logOutput.getDocument().getLength());
	}
	
	public void printLineToLog(String text) {
		logOutput.append(text + '\n');
		logOutput.setCaretPosition(logOutput.getDocument().getLength());
	}
	
	public void printToConsole(String text) {
		consoleOutput.append(text);
		consoleOutput.setCaretPosition(consoleOutput.getDocument().getLength());
	}
	
	public void printLineToConsole(String text) {
		consoleOutput.append(text + "\n");
		consoleOutput.setCaretPosition(consoleOutput.getDocument().getLength());
	}
}
