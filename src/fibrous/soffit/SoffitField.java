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

public class SoffitField {
	private SoffitObject parent = null;
	
	private String name;
	private String value;
	
	private int level = -1;
	
	/**
	 * Constructs a SoffitField with a specified name and value.
	 * @param name
	 * @param value
	 */
	public SoffitField(String name, String value) {
		this.name = name;
		this.value = value;
	}
	
	/**
	 * Returns the name/label of this SoffitField.
	 * @return
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Returns the value of this SoffitField.
	 * @return
	 */
	public String getValue() {
		return value;
	}
	
	/**
	 * Sets the value of this SoffitField.
	 * @param value
	 */
	public void setValue(String value) {
		this.value = value;
	}
	
	/**
	 * Returns the parent object of this field.
	 * Returns null if it is directly attached to the root object.
	 * @return
	 */
	public SoffitObject getParent() {
		return parent;
	}
	
	/**
	 * Returns how deeply nested this field is.
	 * Returns 0 if this field is attached directly to the root object.
	 * @return
	 */
	public int getNestingLevel() {
		return level;
	}
	
	/**
	 * Returns the value of the field.
	 */
	@Override
	public String toString() {
		return value;
	}
	
	/**
	 * Sets the parent of this SoffitField.
	 * It is really only called internally when adding a field to another object.
	 * @param parent
	 */
	protected void setParent(SoffitObject parent) {
		this.parent = parent;
		calcNestingLevel(this.parent);
	}
	
	/**
	 * Calculates how deeply nested this field is.
	 * It is really only called internally when adding a field to another object.
	 * @param parent
	 */
	protected void calcNestingLevel(SoffitObject parent) {
		if(parent == null)
			level = 0;
		else			
			level = parent.getNestedLevel() + 1;
	}
}
