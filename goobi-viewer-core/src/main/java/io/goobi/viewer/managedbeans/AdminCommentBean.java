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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider.SortOrder;
import io.goobi.viewer.managedbeans.tabledata.TableDataSource;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.annotation.comments.Comment;
import io.goobi.viewer.model.annotation.comments.CommentManager;
import io.goobi.viewer.model.annotation.comments.CommentGroup;
import io.goobi.viewer.model.security.user.User;

@Named
@SessionScoped
public class AdminCommentBean implements Serializable {

    private static final long serialVersionUID = -640422863609139392L;

    private static final Logger logger = LogManager.getLogger(AdminCommentBean.class);

    @Inject
    UserBean userBean;

    private TableDataProvider<Comment> lazyModelComments;

    private CommentGroup commentGroupAll;
    private CommentGroup currentCommentGroup;
    private Comment currentComment = null;

    /**
     * @should sort lazyModelComments by dateCreated desc by default
     */
    @PostConstruct
    public void init() {
        try {
            commentGroupAll = DataManager.getInstance().getDao().getCommentGroupUnfiltered();
        } catch (DAOException e) {
            logger.error(e.getMessage());
        }

        {
            lazyModelComments = new TableDataProvider<>(new TableDataSource<Comment>() {

                @Override
                public List<Comment> getEntries(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
                    try {
                        if (StringUtils.isEmpty(sortField)) {
                            sortField = "dateCreated";
                            sortOrder = SortOrder.DESCENDING;
                        }
                        if (currentCommentGroup == null) {
                            return Collections.emptyList();
                        }
                        if (currentCommentGroup.isCoreType() && userBean != null && userBean.isAdmin()) {
                            return DataManager.getInstance()
                                    .getDao()
                                    .getComments(first, pageSize, sortField, sortOrder.asBoolean(), filters, null);
                        }
                        if (!currentCommentGroup.isIdentifiersQueried()) {
                            CommentManager.queryCommentGroupIdentifiers(currentCommentGroup);
                        }
                        if (currentCommentGroup.getIdentifiers().isEmpty()) {
                            return Collections.emptyList();
                        }
                        return DataManager.getInstance()
                                .getDao()
                                .getComments(first, pageSize, sortField, sortOrder.asBoolean(), filters, currentCommentGroup.getIdentifiers());
                    } catch (DAOException e) {
                        logger.error(e.getMessage());
                    } catch (PresentationException e) {
                        logger.error(e.getMessage());
                    } catch (IndexUnreachableException e) {
                        logger.error(e.getMessage());
                    }
                    return Collections.emptyList();
                }

                @Override
                public long getTotalNumberOfRecords(Map<String, String> filters) {
                    if (currentCommentGroup == null) {
                        return 0;
                    }

                    try {
                        if (currentCommentGroup.isCoreType() && userBean != null && userBean.isAdmin()) {
                            return DataManager.getInstance().getDao().getCommentCount(filters, null, null);
                        }

                        if (!currentCommentGroup.isIdentifiersQueried()) {
                            CommentManager.queryCommentGroupIdentifiers(currentCommentGroup);
                        }
                        if (currentCommentGroup.getIdentifiers().isEmpty()) {
                            return 0;
                        }
                        return DataManager.getInstance().getDao().getCommentCount(filters, null, currentCommentGroup.getIdentifiers());
                    } catch (DAOException e) {
                        logger.error(e.getMessage(), e);
                        return 0;
                    } catch (PresentationException e) {
                        logger.error(e.getMessage(), e);
                        return 0;
                    } catch (IndexUnreachableException e) {
                        logger.error(e.getMessage(), e);
                        return 0;
                    }
                }

                @Override
                public void resetTotalNumberOfRecords() {
                }

            });
            lazyModelComments.setEntriesPerPage(AdminBean.DEFAULT_ROWS_PER_PAGE);
            lazyModelComments.getFilter("body_targetPI");
        }
    }

    /**
     *
     * @return
     */
    public boolean isUserCommentsEnabled() {
        if (commentGroupAll != null) {
            return commentGroupAll.isEnabled();
        }

        return false;
    }

    /**
     *
     * @param userCommentsEnabled
     * @throws DAOException
     */
    public void setUserCommentsEnabled(boolean userCommentsEnabled) throws DAOException {
        if (commentGroupAll != null) {
            if (commentGroupAll.isEnabled() != userCommentsEnabled) {
                commentGroupAll.setEnabled(userCommentsEnabled);
                DataManager.getInstance().getDao().updateCommentGroup(commentGroupAll);
            }
        }
    }

    /**
     *
     * @return
     * @throws DAOException
     */
    public List<CommentGroup> getAllCommentGroups() throws DAOException {
        return DataManager.getInstance().getDao().getAllCommentGroups();
    }

    /**
     *
     * @param user Current user
     * @return Filtered list of available {@link CommentGroup}s to the given user
     * @throws DAOException
     */
    public List<CommentGroup> getCommentGroupsForUser(User user) throws DAOException {
        if (user == null) {
            return Collections.emptyList();
        }
        // logger.trace("user: {}", user.getEmail());

        // Unfiltered list for admins
        if (user.isSuperuser()) {
            return DataManager.getInstance().getDao().getAllCommentGroups();
        }

        // Regular users
        List<CommentGroup> ret = new ArrayList<>();
        for (CommentGroup commentGroup : DataManager.getInstance().getDao().getAllCommentGroups()) {
            if (!commentGroup.isCoreType() && commentGroup.getUserGroup() != null && commentGroup.getUserGroup().getMembersAndOwner().contains(user)) {
                ret.add(commentGroup);
            }
        }

        return ret;
    }

    /**
     *
     */
    public void resetCurrentCommentGroupAction() {
        currentCommentGroup = null;
    }

    /**
     *
     */
    public void newCurrentCommentGroupAction() {
        logger.trace("newCurrentCommentGroupAction");
        currentCommentGroup = new CommentGroup();
    }

    /**
     *
     * @return
     * @throws DAOException
     */
    public String saveCurentCommentGroupAction() throws DAOException {
        return saveCommentGroupAction(currentCommentGroup);
    }

    /**
     * <p>
     * saveCommentGroupAction.
     * </p>
     *
     * @param comment a {@link io.goobi.viewer.model.annotation.comments.CommentGroup} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String saveCommentGroupAction(CommentGroup commentGroup) throws DAOException {
        logger.trace("saveCommentGroupAction");
        if (commentGroup.getId() != null) {
            if (DataManager.getInstance().getDao().updateCommentGroup(commentGroup)) {
                Messages.info("updatedSuccessfully");
                currentCommentGroup = null;
                return "pretty:adminUserCommentGroups";
            }
            Messages.info("errSave");
        } else {
            if (DataManager.getInstance().getDao().addCommentGroup(commentGroup)) {
                Messages.info("addedSuccessfully");
                currentCommentGroup = null;
                return "pretty:adminUserCommentGroups";
            }
            Messages.info("errSave");
        }
        return "";
    }


    /**
     * <p>
     * deleteCommentGroupAction.
     * </p>
     *
     * @param commentGroup a {@link io.goobi.viewer.model.annotation.comments.CommentGroup} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String deleteCommentGroupAction(CommentGroup commentGroup) throws DAOException {
        if (DataManager.getInstance().getDao().deleteCommentGroup(commentGroup)) {
            Messages.info("deletedSuccessfully");
        } else {
            Messages.error("deleteFailure");
        }

        return "";
    }

    // Comments

    /**
     *
     */
    public void resetCurrentCommentAction() {
        currentComment = null;
    }

    /**
     * <p>
     * saveCommentAction.
     * </p>
     *
     * @param comment a {@link io.goobi.viewer.model.annotation.comments.Comment} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void saveCommentAction(Comment comment) throws DAOException {
        logger.trace("saveCommentAction");
        if (comment.getId() != null) {
            // Set updated timestamp
            comment.setDateModified(LocalDateTime.now());
            logger.trace(comment.getContentString());
            if (DataManager.getInstance().getDao().updateComment(comment)) {
                Messages.info("updatedSuccessfully");
            } else {
                Messages.info("errSave");
            }
        } else {
            if (DataManager.getInstance().getDao().addComment(comment)) {
                Messages.info("addedSuccessfully");
            } else {
                Messages.info("errSave");
            }
        }
        resetCurrentCommentAction();
    }

    /**
     * <p>
     * deleteCommentAction.
     * </p>
     *
     * @param comment a {@link io.goobi.viewer.model.annotation.comments.Comment} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String deleteCommentAction(Comment comment) throws DAOException {
        if (DataManager.getInstance().getDao().deleteComment(comment)) {
            Messages.info("commentDeleteSuccess");
        } else {
            Messages.error("commentDeleteFailure");
        }

        return "";
    }

    /**
     * <p>
     * Getter for the field <code>lazyModelComments</code>.
     * </p>
     *
     * @return the lazyModelComments
     */
    public TableDataProvider<Comment> getLazyModelComments() {
        return lazyModelComments;
    }

    /**
     * <p>
     * getPageComments.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<Comment> getPageComments() {
        return lazyModelComments.getPaginatorList();
    }

    /**
     * @return the currentCommentGroup
     */
    public CommentGroup getCurrentCommentGroup() {
        return currentCommentGroup;
    }

    /**
     * @param currentCommentGroup the currentCommentGroup to set
     */
    public void setCurrentCommentGroup(CommentGroup currentCommentGroup) {
        this.currentCommentGroup = currentCommentGroup;
        init();
    }

    /**
     * Returns the ID of <code>currentCommentGroup</code>.
     *
     * @return currentCommentGroup.id
     */
    public Long getCurrentCommentGroupId() {
        if (currentCommentGroup != null && currentCommentGroup.getId() != null) {
            return currentCommentGroup.getId();
        }

        return null;
    }

    /**
     * Sets <code>currentCommentGroup</code> by loading it from the DB via the given ID.
     *
     * @param id
     * @throws DAOException
     */
    public void setCurrentCommentGroupId(Long id) throws DAOException {
        logger.trace("setCurrentCommentGroupId: {}", id);
        try {
            Long longId = id;
            if (ObjectUtils.notEqual(getCurrentCommentGroupId(), longId)) {
                if (id != null) {
                    setCurrentCommentGroup(DataManager.getInstance().getDao().getCommentGroup(longId));
                } else {
                    setCurrentCommentGroup(null);
                }
            }
        } catch (NumberFormatException e) {
            setCurrentCommentGroup(null);
        }
    }

    /**
     * <p>
     * Getter for the field <code>selectedComment</code>.
     * </p>
     *
     * @return the selectedComment
     */
    public Comment getSelectedComment() {
        return currentComment;
    }

    /**
     * <p>
     * Setter for the field <code>selectedComment</code>.
     * </p>
     *
     * @param selectedComment the selectedComment to set
     */
    public void setSelectedComment(Comment selectedComment) {
        this.currentComment = selectedComment;
    }

}
