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
	
	public JPanel filterPanel;
	public JSplitPane filterSplitPane;
	public JScrollPane filterEditorScroll;
	public JTextArea filterEditor;
	public JScrollPane filterEditorOutputScroll;
	public JTextArea filterEditorOutput;
	
	public JLabel caret;
	public JTextField input;
	public JButton enterButton;
	public JPanel lowerPanel;
	
	public FiGUIConsole() {
		this.setLayout(new BorderLayout(5, 5));
		lowerPanel = new JPanel(new BorderLayout(5, 5));
		
		tabbedPane = new JTabbedPane();
		this.add(tabbedPane);
		
		//Syslog tab
		logOutput = new JTextArea();
		logOutput.setEditable(false);
		logOutput.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		logOutput.setVisible(true);
		logScroll = new JScrollPane(logOutput);
		logScroll.setVisible(true);
		tabbedPane.add("Syslogs", logScroll);
		
		//Filter editor tab
		filterPanel = new JPanel();
		filterSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		filterSplitPane.setResizeWeight(0.7);
		filterPanel.add(filterSplitPane);
		tabbedPane.add("Filters", filterSplitPane);
		
		//Filter editor
		filterEditor = new JTextArea();
		filterEditor.setEditable(true);
		filterEditor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		filterEditor.setVisible(true);
		filterEditorScroll = new JScrollPane(filterEditor);
		filterEditorScroll.setVisible(true);
		filterSplitPane.setTopComponent(filterEditorScroll);
		
		//Editor output
		filterEditorOutput = new JTextArea();
		filterEditorOutput.setEditable(false);
		filterEditorOutput.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		filterEditorOutput.setBackground(Color.lightGray);
		filterEditorOutput.setVisible(true);
		filterEditorOutputScroll = new JScrollPane(filterEditorOutput);
		filterEditorOutputScroll.setVisible(true);
		filterSplitPane.setBottomComponent(filterEditorOutputScroll);
		
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
	
	public void clearConsoleLog() {
		logOutput.setText("");
	}
	
	public String getConsoleInputText() {
		return input.getText();
	}
	
	public void clearConsoleInput() {
		input.setText("");
	}
	
	public void printToLog(String text) {
		logOutput.append(text);
		logOutput.setCaretPosition(logOutput.getDocument().getLength());
	}
	
	public void printLineToLog(String text) {
		logOutput.append(text + '\n');
		logOutput.setCaretPosition(logOutput.getDocument().getLength());
	}
}
