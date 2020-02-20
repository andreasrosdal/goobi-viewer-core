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
package io.goobi.viewer.model.security.authentication;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of {@link io.goobi.viewer.model.security.authentication.IOAuthResponseListener} which keeps all providers waiting for a response in
 * a {@link java.util.concurrent.ConcurrentHashMap}
 *
 * @author Florian Alpers
 */
public class OAuthResponseListener implements IOAuthResponseListener {

    private final ConcurrentHashMap<OpenIdProvider, Boolean> authenticationProviders = new ConcurrentHashMap<>(5, 0.75f, 6);

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IOAuthResponseListener#register(io.goobi.viewer.model.security.authentication.OpenIdProvider)
     */
    /** {@inheritDoc} */
    @Override
    public void register(OpenIdProvider provider) {
        authenticationProviders.put(provider, Boolean.TRUE);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IOAuthResponseListener#unregister(io.goobi.viewer.model.security.authentication.OpenIdProvider)
     */
    /** {@inheritDoc} */
    @Override
    public void unregister(OpenIdProvider provider) {
        authenticationProviders.remove(provider);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IOAuthResponseListener#getProviders()
     */
    /** {@inheritDoc} */
    @Override
    public Set<OpenIdProvider> getProviders() {
        return authenticationProviders.keySet();
    }

}
