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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.SearchBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.itemfunctionality.Functionality;
import io.goobi.viewer.model.cms.itemfunctionality.SearchFunctionality;
import io.goobi.viewer.model.cms.pages.content.CMSContent;
import io.goobi.viewer.model.cms.pages.content.PersistentCMSComponent;
import io.goobi.viewer.model.search.SearchHelper;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "cms_content_search")
@DiscriminatorValue("search")
public class CMSSearchContent extends CMSContent {

    private static final String BACKEND_COMPONENT_NAME = "search";

    @Column(name = "search_prefix")
    private String searchPrefix = "";

    @Column(name = "displayEmptySearchResults")
    private boolean displayEmptySearchResults = false;

    @Transient
    private final SearchFunctionality search;

    public CMSSearchContent() {
        super();
        this.search = initSearch();
    }

    public CMSSearchContent(CMSSearchContent orig) {
        super(orig);
        this.searchPrefix = orig.searchPrefix;
        this.displayEmptySearchResults = orig.displayEmptySearchResults;
        this.search = initSearch();
    }

    private SearchFunctionality initSearch() {
        if (this.getOwningComponent() != null) {
            SearchFunctionality func = new SearchFunctionality(this.searchPrefix,
                    Optional.ofNullable(this.getOwningComponent()).map(c -> c.getOwningPage()).map(p -> p.getPageUrl()).orElse(""));
            func.setPageNo(Optional.ofNullable(this.getOwningComponent()).map(PersistentCMSComponent::getListPage).orElse(1));
            func.setActiveSearchType(SearchHelper.SEARCH_TYPE_REGULAR);
            return func;
        }
        return new SearchFunctionality(this.searchPrefix, "");
    }

    public void setSearchPrefix(String searchPrefix) {
        this.searchPrefix = searchPrefix;
    }

    public String getSearchPrefix() {
        return searchPrefix;
    }

    public void setDisplayEmptySearchResults(boolean displayEmptySearchResults) {
        this.displayEmptySearchResults = displayEmptySearchResults;
    }

    public boolean isDisplayEmptySearchResults() {
        return displayEmptySearchResults;
    }

    public SearchFunctionality getSearch() {
        return search;
    }

    @Override
    public String getBackendComponentName() {
        return BACKEND_COMPONENT_NAME;
    }

    @Override
    public CMSContent copy() {
        return new CMSSearchContent(this);
    }

    @Override
    public List<File> exportHtmlFragment(String outputFolderPath, String namingScheme) throws IOException, ViewerConfigurationException {
        return Collections.emptyList();
    }

    @Override
    public String handlePageLoad(boolean resetResults) throws PresentationException {
        try {
            SearchBean searchBean = BeanUtils.getSearchBean();
            if (searchBean != null) {
                if (resetResults) {
                    searchBean.resetSearchAction();
                    searchBean.setActiveSearchType(SearchHelper.SEARCH_TYPE_REGULAR);
                }
                if (StringUtils.isNotBlank(searchBean.getExactSearchString().replace("-", ""))) {
                    return searchAction();
                } else if (this.isDisplayEmptySearchResults() || StringUtils.isNotBlank(searchBean.getFacets().getCurrentFacetString())) {
                    String searchString = StringUtils.isNotBlank(this.search.getQueryString().replace("-", "")) ? this.search.getQueryString() : "";
                    searchBean.setExactSearchString(searchString);
                    return searchAction();
                }
            }
        } catch (ViewerConfigurationException | IndexUnreachableException | DAOException e) {
            throw new PresentationException("Error setting up search on page load", e);
        }
        return "";
    }

    /**
     * Uses SearchBean to execute a search.
     *
     * @param item a {@link io.goobi.viewer.model.cms.CMSContentItem} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    private String searchAction() throws ViewerConfigurationException, PresentationException, IndexUnreachableException, DAOException {
        this.search.search(this.getOwningPage().getSubThemeDiscriminatorValue());
        BeanUtils.getNavigationHelper().addSearchUrlWithCurrentSortStringToHistory();
        return "";
    }

    @Override
    public String getData(Integer w, Integer h) {
        return "";
    }

    /**
     * Alias for {@link #getSearch()}. Used in legacy templates
     * 
     * @return
     */
    public Functionality getFunctionality() {
        return getSearch();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}
