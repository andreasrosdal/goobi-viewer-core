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
package de.intranda.digiverso.presentation.servlets.rest.rss;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.exceptions.ViewerConfigurationException;
import de.intranda.digiverso.presentation.managedbeans.SearchBean;
import de.intranda.digiverso.presentation.model.bookshelf.Bookshelf;
import de.intranda.digiverso.presentation.model.rss.Channel;
import de.intranda.digiverso.presentation.model.rss.Description;
import de.intranda.digiverso.presentation.model.rss.RSSFeed;
import de.intranda.digiverso.presentation.model.rss.RssItem;
import de.intranda.digiverso.presentation.model.search.SearchFacets;
import de.intranda.digiverso.presentation.model.search.SearchHelper;
import de.intranda.digiverso.presentation.servlets.rest.ViewerRestServiceBinding;
import de.intranda.digiverso.presentation.servlets.utils.ServletUtils;

/**
 * REST resource providing a rss feed as json response
 * 
 * 
 * @author Florian Alpers
 *
 */
@Path("/rss")
@ViewerRestServiceBinding
public class RssResource {

    private static final Logger logger = LoggerFactory.getLogger(RssResource.class);

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    /**
     * Returns the RSS feed containing the @numHits most recently indexed objects All metadata is provided in the passed language
     * 
     * @param numHits
     * @param language
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ViewerConfigurationException
     */
    @GET
    @Path("/{language}/{numhits}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Channel getTagsForPageJson(@PathParam("numhits") int numHits, @PathParam("language") String language)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {

        Long bookshelfId = null;
        String query = null;
        String partnerId = null;

        Channel rss = RSSFeed.createRssFeed(ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest),
                createQuery(query, bookshelfId, partnerId, servletRequest, true), null, numHits, language);

        servletResponse.setContentType("application/json");

        return rss;
    }

    /**
     * Returns the RSS feed containing the @numHits most recently indexed objects. Only objects are returned whose value in the
     * "subthemeDiscriminatorField" matches that provided in @partnerId All metadata is provided in the passed language
     * 
     * @param numHits
     * @param language
     * @param partnerId
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ViewerConfigurationException
     */
    @GET
    @Path("/{language}/{partnerId}/{numhits}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Channel getTagsForPageJson(@PathParam("numhits") int numHits, @PathParam("language") String language,
            @PathParam("partnerId") String partnerId)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {

        Long bookshelfId = null;
        String query = null;
        if (partnerId.equals("-")) {
            partnerId = null;
        }

        Channel rss = RSSFeed.createRssFeed(ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest),
                createQuery(query, bookshelfId, partnerId, servletRequest, true), null, numHits, language);

        servletResponse.setContentType("application/json");

        return rss;
    }

    /**
     * 
     * Returns the RSS feed containing the @numHits most recently indexed objects. Only objects are returned whose value in the
     * "subthemeDiscriminatorField" matches that provided in @partnerId, and further filtered by the given @query All metadata is provided in the
     * passed language
     * 
     * @param partnerId
     * @param numHits
     * @param query
     * @param language
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ConfigurationExViewerConfigurationExceptionception
     */
    @GET
    @Path("/{query}/{language}/{partnerId}/{numhits}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Channel getTagsForPageJson(@PathParam("partnerId") String partnerId, @PathParam("numhits") int numHits, @PathParam("query") String query,
            @PathParam("language") String language)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {

        Long bookshelfId = null;
        if (query.equals("-")) {
            query = null;
        }
        if (partnerId.equals("-")) {
            partnerId = null;
        }

        Channel rss = RSSFeed.createRssFeed(ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest),
                createQuery(query, bookshelfId, partnerId, servletRequest, true), null, numHits, language);

        servletResponse.setContentType("application/json");

        return rss;
    }

    /**
     * 
     * Returns the RSS feed containing the @numHits most recently indexed objects. Only objects are returned whose value in the
     * "subthemeDiscriminatorField" matches that provided in @partnerId and are in the given @bookshelf All metadata is provided in the passed
     * language
     * 
     * @param bookshelfIdString
     * @param numHits
     * @param language
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ViewerConfigurationException
     */
    @GET
    @Path("bookshelf/{bookshelfId}/{language}/{numhits}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Channel getTagsForPageJson(@PathParam("bookshelfId") String bookshelfIdString, @PathParam("numhits") int numHits,
            @PathParam("language") String language)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {

        Long bookshelfId = null;
        if (bookshelfIdString.matches("\\d+")) {
            bookshelfId = Long.parseLong(bookshelfIdString);
        }
        String query = null;
        String partnerId = null;

        Channel rss = RSSFeed.createRssFeed(ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest),
                createQuery(query, bookshelfId, partnerId, servletRequest, true), null, numHits, language);

        servletResponse.setContentType("application/json");

        return rss;
    }

    /**
     * Returns the RSS feed containing the @numHits most recently indexed objects. Only objects are returned that are within the given @bookshelf All
     * metadata is provided in the passed language
     * 
     * @param partnerId
     * @param bookshelfIdString
     * @param numHits
     * @param query
     * @param language
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ViewerConfigurationException
     */
    @GET
    @Path("/{query}/{language}/{bookshelfId}/{partnerId}/{numhits}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Channel getTagsForPageJson(@PathParam("partnerId") String partnerId, @PathParam("bookshelfId") String bookshelfIdString,
            @PathParam("numhits") int numHits, @PathParam("query") String query, @PathParam("language") String language)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {

        Long bookshelfId = null;
        if (bookshelfIdString.matches("\\d+")) {
            bookshelfId = Long.parseLong(bookshelfIdString);
        }
        if (query.equals("-")) {
            query = null;
        }
        if (partnerId.equals("-")) {
            partnerId = null;
        }

        Channel rss = RSSFeed.createRssFeed(ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest),
                createQuery(query, bookshelfId, partnerId, servletRequest, true), null, numHits, language);

        servletResponse.setContentType("application/json");

        return rss;
    }

    /**
     * 
     * @param query
     * @param facets
     * @param language
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ViewerConfigurationException
     * @throws FeedException
     */
    @GET
    @Path("/search/{query}/{facets}/{advancedSearchGroupOperator}/{language}")
    @Produces({ MediaType.APPLICATION_XML })
    public String getSearchRssFeed(@PathParam("query") String query, @PathParam("facets") String facets,
            @PathParam("advancedSearchGroupOperator") String advancedSearchGroupOperator, @PathParam("language") String language)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException, FeedException {
        if (query.equals("-")) {
            query = createQuery(null, null, null, servletRequest, false);
        } else {
            try {
                query = URLDecoder.decode(query, SearchBean.URL_ENCODING);
            } catch (UnsupportedEncodingException e) {
                logger.error(e.getMessage());
            }
            query = SearchHelper.buildFinalQuery(query, DataManager.getInstance().getConfiguration().isAggregateHits());
        }

        // Optional facetting
        List<String> filterQueries = null;
        if (!"-".equals(facets)) {
            SearchFacets searchFacets = new SearchFacets();
            searchFacets.setCurrentFacetString(facets);
            if ("-".equals(advancedSearchGroupOperator)) {
                advancedSearchGroupOperator = "0";
            }
            filterQueries = searchFacets.generateFacetFilterQueries(Integer.valueOf(advancedSearchGroupOperator), true);
        }
        SyndFeedOutput output = new SyndFeedOutput();
        return output
                .outputString(RSSFeed.createRss(ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest), query, filterQueries, language));
    }

    /**
     * 
     * 
     * @param feed
     * @return
     */
    private static Channel createFeed(SyndFeed feed) {
        Channel rss = new Channel();
        rss.setCopyright(feed.getCopyright());
        rss.setDescription(feed.getDescription());
        rss.setLanguage(feed.getLanguage());
        rss.setLink(feed.getLink());
        rss.setPubDate(feed.getPublishedDate());
        rss.setTitle(feed.getTitle());
        for (Object object : feed.getEntries()) {
            if (object instanceof SyndEntry) {
                SyndEntry entry = (SyndEntry) object;

                RssItem item = new RssItem();
                item.setCreator(entry.getAuthor());
                item.setDescription(new Description(entry.getDescription().getValue()));
                item.setLink(entry.getLink());
                item.setPubDate(entry.getPublishedDate());
                item.setTitle(entry.getTitle());

                rss.addItem(item);
            }
        }
        return rss;
    }

    /**
     * @param query
     * @param bookshelfId
     * @param partnerId
     * @param servletRequest
     * @param addSuffixes
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     * @should augment given query correctly
     * @should create basic query correctly
     * @should add suffixes if requested
     */
    static String createQuery(String query, Long bookshelfId, String partnerId, HttpServletRequest servletRequest, boolean addSuffixes)
            throws IndexUnreachableException, PresentationException, DAOException {
        // Build query, if none given
        if (StringUtils.isEmpty(query)) {
            if (bookshelfId != null) {
                // Bookshelf RSS feed
                Bookshelf bookshelf = DataManager.getInstance().getDao().getBookshelf(bookshelfId);
                if (bookshelf == null) {
                    throw new PresentationException("Requested bookshelf not found: " + bookshelfId);
                }
                if (!bookshelf.isPublic()) {
                    throw new PresentationException("Requested bookshelf not public: " + bookshelfId);
                }
                query = bookshelf.generateSolrQueryForItems();
            } else {
                // Main RSS feed
                query = SolrConstants.ISWORK + ":true";
            }
        }

        StringBuilder sbQuery = new StringBuilder();
        sbQuery.append("(").append(query).append(")");

        if (StringUtils.isNotBlank(partnerId)) {
            sbQuery.append(" AND ")
                    .append(DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField())
                    .append(':')
                    .append(partnerId.trim());
        }

        if (addSuffixes) {
            sbQuery.append(
                    SearchHelper.getAllSuffixes(servletRequest, true, true, DataManager.getInstance().getConfiguration().isSubthemeAddFilterQuery()));
        }

        return sbQuery.toString();
    }

}
