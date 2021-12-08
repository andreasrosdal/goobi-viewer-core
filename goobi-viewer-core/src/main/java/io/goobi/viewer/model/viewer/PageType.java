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
package io.goobi.viewer.model.viewer;

import java.net.URI;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.model.urlresolution.ViewerPathBuilder;

/**
 * <p>
 * PageType class.
 * </p>
 */
public enum PageType {

    viewImage("image"),
    viewToc("toc"),
    viewThumbs("thumbs"),
    viewMetadata("metadata"),
    viewFulltext("fulltext"),
    viewFullscreen("fullscreen"),
    viewObject("object"),
    viewCalendar("calendar"),
    searchlist("searchlist", "search"),
    searchCalendar("searchcalendar", "searchCalendar"),
    searchGeoMap("searchgeomap", "title__search_geomap"),
    term("term", "searchTermList"),
    expandCollection("expandCollection"),
    firstWorkInCollection("rest/redirect/toFirstWork"),
    sitelinks("sitelinks"),
    archives("archives"),
    archive("archive"),
    timematrix("timematrix"),
    //user
    user("user"),
    userSearches("user/searches", "label__user_searches"),
    //admin
    admin("admin"),
    adminUsers("admin/users"),
    adminUser("admin/users"),
    adminUserNew("admin/users/new"),
    adminUserGroups("admin/groups"),
    adminUserGroup("admin/groups"),
    adminUserGroupNew("admin/groups/new"),
    adminIpRanges("admin/ipranges"),
    adminIpRange("admin/ipranges"),
    adminIpRangeNew("admin/ipranges/new"),
    adminLicenseTypes("admin/licenses"),
    adminLicenseType("admin/license"),
    adminRights("admin/rights"),
    adminRightsNew("admin/rights/new"),
    adminUserComments("admin/comments"),
    adminUserTerms("admin/userterms"),
    adminCreateRecord("admin/record/new"),
    adminTranslations("admin/translations"),
    //admin/cms
    adminCms("admin/cms"),
    adminCmsOverview("admin/cms/pages"),
    adminCmsSelectTemplate("admin/cms/pages/templates"),
    adminCmsNewPage("admin/cms/pages/new"),
    adminCmsCategories("admin/cms/categories"),
    adminCmsNewCategory("admin/cms/categories/new"),
    adminCmsStaticPages("admin/cms/pages/mapping"),
    adminCmsMedia("admin/cms/media"),
    adminCmsMenuItems("admin/cms/menus"),
    adminCmsCollections("admin/cms/collections"),
    adminCmsEditCollection("admin/cms/collections/edit"),
    adminCmsGeoMaps("admin/cms/maps"),
    adminCmsGeoMapEdit("admin/cms/maps/edit"),
    adminCmsGeoMapNew("admin/cms/maps/new"),
    adminCmsRecordNotes("admin/cms/recordnotes", "cms__record_notes__title_plural"),
    adminCmsSliders("admin/cms/slider", "cms__sliders__title"),
    cmsPageOfWork("page"),
    cmsPage("cms"),
    //admin/crowdsourcing
    adminCrowdsourcingAnnotations("admin/crowdsourcing/annotations"),
    adminCrowdsourcingCampaigns("admin/crowdsourcing/campaigns"),
    adminUserActivity("admin/user/activity/"),
    annotations("annotations"),
    // TODO remove
    editContent("crowd/editContent"),
    editOcr("crowd/editOcr"),
    editHistory("crowd/editHistory"),

    // The order of page types handled by CMS here determines the listing order of static pages
    index("index", PageTypeHandling.cms),
    search("search", PageTypeHandling.cms),
    advancedSearch("searchadvanced", PageTypeHandling.cms),
    browse("browse", PageTypeHandling.cms),
    privacy("privacy", PageTypeHandling.cms),
    imprint("imprint", PageTypeHandling.cms),
    feedback("feedback", PageTypeHandling.cms),
    crowsourcingCampaigns("campaigns", PageTypeHandling.cms),
    bookmarks("bookmarks", PageTypeHandling.cms),
    crowsourcingAnnotation("campaigns/.../annotate"),
    crowsourcingReview("campaigns/.../review"),

    other(""); //unknown page type name in Navigationhelper. Probably a cms-page

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(PageType.class);

    public final String path;
    private final String label;
    private final PageTypeHandling handling;

    private PageType(String name) {
        this.path = name;
        this.label = name;
        this.handling = PageTypeHandling.none;
    }

    private PageType(String name, PageTypeHandling handling) {
        this.path = name;
        this.label = name;
        this.handling = handling;
    }

    private PageType(String path, String label) {
        this.path = path;
        this.label = label;
        this.handling = PageTypeHandling.none;
    }

    private PageType(String path, String label, PageTypeHandling handling) {
        this.path = path;
        this.label = label;
        this.handling = handling;
    }

    /**
     * <p>
     * Getter for the field <code>handling</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.viewer.PageType.PageTypeHandling} object.
     */
    public PageTypeHandling getHandling() {
        return this.handling;
    }

    /**
     * <p>
     * isHandledWithCms.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isHandledWithCms() {
        return this.handling.equals(PageTypeHandling.cms);
    }

    /**
     * <p>
     * isCmsPage.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isCmsPage() {
        try {            
            switch (this) {
                case editContent:
                case editHistory:
                case editOcr:
                case viewCalendar:
                case viewFullscreen:
                case viewFulltext:
                case viewImage:
                case viewMetadata:
                case viewThumbs:
                case viewToc:
                    return true;
                default:
                    return false;
            }
        } catch(NoClassDefFoundError e) {
            //Gets thrown under some conditions for some reason. For now just ignore
            return false;
        }
    }

    /**
     * <p>
     * isDocumentPage.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDocumentPage() {
        switch (this) {
            case editContent:
            case editHistory:
            case editOcr:
            case viewCalendar:
            case viewFullscreen:
            case viewFulltext:
            case viewImage:
            case viewMetadata:
            case viewThumbs:
            case viewToc:
            case viewObject:
                return true;
            default:
                return false;
        }
    }

    /**
     * <p>
     * getTypesHandledByCms.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public static List<PageType> getTypesHandledByCms() {
        Set<PageType> all = EnumSet.allOf(PageType.class);
        List<PageType> cmsPages = new ArrayList<>();
        for (PageType pageType : all) {
            if (pageType.isHandledWithCms()) {
                cmsPages.add(pageType);
            }
        }
        return cmsPages;
    }

    /**
     * <p>
     * getByName.
     * </p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.viewer.PageType} object.
     * @should return correct type for raw names
     * @should return correct type for mapped names
     * @should return correct type for enum names
     * @should return correct type if name starts with metadata
     */
    public static PageType getByName(String name) {
        if (name == null) {
            return null;
        }
        for (PageType p : PageType.values()) {
            if (p.getName().equalsIgnoreCase(name) || p.path.equalsIgnoreCase(name) || p.label.equalsIgnoreCase(name)
                    || p.name().equalsIgnoreCase(name)) {
                return p;
            }
        }
        // Set type viewMetadata is page name starts with "metadata"
        if (name.startsWith(PageType.viewMetadata.getName())) {
            return PageType.viewMetadata;
        }
        // look for configured names
        for (PageType p : PageType.values()) {
            String configName = DataManager.getInstance().getConfiguration().getPageType(p);
            if (configName != null && configName.equalsIgnoreCase(name)) {
                return p;
            }
        }
        return PageType.other;
    }

    /**
     * <p>
     * getRawName.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getRawName() {
        return path;
    }

    /**
     * <p>
     * Getter for the field <code>name</code>.
     * </p>
     *
     * @return Mapped alternative name, if available; raw name otherwise
     */
    public String getName() {
        String configName = DataManager.getInstance().getConfiguration().getPageType(this);
        if (configName != null) {
            return configName;
        }

        return path;
    }

    public String getLabel() {
        return label;
    }

    public static enum PageTypeHandling {
        none,
        cms;
    }

    /**
     * <p>
     * getPageTypeForDocStructType.
     * </p>
     *
     * @param docStructType a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.viewer.PageType} object.
     */
    public static PageType getPageTypeForDocStructType(String docStructType) {
        // First choice: Use preferred target page type for this docstruct type, if configured
        String preferredPageTypeName = DataManager.getInstance().getConfiguration().getDocstructTargetPageType(docStructType);
        PageType preferredPageType = PageType.getByName(preferredPageTypeName);
        if (StringUtils.isNotEmpty(preferredPageTypeName) && preferredPageType == null) {
            logger.error("docstructTargetPageType configured for '{}' does not exist: {}", docStructType, preferredPageTypeName);
        }
        // Second choice: Use target page type configured as _DEFAULT, if available
        String defaultPageTypeName = DataManager.getInstance().getConfiguration().getDocstructTargetPageType("_DEFAULT");
        PageType defaultPageType = PageType.getByName(defaultPageTypeName);
        if (StringUtils.isNotEmpty(defaultPageTypeName) && defaultPageType == null) {
            logger.error("docstructTargetPageType configured for '_DEFAULT' does not exist: {}", docStructType, defaultPageTypeName);
        }

        if (preferredPageType != null) {
            // logger.trace("Found preferred page type: {}", preferredPageType.getName());
            return preferredPageType;
        } else if (defaultPageType != null) {
            // logger.trace("Found default page type: {}", defaultPageType.getName());
            return defaultPageType;
        }

        return null;
    }

    /**
     * <p>
     * determinePageType.
     * </p>
     *
     * @param docStructType a {@link java.lang.String} object.
     * @param mimeType a {@link java.lang.String} object.
     * @param anchorOrGroup a boolean.
     * @param hasImages a boolean.
     * @param pageResolverUrl If this page type is for a page resolver url, ignore certain preferences
     * @should return configured page type correctly
     * @should return metadata page type for application mime type
     * @should return toc page type for anchors
     * @should return image page type correctly
     * @should return medatata page type if nothing else matches
     * @return a {@link io.goobi.viewer.model.viewer.PageType} object.
     */
    public static PageType determinePageType(String docStructType, String mimeType, Boolean anchorOrGroup, Boolean hasImages,
            boolean pageResolverUrl) {
        // Determine preferred target for the docstruct
        //         logger.trace("determinePageType: docstrct: {} / mime type: {} / anchor: {} / images: {} / resolver: {}", docStructType, mimeType,
        //                anchorOrGroup, hasImages, pageResolverUrl);
        PageType configuredPageType = PageType.getPageTypeForDocStructType(docStructType);
        if (configuredPageType != null && !pageResolverUrl) {
            return configuredPageType;
        }
        if ("application".equals(mimeType)) {
            return PageType.viewMetadata;
        }
        if (Boolean.TRUE.equals(anchorOrGroup)) {
            return PageType.viewToc;
        }
        if (Boolean.TRUE.equals(hasImages)) {
            return PageType.viewObject;
        }

        return PageType.viewMetadata;
    }

    /**
     * <p>
     * matches.
     * </p>
     *
     * @param pagePath a {@link java.lang.String} object.
     * @return true if the given path equals either the intrinsic or configured name of this pageType Leading and trailing slashes are ignored.
     *         PageType other is never matched
     */
    public boolean matches(String pagePath) {
        if (StringUtils.isBlank(pagePath)) {
            return false;
        }
        pagePath = pagePath.replaceAll("^\\/|\\/$", "");
        return pagePath.equalsIgnoreCase(this.name()) || pagePath.equalsIgnoreCase(this.path) || pagePath.equalsIgnoreCase(getName());
    }

    /**
     * <p>
     * matches.
     * </p>
     *
     * @param pagePath a {@link java.net.URI} object.
     * @return true if the given path starts with either the intrinsic or configured name of this pageType Leading and trailing slashes are ignored.
     *         PageType other is never matched
     */
    public boolean matches(URI pagePath) {
        if (pagePath == null || StringUtils.isBlank(pagePath.toString())) {
            return false;
        }
        return ViewerPathBuilder.startsWith(pagePath, this.name()) || ViewerPathBuilder.startsWith(pagePath, this.path)
                || ViewerPathBuilder.startsWith(pagePath, getName());
    }

    /**
     * <p>
     * isRestricted.
     * </p>
     *
     * @return a boolean.
     */
    @Deprecated
    public boolean isRestricted() {
        switch (this) {
            case admin:
            case adminLicenseTypes:
            case adminUserGroups:
            case adminUsers:
            case adminCms:
            case adminCmsCategories:
            case adminCmsNewCategory:
            case adminCmsCollections:
            case adminCmsNewPage:
            case adminCmsEditCollection:
            case adminCmsGeoMaps:
            case adminCmsGeoMapEdit:
            case adminCmsGeoMapNew:
            case adminCmsMedia:
            case adminCmsMenuItems:
            case adminCmsOverview:
            case adminCmsSelectTemplate:
            case adminCmsStaticPages:
            case adminCreateRecord:
            case adminCrowdsourcingAnnotations:
            case adminCrowdsourcingCampaigns:
            case adminIpRange:
            case adminIpRanges:
            case adminIpRangeNew:
            case adminLicenseType:
            case adminRights:
            case adminRightsNew:
            case adminUser:
            case adminUserNew:
            case adminUserActivity:
            case adminUserComments:
            case adminUserGroup:
            case adminUserGroupNew:
            case editContent:
            case editHistory:
            case editOcr:
            case bookmarks:
            case user:
                return true;
            default:
                return false;
        }
    }
}
