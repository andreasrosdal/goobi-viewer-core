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
package io.goobi.viewer.servlets.rest.glossaries;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import io.goobi.viewer.api.rest.ViewerRestServiceBinding;
import io.goobi.viewer.model.glossary.Glossary;
import io.goobi.viewer.model.glossary.GlossaryManager;

/**
 * <p>
 * GlossaryResource class.
 * </p>
 *
 * @author Florian Alpers
 */
@Path("/vocabularies")
@ViewerRestServiceBinding
public class GlossaryResource {

    private static final Logger logger = LoggerFactory.getLogger(GlossaryResource.class);

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    /**
     * <p>
     * listVocabularies.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws java.io.IOException if any.
     */
    @GET
    @Path("/list")
    @Produces({ MediaType.APPLICATION_JSON })
    public List<Glossary> listVocabularies() throws IOException {
        return new GlossaryManager().getGlossaries();
    }

    /**
     * <p>
     * getVocabulary.
     * </p>
     *
     * @param filename a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     * @throws java.io.IOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException if any.
     */
    @GET
    @Path("/{filename}")
    @Produces({ MediaType.APPLICATION_JSON })
    public String getVocabulary(@PathParam("filename") String filename) throws IOException, ContentNotFoundException {
        return new GlossaryManager().getGlossaryAsJson(filename);

    }

}
