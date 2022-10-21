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
package io.goobi.viewer.model.cms.pages.content.types;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.persistence.annotations.PrivateOwned;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.RandomComparator;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.cms.pages.content.CMSCategoryHolder;
import io.goobi.viewer.model.cms.pages.content.CMSContent;
import io.goobi.viewer.model.jsf.CheckboxSelectable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "cms_content_pagelist")
public class CMSPageListContent extends CMSContent implements CMSCategoryHolder {

    private static final String COMPONENT_NAME = "pagelist";
    private static final int DEFAULT_ITEMS_PER_VIEW = 10;

    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "cms_content_pagelist_categories", joinColumns = @JoinColumn(name = "content_id"), inverseJoinColumns = @JoinColumn(name = "category_id"))
    private List<CMSCategory> categories = new ArrayList<>();
    
    @Column(name="items_per_view")
    private int itemsPerView;
    @Column(name="group_by_category")
    private boolean groupByCategory = false;
    
    @Transient List<CheckboxSelectable<CMSCategory>> selectableCategories = null;
    
    @Transient
    List<CMSPage> nestedPages = null;
    @Transient
    private int nestedPagesCount = 0;
    
    public CMSPageListContent() {
        super();
        this.categories = new ArrayList<>();
        this.itemsPerView = DEFAULT_ITEMS_PER_VIEW;
    }
    
    private CMSPageListContent(CMSPageListContent orig) {
        super(orig);
        this.categories = orig.categories;
        this.itemsPerView = orig.itemsPerView;
    }
    
    @Override
    public String getBackendComponentName() {
        return COMPONENT_NAME;
    }
    
    public List<CMSCategory> getCategories() {
        return categories;
    }

    public List<CheckboxSelectable<CMSCategory>> getSelectableCategories() throws DAOException {
        if(this.selectableCategories == null) {
            createSelectableCategories();
        }
        return this.selectableCategories;
        
    }

    private void createSelectableCategories() throws DAOException {
        this.selectableCategories = DataManager.getInstance().getDao().getAllCategories()
            .stream()
           .map(cat -> new CheckboxSelectable<>(this.categories, cat, c -> c.getName()))
           .collect(Collectors.toList());
    }

    @Override
    public CMSContent copy() {
        return new CMSPageListContent(this);
    }

    @Override
    public List<File> exportHtmlFragment(String outputFolderPath, String namingScheme) throws IOException, ViewerConfigurationException {
        return Collections.emptyList();
    }

    @Override
    public String handlePageLoad(boolean resetResults) throws PresentationException {
        return null;
    }

    @Override
    public boolean addCategory(CMSCategory category) {
        if(!this.categories.contains(category)) {            
            this.selectableCategories = null; //reset selectable categories
            return this.categories.add(category);
        } else {
            return false;
        }
    }

    @Override
    public boolean removeCategory(CMSCategory category) {
        if(this.categories.contains(category)) {            
            this.selectableCategories = null; //reset selectable categories
            return this.categories.remove(category);
        } else {
            return false;
        }
    }
    
    /**
     * <p>
     * Getter for the field <code>nestedPages</code>.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSPage> getNestedPages(int pageNo, boolean random, boolean paged) throws DAOException {
        if (nestedPages == null) {
            nestedPages = loadNestedPages(pageNo, random, paged);
        }
        return nestedPages;
    }

    /**
     * <p>
     * Getter for the field <code>nestedPages</code>.
     * </p>
     *
     * @param category a {@link io.goobi.viewer.model.cms.CMSCategory} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSPage> getNestedPages(int pageNo, boolean random, boolean paged, CMSCategory category) throws DAOException {
        if (nestedPages == null) {
            nestedPages = loadNestedPages(pageNo, random, paged);
        }
        List<CMSPage> pages = nestedPages.stream()
                .filter(CMSPage::isPublished)
                .filter(child -> this.getCategories().isEmpty()
                        || !CollectionUtils.intersection(this.getCategories(), child.getCategories()).isEmpty())
                .collect(Collectors.toList());
        return pages;
    }
    /**
     * <p>
     * resetData.
     * </p>
     */
    public void resetData() {
        nestedPages = null;
    }
    
    private List<CMSPage> loadNestedPages(int pageNo, boolean random, boolean paged) throws DAOException {
        int size = getItemsPerView();
        int offset = (pageNo - 1) * size;
        AtomicInteger totalPages = new AtomicInteger(0);
        Stream<CMSPage> nestedPagesStream = DataManager.getInstance()
                .getDao()
                .getAllCMSPages()
                .stream()
                .filter(CMSPage::isPublished)
                .filter(child -> getCategories().isEmpty() || !CollectionUtils.intersection(getCategories(), child.getCategories()).isEmpty())
                .peek(child -> totalPages.incrementAndGet());
        if (random) {
            nestedPagesStream = nestedPagesStream.sorted(new RandomComparator<CMSPage>());
        } else {
            nestedPagesStream = nestedPagesStream.sorted(new CMSPage.PageComparator());
        }
        if (paged) {
            nestedPagesStream = nestedPagesStream.skip(offset).limit(size);
        }
        List<CMSPage> nestedPages = nestedPagesStream.collect(Collectors.toList());
        setNestedPagesCount((int) Math.ceil((totalPages.intValue()) / (double) size));
        return nestedPages;
    }
    /**
     * <p>
     * Getter for the field <code>nestedPagesCount</code>.
     * </p>
     *
     * @return a int.
     */
    public int getNestedPagesCount() {
        return nestedPagesCount;
    }
    /**
     * <p>
     * Setter for the field <code>nestedPagesCount</code>.
     * </p>
     *
     * @param nestedPages a int.
     */
    public void setNestedPagesCount(int nestedPages) {
        this.nestedPagesCount = nestedPages;
    }
    
    public int getItemsPerView() {
        return itemsPerView;
    }
    
    public void setItemsPerView(int itemsPerView) {
        this.itemsPerView = itemsPerView;
    }

    public void setGroupByCategory(boolean groupByCategory) {
        this.groupByCategory = groupByCategory;
    }
    
    public boolean isGroupByCategory() {
        return groupByCategory;
    }
}
