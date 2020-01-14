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
package io.goobi.viewer.model.cms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.persistence.EntityNotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;

/**
 * <p>CMSNavigationManager class.</p>
 */
public class CMSNavigationManager {

    private static final Logger logger = LoggerFactory.getLogger(CMSNavigationManager.class);
    private AtomicInteger visibleItemIdCounter = new AtomicInteger(0);

    private final String associatedTheme;
    private List<SelectableNavigationItem> availableItems;
    private List<CMSNavigationItem> visibleItems;

    //    public static CMSNavigationManager getInstance() {
    //        return instance;
    //    }

    /**
     * <p>Constructor for CMSNavigationManager.</p>
     *
     * @param associatedTheme a {@link java.lang.String} object.
     */
    public CMSNavigationManager(String associatedTheme) {
        this.associatedTheme = associatedTheme;
        try {
            loadItems();
        } catch (DAOException e) {
            logger.error("Could not load items: {}", e.getMessage());
        }
    }

    /**
     * <p>loadItems.</p>
     *
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public final void loadItems() throws DAOException {
        logger.trace("Loading available navigation items");
        availableItems = new ArrayList<>();
        SelectableNavigationItem start = new SelectableNavigationItem("index", "home");
        addAvailableItem(start);
        SelectableNavigationItem search = new SelectableNavigationItem("search", "search");
        addAvailableItem(search);
        SelectableNavigationItem searchadvanced = new SelectableNavigationItem("searchadvanced", "searchAdvanced");
        addAvailableItem(searchadvanced);
        SelectableNavigationItem searchcalendar = new SelectableNavigationItem("searchcalendar", "searchCalendar");
        addAvailableItem(searchcalendar);
        SelectableNavigationItem browse = new SelectableNavigationItem("browse", "browse");
        addAvailableItem(browse);
        SelectableNavigationItem timematrix = new SelectableNavigationItem("timematrix", "timematrix");
        addAvailableItem(timematrix);
        SelectableNavigationItem statistics = new SelectableNavigationItem("statistics", "statistics");
        addAvailableItem(statistics);
        SelectableNavigationItem tags = new SelectableNavigationItem("tags", "tagclouds");
        addAvailableItem(tags);
        SelectableNavigationItem bookmarks = new SelectableNavigationItem("bookmarks", "bookmarkList_bookmarkLists");
        addAvailableItem(bookmarks);
        SelectableNavigationItem user = new SelectableNavigationItem("user", "user");
        user.setDisplayForUsersOnly(true);
        addAvailableItem(user);
        SelectableNavigationItem campaigns = new SelectableNavigationItem("campaigns", "admin__crowdsourcing_campaigns");
        addAvailableItem(campaigns);

        addModuleItems();
        addCMSPageItems();

        visibleItems = loadVisibleItems();
    }

    /**
     * <p>addCMSPageItems.</p>
     *
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void addCMSPageItems() throws DAOException {
        List<CMSPage> cmsPages = BeanUtils.getCmsBean().getAllCMSPages();
        for (CMSPage cmsPage : cmsPages) {
            // Valid pages with a menu title and no relation to a particular record
            if (cmsPage != null && PageValidityStatus.VALID.equals(cmsPage.getValidityStatus()) && StringUtils.isNotBlank(cmsPage.getMenuTitle())
                    && StringUtils.isEmpty(cmsPage.getRelatedPI())) {
                SelectableNavigationItem item = new SelectableNavigationItem(cmsPage);
                addAvailableItem(item);
            }
        }
    }

    /**
     * 
     */
    private void addModuleItems() {
        DataManager.getInstance()
                .getModules()
                .stream()
                .flatMap(module -> module.getCmsMenuContributions().entrySet().stream())
                .map(entry -> new SelectableNavigationItem(entry.getValue(), entry.getKey()))
                .forEach(item -> addAvailableItem(item));

    }

    /**
     * <p>addAvailableItem.</p>
     *
     * @param item a {@link io.goobi.viewer.model.cms.SelectableNavigationItem} object.
     */
    public void addAvailableItem(SelectableNavigationItem item) {
        if (!availableItems.contains(item)) {
            availableItems.add(item);
        }
    }

    /**
     * <p>Getter for the field <code>availableItems</code>.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<SelectableNavigationItem> getAvailableItems() {
        return availableItems;
    }

    /**
     * Add all items from {@link #availableItems} to {@link #visibleItems} for which {@link io.goobi.viewer.model.cms.SelectableNavigationItem#isSelected()} is true. Afterwards
     * sets {@link io.goobi.viewer.model.cms.SelectableNavigationItem#isSelected()} to fals for all {@link #availableItems}
     */
    public void addSelectedItemsToMenu() {
        getAvailableItems().stream()
                .filter(SelectableNavigationItem::isSelected)
                .map(selectedItem -> new CMSNavigationItem(selectedItem))
                .forEach(visibleItem -> addVisibleItem(visibleItem));
        getAvailableItems().forEach(item -> item.setSelected(false));
    }

    /**
     * <p>loadVisibleItems.</p>
     *
     * @return the sublist of available menu items matching the navigationMenuItem-list from the database (in label and url)
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public final List<CMSNavigationItem> loadVisibleItems() throws DAOException {
        List<CMSNavigationItem> daoList = loadItemsFromDatabase().stream().collect(Collectors.toList());
        daoList = cloneItemHierarchy(daoList);
        setVisibleItems(daoList);
        return visibleItems;
    }

    /**
     * @param daoList
     * @return
     */
    private List<CMSNavigationItem> cloneItemHierarchy(List<CMSNavigationItem> items) {
        List<CMSNavigationItem> clones = new ArrayList<>();
        for (CMSNavigationItem item : items) {
            CMSNavigationItem clone = new CMSNavigationItem(item);
            clones.add(clone);
            if (item.getChildItems() != null) {
                List<CMSNavigationItem> childClones = cloneItemHierarchy(item.getChildItems());
                childClones.forEach(cc -> cc.setParentItem(clone));
            }
        }
        return clones;
    }

    /**
     * <p>loadItemsFromDatabase.</p>
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSNavigationItem> loadItemsFromDatabase() throws DAOException {
        String mainTheme = DataManager.getInstance().getConfiguration().getTheme();
        List<CMSNavigationItem> daoList = DataManager.getInstance()
                .getDao()
                .getAllTopCMSNavigationItems()
                .stream()
                .filter(item -> (StringUtils.isBlank(item.getAssociatedTheme()) && mainTheme.equalsIgnoreCase(getAssociatedTheme()))
                        || getAssociatedTheme().equalsIgnoreCase(item.getAssociatedTheme()))
                .collect(Collectors.toList());
        return daoList;
    }

    /**
     * <p>Getter for the field <code>visibleItems</code>.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<CMSNavigationItem> getVisibleItems() {
        return visibleItems;
    }

    /**
     * Adds the given item, along with all their descendants to the visible item list
     *
     * @param items a {@link java.util.List} object.
     */
    public void setVisibleItems(List<CMSNavigationItem> items) {
        this.visibleItems = new ArrayList<>();
        items.stream().map(CMSNavigationItem::getMeWithDescendants).flatMap(l -> l.stream()).forEach(item -> addVisibleItem(item));
    }

    /**
     * Replaces the complete navigation-item database table with the elements of 'visibleItems'
     *
     * @param theme a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void saveVisibleItems(String theme) throws DAOException {

        List<CMSNavigationItem> oldItems = loadItemsFromDatabase();
        for (CMSNavigationItem oldItem : oldItems) {
            deleteItem(oldItem);
        }

        for (CMSNavigationItem item : getVisibleItems()) {
            if (item.getLevel() == 0) {
                addItem(item);
            }
        }
    }

    private static void addItem(CMSNavigationItem item) throws DAOException {
        if (item.getId() != null && DataManager.getInstance().getDao().getCMSNavigationItem(item.getId()) != null) {
            DataManager.getInstance().getDao().updateCMSNavigationItem(item);
        } else {
            DataManager.getInstance().getDao().addCMSNavigationItem(item);
        }
    }

    /**
     * Deletes a navigationitem and all its children from the database
     *
     * @throws DAOException
     */
    private void deleteItem(CMSNavigationItem oldItem) throws ConcurrentModificationException, DAOException {
        if (oldItem.getChildItems() != null && !oldItem.getChildItems().isEmpty()) {
            List<CMSNavigationItem> itemsToDelete = new ArrayList<>();
            List<CMSNavigationItem> deletedItems = new ArrayList<>();
            try {
                for (CMSNavigationItem child : oldItem.getChildItems()) {
                    //                deleteItem(child);
                    itemsToDelete.add(child);
                }
                for (CMSNavigationItem child : itemsToDelete) {
                    deleteItem(child);
                    deletedItems.add(child);
                }
            } catch (ConcurrentModificationException e) {
                for (CMSNavigationItem deletedItem : deletedItems) {
                    addItem(deletedItem);
                }
                throw e;
            }
        }
        if (oldItem.getId() != null) {
            try {
                DataManager.getInstance().getDao().deleteCMSNavigationItem(oldItem);
            } catch (EntityNotFoundException e) {
                //no need to delete
            }
        }

    }

    /**
     * <p>addVisibleItem.</p>
     *
     * @param navigationItem a {@link io.goobi.viewer.model.cms.CMSNavigationItem} object.
     */
    public void addVisibleItem(CMSNavigationItem navigationItem) {
        if (navigationItem.getItemLabel() == null || navigationItem.getPageUrl() == null) {
            logger.error("Missing fields of navigation item: Cannot save");
        } else {
            if (navigationItem.getSortingListId() == null) {
                navigationItem.setSortingListId(visibleItemIdCounter.getAndIncrement());
            }
            visibleItems.add(navigationItem);
        }
    }

    /**
     * <p>reload.</p>
     *
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void reload() throws DAOException {
        loadVisibleItems();
    }

    /**
     * <p>getItem.</p>
     *
     * @param id the item's {@link io.goobi.viewer.model.cms.CMSNavigationItem#getSortingListId()}
     * @return The first matching item from all visible items as optional. Empty optional if no matching item was found
     */
    public Optional<CMSNavigationItem> getItem(String id) {
        return getVisibleItems().stream().filter(item -> item.getSortingListId().toString().equals(id)).findFirst();
    }

    /**
     * <p>Getter for the field <code>associatedTheme</code>.</p>
     *
     * @return the associatedTheme
     */
    public String getAssociatedTheme() {
        return associatedTheme;
    }
}
