/*
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.model.security.authentication;

/**
 * An exception indicating that an error occured when trying to authenticate a user. This should only be used if an internal error occured or the
 * request could not be processed. Ordinary login refusal should not be handled with exceptions
 *
 * @author Florian Alpers
 */
public class AuthenticationProviderException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 2454421697805524935L;

    /**
     * <p>
     * Constructor for AuthenticationProviderException.
     * </p>
     */
    public AuthenticationProviderException() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * <p>
     * Constructor for AuthenticationProviderException.
     * </p>
     *
     * @param arg0 a {@link java.lang.String} object.
     * @param arg1 a {@link java.lang.Throwable} object.
     * @param arg2 a boolean.
     * @param arg3 a boolean.
     */
    public AuthenticationProviderException(String arg0, Throwable arg1, boolean arg2, boolean arg3) {
        super(arg0, arg1, arg2, arg3);
        // TODO Auto-generated constructor stub
    }

    /**
     * <p>
     * Constructor for AuthenticationProviderException.
     * </p>
     *
     * @param arg0 a {@link java.lang.String} object.
     * @param arg1 a {@link java.lang.Throwable} object.
     */
    public AuthenticationProviderException(String arg0, Throwable arg1) {
        super(arg0, arg1);
        // TODO Auto-generated constructor stub
    }

    /**
     * <p>
     * Constructor for AuthenticationProviderException.
     * </p>
     *
     * @param arg0 a {@link java.lang.String} object.
     */
    public AuthenticationProviderException(String arg0) {
        super(arg0);
        // TODO Auto-generated constructor stub
    }

    /**
     * <p>
     * Constructor for AuthenticationProviderException.
     * </p>
     *
     * @param arg0 a {@link java.lang.Throwable} object.
     */
    public AuthenticationProviderException(Throwable arg0) {
        super(arg0);
        // TODO Auto-generated constructor stub
    }

}
