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
package io.goobi.viewer.api.rest.filters;

import java.io.IOException;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotAllowedException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentExceptionMapper.ErrorMessage;
import io.goobi.viewer.api.rest.bindings.CrowdsourcingCampaignBinding;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.CrowdsourcingBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.security.user.User;

/**
 * Allows access to crowdsourcing campaign resources for sessions owned by a goobi-viewer user
 * who has access to the campaign.
 * For access check to work, the {@link Campaign#getId() campaign id} must be set as the
 * request attribute "CampaignId"
 *
 * @author florian
 *
 */
@Provider
@CrowdsourcingCampaignBinding
public class CrowdsourcingCampaignFilter implements ContainerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(CrowdsourcingCampaignFilter.class);

    public static final String CAMPAIGN_ID_REQUEST_ATTRIBUTE = "CampaignId";

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        try {
            Object attribute = servletRequest.getAttribute(CAMPAIGN_ID_REQUEST_ATTRIBUTE);
            if (attribute != null && attribute instanceof Long) {
                Long campaignId = (Long) attribute;
                User user = getUser().orElseThrow(() -> new ServiceNotAllowedException("No user logged in"));
                Campaign campaign = DataManager.getInstance().getDao().getCampaign(campaignId);
                if (!CrowdsourcingBean.isAllowed(user, campaign)) {
                    throw new ServiceNotAllowedException("User is not allowed to access requested campaign resources");
                }
            }
        } catch (ServiceNotAllowedException e) {
            String mediaType = MediaType.APPLICATION_JSON;
            Response response = Response.status(Status.FORBIDDEN).type(mediaType).entity(new ErrorMessage(Status.FORBIDDEN, e, false)).build();
            requestContext.abortWith(response);
        } catch (DAOException e) {
            String mediaType = MediaType.APPLICATION_JSON;
            Response response = Response.status(Status.INTERNAL_SERVER_ERROR)
                    .type(mediaType)
                    .entity(new ErrorMessage(Status.INTERNAL_SERVER_ERROR, e, false))
                    .build();
            requestContext.abortWith(response);
        }
    }

    /**
     * @return
     */
    private Optional<User> getUser() {
        try {
            return Optional.ofNullable(BeanUtils.getUserBean().getUser());
        } catch (Throwable e) {
            logger.error("Unable to get current user from session: " + e.toString());
            return Optional.empty();
        }
    }
}
