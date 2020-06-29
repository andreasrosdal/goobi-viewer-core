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
package io.goobi.viewer.api.rest.v1.users.bookmarks;

import static io.goobi.viewer.api.rest.v1.ApiUrls.USERS_BOOKMARKS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.USERS_BOOKMARKS_ITEM;
import static io.goobi.viewer.api.rest.v1.ApiUrls.USERS_BOOKMARKS_LIST;
import static io.goobi.viewer.api.rest.v1.ApiUrls.USERS_BOOKMARKS_LIST_IIIF;
import static io.goobi.viewer.api.rest.v1.ApiUrls.USERS_BOOKMARKS_LIST_MIRADOR;
import static io.goobi.viewer.api.rest.v1.ApiUrls.USERS_BOOKMARKS_LIST_RSS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.USERS_BOOKMARKS_LIST_RSS_JSON;
import static io.goobi.viewer.api.rest.v1.ApiUrls.USERS_BOOKMARKS_LIST_SHARED;
import static io.goobi.viewer.api.rest.v1.ApiUrls.USERS_BOOKMARKS_LIST_SHARED_MIRADOR;
import static io.goobi.viewer.api.rest.v1.ApiUrls.USERS_BOOKMARKS_PUBLIC;
import static io.goobi.viewer.api.rest.v1.ApiUrls.USERS_BOOKMARKS_SHARED;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;

import de.intranda.api.iiif.presentation.Collection;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.model.SuccessMessage;
import io.goobi.viewer.api.rest.resourcebuilders.AbstractBookmarkResourceBuilder;
import io.goobi.viewer.api.rest.resourcebuilders.SessionBookmarkResourceBuilder;
import io.goobi.viewer.api.rest.resourcebuilders.UserBookmarkResourceBuilder;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RestApiException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.UserBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.bookmark.Bookmark;
import io.goobi.viewer.model.bookmark.BookmarkList;
import io.goobi.viewer.model.rss.Channel;
import io.goobi.viewer.model.rss.RSSFeed;
import io.goobi.viewer.model.security.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * @author florian
 *
 */
@Path(USERS_BOOKMARKS)
@ViewerRestServiceBinding
public class BookmarkResource {

    private AbstractBookmarkResourceBuilder builder;
    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;
    @Inject
    AbstractApiUrlManager urls;

    public BookmarkResource() {
        UserBean bean = BeanUtils.getUserBeanFromRequest(servletRequest);
        if(bean != null) {            
            User currentUser = bean.getUser();
            if(currentUser != null) {
                builder = new UserBookmarkResourceBuilder(currentUser);             
            }
        }
        if(builder == null) {
            HttpSession session = servletRequest.getSession();
            builder = new SessionBookmarkResourceBuilder(session);
        }
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks" },
            summary = "Get all bookmark lists owned by the given user")
    @ApiResponse(responseCode = "400", description = "Invalid user id")
    @ApiResponse(responseCode = "404", description = "No user found for the given id")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    public List<BookmarkList> getOwnedBookmarkLists() throws DAOException, IOException, RestApiException {
        return builder.getAllBookmarkLists();
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks" },
            summary = "Add a new bookmark list")
    @ApiResponse(responseCode = "400", description = "Invalid user id")
    @ApiResponse(responseCode = "403", description = "No user set. Only persistent users may add bookmarklists")
    @ApiResponse(responseCode = "404", description = "No user found for the given id")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    public SuccessMessage addBookmarkList(BookmarkList list) throws DAOException, IOException, RestApiException, IllegalRequestException {
        if (StringUtils.isNotBlank(list.getName())) {
            return builder.addBookmarkList(list.getName());
        }
        return builder.addBookmarkList();
    }

    @GET
    @Path(USERS_BOOKMARKS_LIST)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks" },
            summary = "Get a bookmarklist by its id")
    @ApiResponse(responseCode = "400", description = "Invalid user id")
    @ApiResponse(responseCode = "403", description = "Resource forbidden for user")
    @ApiResponse(responseCode = "404", description = "No user found for the given id")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    public BookmarkList getBookmarkList(
            @Parameter(description = "The id of the bookmark list") @PathParam("listId") Long id)
            throws DAOException, IOException, RestApiException, IllegalRequestException {
        return builder.getBookmarkListById(id);
    }

    @PATCH
    @Path(USERS_BOOKMARKS_LIST)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks" },
            summary = "Set passed attributes to the bookmarkList ")
    @ApiResponse(responseCode = "400", description = "Invalid user id")
    @ApiResponse(responseCode = "403", description = "Resource forbidden for user")
    @ApiResponse(responseCode = "404", description = "No user found for the given id")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    public BookmarkList patchBookmarkList(
            @Parameter(description = "The id of the bookmark list") @PathParam("listId") Long id,
            BookmarkList list) throws DAOException, IOException, RestApiException, IllegalRequestException {
        BookmarkList orig = getBookmarkList(id);
        if(StringUtils.isNotBlank(list.getName())) {
            orig.setName(list.getName());
        }
        if(StringUtils.isNotBlank(list.getDescription())) {
            orig.setDescription(list.getDescription());
        }
        if(list.isIsPublic() != null) {            
            orig.setIsPublic(list.isIsPublic());
        }
        if(StringUtils.isNotBlank(list.getShareKey())) {            
            orig.setShareKey(list.getShareKey());
        }
        builder.updateBookmarkList(orig);
        return orig;
    }
    
    @DELETE
    @Path(USERS_BOOKMARKS_LIST)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks" },
            summary = "Delete a bookmark list")
    @ApiResponse(responseCode = "400", description = "Invalid user id")
    @ApiResponse(responseCode = "403", description = "Resource forbidden for user")
    @ApiResponse(responseCode = "404", description = "No user found for the given id")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    public SuccessMessage deleteBookmarkList(
            @Parameter(description = "The id of the bookmark list") @PathParam("listId") Long id,
            Bookmark item) throws DAOException, IOException, RestApiException, IllegalRequestException {
        return builder.deleteBookmarkList(id);
    }
    
    @POST
    @Path(USERS_BOOKMARKS_LIST)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks" },
            summary = "Add bookmark to list")
    @ApiResponse(responseCode = "400", description = "Invalid user id")
    @ApiResponse(responseCode = "403", description = "Resource forbidden for user")
    @ApiResponse(responseCode = "404", description = "No user found for the given id")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    public BookmarkList addItemToBookmarkList(
            @Parameter(description = "The id of the bookmark list") @PathParam("listId") Long id,
            Bookmark item) throws DAOException, IOException, RestApiException, IllegalRequestException {
        builder.addBookmarkToBookmarkList(id, item.getPi(), item.getLogId(), item.getOrder().toString());
        return builder.getBookmarkListById(id);
    }
    
    @GET
    @Path(USERS_BOOKMARKS_ITEM)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks" },
            summary = "Get a bookmark by its id and the id of the containing list")
    @ApiResponse(responseCode = "400", description = "Invalid user or item id")
    @ApiResponse(responseCode = "403", description = "Resource forbidden for user")
    @ApiResponse(responseCode = "404", description = "No user found for the given id")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    public Bookmark getBookmarkItem(
            @Parameter(description = "The id of the bookmark list") @PathParam("listId") Long listId,
            @Parameter(description = "The id of the bookmark") @PathParam("bookmarkId") Long bookmarkId) throws RestApiException, IllegalRequestException, DAOException, IOException {
        BookmarkList list = getBookmarkList(listId);
        Bookmark item = list.getItems().stream().filter(i -> i.getId().equals(bookmarkId)).findAny().orElse(null);
        if(item  != null) {
            return item;
        } else {
            throw new RestApiException("No item found in list " +  listId + "with id" + bookmarkId, HttpServletResponse.SC_NOT_FOUND);
        }
    }
    
    @DELETE
    @Path(USERS_BOOKMARKS_ITEM)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks" },
            summary = "Delete a bookmark from a list")
    @ApiResponse(responseCode = "400", description = "Invalid user or item id")
    @ApiResponse(responseCode = "403", description = "Resource forbidden for user")
    @ApiResponse(responseCode = "404", description = "No user found for the given id")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    public SuccessMessage deleteBookmarkItem(
            @Parameter(description = "The id of the bookmark list") @PathParam("listId") Long listId,
            @Parameter(description = "The id of the bookmark") @PathParam("bookmarkId") Long bookmarkId) throws RestApiException, IllegalRequestException, DAOException, IOException {
        BookmarkList list = getBookmarkList(listId);
        Bookmark item = list.getItems().stream().filter(i -> i.getId().equals(bookmarkId)).findAny().orElse(null);
        if(item  != null) {
            return builder.deleteBookmarkFromBookmarkList(list.getId(), item.getPi(), item.getLogId(), item.getOrder().toString());
        } else {
            throw new RestApiException("No item found in list " +  listId + "with id" + bookmarkId, HttpServletResponse.SC_NOT_FOUND);
        }
    }
    
    @GET
    @Path(USERS_BOOKMARKS_LIST_IIIF)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks" },
            summary = "Get a bookmarklist by its id")
    @ApiResponse(responseCode = "400", description = "Invalid user id")
    @ApiResponse(responseCode = "403", description = "Resource forbidden for user")
    @ApiResponse(responseCode = "404", description = "No user found for the given id")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    public Collection getBookmarkListAsIIIFCollection(
            @Parameter(description = "The id of the bookmark list") @PathParam("listId") Long id)
            throws DAOException, IOException, RestApiException, IllegalRequestException {
        return builder.getAsCollection(id, urls);
    }
    
    @GET
    @Path(USERS_BOOKMARKS_LIST_MIRADOR)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "users" },
            summary = "Get a bookmarklist by its id as a Mirador viewer config")
    @ApiResponse(responseCode = "400", description = "Invalid user id")
    @ApiResponse(responseCode = "403", description = "Resource forbidden for user")
    @ApiResponse(responseCode = "404", description = "No user found for the given id")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    public String getBookmarkListForMirador(
            @Parameter(description = "The id of the bookmark list") @PathParam("listId") Long id)
            throws DAOException, IOException, RestApiException, IllegalRequestException, ViewerConfigurationException, IndexUnreachableException, PresentationException {
        return builder.getBookmarkListForMirador(id, urls);
    }
    
    @GET
    @Path(USERS_BOOKMARKS_LIST_RSS)
    @Produces({ MediaType.TEXT_XML })
    @Operation(
            tags = { "bookmarks" },
            summary = "Get the bookmark list as an RSS feed")
    @ApiResponse(responseCode = "400", description = "Invalid user id")
    @ApiResponse(responseCode = "403", description = "Resource forbidden for user")
    @ApiResponse(responseCode = "404", description = "No user found for the given id")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    public String getBookmarkListAsRSS(
            @Parameter(description = "The id of the bookmark list") @PathParam("listId") Long id,
            @Parameter(description="Language for RSS metadata")@QueryParam("lang")String language,
            @Parameter(description = "Limit for results to return") @QueryParam("max") Integer maxHits)
            throws DAOException, IOException, RestApiException, ViewerConfigurationException, IndexUnreachableException, PresentationException, ContentLibException {
       BookmarkList list = getBookmarkList(id);
       String query = list.generateSolrQueryForItems();
       return RSSFeed.createRssFeed(language, maxHits, null, query, null, servletRequest);
    }
    
    @GET
    @Path(USERS_BOOKMARKS_LIST_RSS_JSON)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks" },
            summary = "Get the bookmark list as an RSS feed in json format")
    @ApiResponse(responseCode = "400", description = "Invalid user id")
    @ApiResponse(responseCode = "403", description = "Resource forbidden for user")
    @ApiResponse(responseCode = "404", description = "No user found for the given id")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    public Channel getBookmarkListAsRSSJson(
            @Parameter(description = "The id of the bookmark list") @PathParam("listId") Long id,
            @Parameter(description="Language for RSS metadata")@QueryParam("lang")String language,
            @Parameter(description = "Limit for results to return") @QueryParam("max") Integer maxHits)
            throws DAOException, IOException, RestApiException, ViewerConfigurationException, IndexUnreachableException, PresentationException, ContentLibException {
       BookmarkList list = getBookmarkList(id);
       String query = list.generateSolrQueryForItems();
       return RSSFeed.createRssResponse(language, maxHits, null, query, null, servletRequest);
    }
    
    @GET
    @Path(USERS_BOOKMARKS_PUBLIC)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks" },
            summary = "Get all public bookmark lists")
    @ApiResponse(responseCode = "400", description = "Invalid user id")
    @ApiResponse(responseCode = "403", description = "Resource forbidden for user")
    @ApiResponse(responseCode = "404", description = "No user found for the given id")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    public List<BookmarkList> getPublicBookmarkLists()
            throws DAOException, IOException, RestApiException, ViewerConfigurationException, IndexUnreachableException, PresentationException, ContentLibException {
       return builder.getAllPublicBookmarkLists();
    }
    
    @GET
    @Path(USERS_BOOKMARKS_SHARED)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks" },
            summary = "Get all bookmark lists shared with the current user")
    @ApiResponse(responseCode = "400", description = "Invalid user id")
    @ApiResponse(responseCode = "403", description = "Resource forbidden for user")
    @ApiResponse(responseCode = "404", description = "No user found for the given id")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    public List<BookmarkList> getSharedBookmarkLists()
            throws DAOException, IOException, RestApiException, ViewerConfigurationException, IndexUnreachableException, PresentationException, ContentLibException {
       return builder.getAllSharedBookmarkLists();
    }
    
    @GET
    @Path(USERS_BOOKMARKS_LIST_SHARED)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks" },
            summary = "Get a  bookmark list by its share key")
    @ApiResponse(responseCode = "400", description = "Invalid user id")
    @ApiResponse(responseCode = "403", description = "Resource forbidden for user")
    @ApiResponse(responseCode = "404", description = "No user found for the given id")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    public BookmarkList getSharedBookmarkListByKey(
            @Parameter(description = "The share key assigned to the bookmark list") @PathParam("key") String key)
            throws DAOException, IOException, RestApiException, ViewerConfigurationException, IndexUnreachableException, PresentationException, ContentLibException {
       return builder.getSharedBookmarkList(key);
    }
    
    @GET
    @Path(USERS_BOOKMARKS_LIST_SHARED_MIRADOR)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks" },
            summary = "Get a  bookmark list by its share key as a Mirador viewer config")
    @ApiResponse(responseCode = "400", description = "Invalid user id")
    @ApiResponse(responseCode = "403", description = "Resource forbidden for user")
    @ApiResponse(responseCode = "404", description = "No user found for the given id")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    public String getSharedBookmarkListForMirador(
            @Parameter(description = "The share key assigned to the bookmark list") @PathParam("key") String key)
            throws DAOException, IOException, RestApiException, ViewerConfigurationException, IndexUnreachableException, PresentationException, ContentLibException {
       return builder.getSharedBookmarkListForMirador(key, urls);
    }
}
