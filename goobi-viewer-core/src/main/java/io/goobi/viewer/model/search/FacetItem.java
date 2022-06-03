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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.SearchBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.solr.SolrConstants;

/**
 * <p>
 * FacetItem class.
 * </p>
 */
public class FacetItem implements Serializable, IFacetItem {

    private static final long serialVersionUID = 5033196184122928247L;

    private static final Logger logger = LoggerFactory.getLogger(FacetItem.class);

    private static final Comparator<IFacetItem> NUMERIC_COMPARATOR = new FacetItem.NumericComparator();
    private static final Comparator<IFacetItem> ALPHABETIC_COMPARATOR = new FacetItem.AlphabeticComparator();
    private static final Comparator<IFacetItem> COUNT_COMPARATOR = new FacetItem.CountComparator();

    private String field;
    private String value;
    private String value2;
    private String link;
    private String label;
    private String translatedLabel;
    private long count;
    private final boolean hierarchial;

    /**
     * Constructor that doesn't parse the link; for testing purposes.
     *
     * @param hierarchical
     */
    FacetItem(boolean hierarchical) {
        this.hierarchial = hierarchical;
    }

    /**
     * Constructor for active facets received via the URL. The Solr query is split into individual field/value.
     *
     * @param link a {@link java.lang.String} object.
     * @param hierarchical a boolean.
     * @should split field and value correctly
     * @should split field and value range correctly
     */
    public FacetItem(String link, boolean hierarchical) {
        this(link, null, hierarchical);
    }

    /**
     * Constructor for active facets received via the URL. The Solr query is split into individual field/value.
     *
     * @param link a {@link java.lang.String} object.
     * @param label
     * @param hierarchical a boolean.
     * @should split field and value correctly
     * @should split field and value range correctly
     * @should set label to value if no label value given
     */
    public FacetItem(String link, String label, boolean hierarchical) {
        this.label = label;
        this.hierarchial = hierarchical;
        setLink(link.trim());
    }

    public FacetItem(Count count) {
        this(count.getFacetField().getName(), count.getFacetField().getName() + ":" + count.getName(), count.getName(), Messages.translate(count.getName(), BeanUtils.getLocale()), count.getCount(), false);
    }


    /**
     * Internal constructor.
     *
     * @param link {@link String}
     * @param label {@link String}
     * @param translatedLabel {@link String}
     * @param count {@link Integer}
     * @param hierarchical
     */
    private FacetItem(String field, String link, String label, String translatedLabel, long count, boolean hierarchical) {
        this.field = field;
        this.label = label;
        this.translatedLabel = translatedLabel;
        this.count = count;
        this.hierarchial = hierarchical;
        setLink(link.trim());
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((field == null) ? 0 : field.hashCode());
        result = prime * result + ((link == null) ? 0 : link.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        FacetItem other = (FacetItem) obj;
        if (field == null) {
            if (other.field != null) {
                return false;
            }
        } else if (!field.equals(other.field)) {
            return false;
        }
        if (link == null) {
            if (other.link != null) {
                return false;
            }
        } else if (!link.equals(other.link)) {
            return false;
        }
        return true;
    }

    /**
     * Extracts field name and value(s) from the given link string.
     *
     * @should set label to value if label empty
     */
    void parseLink() {
        if (link == null) {
            return;
        }

        int colonIndex = link.indexOf(':');
        if (colonIndex == -1) {
            return;
        }

        this.field = link.substring(0, colonIndex);

        String fullValue = link.substring(colonIndex + 1);
        if (fullValue.startsWith("[") && fullValue.endsWith("]") && fullValue.contains(" TO ")) {
            this.value = fullValue.substring(1, fullValue.indexOf(" TO "));
            this.value2 = fullValue.substring(fullValue.indexOf(" TO ") + 4, fullValue.length() - 1);
        } else {
            value = fullValue;
        }
        if (StringUtils.isEmpty(label)) {
            label = value;
        }
    }

    /**
     * Constructs facet items from thelist of given field:value combinations. Always sorted by the label translation.
     *
     * @param field Facet field
     * @param values Map containing facet values and their counts
     * @param hierarchical true if facet field is hierarchical; false otherwise
     * @param locale Optional locale for translation
     * @param labelMap Optional map for storing alternate labels for later use by the client
     * @return {@link java.util.ArrayList} of {@link io.goobi.viewer.model.search.FacetItem}
     * @should add priority values first
     * @should set label from separate field if configured and found
     */
    public static List<IFacetItem> generateFilterLinkList(String field, Map<String, Long> values, boolean hierarchical, Locale locale,
            Map<String, String> labelMap) {
        // logger.trace("generateFilterLinkList: {}", field);
        List<IFacetItem> retList = new ArrayList<>();
        List<String> priorityValues = DataManager.getInstance().getConfiguration().getPriorityValuesForFacetField(field);
        Map<String, FacetItem> priorityValueMap = new HashMap<>(priorityValues.size());

        // If a separate label field configured for fieldName, load all values and add them to labelMap
        String labelField = DataManager.getInstance().getConfiguration().getLabelFieldForFacetField(field);
        if (labelField != null && labelMap != null) {
            try {
                labelMap.putAll(DataManager.getInstance()
                        .getSearchIndex()
                        .getLabelValuesForFacetField(field, labelField, values.keySet()));
            } catch (PresentationException e) {
                logger.debug(e.getMessage());
            } catch (IndexUnreachableException e) {
                logger.error(e.getMessage(), e);
            }
        }

        for (String value : values.keySet()) {
            // Skip reversed values
            if (value.charAt(0) == 1) {
                continue;
            }
            String label = value;

            String key = field + ":" + value;
            if (labelMap != null && labelMap.containsKey(key)) {
                label = labelMap.get(key);
                logger.trace("using label from map: {}", label);
            }

            if (StringUtils.isEmpty(field)) {
                label = new StringBuilder(value).append(SolrConstants._FACETS_SUFFIX).toString();
            }
            String linkValue = value;
            if (field.endsWith(SolrConstants._UNTOKENIZED)) {
                linkValue = new StringBuilder("\"").append(linkValue).append('"').toString();
            }
            String link = StringUtils.isNotEmpty(field) ? new StringBuilder(field).append(':').append(linkValue).toString() : linkValue;
            FacetItem facetItem =
                    new FacetItem(field, link, StringTools.intern(label), ViewerResourceBundle.getTranslation(label, locale), values.get(value),
                            hierarchical);
            if (!priorityValues.isEmpty() && priorityValues.contains(value)) {
                priorityValueMap.put(value, facetItem);
            } else {
                retList.add(facetItem);
            }
        }
        switch (DataManager.getInstance().getConfiguration().getSortOrder(SearchHelper.defacetifyField(field))) {
            case "numerical":
            case "numerical_asc":
                Collections.sort(retList, FacetItem.NUMERIC_COMPARATOR);
                break;
            case "numerical_desc":
                Collections.sort(retList, FacetItem.NUMERIC_COMPARATOR);
                Collections.reverse(retList);
                break;
            case "alphabetical":
            case "alphabetical_asc":
                Collections.sort(retList, FacetItem.ALPHABETIC_COMPARATOR);
                break;
            case "alphabetical_desc":
                Collections.sort(retList, FacetItem.ALPHABETIC_COMPARATOR);
                Collections.reverse(retList);
                break;
            default:
                Collections.sort(retList, FacetItem.COUNT_COMPARATOR);

        }
        // Add priority values at the beginning
        if (!priorityValueMap.isEmpty()) {
            List<IFacetItem> regularValues = new ArrayList<>(retList);
            retList.clear();
            for (String val : priorityValues) {
                if (priorityValueMap.containsKey(val)) {
                    retList.add(priorityValueMap.get(val));
                }
            }
            retList.addAll(regularValues);
        }
        // logger.debug("filters: {}", retList.size());
        return retList;
    }

    /**
     * Constructs a list of FilterLink objects for the drill-down. Optionally sorted by the raw values.
     *
     * @param field a {@link java.lang.String} object.
     * @param values a {@link java.util.Map} object.
     * @param sort a boolean.
     * @param reverseOrder a boolean.
     * @param hierarchical a boolean.
     * @param locale a {@link java.util.Locale} object.
     * @should sort items correctly
     * @return a {@link java.util.List} object.
     */
    public static List<IFacetItem> generateFacetItems(String field, Map<String, Long> values, boolean sort, boolean reverseOrder,
            boolean hierarchical,
            Locale locale) {
        if (field == null) {
            throw new IllegalArgumentException("field may not be null");
        }
        if (values == null) {
            throw new IllegalArgumentException("values may not be null");
        }

        List<IFacetItem> retList = new ArrayList<>(values.keySet().size());

        boolean numeric = true;
        for (String s : values.keySet()) {
            try {
                Long.valueOf(s);
            } catch (NumberFormatException e) {
                numeric = false;
                break;
            }
        }
        List<String> keys = new ArrayList<>(values.keySet());
        if (sort) {
            if (numeric) {
                List<Long> numberKeys = new ArrayList<>();
                for (String s : keys) {
                    numberKeys.add(Long.valueOf(s));
                }
                Collections.sort(numberKeys);
                if (reverseOrder) {
                    Collections.reverse(numberKeys);
                }
                keys.clear();
                for (Long l : numberKeys) {
                    keys.add(String.valueOf(l));
                }
            } else {
                Collections.sort(keys);
                if (reverseOrder) {
                    Collections.reverse(keys);
                }
            }
        }

        for (Object value : keys) {
            String label = String.valueOf(value);
            if (StringUtils.isEmpty(field)) {
                label += SolrConstants._FACETS_SUFFIX;
            }
            String link = StringUtils.isNotEmpty(field) ? field + ":" + ClientUtils.escapeQueryChars(String.valueOf(value)) : String.valueOf(value);
            retList.add(new FacetItem(field, link, label, ViewerResourceBundle.getTranslation(label, locale), values.get(String.valueOf(value)),
                    hierarchical));
        }

        // logger.debug("filters: " + retList.size());
        return retList;
    }

    /**
     * Returns field:value (with the value escaped for the Solr query).
     *
     * @should construct link correctly
     * @should escape values containing whitespaces
     * @should construct hierarchical link correctly
     * @should construct range link correctly
     * @should construct polygon link correctly
     * @return a {@link java.lang.String} object.
     */
    @Override
    public String getQueryEscapedLink() {
        String field = SearchHelper.facetifyField(this.field);
        String escapedValue = getEscapedValue(value);
        if (hierarchial) {
            return new StringBuilder("(").append(field)
                    .append(':')
                    .append(escapedValue)
                    .append(" OR ")
                    .append(field)
                    .append(':')
                    .append(escapedValue)
                    .append(".*)")
                    .toString();
        }
        if (value2 == null) {
            return new StringBuilder(field).append(':').append(escapedValue).toString();
        }
        String escapedValue2 = getEscapedValue(value2);
        return new StringBuilder(field).append(":[").append(escapedValue).append(" TO ").append(escapedValue2).append(']').toString();
    }

    /**
     *
     * @param value
     * @return
     * @should escape value correctly
     * @should add quotation marks if value contains space
     * @should preserve leading and trailing quotation marks
     */
    static String getEscapedValue(String value) {
        if (StringUtils.isEmpty(value)) {
            return value;
        }
        String escapedValue = null;
        if (value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"' && value.length() > 2) {
            escapedValue = '"' + ClientUtils.escapeQueryChars(value.substring(1, value.length() - 1)) + '"';
        } else {
            escapedValue = ClientUtils.escapeQueryChars(value);
        }
        // Add quotation marks if spaces are contained
        if (escapedValue.contains(" ") && !escapedValue.startsWith("\"") && !escapedValue.endsWith("\"")) {
            escapedValue = '"' + escapedValue + '"';
        }

        return escapedValue;
    }

    /**
     * Link after slash/backslash replacements for partner collection, static drill-down components and topic browsing (HU Berlin).
     *
     * @return a {@link java.lang.String} object.
     */
    @Override
    public String getEscapedLink() {
        return BeanUtils.escapeCriticalUrlChracters(link);
    }

    /**
     * URL escaped link for using in search drill-downs.
     *
     * @return a {@link java.lang.String} object.
     */
    @Override
    public String getUrlEscapedLink() {
        String ret = getEscapedLink();
        try {
            return URLEncoder.encode(ret, SearchBean.URL_ENCODING);
        } catch (UnsupportedEncodingException e) {
            return ret;
        }
    }

    /**
     * <p>
     * Getter for the field <code>field</code>.
     * </p>
     *
     * @return the field
     */
    @Override
    public String getField() {
        return field;
    }

    /**
     * <p>
     * Setter for the field <code>field</code>.
     * </p>
     *
     * @param field the field to set
     */
    @Override
    public void setField(String field) {
        this.field = field;
    }

    /**
     * <p>
     * getFullValue.
     * </p>
     *
     * @return Range of value - value2; just value if value2 empty
     * @should build full value correctly
     */
    public String getFullValue() {
        StringBuilder sb = new StringBuilder(value);
        if (StringUtils.isNotEmpty(value2)) {
            sb.append(" - ").append(value2);
        }

        return sb.toString();
    }

    /**
     * <p>
     * Getter for the field <code>value</code>.
     * </p>
     *
     * @return the value
     */
    @Override
    public String getValue() {
        return value;
    }

    /**
     * <p>
     * Setter for the field <code>value</code>.
     * </p>
     *
     * @param value the value to set
     */
    @Override
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * <p>
     * Getter for the field <code>value2</code>.
     * </p>
     *
     * @return the value2
     */
    @Override
    public String getValue2() {
        return value2;
    }

    /**
     * <p>
     * Setter for the field <code>value2</code>.
     * </p>
     *
     * @param value2 the value2 to set
     */
    @Override
    public void setValue2(String value2) {
        this.value2 = value2;
    }

    /**
     * <p>
     * Getter for the field <code>link</code>.
     * </p>
     *
     * @return the link
     */
    @Override
    public String getLink() {
        return link;
    }

    /**
     * <p>
     * Setter for the field <code>link</code>.
     * </p>
     *
     * @param link the link to set
     */
    @Override
    public void setLink(String link) {
        // TODO move logic out of the setter
        int colonIndex = link.indexOf(':');
        if (colonIndex == -1) {
            throw new IllegalArgumentException(new StringBuilder().append("Field and value are not colon-separated: ").append(link).toString());
        }
        this.link = link;
        if (this.link.endsWith(";;")) {
            this.link = this.link.substring(0, this.link.length() - 2);
        }
        parseLink();
    }

    /**
     * <p>
     * Getter for the field <code>label</code>.
     * </p>
     *
     * @return the label
     */
    @Override
    public String getLabel() {
        return label;
    }

    /**
     * <p>
     * Setter for the field <code>label</code>.
     * </p>
     *
     * @param label the label to set
     * @return this
     */
    @Override
    public FacetItem setLabel(String label) {
        this.label = label;
        return this;
    }

    /**
     * <p>
     * Getter for the field <code>translatedLabel</code>.
     * </p>
     *
     * @return the translatedLabel
     */
    @Override
    public String getTranslatedLabel() {
        return translatedLabel;
    }

    /**
     * <p>
     * Setter for the field <code>translatedLabel</code>.
     * </p>
     *
     * @param translatedLabel the translatedLabel to set
     */
    @Override
    public void setTranslatedLabel(String translatedLabel) {
        this.translatedLabel = translatedLabel;
    }

    /**
     * <p>
     * Getter for the field <code>count</code>.
     * </p>
     *
     * @return the count
     */
    @Override
    public long getCount() {
        return count;
    }

    /**
     * <p>
     * Setter for the field <code>count</code>.
     * </p>
     *
     * @param count the count to set
     * @return this
     */
    @Override
    public FacetItem setCount(long count) {
        this.count = count;
        return this;
    }

    /**
     * <p>
     * isHierarchial.
     * </p>
     *
     * @return the hierarchial
     */
    @Override
    public boolean isHierarchial() {
        return hierarchial;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#toString()
     */
    public String toString() {
        return field + ":" + value + " - " + value2;
    }

    public static class AlphabeticComparator implements Comparator<IFacetItem> {

        @Override
        public int compare(IFacetItem o1, IFacetItem o2) {
            String label1 = o1.getTranslatedLabel() != null ? o1.getTranslatedLabel() : o1.getLabel();
            String label2 = o2.getTranslatedLabel() != null ? o2.getTranslatedLabel() : o2.getLabel();
            int ret = label1.compareTo(label2);
            return ret;
        }

    }

    public static class NumericComparator implements Comparator<IFacetItem> {

        @Override
        public int compare(IFacetItem o1, IFacetItem o2) {
            try {
                int i1 = Integer.parseInt(o1.getLabel());
                int i2 = Integer.parseInt(o2.getLabel());
                return Integer.compare(i1, i2);
            } catch (NumberFormatException e) {
                return o1.getLabel().compareTo(o2.getLabel());
            }
        }

    }

    public static class CountComparator implements Comparator<IFacetItem> {

        @Override
        public int compare(IFacetItem o1, IFacetItem o2) {
            return o1.getCount() > o2.getCount() ? -1
                    : o1.getCount() < o2.getCount() ? +1 : (o1.getLabel() != null ? o1.getLabel().compareTo(o2.getLabel()) : 0);

        }

    }
}
