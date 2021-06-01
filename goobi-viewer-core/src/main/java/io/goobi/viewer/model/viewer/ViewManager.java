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

import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_ALTO_ZIP;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_ALTO;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_PLAINTEXT;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_TEI;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PLAINTEXT_ZIP;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_RECORD;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_TEI_LANG;

import java.awt.Dimension;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.jboss.weld.exceptions.IllegalArgumentException;
import org.jdom2.JDOMException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.undercouch.citeproc.CSL;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.AlphanumCollatorComparator;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.HTTPException;
import io.goobi.viewer.exceptions.IDDOCNotFoundException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.ImageDeliveryBean;
import io.goobi.viewer.managedbeans.NavigationHelper;
import io.goobi.viewer.managedbeans.SearchBean;
import io.goobi.viewer.managedbeans.UserBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.calendar.CalendarView;
import io.goobi.viewer.model.citation.Citation;
import io.goobi.viewer.model.citation.CitationProcessorWrapper;
import io.goobi.viewer.model.citation.CitationTools;
import io.goobi.viewer.model.download.DownloadOption;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.metadata.MetadataTools;
import io.goobi.viewer.model.metadata.MetadataValue;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.toc.TOC;
import io.goobi.viewer.model.transkribus.TranskribusJob;
import io.goobi.viewer.model.transkribus.TranskribusSession;
import io.goobi.viewer.model.transkribus.TranskribusUtils;
import io.goobi.viewer.model.viewer.pageloader.AbstractPageLoader;
import io.goobi.viewer.model.viewer.pageloader.IPageLoader;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;

/**
 * Holds information about the currently open record (structure, pages, etc.). Used to reduced the size of ActiveDocumentBean.
 */
public class ViewManager implements Serializable {

    private static final long serialVersionUID = -7776362205876306849L;

    private static final Logger logger = LoggerFactory.getLogger(ViewManager.class);

    private ImageDeliveryBean imageDeliveryBean;

    /** IDDOC of the top level document. */
    private final long topStructElementIddoc;
    /** IDDOC of the current level document. The initial top level document values eventually gets overridden with the image owner element's IDDOC. */
    private long currentStructElementIddoc;
    /** LOGID of the current level document. */
    private String logId;

    /** Document of the anchor element, if applicable. */
    private StructElement anchorStructElement;

    /** Top level document. */
    private StructElement topStructElement;

    /** Currently selected document. */
    private StructElement currentStructElement;

    private IPageLoader pageLoader;
    private PhysicalElement representativePage;

    /** Table of contents object. */
    private TOC toc;

    private int rotate = 0;
    private int zoomSlider;
    private int currentImageOrder = -1;
    private final List<SelectItem> dropdownPages = new ArrayList<>();
    private final List<SelectItem> dropdownFulltext = new ArrayList<>();
    private String dropdownSelected = "";
    private int currentThumbnailPage = 1;
    private String pi;
    private Boolean accessPermissionPdf = null;
    private Boolean allowUserComments = null;
    private String persistentUrl = null;
    private List<StructElementStub> docHierarchy = null;
    private String mainMimeType = null;
    private Boolean filesOnly = null;
    private String opacUrl = null;
    private String contextObject = null;
    private List<String> versionHistory = null;
    private PageOrientation firstPageOrientation = PageOrientation.right;
    private boolean doublePageMode = false;
    private int firstPdfPage;
    private int lastPdfPage;
    private CalendarView calendarView;
    private Long pagesWithFulltext = null;
    private Long pagesWithAlto = null;
    private Boolean workHasTEIFiles = null;
    private Boolean metadataViewOnly = null;
    private List<String> downloadFilenames = null;
    private String citationStyle = null;
    private CitationProcessorWrapper citationProcessorWrapper;

    /**
     * <p>
     * Constructor for ViewManager.
     * </p>
     *
     * @param topDocument a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @param pageLoader a {@link io.goobi.viewer.model.viewer.pageloader.IPageLoader} object.
     * @param currentDocumentIddoc a long.
     * @param logId a {@link java.lang.String} object.
     * @param mainMimeType a {@link java.lang.String} object.
     * @param imageDeliveryBean a {@link io.goobi.viewer.managedbeans.ImageDeliveryBean} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws ViewerConfigurationException
     * @throws DAOException
     */
    public ViewManager(StructElement topDocument, IPageLoader pageLoader, long currentDocumentIddoc, String logId, String mainMimeType,
            ImageDeliveryBean imageDeliveryBean) throws IndexUnreachableException, PresentationException, DAOException, ViewerConfigurationException {
        this.imageDeliveryBean = imageDeliveryBean;
        this.topStructElement = topDocument;
        this.topStructElementIddoc = topDocument.getLuceneId();
        logger.trace("New ViewManager: {} / {} / {}", topDocument.getLuceneId(), currentDocumentIddoc, logId);
        this.pageLoader = pageLoader;
        this.currentStructElementIddoc = currentDocumentIddoc;
        this.logId = logId;
        if (topStructElementIddoc == currentDocumentIddoc) {
            currentStructElement = topDocument;
        } else {
            currentStructElement = new StructElement(currentDocumentIddoc);
        }
        // Set the anchor StructElement for extracting metadata later
        if (topDocument.isAnchorChild()) {
            anchorStructElement = topDocument.getParent();
        }

        currentThumbnailPage = 1;
        //        annotationManager = new AnnotationManager(topDocument);
        pi = topDocument.getPi();

        if (!topDocument.isAnchor()) {
            // Generate drop-down page selector elements
            dropdownPages.clear();
            dropdownFulltext.clear();
            if (pageLoader != null) {
                pageLoader.generateSelectItems(dropdownPages, dropdownFulltext, BeanUtils.getServletPathWithHostAsUrlFromJsfContext(),
                        isBelowFulltextThreshold(), BeanUtils.getLocale());
            }
        }
        this.mainMimeType = mainMimeType;
        logger.trace("mainMimeType: {}", mainMimeType);
    }

    /**
     * <p>
     * createCalendarView.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.calendar.CalendarView} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public CalendarView createCalendarView() throws IndexUnreachableException, PresentationException {
        // Init calendar view
        String anchorPi = anchorStructElement != null ? anchorStructElement.getPi() : (topStructElement.isAnchor() ? pi : null);
        return new CalendarView(pi, anchorPi, topStructElement.isAnchor() ? null : topStructElement.getMetadataValue(SolrConstants._CALENDAR_YEAR));

    }

    /**
     * <p>
     * getRepresentativeImageInfo.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getRepresentativeImageInfo() throws IndexUnreachableException, DAOException, PresentationException, ViewerConfigurationException {
        PhysicalElement representative = getRepresentativePage();
        if (representative == null) {
            return "";
        }
        return imageDeliveryBean.getImages().getImageUrl(null, pi, representative.getFileName());
        //        StringBuilder urlBuilder = new StringBuilder(DataManager.getInstance().getConfiguration().getIIIFApiUrl());
        //        urlBuilder.append("image/").append(pi).append('/').append(representative.getFileName()).append("/info.json");
        //        return urlBuilder.toString();
    }

    /**
     * <p>
     * getCurrentImageInfo.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getCurrentImageInfo() throws IndexUnreachableException, DAOException {
        if (getCurrentPage() != null && getCurrentPage().getMimeType().startsWith("image")) {
            return getCurrentImageInfo(BeanUtils.getNavigationHelper().getCurrentPageType());
        }

        return "{}";
    }

    /**
     * <p>
     * getCurrentImageInfo.
     * </p>
     *
     * @param pageType a {@link io.goobi.viewer.model.viewer.PageType} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getCurrentImageInfo(PageType pageType) throws IndexUnreachableException, DAOException {
        StringBuilder urlBuilder = new StringBuilder();
        if (isDoublePageMode() && !getCurrentPage().isDoubleImage()) {
            Optional<PhysicalElement> leftPage = getCurrentLeftPage();
            Optional<PhysicalElement> rightPage = getCurrentRightPage();
            logger.trace("left page: {}", leftPage.isPresent() ? leftPage.get().getOrder() : "-");
            logger.trace("right page: {}", rightPage.isPresent() ? rightPage.get().getOrder() : "-");
            urlBuilder.append("[");
            String imageInfoLeft =
                    (leftPage.isPresent() && leftPage.get().isDoubleImage()) ? null : leftPage.map(page -> getImageInfo(page, pageType)).orElse(null);
            String imageInfoRight = (rightPage.isPresent() && rightPage.get().isDoubleImage()) ? null
                    : rightPage.map(page -> getImageInfo(page, pageType)).orElse(null);
            if (StringUtils.isNotBlank(imageInfoLeft)) {
                urlBuilder.append("\"").append(imageInfoLeft).append("\"");
            }
            if (StringUtils.isNotBlank(imageInfoLeft) && StringUtils.isNotBlank(imageInfoRight)) {
                urlBuilder.append(", ");
            }
            if (StringUtils.isNotBlank(imageInfoRight)) {
                urlBuilder.append("\"").append(imageInfoRight).append("\"");
            }
            urlBuilder.append("]");
        } else {
            urlBuilder.append(getImageInfo(getCurrentPage(), pageType));
        }
        return urlBuilder.toString();
    }

    /**
     * @param currentPage
     * @return
     * @throws DAOException
     * @throws IndexUnreachableException
     */
    public Optional<PhysicalElement> getCurrentLeftPage() throws IndexUnreachableException, DAOException {
        boolean actualPageOrderEven = this.currentImageOrder % 2 == 0;
        PageOrientation actualPageOrientation = actualPageOrderEven ? getFirstPageOrientation().opposite() : getFirstPageOrientation();
        if (topStructElement != null && topStructElement.isRtl()) {
            actualPageOrientation = actualPageOrientation.opposite();
        }
        if (actualPageOrientation.equals(PageOrientation.left)) {
            return getPage(this.currentImageOrder);
        } else if (topStructElement != null && topStructElement.isRtl()) {
            return getPage(this.currentImageOrder + 1);
        } else {
            return getPage(this.currentImageOrder - 1);
        }

    }

    /**
     * @param currentPage
     * @return
     * @throws DAOException
     * @throws IndexUnreachableException
     */
    public Optional<PhysicalElement> getCurrentRightPage() throws IndexUnreachableException, DAOException {
        boolean actualPageOrderEven = this.currentImageOrder % 2 == 0;
        PageOrientation actualPageOrientation = actualPageOrderEven ? getFirstPageOrientation().opposite() : getFirstPageOrientation();
        if (topStructElement != null && topStructElement.isRtl()) {
            actualPageOrientation = actualPageOrientation.opposite();
        }
        if (actualPageOrientation.equals(PageOrientation.right)) {
            return getPage(this.currentImageOrder);
        } else if (topStructElement != null && topStructElement.isRtl()) {
            return getPage(this.currentImageOrder - 1);
        } else {
            return getPage(this.currentImageOrder + 1);
        }

    }

    /**
     * 
     * @param page
     * @param pageType
     * @return
     */
    private String getImageInfo(PhysicalElement page, PageType pageType) {
        return imageDeliveryBean.getImages().getImageUrl(page, pageType);
    }

    /**
     * <p>
     * getCurrentImageInfoFullscreen.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getCurrentImageInfoFullscreen() throws IndexUnreachableException, DAOException {
        PhysicalElement currentPage = getCurrentPage();
        if (currentPage == null) {
            return "";
        }
        String url = getImageInfo(currentPage, PageType.viewFullscreen);
        return url;
    }

    /**
     * <p>
     * getCurrentImageInfoCrowd.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getCurrentImageInfoCrowd() throws IndexUnreachableException, DAOException {
        PhysicalElement currentPage = getCurrentPage();
        if (currentPage == null) {
            return "";
        }
        String url = getImageInfo(currentPage, PageType.editOcr);
        return url;
    }

    /**
     * <p>
     * getWatermarkUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getWatermarkUrl() throws IndexUnreachableException, DAOException, ViewerConfigurationException {
        return getWatermarkUrl("viewImage");
    }

    /**
     * <p>
     * getWatermarkUrl.
     * </p>
     *
     * @param pageType a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getWatermarkUrl(String pageType) throws IndexUnreachableException, DAOException, ViewerConfigurationException {
        return imageDeliveryBean.getFooter()
                .getWatermarkUrl(Optional.ofNullable(getCurrentPage()), Optional.ofNullable(getTopStructElement()),
                        Optional.ofNullable(PageType.getByName(pageType)))
                .orElse("");

    }

    /**
     * <p>
     * getCurrentImageUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getCurrentImageUrl() throws ViewerConfigurationException, IndexUnreachableException, DAOException {
        return getCurrentImageUrl(PageType.viewObject);
    }

    /**
     * <p>
     * getCurrentObjectUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getCurrentObjectUrl() throws IndexUnreachableException, DAOException {
        return imageDeliveryBean.getObjects3D().getObjectUrl(pi, getCurrentPage().getFilename());
    }

    /**
     * <p>
     * getCurrentImageUrl.
     * </p>
     *
     * @return the iiif url to the image in a configured size
     * @param view a {@link io.goobi.viewer.model.viewer.PageType} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getCurrentImageUrl(PageType view) throws IndexUnreachableException, DAOException, ViewerConfigurationException {

        int size = DataManager.getInstance()
                .getConfiguration()
                .getImageViewZoomScales(view, Optional.ofNullable(getCurrentPage()).map(page -> page.getImageType()).orElse(null))
                .stream()
                .map(string -> "max".equalsIgnoreCase(string) ? 0 : Integer.parseInt(string))
                .sorted((s1, s2) -> s1 == 0 ? -1 : (s2 == 0 ? 1 : Integer.compare(s2, s1)))
                .findFirst()
                .orElse(800);
        return getCurrentImageUrl(view, size);
    }

    /**
     * <p>
     * getCurrentImageUrl.
     * </p>
     *
     * @param size a int.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getCurrentImageUrl(int size) throws IndexUnreachableException, DAOException {
        return getCurrentImageUrl(PageType.viewImage, size);
    }

    /**
     * <p>
     * getCurrentMasterImageUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getCurrentMasterImageUrl() throws IndexUnreachableException, DAOException {
        return getCurrentMasterImageUrl(Scale.MAX);
    }

    /**
     * <p>
     * getCurrentMasterImageUrl.
     * </p>
     *
     * @param scale a {@link de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getCurrentMasterImageUrl(Scale scale) throws IndexUnreachableException, DAOException {

        PageType pageType = Optional.ofNullable(BeanUtils.getNavigationHelper()).map(nh -> nh.getCurrentPageType()).orElse(null);
        if (pageType == null) {
            pageType = PageType.viewObject;
        }
        StringBuilder sb = new StringBuilder(imageDeliveryBean.getThumbs().getFullImageUrl(getCurrentPage(), scale));
        try {
            if (DataManager.getInstance().getConfiguration().getFooterHeight(pageType, getCurrentPage().getImageType()) > 0) {
                sb.append("?ignoreWatermark=false");
                sb.append(imageDeliveryBean.getFooter().getWatermarkTextIfExists(getCurrentPage()).map(text -> "&watermarkText=" + text).orElse(""));
                sb.append(imageDeliveryBean.getFooter().getFooterIdIfExists(getTopStructElement()).map(id -> "&watermarkId=" + id).orElse(""));
            }
        } catch (ViewerConfigurationException e) {
            logger.error("Unable to read watermark config, ignore watermark", e);
        }
        return sb.toString();
    }

    /**
     * @param view
     * @param size
     * @return
     * @throws DAOException
     * @throws IndexUnreachableException
     */
    private String getCurrentImageUrl(PageType view, int size) throws IndexUnreachableException, DAOException {
        StringBuilder sb = new StringBuilder(imageDeliveryBean.getThumbs().getThumbnailUrl(getCurrentPage(), size, size));
        try {
            if (DataManager.getInstance().getConfiguration().getFooterHeight(view, getCurrentPage().getImageType()) > 0) {
                sb.append("?ignoreWatermark=false");
                sb.append(imageDeliveryBean.getFooter().getWatermarkTextIfExists(getCurrentPage()).map(text -> "&watermarkText=" + text).orElse(""));
                sb.append(imageDeliveryBean.getFooter().getFooterIdIfExists(getTopStructElement()).map(id -> "&watermarkId=" + id).orElse(""));
            }
        } catch (ViewerConfigurationException e) {
            logger.error("Unable to read watermark config, ignore watermark", e);
        }
        return sb.toString();
    }

    /**
     * <p>
     * getPageDownloadUrl.
     * </p>
     *
     * @param option
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getPageDownloadUrl(DownloadOption option) throws IndexUnreachableException, DAOException {
        logger.trace("getPageDownloadUrl: {}", option);
        if (option == null || !option.isValid()) {
            option = getDownloadOptionsForCurrentImage().stream()
                    .findFirst()
                    .orElse(null);
            if (option == null) {
                return "";
            }
        }
        Scale scale;
        if (DownloadOption.MAX == option.getBoxSizeInPixel()) {
            scale = Scale.MAX;
        } else if (option.getBoxSizeInPixel() == DownloadOption.NONE) {
            throw new IllegalArgumentException("Invalid box size: " + option.getBoxSizeInPixel());
        } else {
            scale = new Scale.ScaleToBox(option.getBoxSizeInPixel());
        }
        switch (option.getFormat().toLowerCase()) {
            case "jpg":
            case "jpeg":
                return imageDeliveryBean.getThumbs().getThumbnailUrl(getCurrentPage(), scale);
            default:
                return getCurrentMasterImageUrl(scale);
        }

    }

    public static List<DownloadOption> getDownloadOptionsForImage(
            List<DownloadOption> configuredOptions,
            Dimension origImageSize,
            Dimension configuredMaxSize,
            String imageFilename) {

        List<DownloadOption> options = new ArrayList<>();

        int maxWidth;
        int maxHeight;
        Dimension maxSize;
        if (origImageSize != null && origImageSize.height * origImageSize.width > 0) {
            maxWidth = Math.min(origImageSize.width, configuredMaxSize.width);
            maxHeight = Math.min(origImageSize.height, configuredMaxSize.height);
            maxSize = new Dimension(maxWidth, maxHeight);
        } else {
            maxWidth = configuredMaxSize.width;
            maxHeight = configuredMaxSize.height;
            maxSize = configuredMaxSize;
        }

        for (DownloadOption option : configuredOptions) {
            try {
                Dimension dim = option.getBoxSizeInPixel();
                if (dim == DownloadOption.MAX) {
                    Scale scale = new Scale.ScaleToBox(maxSize);
                    Dimension size = scale.scale(origImageSize);
                    options.add(new DownloadOption(option.getLabel(), getImageFormat(option.getFormat(), imageFilename), size));
                } else if (dim.width * dim.height == 0) {
                    continue;
                } else if ((maxWidth > 0 && maxWidth < dim.width) || (maxHeight > 0 && maxHeight < dim.height)) {
                    continue;
                } else {
                    Scale scale = new Scale.ScaleToBox(option.getBoxSizeInPixel());
                    Dimension size = scale.scale(origImageSize);
                    options.add(new DownloadOption(option.getLabel(), getImageFormat(option.getFormat(), imageFilename), size));
                }
            } catch (IllegalRequestException e) {
                //attempting scale beyond original size. Ignore
            }
        }
        return options;
    }

    public List<DownloadOption> getDownloadOptionsForCurrentImage() throws IndexUnreachableException, DAOException {
        PhysicalElement page = getCurrentPage();
        if (page != null && page.isHasImage()) {
            List<DownloadOption> configuredOptions = DataManager.getInstance().getConfiguration().getSidebarWidgetUsagePageDownloadOptions();
            String imageFilename = page.getFilename();
            Dimension maxSize = new Dimension(
                    page.isAccessPermissionImageZoom() ? DataManager.getInstance().getConfiguration().getViewerMaxImageWidth()
                            : DataManager.getInstance().getConfiguration().getUnzoomedImageAccessMaxWidth(),
                    DataManager.getInstance().getConfiguration().getViewerMaxImageHeight());
            Dimension imageSize = new Dimension(page.getImageWidth(), page.getImageHeight());
            return getDownloadOptionsForImage(configuredOptions, imageSize, maxSize, imageFilename);
        }

        return Collections.emptyList();
    }

    /**
     * return the current image format if argument is 'MASTER', or the argument itself otherwise
     * 
     * @param format
     * @return
     */
    public static String getImageFormat(String format, String imageFilename) {
        if (format != null && format.equalsIgnoreCase("master")) {
            return Optional.ofNullable(imageFilename)
                    .map(ImageFileFormat::getImageFileFormatFromFileExtension)
                    .map(ImageFileFormat::name)
                    .orElse(format);
        }

        return format;
    }

    /**
     * <p>
     * getMasterImageUrlForDownload.
     * </p>
     *
     * @param boxSizeInPixel
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getMasterImageUrlForDownload(String boxSizeInPixel) throws IndexUnreachableException, DAOException {
        if (boxSizeInPixel == null) {
            throw new IllegalArgumentException("boxSizeInPixel may not be null");
        }

        Scale scale;
        if (boxSizeInPixel.equalsIgnoreCase(Scale.MAX_SIZE) || boxSizeInPixel.equalsIgnoreCase(Scale.FULL_SIZE)) {
            scale = Scale.MAX;
        } else if (boxSizeInPixel.matches("\\d{1,9}")) {
            scale = new Scale.ScaleToBox(Integer.valueOf(boxSizeInPixel), Integer.valueOf(boxSizeInPixel));
        } else {
            throw new IllegalArgumentException("Not a valid size parameter: " + boxSizeInPixel);
        }

        return getCurrentMasterImageUrl(scale);
    }

    /**
     * <p>
     * getCurrentSearchResultCoords.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public List<List<String>> getCurrentSearchResultCoords() throws IndexUnreachableException, DAOException, ViewerConfigurationException {
        List<List<String>> coords = new ArrayList<>();
        List<String> coordStrings = getSearchResultCoords(getCurrentPage());
        if (coordStrings != null) {
            for (String string : coordStrings) {
                coords.add(Arrays.asList(string.split(",")));
            }
        }
        return coords;
    }

    private List<String> getSearchResultCoords(PhysicalElement currentImg) throws ViewerConfigurationException {
        if (currentImg == null) {
            return null;
        }
        List<String> coords = null;
        SearchBean searchBean = BeanUtils.getSearchBean();
        if (searchBean != null && (searchBean.getCurrentSearchFilterString() == null
                || searchBean.getCurrentSearchFilterString().equals(SearchHelper.SEARCH_FILTER_ALL.getLabel())
                || searchBean.getCurrentSearchFilterString().equals("filter_" + SolrConstants.FULLTEXT))) {
            logger.trace("Adding word coords to page {}: {}", currentImg.getOrder(), searchBean.getSearchTerms().toString());
            coords = currentImg.getWordCoords(searchBean.getSearchTerms().get(SolrConstants.FULLTEXT), rotate);
        }
        return coords;
    }

    /**
     * <p>
     * getRepresentativeWidth.
     * </p>
     *
     * @return a int.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public int getRepresentativeWidth() throws PresentationException, IndexUnreachableException, DAOException {
        if (getRepresentativePage() != null) {
            return getRepresentativePage().getImageWidth();
        }
        return 0;
    }

    /**
     * <p>
     * getRepresentativeHeight.
     * </p>
     *
     * @return a int.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public int getRepresentativeHeight() throws PresentationException, IndexUnreachableException, DAOException {
        if (getRepresentativePage() != null) {
            return getRepresentativePage().getImageHeight();
        }
        return 0;
    }

    /**
     * <p>
     * getCurrentWidth.
     * </p>
     *
     * @return a int.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public int getCurrentWidth() throws IndexUnreachableException, DAOException {
        PhysicalElement currentPage = getCurrentPage();
        if (currentPage != null) {
            if (rotate % 180 == 90) {
                return currentPage.getImageHeight();
            }
            return currentPage.getImageWidth();
        }
        return 0;
    }

    /**
     * <p>
     * getCurrentHeight.
     * </p>
     *
     * @return a int.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public int getCurrentHeight() throws IndexUnreachableException, DAOException {
        PhysicalElement currentPage = getCurrentPage();
        if (currentPage != null) {
            if (rotate % 180 == 90) {
                return currentPage.getImageWidth();
            }
            return currentPage.getImageHeight();
        }
        return 0;
    }

    /**
     * <p>
     * getRepresentativeImageUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getRepresentativeImageUrl() throws IndexUnreachableException, PresentationException, DAOException {
        return getRepresentativeImageUrl(representativePage.getImageWidth(), representativePage.getImageHeight());
    }

    /**
     * 
     * @param width
     * @param height
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     */
    public String getRepresentativeImageUrl(int width, int height) throws IndexUnreachableException, PresentationException, DAOException {
        if (getRepresentativePage() == null) {
            return null;
        }

        //      Dimension imageSize = new Dimension(representativePage.getImageWidth(), representativePage.getImageHeight());
        return imageDeliveryBean.getThumbs().getThumbnailUrl(representativePage, width, height);
    }

    /**
     * <p>
     * scaleToWidth.
     * </p>
     *
     * @param imageSize a {@link java.awt.Dimension} object.
     * @param scaledWidth a int.
     * @return a {@link java.awt.Dimension} object.
     */
    public static Dimension scaleToWidth(Dimension imageSize, int scaledWidth) {
        double scale = scaledWidth / imageSize.getWidth();
        int scaledHeight = (int) (imageSize.getHeight() * scale);
        return new Dimension(scaledWidth, scaledHeight);
    }

    /**
     * <p>
     * scaleToHeight.
     * </p>
     *
     * @param imageSize a {@link java.awt.Dimension} object.
     * @param scaledHeight a int.
     * @return a {@link java.awt.Dimension} object.
     */
    public static Dimension scaleToHeight(Dimension imageSize, int scaledHeight) {
        double scale = scaledHeight / imageSize.getHeight();
        int scaledWidth = (int) (imageSize.getWidth() * scale);
        return new Dimension(scaledWidth, scaledHeight);
    }

    /**
     * Retrieves the current User from the session, if exists.
     *
     * @return The current User; null of not logged in.
     */
    public User getCurrentUser() {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        if (request != null) {
            UserBean ub = BeanUtils.getUserBean();
            if (ub != null && ub.getUser() != null) {
                return ub.getUser();
            }
        }
        return null;
    }

    /**
     * <p>
     * rotateLeft.
     * </p>
     *
     * @should rotate correctly
     * @return a {@link java.lang.String} object.
     */
    public String rotateLeft() {
        rotate -= 90;
        if (rotate < 0) {
            rotate = 360 + rotate;
        }
        if (rotate == -360) {
            rotate = 0;
        }
        logger.trace("rotateLeft: {}", rotate);

        return null;
    }

    /**
     * <p>
     * rotateRight.
     * </p>
     *
     * @should rotate correctly
     * @return a {@link java.lang.String} object.
     */
    public String rotateRight() {
        rotate += 90;
        if (rotate == 360) {
            rotate = 0;
        }
        logger.trace("rotateRight: {}", rotate);

        return null;
    }

    /**
     * <p>
     * resetImage.
     * </p>
     *
     * @should reset rotation
     * @return a {@link java.lang.String} object.
     */
    public String resetImage() {
        this.rotate = 0;
        logger.trace("resetImage: {}", rotate);

        return null;
    }

    /**
     * <p>
     * isHasUrns.
     * </p>
     *
     * @return true if this record contains URN or IMAGEURN fields; false otherwise
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public boolean isHasUrns() throws PresentationException, IndexUnreachableException {
        return topStructElement.getMetadataFields().containsKey(SolrConstants.URN)
                || topStructElement.getFirstPageFieldValue(SolrConstants.IMAGEURN) != null;
    }

    /**
     * <p>
     * isHasVolumes.
     * </p>
     *
     * @return true if this is an anchor record and has indexed volumes; false otherwise
     */
    public boolean isHasVolumes() {
        if (!topStructElement.isAnchor()) {
            return false;
        }

        return topStructElement.getNumVolumes() > 0;
    }

    /**
     * <p>
     * isHasPages.
     * </p>
     *
     * @return true if record contains pages; false otherwise
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public boolean isHasPages() throws IndexUnreachableException {
        return pageLoader != null && pageLoader.getNumPages() > 0;
    }

    /**
     * <p>
     * isFilesOnly.
     * </p>
     *
     * @return true if record or first child or first page have an application mime type; false otherwise
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isFilesOnly() throws IndexUnreachableException, DAOException {
        // TODO check all files for mime type?
        if (filesOnly == null) {
            if (MimeType.APPLICATION.getName().equals(mainMimeType)) {
                filesOnly = true;
            } else {
                boolean childIsFilesOnly = isChildFilesOnly();
                PhysicalElement firstPage = pageLoader.getPage(pageLoader.getFirstPageOrder());
                filesOnly = childIsFilesOnly || (isHasPages() && firstPage != null && firstPage.getMimeType().equals(MimeType.APPLICATION.getName()));
            }

        }

        return filesOnly;
    }

    /**
     * Convenience method for identifying born digital material records.
     *
     * @return true if record is born digital material (no scanned images); false otherwise
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isBornDigital() throws IndexUnreachableException, DAOException {
        return isHasPages() && isFilesOnly();
    }

    /**
     * 
     * @return
     * @throws IndexUnreachableException
     */
    private boolean isChildFilesOnly() throws IndexUnreachableException {
        boolean childIsFilesOnly = false;
        if (currentStructElement != null && (currentStructElement.isAnchor() || currentStructElement.isGroup())) {
            try {
                String mimeType = currentStructElement.getFirstVolumeFieldValue(SolrConstants.MIMETYPE);
                if (MimeType.APPLICATION.getName().equals(mimeType)) {
                    childIsFilesOnly = true;
                }
            } catch (PresentationException e) {
                logger.warn(e.toString());
            }
        }
        return childIsFilesOnly;
    }

    /**
     * Defines the criteria whether to list all remaining volumes in the TOC if the current record is a volume.
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isListAllVolumesInTOC() throws IndexUnreachableException, DAOException {
        return DataManager.getInstance().getConfiguration().isTocListSiblingRecords() || isFilesOnly();
    }

    /**
     * Returns all pages in their correct order. Used for e-publications.
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<PhysicalElement> getAllPages() throws IndexUnreachableException, DAOException {
        List<PhysicalElement> ret = new ArrayList<>();
        if (pageLoader != null) {
            for (int i = pageLoader.getFirstPageOrder(); i <= pageLoader.getLastPageOrder(); ++i) {
                PhysicalElement page = pageLoader.getPage(i);
                if (page != null) {
                    ret.add(page);
                }
            }
        }

        return ret;
    }

    /**
     * <p>
     * getCurrentPage.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public PhysicalElement getCurrentPage() {
        return getPage(currentImageOrder).orElse(null);
    }

    /**
     * @param step
     * @return
     * @throws IndexUnreachableException
     */
    public PhysicalElement getNextPrevPage(int step) throws IndexUnreachableException {
        int index = currentImageOrder + step;
        if (index <= 0 || index >= pageLoader.getNumPages()) {
            return null;
        }
        return getPage(index).orElse(null);
    }

    /**
     * Returns the page with the given order number from the page loader, if exists.
     *
     * @param order a int.
     * @return requested page if exists; null otherwise.
     * @should return correct page
     * @should return null if order less than zero
     * @should return null if order larger than number of pages
     * @should return null if pageLoader is null
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public Optional<PhysicalElement> getPage(int order) {
        try {
            if (pageLoader != null && pageLoader.getPage(order) != null) {
                // logger.debug("page " + order + ": " + pageLoader.getPage(order).getFileName());
                return Optional.ofNullable(pageLoader.getPage(order));
            }
        } catch (IndexUnreachableException e) {
            logger.error("Error getting current page " + e.toString());
        }

        return Optional.empty();
    }

    /**
     * <p>
     * Getter for the field <code>representativePage</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public PhysicalElement getRepresentativePage() throws PresentationException, IndexUnreachableException, DAOException {
        if (representativePage == null) {
            String thumbnailName = topStructElement.getMetadataValue(SolrConstants.THUMBNAIL);
            if (pageLoader != null) {
                if (thumbnailName != null) {
                    representativePage = pageLoader.getPageForFileName(thumbnailName);
                }
                if (representativePage == null) {
                    representativePage = pageLoader.getPage(pageLoader.getFirstPageOrder());
                }
            }
        }

        return representativePage;
    }

    /**
     * <p>
     * getFirstPage.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public PhysicalElement getFirstPage() throws IndexUnreachableException, DAOException {
        return pageLoader.getPage(pageLoader.getFirstPageOrder());
    }

    /**
     * Getter for the paginator or the direct page number input field
     *
     * @return currentImageNo
     */
    public int getCurrentImageOrderForPaginator() {
        return getCurrentImageOrder();
    }

    /**
     * Setter for the direct page number input field
     *
     * @param currentImageOrder a int.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws IDDOCNotFoundException
     */
    public void setCurrentImageOrderForPaginator(int currentImageOrder)
            throws IndexUnreachableException, PresentationException, IDDOCNotFoundException {
        logger.trace("setCurrentImmageNoForPaginator({})", currentImageOrder);
        setCurrentImageOrder(currentImageOrder);
    }

    /**
     * <p>
     * currentImageOrder.
     * </p>
     *
     * @return the currentImageOrder
     */
    public int getCurrentImageOrder() {
        return currentImageOrder;
    }

    /**
     * <p>
     * currentPageOrder.
     * </p>
     *
     * @param currentPageOrder the currentPageOrder to set
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws RecordNotFoundException
     * @throws PresentationException
     * @throws IDDOCNotFoundException
     */
    public void setCurrentImageOrder(int currentImageOrder) throws IndexUnreachableException, PresentationException, IDDOCNotFoundException {
        logger.trace("setCurrentImageNo: {}", currentImageOrder);
        if (pageLoader == null) {
            return;
        }

        if (currentImageOrder < pageLoader.getFirstPageOrder()) {
            currentImageOrder = pageLoader.getFirstPageOrder();
        } else if (currentImageOrder >= pageLoader.getLastPageOrder()) {
            currentImageOrder = pageLoader.getLastPageOrder();
        }
        this.currentImageOrder = currentImageOrder;
        persistentUrl = null;

        if (StringUtils.isEmpty(logId)) {
            Long iddoc = pageLoader.getOwnerIddocForPage(currentImageOrder);
            // Set the currentDocumentIddoc to the IDDOC of the image owner document, but only if no specific document LOGID has been requested
            if (iddoc != null && iddoc > -1) {
                currentStructElementIddoc = iddoc;
                logger.trace("currentDocumentIddoc: {} ({})", currentStructElementIddoc, pi);
            } else if (isHasPages()) {
                logger.warn("currentDocumentIddoc not found for '{}', page {}", pi, currentImageOrder);
                throw new IDDOCNotFoundException("currentElementIddoc not found for '" + pi + "', page " + currentImageOrder);
            }
        } else {
            // If a specific LOGID has been requested, look up its IDDOC
            logger.trace("Selecting currentElementIddoc by LOGID: {} ({})", logId, pi);
            long iddoc = DataManager.getInstance().getSearchIndex().getIddocByLogid(getPi(), logId);
            if (iddoc > -1) {
                currentStructElementIddoc = iddoc;
            } else {
                logger.trace("currentElementIddoc not found for '{}', LOGID: {}", pi, logId);
            }
            // Reset LOGID so that the same TOC element doesn't stay highlighted when flipping pages
            logId = null;
        }
        if (currentStructElement == null || currentStructElement.getLuceneId() != currentStructElementIddoc) {
            setCurrentStructElement(new StructElement(currentStructElementIddoc));
        }
    }

    /**
     * Main method for setting the current page(s) in this ViewManager.
     * 
     * @param currentImageOrderString A string containing a single page number or a range of two pages
     * @throws NumberFormatException
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws IDDOCNotFoundException
     */
    public void setCurrentImageOrderString(String currentImageOrderString)
            throws NumberFormatException, IndexUnreachableException, PresentationException, IDDOCNotFoundException {
        if (StringUtils.isEmpty(currentImageOrderString)) {
            return;
        }

        int newImageOrder = 1;
        if (currentImageOrderString.contains("-")) {
            String[] orderSplit = currentImageOrderString.split("[-]");
            newImageOrder = Integer.valueOf(orderSplit[0]);
        } else {
            newImageOrder = Integer.valueOf(currentImageOrderString);
        }

        setCurrentImageOrder(newImageOrder);
    }

    /**
     * Returns the ORDERLABEL value for the current page.
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getCurrentImageLabel() throws IndexUnreachableException, DAOException {
        PhysicalElement currentPage = getCurrentPage();
        if (currentPage != null) {
            return currentPage.getOrderLabel().trim();
        }

        return null;
    }

    /**
     * <p>
     * nextImage.
     * </p>
     *
     * @return {@link java.lang.String}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws IDDOCNotFoundException
     */
    public String nextImage() throws IndexUnreachableException, PresentationException, IDDOCNotFoundException {
        //        logger.debug("currentImageNo: {}", currentImageOrder);
        if (currentImageOrder < pageLoader.getLastPageOrder()) {
            setCurrentImageOrder(currentImageOrder);
        }
        updateDropdownSelected();
        return null;
    }

    /**
     * <p>
     * prevImage.
     * </p>
     *
     * @return {@link java.lang.String}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws IDDOCNotFoundException
     */
    public String prevImage() throws IndexUnreachableException, PresentationException, IDDOCNotFoundException {
        if (currentImageOrder > 0) {
            setCurrentImageOrder(currentImageOrder);
        }
        updateDropdownSelected();
        return "";
    }

    /**
     * <p>
     * firstImage.
     * </p>
     *
     * @return {@link java.lang.String}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws IDDOCNotFoundException
     */
    public String firstImage() throws IndexUnreachableException, PresentationException, IDDOCNotFoundException {
        setCurrentImageOrder(pageLoader.getFirstPageOrder());
        updateDropdownSelected();
        return null;
    }

    /**
     * <p>
     * lastImage.
     * </p>
     *
     * @return {@link java.lang.String}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws IDDOCNotFoundException
     */
    public String lastImage() throws IndexUnreachableException, PresentationException, IDDOCNotFoundException {
        setCurrentImageOrder(pageLoader.getLastPageOrder());
        updateDropdownSelected();
        return null;
    }

    /**
     * <p>
     * isMultiPageRecord.
     * </p>
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public boolean isMultiPageRecord() throws IndexUnreachableException {
        return getImagesCount() > 1;
    }

    /**
     * <p>
     * getImagesCount.
     * </p>
     *
     * @return {@link java.lang.Integer}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public int getImagesCount() throws IndexUnreachableException {
        if (pageLoader == null) {
            return -1;
        }
        return pageLoader.getNumPages();
    }

    /**
     * 
     * @return Last page number
     */    public int getLastPageOrder() {
        if (pageLoader == null) {
            return -1;
        }
        return pageLoader.getLastPageOrder();
    }

     /**
      * 
      * @return First page number
      */
    public int getFirstPageOrder() {
        if (pageLoader == null) {
            return -1;
        }
        return pageLoader.getFirstPageOrder();
    }

    /**
     * <p>
     * Getter for the field <code>dropdownPages</code>.
     * </p>
     *
     * @return the dropdownPages
     */
    public List<SelectItem> getDropdownPages() {
        return dropdownPages;
    }

    /**
     * <p>
     * Getter for the field <code>dropdownFulltext</code>.
     * </p>
     *
     * @return the dropdownPages
     */
    public List<SelectItem> getDropdownFulltext() {
        return dropdownFulltext;
    }

    /**
     * <p>
     * Setter for the field <code>dropdownSelected</code>.
     * </p>
     *
     * @param dropdownSelected the dropdownSelected to set
     */
    public void setDropdownSelected(String dropdownSelected) {
        this.dropdownSelected = dropdownSelected;
        //        logger.debug("dropdownSelected: " + dropdownSelected);
    }

    /**
     * <p>
     * Getter for the field <code>dropdownSelected</code>.
     * </p>
     *
     * @return the dropdownSelected
     */
    public String getDropdownSelected() {
        return dropdownSelected;
    }

    /**
     *
     * Returns the PhysicalElements for the current thumbnail page using the configured number of thumbnails per page;
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<PhysicalElement> getImagesSection() throws IndexUnreachableException, DAOException {
        return getImagesSection(DataManager.getInstance().getConfiguration().getViewerThumbnailsPerPage());
    }

    /**
     * Returns the PhysicalElements for the current thumbnail page.
     *
     * @param thumbnailsPerPage Length of the thumbnail list per page.
     * @return PhysicalElements for the current thumbnail page.
     * @should return correct PhysicalElements for a thumbnail page
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    protected List<PhysicalElement> getImagesSection(int thumbnailsPerPage) throws IndexUnreachableException, DAOException {
        List<PhysicalElement> imagesSection = new ArrayList<>();

        if (pageLoader != null) {
            int i = getFirstDisplayedThumbnailIndex(thumbnailsPerPage);
            int end = getLastDisplayedThumbnailIndex(thumbnailsPerPage);
            //        logger.debug(i + " - " + end);
            for (; i < end; i++) {
                if (i > pageLoader.getLastPageOrder()) {
                    break;
                }
                if (pageLoader.getPage(i) != null) {
                    imagesSection.add(pageLoader.getPage(i));
                }
            }
        }

        return imagesSection;
    }

    /**
     * @param thumbnailsPerPage
     * @param i
     * @return
     */
    private int getLastDisplayedThumbnailIndex(int thumbnailsPerPage) {
        int end = getFirstDisplayedThumbnailIndex(thumbnailsPerPage) + thumbnailsPerPage;
        return end;
    }

    /**
     * @param thumbnailsPerPage
     * @return
     */
    private int getFirstDisplayedThumbnailIndex(int thumbnailsPerPage) {
        int i = pageLoader.getFirstPageOrder();
        if (currentThumbnailPage > 1) {
            i = (currentThumbnailPage - 1) * thumbnailsPerPage + 1;
        }
        return i;
    }

    /**
     * <p>
     * getFirstDisplayedThumbnailIndex.
     * </p>
     *
     * @return a int.
     */
    public int getFirstDisplayedThumbnailIndex() {
        return getFirstDisplayedThumbnailIndex(DataManager.getInstance().getConfiguration().getViewerThumbnailsPerPage());
    }

    /**
     * <p>
     * Getter for the field <code>currentThumbnailPage</code>.
     * </p>
     *
     * @return a int.
     */
    public int getCurrentThumbnailPage() {
        return currentThumbnailPage;
    }

    /**
     * <p>
     * Setter for the field <code>currentThumbnailPage</code>.
     * </p>
     *
     * @param currentThumbnailPage a int.
     */
    public void setCurrentThumbnailPage(int currentThumbnailPage) {
        this.currentThumbnailPage = currentThumbnailPage;
    }

    /**
     * <p>
     * nextThumbnailSection.
     * </p>
     */
    public void nextThumbnailSection() {
        ++currentThumbnailPage;
    }

    /**
     * <p>
     * previousThumbnailSection.
     * </p>
     */
    public void previousThumbnailSection() {
        --currentThumbnailPage;
    }

    /**
     * <p>
     * hasPreviousThumbnailSection.
     * </p>
     *
     * @return a boolean.
     */
    public boolean hasPreviousThumbnailSection() {
        int currentFirstThumbnailIndex = getFirstDisplayedThumbnailIndex();
        int previousLastThumbnailIndex = currentFirstThumbnailIndex - DataManager.getInstance().getConfiguration().getViewerThumbnailsPerPage();
        return previousLastThumbnailIndex >= pageLoader.getFirstPageOrder();
    }

    /**
     * <p>
     * hasNextThumbnailSection.
     * </p>
     *
     * @return a boolean.
     */
    public boolean hasNextThumbnailSection() {
        int currentFirstThumbnailIndex = getFirstDisplayedThumbnailIndex();
        int previousLastThumbnailIndex = currentFirstThumbnailIndex + DataManager.getInstance().getConfiguration().getViewerThumbnailsPerPage();
        return previousLastThumbnailIndex <= pageLoader.getLastPageOrder();
    }

    /**
     * <p>
     * updateDropdownSelected.
     * </p>
     */
    public void updateDropdownSelected() {
        setDropdownSelected(String.valueOf(currentImageOrder));
    }

    /**
     * <p>
     * dropdownAction.
     * </p>
     *
     * @param event {@link javax.faces.event.ValueChangeEvent}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws java.lang.NumberFormatException if any.
     * @throws IDDOCNotFoundException
     */
    public void dropdownAction(ValueChangeEvent event)
            throws NumberFormatException, IndexUnreachableException, PresentationException, IDDOCNotFoundException {
        setCurrentImageOrder(Integer.valueOf((String) event.getNewValue()) - 1);
    }

    /**
     * <p>
     * getImagesSizeThumbnail.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getImagesSizeThumbnail() throws IndexUnreachableException {
        if (pageLoader == null) {
            return "0";
        }

        double im = pageLoader.getNumPages();
        double thumb = DataManager.getInstance().getConfiguration().getViewerThumbnailsPerPage();
        int answer = (int) Math.floor(im / thumb);
        if (im % thumb != 0 || answer == 0) {
            answer++;
        }

        return String.valueOf(answer);
    }

    /**
     * <p>
     * getLinkForDFGViewer.
     * </p>
     *
     * @return DFG Viewer link
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getLinkForDFGViewer() throws IndexUnreachableException {
        if (topStructElement != null && SolrConstants._METS.equals(topStructElement.getSourceDocFormat()) && isHasPages()) {
            try {
                StringBuilder sbPath = new StringBuilder();
                sbPath.append(DataManager.getInstance().getConfiguration().getDfgViewerUrl());
                sbPath.append(URLEncoder.encode(getMetsResolverUrl(), "utf-8"));
                sbPath.append("&set[image]=").append(currentImageOrder);
                return sbPath.toString();
            } catch (UnsupportedEncodingException e) {
                logger.error("error while encoding url", e);
                return null;
            }
        }

        return null;
    }

    /**
     * <p>
     * getMetsResolverUrl.
     * </p>
     *
     * @return METS resolver link for the DFG Viewer
     */
    public String getMetsResolverUrl() {
        try {
            return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/metsresolver?id=" + getPi();
        } catch (Exception e) {
            logger.error("Could not get METS resolver URL for {}.", topStructElementIddoc);
            Messages.error("errGetCurrUrl");
        }
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/metsresolver?id=" + 0;
    }

    /**
     * <p>
     * getLidoResolverUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getLidoResolverUrl() {
        try {
            return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/lidoresolver?id=" + getPi();
        } catch (Exception e) {
            logger.error("Could not get LIDO resolver URL for {}.", topStructElementIddoc);
            Messages.error("errGetCurrUrl");
        }
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/lidoresolver?id=" + 0;
    }

    /**
     * <p>
     * getDenkxwebResolverUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getDenkxwebResolverUrl() {
        try {
            return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/denkxwebresolver?id=" + getPi();
        } catch (Exception e) {
            logger.error("Could not get DenkXweb resolver URL for {}.", topStructElementIddoc);
            Messages.error("errGetCurrUrl");
        }
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/denkxwebresolver?id=" + 0;
    }

    /**
     * <p>
     * getDublinCoreResolverUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getDublinCoreResolverUrl() {
        try {
            return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/dublincoreresolver?id=" + getPi();
        } catch (Exception e) {
            logger.error("Could not get DublinCore resolver URL for {}.", topStructElementIddoc);
            Messages.error("errGetCurrUrl");
        }
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/dublincoreresolver?id=" + 0;
    }

    /**
     * <p>
     * getAnchorMetsResolverUrl.
     * </p>
     *
     * @return METS resolver URL for the anchor; null if no parent PI found (must be null, otherwise an empty link will be displayed).
     */
    public String getAnchorMetsResolverUrl() {
        if (anchorStructElement != null) {
            String parentPi = anchorStructElement.getMetadataValue(SolrConstants.PI);
            return new StringBuilder(BeanUtils.getServletPathWithHostAsUrlFromJsfContext()).append("/metsresolver?id=").append(parentPi).toString();
        }

        return null;
    }

    /**
     * Return the url to a REST service delivering all alto files of a work as zip
     *
     * @return the url to a REST service delivering all alto files of a work as zip
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getAltoUrlForAllPages() throws ViewerConfigurationException, PresentationException, IndexUnreachableException {
        String pi = getPi();
        return DataManager.getInstance()
                .getRestApiManager()
                .getContentApiManager()
                .map(urls -> urls.path(RECORDS_RECORD, RECORDS_ALTO_ZIP).params(pi).build())
                .orElse("");
    }

    /**
     * Return the url to a REST service delivering all plain text of a work as zip
     *
     * @return the url to a REST service delivering all plain text of a work as zip
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getFulltextUrlForAllPages() throws ViewerConfigurationException, PresentationException, IndexUnreachableException {
        String pi = getPi();
        return DataManager.getInstance()
                .getRestApiManager()
                .getContentApiManager()
                .map(urls -> urls.path(RECORDS_RECORD, RECORDS_PLAINTEXT_ZIP).params(pi).build())
                .orElse("");
    }

    /**
     * Return the url to a REST service delivering a TEI document containing the text of all pages
     *
     * @return the TEI REST url
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getTeiUrlForAllPages() throws ViewerConfigurationException, IndexUnreachableException {
        String pi = getPi();
        return DataManager.getInstance()
                .getRestApiManager()
                .getContentApiManager()
                .map(urls -> urls.path(RECORDS_RECORD, RECORDS_TEI_LANG)
                        .params(pi, BeanUtils.getLocale().getLanguage())
                        .build())
                .orElse("");
    }

    /**
     * Return the url to a REST service delivering the fulltext of the current page as TEI
     *
     * @return the TEI REST url
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getTeiUrl() throws ViewerConfigurationException, IndexUnreachableException, DAOException {
        String plaintextFilename = null;
        try {
            plaintextFilename = FileTools.getFilenameFromPathString(getCurrentPage().getFulltextFileName());
        } catch (FileNotFoundException e) {
            logger.trace("FULLTEXT not found: {}", e.getMessage());
        }
        String altoFilename = null;
        try {
            altoFilename = FileTools.getFilenameFromPathString(getCurrentPage().getAltoFileName());
        } catch (FileNotFoundException e) {
            logger.trace("ALTO not found: {}", e.getMessage());
        }
        String filenameToUse = StringUtils.isNotBlank(plaintextFilename) ? plaintextFilename : altoFilename;
        if (StringUtils.isBlank(filenameToUse)) {
            return "";
        }

        String pi = getPi();
        return DataManager.getInstance()
                .getRestApiManager()
                .getContentApiManager()
                .map(urls -> urls.path(RECORDS_FILES, RECORDS_FILES_TEI)
                        .params(pi, filenameToUse)
                        .build())
                .orElse("");
    }

    /**
     * Return the url to a REST service delivering the alto file of the given page as xml
     *
     * @return the url to a REST service delivering the alto file of the given page as xml
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getAltoUrl() throws ViewerConfigurationException, PresentationException, IndexUnreachableException, DAOException {
        String filename;
        try {
            filename = FileTools.getFilenameFromPathString(getCurrentPage().getAltoFileName());
        } catch (FileNotFoundException e) {
            return "";
        }
        String pi = getPi();
        return DataManager.getInstance()
                .getRestApiManager()
                .getContentApiManager()
                .map(urls -> urls.path(RECORDS_FILES, RECORDS_FILES_ALTO)
                        .params(pi, filename)
                        .build())
                .orElse("");
    }

    /**
     * Return the url to a REST service delivering the fulltext as plain text of the given page
     *
     * @return the url to a REST service delivering the fulltext as plain text of the given page
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getFulltextUrl() throws ViewerConfigurationException, PresentationException, IndexUnreachableException, DAOException {
        String plaintextFilename = null;
        try {
            plaintextFilename = FileTools.getFilenameFromPathString(getCurrentPage().getFulltextFileName());
        } catch (FileNotFoundException e) {
            logger.trace("FULLTEXT not found: {}", e.getMessage());
        }
        String altoFilename = null;
        try {
            altoFilename = FileTools.getFilenameFromPathString(getCurrentPage().getAltoFileName());
        } catch (FileNotFoundException e) {
            logger.trace("ALTO not found: {}", e.getMessage());
        }
        String filenameToUse = StringUtils.isNotBlank(plaintextFilename) ? plaintextFilename : altoFilename;
        if (StringUtils.isBlank(filenameToUse)) {
            return "";
        }

        String pi = getPi();
        return DataManager.getInstance()
                .getRestApiManager()
                .getContentApiManager()
                .map(urls -> urls.path(RECORDS_FILES, RECORDS_FILES_PLAINTEXT)
                        .params(pi, filenameToUse)
                        .build())
                .orElse("");
    }

    /**
     * Returns the pdf download link for the current document
     *
     * @return {@link java.lang.String}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getPdfDownloadLink() throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        return imageDeliveryBean.getPdf().getPdfUrl(getTopStructElement(), "");
    }

    /**
     * Returns the pdf download link for the current page
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getPdfPageDownloadLink() throws IndexUnreachableException, DAOException, ViewerConfigurationException {
        PhysicalElement currentPage = getCurrentPage();
        if (currentPage == null) {
            return null;
        }
        return imageDeliveryBean.getPdf().getPdfUrl(getTopStructElement(), currentPage);
    }

    /**
     * Returns the pdf download link for the current struct element
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public String getPdfStructDownloadLink() throws IndexUnreachableException, DAOException, ViewerConfigurationException, PresentationException {
        StructElement currentStruct = getCurrentStructElement();
        return imageDeliveryBean.getPdf().getPdfUrl(currentStruct, currentStruct.getLabel());

    }

    /**
     * Returns the pdf download link for a pdf of all pages from this.firstPdfPage to this.lastPdfPage (inclusively)
     *
     * @should construct url correctly
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getPdfPartDownloadLink() throws IndexUnreachableException, DAOException, ViewerConfigurationException {
        logger.trace("getPdfPartDownloadLink: {}-{}", firstPdfPage, lastPdfPage);
        if (firstPdfPage > pageLoader.getLastPageOrder()) {
            firstPdfPage = pageLoader.getLastPageOrder();
        }
        if (lastPdfPage > pageLoader.getLastPageOrder()) {
            lastPdfPage = pageLoader.getLastPageOrder();
        }
        if (firstPdfPage < 1) {
            firstPdfPage = 1;
        }
        if (lastPdfPage < firstPdfPage) {
            lastPdfPage = firstPdfPage;
        }

        //        StringBuilder sb = new StringBuilder(DataManager.getInstance().getConfiguration().getContentServerWrapperUrl()).append("?action=pdf&images=");
        List<PhysicalElement> pages = new ArrayList<>();
        for (int i = firstPdfPage; i <= lastPdfPage; ++i) {
            PhysicalElement page = pageLoader.getPage(i);
            pages.add(page);
            //            sb.append(getPi()).append('/').append(page.getFileName()).append('$');
        }
        PhysicalElement[] pageArr = new PhysicalElement[pages.size()];
        return imageDeliveryBean.getPdf().getPdfUrl(getTopStructElement(), pages.toArray(pageArr));
    }

    /**
     * <p>
     * isPdfPartDownloadLinkEnabled.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isPdfPartDownloadLinkEnabled() {
        return firstPdfPage <= lastPdfPage;
    }

    /**
     * Reset the pdf access permissions. They will be evaluated again on the next call to {@link #isAccessPermissionPdf()}
     */
    public void resetAccessPermissionPdf() {
        this.accessPermissionPdf = null;
    }

    /**
     * <p>
     * isAccessPermissionPdf.
     * </p>
     *
     * @return true if record/structure PDF download is allowed; false otherwise
     */
    public boolean isAccessPermissionPdf() {
        try {
            if (topStructElement == null || !topStructElement.isWork() || !isHasPages()) {
                return false;
            }
            if (!MimeType.isImageOrPdfDownloadAllowed(topStructElement.getMetadataValue(SolrConstants.MIMETYPE))) {
                return false;
            }
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
            return false;
        }
        // Only allow PDF downloads for records coming from METS files
        if (!SolrConstants._METS.equals(topStructElement.getSourceDocFormat())) {
            return false;
        }

        if (accessPermissionPdf == null) {
            try {
                accessPermissionPdf = isAccessPermission(IPrivilegeHolder.PRIV_DOWNLOAD_PDF);
            } catch (IndexUnreachableException e) {
                logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
                return false;
            } catch (DAOException e) {
                logger.debug("DAOException thrown here: {}", e.getMessage());
                return false;
            } catch (RecordNotFoundException e) {
                logger.error("Record not found in index: {}", pi);
                return false;
            }
        }

        return accessPermissionPdf;
    }

    /**
     * 
     * @param privilege Privilege name to check
     * @return true if current user has the privilege for this record; false otherwise
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws RecordNotFoundException
     */
    public boolean isAccessPermission(String privilege) throws IndexUnreachableException, DAOException, RecordNotFoundException {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        return AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(getPi(), null, privilege, request);
    }

    /**
     * Reset the permissions for writing user comments. They will be evaluated again on the next call to {@link #isAllowUserComments()}
     */
    public void resetAllowUserComments() {
        this.allowUserComments = null;
    }

    /**
     * Indicates whether user comments are allowed for the current record based on several criteria.
     *
     * @return a boolean.
     */
    public boolean isAllowUserComments() {
        if (!DataManager.getInstance().getConfiguration().isUserCommentsEnabled()) {
            return false;
        }

        if (allowUserComments == null) {
            String query = DataManager.getInstance().getConfiguration().getUserCommentsConditionalQuery();
            try {
                if (StringUtils.isNotEmpty(query) && DataManager.getInstance()
                        .getSearchIndex()
                        .getHitCount(new StringBuilder(SolrConstants.PI).append(':')
                                .append(pi)
                                .append(" AND (")
                                .append(query)
                                .append(')')
                                .toString()) == 0) {
                    allowUserComments = false;
                    logger.trace("User comments are not allowed for this record.");
                } else {
                    allowUserComments = true;
                }
            } catch (IndexUnreachableException e) {
                logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
                return false;
            } catch (PresentationException e) {
                logger.debug("PresentationException thrown here: {}", e.getMessage());
                return false;
            }
        }

        return allowUserComments;
    }

    /**
     * <p>
     * isDisplayTitleBarPdfLink.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplayTitleBarPdfLink() {
        return DataManager.getInstance().getConfiguration().isTitlePdfEnabled() && isAccessPermissionPdf();
    }

    /**
     * <p>
     * isDisplayMetadataPdfLink.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplayMetadataPdfLink() {
        return topStructElement != null && topStructElement.isWork() && DataManager.getInstance().getConfiguration().isMetadataPdfEnabled()
                && isAccessPermissionPdf();
    }

    /**
     * <p>
     * isDisplayPagePdfLink.
     * </p>
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    @Deprecated
    public boolean isDisplayPagePdfLink() throws IndexUnreachableException, DAOException {
        PhysicalElement currentPage = getCurrentPage();
        if (currentPage != null) {
            return currentPage.isDisplayPagePdfLink();
        }

        return false;
    }

    /**
     * Convenience method that checks whether only the metadata view link is displayed for this record (i.e. criteria for all other links are not
     * met).
     * 
     * @return
     * @throws DAOException
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws ViewerConfigurationException
     */
    public boolean isMetadataViewOnly() throws IndexUnreachableException, DAOException, PresentationException, ViewerConfigurationException {
        if (metadataViewOnly == null) {
            // Check whether this mode is enabled first to avoid all the other checks
            if (!DataManager.getInstance().getConfiguration().isShowRecordLabelIfNoOtherViews()) {
                metadataViewOnly = false;
                return metadataViewOnly;
            }

            // Display object view criteria
            if (isDisplayObjectViewLink()) {
                metadataViewOnly = false;
                return metadataViewOnly;
            }
            if (isDisplayCalendarViewLink()) {
                metadataViewOnly = false;
                return metadataViewOnly;
            }
            if (isDisplayTocViewLink()) {
                metadataViewOnly = false;
                return metadataViewOnly;
            }
            if (isDisplayThumbnailViewLink()) {
                metadataViewOnly = false;
                return metadataViewOnly;
            }
            if (isDisplayFulltextViewLink()) {
                metadataViewOnly = false;
                return metadataViewOnly;
            }
            if (isDisplayExternalFulltextLink()) {
                metadataViewOnly = false;
                return metadataViewOnly;
            }
            if (isDisplayNerViewLink()) {
                metadataViewOnly = false;
                return metadataViewOnly;
            }
            if (isDisplayExternalResolverLink()) {
                metadataViewOnly = false;
                return metadataViewOnly;
            }

            metadataViewOnly = true;
        }

        return metadataViewOnly;
    }

    /**
     * 
     * @return true if object view link may be displayed; false otherwise
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public boolean isDisplayObjectViewLink() throws IndexUnreachableException, DAOException {
        return DataManager.getInstance().getConfiguration().isSidebarPageViewLinkVisible() && isHasPages() && !isFilesOnly();
    }

    /**
     * 
     * @return true if calendar view link may be displayed; false otherwise
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws PresentationException
     */
    public boolean isDisplayCalendarViewLink() throws IndexUnreachableException, DAOException, PresentationException {
        return DataManager.getInstance().getConfiguration().isSidebarCalendarViewLinkVisible() && calendarView != null && calendarView.isDisplay();
    }

    /**
     * 
     * @return true if TOC view link may be displayed; false otherwise
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws PresentationException
     */
    public boolean isDisplayTocViewLink() throws IndexUnreachableException, DAOException, PresentationException {
        return DataManager.getInstance().getConfiguration().isSidebarTocViewLinkVisible() && !isFilesOnly() && topStructElement != null
                && !topStructElement.isLidoRecord() && toc != null
                && (toc.isHasChildren() || DataManager.getInstance().getConfiguration().isDisplayEmptyTocInSidebar());
    }

    /**
     * 
     * @return true if thumbnail view link may be displayed; false otherwise
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws PresentationException
     */
    public boolean isDisplayThumbnailViewLink() throws IndexUnreachableException, DAOException, PresentationException {
        return DataManager.getInstance().getConfiguration().isSidebarThumbsViewLinkVisible()
                && pageLoader != null && pageLoader.getNumPages() > 1 && !isFilesOnly();
    }

    /**
     * 
     * @return true if metadata view link may be displayed; false otherwise
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws PresentationException
     */
    public boolean isDisplayMetadataViewLink() throws IndexUnreachableException, DAOException, PresentationException {
        return DataManager.getInstance().getConfiguration().isSidebarMetadataViewLinkVisible() && topStructElement != null
                && !topStructElement.isGroup();
    }

    /**
     * 
     * @return true if full-text view link may be displayed; false otherwise
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws PresentationException
     * @throws ViewerConfigurationException
     */
    public boolean isDisplayFulltextViewLink() throws IndexUnreachableException, DAOException, PresentationException, ViewerConfigurationException {
        return DataManager.getInstance().getConfiguration().isSidebarFulltextLinkVisible() && topStructElement != null
                && topStructElement.isFulltextAvailable()
                && !isFilesOnly()
                && getCurrentPage() != null
                && getCurrentPage().isFulltextAccessPermission();
    }

    /**
     * 
     * @return true if external full-text link may be displayed; false otherwise
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws PresentationException
     * @throws ViewerConfigurationException
     */
    public boolean isDisplayExternalFulltextLink()
            throws IndexUnreachableException, DAOException, PresentationException, ViewerConfigurationException {
        return topStructElement != null
                && topStructElement.getMetadataValue("MD_LOCATION_URL_EXTERNALFULLTEXT") != null && getCurrentPage() != null
                && getCurrentPage().isFulltextAccessPermission();
    }

    /**
     * 
     * @return true if NER view link may be displayed; false otherwise
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws PresentationException
     */
    public boolean isDisplayNerViewLink() throws IndexUnreachableException, DAOException, PresentationException {
        return topStructElement != null && topStructElement.isNerAvailable();
    }

    /**
     * 
     * @return true if NER view link may be displayed; false otherwise
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws PresentationException
     */
    public boolean isDisplayExternalResolverLink() throws IndexUnreachableException, DAOException, PresentationException {
        return topStructElement != null
                && topStructElement.getMetadataValue("MD_LOCATION_URL_EXTERNALRESOLVER") != null;
    }

    /**
     * <p>
     * getOaiMarcUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getOaiMarcUrl() throws IndexUnreachableException {
        return DataManager.getInstance().getConfiguration().getMarcUrl() + getPi();
    }

    /**
     * <p>
     * getOaiDcUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getOaiDcUrl() throws IndexUnreachableException {
        return DataManager.getInstance().getConfiguration().getDcUrl() + getPi();
    }

    /**
     * <p>
     * getOaiEseUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getOaiEseUrl() throws IndexUnreachableException {
        return DataManager.getInstance().getConfiguration().getEseUrl() + getPi();
    }

    /**
     * <p>
     * Getter for the field <code>opacUrl</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getOpacUrl() {
        if (currentStructElement != null && opacUrl == null) {
            try {
                StructElement topStruct = currentStructElement.getTopStruct();
                if (topStruct != null) {
                    opacUrl = topStruct.getMetadataValue(SolrConstants.OPACURL);
                }
            } catch (PresentationException e) {
                logger.debug("PresentationException thrown here: {}", e.getMessage());
            } catch (IndexUnreachableException e) {
                logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
            }
        }

        return opacUrl;
    }

    /**
     * <p>
     * Getter for the field <code>persistentUrl</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getPersistentUrl() throws IndexUnreachableException, DAOException {
        PhysicalElement currentPage = getCurrentPage();
        if (topStructElement != null) {
            String customPURL = topStructElement.getMetadataValue("MD_PURL");
            if (StringUtils.isNotEmpty(customPURL)) {
                return customPURL;
            }
        }
        String urn = currentPage != null ? currentPage.getUrn() : null;
        if (urn == null && currentStructElement != null) {
            urn = currentStructElement.getMetadataValue(SolrConstants.URN);
        }

        if (persistentUrl == null) {
            persistentUrl = getPersistentUrl(urn);
        }
        return persistentUrl;
    }

    /**
     * Returns the PURL for the current page (either via the URN resolver or a pretty URL)
     *
     * @return PURL for the current page
     * @should generate purl via urn correctly
     * @should generate purl without urn correctly
     * @param urn a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getPersistentUrl(String urn) throws IndexUnreachableException {
        String persistentUrl = "";
        StringBuilder url = new StringBuilder();
        if (StringUtils.isNotEmpty(urn) && !urn.equalsIgnoreCase("NULL")) {
            // URN-based PURL
            if (urn.startsWith("http:") || urn.startsWith("https:")) {
                // URN is full URL
                persistentUrl = urn;
            } else {
                // Just the URN
                url.append(DataManager.getInstance().getConfiguration().getUrnResolverUrl()).append(urn);
                persistentUrl = url.toString();
            }
        } else {
            // Prefer configured target page type for the docstruct type
            PageType pageType = null;
            if (topStructElement != null) {
                boolean anchorOrGroup = topStructElement.isAnchor() || topStructElement.isGroup();
                pageType = PageType.determinePageType(topStructElement.getDocStructType(), null, anchorOrGroup, isHasPages(), false);
            }
            if (pageType == null) {
                if (isHasPages()) {
                    pageType = PageType.viewImage;
                } else {
                    pageType = PageType.viewMetadata;
                }
            }
            url.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext());
            url.append('/').append(pageType.getName()).append('/').append(getPi()).append('/').append(currentImageOrder).append('/');
            persistentUrl = url.toString();
        }
        logger.trace("PURL: {}", persistentUrl);

        return persistentUrl;
    }

    /**
     * Returns the main title of the current volume's anchor, if available.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getAnchorTitle() {
        if (anchorStructElement != null) {
            return anchorStructElement.getMetadataValue(SolrConstants.TITLE);
        }

        return null;
    }

    /**
     * Returns the main title of the current volume.
     *
     * @return The volume's main title.
     */
    public String getVolumeTitle() {
        if (topStructElement != null) {
            return topStructElement.getMetadataValue(SolrConstants.TITLE);
        }
        return null;
    }

    /**
     * <p>
     * isBelowFulltextThreshold.
     * </p>
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public boolean isBelowFulltextThreshold() throws PresentationException, IndexUnreachableException {
        int threshold = DataManager.getInstance().getConfiguration().getFulltextPercentageWarningThreshold();
        return isBelowFulltextThreshold(threshold);
    }

    /**
     * 
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @should return true if there are no pages
     */
    boolean isBelowFulltextThreshold(double threshold) throws PresentationException, IndexUnreachableException {
        if (pageLoader.getNumPages() == 0) {
            return true;
        }
        if (pagesWithFulltext == null) {
            pagesWithFulltext = DataManager.getInstance()
                    .getSearchIndex()
                    .getHitCount(new StringBuilder("+").append(SolrConstants.PI_TOPSTRUCT)
                            .append(':')
                            .append(pi)
                            .append(" +")
                            .append(SolrConstants.DOCTYPE)
                            .append(":PAGE")
                            .append(" +")
                            .append(SolrConstants.FULLTEXTAVAILABLE)
                            .append(":true")
                            .toString());
        }
        double percentage = pagesWithFulltext * 100.0 / pageLoader.getNumPages();
        // logger.trace("{}% of pages have full-text", percentage);
        if (percentage < threshold) {
            return true;
        }

        return false;
    }

    /**
     * <p>
     * isFulltextAvailableForWork.
     * </p>
     *
     * @return true if record has full-text and user has access rights; false otherwise
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws RecordNotFoundException
     */
    public boolean isFulltextAvailableForWork() throws IndexUnreachableException, DAOException, PresentationException, RecordNotFoundException {
        if (isBornDigital()) {
            return false;
        }

        boolean access = AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(getPi(), null, IPrivilegeHolder.PRIV_VIEW_FULLTEXT,
                BeanUtils.getRequest());
        return access && (!isBelowFulltextThreshold(0.0001) || isAltoAvailableForWork());
    }

    /**
     * 
     * @return true if any of this record's pages has an image and user has access rights; false otherwise
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws RecordNotFoundException
     */
    public boolean isRecordHasImages() throws IndexUnreachableException, DAOException, RecordNotFoundException {
        if (topStructElement == null || !topStructElement.isHasImages()) {
            return false;
        }

        return AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(getPi(), null, IPrivilegeHolder.PRIV_VIEW_IMAGES,
                BeanUtils.getRequest());
    }

    /**
     * <p>
     * isTeiAvailableForWork.
     * </p>
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws RecordNotFoundException
     */
    public boolean isTeiAvailableForWork() throws IndexUnreachableException, DAOException, PresentationException, RecordNotFoundException {
        if (isBornDigital()) {
            return false;
        }

        boolean access = AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(getPi(), null, IPrivilegeHolder.PRIV_VIEW_FULLTEXT,
                BeanUtils.getRequest());
        return access && (!isBelowFulltextThreshold(0.0001) || isAltoAvailableForWork() || isWorkHasTEIFiles());
    }

    /**
     * @return true if there are any TEI files associated directly with the top document
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    private boolean isWorkHasTEIFiles() throws IndexUnreachableException, PresentationException {
        if (workHasTEIFiles == null) {
            long teiDocs = DataManager.getInstance()
                    .getSearchIndex()
                    .getHitCount(new StringBuilder("+").append(SolrConstants.PI_TOPSTRUCT)
                            .append(':')
                            .append(pi)
                            .append(" + ")
                            .append(SolrConstants.DOCTYPE)
                            .append(":")
                            .append(SolrConstants.DOCSTRCT)
                            .append(" +")
                            .append(SolrConstants.FILENAME_TEI)
                            .append(":*")
                            .toString());
            int threshold = 1;
            logger.trace("{} of pages have tei", teiDocs);
            if (teiDocs < threshold) {
                workHasTEIFiles = false;
            } else {
                workHasTEIFiles = true;
            }
        }

        return workHasTEIFiles;
    }

    /**
     * @return the toc
     */
    public TOC getToc() {
        return toc;
    }

    /**
     * @param toc the toc to set
     */
    public void setToc(TOC toc) {
        this.toc = toc;
    }

    /**
     * <p>
     * isAltoAvailableForWork.
     * </p>
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws RecordNotFoundException
     */
    public boolean isAltoAvailableForWork() throws IndexUnreachableException, PresentationException, DAOException, RecordNotFoundException {
        boolean access = AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(getPi(), null, IPrivilegeHolder.PRIV_VIEW_FULLTEXT,
                BeanUtils.getRequest());
        if (!access) {
            return false;
        }
        if (pagesWithAlto == null) {

            pagesWithAlto = DataManager.getInstance()
                    .getSearchIndex()
                    .getHitCount(new StringBuilder("+").append(SolrConstants.PI_TOPSTRUCT)
                            .append(':')
                            .append(pi)
                            .append(" +")
                            .append(SolrConstants.DOCTYPE)
                            .append(":PAGE")
                            .append(" +")
                            .append(SolrConstants.FILENAME_ALTO)
                            .append(":*")
                            .toString());
            logger.trace("{} of pages have full-text", pagesWithAlto);
        }
        int threshold = 1;
        if (pagesWithAlto < threshold) {
            return false;
        }

        return true;
    }

    /**
     * Default fulltext getter (with HTML escaping).
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @deprecated Use <code>PhysicalElement.getFullText()</code>
     */
    @Deprecated
    public String getFulltext() throws IndexUnreachableException, DAOException, ViewerConfigurationException {
        return getFulltext(true, null);
    }

    /**
     * Returns the full-text for the current page, stripped of any included JavaScript.
     *
     * @param escapeHtml If true HTML tags will be escaped to prevent pseudo-HTML from breaking the text.
     * @param language a {@link java.lang.String} object.
     * @return Full-text for the current page.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @deprecated Use <code>PhysicalElement.getFullText()</code>
     */
    @Deprecated
    public String getFulltext(boolean escapeHtml, String language) throws IndexUnreachableException, DAOException, ViewerConfigurationException {
        String currentFulltext = null;

        // Current page fulltext

        if (isDoublePageMode()) {
            // Double page view
            StringBuilder sb = new StringBuilder();
            Optional<PhysicalElement> leftPage = getCurrentLeftPage();
            if (leftPage.isPresent() && StringUtils.isNotEmpty(leftPage.get().getFullText())) {
                sb.append(leftPage.get().getFullText());
            }
            Optional<PhysicalElement> rightPage = getCurrentRightPage();
            if (rightPage.isPresent() && StringUtils.isNotEmpty(rightPage.get().getFullText())) {
                if (sb.length() > 0) {
                    sb.append("<hr />");
                }
                sb.append(rightPage.get().getFullText());
            }
            currentFulltext = sb.toString();
        } else {
            // Single page view
            PhysicalElement currentPage = getCurrentPage();
            if (currentPage == null || StringUtils.isEmpty(currentPage.getFullText())) {
                return currentFulltext;
            }
            currentFulltext = currentPage.getFullText();
        }

        if (escapeHtml) {
            currentFulltext = StringTools.escapeHtmlChars(currentFulltext);
        }

        // logger.trace(currentFulltext);
        return currentFulltext;
    }

    /**
     * 
     * 
     * @return the probable mimeType of the fulltext of the current page. Loads the fulltext of that page if neccessary
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ViewerConfigurationException
     */
    public String getFulltextMimeType() throws IndexUnreachableException, DAOException, ViewerConfigurationException {
        PhysicalElement currentImg = getCurrentPage();
        if (currentImg != null) {
            return currentImg.getFulltextMimeType();
        }

        return null;
    }

    /**
     * <p>
     * getCurrentRotate.
     * </p>
     *
     * @return a int.
     */
    public int getCurrentRotate() {
        return rotate;
    }

    /**
     * <p>
     * Setter for the field <code>zoomSlider</code>.
     * </p>
     *
     * @param zoomSlider a int.
     */
    public void setZoomSlider(int zoomSlider) {
        this.zoomSlider = zoomSlider;
    }

    /**
     * <p>
     * Getter for the field <code>zoomSlider</code>.
     * </p>
     *
     * @return a int.
     */
    public int getZoomSlider() {
        return this.zoomSlider;
    }

    /**
     * List all files in {@link Configuration#getOrigContentFolder()} for which accecss is granted and which are not hidden per config
     * 
     * @return the list of downloadable filenames. If no downloadable resources exists, an empty list is returned
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws IOException
     */
    private List<String> listDownloadableContent() throws PresentationException, IndexUnreachableException, DAOException, IOException {
        //        if (this.downloadFilenames == null) {
        Path sourceFileDir = DataFileTools.getDataFolder(pi, DataManager.getInstance().getConfiguration().getOrigContentFolder());
        if (Files.exists(sourceFileDir) && AccessConditionUtils.checkContentFileAccessPermission(pi,
                (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest())) {
            String hideDownloadFilesRegex = DataManager.getInstance().getConfiguration().getHideDownloadFileRegex();
            try (Stream<Path> files = Files.list(sourceFileDir)) {
                Stream<String> filenames = files.map(path -> path.getFileName().toString());
                if (StringUtils.isNotEmpty(hideDownloadFilesRegex)) {
                    filenames = filenames.filter(filename -> !filename.matches(hideDownloadFilesRegex));
                }
                this.downloadFilenames = filenames.collect(Collectors.toList());
            }
        } else {
            this.downloadFilenames = Collections.emptyList();
        }
        //        }
        return this.downloadFilenames;
    }

    /**
     * Returns true if original content download has been enabled in the configuration and there are files in the original content folder for this
     * record.
     *
     * @return a boolean.
     */
    public boolean isDisplayContentDownloadMenu() {
        if (!DataManager.getInstance().getConfiguration().isDisplaySidebarWidgetDownloads()) {
            return false;
        }
        try {
            return !listDownloadableContent().isEmpty();
        } catch (PresentationException | IndexUnreachableException | DAOException | IOException e) {
            logger.warn("Error listing downloadable content: " + e.toString());
        }

        return false;
    }

    /**
     * Returns a list of original content file download links (name+url) for the current document.
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws IOException
     */
    public List<LabeledLink> getContentDownloadLinksForWork() throws IOException, PresentationException, IndexUnreachableException, DAOException {
        AlphanumCollatorComparator comparator = new AlphanumCollatorComparator(null);
        List<LabeledLink> links = listDownloadableContent().stream()
                .sorted(comparator)
                .map(this::getLinkToDownloadFile)
                .filter(link -> link != LabeledLink.EMPTY)
                .collect(Collectors.toList());
        return links;

    }

    private LabeledLink getLinkToDownloadFile(String filename) {
        try {
            String pi = getPi();
            String filenameEncoded = URLEncoder.encode(filename, StringTools.DEFAULT_ENCODING);
            return DataManager.getInstance()
                    .getRestApiManager()
                    .getContentApiManager()
                    .map(urls -> urls.path(ApiUrls.RECORDS_FILES, ApiUrls.RECORDS_FILES_SOURCE).params(pi, filenameEncoded).build())
                    .map(url -> new LabeledLink(filename, url, 0))
                    .orElse(LabeledLink.EMPTY);
        } catch (UnsupportedEncodingException | IndexUnreachableException e) {
            logger.error("Failed to create download link to " + filename, e);
            return LabeledLink.EMPTY;
        }
    }

    /**
     * <p>
     * Getter for the field <code>topStructElementIddoc</code>.
     * </p>
     *
     * @return the topStructElementIddoc
     */
    public long getTopStructElementIddoc() {
        return topStructElementIddoc;
    }

    @Deprecated
    public long getTopDocumentIddoc() {
        return getTopStructElementIddoc();
    }

    public Long getAnchorDocumentIddoc() {
        if (this.anchorStructElement != null) {
            return anchorStructElement.getLuceneId();
        }

        return null;
    }

    /**
     * Returns <code>topDocument</code>. If the IDDOC of <code>topDocument</code> is different from <code>topDocumentIddoc</code>,
     * <code>topDocument</code> is reloaded.
     *
     * @return the currentDocument
     * @throws IndexUnreachableException
     */
    private StructElement loadTopStructElement() throws IndexUnreachableException {
        if (topStructElement == null || topStructElement.getLuceneId() != topStructElementIddoc) {
            topStructElement = new StructElement(topStructElementIddoc, null);
        }
        return topStructElement;
    }

    /**
     * <p>
     * Getter for the field <code>topStructElement</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.viewer.StructElement} object.
     */
    public StructElement getTopStructElement() {
        try {
            return loadTopStructElement();
        } catch (IndexUnreachableException e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * <p>
     * setTopStructElement.
     * </p>
     *
     * @param topStructElement the topStructElement to set
     */
    public void setTopStructElement(StructElement topStructElement) {
        this.topStructElement = topStructElement;
    }

    @Deprecated
    public StructElement getTopDocument() {
        return getTopStructElement();
    }

    /**
     * <p>
     * Getter for the field <code>currentStructElementIddoc</code>.
     * </p>
     *
     * @return the currentStructElementIddoc
     */
    public long getCurrentStructElementIddoc() {
        return currentStructElementIddoc;
    }

    /**
     * <p>
     * Setter for the field <code>currentStructElementIddoc</code>.
     * </p>
     *
     * @param currentStructElementIddoc the currentStructElementIddoc to set
     */
    public void setCurrentStructElementtIddoc(long currentStructElementIddoc) {
        this.currentStructElementIddoc = currentStructElementIddoc;
    }

    /**
     * <p>
     * Getter for the field <code>currentDocumentIddoc</code>.
     * </p>
     *
     * @return the currentDocumentIddoc
     */
    @Deprecated
    public long getCurrentDocumentIddoc() {
        return currentStructElementIddoc;
    }

    /**
     * <p>
     * Setter for the field <code>currentDocumentIddoc</code>.
     * </p>
     *
     * @param currentDocumentIddoc the currentDocumentIddoc to set
     */
    @Deprecated
    public void setCurrentDocumentIddoc(long currentDocumentIddoc) {
        this.currentStructElementIddoc = currentDocumentIddoc;
    }

    /**
     * <p>
     * Getter for the field <code>currentStructElement</code>.
     * </p>
     *
     * @return the currentStructElement
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public StructElement getCurrentStructElement() throws IndexUnreachableException {
        if (currentStructElement == null || currentStructElement.getLuceneId() != currentStructElementIddoc) {
            logger.trace("Creating new currentDocument from IDDOC {}, old currentDocumentIddoc: {}", currentStructElementIddoc,
                    currentStructElementIddoc);
            currentStructElement = new StructElement(currentStructElementIddoc);
        }
        return currentStructElement;
    }

    /**
     * <p>
     * Setter for the field <code>currentStructElement</code>.
     * </p>
     *
     * @param currentStructElement the currentStructElement to set
     */
    public void setCurrentStructElement(StructElement currentStructElement) {
        this.currentStructElement = currentStructElement;
    }

    @Deprecated
    public StructElement getCurrentDocument() throws IndexUnreachableException {
        return getCurrentStructElement();
    }

    @Deprecated
    public void setCurrentDocument(StructElement currentDocument) {
        setCurrentStructElement(currentDocument);
    }

    /**
     * <p>
     * getCurrentDocumentHierarchy.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public List<StructElementStub> getCurrentDocumentHierarchy() throws IndexUnreachableException {
        if (docHierarchy == null) {
            //            PageType pageType = PageType.viewImage;
            docHierarchy = new LinkedList<>();

            StructElement curDoc = getCurrentStructElement();
            while (curDoc != null) {
                docHierarchy.add(curDoc.createStub());
                curDoc = curDoc.getParent();
            }
            Collections.reverse(docHierarchy);
        }

        logger.trace("docHierarchy size: {}", docHierarchy.size());
        if (!DataManager.getInstance().getConfiguration().getIncludeAnchorInTitleBreadcrumbs() && !docHierarchy.isEmpty()) {
            return docHierarchy.subList(1, docHierarchy.size());
        }
        return docHierarchy;
    }

    /**
     * <p>
     * Getter for the field <code>logId</code>.
     * </p>
     *
     * @return the logId
     */
    public String getLogId() {
        return logId;
    }

    /**
     * <p>
     * Setter for the field <code>logId</code>.
     * </p>
     *
     * @param logId the logId to set
     */
    public void setLogId(String logId) {
        this.logId = logId;
        // Reset the hieararchy list so that a new one is created
        docHierarchy = null;
    }

    /**
     * <p>
     * Getter for the field <code>pageLoader</code>.
     * </p>
     *
     * @return the pageLoader
     */
    public IPageLoader getPageLoader() {
        return pageLoader;

    }

    /**
     * <p>
     * getHtmlHeadDCMetadata.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Deprecated
    public String getHtmlHeadDCMetadata() {
        return getDublinCoreMetaTags();
    }

    /**
     * Generates DC meta tags for the head of a HTML page.
     *
     * @return String with tags
     */
    public String getDublinCoreMetaTags() {
        return MetadataTools.generateDublinCoreMetaTags(this.topStructElement);
    }

    /**
     * <p>
     * getHighwirePressMetaTags.
     * </p>
     *
     * @return String with tags
     */
    public String getHighwirePressMetaTags() {
        try {
            return MetadataTools.generateHighwirePressMetaTags(this.topStructElement, isFilesOnly() ? getAllPages() : null);
        } catch (IndexUnreachableException e) {
            logger.error(e.getMessage(), e);
            return "";
        } catch (ViewerConfigurationException e) {
            logger.error(e.getMessage(), e);
            return "";
        } catch (DAOException e) {
            logger.error(e.getMessage(), e);
            return "";
        } catch (PresentationException e) {
            logger.error(e.getMessage(), e);
            return "";
        }
    }

    /**
     * <p>
     * isHasVersionHistory.
     * </p>
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public boolean isHasVersionHistory() throws PresentationException, IndexUnreachableException {
        if (StringUtils.isEmpty(DataManager.getInstance().getConfiguration().getPreviousVersionIdentifierField())
                && StringUtils.isEmpty(DataManager.getInstance().getConfiguration().getNextVersionIdentifierField())) {
            return false;
        }

        return getVersionHistory().size() > 1;
    }

    /**
     * <p>
     * Getter for the field <code>versionHistory</code>.
     * </p>
     *
     * @should create create history correctly
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public List<String> getVersionHistory() throws PresentationException, IndexUnreachableException {
        if (versionHistory == null) {
            versionHistory = new ArrayList<>();

            String versionLabelField = DataManager.getInstance().getConfiguration().getVersionLabelField();

            {
                String nextVersionIdentifierField = DataManager.getInstance().getConfiguration().getNextVersionIdentifierField();
                if (StringUtils.isNotEmpty(nextVersionIdentifierField)) {
                    List<String> next = new ArrayList<>();
                    String identifier = topStructElement.getMetadataValue(nextVersionIdentifierField);
                    while (identifier != null) {
                        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(SolrConstants.PI + ":" + identifier, null);
                        if (doc != null) {
                            JSONObject jsonObj = new JSONObject();
                            String versionLabel =
                                    versionLabelField != null ? SolrTools.getSingleFieldStringValue(doc, versionLabelField) : null;
                            if (StringUtils.isNotEmpty(versionLabel)) {
                                jsonObj.put("label", versionLabel);
                            }
                            jsonObj.put("id", identifier);
                            if (doc.getFieldValues("MD_YEARPUBLISH") != null) {
                                jsonObj.put("year", doc.getFieldValues("MD_YEARPUBLISH").iterator().next());
                            }
                            jsonObj.put("order", "1"); // "1" means this is a
                                                       // succeeding version
                            next.add(jsonObj.toString());
                            identifier = null;
                            if (doc.getFieldValues(nextVersionIdentifierField) != null) {
                                identifier = (String) doc.getFieldValues(nextVersionIdentifierField).iterator().next();
                            }
                        }
                    }
                    Collections.reverse(next);
                    versionHistory.addAll(next);
                }
            }

            {
                // This version
                JSONObject jsonObj = new JSONObject();
                String versionLabel = versionLabelField != null ? topStructElement.getMetadataValue(versionLabelField) : null;
                if (versionLabel != null) {
                    jsonObj.put("label", versionLabel);
                }
                jsonObj.put("id", getPi());
                jsonObj.put("year", topStructElement.getMetadataValue("MD_YEARPUBLISH"));
                jsonObj.put("order", "0"); // "0" identifies the currently loaded version
                versionHistory.add(jsonObj.toString());
            }

            {
                String prevVersionIdentifierField = DataManager.getInstance().getConfiguration().getPreviousVersionIdentifierField();
                if (StringUtils.isNotEmpty(prevVersionIdentifierField)) {
                    List<String> previous = new ArrayList<>();
                    String identifier = topStructElement.getMetadataValue(prevVersionIdentifierField);
                    while (identifier != null) {
                        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(SolrConstants.PI + ":" + identifier, null);
                        if (doc != null) {
                            JSONObject jsonObj = new JSONObject();
                            String versionLabel =
                                    versionLabelField != null ? SolrTools.getSingleFieldStringValue(doc, versionLabelField) : null;
                            if (StringUtils.isNotEmpty(versionLabel)) {
                                jsonObj.put("label", versionLabel);
                            }
                            jsonObj.put("id", identifier);
                            if (doc.getFieldValues("MD_YEARPUBLISH") != null) {
                                jsonObj.put("year", doc.getFieldValues("MD_YEARPUBLISH").iterator().next());
                            }
                            jsonObj.put("order", "-1"); // "-1" means this is a
                                                        // preceding version
                            previous.add(jsonObj.toString());
                            identifier = null;
                            if (doc.getFieldValues(prevVersionIdentifierField) != null) {
                                identifier = (String) doc.getFieldValues(prevVersionIdentifierField).iterator().next();
                            }
                        } else {
                            //Identifier has no matching document. break while-loop
                            break;
                        }
                    }
                    versionHistory.addAll(previous);
                }
            }
            // Collections.reverse(versionHistory);
        }

        //		logger.trace("Version history size: {}", versionHistory.size());
        return versionHistory;
    }

    /**
     * Returns the ContextObject value for a COinS element (generated using metadata from <code>currentDocument</code>).
     *
     * @return a {@link java.lang.String} object.
     */
    public String getContextObject() {
        if (currentStructElement != null && contextObject == null) {
            try {
                contextObject =
                        currentStructElement.generateContextObject(BeanUtils.getNavigationHelper().getCurrentUrl(),
                                currentStructElement.getTopStruct());
            } catch (PresentationException e) {
                logger.debug("PresentationException thrown here: {}", e.getMessage());
            } catch (IndexUnreachableException e) {
                logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
            }
        }

        return contextObject;
    }

    /**
     * <p>
     * addToTranskribusAction.
     * </p>
     *
     * @param login If true, the user will first be logged into their Transkribus account in the UserBean.
     * @return a {@link java.lang.String} object.
     */
    public String addToTranskribusAction(boolean login) {
        logger.trace("addToTranskribusAction");
        UserBean ub = BeanUtils.getUserBean();
        if (ub == null) {
            logger.error("Could not retrieve UserBean");
            Messages.error("transkribus_recordInjestError");
            return "";
        }

        TranskribusSession session = ub.getUser().getTranskribusSession();
        if (session == null && login) {
            ub.transkribusLoginAction();
            session = ub.getUser().getTranskribusSession();
        }
        if (session == null) {
            Messages.error("transkribus_recordInjestError");
            return "";
        }
        try {
            NavigationHelper nh = BeanUtils.getNavigationHelper();
            String resolverUrlRoot = nh != null ? nh.getApplicationUrl() : "http://viewer.goobi.io/" + "metsresolver?id=";
            TranskribusJob job = TranskribusUtils.ingestRecord(DataManager.getInstance().getConfiguration().getTranskribusRestApiUrl(), session, pi,
                    resolverUrlRoot);
            if (job == null) {
                Messages.error("transkribus_recordInjestError");
                return "";
            }
            Messages.info("transkribus_recordIngestSuccess");
        } catch (IOException | JDOMException e) {
            logger.error(e.getMessage(), e);
            Messages.error("transkribus_recordInjestError");
        } catch (DAOException e) {
            logger.debug("DAOException thrown here");
            logger.error(e.getMessage(), e);
            Messages.error("transkribus_recordInjestError");
        } catch (HTTPException e) {
            if (e.getCode() == 401) {
                ub.getUser().setTranskribusSession(null);
                Messages.error("transkribus_sessionExpired");
            } else {
                logger.error(e.getMessage(), e);
                Messages.error("transkribus_recordInjestError");
            }
        }

        return "";
    }

    /**
     * <p>
     * isRecordAddedToTranskribus.
     * </p>
     *
     * @param session a {@link io.goobi.viewer.model.transkribus.TranskribusSession} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isRecordAddedToTranskribus(TranskribusSession session) throws DAOException {
        if (session == null) {
            return false;
        }
        List<TranskribusJob> jobs = DataManager.getInstance().getDao().getTranskribusJobs(pi, session.getUserId(), null);

        return jobs != null && !jobs.isEmpty();
    }

    /**
     * <p>
     * useTiles.
     * </p>
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public boolean useTiles() throws IndexUnreachableException, DAOException, ViewerConfigurationException {
        PhysicalElement currentPage = getCurrentPage();
        if (currentPage == null) {
            return false;
        }

        return DataManager.getInstance().getConfiguration().useTiles();
    }

    /**
     * <p>
     * useTilesFullscreen.
     * </p>
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public boolean useTilesFullscreen() throws IndexUnreachableException, DAOException, ViewerConfigurationException {
        PhysicalElement currentPage = getCurrentPage();
        if (currentPage == null) {
            return false;
        }

        return DataManager.getInstance().getConfiguration().useTilesFullscreen();
    }

    /**
     * <p>
     * Getter for the field <code>pi</code>.
     * </p>
     *
     * @return the pi
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getPi() throws IndexUnreachableException {
        if (StringUtils.isEmpty(pi)) {
            pi = getCurrentStructElement().getMetadataValue(SolrConstants.PI_TOPSTRUCT);
        }

        return pi;
    }

    /**
     * If the current record is a volume, returns the PI of the anchor record.
     *
     * @return anchor PI if record is volume; null otherwise.
     */
    public String getAnchorPi() {
        if (anchorStructElement != null) {
            return anchorStructElement.getMetadataValue(SolrConstants.PI);
        }

        return null;
    }

    /**
     * <p>
     * Getter for the field <code>mainMimeType</code>.
     * </p>
     *
     * @return the mainMimeType
     */
    public String getMainMimeType() {
        return mainMimeType;
    }

    /**
     * <p>
     * togglePageOrientation.
     * </p>
     */
    public void togglePageOrientation() {
        this.firstPageOrientation = this.firstPageOrientation.opposite();
    }

    /**
     * <p>
     * Setter for the field <code>doublePageMode</code>.
     * </p>
     *
     * @param doublePageMode the doublePageMode to set
     */
    public void setDoublePageMode(boolean doublePageMode) {
        this.doublePageMode = doublePageMode;
    }

    /**
     * <p>
     * isDoublePageMode.
     * </p>
     *
     * @return the doublePageMode
     */
    public boolean isDoublePageMode() {
        return doublePageMode;
    }

    /**
     * <p>
     * Getter for the field <code>firstPdfPage</code>.
     * </p>
     *
     * @return the firstPdfPage
     */
    public String getFirstPdfPage() {
        return String.valueOf(firstPdfPage);
    }

    /**
     * <p>
     * Setter for the field <code>firstPdfPage</code>.
     * </p>
     *
     * @param firstPdfPage the firstPdfPage to set
     */
    public void setFirstPdfPage(String firstPdfPage) {
        this.firstPdfPage = Integer.valueOf(firstPdfPage);
    }

    /**
     * <p>
     * Getter for the field <code>lastPdfPage</code>.
     * </p>
     *
     * @return the lastPdfPage
     */
    public String getLastPdfPage() {
        return String.valueOf(lastPdfPage);
    }

    /**
     * <p>
     * Setter for the field <code>lastPdfPage</code>.
     * </p>
     *
     * @param lastPdfPage the lastPdfPage to set
     */
    public void setLastPdfPage(String lastPdfPage) {
        logger.trace("setLastPdfPage: {}", lastPdfPage);
        if (lastPdfPage != null) {
            this.lastPdfPage = Integer.valueOf(lastPdfPage);
        }
    }

    /**
     * <p>
     * Getter for the field <code>calendarView</code>.
     * </p>
     *
     * @return the calendarView
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public CalendarView getCalendarView() throws IndexUnreachableException, PresentationException {
        if (calendarView == null) {
            calendarView = createCalendarView();
        }
        return calendarView;
    }

    /**
     * <p>
     * Getter for the field <code>firstPageOrientation</code>.
     * </p>
     *
     * @return the firstPageOrientation
     */
    public PageOrientation getFirstPageOrientation() {
        if (getCurrentPage().isFlipRectoVerso()) {
            logger.trace("page {} is flipped", getCurrentPage().getOrder());
            return firstPageOrientation.opposite();
        }
        return firstPageOrientation;
    }

    /**
     * <p>
     * Setter for the field <code>firstPageOrientation</code>.
     * </p>
     *
     * @param firstPageOrientation the firstPageOrientation to set
     */
    public void setFirstPageOrientation(PageOrientation firstPageOrientation) {
        this.firstPageOrientation = firstPageOrientation;
    }

    /**
     * <p>
     * getCurrentPageSourceIndex.
     * </p>
     *
     * @return 1 if we are in double page mode and the current page is the right page. 0 otherwise
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public int getCurrentPageSourceIndex() throws IndexUnreachableException, DAOException {
        if (!isDoublePageMode()) {
            return 0;
        }

        PhysicalElement currentRightPage = getCurrentRightPage().orElse(null);
        if (currentRightPage != null) {
            return currentRightPage.equals(getCurrentPage()) ? 1 : 0;
        }

        return 0;
    }

    /**
     * <p>
     * getTopDocumentTitle.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getTopDocumentTitle() {
        return getDocumentTitle(this.topStructElement);
    }

    /**
     * <p>
     * getDocumentTitle.
     * </p>
     *
     * @param document a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @return a {@link java.lang.String} object.
     */
    public String getDocumentTitle(StructElement document) {
        if (document == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        switch (document.docStructType) {
            case "Comment":
                sb.append("\"").append(document.getMetadataValue(SolrConstants.TITLE)).append("\"");
                if (StringUtils.isNotBlank(document.getMetadataValue("MD_AUTHOR"))) {
                    sb.append(" von ").append(document.getMetadataValue("MD_AUTHOR"));
                }
                if (StringUtils.isNotBlank(document.getMetadataValue("MD_YEARPUBLISH"))) {
                    sb.append(" (").append(document.getMetadataValue("MD_YEARPUBLISH")).append(")");
                }
                break;
            case "FormationHistory":
                sb.append("\"").append(document.getMetadataValue(SolrConstants.TITLE)).append("\"");
                //TODO: Add Einsatzland z.b.: (Deutschland)
                if (StringUtils.isNotBlank(document.getMetadataValue("MD_AUTHOR"))) {
                    sb.append(" von ").append(document.getMetadataValue("MD_AUTHOR"));
                }
                if (StringUtils.isNotBlank(document.getMetadataValue("MD_YEARPUBLISH"))) {
                    sb.append(" (").append(document.getMetadataValue("MD_YEARPUBLISH")).append(")");
                }
                break;
            case "Source":
            default:
                sb.append(document.getDisplayLabel());
        }

        return sb.toString();
    }

    /**
     * <p>
     * Setter for the field <code>pageLoader</code>.
     * </p>
     *
     * @param loader a {@link io.goobi.viewer.model.viewer.pageloader.IPageLoader} object.
     */
    public void setPageLoader(IPageLoader loader) {
        this.pageLoader = loader;

    }

    /**
     * <p>
     * getCiteLinkWork.
     * </p>
     *
     * @return A persistent link to the current work
     *
     *         TODO: additional urn-resolving logic
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public String getCiteLinkWork() throws IndexUnreachableException, DAOException, PresentationException {
        if (topStructElement == null) {
            return "";
        }

        String customPURL = topStructElement.getMetadataValue("MD_PURL");
        if (StringUtils.isNotEmpty(customPURL)) {
            return customPURL;
        } else if (StringUtils.isNotBlank(topStructElement.getMetadataValue(SolrConstants.URN))) {
            String urn = topStructElement.getMetadataValue(SolrConstants.URN);
            return getPersistentUrl(urn);
        } else {
            StringBuilder url = new StringBuilder();
            boolean anchorOrGroup = topStructElement.isAnchor() || topStructElement.isGroup();
            PageType pageType = PageType.determinePageType(topStructElement.getDocStructType(), null, anchorOrGroup, isHasPages(), false);
            if (pageType == null) {
                if (isHasPages()) {
                    pageType = PageType.viewObject;
                } else {
                    pageType = PageType.viewMetadata;
                }
            }
            url.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext());
            url.append('/').append(pageType.getName()).append('/').append(getPi()).append('/');
            if (getRepresentativePage() != null) {
                url.append(getRepresentativePage().getOrder()).append('/');
            }
            return url.toString();
        }
    }

    /**
     * <p>
     * isDisplayCiteLinkWork.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplayCiteLinkWork() {
        return topStructElement != null;
    }

    /**
     * <p>
     * getCiteLinkPage.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getCiteLinkPage() throws IndexUnreachableException, DAOException {
        PhysicalElement currentPage = getCurrentPage();
        if (currentPage == null) {
            return "";
        }

        String urn = currentPage.getUrn();
        return getPersistentUrl(urn);
    }

    /**
     * <p>
     * isDisplayCiteLinkPage.
     * </p>
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isDisplayCiteLinkPage() throws IndexUnreachableException, DAOException {
        return getCurrentPage() != null;
    }

    /**
     * 
     * @return
     * @throws IOException
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public String getCitationStringHtml() throws IOException, IndexUnreachableException, PresentationException {
        return getCitationString("html");
    }

    /**
     * 
     * @return
     * @throws IOException
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public String getCitationStringPlain() throws IOException, IndexUnreachableException, PresentationException {
        return getCitationString("text");
    }

    /**
     * 
     * @param outputFormat Output format (html or text)
     * @return Generated citation string for the selected style
     * @throws IOException
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @should return apa html citation correctly
     * @should return apa html plaintext correctly
     */
    String getCitationString(String outputFormat) throws IOException, IndexUnreachableException, PresentationException {
        if (StringUtils.isEmpty(citationStyle)) {
            List<String> availableStyles = DataManager.getInstance().getConfiguration().getSidebarWidgetUsageCitationStyles();
            if (availableStyles.isEmpty()) {
                return "";
            }
            citationStyle = availableStyles.get(0);
        }

        if (citationProcessorWrapper == null) {
            citationProcessorWrapper = new CitationProcessorWrapper();
        }
        CSL processor = citationProcessorWrapper.getCitationProcessor(citationStyle);
        Metadata md = DataManager.getInstance().getConfiguration().getSidebarWidgetUsageCitationSource();
        md.populate(topStructElement, BeanUtils.getLocale());
        for (MetadataValue val : md.getValues()) {
            if (!val.getCitationValues().isEmpty()) {
                Citation citation = new Citation(pi, processor, citationProcessorWrapper.getCitationItemDataProvider(),
                        CitationTools.getCSLTypeForDocstrct(topStructElement.getDocStructType()), val.getCitationValues());
                String ret = citation.getCitationString(outputFormat);
                // logger.trace("citation: {}", ret);
                return ret;
            }
        }

        return "";
    }

    /**
     * @return the citationStyle
     */
    public String getCitationStyle() {
        return citationStyle;
    }

    /**
     * @param citationStyle the citationStyle to set
     */
    public void setCitationStyle(String citationStyle) {
        this.citationStyle = citationStyle;
    }

    /**
     * @return the citationProcessorWrapper
     */
    public CitationProcessorWrapper getCitationProcessorWrapper() {
        return citationProcessorWrapper;
    }

    /**
     * @param citationProcessorWrapper the citationProcessorWrapper to set
     */
    public void setCitationProcessorWrapper(CitationProcessorWrapper citationProcessorWrapper) {
        this.citationProcessorWrapper = citationProcessorWrapper;
    }

    /**
     * 
     * @return
     */
    public String getArchiveEntryIdentifier() {
        if (topStructElement == null) {
            return null;
        }

        // logger.trace("getArchiveEntryIdentifier: {}", topDocument.getMetadataValue(SolrConstants.ARCHIVE_ENTRY_ID));
        return topStructElement.getMetadataValue(SolrConstants.ARCHIVE_ENTRY_ID);
    }

    /**
     * Creates an instance of ViewManager loaded with the record with the given identifier.
     * 
     * @param pi Record identifier
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws ViewerConfigurationException
     * @throws DAOException
     * @throws RecordNotFoundException
     */
    public static ViewManager createViewManager(String pi)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException, RecordNotFoundException {
        if (pi == null) {
            throw new IllegalArgumentException("pi may not be null");
        }

        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(SolrConstants.PI + ":" + pi, null);
        if (doc == null) {
            throw new RecordNotFoundException(pi);
        }

        long iddoc = Long.valueOf((String) doc.getFieldValue(SolrConstants.IDDOC));
        StructElement topDocument = new StructElement(iddoc, doc);
        ViewManager ret = new ViewManager(topDocument, AbstractPageLoader.create(topDocument), iddoc, null, null, null);

        return ret;
    }
}
