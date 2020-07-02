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
package io.goobi.viewer.api.rest.v1.records;

import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_SECTIONS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_SECTIONS_RANGE;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_SECTIONS_RIS_FILE;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_SECTIONS_RIS_TEXT;

import java.net.URISyntaxException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.api.iiif.presentation.IPresentationModelElement;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.resourcebuilders.IIIFPresentationResourceBuilder;
import io.goobi.viewer.api.rest.resourcebuilders.RisResourceBuilder;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.servlets.rest.iiif.presentation.IIIFPresentationBinding;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author florian
 *
 */
@javax.ws.rs.Path(RECORDS_SECTIONS)
@ViewerRestServiceBinding
public class RecordSectionResource {

    private static final Logger logger = LoggerFactory.getLogger(RecordResource.class);
    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;
    @Inject
    private AbstractApiUrlManager urls;

    private final String pi;
    private final String divId;

    public RecordSectionResource(
            @Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi,
            @Parameter(description = "Logical div ID of METS section") @PathParam("divId") String divId) {
        this.pi = pi;
        this.divId = divId;
    }
    
    @GET
    @javax.ws.rs.Path(RECORDS_SECTIONS_RIS_FILE)
    @Produces({ MediaType.TEXT_PLAIN })
    @Operation(tags = { "records"}, summary = "Download ris as file")
    public String getRISAsFile()
            throws PresentationException, IndexUnreachableException, DAOException, ContentLibException {

        StructElement se = getStructElement(pi, divId);
        String fileName = se.getPi() + "_" + se.getLogid() + ".ris";
        servletResponse.addHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        String ris = new RisResourceBuilder(servletRequest, servletResponse).getRIS(se);
        return ris;
    }

    /**
     * <p>
     * getRISAsText.
     * </p>
     *
     * @param iddoc a long.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    @GET
    @javax.ws.rs.Path(RECORDS_SECTIONS_RIS_TEXT)
    @Produces({ MediaType.TEXT_PLAIN })
    @Operation(tags = { "records"}, summary = "Get ris as text")
    public String getRISAsText()
            throws PresentationException, IndexUnreachableException, ContentNotFoundException, DAOException {

        StructElement se = getStructElement(pi, divId);
        return new RisResourceBuilder(servletRequest, servletResponse).getRIS(se);
    }
    
    @GET
    @javax.ws.rs.Path(RECORDS_SECTIONS_RANGE)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = {"records", "iiif"}, summary = "Get IIIF range for section")
    @CORSBinding
    @IIIFPresentationBinding
    public IPresentationModelElement getRange() throws ContentNotFoundException, PresentationException, IndexUnreachableException, URISyntaxException, ViewerConfigurationException, DAOException {
        IIIFPresentationResourceBuilder builder = new IIIFPresentationResourceBuilder(urls);
        return builder.getRange(pi, divId);
    }

    /**
     * @param pi
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    private StructElement getStructElement(String pi, String divId) throws PresentationException, IndexUnreachableException {
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc("+PI_TOPSTRUCT:" + pi + " +DOCTYPE:DOCSTRCT +LOGID:" + divId, null);
        StructElement struct = new StructElement(Long.valueOf((String)doc.getFieldValue(SolrConstants.IDDOC)), doc);
        return struct;
    }
    
}
