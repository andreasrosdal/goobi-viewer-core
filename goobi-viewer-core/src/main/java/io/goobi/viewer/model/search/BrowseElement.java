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
package io.goobi.viewer.model.search;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.imaging.IIIFUrlHandler;
import io.goobi.viewer.controller.imaging.ThumbnailHandler;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.NavigationHelper;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.crowdsourcing.DisplayUserGeneratedContent;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.metadata.MetadataParameter;
import io.goobi.viewer.model.metadata.MetadataParameter.MetadataParameterType;
import io.goobi.viewer.model.metadata.MetadataTools;
import io.goobi.viewer.model.metadata.MetadataValue;
import io.goobi.viewer.model.viewer.EventElement;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.StructElementStub;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;
import io.goobi.viewer.solr.SolrConstants.MetadataGroupType;

/**
 * Representation of a search hit. TODO integrate into SearchHit
 */
public class BrowseElement implements Serializable {

    private static final long serialVersionUID = 6621169815560734613L;

    private static final Logger logger = LogManager.getLogger(BrowseElement.class);

    @JsonIgnore
    private String fulltext;
    private String fulltextForHtml;
    /** Element label (usually the title). */
    private final IMetadataValue label;
    /** Truncated and highlighted variant of the label. */
    private IMetadataValue labelShort = new SimpleMetadataValue();
    /** Type of the index document. */
    private DocType docType;
    /** Type of grouped metadata document (person, etc.) */
    @JsonIgnore
    private MetadataGroupType metadataGroupType = null;
    /** Name of the grouped metadata field. */
    @JsonIgnore
    private String originalFieldName = null;
    /** Type of the docstruct. */
    private String docStructType;
    private long iddoc;
    private String thumbnailUrl;
    //    private boolean thumbnailAccessDenied = false;
    private int imageNo;
    @JsonIgnore
    private String volumeNo = null;
    /** StructElementStubs for hierarchy URLs. */
    @JsonIgnore
    private final List<StructElementStub> structElements = new ArrayList<>();
    @JsonIgnore
    private List<EventElement> events;
    @JsonIgnore
    private boolean anchor = false;
    @JsonIgnore
    private boolean hasImages = false;
    @JsonIgnore
    private boolean hasMedia = false;
    @JsonIgnore
    private boolean showThumbnail = false;
    @JsonIgnore
    private long numVolumes = 0;
    private String pi;
    private String logId;
    @JsonIgnore
    private NavigationHelper navigationHelper;
    @JsonIgnore
    private List<Metadata> metadataList = null;
    @JsonIgnore
    private final Set<String> existingMetadataFields = new HashSet<>();
    /**
     * List of just the metadata fields that were added because they contained search terms (for use where not the entire metadata list is desired).
     */
    @JsonIgnore
    private final List<Metadata> additionalMetadataList = new ArrayList<>();
    @JsonIgnore
    private String mimeType = "";
    @JsonIgnore
    private String contextObject;
    private String url;
    @JsonIgnore
    private String sidebarPrevUrl;
    @JsonIgnore
    private String sidebarNextUrl;
    @JsonIgnore
    private final Locale locale;
    @JsonIgnore
    private final String dataRepository;

    private List<String> recordLanguages;

    /**
     * Constructor for unit tests and special instances.
     *
     * @param pi
     * @param imageNo
     * @param label
     * @param fulltext
     * @param locale
     * @param dataRepository
     * @param url Injected URL, overrides URL generation
     *
     * @should build overview page url correctly
     */
    BrowseElement(String pi, int imageNo, String label, String fulltext, Locale locale, String dataRepository, String url) {
        this.pi = pi;
        this.imageNo = imageNo;
        this.label = new SimpleMetadataValue(label);
        this.fulltext = fulltext;
        this.locale = locale;
        this.metadataList = new ArrayList<>();
        this.url = url;
        if (this.url == null) {
            this.url = generateUrl();
        }
        this.dataRepository = dataRepository;
    }

    /**
     * Constructor.
     *
     * @param structElement {@link StructElement}
     * @param metadataList
     * @param locale
     * @param fulltext
     * @param
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ViewerConfigurationException
     */
    BrowseElement(StructElement structElement, List<Metadata> metadataList, Locale locale, String fulltext, Map<String, Set<String>> searchTerms,
            ThumbnailHandler thumbs) throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        this.metadataList = metadataList;
        if (this.metadataList == null) {
            this.metadataList = new ArrayList<>();
        }
        this.locale = locale;
        this.fulltext = fulltext;

        // Collect the docstruct hierarchy
        StructElement anchorStructElement = null;
        StructElement topStructElement = null; // this can be null in unit tests
        StructElement tempElement = structElement;
        while (tempElement != null && !tempElement.isWork()) {
            structElements.add(tempElement.createStub());
            tempElement = tempElement.getParent();
        }
        // Add topstruct to the hierarchy
        if (tempElement != null) {
            structElements.add(tempElement.createStub());
            topStructElement = tempElement;
        }

        // Determine Solr document type. Must happen before certain things, such as label generation.
        docType = DocType.getByName(structElement.getMetadataValue(SolrConstants.DOCTYPE));
        if (DocType.METADATA.equals(docType)) {
            metadataGroupType = MetadataGroupType.getByName(structElement.getMetadataValue(SolrConstants.METADATATYPE));
            // The LABEL field in grouped metadata docs contains the name of the field defined in the indexed configuration
            originalFieldName = structElement.getMetadataValue(SolrConstants.LABEL);
        }

        // If the topstruct is a volume of any kind or a subelement, add the anchor and volume labels to
        if (!structElement.isAnchor() && topStructElement != null) {
            // Add anchor label to volumes
            if (!structElement.isAnchor()) {
                anchorStructElement = topStructElement.getParent();
                if (anchorStructElement != null) {
                    // Add anchor to the docstruct hierarchy
                    structElements.add(anchorStructElement.createStub());
                }
            }
        }

        // Populate metadata
        if (!this.metadataList.isEmpty()) {
            int length = DataManager.getInstance().getConfiguration().getSearchHitMetadataValueLength();
            int number = DataManager.getInstance().getConfiguration().getSearchHitMetadataValueNumber();
            populateMetadataList(structElement, topStructElement, anchorStructElement, searchTerms, length, number, locale);

            // Add event metadata for LIDO records
            if (topStructElement != null && topStructElement.isLidoRecord()) {
                populateEvents(topStructElement, searchTerms);
            }
        }

        if (navigationHelper == null) {
            try {
                navigationHelper = BeanUtils.getNavigationHelper();
            } catch (NullPointerException e) {
                // logger.trace("No navigationHelper available");
            }
        }

        anchor = structElement.isAnchor();
        numVolumes = structElement.getNumVolumes();
        docStructType = structElement.getDocStructType();
        dataRepository = structElement.getMetadataValue(SolrConstants.DATAREPOSITORY);
        label = createMultiLanguageLabel(structElement);

        pi = structElement.getPi();
        if (pi == null) {
            logger.warn("Index document {} has no PI_TOPSTRUCT field. Please re-index.", structElement.getLuceneId());
            return;
        }
        pi = StringTools.intern(pi);
        iddoc = structElement.getLuceneId();
        logId = StringTools.intern(structElement.getMetadataValue(SolrConstants.LOGID));
        volumeNo = structElement.getVolumeNo();
        if (StringUtils.isEmpty(volumeNo)) {
            volumeNo = structElement.getVolumeNoSort();
        }

        // generate thumbnail url
        String filename = structElement.getMetadataValue(SolrConstants.FILENAME);
        if (StringUtils.isEmpty(filename)) {
            filename = structElement.getMetadataValue(SolrConstants.THUMBNAIL);
        }
        if (StringUtils.isEmpty(filename)) {
            filename = structElement.getFirstPageFieldValue(SolrConstants.FILENAME_HTML_SANDBOXED);
        }
        if (anchor) {
            mimeType = structElement.getFirstVolumeFieldValue(SolrConstants.MIMETYPE);
        } else {
            mimeType = structElement.getMetadataValue(SolrConstants.MIMETYPE);
        }
        if (mimeType == null && filename != null) {
            mimeType = getMimeTypeFromExtension(filename);
        }
        if (mimeType == null) {
            mimeType = "";
        }

        String imageNoStr = structElement.getMetadataValue(SolrConstants.ORDER);
        if (StringUtils.isNotEmpty(imageNoStr)) {
            // ORDER field exists (page doc)
            try {
                imageNo = Integer.parseInt(imageNoStr);
            } catch (NumberFormatException e) {
                logger.debug("No valid image number found for IDDOC {}, make a 1 here", iddoc);
                imageNo = 0;
            }
        } else {
            // Use FILENAME (page) or THUMBPAGENO (docstruct doc)
            imageNoStr = structElement.getMetadataValue(SolrConstants.FILENAME);
            if (StringUtils.isNotEmpty(imageNoStr)) {
                imageNoStr = imageNoStr.substring(0, imageNoStr.indexOf('.'));
            } else {
                imageNoStr = structElement.getMetadataValue(SolrConstants.THUMBPAGENO);
            }
            if (StringUtils.isNotBlank(imageNoStr)) {
                try {
                    imageNo = Integer.parseInt(imageNoStr);
                } catch (NumberFormatException e) {
                    logger.debug("No valid image number found for IDDOC {}, make a 1 here", iddoc);
                    imageNo = 0;
                }
            } else {
                imageNo = 1;
            }
        }

        // Thumbnail
        if (thumbs != null) {
            String sbThumbnailUrl = thumbs.getThumbnailUrl(structElement);
            if (sbThumbnailUrl != null && sbThumbnailUrl.length() > 0) {
                thumbnailUrl = StringTools.intern(sbThumbnailUrl.toString());
            }
        }

        //check if we have images
        hasImages = !isAnchor() && (this.mimeType.startsWith("image") || structElement.isHasImages());

        //..or if we have video or audio
        hasMedia = !hasImages && !isAnchor()
                && (this.mimeType.startsWith("audio") || this.mimeType.startsWith("video") || this.mimeType.startsWith("application")
                        || this.mimeType.startsWith("text")/*sandboxed*/);

        showThumbnail = hasImages || hasMedia || isAnchor();

        //record languages
        this.recordLanguages = structElement.getMetadataValues(SolrConstants.LANGUAGE);

        this.url = generateUrl();
        sidebarPrevUrl = generateSidebarUrl("prevHit");
        sidebarNextUrl = generateSidebarUrl("nextHit");

        Collections.reverse(structElements);
    }

    /**
     *
     * @param structElement
     * @param topStructElement
     * @param anchorStructElement
     * @param searchTerms
     * @param length
     * @param number
     * @param locale
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    void populateMetadataList(StructElement structElement, StructElement topStructElement, StructElement anchorStructElement,
            Map<String, Set<String>> searchTerms, int length, int number, Locale locale) throws IndexUnreachableException, PresentationException {
        for (Metadata md : this.metadataList) {
            for (MetadataParameter param : md.getParams()) {
                StructElement elementToUse = structElement;
                if (StringUtils.isNotEmpty(param.getSource())) {
                    StructElement tempElement = structElement;
                    while (tempElement != null) {
                        if (param.getSource().equals(tempElement.getDocStructType())) {
                            elementToUse = tempElement;
                            break;
                        }
                        tempElement = tempElement.getParent();
                    }
                } else if (MetadataParameterType.TOPSTRUCTFIELD.equals(param.getType()) && topStructElement != null) {
                    // Use topstruct value, if the parameter has the type "topstructfield"
                    elementToUse = topStructElement;
                } else if (MetadataParameterType.ANCHORFIELD.equals(param.getType())) {
                    // Use anchor value, if the parameter has the type "anchorfield"
                    if (anchorStructElement != null) {
                        elementToUse = anchorStructElement;
                    } else {
                        // Add empty parameter if there is no anchor
                        md.setParamValue(0, md.getParams().indexOf(param), Collections.singletonList(""), null, null, null, null, locale);
                        continue;
                    }
                }
                int count = 0;
                List<String> metadataValues = elementToUse.getMetadataValues(param.getKey());
                // If the current element does not contain metadata values, look in the topstruct
                if (metadataValues.isEmpty()) {
                    if (topStructElement != null && !topStructElement.equals(elementToUse)
                            && !MetadataParameterType.ANCHORFIELD.equals(param.getType()) && param.isTopstructValueFallback()) {
                        metadataValues = topStructElement.getMetadataValues(param.getKey());
                        // logger.debug("Checking topstruct metadata: " + topStructElement.getDocStruct());
                    } else {
                        md.setParamValue(count, md.getParams().indexOf(param), Collections.singletonList(""), null, null, null, null, locale);
                        count++;
                    }
                }
                // Set actual values
                for (String value : metadataValues) {
                    if (count >= md.getNumber() && md.getNumber() != -1 || count >= number) {
                        break;
                    }
                    // Apply replace rules
                    if (!param.getReplaceRules().isEmpty()) {
                        value = MetadataTools.applyReplaceRules(value, param.getReplaceRules(), topStructElement.getPi());
                    }
                    // Truncate long values
                    if (length > 0 && value.length() > length) {
                        value = new StringBuilder(value.substring(0, length - 3)).append("...").toString();
                    }
                    // Add highlighting
                    if (searchTerms != null) {
                        if (searchTerms.get(md.getLabel()) != null) {
                            value = SearchHelper.applyHighlightingToPhrase(value, searchTerms.get(md.getLabel()));
                        } else if (md.getLabel().startsWith("MD_SHELFMARK") && searchTerms.get("MD_SHELFMARKSEARCH") != null) {
                            value = SearchHelper.applyHighlightingToPhrase(value, searchTerms.get("MD_SHELFMARKSEARCH"));
                        }
                        if (searchTerms.get(SolrConstants.DEFAULT) != null) {
                            value = SearchHelper.applyHighlightingToPhrase(value, searchTerms.get(SolrConstants.DEFAULT));
                        }
                    }
                    md.setParamValue(count, md.getParams().indexOf(param), Collections.singletonList(StringTools.intern(value)), null,
                            param.isAddUrl() ? elementToUse.getUrl() : null, null, null, locale);
                    this.existingMetadataFields.add(md.getLabel());
                    count++;
                }
            }
        }
    }

    /**
     * Looks up LIDO events and search hit metadata for the given record topstruct element. Applies search hit value highlighting, if search terms are
     * provided.
     *
     * @param topStructElement Top structure element of the LIDO record
     * @param searchTerms Map containing all generated search terms
     * @throws IndexUnreachableException
     */
    private void populateEvents(StructElement topStructElement, Map<String, Set<String>> searchTerms) throws IndexUnreachableException {
        if (topStructElement == null || !topStructElement.isLidoRecord()) {
            return;
        }
        logger.trace("populateEvents: {}, {}", topStructElement.getLabel(), searchTerms);

        this.events = topStructElement.generateEventElements(locale, true);
        if (this.events.isEmpty()) {
            return;
        }

        Collections.sort(this.events);

        // Value highlighting
        if (searchTerms == null) {
            return;
        }
        for (EventElement event : events) {
            for (Metadata md : event.getSearchHitMetadata()) {
                for (MetadataParameter param : md.getParams()) {
                    for (MetadataValue mdValue : md.getValues()) {
                        if (searchTerms.get(md.getLabel()) != null) {
                            mdValue.applyHighlightingToParamValue(md.getParams().indexOf(param), searchTerms.get(md.getLabel()));
                        } else if (searchTerms.get(SolrConstants.DEFAULT) != null) {
                            mdValue.applyHighlightingToParamValue(md.getParams().indexOf(param), searchTerms.get(SolrConstants.DEFAULT));
                        }
                    }
                }
            }
        }
    }

    /**
     * <p>
     * createMultiLanguageLabel.
     * </p>
     *
     * @param structElement a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @return a {@link de.intranda.metadata.multilanguage.IMetadataValue} object.
     */
    public IMetadataValue createMultiLanguageLabel(StructElement structElement) {
        MultiLanguageMetadataValue value = new MultiLanguageMetadataValue();
        for (Locale locale : ViewerResourceBundle.getAllLocales()) {
            StringBuilder sbLabel = new StringBuilder(generateLabel(structElement, locale));
            String subtitle = structElement.getMetadataValueForLanguage(SolrConstants.SUBTITLE, locale.getLanguage());
            if (StringUtils.isNotEmpty(subtitle)) {
                sbLabel.append(" : ").append(subtitle);
            }
            value.setValue(sbLabel.toString(), locale);
        }

        return value;
    }

    /**
     *
     * @param structElement
     * @param sortFields If manual sorting was used, display the sorting fields
     * @param ignoreFields Fields to be skipped
     * @should add sort fields correctly
     * @should not add fields on ignore list
     * @should not add fields already in the list
     */
    void addSortFieldsToMetadata(StructElement structElement, List<StringPair> sortFields, Set<String> ignoreFields) {
        if (sortFields == null || sortFields.isEmpty()) {
            return;
        }

        for (StringPair sortField : sortFields) {
            // Skip fields that are in the ignore list
            if (ignoreFields != null && ignoreFields.contains(sortField.getOne())) {
                continue;
            }
            // Title is already in the header
            if ("SORT_TITLE".equals(sortField.getOne())) {
                continue;
            }
            // Skip fields that are already in the list
            boolean skip = false;

            for (Metadata md : metadataList) {
                if (md.getLabel().equals(sortField.getOne().replace("SORT_", "MD_"))) {
                    skip = true;
                    break;
                }
            }
            if (skip) {
                continue;
            }
            // Look up the exact field name in the Solr doc and add its values that contain any of the terms for that field
            if (!skip && structElement.getMetadataFields().containsKey(sortField.getOne())) {
                List<String> fieldValues = structElement.getMetadataFields().get(sortField.getOne());
                for (String fieldValue : fieldValues) {
                    MetadataParameterType type;
                    switch (sortField.getOne()) {
                        case SolrConstants.DATECREATED:
                        case SolrConstants.DATEINDEXED:
                        case SolrConstants.DATEUPDATED:
                            type = MetadataParameterType.MILLISFIELD;
                            break;
                        default:
                            type = MetadataParameterType.FIELD;
                            break;
                    }

                    Metadata md = new Metadata(String.valueOf(structElement.getLuceneId()), sortField.getOne(), "",
                            new MetadataParameter().setType(type), fieldValue, locale);

                    metadataList.add(md);
                    additionalMetadataList.add(md);
                }
            }
        }
    }

    /**
     * Adds metadata fields that aren't configured in <code>metadataList</code> but match give search terms. Applies highlighting to matched terms.
     *
     * @param structElement
     * @param searchTerms
     * @param ignoreFields Fields to be skipped
     * @param translateFields Fields to be translated
     * @param oneLineFields Fields to be added as a single string containing all values
     * @should add metadata fields that match search terms
     * @should not add duplicates from default terms
     * @should not add duplicates from explicit terms
     * @should not add ignored fields
     * @should translate configured field values correctly
     * @should write one line fields into a single string
     */
    void addAdditionalMetadataContainingSearchTerms(StructElement structElement, Map<String, Set<String>> searchTerms,
            Set<String> ignoreFields, Set<String> translateFields, Set<String> oneLineFields) {
        // logger.trace("addAdditionalMetadataContainingSearchTerms");

        if (searchTerms == null) {
            return;
        }
        for (String termsFieldName : searchTerms.keySet()) {
            // Skip fields that are in the ignore list
            if (ignoreFields != null && ignoreFields.contains(termsFieldName)) {
                continue;
            }
            // Skip fields that are already in the list
            boolean skip = false;
            for (Metadata md : metadataList) {
                if (md.getLabel().equals(termsFieldName)) {
                    continue;
                }
            }
            if (skip) {
                continue;
            }
            switch (termsFieldName) {
                case SolrConstants.DEFAULT:
                    // If searching in DEFAULT, add all fields that contain any of the terms (instead of DEFAULT)
                    for (String docFieldName : structElement.getMetadataFields().keySet()) {
                        // Skip fields that are in the ignore list
                        if (ignoreFields != null && ignoreFields.contains(docFieldName)) {
                            continue;
                        }
                        if (!docFieldName.startsWith("MD_") || docFieldName.endsWith(SolrConstants.SUFFIX_UNTOKENIZED)) {
                            continue;
                        }
                        // Skip fields that are already in the list
                        for (Metadata md : metadataList) {
                            if (md.getLabel().equals(docFieldName)) {
                                skip = true;
                                break;
                            }
                        }
                        if (skip) {
                            skip = false;
                            continue;
                        }
                        List<String> fieldValues = structElement.getMetadataFields().get(docFieldName);

                        if (oneLineFields != null && oneLineFields.contains(docFieldName)) {
                            // All values into a single field value
                            StringBuilder sb = new StringBuilder();
                            for (String fieldValue : fieldValues) {
                                // Skip values that are equal to the hit label
                                Optional<String> labelValue = label.getValue();
                                if (labelValue.isPresent() && fieldValue.equals(labelValue.get())) {
                                    continue;
                                }
                                String highlightedValue = SearchHelper.applyHighlightingToPhrase(fieldValue, searchTerms.get(termsFieldName));
                                if (!highlightedValue.equals(fieldValue)) {
                                    // Translate values for certain fields, keeping the highlighting
                                    if (translateFields != null && (translateFields.contains(docFieldName)
                                            || translateFields.contains(SearchHelper.adaptField(docFieldName, null)))) {
                                        String translatedValue = ViewerResourceBundle.getTranslation(fieldValue, locale);
                                        highlightedValue = highlightedValue.replaceAll("(\\W)(" + Pattern.quote(fieldValue) + ")(\\W)",
                                                "$1" + translatedValue + "$3");
                                    }
                                    highlightedValue = SearchHelper.replaceHighlightingPlaceholders(highlightedValue);
                                    if (sb.length() > 0) {
                                        sb.append(", ");
                                    }
                                    sb.append(highlightedValue);
                                }
                            }
                            if (sb.length() > 0) {
                                metadataList.add(new Metadata(String.valueOf(structElement.getLuceneId()), docFieldName, "", sb.toString()));
                                additionalMetadataList
                                        .add(new Metadata(String.valueOf(structElement.getLuceneId()), docFieldName, "", sb.toString()));
                                existingMetadataFields.add(docFieldName);
                                logger.trace("added existing field: {}", docFieldName);
                            }
                        } else {
                            for (String fieldValue : fieldValues) {
                                // Skip values that are equal to the hit label
                                Optional<String> labelValue = label.getValue();
                                if (labelValue.isPresent() && fieldValue.equals(labelValue.get())) {
                                    continue;
                                }
                                String highlightedValue = SearchHelper.applyHighlightingToPhrase(fieldValue, searchTerms.get(termsFieldName));
                                if (!highlightedValue.equals(fieldValue)) {
                                    // Translate values for certain fields, keeping the highlighting
                                    if (translateFields != null && (translateFields.contains(termsFieldName)
                                            || translateFields.contains(SearchHelper.adaptField(termsFieldName, null)))) {
                                        String translatedValue = ViewerResourceBundle.getTranslation(fieldValue, locale);
                                        highlightedValue = highlightedValue.replaceAll("(\\W)(" + Pattern.quote(fieldValue) + ")(\\W)",
                                                "$1" + translatedValue + "$3");
                                    }
                                    highlightedValue = SearchHelper.replaceHighlightingPlaceholders(highlightedValue);
                                    metadataList.add(new Metadata(String.valueOf(structElement.getLuceneId()), docFieldName, "", highlightedValue));
                                    additionalMetadataList
                                            .add(new Metadata(String.valueOf(structElement.getLuceneId()), docFieldName, "", highlightedValue));
                                    existingMetadataFields.add(docFieldName);
                                    logger.trace("added existing field: {}", docFieldName);
                                }
                            }
                        }
                    }
                    break;
                default:
                    // Skip fields that are already in the list
                    for (Metadata md : metadataList) {
                        if (md.getLabel().equals(termsFieldName)) {
                            skip = true;
                            break;
                        }
                    }
                    // Look up the exact field name in the Solr doc and add its values that contain any of the terms for that field
                    if (!skip && structElement.getMetadataFields().containsKey(termsFieldName)) {
                        List<String> fieldValues = structElement.getMetadataFields().get(termsFieldName);
                        if (oneLineFields != null && oneLineFields.contains(termsFieldName)) {
                            // All values into a single field value
                            StringBuilder sb = new StringBuilder();
                            for (String fieldValue : fieldValues) {
                                String highlightedValue = SearchHelper.applyHighlightingToPhrase(fieldValue, searchTerms.get(termsFieldName));
                                if (!highlightedValue.equals(fieldValue)) {
                                    // Translate values for certain fields, keeping the highlighting
                                    if (translateFields != null && (translateFields.contains(termsFieldName)
                                            || translateFields.contains(SearchHelper.adaptField(termsFieldName, null)))) {
                                        String translatedValue = ViewerResourceBundle.getTranslation(fieldValue, locale);
                                        highlightedValue = highlightedValue.replaceAll("(\\W)(" + Pattern.quote(fieldValue) + ")(\\W)",
                                                "$1" + translatedValue + "$3");
                                    }
                                    highlightedValue = SearchHelper.replaceHighlightingPlaceholders(highlightedValue);
                                    if (sb.length() > 0) {
                                        sb.append(", ");
                                    }
                                    sb.append(highlightedValue);
                                }
                            }
                            if (sb.length() > 0) {
                                metadataList.add(new Metadata(String.valueOf(structElement.getLuceneId()), termsFieldName, "", sb.toString()));
                                additionalMetadataList
                                        .add(new Metadata(String.valueOf(structElement.getLuceneId()), termsFieldName, "", sb.toString()));
                                existingMetadataFields.add(termsFieldName);
                            }
                        } else {
                            for (String fieldValue : fieldValues) {
                                String highlightedValue = SearchHelper.applyHighlightingToPhrase(fieldValue, searchTerms.get(termsFieldName));
                                if (!highlightedValue.equals(fieldValue)) {
                                    // Translate values for certain fields, keeping the highlighting
                                    if (translateFields != null && (translateFields.contains(termsFieldName)
                                            || translateFields.contains(SearchHelper.adaptField(termsFieldName, null)))) {
                                        String translatedValue = ViewerResourceBundle.getTranslation(fieldValue, locale);
                                        highlightedValue = highlightedValue.replaceAll("(\\W)(" + Pattern.quote(fieldValue) + ")(\\W)",
                                                "$1" + translatedValue + "$3");
                                    }
                                    highlightedValue = SearchHelper.replaceHighlightingPlaceholders(highlightedValue);
                                    metadataList.add(new Metadata(String.valueOf(structElement.getLuceneId()), termsFieldName, "", highlightedValue));
                                    additionalMetadataList
                                            .add(new Metadata(String.valueOf(structElement.getLuceneId()), termsFieldName, "", highlightedValue));
                                    existingMetadataFields.add(termsFieldName);
                                }
                            }
                        }
                    }
                    break;
            }
        }
    }

    /**
     * @param filename
     * @return
     */
    private static String getMimeTypeFromExtension(String filename) {
        try {
            URL fileUrl = new URL(filename);
            return ImageFileFormat.getImageFileFormatFromFileExtension(fileUrl.getPath()).getMimeType();
        } catch (MalformedURLException e) {
            logger.warn(e.getMessage());
        }
        return "";
    }

    /**
     *
     * @param se
     * @param locale
     * @return
     */
    private String generateLabel(StructElement se, Locale locale) {
        String ret = "";

        if (docType != null) {
            switch (docType) {
                case METADATA:
                    // Grouped metadata
                    if (metadataGroupType != null) {
                        switch (metadataGroupType) {
                            case PERSON:
                            case CORPORATION:
                            case LOCATION:
                            case SUBJECT:
                            case ORIGININFO:
                            case OTHER:
                                if (se.getMetadataValue("NORM_NAME") != null) {
                                    ret = se.getMetadataValue("NORM_NAME");
                                } else {
                                    ret = se.getMetadataValue("MD_VALUE");
                                }
                                if (ret == null) {
                                    ret = se.getMetadataValue(SolrConstants.LABEL);
                                }
                                break;
                            default:
                                ret = se.getMetadataValue(SolrConstants.LABEL);
                                break;
                        }
                    } else {
                        ret = se.getMetadataValue(SolrConstants.LABEL);
                    }
                    ret = ViewerResourceBundle.getTranslation(ret, locale);
                    break;
                case EVENT:
                    // Try to use the event name or type (optionally with dates), otherwise use LABEL
                    ret = se.getMetadataValue("MD_EVENTNAME");
                    if (StringUtils.isEmpty(ret)) {
                        ret = se.getMetadataValue(SolrConstants.EVENTTYPE);
                    }
                    if (StringUtils.isNotEmpty(ret)) {
                        String eventDate = se.getMetadataValue(SolrConstants.EVENTDATE);
                        String eventDateStart = se.getMetadataValue(SolrConstants.EVENTDATESTART);
                        String eventDateEnd = se.getMetadataValue(SolrConstants.EVENTDATESTART);
                        if (StringUtils.isNotEmpty(eventDateStart) && StringUtils.isNotEmpty(eventDateEnd) && !eventDateStart.equals(eventDateEnd)) {
                            ret += " (" + eventDateStart + " - " + eventDateEnd + ")";
                        } else if (StringUtils.isNotEmpty(eventDate)) {
                            ret += " (" + eventDate + ")";
                        }
                    } else {
                        ret = se.getMetadataValue(SolrConstants.LABEL);
                    }
                    ret = ViewerResourceBundle.getTranslation(ret, locale);
                    break;
                case UGC:
                    // User-generated content
                    ret = DisplayUserGeneratedContent.generateUgcLabel(se);
                    ret = ViewerResourceBundle.getTranslation(ret, locale);
                    break;
                default:
                    ret = generateDefaultLabel(se, locale);
                    break;
            }
        } else {
            logger.warn("{} field seems to be missing on Solr document {}", SolrConstants.DOCTYPE, se.getLuceneId());
            ret = generateDefaultLabel(se, locale);
        }

        if (ret == null) {
            ret = "";
            logger.error("Index document {}, has no LABEL, MD_TITLE or DOCSTRUCT fields. Perhaps there is no connection to the owner doc?",
                    se.getLuceneId());
        }

        return ret;
    }

    /**
     *
     * @param se
     * @param locale
     * @return
     * @should translate docstruct label
     */
    static String generateDefaultLabel(StructElement se, Locale locale) {
        String ret = null;
        if (locale != null) {
            // Prefer localized title
            String englishTitle = null;
            String germanTitle = null;
            String anyTitle = null;
            for (String key : se.getMetadataFields().keySet()) {
                if (key.equals(SolrConstants.TITLE + "_LANG_" + locale.getLanguage().toUpperCase())) {
                    ret = se.getMetadataValue(key);
                    break;
                } else if (key.equals(SolrConstants.TITLE + "_LANG_DE")) {
                    germanTitle = se.getMetadataValue(key);
                } else if (key.equals(SolrConstants.TITLE + "_LANG_EN")) {
                    englishTitle = se.getMetadataValue(key);
                } else if (key.matches(SolrConstants.TITLE + "_LANG_[A-Z][A-Z]")) {
                    anyTitle = se.getMetadataValue(key);
                }
            }
            if (StringUtils.isBlank(ret)) {
                if (StringUtils.isNotBlank(englishTitle)) {
                    ret = englishTitle;
                } else if (StringUtils.isNotBlank(germanTitle)) {
                    ret = germanTitle;
                } else {
                    ret = anyTitle;
                }
            }
        }
        // Fallback to LABEL or TITLE
        if (StringUtils.isEmpty(ret)) {
            ret = se.getMetadataValue(SolrConstants.LABEL);
            if (StringUtils.isEmpty(ret)) {
                ret = se.getMetadataValue(SolrConstants.TITLE);
            }
            // Fallback to DOCSTRCT
            if (StringUtils.isEmpty(ret)) {
                ret = ViewerResourceBundle.getTranslation(se.getDocStructType(), locale);
                // Fallback to DOCTYPE
                if (StringUtils.isEmpty(ret)) {
                    ret = ViewerResourceBundle.getTranslation("doctype_" + se.getMetadataValue(SolrConstants.DOCTYPE), locale);
                }
            }
        }

        return ret;

    }

    /**
     * <p>
     * Getter for the field <code>label</code>.
     * </p>
     *
     * @return the label
     */
    public String getLabel() {
        return label.getValue(BeanUtils.getLocale()).orElse(label.getValue().orElse(""));
    }

    /**
     * <p>
     * Getter for the field <code>label</code>.
     * </p>
     *
     * @param locale a {@link java.util.Locale} object.
     * @return a {@link java.lang.String} object.
     */
    public String getLabel(Locale locale) {
        return label.getValue(locale).orElse("");
    }

    /**
     * <p>
     * getLabelAsMetadataValue.
     * </p>
     *
     * @return a {@link de.intranda.metadata.multilanguage.IMetadataValue} object.
     */
    public IMetadataValue getLabelAsMetadataValue() {
        return label;
    }

    /**
     * <p>
     * Getter for the field <code>labelShort</code>.
     * </p>
     *
     * @return the labelShort
     */
    public String getLabelShort() {
        return labelShort.getValue(BeanUtils.getLocale()).orElse(labelShort.getValue().orElse(""));
    }

    /**
     * <p>
     * Setter for the field <code>labelShort</code>.
     * </p>
     *
     * @param labelShort the labelShort to set
     */
    public void setLabelShort(IMetadataValue labelShort) {
        this.labelShort = labelShort;
    }

    /**
     * <p>
     * Getter for the field <code>docStructType</code>.
     * </p>
     *
     * @return the type
     */
    public String getDocStructType() {
        return docStructType;
    }

    /**
     * <p>
     * Getter for the field <code>iddoc</code>.
     * </p>
     *
     * @return the iddoc
     */
    public long getIddoc() {
        return iddoc;
    }

    /**
     * <p>
     * Getter for the field <code>thumbnailUrl</code>.
     * </p>
     *
     * @return the thumbnailUrl
     */
    public String getThumbnailUrl() {
        //        logger.trace("thumbnailUrl {}", thumbnailUrl);
        return thumbnailUrl;
    }

    /**
     * Called from HTML.
     *
     * @param width a {@link java.lang.String} object.
     * @param height a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getThumbnailUrl(String width, String height) {
        synchronized (this) {
            String url = getThumbnailUrl();
            String urlNew = new IIIFUrlHandler().getModifiedIIIFFUrl(url, null,
                    new Scale.ScaleToBox(Integer.parseInt(width), Integer.parseInt(height)), null, null, null);
            return urlNew;
        }
    }

    /**
     * <p>
     * Getter for the field <code>imageNo</code>.
     * </p>
     *
     * @return a int.
     */
    public int getImageNo() {
        return imageNo;
    }

    /**
     * <p>
     * Getter for the field <code>structElements</code>.
     * </p>
     *
     * @return the structElements
     */
    public List<StructElementStub> getStructElements() {
        return structElements;
    }

    /**
     * Returns the lowest <code>StructElementStub</code> in the list.
     *
     * @return last StructElementStub in the list
     */
    public StructElementStub getBottomStructElement() {
        if (structElements == null || structElements.isEmpty()) {
            return null;
        }

        return structElements.get(structElements.size() - 1);
    }

    /**
     * @return the events
     */
    public List<EventElement> getEvents() {
        return events;
    }

    /**
     * <p>
     * Setter for the field <code>fulltext</code>.
     * </p>
     *
     * @param fulltext the fulltext to set
     */
    public void setFulltext(String fulltext) {
        this.fulltext = fulltext;
    }

    /**
     * <p>
     * Getter for the field <code>fulltext</code>.
     * </p>
     *
     * @return the fulltext
     */
    public String getFulltext() {
        return fulltext;
    }

    /**
     * Returns a relevant full-text fragment for displaying in the search hit box, stripped of any contained JavaScript.
     *
     * @return Full-text fragment sans any line breaks or JavaScript
     * @should remove any line breaks
     * @should remove any JS
     */
    public String getFulltextForHtml() {
        if (fulltextForHtml == null) {
            if (fulltext != null) {
                fulltextForHtml = StringTools.stripJS(fulltext).replaceAll("\n", " ");
            } else {
                fulltextForHtml = "";
            }
        }

        return fulltextForHtml;
    }

    /**
     * <p>
     * Getter for the field <code>volumeNo</code>.
     * </p>
     *
     * @return the volumeNo
     */
    public String getVolumeNo() {
        return volumeNo;
    }

    /**
     * <p>
     * Setter for the field <code>volumeNo</code>.
     * </p>
     *
     * @param volumeNo the volumeNo to set
     */
    public void setVolumeNo(String volumeNo) {
        this.volumeNo = volumeNo;
    }

    /**
     *
     * @return true if doctype is GROUP; false otherwise
     */
    public boolean isGroup() {
        return DocType.GROUP.equals(docType);
    }

    /**
     * <p>
     * isAnchor.
     * </p>
     *
     * @return the anchor
     */
    public boolean isAnchor() {
        return anchor;
    }

    /**
     * <p>
     * Setter for the field <code>anchor</code>.
     * </p>
     *
     * @param anchor the anchor to set
     */
    public void setAnchor(boolean anchor) {
        this.anchor = anchor;
    }

    /**
     * <p>
     * isHasImages.
     * </p>
     *
     * @return the hasImages
     */
    public boolean isHasImages() {
        return hasImages;
    }

    /**
     * <p>
     * Setter for the field <code>hasImages</code>.
     * </p>
     *
     * @param hasImages the hasImages to set
     */
    public void setHasImages(boolean hasImages) {
        this.hasImages = hasImages;
    }

    /**
     * @return the showThumbnail
     */
    public boolean isShowThumbnail() {
        return showThumbnail;
    }

    /**
     * @param showThumbnail the showThumbnail to set
     */
    public void setShowThumbnail(boolean showThumbnail) {
        this.showThumbnail = showThumbnail;
    }

    /**
     * <p>
     * Getter for the field <code>numVolumes</code>.
     * </p>
     *
     * @return the numVolumes
     */
    public long getNumVolumes() {
        return numVolumes;
    }

    /**
     * <p>
     * Setter for the field <code>pi</code>.
     * </p>
     *
     * @param pi the identifier to set
     */
    public void setPi(String pi) {
        this.pi = pi;
    }

    /**
     * <p>
     * Getter for the field <code>pi</code>.
     * </p>
     *
     * @return the identifier
     */
    public String getPi() {
        return pi;
    }

    /**
     * Returns the search hint URL (without the application root!).
     *
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * <p>
     * Getter for the field <code>sidebarPrevUrl</code>.
     * </p>
     *
     * @return the sidebarPrevUrl
     */
    public String getSidebarPrevUrl() {
        return sidebarPrevUrl;
    }

    /**
     * <p>
     * Getter for the field <code>sidebarNextUrl</code>.
     * </p>
     *
     * @return the sidebarNextUrl
     */
    public String getSidebarNextUrl() {
        return sidebarNextUrl;
    }

    /**
     *
     * @return
     */
    private String generateUrl() {
        return DataManager.getInstance().getUrlBuilder().generateURL(this);
    }

    /**
     * Important: hits have to have 3 Pretty parameters (e.g. /image/nextHit/PPN123/1/)
     *
     * @param type
     * @return
     */
    private String generateSidebarUrl(String type) {
        PageType configuredPageType = PageType.getPageTypeForDocStructType(docStructType);

        StringBuilder sb = new StringBuilder();
        if (anchor) {
            if (navigationHelper != null && PageType.viewMetadata.getName().equals(navigationHelper.getCurrentView())) {
                // Use the preferred view, if set and allowed for multivolumes
                String view = StringUtils.isNotEmpty(navigationHelper.getPreferredView()) ? navigationHelper.getPreferredView()
                        : PageType.viewToc.getName();
                if (!view.equals(PageType.viewToc.getName()) && !view.equals(PageType.viewMetadata.getName())) {
                    view = PageType.viewToc.getName();
                }
                sb.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext())
                        .append('/')
                        .append(view)
                        .append('/')
                        .append(type)
                        .append('/')
                        .append(pi)
                        .append('/')
                        .append(imageNo)
                        .append('/')
                        .append(StringUtils.isNotEmpty(logId) ? logId : '-')
                        .append('/');
            } else {
                sb.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext())
                        .append('/')
                        .append(PageType.viewToc.getName())
                        .append('/')
                        .append(type)
                        .append('/')
                        .append(pi)
                        .append('/')
                        .append(imageNo)
                        .append('/')
                        .append(StringUtils.isNotEmpty(logId) ? logId : '-')
                        .append('/');
            }
        } else if (navigationHelper != null && StringUtils.isNotEmpty(navigationHelper.getPreferredView())) {
            // Use the preferred view, if set
            sb.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext())
                    .append('/')
                    .append(navigationHelper.getPreferredView())
                    .append('/')
                    .append(type)
                    .append('/')
                    .append(pi)
                    .append('/')
                    .append(imageNo)
                    .append('/')
                    .append(StringUtils.isNotEmpty(logId) ? logId : '-')
                    .append('/');
        } else if (configuredPageType != null) {
            // logger.trace("Found configured page type: {}", configuredPageType.getName());
            sb.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext())
                    .append('/')
                    .append(configuredPageType.getName())
                    .append('/')
                    .append(type)
                    .append('/')
                    .append(pi)
                    .append('/')
                    .append(imageNo)
                    .append('/')
                    .append(StringUtils.isNotEmpty(logId) ? logId : '-')
                    .append('/');
        } else if (hasImages || hasMedia) {
            // Regular image view
            sb.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext())
                    .append('/')
                    .append(PageType.viewObject.getName())
                    .append('/')
                    .append(type)
                    .append('/')
                    .append(pi)
                    .append('/')
                    .append(imageNo)
                    .append('/')
                    .append(StringUtils.isNotEmpty(logId) ? logId : '-')
                    .append('/');
        } else {
            // Metadata view for elements without a thumbnail
            sb.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext())
                    .append('/')
                    .append(PageType.viewMetadata.getName())
                    .append('/')
                    .append(type)
                    .append('/')
                    .append(pi)
                    .append('/')
                    .append(imageNo)
                    .append('/')
                    .append(StringUtils.isNotEmpty(logId) ? logId : '-')
                    .append('/');
        }

        return sb.toString();
    }

    /**
     * <p>
     * Getter for the field <code>metadataList</code>.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<Metadata> getMetadataList() {
        return metadataList;
    }

    /**
     *
     * @param field Requested field name
     * @param locale Requested locale
     * @return
     */
    public List<Metadata> getMetadataListForLocale(String field, Locale locale) {
        return Metadata.filterMetadata(metadataList, locale != null ? locale.getLanguage() : null, field);
    }

    /**
     * <p>
     * getMetadataListForLocale.
     * </p>
     *
     * @param locale a {@link java.util.Locale} object.
     * @return a {@link java.util.List} object.
     */
    public List<Metadata> getMetadataListForLocale(Locale locale) {
        return Metadata.filterMetadata(metadataList, locale != null ? locale.getLanguage() : null, null);
    }

    /**
     * <p>
     * getMetadataListForCurrentLocale.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<Metadata> getMetadataListForCurrentLocale() {
        return getMetadataListForLocale(BeanUtils.getLocale());
    }

    /**
     * <p>
     * Setter for the field <code>metadataList</code>.
     * </p>
     *
     * @param metadataList a {@link java.util.List} object.
     */
    public void setMetadataList(List<Metadata> metadataList) {
        this.metadataList = metadataList;
    }

    /**
     * @return the existingMetadataFields
     */
    public Set<String> getExistingMetadataFields() {
        return existingMetadataFields;
    }

    /**
     * <p>
     * Getter for the field <code>metadataGroupType</code>.
     * </p>
     *
     * @return the metadataGroupType
     */
    public MetadataGroupType getMetadataGroupType() {
        return metadataGroupType;
    }

    /**
     * <p>
     * Getter for the field <code>metadataList</code>.
     * </p>
     *
     * @param metadataLabel a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     */
    public List<Metadata> getMetadataList(String metadataLabel) {
        List<Metadata> list = new ArrayList<>();
        for (Metadata metadata : getMetadataList()) {
            if (metadata.getLabel().equals(metadataLabel)) {
                list.add(metadata);
            }
        }
        return list;
    }

    /**
     * <p>
     * Getter for the field <code>additionalMetadataList</code>.
     * </p>
     *
     * @return the additionalMetadataList
     */
    public List<Metadata> getAdditionalMetadataList() {
        return additionalMetadataList;
    }

    /**
     * <p>
     * Getter for the field <code>dataRepository</code>.
     * </p>
     *
     * @return the dataRepository
     */
    public String getDataRepository() {
        return dataRepository;
    }

    /**
     * Returns the ContextObject value for a COinS element using the docstruct hierarchy for this search hit..
     *
     * @return a {@link java.lang.String} object.
     */
    public String getContextObject() {
        if (contextObject == null && !structElements.isEmpty()) {
            StructElementStub topStruct = structElements.get(structElements.size() - 1);
            if (topStruct.isAnchor() && structElements.size() > 1) {
                topStruct = structElements.get(structElements.size() - 2);
            }
            try {
                contextObject = structElements.get(0).generateContextObject(getUrl(), topStruct);
            } catch (PresentationException e) {
                logger.debug("PresentationException thrown here: {}", e.getMessage());
            }
        }

        return contextObject;
    }

    /**
     * <p>
     * Getter for the field <code>recordLanguages</code>.
     * </p>
     *
     * @return the recordLanguages
     */
    public List<String> getRecordLanguages() {
        return recordLanguages;
    }

    /**
     * <p>
     * Setter for the field <code>hasMedia</code>.
     * </p>
     *
     * @param hasMedia the hasMedia to set
     */
    public void setHasMedia(boolean hasMedia) {
        this.hasMedia = hasMedia;
    }

    /**
     * <p>
     * isHasMedia.
     * </p>
     *
     * @return the hasMedia
     */
    public boolean isHasMedia() {
        return hasMedia;
    }

    /**
     * <p>
     * Getter for the field <code>originalFieldName</code>.
     * </p>
     *
     * @return the originalFieldName
     */
    public String getOriginalFieldName() {
        return originalFieldName;
    }

    /**
     * <p>
     * determinePageType.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.viewer.PageType} object.
     */
    public PageType determinePageType() {
        return PageType.determinePageType(docStructType, mimeType, anchor || DocType.GROUP.equals(docType), hasImages || hasMedia, false);
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
     * Getter for the field <code>docType</code>.
     * </p>
     *
     * @return the docType
     */
    public DocType getDocType() {
        return docType;
    }

}
