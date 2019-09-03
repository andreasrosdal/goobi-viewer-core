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
package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider.SortOrder;
import io.goobi.viewer.managedbeans.tabledata.TableDataSource;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.crowdsourcing.queries.CrowdsourcingQuery;

@Named
@SessionScoped
public class CrowdsourcingBean implements Serializable {

    private static final long serialVersionUID = -6452528640177147828L;

    private static final Logger logger = LoggerFactory.getLogger(CrowdsourcingBean.class);

    private static final int DEFAULT_ROWS_PER_PAGE = 15;

    @Inject
    private NavigationHelper navigationHelper;
    @Inject
    private UserBean userBean;

    private TableDataProvider<Campaign> lazyModelCampaigns;
    private Campaign selectedCampaign;
    private CrowdsourcingQuery selectedQuery;
    private boolean editMode = false;

    @PostConstruct
    public void init() {
        if (lazyModelCampaigns == null) {
            lazyModelCampaigns = new TableDataProvider<>(new TableDataSource<Campaign>() {

                private Optional<Long> numCreatedPages = Optional.empty();

                @Override
                public List<Campaign> getEntries(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
                    try {
                        if (StringUtils.isBlank(sortField)) {
                            sortField = "id";
                        }

                        List<Campaign> ret =
                                DataManager.getInstance().getDao().getCampaigns(first, pageSize, sortField, sortOrder.asBoolean(), filters);
                        return ret;
                    } catch (DAOException e) {
                        logger.error("Could not initialize lazy model: {}", e.getMessage());
                    }

                    return Collections.emptyList();
                }

                @Override
                public long getTotalNumberOfRecords(Map<String, String> filters) {
                    if (!numCreatedPages.isPresent()) {
                        try {
                            numCreatedPages = Optional.ofNullable(DataManager.getInstance().getDao().getCampaignCount(filters));
                        } catch (DAOException e) {
                            logger.error("Unable to retrieve total number of campaigns", e);
                        }
                    }
                    return numCreatedPages.orElse(0l);
                }

                @Override
                public void resetTotalNumberOfRecords() {
                    numCreatedPages = Optional.empty();
                }
            });
            lazyModelCampaigns.setEntriesPerPage(DEFAULT_ROWS_PER_PAGE);
            lazyModelCampaigns.addFilter("CMSPageLanguageVersion", "title_menuTitle");
            lazyModelCampaigns.addFilter("classifications", "classification");
        }
    }

    /**
     * @return
     */
    public static List<Locale> getAllLocales() {
        List<Locale> list = new LinkedList<>();
        list.add(ViewerResourceBundle.getDefaultLocale());
        if (FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getApplication() != null) {
            Iterator<Locale> iter = FacesContext.getCurrentInstance().getApplication().getSupportedLocales();
            while (iter.hasNext()) {
                Locale locale = iter.next();
                if (!list.contains(locale)) {
                    list.add(locale);
                }
            }
        }
        return list;
    }

    public String createNewCampaignAction() {
        selectedCampaign = new Campaign(ViewerResourceBundle.getDefaultLocale());
        return "pretty:adminCrowdAddCampaign";
    }

    public String editCampaignAction(Campaign campaign) {
        selectedCampaign = campaign;
        return "pretty:adminCrowdEditCampaign";
    }

    public String createNewQueryAction() {
        if (selectedCampaign != null) {
            selectedQuery = new CrowdsourcingQuery(selectedCampaign);
        }
        
        return "";
    }

    public String editQueryAction(CrowdsourcingQuery query) {
        selectedQuery = query;
        return "";
    }

    /**
     * @return
     * @throws DAOException
     */
    public List<Campaign> getAllCampaigns() throws DAOException {
        List<Campaign> pages = DataManager.getInstance().getDao().getAllCampaigns();
        return pages;
    }

    /**
     * Adds the current page to the database, if it doesn't exist or updates it otherwise
     *
     * @throws DAOException
     *
     */
    public void saveSelectedCampaign() throws DAOException {
        logger.trace("saveSelectedCampaign");
        try {
            if (userBean == null || !userBean.getUser().isSuperuser()) {
                // Only authorized admins may save
                return;
            }
            if (selectedCampaign == null) {
                return;
            }

            // Save
            boolean success = false;
            Date now = new Date();
            if (selectedCampaign.getDateCreated() == null) {
                selectedCampaign.setDateCreated(now);
            }
            selectedCampaign.setDateUpdated(now);
            logger.trace("update dao");
            if (selectedCampaign.getId() != null) {
                success = DataManager.getInstance().getDao().updateCampaign(selectedCampaign);
            } else {
                success = DataManager.getInstance().getDao().addCampaign(selectedCampaign);
            }
            if (success) {
                Messages.info("crowdsoucing_campaignSaveSuccess");
                logger.trace("reload campaign");
                //                selectedPage = getCMSPage(selectedPage.getId());
                setSelectedCampaign(selectedCampaign);
                logger.trace("update pages");
                lazyModelCampaigns.update();
            } else {
                Messages.error("crowdsourcing_campaignSaveFailure");
            }
        } finally {
            selectedQuery = null;
        }
    }

    public String saveSelectedQueryAction() {
        try {

        } finally {
            selectedQuery = null;
        }

        return "";
    }

    /**
     * @return the lazyModelCampaigns
     */
    public TableDataProvider<Campaign> getLazyModelCampaigns() {
        return lazyModelCampaigns;
    }

    /**
     * @return the selectedCampaign
     */
    public Campaign getSelectedCampaign() {
        return selectedCampaign;
    }

    /**
     * @param selectedCampaign the selectedCampaign to set
     */
    public void setSelectedCampaign(Campaign selectedCampaign) {
        this.selectedCampaign = selectedCampaign;
    }

    /**
     * @return the selectedQuery
     */
    public CrowdsourcingQuery getSelectedQuery() {
        return selectedQuery;
    }

    /**
     * @param selectedQuery the selectedQuery to set
     */
    public void setSelectedQuery(CrowdsourcingQuery selectedQuery) {
        this.selectedQuery = selectedQuery;
    }

    /**
     * @return the editMode
     */
    public boolean isEditMode() {
        return editMode;
    }

    /**
     * @param editMode the editMode to set
     */
    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

}
