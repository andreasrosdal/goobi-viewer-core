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

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.termbrowsing.BrowsingMenuFieldConfig;

public class BrowsingMenuFieldConfigTest {
    /**
     * @see BrowsingMenuFieldConfig#setDocstructFilterString(String)
     * @verifies create filter query correctly
     */
    @Test
    public void setDocstructFilterString_shouldCreateFilterQueryCorrectly() throws Exception {
        BrowsingMenuFieldConfig bmfc =
                new BrowsingMenuFieldConfig("MD_TITLE", "SORT_TITLE", "+(DOCSTRCT:monograph DOCSTRCT:manuscript)", false, false);
        Assert.assertEquals(1, bmfc.getFilterQueries().size());
        Assert.assertEquals("+(DOCSTRCT:monograph DOCSTRCT:manuscript)", bmfc.getFilterQueries().get(0));
    }

    /**
     * @see BrowsingMenuFieldConfig#setRecordsAndAnchorsOnly(boolean)
     * @verifies create filter query correctly
     */
    @Test
    public void setRecordsAndAnchorsOnly_shouldCreateFilterQueryCorrectly() throws Exception {
        BrowsingMenuFieldConfig bmfc = new BrowsingMenuFieldConfig("MD_TITLE", "SORT_TITLE", null, false, true);
        Assert.assertEquals(1, bmfc.getFilterQueries().size());
        Assert.assertEquals(SearchHelper.ALL_RECORDS_QUERY, bmfc.getFilterQueries().get(0));
    }
}