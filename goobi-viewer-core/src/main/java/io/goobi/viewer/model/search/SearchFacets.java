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
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.SearchBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.CMSCollection;
import io.goobi.viewer.solr.SolrConstants;

/**
 * Current faceting settings for a search.
 */
public class SearchFacets implements Serializable {

    private static final long serialVersionUID = -7170821006287251119L;

    private static final Logger logger = LoggerFactory.getLogger(SearchFacets.class);

    private final Object lock = new Object();

    /** Available regular facets for the current search result. */
    private final Map<String, List<IFacetItem>> availableFacets = new LinkedHashMap<>();
    /** Currently applied facets. */
    private final List<IFacetItem> currentFacets = new ArrayList<>();

    private final Map<String, Boolean> facetsExpanded = new HashMap<>();

    private final Map<String, String> minValues = new HashMap<>();

    private final Map<String, String> maxValues = new HashMap<>();

    private final Map<String, List<Integer>> valueRanges = new HashMap<>();
    /** Map storing labels from separate label fields that were already retrieved from the index. */
    private final Map<String, String> labelMap = new HashMap<>();

    private String tempValue;

    /**
     * <p>
     * resetAvailableFacets.
     * </p>
     */
    public void resetAvailableFacets() {
        logger.trace("resetAvailableFacets");
        availableFacets.clear();
        facetsExpanded.clear();
    }

    /**
     * <p>
     * resetCurrentFacets.
     * </p>
     */
    public void resetCurrentFacets() {
        resetCurrentFacetString();
    }

    /**
     * <p>
     * resetSliderRange.
     * </p>
     */
    public void resetSliderRange() {
        logger.trace("resetSliderRange");
        minValues.clear();
        maxValues.clear();
        valueRanges.clear();
    }

    /**
     * Generates a list containing filter queries for the selected regular and hierarchical facets.
     *
     * @param advancedSearchGroupOperator a int.
     * @param includeRangeFacets a boolean.
     * @return a {@link java.util.List} object.
     */
    public List<String> generateFacetFilterQueries(int advancedSearchGroupOperator, boolean includeRangeFacets, boolean includeGeoFacet) {
        List<String> ret = new ArrayList<>(2);

        // Add hierarchical facets
        String hierarchicalQuery = generateHierarchicalFacetFilterQuery(advancedSearchGroupOperator);
        if (StringUtils.isNotEmpty(hierarchicalQuery)) {
            ret.add(hierarchicalQuery);
        }

        // Add regular facets
        String regularQuery = generateFacetFilterQuery(includeRangeFacets);
        if (StringUtils.isNotEmpty(regularQuery)) {
            ret.add(regularQuery);
        }

        return ret;
    }

    /**
     * Generates a filter query for the selected hierarchical facets.
     *
     * @param advancedSearchGroupOperator
     * @return
     * @should generate query correctly
     * @should return null if facet list is empty
     */
    String generateHierarchicalFacetFilterQuery(int advancedSearchGroupOperator) {
        if (currentFacets.isEmpty()) {
            return null;
        }

        StringBuilder sbQuery = new StringBuilder();
        int count = 0;
        for (IFacetItem facetItem : currentFacets) {
            if (!facetItem.isHierarchial()) {
                continue;
            }
            if (count > 0) {
                if (advancedSearchGroupOperator == 1) {
                    sbQuery.append(" OR ");
                } else {
                    sbQuery.append(SolrConstants.SOLR_QUERY_AND);
                }
            }
            String field = SearchHelper.facetifyField(facetItem.getField());
            sbQuery.append('(')
                    .append(field)
                    .append(':')
                    .append("\"" + facetItem.getValue() + "\"")
                    .append(" OR ")
                    .append(field)
                    .append(':')
                    .append(facetItem.getValue())
                    .append(".*)");
            count++;
        }

        return sbQuery.toString();
    }

    /**
     * Generates a filter query for the selected non-hierarchical facets.
     *
     * @param includeRangeFacets
     * @return
     * @should generate query correctly
     * @should return null if facet list is empty
     * @should skip range facet fields if so requested
     * @should skip subelement fields
     */
    String generateFacetFilterQuery(boolean includeRangeFacets) {
        if (currentFacets.isEmpty()) {
            return null;
        }

        StringBuilder sbQuery = new StringBuilder();
        for (IFacetItem facetItem : currentFacets) {
            if (facetItem.isHierarchial()) {
                continue;
            }
            if (facetItem.getField().equals(SolrConstants.DOCSTRCT_SUB)) {
                continue;
            }
            if (!includeRangeFacets && DataManager.getInstance().getConfiguration().getRangeFacetFields().contains(facetItem.getField())) {
                continue;
            }
            if (sbQuery.length() > 0) {
                sbQuery.append(" AND ");
            }
            sbQuery.append(facetItem.getQueryEscapedLink());
            logger.trace("Added facet: {}", facetItem.getQueryEscapedLink());
        }

        return sbQuery.toString();
    }

    /**
     * Generates a filter query for the selected subelement facets.
     *
     * @return
     * @should generate query correctly
     */
    String generateSubElementFacetFilterQuery() {
        StringBuilder sbQuery = new StringBuilder();

        if (!currentFacets.isEmpty()) {
            for (IFacetItem facetItem : currentFacets) {
                if (facetItem.getField().equals(SolrConstants.DOCSTRCT_SUB)) {
                    if (sbQuery.length() > 0) {
                        sbQuery.append(" AND ");
                    }
                    sbQuery.append(facetItem.getQueryEscapedLink());
                    logger.trace("Added subelement facet: {}", facetItem.getQueryEscapedLink());
                }
            }
        }

        return sbQuery.toString();
    }

    /**
     * Returns the first FacetItem objects in <code>currentFacets</code> where the field name matches the given field name.
     *
     * @param field The field name to match.
     * @return a {@link io.goobi.viewer.model.search.FacetItem} object.
     */
    public IFacetItem getCurrentFacetForField(String field) {
        List<IFacetItem> ret = getCurrentFacetsForField(field);
        if (!ret.isEmpty()) {
            return ret.get(0);
        }

        return null;
    }

    /**
     * Returns a list of FacetItem objects in <code>currentFacets</code> where the field name matches the given field name.
     *
     * @param field The field name to match.
     * @return a {@link java.util.List} object.
     */
    public List<IFacetItem> getCurrentFacetsForField(String field) {
        List<IFacetItem> ret = new ArrayList<>();

        for (IFacetItem facet : currentFacets) {
            if (facet.getField().equals(field)) {
                ret.add(facet);
            }
        }

        return ret;
    }

    /**
     * Checks whether the given facet is currently in use.
     *
     * @param facet The facet to check.
     * @return a boolean.
     */
    public boolean isFacetCurrentlyUsed(IFacetItem facet) {
        for (IFacetItem fi : getCurrentFacetsForField(facet.getField())) {
            if (fi.getLink().equals(facet.getLink())) {
                return true;
            }
        }

        return false;
    }

    /**
     * <p>
     * isFacetListSizeSufficient.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean isFacetListSizeSufficient(String field) {
        if (availableFacets.get(field) != null) {
            if (SolrConstants.DOCSTRCT_SUB.equals(field)) {
                return getAvailableFacetsListSizeForField(field) > 0;
            }
            return getAvailableFacetsListSizeForField(field) > 1;
        }

        return false;
    }

    /**
     * Returns the size of the full element list of the facet for the given field.
     *
     * @param field a {@link java.lang.String} object.
     * @return a int.
     */
    public int getAvailableFacetsListSizeForField(String field) {
        if (availableFacets.get(field) != null) {
            return availableFacets.get(field).size();
        }

        return 0;
    }

    /**
     * <p>
     * getCurrentFacetsSizeForField.
     * </p>
     *
     * @return Size of <code>currentFacets</code>.
     * @param field a {@link java.lang.String} object.
     */
    public int getCurrentFacetsSizeForField(String field) {
        return getCurrentFacetsForField(field).size();
    }

    /**
     * Returns a collapsed sublist of the available facet elements for the given field.
     *
     * @param field a {@link java.lang.String} object.
     * @should return full DC facet list if expanded
     * @should return full DC facet list if list size less than default
     * @should return reduced DC facet list if list size larger than default
     * @should return full facet list if expanded
     * @should return full facet list if list size less than default
     * @should return reduced facet list if list size larger than default
     * @should not contain currently used facets
     * @return a {@link java.util.List} object.
     */
    public List<IFacetItem> getLimitedFacetListForField(String field) {
        // logger.trace("getLimitedFacetListForField: {}", field);
        List<IFacetItem> facetItems = availableFacets.get(field);
        if (facetItems == null) {
            return Collections.emptyList();
        }
        // Remove currently used facets
        facetItems.removeAll(currentFacets);
        int initial = DataManager.getInstance().getConfiguration().getInitialFacetElementNumber(field);
        if (!isFacetExpanded(field) && initial != -1 && facetItems.size() > initial) {
            return facetItems.subList(0, initial);
        }

        // logger.trace("facet items {}: {}", field, facetItems.size());
        return facetItems;
    }

    /**
     * If the facet for given field is expanded, return the size of the facet, otherwise the initial (collapsed) number of elements as configured.
     *
     * @should return full facet size if expanded
     * @should return default if collapsed
     * @param field a {@link java.lang.String} object.
     * @return a int.
     * @deprecated Check whether still in use
     */
    @Deprecated
    public int getFacetElementDisplayNumber(String field) {
        if (isFacetExpanded(field) && availableFacets.get(field) != null) {
            return availableFacets.get(field).size();
        }
        return DataManager.getInstance().getConfiguration().getInitialFacetElementNumber(field);
    }

    /**
     * Sets the expanded flag to <code>true</code> for the given facet field.
     *
     * @param field a {@link java.lang.String} object.
     */
    public void expandFacet(String field) {
        logger.trace("expandFacet: {}", field);
        facetsExpanded.put(field, true);
    }

    /**
     * Sets the expanded flag to <code>false</code> for the given facet field.
     *
     * @param field a {@link java.lang.String} object.
     */
    public void collapseFacet(String field) {
        logger.trace("collapseFacet: {}", field);
        facetsExpanded.put(field, false);
    }

    /**
     * Returns true if the "(more)" link is to be displayed for a facet box. This is the case if the facet has more elements than the initial number
     * of displayed elements and the facet hasn't been manually expanded yet.
     *
     * @param field a {@link java.lang.String} object.
     * @should return true if DC facet collapsed and has more elements than default
     * @should return true if facet collapsed and has more elements than default
     * @should return false if DC facet expanded
     * @should return false if facet expanded
     * @should return false if DC facet smaller than default
     * @should return false if facet smaller than default
     * @return a boolean.
     */
    public boolean isDisplayFacetExpandLink(String field) {
        List<IFacetItem> facetItems = availableFacets.get(field);
        int expandSize = DataManager.getInstance().getConfiguration().getInitialFacetElementNumber(field);
        
        return facetItems != null && !isFacetExpanded(field) && expandSize > 0 && facetItems.size() > expandSize;
    }

    /**
     * <p>
     * isDisplayFacetCollapseLink.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean isDisplayFacetCollapseLink(String field) {
        return isFacetExpanded(field);
    }

    /**
     * Returns a URL encoded SSV string of facet fields and values from the elements in currentFacets (hyphen if empty).
     *
     * @return SSV string of facet queries or "-" if empty
     * @should contain queries from all FacetItems
     * @should return hyphen if currentFacets empty
     */
    public String getCurrentFacetString() {
        String ret = generateFacetPrefix(new ArrayList<>(currentFacets), true);
        if (StringUtils.isEmpty(ret)) {
            ret = "-";
        }
        try {
            return URLEncoder.encode(ret, SearchBean.URL_ENCODING);
        } catch (UnsupportedEncodingException e) {
            return ret;
        }
    }

    /**
     * <p>
     * getCurrentFacetString.
     * </p>
     *
     * @param urlEncode a boolean.
     * @return a {@link java.lang.String} object.
     */
    public String getCurrentFacetString(boolean urlEncode) {
        String ret = generateFacetPrefix(new ArrayList<>(currentFacets), true);
        if (StringUtils.isEmpty(ret)) {
            ret = "-";
        }
        try {
            return URLEncoder.encode(ret, SearchBean.URL_ENCODING);
        } catch (UnsupportedEncodingException e) {
            return ret;
        }
    }

    /**
     * <p>
     * getCurrentHierarchicalFacetString.
     * </p>
     *
     * @return the currentCollection
     */
    @Deprecated
    public String getCurrentHierarchicalFacetString() {
        return "-";
    }

    /**
     * Receives an SSV string of facet fields and values (FIELD1:value1;FIELD2:value2;FIELD3:value3) and generates new Elements for currentFacets.
     *
     * @param currentFacetString a {@link java.lang.String} object.
     * @should create FacetItems from all links
     * @should decode slashes and backslashes
     * @should reset slider range if no slider field among current facets
     * @should not reset slider range if slider field among current facets
     */
    public void setCurrentFacetString(String currentFacetString) {
        parseFacetString(currentFacetString, currentFacets, labelMap);
    }

    /**
     * Receives an SSV string of facet fields and values (FIELD1:value1;FIELD2:value2;FIELD3:value3) and generates new Elements for
     * currentHierarchicalFacets.
     *
     * @param currentHierarchicalFacetString a {@link java.lang.String} object.
     */
    @Deprecated
    public void setCurrentHierarchicalFacetString(String currentHierarchicalFacetString) {
        //
    }

    /**
     * Constructs a list of facet items out of the given facet string.
     *
     * @param facetString String containing field:value pairs
     * @param facetItems List of facet items to which to add the parsed items
     * @param labelMap Map containing labels for a field:value pair if the facet field uses separate labels
     * @should fill list correctly
     * @should empty list before filling
     * @should add DC field prefix if no field name is given
     * @should set hierarchical status correctly
     * @should use label from labelMap if available
     */
    static void parseFacetString(String facetString, List<IFacetItem> facetItems, Map<String, String> labelMap) {

        if (facetItems == null) {
            facetItems = new ArrayList<>();
        } else {
            facetItems.clear();
        }

        if (StringUtils.isEmpty(facetString) || "-".equals(facetString)) {
            return;
        }

        if (labelMap == null) {
            labelMap = Collections.emptyMap();
        }
        try {
            facetString = URLDecoder.decode(facetString, StandardCharsets.UTF_8.name());
            facetString = StringTools.unescapeCriticalUrlChracters(facetString);
            facetString = URLDecoder.decode(facetString, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            //
        }
        String[] facetStringSplit = facetString.split(";;");
        for (String facetLink : facetStringSplit) {
            if (StringUtils.isNotEmpty(facetLink)) {
                if (!facetLink.contains(":")) {
                    facetLink = new StringBuilder(SolrConstants.DC).append(':').append(facetLink).toString();
                }
                String facetField = facetLink.substring(0, facetLink.indexOf(":"));
                if (facetField.equals(DataManager.getInstance().getConfiguration().getGeoFacetFields())) {
                    GeoFacetItem item = new GeoFacetItem(facetField);
                    item.setValue(facetLink.substring(facetLink.indexOf(":") + 1));
                    facetItems.add(item);
                } else {
                    // If there is a cached pre-generated label for this facet link (separate label field), use it so that there's no empty label
                    String label = labelMap.containsKey(facetLink) ? labelMap.get(facetLink) : null;
                    facetItems.add(
                            new FacetItem(facetLink, label, isFieldHierarchical(facetLink.substring(0, facetLink.indexOf(":")))));
                }
            }
        }
    }

    /**
     *
     * @param field
     * @return true if field is hierarchical; false otherwise
     */
    static boolean isFieldHierarchical(String field) {
        //        logger.trace("isFieldHierarchical: {} ? {}", field,
        return DataManager.getInstance().getConfiguration().getHierarchicalFacetFields().contains(field);
    }

    /**
     * Updates existing facet item for the given field with a new value. If no item for that field yet exist, a new one is added.
     *
     * @param field a {@link java.lang.String} object.
     * @param hierarchical a boolean.
     * @return a {@link java.lang.String} object.
     */
    public String updateFacetItem(String field, boolean hierarchical) {
        updateFacetItem(field, tempValue, currentFacets, hierarchical);

        return "pretty:search6"; // TODO advanced search
    }

    /**
     * Updates existing facet item for the given field with a new value. If no item for that field yet exist, a new one is added.
     *
     * @param field
     * @param updateValue
     * @param facetItems
     * @param hierarchical
     * @should update facet item correctly
     * @should add new item correctly
     */
    static void updateFacetItem(String field, String updateValue, List<IFacetItem> facetItems, boolean hierarchical) {
        if (facetItems == null) {
            facetItems = new ArrayList<>();
        }

        if (StringUtils.isNotEmpty(updateValue) && !"-".equals(updateValue)) {
            try {
                updateValue = URLDecoder.decode(updateValue, "utf-8");
                updateValue = StringTools.unescapeCriticalUrlChracters(updateValue);
            } catch (UnsupportedEncodingException e) {
                //
            }

            IFacetItem fieldItem = null;
            for (IFacetItem item : facetItems) {
                if (item.getField().equals(field)) {
                    fieldItem = item;
                    break;
                }
            }
            if (fieldItem == null) {
                String geoFacetField = DataManager.getInstance().getConfiguration().getGeoFacetFields();
                if (geoFacetField != null && geoFacetField.equals(field)) {
                    fieldItem = new GeoFacetItem(field);
                    fieldItem.setValue(updateValue);
                } else {
                    fieldItem = new FacetItem(field + ":" + updateValue, hierarchical);
                }
                facetItems.add(fieldItem);
            }
            fieldItem.setLink(field + ":" + updateValue);
            logger.trace("Facet item updated: {}", fieldItem.getLink());
        }
    }

    /**
     * <p>
     * getHierarchicalFacets.
     * </p>
     *
     * @param facetString a {@link java.lang.String} object.
     * @param facetFields a {@link java.util.List} object.
     * @return a {@link java.util.List} object.
     */
    public static List<String> getHierarchicalFacets(String facetString, List<String> facetFields) {
        List<String> facets = Arrays.asList(StringUtils.split(facetString, ";;"));
        List<String> values = new ArrayList<>();

        for (String facetField : facetFields) {
            String matchingFacet = facets.stream()
                    .filter(facet -> facet.replace(SolrConstants._UNTOKENIZED, "").startsWith(facetField + ":"))
                    .findFirst()
                    .orElse("");
            if (StringUtils.isNotBlank(matchingFacet)) {
                int separatorIndex = matchingFacet.indexOf(":");
                if (separatorIndex > 0 && separatorIndex < matchingFacet.length() - 1) {
                    String value = matchingFacet.substring(separatorIndex + 1);
                    values.add(value);
                }
            }
        }
        return values;
    }

    /**
     * <p>
     * splitHierarchicalFacet.
     * </p>
     *
     * @param facet a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     */
    public static List<String> splitHierarchicalFacet(String facet) {
        List<String> facets = new ArrayList<>();
        while (facet.contains(".")) {
            facets.add(facet);
            facet = facet.substring(0, facet.lastIndexOf("."));
        }
        if (StringUtils.isNotBlank(facet)) {
            facets.add(facet);
        }
        Collections.reverse(facets);
        return facets;
    }

    /**
     * <p>
     * getCurrentMinRangeValue.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @return Current min value, if facet in use; otherwise absolute min value for that field
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getCurrentMinRangeValue(String field) throws PresentationException, IndexUnreachableException {
        synchronized (lock) {
            for (IFacetItem item : currentFacets) {
                if (item.getField().equals(field)) {
                    logger.trace("currentMinRangeValue: {}", item.getValue());
                    return item.getValue();
                }
            }
        }

        return getAbsoluteMinRangeValue(field);
    }

    /**
     * <p>
     * getCurrentMaxRangeValue.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @return Current max value, if facet in use; otherwise absolute max value for that field
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getCurrentMaxRangeValue(String field) throws PresentationException, IndexUnreachableException {
        synchronized (lock) {
            for (IFacetItem item : currentFacets) {
                if (item.getField().equals(field)) {
                    if (item.getValue2() != null) {
                        logger.trace("currentMaxRangeValue: {}", item.getValue());
                        return item.getValue2();
                    }
                }
            }

            return getAbsoluteMaxRangeValue(field);
        }
    }

    /**
     * Returns the minimum value for the given field available in the search index.
     *
     * @param field a {@link java.lang.String} object.
     * @return Smallest available value
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getAbsoluteMinRangeValue(String field) throws PresentationException, IndexUnreachableException {
        if (!minValues.containsKey(field)) {
            return "0";
        }
        return minValues.get(field);
    }

    /**
     * Returns the maximum value for the given field available in the search index.
     *
     * @param field a {@link java.lang.String} object.
     * @return Largest available value
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getAbsoluteMaxRangeValue(String field) throws PresentationException, IndexUnreachableException {
        if (!maxValues.containsKey(field)) {
            return "0";
        }
        return maxValues.get(field);
    }

    /**
     * Returns a sorted list of all available values for the given field among available facet values.
     *
     * @param field a {@link java.lang.String} object.
     * @return sorted list of all values for the given field among available facet values
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public List<Integer> getValueRange(String field) throws PresentationException, IndexUnreachableException {
        if (!maxValues.containsKey(field)) {
            return Collections.emptyList();
        }
        return valueRanges.get(field);
    }

    /**
     * Adds the min and max values from the search index for the given field to the bottomValues map. Min and max values are determined via an
     * alphanumeric comparator.
     *
     * @param field
     * @should populate values correctly
     * @should add all values to list
     */
    void populateAbsoluteMinMaxValuesForField(String field, List<String> stringValues) {
        if (field == null) {
            return;
        }

        if (!SolrConstants._CALENDAR_YEAR.equals(field) && !field.startsWith("MDNUM_")) {
            logger.info("{} is not an integer type field, cannot use with a range query", field);
            return;
        }

        List<Integer> intValues = null;
        if (stringValues != null) {
            intValues = new ArrayList<>(stringValues.size());
            for (String s : stringValues) {
                if (s == null) {
                    continue;
                }
                intValues.add(Integer.valueOf(s));
            }
        } else {
            logger.trace("No facets found for field {}", field);
            intValues = Collections.emptyList();
        }
        if (!intValues.isEmpty()) {
            Collections.sort(intValues);
            valueRanges.put(field, intValues);
            minValues.put(field, String.valueOf(intValues.get(0)));
            maxValues.put(field, String.valueOf(intValues.get(intValues.size() - 1)));
            logger.trace("Absolute range for field {}: {} - {}", field, minValues.get(field), maxValues.get(field));
        }
    }

    /**
     * <p>
     * resetCurrentFacetString.
     * </p>
     */
    public void resetCurrentFacetString() {
        logger.trace("resetCurrentFacetString");
        setCurrentFacetString("-");
    }

    /**
     * Returns a URL encoded value returned by generateFacetPrefix() for regular facets. Returns an empty string instead a hyphen if empty.
     *
     * @return a {@link java.lang.String} object.
     *
     */
    public String getCurrentFacetStringPrefix() {
        return getCurrentFacetStringPrefix(true);
    }

    /**
     * Returns the value returned by generateFacetPrefix() for regular facets. Returns an empty string instead a hyphen if empty.
     *
     * @param urlEncode
     * @return
     */
    public String getCurrentFacetStringPrefix(boolean urlEncode) {
        if (urlEncode) {
            try {
                return URLEncoder.encode(generateFacetPrefix(new ArrayList<>(currentFacets), true), SearchBean.URL_ENCODING);
            } catch (UnsupportedEncodingException e) {
                logger.error(e.getMessage());
            }
        }

        return generateFacetPrefix(currentFacets, true);
    }

    /**
     * Generates an SSV string of facet fields and values from the elements in the given List<FacetString> (empty string if empty).
     *
     * @param facetItems
     * @param escapeSlashes If true, slashes and backslashes are replaced with URL-compatible replacement strings
     * @return
     * @should encode slashed and backslashes
     */
    static String generateFacetPrefix(List<IFacetItem> facetItems, boolean escapeSlashes) {
        if (facetItems == null) {
            throw new IllegalArgumentException("facetItems may not be null");
        }
        StringBuilder sb = new StringBuilder();
        for (IFacetItem facetItem : facetItems) {
            if (escapeSlashes) {
                sb.append(BeanUtils.escapeCriticalUrlChracters(facetItem.getLink()));
            } else {
                sb.append(facetItem.getLink());
            }
            sb.append(";;");
        }

        return sb.toString();
    }

    /**
     * <p>
     * removeFacetAction.
     * </p>
     *
     * @param facetQuery a {@link java.lang.String} object.
     * @should remove facet correctly
     * @should remove facet containing reserved chars
     * @param ret a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String removeFacetAction(final String facetQuery, final String ret) {
        logger.trace("removeFacetAction: {}", facetQuery);
        String currentFacetString = generateFacetPrefix(new ArrayList<>(currentFacets), false);
        if (currentFacetString.contains(facetQuery)) {
            currentFacetString = currentFacetString.replaceAll("(" + Pattern.quote(facetQuery) + ")(?=;|(?=/))", "").replace(";;;;", ";;");
            setCurrentFacetString(currentFacetString);
        }

        return ret;
    }

    /**
     * Returns true if the value for the given field type in <code>facetsExpanded</code> has been previously set to true.
     *
     * @param field a {@link java.lang.String} object.
     * @should return false if value null
     * @should return true if value true
     * @return a boolean.
     */
    public boolean isFacetExpanded(String field) {
        return facetsExpanded.get(field) != null && facetsExpanded.get(field);
    }

    /**
     * <p>
     * isFacetCollapsed.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean isFacetCollapsed(String field) {
        return !isFacetExpanded(field);
    }

    /**
     * <p>
     * getAllAvailableFacets.
     * </p>
     *
     * @should return all facet items in correct order
     * @return a {@link java.util.Map} object.
     */
    public Map<String, List<IFacetItem>> getAllAvailableFacets() {
        Map<String, List<IFacetItem>> ret = new LinkedHashMap<>();

        List<String> allFacetFields = DataManager.getInstance().getConfiguration().getAllFacetFields();
        
        for (String field : allFacetFields) {
            if (availableFacets.containsKey(field)) {
                ret.put(field, availableFacets.get(field));
            }
        }

        synchronized (lock) {
            //add current facets which have no hits. This may happen due to geomap facetting
            List<IFacetItem> currentFacetsLocal = new ArrayList<>(currentFacets);
            for (IFacetItem currentItem : currentFacetsLocal) {
                // Make a copy of the list to avoid concurrent modification
                List<IFacetItem> availableFacetItems = new ArrayList<>(ret.getOrDefault(currentItem.getField(), new ArrayList<>()));
                if (!availableFacetItems.contains(currentItem)) {
                    availableFacetItems.add(currentItem);
                    ret.put(currentItem.getField(), availableFacetItems);
                }
            }

            return ret;
        }

    }

    /**
     * <p>
     * getConfiguredSubelementFacetFields.
     * </p>
     *
     * @return Configured subelement fields names only
     */
    public List<String> getConfiguredSubelementFacetFields() {
        List<String> ret = new ArrayList<>();

        for (String field : DataManager.getInstance().getConfiguration().getAllFacetFields()) {
            if (SolrConstants.DOCSTRCT_SUB.equals(field)) {
                ret.add(SearchHelper.facetifyField(field));
            }
        }

        return ret;
    }

    /**
     * <p>
     * Getter for the field <code>availableFacets</code>.
     * </p>
     *
     * @return the availableFacets
     */
    public Map<String, List<IFacetItem>> getAvailableFacets() {
        return availableFacets;
    }

    /**
     * <p>
     * Getter for the field <code>currentFacets</code>.
     * </p>
     *
     * @return the currentFacets
     */
    public synchronized List<IFacetItem> getCurrentFacets() {
        return currentFacets;
    }

    /**
     * <p>
     * Getter for the field <code>tempValue</code>.
     * </p>
     *
     * @return the tempValue
     */
    public String getTempValue() {
        return tempValue;
    }

    /**
     * <p>
     * Setter for the field <code>tempValue</code>.
     * </p>
     *
     * @param tempValue the tempValue to set
     */
    public void setTempValue(String tempValue) {
        this.tempValue = tempValue;
    }

    /**
     * Returns true if the given <code>field</code> is language-specific to a different language than the given <code>language</code>.
     *
     * @param field a {@link java.lang.String} object.
     * @param language a {@link java.lang.String} object.
     * @should return true if language code different
     * @should return false if language code same
     * @should return false if no language code
     * @return a boolean.
     */
    public boolean isHasWrongLanguageCode(String field, String language) {
        if (field == null) {
            throw new IllegalArgumentException("field may not be null");
        }
        if (language == null) {
            throw new IllegalArgumentException("language may not be null");
        }
        
        return field.contains(SolrConstants._LANG_) && !field.endsWith(SolrConstants._LANG_ + language.toUpperCase());
    }

    /**
     * <p>
     * getFacetValue.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getFacetValue(String field) {
        return getCurrentFacets().stream().filter(facet -> facet.getField().equals(field)).map(facet -> getFacetName(facet)).findFirst().orElse("");
    }

    /**
     * <p>
     * getFacetDescription.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getFacetDescription(String field) {
        return getCurrentFacets().stream()
                .filter(facet -> facet.getField().equals(field))
                .map(facet -> getFacetDescription(facet))
                .findFirst()
                .orElse("");
    }

    /**
     * <p>
     * getFirstHierarchicalFacetValue.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getFirstHierarchicalFacetValue() {
        return getCurrentFacets().stream().filter(facet -> facet.isHierarchial()).map(facet -> getFacetName(facet)).findFirst().orElse("");
    }

    /**
     * <p>
     * getFirstHierarchicalFacetDescription.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getFirstHierarchicalFacetDescription(String field) {
        return getCurrentFacets().stream().filter(facet -> facet.isHierarchial()).map(facet -> getFacetDescription(facet)).findFirst().orElse("");
    }

    /**
     * @param facet
     * @return
     */
    private static String getFacetDescription(IFacetItem facet) {
        String desc = "";
        try {
            CMSCollection cmsCollection = DataManager.getInstance().getDao().getCMSCollection(facet.getField(), facet.getValue());
            if (cmsCollection != null) {
                desc = cmsCollection.getDescription();
            }
        } catch (DAOException e) {
            logger.trace("Error retrieving cmsCollection from DAO");
        }
        return desc;
    }

    /**
     * @param facet
     * @return
     */
    private static String getFacetName(IFacetItem facet) {
        if (facet == null) {
            return "";
        }

        return facet.getValue();
    }

    /**
     * @return the labelMap
     */
    public Map<String, String> getLabelMap() {
        return labelMap;
    }

    /**
     * @return the geoFacetting
     */
    public GeoFacetItem getGeoFacetting() {
        synchronized (lock) {
            return new ArrayList<>(this.currentFacets)
                    .stream()
                    .filter(f -> f instanceof GeoFacetItem)
                    .map(f -> (GeoFacetItem) f)
                    .findAny()
                    .orElse(new GeoFacetItem(DataManager.getInstance().getConfiguration().getGeoFacetFields()));
        }
    }

    /**
     * Sets the feature of the geoFacettingfield to to given feature. A new GeoFacetItem is added to currentFacets if none exists yet
     *
     * @param feature
     */
    public void setGeoFacetFeature(String feature) {
        GeoFacetItem item = getGeoFacetting();
        item.setField(DataManager.getInstance().getConfiguration().getGeoFacetFields());
        synchronized (lock) {
            if (StringUtils.isBlank(feature)) {
                this.currentFacets.remove(item);
            } else {
                item.setFeature(feature);
                if (!this.currentFacets.contains(item)) {
                    this.currentFacets.add(item);
                }
            }
        }
    }

    public String getGeoFacetFeature() {
        if (this.getGeoFacetting().isActive()) {
            return this.getGeoFacetting().getFeature();
        }
        return "";
    }
}
