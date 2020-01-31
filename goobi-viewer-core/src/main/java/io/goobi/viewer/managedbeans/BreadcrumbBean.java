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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ocpsoft.pretty.PrettyContext;
import com.ocpsoft.pretty.faces.url.URL;

import de.intranda.metadata.multilanguage.IMetadataValue;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordDeletedException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.cms.CMSPage;
import io.goobi.viewer.model.search.SearchFacets;
import io.goobi.viewer.model.viewer.CompoundLabeledLink;
import io.goobi.viewer.model.viewer.LabeledLink;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.ViewManager;

@Named
@SessionScoped
public class BreadcrumbBean implements Serializable {

    private static final long serialVersionUID = -7671680493703878185L;

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(BreadcrumbBean.class);

    /** Constant <code>WEIGHT_TAG_MAIN_MENU=1</code> */
    public static final int WEIGHT_TAG_MAIN_MENU = 1;
    /** Constant <code>WEIGHT_ACTIVE_COLLECTION=2</code> */
    public static final int WEIGHT_ACTIVE_COLLECTION = 2;
    /** Constant <code>WEIGHT_OPEN_DOCUMENT=3</code> */
    public static final int WEIGHT_OPEN_DOCUMENT = 3;
    /** Constant <code>WEIGHT_BROWSE=1</code> */
    public static final int WEIGHT_BROWSE = 1;
    /** Constant <code>WEIGHT_SEARCH=1</code> */
    public static final int WEIGHT_SEARCH = 1;
    /** Constant <code>WEIGHT_SEARCH_RESULTS=2</code> */
    public static final int WEIGHT_SEARCH_RESULTS = 2;
    /** Constant <code>WEIGHT_SEARCH_TERMS=1</code> */
    public static final int WEIGHT_SEARCH_TERMS = 1;
    /** Constant <code>WEIGHT_TAG_CLOUD=1</code> */
    public static final int WEIGHT_TAG_CLOUD = 1;
    /** Constant <code>WEIGHT_SITELINKS=1</code> */
    public static final int WEIGHT_SITELINKS = 1;
    /** Constant <code>WEIGHT_USER_ACCOUNT=1</code> */
    public static final int WEIGHT_USER_ACCOUNT = 1;
    /** Constant <code>WEIGHT_CROWDSOURCING_OVERVIEW=3</code> */
    public static final int WEIGHT_CROWDSOURCING_OVERVIEW = 3;
    /** Constant <code>WEIGHT_CROWDSOURCING_EDIT_OVERVIEW=4</code> */
    public static final int WEIGHT_CROWDSOURCING_EDIT_OVERVIEW = 4;
    /** Constant <code>WEIGHT_CROWDSOURCING_EDIT_OCR_CONTENTS=5</code> */
    public static final int WEIGHT_CROWDSOURCING_EDIT_OCR_CONTENTS = 5;
    /** Constant <code>WEIGHT_CROWDSOURCING_CAMPAIGN=2</code> */
    public static final int WEIGHT_CROWDSOURCING_CAMPAIGN = 2;
    /** Constant <code>WEIGHT_CROWDSOURCING_CAMPAIGN_ITEM=3</code> */
    public static final int WEIGHT_CROWDSOURCING_CAMPAIGN_ITEM = 3;
    /** Constant <code>WEIGHT_CROWDSOURCING_CAMPAIGN_PARENT=1</code> */
    public static final int WEIGHT_CROWDSOURCING_CAMPAIGN_PARENT = 1;

    private List<LabeledLink> breadcrumbs = new LinkedList<>();

    /**
     * <p>
     * init.
     * </p>
     *
     * @should sort lazyModelComments by dateUpdated desc by default
     */
    @PostConstruct
    public void init() {

    }

    /**
     * Attaches a new link to the breadcrumb list at the appropriate position (depending on the link's weight).
     *
     * @param newLink The breadcrumb link to add.
     * @should always remove breadcrumbs coming after the proposed breadcrumb
     */
    public void updateBreadcrumbs(LabeledLink newLink) {
        logger.trace("updateBreadcrumbs (LabeledLink): {}", newLink.toString());
        List<LabeledLink> breadcrumbs = Collections.synchronizedList(this.breadcrumbs);
        synchronized (breadcrumbs) {

            // Always add the home page if there are no breadcrumbs
            if (breadcrumbs.isEmpty()) {
                resetBreadcrumbs();
            }
            logger.trace("Adding breadcrumb: {} ({})", newLink.getUrl(), newLink.getWeight());
            // Determine the position at which to add the new link
            int position = breadcrumbs.size();
            for (int i = 0; i < breadcrumbs.size(); ++i) {
                LabeledLink link = breadcrumbs.get(i);
                if (link.getWeight() >= newLink.getWeight()) {
                    position = i;
                    break;
                }
            }
            try {
                // To avoid duplicate breadcrumbs while flipping pages, the LabeledLink.equals() method will prevent multiple breadcrumbs with the same name
                if (breadcrumbs.contains(newLink)) {
                    logger.trace("Breadcrumb '{}' is already in the list.", newLink);
                    return;
                }
                breadcrumbs.add(position, newLink);
            } finally {
                // Remove any following links, even if the proposed link is a duplicate
                if (position < breadcrumbs.size()) {
                    try {
                        breadcrumbs.subList(position + 1, breadcrumbs.size()).clear();
                    } catch (NullPointerException e) {
                        // This throws a NPE sometimes
                    }
                }
                // logger.trace("breadcrumbs: " + breadcrumbs.size() + " " +
                // breadcrumbs.toString());
            }
        }

    }

    /**
     * Updates breadcrumbs from the given CMS page (and any breadcrumb predecessor pages).
     * 
     * @param cmsPage The CMS page from which to create a breadcrumb
     * @throws RecordNotFoundException
     * @throws RecordDeletedException
     * @throws DAOException
     * @throws IndexUnreachableException
     * @throws ViewerConfigurationException
     */
    public void updateBreadcrumbs(CMSPage cmsPage)
            throws RecordNotFoundException, RecordDeletedException, DAOException, IndexUnreachableException, ViewerConfigurationException {
        logger.trace("updateBreadcrumbs (CMSPage): {}", cmsPage.getTitle());

        List<LabeledLink> tempBreadcrumbs = new ArrayList<>();
        int weight = 1;
        try {
            // If the CMS page is part of a record, add a breadcrumb after said record and abort
            if (StringUtils.isNotBlank(cmsPage.getRelatedPI())) {
                // TODO Find a way without having a cyclic dependency
                ActiveDocumentBean adb = BeanUtils.getActiveDocumentBean();
                if (adb != null) {
                    try {
                        adb.setPersistentIdentifier(cmsPage.getRelatedPI());
                        adb.open();
                    } catch (PresentationException e) {
                        logger.debug("PresentationException thrown here: {}", e.getMessage(), e);
                        Messages.error(e.getMessage());
                    }
                }
                weight = this.breadcrumbs.get(this.breadcrumbs.size() - 1).getWeight() + 1;
                tempBreadcrumbs.add(new LabeledLink(StringUtils.isNotBlank(cmsPage.getMenuTitle()) ? cmsPage.getMenuTitle() : cmsPage.getTitle(),
                        cmsPage.getPageUrl(), weight));
                return;
            }
            resetBreadcrumbs();
            Set<CMSPage> linkedPages = new HashSet<>();
            CMSPage currentPage = cmsPage;

            // If the current cms page contains a collection and we are in a subcollection of it, attempt to add a breadcrumb link for the subcollection
            try {
                if (cmsPage.getCollection() != null && cmsPage.getCollection().isSubcollection()) {
                    LabeledLink link = new LabeledLink(cmsPage.getCollection().getTopVisibleElement(),
                            cmsPage.getCollection().getCollectionUrl(cmsPage.getCollection().getTopVisibleElement()), WEIGHT_SEARCH_RESULTS);
                    tempBreadcrumbs.add(0, link);
                    // logger.trace("added cms page collection breadcrumb: {}", link.toString());
                }
            } catch (PresentationException | IndexUnreachableException e) {
                logger.error(e.toString(), e);
            }

            while (currentPage != null) {
                if (linkedPages.contains(currentPage)) {
                    //encountered a breadcrumb loop. Simply break here
                    break;
                }
                linkedPages.add(currentPage);
                if (DataManager.getInstance()
                        .getDao()
                        .getStaticPageForCMSPage(currentPage)
                        .stream()
                        .findFirst()
                        .map(sp -> sp.getPageName())
                        .filter(name -> PageType.index.name().equals(name))
                        .isPresent()) {
                    logger.trace("CMS index page found");
                    // The current page is the start page, which is already the breadcrumb root
                    break;
                }
                LabeledLink pageLink =
                        new LabeledLink(StringUtils.isNotBlank(currentPage.getMenuTitle()) ? currentPage.getMenuTitle() : currentPage.getTitle(),
                                currentPage.getPageUrl(), weight);
                tempBreadcrumbs.add(0, pageLink);
                // logger.trace("added cms page breadcrumb: (page id {}) - {}", currentPage.getId(), pageLink.toString());
                if (StringUtils.isNotBlank(currentPage.getParentPageId())) {
                    try {
                        Long cmsPageId = Long.parseLong(currentPage.getParentPageId());
                        currentPage = DataManager.getInstance().getDao().getCMSPage(cmsPageId);
                    } catch (NumberFormatException | DAOException e) {
                        logger.error("CMS breadcrumb creation: Parent page of page {} is not a valid page id", currentPage.getId());
                        currentPage = null;
                    }
                } else {
                    currentPage = null;
                }

            }
        } finally {
            List<LabeledLink> breadcrumbs = Collections.synchronizedList(this.breadcrumbs);
            synchronized (breadcrumbs) {
                for (LabeledLink bc : tempBreadcrumbs) {
                    bc.setWeight(weight++);
                    breadcrumbs.add(bc);
                }
                // tempBreadcrumbs.forEach(bc -> breadcrumbs.add(bc));
            }
        }
    }

    /**
     * This is used for flipping search result pages (so that the breadcrumb always has the last visited result page as its URL).
     *
     * @param facetString a {@link java.lang.String} object.
     */
    public void updateBreadcrumbsForSearchHits(String facetString) {
        //        if (!facets.getCurrentHierarchicalFacets().isEmpty()) {
        //            updateBreadcrumbsWithCurrentUrl(facets.getCurrentHierarchicalFacets().get(0).getValue().replace("*", ""),
        //                    NavigationHelper.WEIGHT_ACTIVE_COLLECTION);
        //        } else {
        facetString = StringTools.decodeUrl(facetString);
        List<String> facets =
                SearchFacets.getHierarchicalFacets(facetString, DataManager.getInstance().getConfiguration().getHierarchicalDrillDownFields());
        if (facets.size() > 0) {
            String facet = facets.get(0);
            facets = SearchFacets.splitHierarchicalFacet(facet);
            updateBreadcrumbsWithCurrentCollection(DataManager.getInstance().getConfiguration().getHierarchicalDrillDownFields().get(0), facets,
                    WEIGHT_SEARCH_RESULTS);
        } else {
            updateBreadcrumbsWithCurrentUrl("searchHitNavigation", WEIGHT_SEARCH_RESULTS);
        }
        //        }
    }

    /**
     * Adds a new collection breadcrumb hierarchy for the current Pretty URL.
     *
     * @param field Facet field for building the URL
     * @param subItems Facet values
     * @param weight The weight of the link
     */
    private void updateBreadcrumbsWithCurrentCollection(String field, List<String> subItems, int weight) {
        logger.trace("updateBreadcrumbsWithCurrentCollection: {} ({})", field, weight);
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        updateBreadcrumbs(new LabeledLink("browseCollection", getBrowseUrl() + '/', WEIGHT_BROWSE));
        updateBreadcrumbs(new CompoundLabeledLink("browseCollection", "", field, subItems, weight));
    }

    /**
     * Adds a new breadcrumb for the current Pretty URL.
     *
     * @param name Breadcrumb name.
     * @param weight The weight of the link.
     */
    void updateBreadcrumbsWithCurrentUrl(String name, int weight) {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        URL url = PrettyContext.getCurrentInstance(request).getRequestURL();
        updateBreadcrumbs(new LabeledLink(name, BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + url.toURL(), weight));
    }

    /**
     * Empties the breadcrumb list and adds a link to the start page.
     */
    void resetBreadcrumbs() {
        // logger.trace("reset breadcrumbs");
        List<LabeledLink> breadcrumbs = Collections.synchronizedList(this.breadcrumbs);
        synchronized (breadcrumbs) {
            breadcrumbs.clear();
            breadcrumbs.add(new LabeledLink("home", BeanUtils.getServletPathWithHostAsUrlFromJsfContext(), 0));
        }
    }

    /**
     * Adds a link to the breadcrumbs using the current PrettyURL. Can be called from XHTML.
     *
     * @param linkName a {@link java.lang.String} object.
     * @param linkWeight a int.
     */
    public void addStaticLinkToBreadcrumb(String linkName, int linkWeight) {
        NavigationHelper nh = BeanUtils.getNavigationHelper(); // TODO
        addStaticLinkToBreadcrumb(linkName, nh.getCurrentPrettyUrl(), linkWeight);
    }

    /**
     * Adds a link to the breadcrumbs using the given URL. Can be called from XHTML.
     *
     * @param linkName a {@link java.lang.String} object.
     * @param linkWeight a int.
     * @param url a {@link java.lang.String} object.
     */
    public void addStaticLinkToBreadcrumb(String linkName, String url, int linkWeight) {
        logger.trace("addStaticLinkToBreadcrumb: {} - {} ({})", linkName, url, linkWeight);
        if (linkWeight < 0) {
            return;
        }
        PageType page = PageType.getByName(url);
        if (page != null && !page.equals(PageType.other)) {
            url = getUrl(page);
        } else {
        }
        LabeledLink newLink = new LabeledLink(linkName, url, linkWeight);
        updateBreadcrumbs(newLink);
    }

    /**
     * <p>
     * addCollectionHierarchyToBreadcrumb.
     * </p>
     *
     * @param collection Full collection string containing all levels
     * @param field Solr field
     * @param splittingChar a {@link java.lang.String} object.
     * @should create breadcrumbs correctly
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void addCollectionHierarchyToBreadcrumb(final String collection, final String field, final String splittingChar)
            throws PresentationException, DAOException {
        logger.trace("addCollectionHierarchyToBreadcrumb: {}", collection);
        if (field == null) {
            throw new IllegalArgumentException("field may not be null");
        }
        if (splittingChar == null) {
            throw new IllegalArgumentException("splittingChar may not be null");
        }
        if (StringUtils.isEmpty(collection)) {
            return;
        }

        updateBreadcrumbs(new LabeledLink("browseCollection",
                BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.browse.getName() + '/', WEIGHT_BROWSE));
        List<String> hierarchy = StringTools.getHierarchyForCollection(collection, splittingChar);
        // Individual hierarchy elements will all be added with the active collection weight
        updateBreadcrumbs(new CompoundLabeledLink("browseCollection", "", field, hierarchy, WEIGHT_ACTIVE_COLLECTION));
    }

    /**
     * 
     * @param viewManager
     * @param name
     * @param url
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws PresentationException
     */
    public void addRecordBreadcrumbs(ViewManager viewManager, IMetadataValue name, URL url)
            throws IndexUnreachableException, PresentationException, DAOException {
        // Add collection hierarchy to breadcrumbs, if the record only belongs to one collection
        String collectionHierarchyField = DataManager.getInstance().getConfiguration().getCollectionHierarchyField();
        if (collectionHierarchyField != null) {
            List<String> collections = viewManager.getTopDocument().getMetadataValues(collectionHierarchyField);
            if (collections.size() == 1) {
                addCollectionHierarchyToBreadcrumb(collections.get(0), collectionHierarchyField,
                        DataManager.getInstance().getConfiguration().getCollectionSplittingChar(collectionHierarchyField));
            }
        }
        int weight = WEIGHT_OPEN_DOCUMENT;
        IMetadataValue anchorName = null;

        if (viewManager.getTopDocument().isVolume() && viewManager.getAnchorPi() != null) {
            logger.trace("anchor breadcrumb");
            // Anchor breadcrumb
            StructElement anchorDocument = viewManager.getTopDocument().getParent();
            anchorName = anchorDocument.getMultiLanguageDisplayLabel();
            for (String language : anchorName.getLanguages()) {
                String translation = anchorName.getValue(language).orElse("");
                if (translation != null && translation.length() > DataManager.getInstance().getConfiguration().getBreadcrumbsClipping()) {
                    translation = new StringBuilder(translation.substring(0, DataManager.getInstance().getConfiguration().getBreadcrumbsClipping()))
                            .append("...")
                            .toString();
                    anchorName.setValue(translation, language);
                }
            }
            PageType pageType = PageType.determinePageType(anchorDocument.getDocStructType(), null, true, false, false);
            String anchorUrl = '/' + DataManager.getInstance().getUrlBuilder().buildPageUrl(anchorDocument.getPi(), 1, null, pageType);
            updateBreadcrumbs(new LabeledLink(anchorName, BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + anchorUrl, weight++));
        }
        // If volume name is the same as anchor name, add the volume number, otherwise the volume breadcrumb will be rejected as a duplicate
        if (anchorName != null && anchorName.getValue().equals(name.getValue())) {
            StringBuilder sb = new StringBuilder(name.getValue().get());
            sb.append(" (");
            if (viewManager.getTopDocument().getMetadataValue(SolrConstants.CURRENTNO) != null) {
                sb.append(viewManager.getTopDocument().getMetadataValue(SolrConstants.CURRENTNO));
            } else if (viewManager.getTopDocument().getMetadataValue(SolrConstants.CURRENTNOSORT) != null) {
                sb.append(viewManager.getTopDocument().getMetadataValue(SolrConstants.CURRENTNOSORT));
            }
            sb.append(')');
            name.setValue(sb.toString());
        }
        // Volume/monograph breadcrumb
        updateBreadcrumbs(new LabeledLink(name, BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + url.toURL(), weight));
    }

    /**
     * Returns the list of current breadcrumb elements. Note that only the sub-links are used for elements of class <code>CompoundLabeledLink</code>,
     * not the main link.
     *
     * @return the List of flattened breadcrumb links
     */
    public List<LabeledLink> getBreadcrumbs() {
        List<LabeledLink> baseLinks = Collections.synchronizedList(this.breadcrumbs);
        List<LabeledLink> flattenedLinks = new ArrayList<>();
        for (LabeledLink labeledLink : baseLinks) {
            if (labeledLink instanceof CompoundLabeledLink) {
                flattenedLinks.addAll(((CompoundLabeledLink) labeledLink).getSubLinks());
            } else {
                flattenedLinks.add(labeledLink);
            }
        }
        logger.trace("getBreadcrumbs: {}", flattenedLinks.toString());
        return flattenedLinks;
    }

    /**
     * Returns the bottom breadcrumb. Used to return to the previous page from the errorGeneral page.
     *
     * @return a {@link io.goobi.viewer.model.viewer.LabeledLink} object.
     */
    public LabeledLink getLastBreadcrumb() {
        List<LabeledLink> breadcrumbs = Collections.synchronizedList(this.breadcrumbs);
        synchronized (breadcrumbs) {
            if (!breadcrumbs.isEmpty()) {
                return breadcrumbs.get(breadcrumbs.size() - 1);
            }

            return null;
        }
    }

    /**
     * <p>
     * getBrowseUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    private static String getBrowseUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.browse.getName();
    }

    /**
     * @param page
     * @return
     */
    private String getUrl(PageType page) {
        return getApplicationUrl() + page.getName();
    }

    /**
     * <p>
     * getApplicationUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    private String getApplicationUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/";
    }
}