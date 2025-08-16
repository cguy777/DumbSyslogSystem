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
import java.util.Scanner;

/**
 * The base class which should be extended by all other commands.
 * You must override the {@link execute()} method to perform whatever action that is needed.
 * You must define the commandString (what input is needed to execute the command).
 * Once a new command class has been created, it must be instantiated and added to the {@link FiCLI}.
 * Whenever input is passed to the InputParser, if it matches the command string, the execute method will run.
 * Define the String {@link commandDescription} to either provide help, show a description, or an example on how to use the command.
 * @author noahm
 *
 */
public abstract class FiCommand {
	
	/**
	 * This is the String that will launch this command.
	 */
	public String commandString;
	
	/**
	 * A list of arguments derived from the input.
	 */
	public ArrayList<String> arguments;
	
	/**
	 * The description of the command.
	 * Shown when all of the commands are listed.
	 */
	public String commandDescription = "";

	/**
	 * Change this to false if you don't want a command to be listed.
	 */
	public boolean isVisible = true;
	
	/**
	 * You must determine what string this command will respond to (the commandString parameter).
	 * @param commandString
	 */
	public FiCommand(String commandString) {
		this.commandString = commandString;
		
		arguments = new ArrayList<>();
	}
	
	/**
	 * Override this method to perform whatever function is needed.
	 */
	public abstract void execute();
	
	/**
	 * Determines if the command that was passed matches the defined command string.
	 * Fills the arguments ArrayList if it contains any arguments.
	 * @param cString
	 * @return
	 */
	public final boolean isCommand(String cString) {
			
		//Cleanup from the last time the command was called.
		arguments.clear();
			
		//Check for obviously different commands and proceed accordingly
		if(cString.length() < commandString.length())
			return false;
			
		String compareString = cString.substring(0, commandString.length());
		
		//Compare the command string and scan for any arguments
		if(compareString.compareTo(commandString) == 0) {
			String argumentsString = cString.substring(commandString.length());
			
			arguments = getTokens(argumentsString);
			if(arguments == null)
				return false;
			
			return true;
		} else
			return false;
	}
	
	private ArrayList<String> getTokens(String s) {
		ArrayList<String> tokens = new ArrayList<>();
		
		int pos = 0;
		while(pos < s.length()) {
			char c = s.charAt(pos);
			
			if(c == ' ') {
				pos++;
				continue;
			}
			
			String token = null;
			
			//Quotation delineated
			if(c == '"')
				token = readToDelineator(s, pos + 1, '"', false);
			//Whole word (whitespace delineated)
			else if(c != ' ')
				token = readToDelineator(s, pos, ' ', true);
			
			//Return null for error
			if(token == null)
				return null;
			
			tokens.add(token);
			pos += token.length() + 2;
			pos++;
		}
		
		return tokens;
	}
	
	private String readToDelineator(String s, int startPos, char del, boolean acceptEOL) {
		String token = "";
		boolean escaping = false;
		for(int i = startPos; i < s.length(); i++) {
			char c = s.charAt(i);
			
			if(c == '\\') {
				if(!escaping) {
					escaping = true;
					continue;
				}
			}
			
			if(c == del) {
				if(!escaping)
					return token;
			}
			
			token += c;
			escaping = false;
		}
		
		if(acceptEOL)
			return token;
		else
			return null;
	}
}
