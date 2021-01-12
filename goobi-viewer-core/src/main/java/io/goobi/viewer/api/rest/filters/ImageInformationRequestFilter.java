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
package io.goobi.viewer.api.rest.filters;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.apache.commons.text.StringTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotAllowedException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentExceptionMapper.ErrorMessage;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerImageInfoBinding;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;

/**
 * <p>
 * ImageInformationRequestFilter class.
 * </p>
 */
@Provider
@ContentServerImageInfoBinding
public class ImageInformationRequestFilter implements ContainerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(ImageInformationRequestFilter.class);

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    /** {@inheritDoc} */
    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        logger.trace("filter");
        try {
            String pi;
            String imageName = null;
            if (servletRequest.getAttribute("filename") != null) {
                //read parameters 
                pi = (String) servletRequest.getAttribute("pi");
                imageName = (String) servletRequest.getAttribute("filename");
            } else if (servletRequest.getRequestURI().contains("rest/pdf/")) {
                // Old API PDF quickfix
                // TODO Why is this filter even applied to /rest/pdf/mets/foo.xml/-/info.json ?
                String requestPath = servletRequest.getRequestURI();
                requestPath = requestPath.substring(requestPath.indexOf("rest/pdf/") + 9);
                logger.trace("Filtering request: {}", requestPath);
                StringTokenizer tokenizer = new StringTokenizer(requestPath, "/");
                List<String> pathSegments = tokenizer.getTokenList();
                pi = pathSegments.get(1);
                logger.trace("pi: " + pi);
                //                imageName = pathSegments.size() > 3 ? pathSegments.get(3) : "";
            } else {
                String requestPath = servletRequest.getRequestURI();
                requestPath = requestPath.substring(requestPath.indexOf("records/") + 8);
                logger.trace("Filtering request: {}", requestPath);
                StringTokenizer tokenizer = new StringTokenizer(requestPath, "/");
                List<String> pathSegments = tokenizer.getTokenList();
                pi = pathSegments.get(0);
                imageName = pathSegments.size() > 3 ? pathSegments.get(3) : "";
            }
            imageName = StringTools.decodeUrl(imageName);
            // logger.trace("image: {}", imageName);
            if (forwardToCanonicalUrl(pi, imageName, servletRequest, servletResponse)) {
                //if page order is given for image filename, forward to url with correct filename
                return;
            }
            //only for actual image requests, no info requests
            if (imageName != null && !BeanUtils.getImageDeliveryBean().isExternalUrl(imageName)
                    && !BeanUtils.getImageDeliveryBean().isPublicUrl(imageName)
                    && !BeanUtils.getImageDeliveryBean().isStaticImageUrl(imageName)) {
                filterForAccessConditions(request, pi, imageName);
                FilterTools.filterForConcurrentViewLimit(pi, servletRequest);
            }
        } catch (ServiceNotAllowedException e) {
            String mediaType = MediaType.APPLICATION_JSON;
            //            if (request.getUriInfo() != null && request.getUriInfo().getPath().endsWith("json")) {
            //                mediaType = MediaType.APPLICATION_JSON;
            //            }
            Response response = Response.status(Status.FORBIDDEN).type(mediaType).entity(new ErrorMessage(Status.FORBIDDEN, e, false)).build();
            request.abortWith(response);
        } catch (ViewerConfigurationException e) {
            Response response =
                    Response.status(Status.INTERNAL_SERVER_ERROR).entity(new ErrorMessage(Status.INTERNAL_SERVER_ERROR, e, false)).build();
            request.abortWith(response);
        }
    }

    /**
     * <p>
     * forwardToCanonicalUrl.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param imageName a {@link java.lang.String} object.
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @param response a {@link javax.servlet.http.HttpServletResponse} object.
     * @return a boolean.
     * @throws java.io.IOException if any.
     */
    public static boolean forwardToCanonicalUrl(String pi, String imageName, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if (imageName == null || imageName.contains(".") || !imageName.matches("\\d+")) {
            return false;
        }
        //        if (imageName != null && !imageName.contains(".") && imageName.matches("\\d+")) {
        try {
            Optional<String> filename = DataManager.getInstance().getSearchIndex().getFilename(pi, Integer.parseInt(imageName));
            if (filename.isPresent()) {
                String redirectURI = request.getRequestURI().replace("/" + imageName, "/" + filename.get());
                response.sendRedirect(redirectURI);
                return true;
            }
        } catch (NumberFormatException | PresentationException | IndexUnreachableException e) {
            logger.error("Unable to resolve image file for image order {} and pi {}", imageName, pi);
        }
        //        }
        return false;
    }

    /**
     * @param requestPath
     * @param pathSegments
     * @throws ServiceNotAllowedException
     * @throws IndexUnreachableException
     */
    private void filterForAccessConditions(ContainerRequestContext request, String pi, String contentFileName) throws ServiceNotAllowedException {
        logger.trace("filterForAccessConditions: {}", servletRequest.getSession().getId());
        contentFileName = StringTools.decodeUrl(contentFileName);
        boolean access = false;
        try {
            access = AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(pi, null, IPrivilegeHolder.PRIV_LIST, servletRequest);
        } catch (IndexUnreachableException e) {
            throw new ServiceNotAllowedException("Serving this image is currently impossible due to " + e.getMessage());
        } catch (DAOException e) {
            throw new ServiceNotAllowedException("Serving this image is currently impossible due to " + e.getMessage());
        } catch (RecordNotFoundException e) {
            throw new ServiceNotAllowedException("Record not found in index: " + pi);
        }

        if (!access) {
            throw new ServiceNotAllowedException("Serving this image is restricted due to access conditions");
        }
    }

}
