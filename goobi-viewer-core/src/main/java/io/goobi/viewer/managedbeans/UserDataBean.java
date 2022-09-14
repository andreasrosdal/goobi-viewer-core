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
package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider.SortOrder;
import io.goobi.viewer.managedbeans.tabledata.TableDataSource;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.annotation.CrowdsourcingAnnotation;
import io.goobi.viewer.model.annotation.PersistentAnnotation;
import io.goobi.viewer.model.annotation.comments.Comment;
import io.goobi.viewer.model.annotation.serialization.AnnotationLister;
import io.goobi.viewer.model.annotation.serialization.SqlAnnotationLister;
import io.goobi.viewer.model.annotation.serialization.SqlCommentLister;
import io.goobi.viewer.model.bookmark.Bookmark;
import io.goobi.viewer.model.bookmark.BookmarkList;
import io.goobi.viewer.model.search.Search;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserActivity;

@Named
@ViewScoped
public class UserDataBean implements Serializable {

    private static final long serialVersionUID = -766868003675598285L;

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(UserBean.class);

    private static final int DEFAULT_ROWS_PER_PAGE = 15;

    @Inject
    private UserBean userBean;

    private TableDataProvider<PersistentAnnotation> lazyModelAnnotations;
    private TableDataProvider<PersistentAnnotation> lazyModelComments;

    /**
     * Required setter for ManagedProperty injection
     *
     * @param userBean the userBean to set
     */
    public void setBreadcrumbBean(UserBean userBean) {
        this.userBean = userBean;
    }

    /**
     * Initialize all campaigns as lazily loaded list
     * 
     * @throws DAOException
     */
    @PostConstruct
    public void init() throws DAOException {
        if (lazyModelAnnotations == null) {
            lazyModelAnnotations = initLazyModel(new SqlAnnotationLister());
        }
        if (lazyModelComments == null) {
            lazyModelComments = initLazyModel(new SqlCommentLister());
        }
    }

    @SuppressWarnings("rawtypes")
    private TableDataProvider<PersistentAnnotation> initLazyModel(AnnotationLister lister) {
        TableDataProvider<PersistentAnnotation> model = new TableDataProvider<>(new TableDataSource<PersistentAnnotation>() {

            private Optional<Long> numCreatedPages = Optional.empty();

            @SuppressWarnings("unchecked")
            @Override
            public List<PersistentAnnotation> getEntries(int first, int pageSize, String sortField, SortOrder sortOrder,
                    Map<String, String> filters) {
                if (StringUtils.isBlank(sortField)) {
                    sortField = "id";
                    sortOrder = SortOrder.DESCENDING;
                }
                List<PersistentAnnotation> ret = lister.getAnnotations(first, pageSize, filters.get("targetPI_body_campaign_dateCreated"), null, null,
                        Collections.singletonList(userBean.getUser().getId()), null, null, sortField, sortOrder.asBoolean());
                return ret;
            }

            @SuppressWarnings("unchecked")
            @Override
            public long getTotalNumberOfRecords(Map<String, String> filters) {
                if (!numCreatedPages.isPresent()) {
                    numCreatedPages = Optional.ofNullable(lister.getAnnotationCount(filters.get("targetPI_body_campaign_dateCreated"), null, null,
                            Collections.singletonList(userBean.getUser().getId()), null, null));
                }
                return numCreatedPages.orElse(0l);
            }

            @Override
            public void resetTotalNumberOfRecords() {
                numCreatedPages = Optional.empty();
            }
        });
        model.setEntriesPerPage(DEFAULT_ROWS_PER_PAGE);
        model.setFilters("targetPI_body_campaign_dateCreated");
        return model;
    }

    /**
     * Returns saved searches for the logged in user.
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @should return searches for correct user
     * @should return null if no user logged in
     */
    public List<Search> getSearches(User user, Integer numEntries) throws DAOException {
        return DataManager.getInstance()
                .getDao()
                .getSearches(user)
                .stream()
                .sorted((s1, s2) -> s2.getDateUpdated().compareTo(s1.getDateUpdated()))
                .limit(numEntries == null ? Integer.MAX_VALUE : numEntries)
                .collect(Collectors.toList());
    }

    /**
     *
     * @return
     * @throws DAOException
     */
    public List<CrowdsourcingAnnotation> getAnnotations() throws DAOException {
        if (userBean == null || userBean.getUser() == null) {
            return Collections.emptyList();
        }

        return DataManager.getInstance().getDao().getAnnotationsForUserId(userBean.getUser().getId(), null, null, false);
    }

    /**
     * Deletes the given persistent user search.
     *
     * @param search a {@link io.goobi.viewer.model.search.Search} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String deleteSearchAction(Search search) throws DAOException {
        if (search == null) {
            return "";
        }

        logger.debug("Deleting search query: {}", search.getId());
        if (DataManager.getInstance().getDao().deleteSearch(search)) {
            String msg = ViewerResourceBundle.getTranslation("savedSearch_deleteSuccess", null);
            Messages.info(msg.replace("{0}", search.getName()));
        } else {
            String msg = ViewerResourceBundle.getTranslation("savedSearch_deleteFailure", null);
            Messages.error(msg.replace("{0}", search.getName()));
        }

        return "";
    }

    /**
     * <p>
     * Getter for the field <code>lazyModelAnnotations</code>.
     * </p>
     *
     * @return the lazyModelAnnotations
     */
    public TableDataProvider<PersistentAnnotation> getLazyModelAnnotations() {
        return lazyModelAnnotations;
    }

    public TableDataProvider<PersistentAnnotation> getLazyModelComments() {
        return lazyModelComments;
    }

    public long getNumBookmarkLists(User user) throws DAOException {
        return DataManager.getInstance().getDao().getBookmarkListCount(user);
    }

    public long getNumSearches(User user) throws DAOException {
        return DataManager.getInstance().getDao().getSearchCount(user, null);
    }

    public long getNumComments(User user) throws DAOException {
        // TODO filter via PI whitelist here?
        logger.trace("getNumComments");
        return DataManager.getInstance().getDao().getCommentCount(null, user, null);
    }

    public long getNumAnnotations(User user) throws DAOException {
        return DataManager.getInstance()
                .getDao()
                .getAnnotationCount(Collections.singletonMap("creatorId", String.valueOf(userBean.getUser().getId())));
    }

    public Long getNumRecordsWithComments(User user) throws DAOException {
        return DataManager.getInstance().getDao().getNumRecordsWithComments(user);
    }

    public long getAnnotationCount() throws DAOException {
        if (userBean == null || userBean.getUser() == null) {
            return 0;
        }
        return getNumAnnotations(userBean.getUser());
    }

    public long getCommentCount() throws DAOException {
        if (userBean == null || userBean.getUser() == null) {
            return 0;
        }
        return getNumComments(userBean.getUser());
    }

    /**
     *
     * @param user
     * @param numEntries
     * @return
     * @throws DAOException
     * @should return the latest comments
     */
    public List<Comment> getLatestComments(User user, int numEntries) throws DAOException {
        List<Comment> lastCreatedComments = DataManager.getInstance().getDao().getCommentsOfUser(user, numEntries, "dateCreated", true);
        List<Comment> lastUpdatedComments = DataManager.getInstance().getDao().getCommentsOfUser(user, numEntries, "dateModified", true);

        return CollectionUtils.union(lastCreatedComments, lastUpdatedComments)
                .stream()
                .distinct()
                .sorted((c1, c2) -> c1.compareTo(c2) * -1)
                .limit(numEntries)
                .collect(Collectors.toList());
    }

    public List<BookmarkList> getBookmarkListsForUser(User user, int numEntries) throws DAOException {
        return DataManager.getInstance()
                .getDao()
                .getBookmarkLists(user)
                .stream()
                .sorted()
                .limit(numEntries)
                .collect(Collectors.toList());
    }

    public List<UserActivity> getLatestActivity(User user, int numEntries) throws DAOException {
        List<Search> searches = DataManager.getInstance().getDao().getSearches(user, 0, numEntries, "dateUpdated", true, null);
        List<Bookmark> bookmarks = DataManager.getInstance()
                .getDao()
                .getBookmarkLists(user)
                .stream()
                .flatMap(list -> list.getItems().stream())
                .sorted((bm1, bm2) -> bm1.getDateAdded().compareTo(bm2.getDateAdded()))
                .limit(numEntries)
                .collect(Collectors.toList());
        List<Comment> lastCreatedComments = DataManager.getInstance().getDao().getCommentsOfUser(user, numEntries, "dateCreated", true);
        List<Comment> lastUpdatedComments = DataManager.getInstance()
                .getDao()
                .getCommentsOfUser(user, numEntries, "dateModified", true)
                .stream()
                .filter(c -> c.getDateModified() != null)
                .collect(Collectors.toList());
        List<CrowdsourcingAnnotation> lastCreatedCrowdsourcingAnnotations =
                DataManager.getInstance().getDao().getAnnotationsForUserId(user.getId(), numEntries, "dateCreated", true);
        List<CrowdsourcingAnnotation> lastUpdatedCrowdsourcingAnnotations = DataManager.getInstance()
                .getDao()
                .getAnnotationsForUserId(user.getId(), numEntries, "dateModified", true)
                .stream()
                .filter(c -> c.getDateModified() != null)
                .collect(Collectors.toList());

        Stream<UserActivity> activities = Stream.of(
                searches.stream().map(UserActivity::getFromSearch),
                bookmarks.stream().map(UserActivity::getFromBookmark),
                lastCreatedComments.stream().map(UserActivity::getFromComment),
                lastUpdatedComments.stream().map(UserActivity::getFromCommentUpdate),
                lastCreatedCrowdsourcingAnnotations.stream().map(UserActivity::getFromCampaignAnnotation),
                lastUpdatedCrowdsourcingAnnotations.stream().map(UserActivity::getFromCampaignAnnotationUpdate))
                .flatMap(Function.identity())
                .distinct()
                .sorted((a1, a2) -> a2.getDate().compareTo(a1.getDate()));
        return activities.limit(numEntries).collect(Collectors.toList());
    }
}
