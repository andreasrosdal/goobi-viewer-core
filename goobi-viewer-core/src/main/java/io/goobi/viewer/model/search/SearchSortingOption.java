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
package io.goobi.viewer.model.search;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.solr.SolrConstants;

/**
 * @author florian
 *
 */
public class SearchSortingOption {

    private static final String DEFAULT_SORT_FIELD_LABEL = "searchSortingDropdown_relevance";

    private final String field;
    private final boolean ascending;

    /**
     * Constructor for default sorting
     */
    public SearchSortingOption() {
        this.field = "";
        this.ascending = true;
    }

    /**
     * Constructor for sort field
     * 
     * @param field
     * @param ascending
     */
    public SearchSortingOption(String field, boolean ascending) {
        this.field = field;
        this.ascending = ascending;
    }

    /**
     * @return the field
     */
    public String getField() {
        return field;
    }

    /**
     * @return the ascending
     */
    public boolean isAscending() {
        return ascending;
    }

    public boolean isDescending() {
        return !ascending;
    }

    public String getLabel() {
        if (SolrConstants.SORT_RANDOM.equalsIgnoreCase(field)) {
            return ViewerResourceBundle.getTranslation(field, null);
        } else if (StringUtils.isNotBlank(field)) {
            return ViewerResourceBundle.getTranslation(field, null) + " " + ViewerResourceBundle.getTranslation(getSearchSortingKey(), null);
        } else {
            return ViewerResourceBundle.getTranslation(DEFAULT_SORT_FIELD_LABEL, null);
        }
    }

    public String getSortString() {
        if (StringUtils.isNotBlank(field)) {
            return (isDescending() ? "!" : "") + field;
        }
        return "";
    }

    private String getSearchSortingKey() {
        if (isAscending()) {
            return getSearchSortingAscendingKey(field);
        }
        return getSearchSortingDescendingKey(field);
    }

    private static String getSearchSortingAscendingKey(String field) {
        return DataManager.getInstance().getConfiguration().getSearchSortingKeyAscending(field).orElse("searchSortingDropdown_ascending");
    }

    private static String getSearchSortingDescendingKey(String field) {
        return DataManager.getInstance().getConfiguration().getSearchSortingKeyDescending(field).orElse("searchSortingDropdown_descending");
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.field.hashCode();
    }

    /**
     * Two SearchSortingOptions are equal if they either both have an empty {@link #getField()} or if both {@link #getField()} and
     * {@link #isAscending()} are equal
     * @should return true if both options are random
     */
    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj.getClass().equals(this.getClass())) {
            SearchSortingOption other = (SearchSortingOption) obj;
            if (StringUtils.isBlank(this.field)) {
                return StringUtils.isBlank(other.field);
            }
//            if (this.getField().equals(SolrConstants.SORT_RANDOM)) {
//                return other.getSortString().startsWith("random_");
//            }
//            if (other.getField().equals(SolrConstants.SORT_RANDOM)) {
//                return this.getSortString().startsWith("random_");
//            }
            return StringUtils.equals(this.getField(), other.getField()) && this.isAscending() == other.isAscending();
        }
        return false;
    }

    @Override
    public String toString() {
        return getSortString();
    }
}
