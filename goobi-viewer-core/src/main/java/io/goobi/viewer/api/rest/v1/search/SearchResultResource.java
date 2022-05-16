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
package io.goobi.viewer.api.rest.v1.search;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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

import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.model.search.SearchHitChildList;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.NavigationHelper;
import io.goobi.viewer.managedbeans.SearchBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.search.SearchHit;

/**
 * <p>
 * SearchResultResource class.
 * </p>
 */
@Path(ApiUrls.SEARCH)
@ViewerRestServiceBinding
public class SearchResultResource {

    private static final Logger logger = LoggerFactory.getLogger(SearchResultResource.class);

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    /**
     * <p>
     * getTagsForPageJson.
     * </p>
     *
     * @param hitId a {@link java.lang.String} object.
     * @param numChildren a int.
     * @return a {@link io.goobi.viewer.servlets.rest.search.SearchHitChildList} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    @GET
    @Path(ApiUrls.SEARCH_HIT_CHILDREN)
    @Produces({ MediaType.APPLICATION_JSON })
    public SearchHitChildList getTagsForPageJson(@PathParam("id") String hitId, @PathParam("numChildren") int numChildren)
            throws DAOException, PresentationException, IndexUnreachableException, IOException, ViewerConfigurationException {
        // logger.trace("/search/hit/{}/{}/", hitId, numChildren);
        SearchBean searchBean = BeanUtils.getSearchBean();
        if (searchBean == null) {
            servletResponse.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "No instance of SearchBean found in the user session. Execute a search first.");
            return null;
        }
        Locale locale = null;
        NavigationHelper nh = BeanUtils.getNavigationHelper();
        if (nh != null) {
            locale = nh.getLocale();
        }
        List<SearchHit> searchHits = searchBean.getCurrentSearch().getHits();
        if (searchHits != null) {
            for (SearchHit searchHit : searchHits) {
                if (hitId.equals(Long.toString(searchHit.getBrowseElement().getIddoc()))) {
                    // logger.trace("found: {}", hitId);
                    if (searchHit.getHitsPopulated() < numChildren) {
                        searchHit.populateChildren(numChildren - searchHit.getHitsPopulated(), searchHit.getHitsPopulated(), locale,
                                servletRequest, BeanUtils.getImageDeliveryBean().getThumbs());
                    }
                    Collections.sort(searchHit.getChildren());
                    SearchHitChildList searchHitChildren =
                            new SearchHitChildList(searchHit.getChildren(), searchHit.getHitsPopulated(), searchHit.isHasMoreChildren());
                    // logger.trace("children: {}", searchHit.getChildren().size());
                    return searchHitChildren;
                }
            }
        }

        servletResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "IDDOC " + hitId + " is not in the current search result set.");
        return null;
    }
}
