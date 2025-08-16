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

/**
 * This class is returned by the {@link FiCLI} when processCommand() is called.
 * The int "state" will be set to reflect the type of command that was passed.
 * Depending on the FiCLI configuration, the state will be either VALID, INVALID, EXIT, or BACK (0, 1, 2, or 3).
 * The String "input" is the command that was passed to the FiCLI object.
 * @author noahm
 */

public class FiState {
	/**
	 * This indicates that a valid command was recognized and executed.
	 * This does not take into consideration proper syntax for arguments within the command.
	 */
	public final static int VALID = 0;
	
	/**
	 * This indicates that an unrecognized command was input and no action was performed.
	 */
	public final static int INVALID = 1;
	
	/**
	 * This indicates that the String configured for exiting was input.
	 * No action is taken at this time, and it is up to the programmer to define the exit behavior.
	 */
	public final static int EXIT = 2;
	
	/**
	 * This indicates that the String configured for "back" was input.
	 * No action is taken at this time, and it is up to the programmer to define this "back" behavior.
	 */
	public final static int BACK = 3;
	
	/**
	 * The state returned from the system based off of the command that was received.
	 */
	public int state;
	
	/**
	 * The input that was passed as a command.
	 */
	public String input;
	
	/**
	 * Only supposed to be instantiated by an FiCLI object when a command is parsed.
	 * @param state
	 * @param input
	 */
	protected FiState(int state, String input) {
		this.state = state;
		this.input = input;
	}
}