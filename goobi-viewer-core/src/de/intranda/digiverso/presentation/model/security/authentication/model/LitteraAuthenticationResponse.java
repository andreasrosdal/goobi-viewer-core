/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.intranda.digiverso.presentation.model.security.authentication.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Representation of the Littera authentication response which is delivered as xml. It only contains 
 * the single information if a login attempt succeeded or not
 * 
 * @author florian
 *
 */
@XmlRootElement(name="Response")
public class LitteraAuthenticationResponse {
	
	private boolean authenticationSuccessful;

	public LitteraAuthenticationResponse() {
	}
	
	public LitteraAuthenticationResponse(boolean success) {
		this.authenticationSuccessful = success;
	}
	
	/**
	 * @return the authenticationSuccessful
	 */
	@XmlAttribute
	public boolean isAuthenticationSuccessful() {
		return authenticationSuccessful;
	}
	
	/**
	 * @param authenticationSuccessful the authenticationSuccessful to set
	 */
	public void setAuthenticationSuccessful(boolean authenticationSuccessful) {
		this.authenticationSuccessful = authenticationSuccessful;
	}
}