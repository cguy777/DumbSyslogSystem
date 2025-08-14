/* BSD 3-Clause License
 *
 * Copyright (c) 2023, Noah McLean
 * All rights reserved.
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

package fibrous.soffit;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Stack;

//*********************************************************
//String Object Framework For Information Transfer (SOFFIT)
//*********************************************************

/**
 * This class provides your basic stream IO processes for SOFFIT.
 * @author noahm
 *
 */
public class SoffitUtil {
	
	public static final String SOFFIT_START = "__SoffitStart";
	public static final String SOFFIT_END = "__SoffitEnd";
	public static final char ESCAPE_SEQUENCE = '\\';
	
	private static int lineNumber = 0;
	
	/**
	 * Parses an {@link InputStream} as a root SOFFIT object.
	 * @param stream
	 * @return The SOFFIT root object as parsed from the InputStream.
	 * @throws SoffitException
	 */
	public static SoffitObject ReadStream(InputStream stream) throws SoffitException {
		lineNumber = 0;
		
		SoffitObject root = new SoffitObject(null, null);
		
		String header = getLine(stream);
		if(!header.equals(SOFFIT_START))
			throw new SoffitException("SOFFIT header not found.");
		
		parseObject(stream, root);
		
		return root;
	}
	
	/**
	 * Writes a SOFFIT object to an {@link OutputStream}.
	 * @param root
	 * @param output
	 */
	public static void WriteStream(SoffitObject root, OutputStream output) throws IOException{
		BufferedOutputStream bStream = new BufferedOutputStream(output);
		
		//Write header
		byte[] lineBytes = convertLineToBytes(SOFFIT_START + "\n");
		bStream.write(lineBytes);
		bStream.flush();
	
		//Write the object itself
		writeObjects(root, bStream);
		bStream.flush();
		
		//Write footer
		lineBytes = convertLineToBytes(SOFFIT_END + "\n");
		bStream.write(lineBytes);
		bStream.flush();
	}
	
	/**
	 * Convenience method to write an object to an {@link OutputStream} and then format that stream into a string.
	 * The returned string appears exactly as it would in in a stream, containing both the header and footer.
	 * @param root
	 * @return
	 */
	public static String WriteStreamToString(SoffitObject root) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			WriteStream(root, baos);
		} catch (IOException e) {
			//This shouldn't really get called
			e.printStackTrace();
		}
		
		return baos.toString();
	}
	
	/**
	 * Convenience method to read a Soffit stream from a formatted string.
	 * @param stream
	 * @return
	 */
	public static SoffitObject ReadStreamFromString(String stream) {
		ByteArrayInputStream bais = new ByteArrayInputStream(stream.getBytes());
		return ReadStream(bais);
	}
	
	/**
	 * Writes fields first, and then every object recursively.
	 * @throws IOException 
	 */
	private static void writeObjects(SoffitObject object, BufferedOutputStream bStream) throws IOException {
		String line = null;
		byte[] lineBytes = null;
		
		//Write the fields first
		for(int i = 0; i < object.getAllFields().size(); i++) {			
			//Write the line containing a field...
			
			SoffitField field = object.getAllFields().get(i);
			
			//Check for null pointer assigned to the field's value.
			if(field.getValue() == null)
				throw new NullPointerException("Value assigned to SOFFIT field \"" + field.getName() + "\" is a null pointer.");
		
			bStream.write(convertFieldToLineBytes(field));
			bStream.flush();
		}
		
		//Write object declarations and then recursively write other objects
		for(int i = 0; i < object.getAllObjects().size(); i++) {
			
			SoffitObject currentObject = object.getAllObjects().get(i);
			//Write the line the object declaration...
			bStream.write(convertObjectDeclarationToLineBytes(currentObject));
			bStream.flush();
			
			//Recursively write the next fields and object declarations.
			writeObjects(currentObject, bStream);
			
			//Write closing brackets.
			line = "";
			
			//Set indentation
			if(currentObject.getParent() != null) {
				for(int i2 = 0; i2 < currentObject.getNestedLevel(); i2++) {
					line += "\t";
				}	
			}
			
			line += "}\n";
			
			//Convert to byte array.
			lineBytes = convertLineToBytes(line);
			
			//Flush the output stream.
			bStream.write(lineBytes);
			bStream.flush();
		}
	}	
	
	/**
	 * Parses and interprets SoffitObjects and its contained SoffitFields and nested SoffitObjects.
	 */
	
	private static void parseObject(InputStream stream, SoffitObject parent) throws SoffitException {
		Stack<SoffitObject> stack = new Stack<>();
		stack.push(parent);
		
		while (!stack.isEmpty()) {
			SoffitObject currentObject = stack.peek();
			String line = getLine(stream);
			
			//If we didn't get anything, then break out.
			if (line == null) {
			    throw new SoffitException("Incomplete SOFFIT stream.");
			}
			
			ArrayList<String> tokens = getLineTokens(line);
			
			//Ensure there are no double quotes in first token (The first token would be an object type, field name, or closing bracket)
			if(containsCharacter(tokens.get(0), '"'))
				throw new SoffitException("SOFFIT syntax error.", lineNumber);
			
			//Closing Bracket
			if (tokens.size() == 1 && tokens.get(0).equals("}")) {
				if (!currentObject.isRoot()) {
					stack.pop();
				} else {
					throw new SoffitException("SOFFIT stream contained too many closing brackets.", lineNumber);
				}
			//SOFFIT Footer
			} else if (tokens.get(0).equals(SOFFIT_END)) {
				if (!currentObject.isRoot()) {
					throw new SoffitException("SOFFIT footer encountered in non-root object.", lineNumber);
				}
				break;
			//Handle Objects
			} else if (isObject(tokens)) {
				SoffitObject object;
				String type = tokens.get(0);
				
				if(tokens.size() == 2) {
					object = new SoffitObject(type, null);
				} else {
					String name = stripQuotations(tokens.get(1));
					
					//Check for proper escape sequences
					try {
						name = convertFromEscapeSequence(name);
					} catch (SoffitException e) {
						throw new SoffitException(e, lineNumber);
					}
					object = new SoffitObject(type, name);
				}
				
				currentObject.add(object);
				stack.push(object);
			//Handle Fields
			} else if (isField(tokens)) {
				String name = tokens.get(0);
				String value = "";
				
				//Set value, if defined.
				if(tokens.size() > 1)
					value = stripQuotations(tokens.get(1));
			
				// Check for proper escape sequences
				try {
					value = convertFromEscapeSequence(value);
				} catch (SoffitException e) {
					throw new SoffitException(e, lineNumber);
				}
				currentObject.add(new SoffitField(name, value));
			} else {
			    throw new SoffitException("SOFFIT syntax error.", lineNumber);
			}
		}
	}
	
	/**
	 * Internal to the parseObject method.
	 */
	private static ArrayList<String> getLineTokens(String s) {
		String line = s;
		ArrayList<String> tokens = new ArrayList<>();
		
		int mark = 0;
		String nextToken = null;
		
		try {
			while(true) {
				nextToken = "";
				
				//Look for quotes
				if(line.charAt(mark) == '"') {
					
					nextToken += line.charAt(mark);
					mark++;
					for(int i = mark;; i++) {
						mark++;
						
						//Check for escape sequence.
						if(line.charAt(i) == '\\') {
							if(line.charAt(i + 1) == '"') {
								nextToken += '"';
								mark++;
								i++;
								continue;
							}
						}
						
						if(line.charAt(i) == '"') {
							nextToken += line.charAt(i);
							break;
						}
						
						nextToken += line.charAt(i);
					}
				} else {
					for(int i = mark;; i++) {
						mark++;
						
						//Check for space
						if(line.charAt(i) == ' ')
							break;
						
						nextToken += line.charAt(i);
					}
				}
				
				if(!nextToken.isBlank() && !nextToken.isEmpty()) {
					nextToken.strip();
					tokens.add(nextToken);
				}
			}
		} catch (IndexOutOfBoundsException e) {
			if(!nextToken.isBlank() && !nextToken.isEmpty()) {
				nextToken.strip();
				tokens.add(nextToken);
			}
		}
		
		return tokens;
	}
	
	private static String getLine(InputStream is) {
	
		while(true) {
			boolean eos = false;
			String line = "";
			lineNumber++;
			
			while(true) {
				
				try {
					int c = is.read();
					
					//Check for EOS;
					if(c == -1) {
						eos = true;
						break;
					}
					
					//Check for new line
					if(c == (int) '\n')
						break;
					if(c == (int) '\r')
						break;
					
					line += (char) c;
					
				} catch(IOException e) {
					//Explicitly return null
					return null;
				}
			}
			
			line = line.strip();
			
			//Check for EOS and essentially a null line
			if(eos && line.length() == 0)
				return null;
			
			//Return if EOS is reached
			if(eos)
				return line;
			
			//Check for blank line
			if(line.isEmpty() || line.isBlank())
				continue;
			
			//Check for comments
			if(line.charAt(0) == '#')
				continue;
			
			if(line != null)
				return line;
		}
	}
	
	/**
	 * Internal to the writeObject method.
	 */
	private static byte[] convertFieldToLineBytes(SoffitField field) {
		
		String name = field.getName();
		String value = field.getValue();
		
		String line = "";
		
		//Set indentation
		for(int i = 0; i < field.getNestingLevel(); i++) {
			line += '\t';
		}
		
		//Name
		for(int i = 0; i < name.length(); i++) {
			line += name.charAt(i);
		}
		line += ' ';
		
		//Value
		//Check for blank value
		if(!field.getValue().isEmpty()) {
			line += '"';
			value = convertToEscapeSequence(value);
			for(int i = 0; i < value.length(); i++) {
				//Add all normal characters
				line += value.charAt(i);
			}
			line += '"';
		}
		
		line += "\n";
		
		return convertLineToBytes(line);
	}
	
	/**
	 * Internal to the writeObjects method.
	 */
	private static byte[] convertObjectDeclarationToLineBytes(SoffitObject object) {		
		String type = object.getType();
		String name = object.getName();
		
		String line = "";
		
		//Set indentation
		for(int i = 0; i < object.getNestedLevel(); i++) {
			line += '\t';
		}
		
		//Type
		for(int i = 0; i < type.length(); i++) {
			line += type.charAt(i);
		}
		line += " ";
		
		//name
		if(name.length() > 0 && name != null) {
			line += '"';
			name = convertToEscapeSequence(name);
			for(int i = 0; i < name.length(); i++) {
				//Add all normal characters
				line += name.charAt(i);
			}
			line += "\" ";
		}
		line += "{\n";
		
		return convertLineToBytes(line);
	}
	
	/**
	 * Internal to the parseObject method.
	 */
	private static String convertFromEscapeSequence(String s) {
		
		String convertedString = "";
		
		for(int i = 0; i < s.length(); i++) {
			//Look for escape character
			if(s.charAt(i) == '\\') {
				
				//Double quote
				if(s.charAt(i + 1) == '"') {
					convertedString += '"';
					i++;
					continue;
				}
				
				//Newline
				if(s.charAt(i + 1) == 'n') {
					convertedString += '\n';
					i++;
					continue;
				}
				
				//Backslash
				if(s.charAt(i + 1) == '\\') {
					convertedString += '\\';
					i++;
					continue;
				}
				
				throw new SoffitException("Invalid SOFFIT escape sequence");
			}
			
			//Add all normal characters
			convertedString += s.charAt(i);
		}
		
		return convertedString;
	}
	
	private static String convertToEscapeSequence(String s) {
		String convertedString = "";
		for(int i = 0; i < s.length(); i++) {
			//Double quote correction
			if(s.charAt(i) == '"') {
				convertedString += "\\\"";
				continue;
			}
			//Newline correction
			if(s.charAt(i) == '\n') {
				convertedString += "\\n";
				continue;
			}
			//Backslash correction
			if(s.charAt(i) == ESCAPE_SEQUENCE) {
				convertedString += "\\\\";
				continue;
			}
			
			//Add all normal characters
			convertedString += s.charAt(i);
		}
		
		return convertedString;
	}
	
	private static String stripQuotations(String s) {
		String stripped = "";
		for(int i = 0; i < s.length() - 2; i++) {
			stripped += s.charAt(i + 1);
		}
		return stripped;
	}
	
	private static boolean isField(ArrayList<String> tokens) {
		if(tokens.size() > 2)
			return false;
		
		String lastToken = tokens.get(tokens.size() - 1);
		
		//Bracket indicates object
		if(lastToken.equals("{"))
			return false;
		
		//Check for quotes
		if(lastToken.charAt(0) == '"' && lastToken.charAt(lastToken.length() - 1) == '"')
			return true;
		
		//Null field
		if(tokens.size() == 1) {
			return true;
		}
		
		//default determination of false
		return false;
	}
	
	private static boolean isObject(ArrayList<String> tokens) {
		
		//Check requirements for object without a name
		if(tokens.size() == 2) {
			//Check for trailing bracket
			//Bracket is required to be an object
			String lastToken = tokens.get(1);
			if(lastToken.equals("{"))
				return true;
		}
		
		//Check requirements for object with a name
		if(tokens.size() == 3) {
			String token2 = tokens.get(1);
			String token3 = tokens.get(2);
			
			//Verify enclosing quotes for name, if a name is specified
			if(token2.charAt(0) == '"' && token2.charAt(token2.length() - 1) == '"') {
				
				//Check for trailing bracket
				if(token3.equals("{"))
					return true;
			}
		}		
		
		//default determination of false
		return false;
	}

	private static byte[] convertLineToBytes(String line) {
		byte[] lineCharArray = new byte[line.length()];
		//Convert to byte array.
		for(int i = 0; i < line.length(); i++) {
			lineCharArray[i] = (byte) line.charAt(i);
		}
	
		return lineCharArray;
	}
	
	private static boolean containsCharacter(String s, char c) {
		for(int i = 0; i < s.length(); i++) {
			if(s.charAt(i) == c)
				return true;
		}
		
		return false;
	}
}
