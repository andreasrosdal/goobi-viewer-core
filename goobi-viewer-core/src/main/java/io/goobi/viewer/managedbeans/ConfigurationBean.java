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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.faces.annotation.FacesConfig;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageType;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.citation.CitationLink;
import io.goobi.viewer.model.citation.CitationLink.CitationLinkLevel;
import io.goobi.viewer.model.job.download.DownloadOption;
import io.goobi.viewer.model.maps.GeoMapMarker;
import io.goobi.viewer.model.misc.EmailRecipient;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.translations.language.Language;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.modules.IModule;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrSearchIndex;

/**
 * This is a wrapper class for the <code>Configuration</code> class for access from HTML.
 */
@FacesConfig
@Named
@ApplicationScoped
public class ConfigurationBean implements Serializable {

    private static final long serialVersionUID = -1371688138567741188L;

    private static final Logger logger = LogManager.getLogger(ConfigurationBean.class);

    /**
     * Empty constructor.
     */
    public ConfigurationBean() {
        // the emptiness inside
    }

    /**
     * <p>
     * getModules.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<IModule> getModules() {
        return DataManager.getInstance().getModules();
    }

    /**
     * <p>
     * getName.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getName() {
        return DataManager.getInstance().getConfiguration().getName();
    }

    /**
     * <p>
     * isBookshelvesEnabled.
     * </p>
     *
     * @return a boolean.
     */
    @Deprecated
    public boolean isBookshelvesEnabled() {
        return isBookmarksEnabled();
    }

    /**
     * <p>
     * isBookmarksEnabled.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isBookmarksEnabled() {
        return DataManager.getInstance().getConfiguration().isBookmarksEnabled();
    }

    public boolean isSearchSavingEnabled() {
        return DataManager.getInstance().getConfiguration().isSearchSavingEnabled();
    }

    /**
     * <p>
     * useOpenSeadragon.
     * </p>
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public boolean useOpenSeadragon() throws ViewerConfigurationException {
        return DataManager.getInstance().getConfiguration().useOpenSeadragon();
    }

    /**
     * <p>
     * useTiles.
     * </p>
     *
     * @param pageType a {@link java.lang.String} object.
     * @param mimeType a {@link java.lang.String} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public boolean useTiles(String pageType, String mimeType) throws ViewerConfigurationException {
        return DataManager.getInstance().getConfiguration().useTiles(PageType.getByName(pageType), getImageType(mimeType));
    }
    
    /**
     * whether to show a navigator element in the openseadragon viewe
     * @param pageType  get settings for this pageType
     * @param mimeType  get settings for this image type
     * @return  true if navigator should be shown
     * @throws ViewerConfigurationException
     */
    public boolean showImageNavigator(String pageType, String mimeType) throws ViewerConfigurationException {
        return DataManager.getInstance().getConfiguration().showImageNavigator(PageType.getByName(pageType), getImageType(mimeType));
    }

    /**
     * <p>
     * getFooterHeight.
     * </p>
     *
     * @param pageType a {@link java.lang.String} object.
     * @param mimeType a {@link java.lang.String} object.
     * @return a int.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public int getFooterHeight(String pageType, String mimeType) throws ViewerConfigurationException {
        return DataManager.getInstance().getConfiguration().getFooterHeight(PageType.getByName(pageType), getImageType(mimeType));
    }

    /**
     * <p>
     * getImageSizes.
     * </p>
     *
     * @param pageType a {@link java.lang.String} object.
     * @param mimeType a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public List<String> getImageSizes(String pageType, String mimeType) throws ViewerConfigurationException {
        return DataManager.getInstance().getConfiguration().getImageViewZoomScales(PageType.getByName(pageType), getImageType(mimeType));
    }

    /**
     * <p>
     * getTileSizes.
     * </p>
     *
     * @param pageType a {@link java.lang.String} object.
     * @param mimeType a {@link java.lang.String} object.
     * @return a {@link java.util.Map} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public Map<Integer, List<Integer>> getTileSizes(String pageType, String mimeType) throws ViewerConfigurationException {
        return DataManager.getInstance().getConfiguration().getTileSizes(PageType.getByName(pageType), getImageType(mimeType));
    }

    /**
     * <p>
     * useTiles.
     * </p>
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public boolean useTiles() throws ViewerConfigurationException {
        return DataManager.getInstance().getConfiguration().useTiles();
    }

    /**
     * <p>
     * useTilesFullscreen.
     * </p>
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public boolean useTilesFullscreen() throws ViewerConfigurationException {
        return DataManager.getInstance().getConfiguration().useTiles(PageType.viewFullscreen, null);
    }

    /**
     * <p>
     * useTilesCrowd.
     * </p>
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public boolean useTilesCrowd() throws ViewerConfigurationException {
        return DataManager.getInstance().getConfiguration().useTiles(PageType.editContent, null);
    }

    /**
     * <p>
     * getFooterHeight.
     * </p>
     *
     * @return a int.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public int getFooterHeight() throws ViewerConfigurationException {
        return DataManager.getInstance().getConfiguration().getFooterHeight();
    }

    /**
     * <p>
     * getFooterHeightFullscreen.
     * </p>
     *
     * @return a int.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public int getFooterHeightFullscreen() throws ViewerConfigurationException {
        return DataManager.getInstance().getConfiguration().getFooterHeight(PageType.viewFullscreen, null);
    }

    /**
     * <p>
     * getFooterHeightCrowd.
     * </p>
     *
     * @return a int.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public int getFooterHeightCrowd() throws ViewerConfigurationException {
        return DataManager.getInstance().getConfiguration().getFooterHeight(PageType.editContent, null);
    }

    /**
     * <p>
     * isRememberImageZoom.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isRememberImageZoom() {
        return DataManager.getInstance().getConfiguration().isRememberImageZoom();
    }

    /**
     * <p>
     * isRememberImageRotation.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isRememberImageRotation() {
        return DataManager.getInstance().getConfiguration().isRememberImageRotation();
    }

    /**
     * <p>
     * isDisplayStatistics.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplayStatistics() {
        return DataManager.getInstance().getConfiguration().isDisplayStatistics();
    }

    /**
     * <p>
     * isDisplaySearchRssLinks.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isDisplaySearchRssLinks() {
        return DataManager.getInstance().getConfiguration().isDisplaySearchRssLinks();
    }

    /**
     * <p>
     * isAdvancedSearchEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isAdvancedSearchEnabled() {
        return DataManager.getInstance().getConfiguration().isAdvancedSearchEnabled();
    }

    /**
     * <p>
     * isTimelineSearchEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isTimelineSearchEnabled() {
        return DataManager.getInstance().getConfiguration().isTimelineSearchEnabled();
    }

    /**
     * <p>
     * isCalendarSearchEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isCalendarSearchEnabled() {
        return DataManager.getInstance().getConfiguration().isCalendarSearchEnabled();
    }

    /**
     * <p>
     * isDisplayBreadcrumbs.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplayBreadcrumbs() {
        return DataManager.getInstance().getConfiguration().getDisplayBreadcrumbs();
    }

    /**
     * <p>
     * isDisplayMetadataPageLinkBlock.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplayMetadataPageLinkBlock() {
        return DataManager.getInstance().getConfiguration().getDisplayMetadataPageLinkBlock();
    }

    /**
     * <p>
     * isPagePdfEnabled.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isPagePdfEnabled() {
        return DataManager.getInstance().getConfiguration().isPagePdfEnabled();
    }

    /**
     * <p>
     * getRssTitle.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getRssTitle() {
        return DataManager.getInstance().getConfiguration().getRssTitle();
    }

    /**
     * <p>
     * isDisplayTagCloudStartpage.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplayTagCloudStartpage() {
        return DataManager.getInstance().getConfiguration().isDisplayTagCloudStartpage();
    }

    /**
     * <p>
     * isDisplaySearchResultNavigation.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplaySearchResultNavigation() {
        return DataManager.getInstance().getConfiguration().isDisplaySearchResultNavigation();
    }

    /**
     * <p>
     * isDisplayStructType.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplayStructType() {
        return DataManager.getInstance().getConfiguration().getDisplayStructType();
    }

    /**
     * <p>
     * isDisplayCollectionBrowsing.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplayCollectionBrowsing() {
        return DataManager.getInstance().getConfiguration().isDisplayCollectionBrowsing();
    }

    /**
     * <p>
     * isDisplayUserNavigation.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplayUserNavigation() {
        return DataManager.getInstance().getConfiguration().isDisplayUserNavigation();
    }

    /**
     * <p>
     * isDisplayTagCloudNavigation.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplayTagCloudNavigation() {
        return DataManager.getInstance().getConfiguration().isDisplayTagCloudNavigation();
    }

    /**
     * <p>
     * isDisplayTitlePURL.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplayTitlePURL() {
        return DataManager.getInstance().getConfiguration().isDisplayTitlePURL();
    }

    /**
     * <p>
     * isSidebarTocWidgetVisible.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isSidebarTocWidgetVisible() {
        return DataManager.getInstance().getConfiguration().isSidebarTocWidgetVisible();
    }

    /**
     * <p>
     * isSidebarTocWidgetVisibleInFullscreen.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isSidebarTocWidgetVisibleInFullscreen() {
        return DataManager.getInstance().getConfiguration().isSidebarTocWidgetVisibleInFullscreen();
    }

    /**
     * <p>
     * isSidebarOpacLinkVisible.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isSidebarOpacLinkVisible() {
        return DataManager.getInstance().getConfiguration().isSidebarOpacLinkVisible();
    }

    /**
     * <p>
     * isSidebarTocPageNumbersVisible.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isSidebarTocPageNumbersVisible() {
        return DataManager.getInstance().getConfiguration().getSidebarTocPageNumbersVisible();
    }

    /**
     * <p>
     * isSidebarPageLinkVisible.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSidebarPageLinkVisible() {
        return DataManager.getInstance().getConfiguration().isSidebarPageViewLinkVisible();
    }

    /**
     * <p>
     * isSidebarCalendarLinkVisible.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSidebarCalendarLinkVisible() {
        return DataManager.getInstance().getConfiguration().isSidebarCalendarViewLinkVisible();
    }

    /**
     * <p>
     * isSidebarMetadataLinkVisible.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSidebarMetadataLinkVisible() {
        return DataManager.getInstance().getConfiguration().isSidebarMetadataViewLinkVisible();
    }

    /**
     * <p>
     * isSidebarThumbsLinkVisible.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSidebarThumbsLinkVisible() {
        return DataManager.getInstance().getConfiguration().isSidebarThumbsViewLinkVisible();
    }

    /**
     * <p>
     * isSidebarFulltextLinkVisible.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSidebarFulltextLinkVisible() {
        return DataManager.getInstance().getConfiguration().isSidebarFulltextLinkVisible();
    }

    /**
     * <p>
     * isTocTreeView.
     * </p>
     *
     * @param docStructType a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean isTocTreeView(String docStructType) {
        return DataManager.getInstance().getConfiguration().isTocTreeView(docStructType);
    }

    /**
     * <p>
     * isSidebarTocTreeView.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isSidebarTocTreeView() {
        return DataManager.getInstance().getConfiguration().isSidebarTocTreeView();
    }

    /**
     * <p>
     * getSidebarTocLengthBeforeCut.
     * </p>
     *
     * @return a int.
     */
    public int getSidebarTocLengthBeforeCut() {
        return DataManager.getInstance().getConfiguration().getSidebarTocLengthBeforeCut();
    }

    /**
     * <p>
     * isDisplayTitleBreadcrumbs.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplayTitleBreadcrumbs() {
        return DataManager.getInstance().getConfiguration().getDisplayTitleBreadcrumbs();
    }

    /**
     * <p>
     * getTitleBreadcrumbsMaxTitleLength.
     * </p>
     *
     * @return a int.
     */
    public int getTitleBreadcrumbsMaxTitleLength() {
        return DataManager.getInstance().getConfiguration().getTitleBreadcrumbsMaxTitleLength();
    }

    /**
     * <p>
     * isDisplayTimeMatrix.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplayTimeMatrix() {
        return DataManager.getInstance().getConfiguration().isDisplayTimeMatrix();
    }

    /**
     * <p>
     * isDisplayCrowdsourcingModuleLinks.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplayCrowdsourcingModuleLinks() {
        return DataManager.getInstance().getConfiguration().isDisplayCrowdsourcingModuleLinks();
    }

    /**
     * <p>
     * getTimeMatrixStartYear.
     * </p>
     *
     * @return a int.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public int getTimeMatrixStartYear(String subTheme) throws PresentationException, IndexUnreachableException {
        String value = DataManager.getInstance().getConfiguration().getStartYearForTimeline();
        if ("MIN".equals(value)) {
            String subQuery = null;
            if (StringUtils.isNotBlank(subTheme)) {
                subQuery = String.format("+%s:%s", DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField(), subTheme);
            }
            return SearchHelper.getMinMaxYears(subQuery)[0];
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            logger.error("'{}' is not a valid value for 'startyear'", value);
            return 0;
        }
    }

    /**
     * <p>
     * getTimeMatrixEndYear.
     * </p>
     *
     * @return a int.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public int getTimeMatrixEndYear(String subTheme) throws PresentationException, IndexUnreachableException {
        String value = DataManager.getInstance().getConfiguration().getEndYearForTimeline();
        if ("MAX".equals(value)) {
            String subQuery = null;
            if (StringUtils.isNotBlank(subTheme)) {
                subQuery = String.format("+%s:%s", DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField(), subTheme);
            }
            return SearchHelper.getMinMaxYears(subQuery)[1];
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            logger.error("'{}' is not a valid value for 'startyear'", value);
            return 2500;
        }
    }

    /**
     * <p>
     * getTimeMatrixHits.
     * </p>
     *
     * @return Total hit number for the time matrix
     */
    public int getTimeMatrixHits() {
        String value = DataManager.getInstance().getConfiguration().getTimelineHits();
        if ("MAX".equals(value)) {
            return SolrSearchIndex.MAX_HITS;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            logger.error("'{}' is not a valid value for 'startyear'", value);
            return 108;
        }
    }

    @Deprecated
    public boolean isPiwikTracking() {
        return isPiwikTrackingEnabled();
    }

    /**
     * <p>
     * isPiwikTracking.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isPiwikTrackingEnabled() {
        return DataManager.getInstance().getConfiguration().isPiwikTrackingEnabled();
    }

    /**
     * <p>
     * getPiwikBaseURL.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getPiwikBaseURL() {
        return DataManager.getInstance().getConfiguration().getPiwikBaseURL();
    }

    /**
     * <p>
     * getPiwikSiteID.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getPiwikSiteID() {
        return DataManager.getInstance().getConfiguration().getPiwikSiteID();
    }

    /**
     * <p>
     * getAnchorThumbnailMode.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getAnchorThumbnailMode() {
        return DataManager.getInstance().getConfiguration().getAnchorThumbnailMode();
    }

    /**
     * <p>
     * getSortFields.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getSortFields() {
        return DataManager.getInstance()
                .getConfiguration()
                .getSortFields()
                .stream()
                .filter(field -> !isLanguageVersionOtherThan(field, BeanUtils.getLocale().getLanguage()))
                .collect(Collectors.toList());
    }

    /**
     * @param field
     * @param language
     * @return
     */
    private static boolean isLanguageVersionOtherThan(String field, String language) {
        return field.matches(".*_LANG_[A-Z][A-Z]") && !field.matches(".*_LANG_" + language.toUpperCase());
    }

    /**
     * <p>
     * getTocIndentation.
     * </p>
     *
     * @return a int.
     */
    public int getTocIndentation() {
        return DataManager.getInstance().getConfiguration().getTocIndentation();
    }

    /**
     * <p>
     * isPageBrowseEnabled.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isPageBrowseEnabled() {
        return DataManager.getInstance().getConfiguration().isPageBrowseEnabled();
    }

    /**
     * <p>
     * isPageBrowseStep1Visible.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isPageBrowseStep1Visible() {
        List<Integer> steps = DataManager.getInstance().getConfiguration().getPageBrowseSteps();
        if (steps != null && steps.size() > 0 && steps.get(0) > 0) {
            return true;
        }
        return false;
    }

    /**
     * <p>
     * isPageBrowseStep2Visible.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isPageBrowseStep2Visible() {
        List<Integer> steps = DataManager.getInstance().getConfiguration().getPageBrowseSteps();
        if (steps != null && steps.size() > 1 && steps.get(1) > 0) {
            return true;
        }
        return false;
    }

    /**
     * <p>
     * isPageBrowseStep3Visible.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isPageBrowseStep3Visible() {
        List<Integer> steps = DataManager.getInstance().getConfiguration().getPageBrowseSteps();
        if (steps != null && steps.size() > 2 && steps.get(2) > 1) {
            return true;
        }
        return false;
    }

    /**
     * <p>
     * getPageBrowseStep1.
     * </p>
     *
     * @return a int.
     */
    public int getPageBrowseStep1() {
        List<Integer> steps = DataManager.getInstance().getConfiguration().getPageBrowseSteps();
        if (steps != null && steps.size() > 0 && steps.get(0) > 1) {
            return steps.get(0);
        }
        return 0;
    }

    /**
     * <p>
     * getPageBrowseStep2.
     * </p>
     *
     * @return a int.
     */
    public int getPageBrowseStep2() {
        List<Integer> steps = DataManager.getInstance().getConfiguration().getPageBrowseSteps();
        if (steps != null && steps.size() > 1 && steps.get(1) > 1) {
            return steps.get(1);
        }
        return 0;
    }

    /**
     * <p>
     * getPageBrowseStep3.
     * </p>
     *
     * @return a int.
     */
    public int getPageBrowseStep3() {
        List<Integer> steps = DataManager.getInstance().getConfiguration().getPageBrowseSteps();
        if (steps != null && steps.size() > 2 && steps.get(2) > 1) {
            return steps.get(2);
        }
        return 0;
    }

    /**
     *
     * @return
     */
    public int getPageSelectDropdownDisplayMinPages() {
        return DataManager.getInstance().getConfiguration().getPageSelectDropdownDisplayMinPages();
    }

    /**
     * <p>
     * getReCaptchaSiteKey.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getReCaptchaSiteKey() {
        return DataManager.getInstance().getConfiguration().getReCaptchaSiteKey();
    }

    /**
     * <p>
     * isUseReCaptcha.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isUseReCaptcha() {
        return DataManager.getInstance().getConfiguration().isUseReCaptcha();
    }

    /**
     * <p>
     * isTocEpubEnabled.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isTocEpubEnabled() {
        return DataManager.getInstance().getConfiguration().isTocEpubEnabled() && isGeneratePdfInTaskManager();
    }

    /**
     * <p>
     * isGeneratePdfInTaskManager.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isGeneratePdfInTaskManager() {
        return DataManager.getInstance().getConfiguration().isGeneratePdfInTaskManager();
    }

    /**
     * <p>
     * isDocHierarchyPdfEnabled.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDocHierarchyPdfEnabled() {
        return DataManager.getInstance().getConfiguration().isDocHierarchyPdfEnabled();
    }

    /**
     * <p>
     * isShowSearchInItem. </p
     *
     * @return a boolean.
     */
    public boolean isShowSearchInItem() {
        return DataManager.getInstance().getConfiguration().isSearchInItemEnabled();
    }

    /**
     * <p>
     * isContentUploadEnabled.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isContentUploadEnabled() {
        return DataManager.getInstance().getConfiguration().isContentUploadEnabled();
    }

    /**
     * <p>
     * isTranskribusEnabled.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isTranskribusEnabled() {
        return DataManager.getInstance().getConfiguration().isTranskribusEnabled();
    }

    /**
     * <p>
     * isSearchExcelExportEnabled.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isSearchExcelExportEnabled() {
        return DataManager.getInstance().getConfiguration().isSearchExcelExportEnabled();
    }
    
    /**
     * <p>
     * isSearchRisExportEnabled.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isSearchRisExportEnabled() {
        return DataManager.getInstance().getConfiguration().isSearchRisExportEnabled();
    }

    /**
     * <p>
     * isDoublePageNavigationEnabled.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDoublePageNavigationEnabled() {
        return DataManager.getInstance().getConfiguration().isDoublePageNavigationEnabled();
    }

    /**
     * <p>
     * isSitelinksEnabled.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isSitelinksEnabled() {
        return DataManager.getInstance().getConfiguration().isSitelinksEnabled();
    }

    /**
     * <p>
     * getRestApiUrl.
     * </p>
     *
     * @return REST API URL
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getRestApiUrl() throws ViewerConfigurationException {
        return DataManager.getInstance().getConfiguration().getRestApiUrl();
    }

    /**
     *
     * @return
     */
    public String getRestApiUrlForIIIFPresention() {
        return DataManager.getInstance().getRestApiManager().getIIIFDataApiUrl();
    }

    /**
     * <p>
     * getIiifApiUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getIiifApiUrl() throws ViewerConfigurationException {
        return DataManager.getInstance().getConfiguration().getIIIFApiUrl();
    }

    /**
     * @return The url to the /api/v1 Rest Api
     */
    public String getRestApiUrlV1() {
        return DataManager.getInstance().getConfiguration().getRestApiUrl().replace("/rest", "/api/v1");
    }

    /**
     * <p>
     * getIso639_1.
     * </p>
     *
     * @param language a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getIso639_1(String language) {
        return DataManager.getInstance().getLanguageHelper().getLanguage(language).getIsoCodeOld();
    }

    /**
     * <p>
     * getIso639_2B.
     * </p>
     *
     * @param language a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getIso639_2B(String language) {
        return DataManager.getInstance().getLanguageHelper().getLanguage(language).getIsoCode();
    }

    /**
     * <p>
     * getTranslation.
     * </p>
     *
     * @param language a {@link java.lang.String} object.
     * @param locale a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getTranslation(String language, String locale) {
        Language lang = DataManager.getInstance().getLanguageHelper().getLanguage(language);
        switch (locale.toLowerCase()) {
            case "de":
            case "ger":
                return lang.getGermanName();
            case "fre":
            case "fra":
            case "fr":
                return lang.getFrenchName();
            default:
                return lang.getEnglishName();
        }
    }

    private static ImageType getImageType(String mimeType) {
        ImageType imageType = new ImageType(false);
        imageType.setFormat(ImageFileFormat.getImageFileFormatFromMimeType(mimeType));
        return imageType;
    }

    /**
     * <p>
     * isDisplaySidebarBrowsingTerms.
     * </p>
     *
     * @return a boolean.
     * @should return correct value
     */
    public boolean isDisplaySidebarBrowsingTerms() {
        return DataManager.getInstance().getConfiguration().isDisplaySidebarBrowsingTerms();
    }

    /**
     * <p>
     * isDisplaySidebarRssFeed.
     * </p>
     *
     * @return a boolean.
     * @should return correct value
     */
    public boolean isDisplaySidebarRssFeed() {
        return DataManager.getInstance().getConfiguration().isDisplaySidebarRssFeed();
    }

    /**
     * <p>
     * isDisplayWidgetUsage.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplaySidebarWidgetUsage() {
        return DataManager.getInstance().getConfiguration().isDisplayWidgetUsage();
    }

    /**
     * <p>
     * isDisplaySidebarWidgetUsageCitationLinks.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplaySidebarWidgetUsageCitationLinks() {
        return DataManager.getInstance().getConfiguration().isDisplaySidebarWidgetUsageCitationLinks();
    }

    /**
     * <p>
     * isDisplaySidebarWidgetUsageCitationRecommendation.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplaySidebarWidgetUsageCitationRecommendation() {
        return DataManager.getInstance().getConfiguration().isDisplaySidebarWidgetUsageCitationRecommendation();
    }

    /**
     *
     * @return List of configured <code>DownloadOption</code> items
     */
    public List<DownloadOption> getSidebarWidgetUsagePageDownloadOptions() {
        return DataManager.getInstance().getConfiguration().getSidebarWidgetUsagePageDownloadOptions();
    }

    public boolean isDisplaySidebarWidgetUsagePageDownloadOptions() {
        return DataManager.getInstance().getConfiguration().isDisplayWidgetUsageDownloadOptions();
    }

    /**
     *
     * @return List of available citation style names
     */
    public List<String> getSidebarWidgetUsageCitationRecommendationStyles() {
        return DataManager.getInstance().getConfiguration().getSidebarWidgetUsageCitationRecommendationStyles();
    }

    /**
     * @param levelName
     * @return List of configured citation links
     */
    public List<CitationLink> getSidebarWidgetUsageCitationLinksForLevel(String levelName) {
        if (StringUtils.isEmpty(levelName)) {
            return Collections.emptyList();
        }

        CitationLinkLevel level = CitationLinkLevel.getByName(levelName);
        if (level == null) {
            logger.warn("Unknown citation link level: {}", levelName);
            return Collections.emptyList();
        }

        List<CitationLink> ret = new ArrayList<>();
        for (CitationLink link : DataManager.getInstance().getConfiguration().getSidebarWidgetUsageCitationLinks()) {
            if (level.equals(link.getLevel())) {
                ret.add(link);
            }
        }

        return ret;
    }

    /**
     * <p>
     * isSubthemeDiscriminatorFieldSet.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isSubthemeDiscriminatorFieldSet() {
        return StringUtils.isNotEmpty(DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField());
    }

    /**
     * <p>
     * getTwitterName.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getTwitterName() {
        return DataManager.getInstance().getConfiguration().getTwitterUserName();
    }

    /**
     * @deprecated Superseded by isCopyrightIndicatorEnabled
     */
    @Deprecated(since = "22.07")
    public boolean isDisplayCopyrightInfo() {
        return false;
    }

    /**
     * 
     * @return
     */
    public boolean isCopyrightIndicatorEnabled() {
        return DataManager.getInstance().getConfiguration().isCopyrightIndicatorEnabled();
    }

    /**
     * 
     * @return
     */
    public String getCopyrightIndicatorStyle() {
        return DataManager.getInstance().getConfiguration().getCopyrightIndicatorStyle();
    }

    public boolean isDisplaySocialMediaShareLinks() {
        return DataManager.getInstance().getConfiguration().isDisplaySocialMediaShareLinks();
    }

    public String getMapBoxToken() {
        return DataManager.getInstance().getConfiguration().getMapBoxToken();
    }

    public String getMapBoxUser() {
        return DataManager.getInstance().getConfiguration().getMapBoxUser();
    }

    public String getMapBoxStyleId() {
        return DataManager.getInstance().getConfiguration().getMapBoxStyleId();
    }

    /**
     *
     * @return
     */
    public List<Integer> getSearchHitsPerPageValues() {
        return DataManager.getInstance().getConfiguration().getSearchHitsPerPageValues();
    }

    /**
     *
     * @return
     */
    public List<EmailRecipient> getFeedbackEmailRecipients() {
        return DataManager.getInstance().getConfiguration().getFeedbackEmailRecipients();
    }

    /**
     *
     * @return true if default sorting field is 'RANDOM'; false otherwise
     */
    public boolean isDefaultSortFieldRandom() {
        return SolrConstants.SORT_RANDOM.equals(DataManager.getInstance().getConfiguration().getDefaultSortField());
    }

    public boolean isDisplayUserGeneratedContentBelowImage() {
        return DataManager.getInstance().getConfiguration().isDisplayUserGeneratedContentBelowImage();
    }

    /**
     * @param template
     * @param fallbackToDefaultTemplate
     * @return true if docstruct navigation is enabled and properly configured; false otherwise
     */
    public boolean isDisplayDocstructNavigation(String template, boolean fallbackToDefaultTemplate) {
        return DataManager.getInstance().getConfiguration().isDocstructNavigationEnabled()
                && !DataManager.getInstance().getConfiguration().getDocstructNavigationTypes(template, fallbackToDefaultTemplate).isEmpty();
    }

    public boolean isDisplayAnnotationTextInImage() {
        return DataManager.getInstance().getConfiguration().isDisplayAnnotationTextInImage();
    }

    public boolean isDisplayAddressSearchInMap() {
        return DataManager.getInstance().getConfiguration().isDisplayAddressSearchInMap();
    }

    public boolean isArchivesEnabled() {
        return DataManager.getInstance().getConfiguration().isArchivesEnabled();
    }

    public String getSearchSortingAscendingKey(String field) {
        return DataManager.getInstance().getConfiguration().getSearchSortingKeyAscending(field).orElse("searchSortingDropdown_ascending");
    }

    public String getSearchSortingDescendingKey(String field) {
        return DataManager.getInstance().getConfiguration().getSearchSortingKeyDescending(field).orElse("searchSortingDropdown_descending");
    }

    /**
     *
     * @param facetField
     * @return
     */
    public boolean isTranslateFacetFieldLabels(String facetField) {
        return DataManager.getInstance().getConfiguration().isTranslateFacetFieldLabels(facetField);
    }

    public boolean useHeatmapForMapSearch() {
        return DataManager.getInstance().getConfiguration().useHeatmapForMapSearch();
    }

    public GeoMapMarker getMarkerForMapSearch() {
        return DataManager.getInstance().getConfiguration().getMarkerForMapSearch();
    }
    
    public String getSelectionColorForMapSearch() {
        return DataManager.getInstance().getConfiguration().getSelectionColorForMapSearch();
    }

    public boolean useHeatmapForFacetting() {
        return DataManager.getInstance().getConfiguration().useHeatmapForFacetting();
    }

    public GeoMapMarker getMarkerForFacetting() {
        return DataManager.getInstance().getConfiguration().getMarkerForFacetting();
    }
    
    public String getSelectionColorForFacetting() {
        return DataManager.getInstance().getConfiguration().getSelectionColorForFacetting();
    }

    public boolean useHeatmapForCMSMaps() {
        return DataManager.getInstance().getConfiguration().useHeatmapForCMSMaps();
    }

    public GeoMapMarker getDefaultMarkerForCMSMaps() {
        List<GeoMapMarker> markers = DataManager.getInstance().getConfiguration().getGeoMapMarkers();
        GeoMapMarker marker = markers.stream()
                .filter(m -> m.getName().equalsIgnoreCase("default"))
                .findAny()
                .orElse(new GeoMapMarker("default"));
        return marker;
    }

    public int getGeomapAnnotationZoom() {
        return DataManager.getInstance().getConfiguration().getGeomapAnnotationZoom();
    }

    public String getCampaignGeomapInitialViewAsJson() {
        int zoom = DataManager.getInstance().getConfiguration().getCrowdsourcingCampaignGeomapZoom();
        String lngLatString = DataManager.getInstance().getConfiguration().getCrowdsourcingCampaignGeomapLngLat();

        JSONArray lngLatArray = new JSONArray("[" + lngLatString + "]");
        JSONObject view = new JSONObject();
        view.put("zoom", zoom);
        view.put("center", lngLatArray);

        return view.toString();
    }

    public String getCampaignGeomapTilesource() {
        return DataManager.getInstance().getConfiguration().getCrowdsourcingCampaignGeomapTilesource();
    }

    /**
     * 
     * @return
     */
    public boolean isConfigEditorEnabled() {
        return DataManager.getInstance().getConfiguration().isConfigEditorEnabled();
    }

}
