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
package io.goobi.viewer.model.viewer.pageloader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.controller.SolrSearchIndex;
import io.goobi.viewer.controller.SolrConstants.DocType;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.PhysicalElementBuilder;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.model.viewer.StructElement;

/**
 * <p>
 * Abstract AbstractPageLoader class.
 * </p>
 */
public abstract class AbstractPageLoader implements IPageLoader {

    private static final Logger logger = LoggerFactory.getLogger(AbstractPageLoader.class);

    /**
     * Replaces the static variable placeholders (the ones that don't change depending on the page) of the given label format with values.
     *
     * @param format a {@link java.lang.String} object.
     * @param locale a {@link java.util.Locale} object.
     * @should replace numpages currectly
     * @should replace message keys correctly
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    protected String buildPageLabelTemplate(String format, Locale locale) throws IndexUnreachableException {
        if (format == null) {
            throw new IllegalArgumentException("format may not be null");
        }
        String labelTemplate = format.replace("{numpages}", String.valueOf(getNumPages()));
        Pattern p = Pattern.compile("\\{msg\\..*?\\}");
        Matcher m = p.matcher(labelTemplate);
        while (m.find()) {
            String key = labelTemplate.substring(m.start() + 5, m.end() - 1);
            labelTemplate = labelTemplate.replace(labelTemplate.substring(m.start(), m.end()), ViewerResourceBundle.getTranslation(key, locale));
        }
        return labelTemplate;
    }

    /**
     * <p>
     * loadPage.
     * </p>
     *
     * @param topElement a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @param page a int.
     * @return a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public static PhysicalElement loadPage(StructElement topElement, int page) throws PresentationException, IndexUnreachableException {
        if (topElement.isAnchor() || topElement.isGroup()) {
            logger.debug("Anchor or group document, no pages.");
            return null;
        }

        String pi = topElement.getPi();
        logger.trace("Loading pages for '{}'...", pi);
        List<String> fields = new ArrayList<>(Arrays.asList(FIELDS));

        StringBuilder sbQuery = new StringBuilder();
        sbQuery.append("+")
                .append(SolrConstants.PI_TOPSTRUCT)
                .append(':')
                .append(topElement.getPi())
                .append(" +")
                .append(SolrConstants.DOCTYPE)
                .append(':')
                .append(DocType.PAGE)
                .append(" +")
                .append(SolrConstants.ORDER)
                .append(':')
                .append(page);
        SolrDocumentList result = DataManager.getInstance()
                .getSearchIndex()
                .search(sbQuery.toString(), SolrSearchIndex.MAX_HITS, Collections.singletonList(new StringPair(SolrConstants.ORDER, "asc")), fields);
        if (result == null || result.isEmpty()) {
            return null;
        }

        return loadPageFromDoc(result.get(0), pi, topElement, null);
    }

    /**
     * <p>
     * loadPageFromDoc.
     * </p>
     *
     * @param doc Solr document from which to construct the page
     * @param pi Record identifier
     * @param topElement StructElement of the top record element
     * @param pageOwnerIddocMap Optional map containing relationships between pages and owner IDDOCs
     * @return Constructed PhysicalElement
     */
    protected static PhysicalElement loadPageFromDoc(SolrDocument doc, String pi, StructElement topElement, Map<Integer, Long> pageOwnerIddocMap) {
        if (doc == null) {
            throw new IllegalArgumentException("doc may not be null");
        }
        if (pi == null) {
            throw new IllegalArgumentException("pi may not be null");
        }

        // PHYSID
        String physId = "";
        if (doc.getFieldValue(SolrConstants.PHYSID) != null) {
            physId = (String) doc.getFieldValue(SolrConstants.PHYSID);
        }
        // ORDER
        int order = (Integer) doc.getFieldValue(SolrConstants.ORDER);
        // ORDERLABEL
        String orderLabel = "";
        if (doc.getFieldValue(SolrConstants.ORDERLABEL) != null) {
            orderLabel = (String) doc.getFieldValue(SolrConstants.ORDERLABEL);
        }
        // IDDOC_OWNER
        if (doc.getFieldValue(SolrConstants.IDDOC_OWNER) != null && pageOwnerIddocMap != null) {
            String iddoc = (String) doc.getFieldValue(SolrConstants.IDDOC_OWNER);
            pageOwnerIddocMap.put(order, Long.valueOf(iddoc));
        }
        // Mime type
        String mimeType = null;
        if (doc.getFieldValue(SolrConstants.MIMETYPE) != null) {
            mimeType = (String) doc.getFieldValue(SolrConstants.MIMETYPE);
        }
        // Main file name
        String fileName = "";
        if (doc.getFieldValue(SolrConstants.FILENAME_HTML_SANDBOXED) != null) {
            fileName = (String) doc.getFieldValue(SolrConstants.FILENAME_HTML_SANDBOXED);
        } else if (doc.getFieldValue(SolrConstants.FILENAME) != null) {
            fileName = (String) doc.getFieldValue(SolrConstants.FILENAME);
        }

        String dataRepository = "";
        if (doc.getFieldValue(SolrConstants.DATAREPOSITORY) != null) {
            dataRepository = (String) doc.getFieldValue(SolrConstants.DATAREPOSITORY);
        } else if (topElement != null) {
            dataRepository = topElement.getDataRepository();
        }

        // URN
        String urn = "";
        if (doc.getFieldValue(SolrConstants.IMAGEURN) != null && !doc.getFirstValue(SolrConstants.IMAGEURN).equals("NULL")) {
            urn = (String) doc.getFieldValue(SolrConstants.IMAGEURN);
        }
        StringBuilder sbPurlPart = new StringBuilder();
        sbPurlPart.append('/').append(pi).append('/').append(order).append('/');

        PhysicalElement pe = new PhysicalElementBuilder().setPi(pi)
                .setPhysId(physId)
                .setFilePath(fileName)
                .setOrder(order)
                .setOrderLabel(orderLabel)
                .setUrn(urn)
                .setPurlPart(sbPurlPart.toString())
                .setMimeType(mimeType)
                .setDataRepository(dataRepository)
                .build();

        if (doc.getFieldValue(SolrConstants.WIDTH) != null) {
            pe.setWidth((Integer) doc.getFieldValue(SolrConstants.WIDTH));
        }
        if (doc.getFieldValue(SolrConstants.HEIGHT) != null) {
            pe.setHeight((Integer) doc.getFieldValue(SolrConstants.HEIGHT));
        }

        // Full-text filename
        pe.setFulltextFileName((String) doc.getFirstValue(SolrConstants.FILENAME_FULLTEXT));
        // ALTO filename
        pe.setAltoFileName((String) doc.getFirstValue(SolrConstants.FILENAME_ALTO));

        // Access conditions
        if (doc.getFieldValues(SolrConstants.ACCESSCONDITION) != null) {
            for (Object o : doc.getFieldValues(SolrConstants.ACCESSCONDITION)) {
                String accessCondition = (String) o;
                if (StringUtils.isNotEmpty(accessCondition)) {
                    pe.getAccessConditions().add(accessCondition);
                }
            }
        }

        // File names for different formats (required for A/V)
        String filenameRoot = new StringBuilder(SolrConstants.FILENAME).append('_').toString();
        for (String fieldName : doc.getFieldNames()) {
            if (fieldName.startsWith(filenameRoot)) {
                // logger.trace("Format: {}", fieldName);
                String format = fieldName.split("_")[1].toLowerCase();
                String value = (String) doc.getFieldValue(fieldName);
                pe.getFileNames().put(format, value);
            }
        }

        // METS file ID root
        if (doc.getFieldValue(SolrConstants.FILEIDROOT) != null) {
            pe.setFileIdRoot((String) doc.getFieldValue(SolrConstants.FILEIDROOT));
        }

        // File size
        if (doc.getFieldValue("MDNUM_FILESIZE") != null) {
            pe.setFileSize((long) doc.getFieldValue("MDNUM_FILESIZE"));
        }

        // Full-text available
        if (doc.containsKey(SolrConstants.FULLTEXTAVAILABLE)) {
            pe.setFulltextAvailable((boolean) doc.getFieldValue(SolrConstants.FULLTEXTAVAILABLE));
        }

        // Image available
        if (doc.containsKey(SolrConstants.BOOL_IMAGEAVAILABLE)) {
            pe.setHasImage((boolean) doc.getFieldValue(SolrConstants.BOOL_IMAGEAVAILABLE));
        }
        
        // Double page view
        if (doc.containsKey(SolrConstants.BOOL_DOUBPLE_PAGE)) {
            pe.setDoublePage((boolean) doc.getFieldValue(SolrConstants.BOOL_DOUBPLE_PAGE));
        }

        return pe;
    }
}
