/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2023, Noah McLean
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package fibrous.ficli;

import java.util.ArrayList;

/**
 * Parses text input, and executes commands accordingly.
 * Add {@link FiCommand} objects via the addCommand() method.
 * Will list the available commands and their configured descriptions when the listCommandsString is input.
 * The listCommandsString is configurable via an appropriate constructor.
 * Output from this class can be directed to anything by creating an object that implements {@link FiOutputStream} and passing it through the appropriate constructor.
 * @author noahm
 *
 */
public class FiCLI {
	
	private ArrayList<FiCommand> commands;
	private String listCommandsString = "?";
	private FiOutputStream oStream;
	private FiInputStream iStream;
	
	private String exitString = "exit";
	private String backString = "back";
	
	private String exitDescription = "Exits the application";
	private String backDescription = "Exits this menu";
	
	private boolean canUseExit = true;
	private boolean canUseBack = false;
	
	//Set the default spacing to the default length of back and exit
	private int spacing = 9;
	
	private String caret = "> ";

	/**
	 * Creates a new InputParser with a configurable {@link FiOutputStream} and {@link FiInputStream}.
	 * @param is The {@link FiInputStream} object that this parser will use to directly read input data.
	 * @param os The {@link FiOutputStream} object that this parser will use to directly output data.
	 * @param listCommandsString What input string should show all of the commands that have been configured.
	 */
	public FiCLI(FiInputStream is, FiOutputStream os, String listCommandsString) {
		commands = new ArrayList<>();
		iStream = is;
		oStream = os;
		this.listCommandsString = listCommandsString;
	}
	
	/**
	 * Creates a new InputParser using the default {@link FiConsoleIO}.
	 * @param listCommandsString What input string should show all of the commands that have been configured.
	 */
	public FiCLI(String listCommandsString) {
		commands = new ArrayList<>();
		
		FiConsoleIO consoleIO = new FiConsoleIO();
		iStream = consoleIO;
		oStream = consoleIO;
		
		this.listCommandsString = listCommandsString;
	}
	
	/**
	 * Creates a new InputParser using the default {@link FiConsoleIO}, and a default listCommndsString of '?'.
	 */
	public FiCLI() {
		commands = new ArrayList<>();
		FiConsoleIO consoleIO = new FiConsoleIO();
		iStream = consoleIO;
		oStream = consoleIO;
	}
	
	/**
	 * Adds a command that will be executed when input matches it's defined syntax.
	 * @param command An FiCommand to be added.
	 */
	public void addCommand(FiCommand command) {
		sortAndAddCommand(command);
	}
	
	/**
	 * Sets the list of commands through an {@linkplain ArrayList} containing {@link FiCommand} objects.
	 * @param commands An ArrayList containing FiCommand objects
	 */
	public void setCommands(ArrayList<FiCommand> commands) {
		this.commands = commands;
	}
	
	/**
	 * Returns an ArrayList containing all of the currently configured {@link FiCommand} objects.
	 * @return An ArrayList containing all of the currently configured FiCommand objects.
	 */
	public ArrayList<FiCommand> getCommands() {
		return commands;
	}
	
	/**
	 * Removes all commands.
	 */
	public void clearCommands() {
		commands.clear();
	}
	
	/**
	 * Determines what String will return {@link FiState}.EXIT when doCommand() is called.
	 * This may be synonymous with the FiState.BACK state in certain contexts.
	 * In general, this state is used to determine if the application should exit.
	 * Using this method also automatically allows FiState.EXIT to be returned.
	 * @param s
	 */
	public void setExitString(String s) {
		exitString = s;
		canUseExit = true;
		
		//Ensure our description spacing is correct
		if(exitString.length() + 5 > spacing)
			spacing = exitString.length() + 5;
	}
	
	/**
	 * Determines what String will return {@link FiState}.BACK when doCommand() is called.
	 * This may be synonymous with the FiState.EXIT state in certain contexts and configurations.
	 * In general, this state is used to determine if this parser should close, and not the application itself.
	 * It is useful in menu systems where nested parsers are implemented, and you still want to allow exiting from the application, regardless of where in the CLI the user is.
	 * Using this method also automatically allows FiState.BACK to be returned.
	 * @param s
	 */
	public void setBackString(String s) {
		backString = s;
		canUseBack = true;
		
		//Ensure our description spacing is correct
		if(backString.length() + 5 > spacing)
			spacing = backString.length() + 5;
	}
	
	/**
	 * Determines if the processCommand method can return with FiState.EXIT or BACK.
	 * @param exit
	 * @param back
	 */
	public void allowAdditionalStates(boolean exit, boolean back) {
		canUseExit = exit;
		canUseBack = back;
	}
	
	/**
	 * Sets the description that the exit command will show (when FiState.EXIT is allowed to be returned.)
	 * @param desc
	 */
	public void setExitDescription(String desc) {
		exitDescription = desc;
	}
	
	/**
	 * Sets the description that the back command will show (when FiState.BACK is allowed to be returned.)
	 * @param desc
	 */
	public void setBackDescription(String desc) {
		backDescription = desc;
	}
	
	/**
	 * Sets the caret.
	 * @param c
	 */
	public void setCaret(String c) {
		caret = c;
	}
	
	/**
	 * Returns the currently configured caret.
	 * @return
	 */
	public String getCaret() {
		return caret;
	}
	
	/**
	 * Manually prints the caret to the configured OutputStream.
	 */
	public void printCaret() {
		oStream.print(caret);
	}
	
	/**
	 * Sets the {@link FiInputStream} that this FiCLI will use for input.
	 * @param fis
	 */
	public void setInput(FiInputStream fis) {
		iStream = fis;
	}
	
	/**
	 * Sets the {@link FiOutputStream} that this FiCLI will use for output.
	 * @param fos
	 */
	public void setOutput(FiOutputStream fos) {
		oStream = fos;
	}
	
	/**
	 * Outputs the caret, indicating that input is requested, and then waits for input and reacts accordingly.
	 * Checks a string that is passed and attempts to match it against a stored command or other special command.
	 * If the string is a valid command, it executes the command, and returns true.
	 * If not, nothing happens and it returns false, which allows you to create your own error handling.
	 * @return An {@link FiState} object representing what happened with regards to the command that was input.
	 */
	public FiState processCommand() {
		
		oStream.print(caret);
		
		String commandString = iStream.readLine();
		
		//Check for the string that should list the commands
		if(commandString.compareTo(listCommandsString) == 0) {
			listCommands();
			return new FiState(FiState.VALID, commandString);
		}
		
		//Check for the string that should generally exit the application, if allowed
		if(canUseExit && commandString.compareTo(exitString) == 0)
			return new FiState(FiState.EXIT, commandString);
		
		//Check for the string that should generally exit this parser, if allowed
		if(canUseBack && commandString.compareTo(backString) == 0)
			return new FiState(FiState.BACK, commandString);
		
		for(int i = 0; i < commands.size(); i++) {
			FiCommand command = commands.get(i);
			
			if(command.isCommand(commandString)) {
				command.execute();
				return new FiState(FiState.VALID, commandString);
			}
		}
		
		//Returns false to allow for extensive error handling.
		return new FiState(FiState.INVALID, commandString);
	}
	
	/**
	 * Returns the number of currently configured commands.
	 */
	public int numOfCommands() {
		return commands.size();
	}
	
	/**
	 * Returns the currently configured String/command that will display all currently configured commands.
	 */
	public String getListCommandsString() {
		return listCommandsString;
	}
	
	/**
	 * Returns the currently configured {@link FiInputStream}.
	 * @return
	 */
	public FiInputStream getInputStream() {
		return iStream;
	}
	
	/**
	 * Returns the currently configured {@link FiOutputStream}.
	 * @return
	 */
	public FiOutputStream getOutputStream() {
		return oStream;
	}
	
	/**
	 * Lists all configured commands, plus additional, special commands, if allowed.
	 */
	private void listCommands() {
		oStream.println("\nAvailable Commands:");
		
		if(canUseExit) {
			oStream.println(exitString + addSpaces(exitString) + exitDescription);
		}
		
		if(canUseBack) {
			oStream.println(backString + addSpaces(backString) + backDescription);
		}
		
		for(int i = 0; i < numOfCommands(); i++) {
			
			//Check if the command should be visible or not.
			//If a command isn't visible, it won't be listed,
			//but it can still be executed.
			if(!getCommands().get(i).isVisible)
				continue;
			
			oStream.println(getCommands().get(i).commandString + addSpaces(getCommands().get(i).commandString) + getCommands().get(i).commandDescription);
		}
	}
	
	/**
	 * Adds commands in alphabetical order.
	 * @param c
	 */
	private void sortAndAddCommand(FiCommand c) {
		
		//Set the maximum spacing to make all of the command descriptions line up.
		if(c.commandString.length() + 5 > spacing)
			spacing = c.commandString.length() + 5;
		
		for(int i = 0; i < commands.size(); i++) {
			if(c.commandString.compareTo(commands.get(i).commandString) < 0) {
				commands.add(i, c);
				return;
			}
		}
		
		commands.add(c);
	}
	
	/**
	 * Adds an equalizing amount of spacing so the command descriptions can all line up and look nice.
	 * @param cs The commandString
	 * @return
	 */
	private String addSpaces(String cs) {
		String additionalSpace = "";
		
		int spaces = spacing - cs.length();
		
		for(int i = 0; i < spaces; i++) {
			additionalSpace += " ";
		}
		
		return additionalSpace;
	}
}
