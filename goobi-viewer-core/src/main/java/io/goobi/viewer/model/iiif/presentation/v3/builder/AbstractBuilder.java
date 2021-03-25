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
package io.goobi.viewer.model.iiif.presentation.v3.builder;

import static io.goobi.viewer.api.rest.v2.ApiUrls.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.api.annotation.oa.OpenAnnotation;
import de.intranda.api.iiif.presentation.enums.AnnotationType;
import de.intranda.api.iiif.presentation.v3.AbstractPresentationModelElement3;
import de.intranda.api.iiif.presentation.v3.Canvas3;
import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.Metadata;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import de.unigoettingen.sub.commons.util.PathConverter;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.AbstractApiUrlManager.ApiPath;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.controller.SolrSearchIndex;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.iiif.presentation.v3.builder.LinkingProperty.LinkingTarget;
import io.goobi.viewer.model.iiif.presentation.v3.builder.LinkingProperty.LinkingType;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StructElement;

/**
 * <p>
 * Abstract AbstractBuilder class.
 * </p>
 *
 * @author Florian Alpers
 */
public abstract class AbstractBuilder {

    private static final Logger logger = LoggerFactory.getLogger(AbstractBuilder.class);

    protected final AbstractApiUrlManager urls;
    
    private final Map<LinkingProperty.LinkingType, List<LinkingProperty>> linkingProperties = new HashMap<>();

    private final List<Locale> translationLocales = DataManager.getInstance().getConfiguration().getIIIFTranslationLocales();
    
    protected final DataRetriever dataRetriever = new DataRetriever();
    
    /**
     * <p>
     * Constructor for AbstractBuilder.
     * </p>
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     */
    public AbstractBuilder(AbstractApiUrlManager apiUrlManager) {
        if (apiUrlManager == null) {
            apiUrlManager = DataManager.getInstance().getRestApiManager().getDataApiManager().orElse(null);
        }
        this.urls = apiUrlManager;
        this.initLinkingProperties();
    }

    /**
     *  Read config for rendering linking properties and add configured properties to linkingProperties map
     */
    private void initLinkingProperties() {
        if(DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingPDF()) {
            IMetadataValue label = getLabel(DataManager.getInstance().getConfiguration().getLabelIIIFRenderingPDF());
            addRendering(LinkingTarget.PDF, label);
        }
        if(DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingViewer()) {
            IMetadataValue label = getLabel(DataManager.getInstance().getConfiguration().getLabelIIIFRenderingViewer());
            addRendering(LinkingTarget.VIEWER, label);
        }
        if(DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingPlaintext()) {
            IMetadataValue label = getLabel(DataManager.getInstance().getConfiguration().getLabelIIIFRenderingPlaintext());
            addRendering(LinkingTarget.PLAINTEXT, label);
        }
        if(DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingAlto()) {
            IMetadataValue label = getLabel(DataManager.getInstance().getConfiguration().getLabelIIIFRenderingAlto());
            addRendering(LinkingTarget.ALTO, label);
        }
    }

    /**
     * @param labelIIIFRenderingPDF
     * @return
     */
    protected IMetadataValue getLabel(String value) {
        return ViewerResourceBundle.getTranslations(value, this.translationLocales, false);
    }

    /**
     * @param iconURI
     * @return
     */
    public URI absolutize(URI uri) {
        if (uri == null) {
            return null;
        }
        if (uri.isAbsolute()) {
            return uri;
        }
        try {
            return PathConverter.resolve(this.urls.getApplicationUrl(), uri.toString());
        } catch (URISyntaxException e) {
            logger.error(e.toString(), e);
            return uri;
        }
    }

    public URI absolutize(String uri) {
        return absolutize(URI.create(uri));
    }

    /**
     * <p>
     * getLocale.
     * </p>
     *
     * @param language a {@link java.lang.String} object.
     * @return a {@link java.util.Locale} object.
     */
    protected Locale getLocale(String language) {
        Locale locale = Locale.forLanguageTag(language);
        if (locale == null) {
            locale = Locale.ENGLISH;
        }
        return locale;
    }

    /**
     * <p>
     * getMetsResolverUrl.
     * </p>
     *
     * @return METS resolver link for the DFG Viewer
     * @param ele a {@link io.goobi.viewer.model.viewer.StructElement} object.
     */
    public String getMetsResolverUrl(StructElement ele) {
        try {
            return urls.getApplicationUrl() + "/metsresolver?id=" + ele.getPi();
        } catch (Exception e) {
            logger.error("Could not get METS resolver URL for {}.", ele.getLuceneId());
            Messages.error("errGetCurrUrl");
        }
        return urls.getApplicationUrl() + "/metsresolver?id=" + 0;
    }

    /**
     * <p>
     * getLidoResolverUrl.
     * </p>
     *
     * @return LIDO resolver link for the DFG Viewer
     * @param ele a {@link io.goobi.viewer.model.viewer.StructElement} object.
     */
    public String getLidoResolverUrl(StructElement ele) {
        try {
            return urls.getApplicationUrl() + "/lidoresolver?id=" + ele.getPi();
        } catch (Exception e) {
            logger.error("Could not get LIDO resolver URL for {}.", ele.getLuceneId());
            Messages.error("errGetCurrUrl");
        }
        return urls.getApplicationUrl() + "/lidoresolver?id=" + 0;
    }

    /**
     * <p>
     * getViewUrl.
     * </p>
     *
     * @return viewer url for the given page in the given {@link io.goobi.viewer.model.viewer.PageType}
     * @param ele a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     * @param pageType a {@link io.goobi.viewer.model.viewer.PageType} object.
     */
    public String getViewUrl(PhysicalElement ele, PageType pageType) {
        try {
            return urls.getApplicationUrl() + "/" + pageType.getName() + ele.getPurlPart();
        } catch (Exception e) {
            logger.error("Could not get METS resolver URL for page {} + in {}.", ele.getOrder(), ele.getPi());
            Messages.error("errGetCurrUrl");
        }
        return urls.getApplicationUrl() + "/metsresolver?id=" + 0;
    }

    /**
     * Simple method to create a label for a {@link org.apache.solr.common.SolrDocument} from {@link io.goobi.viewer.controller.SolrConstants.LABEL},
     * {@link io.goobi.viewer.controller.SolrConstants.TITLE} or {@link io.goobi.viewer.controller.SolrConstants.DOCSTRUCT}
     *
     * @param solrDocument a {@link org.apache.solr.common.SolrDocument} object.
     * @return a {@link java.util.Optional} object.
     */
    public Optional<IMetadataValue> getLabelIfExists(SolrDocument solrDocument) {

        String label = (String) solrDocument.getFirstValue(SolrConstants.LABEL);
        String title = (String) solrDocument.getFirstValue(SolrConstants.TITLE);
        String docStruct = (String) solrDocument.getFirstValue(SolrConstants.DOCSTRCT);

        if (StringUtils.isNotBlank(label)) {
            return Optional.of(new SimpleMetadataValue(label));
        } else if (StringUtils.isNotBlank(title)) {
            return Optional.of(new SimpleMetadataValue(title));
        } else if (StringUtils.isNotBlank(docStruct)) {
            return Optional.of(getLabel(docStruct));
        } else {
            return Optional.empty();
        }
    }

    /**
     * <p>
     * addMetadata.
     * </p>
     *
     * @param manifest a {@link de.intranda.api.iiif.presentation.AbstractPresentationModelElement} object.
     * @param ele a {@link io.goobi.viewer.model.viewer.StructElement} object.
     */
    public void addMetadata(AbstractPresentationModelElement3 manifest, StructElement ele) {
        List<String> displayFields = DataManager.getInstance().getConfiguration().getIIIFMetadataFields();
        List<String> eventFields = DataManager.getInstance().getConfiguration().getIIIFEventFields();
        displayFields.addAll(eventFields);

        for (String field : getMetadataFields(ele)) {
            if (contained(field, displayFields) && !field.endsWith(SolrConstants._UNTOKENIZED) && !field.matches(".*_LANG_\\w{2,3}")) {
                String configuredLabel = DataManager.getInstance().getConfiguration().getIIIFMetadataLabel(field);
                String label = StringUtils.isNotBlank(configuredLabel) ? configuredLabel
                        : (field.contains("/") ? field.substring(field.indexOf("/") + 1) : field);
                SolrSearchIndex.getTranslations(field, ele, this.translationLocales,(s1, s2) -> s1 + "; " + s2)
                        .map(value -> new Metadata(getLabel(label), value))
                        .ifPresent(md -> {
                            md.getLabel().removeTranslation(MultiLanguageMetadataValue.DEFAULT_LANGUAGE);
                            md.getValue().removeTranslation(MultiLanguageMetadataValue.DEFAULT_LANGUAGE);
                            manifest.addMetadata(md);
                        });
            }
        }
    }
   

    /**
     * Return true if the field is contained in displayFields, accounting for wildcard characters
     * 
     * @param field
     * @param displayFields
     * @return
     */
    private static boolean contained(String field, List<String> displayFields) {

        return displayFields.stream().map(displayField -> displayField.replace("*", "")).anyMatch(displayField -> field.startsWith(displayField));
    }



    /**
     * @param ele
     * @return
     */
    private static List<String> getMetadataFields(StructElement ele) {
        Set<String> fields = ele.getMetadataFields().keySet();
        List<String> baseFields = fields.stream().map(field -> field.replaceAll("_LANG_\\w{2,3}$", "")).distinct().collect(Collectors.toList());
        return baseFields;
    }



    /**
     * Gets the attribution text configured in webapi.iiif.attribution and returns all translations if any are found, or the configured string itself
     * otherwise
     *
     * @return the configured attribution
     */
    protected List<IMetadataValue> getAttributions() {
        List<IMetadataValue> messages = DataManager.getInstance()
                .getConfiguration()
                .getIIIFAttribution()
                .stream()
                .map(this::getLabel)
                .collect(Collectors.toList());

        return messages;
    }

    /**
     * <p>
     * getDescription.
     * </p>
     *
     * @param ele a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @return a {@link java.util.Optional} object.
     */
    protected Optional<IMetadataValue> getDescription(StructElement ele) {
        List<String> fields = DataManager.getInstance().getConfiguration().getIIIFDescriptionFields();
        for (String field : fields) {
            Optional<IMetadataValue> optional = SolrSearchIndex.getTranslations(field, ele, (s1, s2) -> s1 + "; " + s2).map(md -> {
                md.removeTranslation(MultiLanguageMetadataValue.DEFAULT_LANGUAGE);
                return md;
            });
            if (optional.isPresent()) {
                return optional;
            }
        }
        return Optional.empty();
    }

    /**
     * <p>
     * getCollectionURI.
     * </p>
     *
     * @param collectionField a {@link java.lang.String} object.
     * @param baseCollectionName a {@link java.lang.String} object.
     * @return a {@link java.net.URI} object.
     */
    public URI getCollectionURI(String collectionField, String baseCollectionName) {
        String urlString;
        if (StringUtils.isNotBlank(baseCollectionName)) {
            baseCollectionName = StringTools.encodeUrl(baseCollectionName);
            urlString = this.urls.path(COLLECTIONS, COLLECTIONS_COLLECTION).params(collectionField, baseCollectionName).build();
        } else {
            urlString = this.urls.path(COLLECTIONS).params(collectionField).build();
        }
        return URI.create(urlString);
    }

    /**
     * <p>
     * getManifestURI.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @return a {@link java.net.URI} object.
     */
    public URI getManifestURI(String pi) {
        String urlString = this.urls.path(RECORDS_RECORD, RECORDS_MANIFEST).params(pi).build();
        return URI.create(urlString);
    }

    /**
     * <p>
     * getManifestURI.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param mode a {@link io.goobi.viewer.model.iiif.presentation.v2.builder.BuildMode} object.
     * @return a {@link java.net.URI} object.
     */
    public URI getManifestURI(String pi, BuildMode mode) {
        String urlString = this.urls.path(RECORDS_RECORD, RECORDS_MANIFEST).params(pi).query("mode", mode.name()).build();
        return URI.create(urlString);
    }

    /**
     * <p>
     * getRangeURI.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param logId a {@link java.lang.String} object.
     * @return a {@link java.net.URI} object.
     */
    public URI getRangeURI(String pi, String logId) {
        String urlString = this.urls.path(RECORDS_SECTIONS, RECORDS_SECTIONS_RANGE).params(pi, logId).build();
        return URI.create(urlString);
    }

    /**
     * <p>
     * getSequenceURI.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param label a {@link java.lang.String} object.
     * @return a {@link java.net.URI} object.
     */
    public URI getSequenceURI(String pi, String label) {
        if (StringUtils.isBlank(label)) {
            label = "basic";
        }
        String urlString = this.urls.path(RECORDS_PAGES, RECORDS_PAGES_SEQUENCE).params(pi, label).build();
        return URI.create(urlString);
    }

    /**
     * <p>
     * getCanvasURI.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param pageNo a int.
     * @return a {@link java.net.URI} object.
     */
    public URI getCanvasURI(String pi, int pageNo) {
        String urlString = this.urls.path(RECORDS_PAGES, RECORDS_PAGES_CANVAS).params(pi, pageNo).build();
        return URI.create(urlString);
    }

    /**
     * Get the page order (1-based) from a canavs URI. That is the number in the last path paramter after '/canvas/' If the URI doesn't match a canvas
     * URI, null is returned
     *
     * @param uri a {@link java.net.URI} object.
     * @return a {@link java.lang.Integer} object.
     */
    public Integer getPageOrderFromCanvasURI(URI uri) {
        String regex = "/pages/(\\d+)/canvas";
        Matcher matcher = Pattern.compile(regex).matcher(uri.toString());
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }

        return null;
    }

    /**
     * Get the persistent identifier from a canvas URI. This is the URI path param between '/iiif/manifests/' and '/canvas/'
     *
     * @param uri a {@link java.net.URI} object.
     * @return The pi, or null if the URI doesn't match a iiif canvas URI
     */
    public String getPIFromCanvasURI(URI uri) {
        String regex = "/records/([\\w\\-\\s]+)/pages/(\\d+)/canvas";
        Matcher matcher = Pattern.compile(regex).matcher(uri.toString());
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }

    /**
     * <p>
     * getAnnotationListURI.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param pageNo a int.
     * @param type a {@link de.intranda.api.iiif.presentation.enums.AnnotationType} object.
     * @return a {@link java.net.URI} object.
     */
    public URI getAnnotationListURI(String pi, int pageNo, AnnotationType type, boolean openAnnotation) {
        ApiPath url;
        switch(type) {
            case COMMENT:
                url = this.urls.path(RECORDS_PAGES, RECORDS_PAGES_COMMENTS).params(pi, pageNo);
                break;
            case CROWDSOURCING:
                url = this.urls.path(RECORDS_PAGES, RECORDS_PAGES_ANNOTATIONS).params(pi, pageNo);
                break;
            case ALTO:
            case FULLTEXT:
            default:
                url = this.urls.path(RECORDS_PAGES, RECORDS_PAGES_TEXT).params(pi, pageNo);
        }
        
        
        if (openAnnotation) {
            url = url.query("format", "oa");

        }

        return URI.create(url.build());
    }

    /**
     * <p>
     * getAnnotationListURI.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param type a {@link de.intranda.api.iiif.presentation.enums.AnnotationType} object.
     * @return a {@link java.net.URI} object.
     */
    public URI getAnnotationListURI(String pi, AnnotationType type) {
        String urlString = this.urls.path(RECORDS_RECORD, RECORDS_ANNOTATIONS).params(pi).query("type", type.name()).build();

        return URI.create(urlString);
    }

    /**
     * <p>
     * getCommentAnnotationURI.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param pageNo a int.
     * @param id a long.
     * @return a {@link java.net.URI} object.
     */
    public URI getCommentAnnotationURI(long id) {
        String urlString = this.urls.path(ANNOTATIONS, ANNOTATIONS_COMMENT).params(id).build();

        return URI.create(urlString);
    }


    /**
     * <p>
     * getImageAnnotationURI.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param order a int.
     * @return a {@link java.net.URI} object.
     */
    public URI getImageAnnotationURI(String pi, int order) {
        String urlString = this.urls.path(RECORDS_PAGES, RECORDS_PAGES_CANVAS).params(pi, order).build() + "image/1/";
        return URI.create(urlString);
    }

    /**
     * <p>
     * getAnnotationURI.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param order a int.
     * @param type a {@link de.intranda.api.iiif.presentation.enums.AnnotationType} object.
     * @param annoNum a int.
     * @return a {@link java.net.URI} object.
     * @throws java.net.URISyntaxException if any.
     */
    public URI getAnnotationURI(String pi, int order, AnnotationType type, int annoNum) throws URISyntaxException {
        String urlString = this.urls.path(RECORDS_PAGES, RECORDS_PAGES_CANVAS).params(pi, order).build() + type.name() + "/" + annoNum + "/";
        return URI.create(urlString);
    }

    /**
     * <p>
     * getAnnotationURI.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param type a {@link de.intranda.api.iiif.presentation.enums.AnnotationType} object.
     * @param id a {@link java.lang.String} object.
     * @return a {@link java.net.URI} object.
     */
    public URI getAnnotationURI(String id) {
        String urlString = this.urls.path(ANNOTATIONS, ANNOTATIONS_ANNOTATION).params(id).build();
        return URI.create(urlString);
    }


    /**
     * Get URL to search service from {@link ApiUrls}
     * 
     * @param pi The persistent identifier of the work to search
     * @return the service URI
     */
    public URI getSearchServiceURI(String pi) {
        return URI.create(urls.path(RECORDS_RECORD, RECORDS_MANIFEST_SEARCH).params(pi).build());
    }


    /**
     * Get URL to auto complete service from {@link ApiUrls}
     * 
     * @param pi The persistent identifier of the work to search for autocomplete
     * @return the service URI
     */
    public URI getAutoCompleteServiceURI(String pi) {
        return URI.create(urls.path(RECORDS_RECORD, RECORDS_MANIFEST_AUTOCOMPLETE).params(pi).build());
    }

    /**
     * <p>
     * getSearchURI.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param query a {@link java.lang.String} object.
     * @param motivation a {@link java.util.List} object.
     * @return a {@link java.net.URI} object.
     */
    public URI getSearchURI(String pi, String query, List<String> motivation) {
        String uri = getSearchServiceURI(pi).toString();
        uri += ("?q=" + query);
        if (!motivation.isEmpty()) {
            uri += ("&motivation=" + StringUtils.join(motivation, "+"));
        }
        return URI.create(uri);
    }

    /**
     * <p>
     * getAutoSuggestURI.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param query a {@link java.lang.String} object.
     * @param motivation a {@link java.util.List} object.
     * @return a {@link java.net.URI} object.
     */
    public URI getAutoSuggestURI(String pi, String query, List<String> motivation) {
        String uri = getAutoCompleteServiceURI(pi).toString();
        if (StringUtils.isNotBlank(query)) {
            uri += ("?q=" + query);
            if (!motivation.isEmpty()) {
                uri += ("&motivation=" + StringUtils.join(motivation, "+"));
            }
        }
        return URI.create(uri);
    }

    
    public AbstractBuilder addSeeAlso(LinkingProperty.LinkingTarget target, IMetadataValue label) {
        LinkingType type = LinkingType.SEE_ALSO;
        addLinkingProperty(target, label, type);
        return this;
    }
    
    public AbstractBuilder addRendering(LinkingProperty.LinkingTarget target, IMetadataValue label) {
        LinkingType type = LinkingType.RENDERING;
        addLinkingProperty(target, label, type);
        return this;
    }

    private void addLinkingProperty(LinkingProperty.LinkingTarget target, IMetadataValue label, LinkingType type) {
        LinkingProperty property = new LinkingProperty(type, target, label);
        List<LinkingProperty> seeAlsos = this.linkingProperties.get(type);
        if(seeAlsos == null) {
            seeAlsos = new ArrayList<>();
            this.linkingProperties.put(type, seeAlsos);
        }
        seeAlsos.add(property);
    }
    
    public List<LinkingProperty> getSeeAlsos() {
        List<LinkingProperty> seeAlsos = this.linkingProperties.get(LinkingType.SEE_ALSO);
        if(seeAlsos != null) {
            return seeAlsos;
        } else {
            return Collections.emptyList();
        }
    }
    
    public List<LinkingProperty> getRenderings() {
        List<LinkingProperty> renderings = this.linkingProperties.get(LinkingType.RENDERING);
        if(renderings != null) {
            return renderings;
        } else {
            return Collections.emptyList();
        }
    }

}
