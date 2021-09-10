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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.solr.SolrConstants;

public class FacetItemTest extends AbstractTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * @see FacetItem#FacetItem(String)
     * @verifies split field and value correctly
     */
    @Test
    public void FacetItem_shouldSplitFieldAndValueCorrectly() throws Exception {
        IFacetItem item = new FacetItem("FIELD:value:1:2:3", false);
        Assert.assertEquals("FIELD", item.getField());
        Assert.assertEquals("value:1:2:3", item.getValue());
    }

    /**
     * @see FacetItem#FacetItem(String,boolean)
     * @verifies split field and value range correctly
     */
    @Test
    public void FacetItem_shouldSplitFieldAndValueRangeCorrectly() throws Exception {
        IFacetItem item = new FacetItem("FIELD:[foo TO bar]", false);
        Assert.assertEquals("FIELD", item.getField());
        Assert.assertEquals("foo", item.getValue());
        Assert.assertEquals("bar", item.getValue2());
    }

    /**
     * @see FacetItem#getQueryEscapedLink()
     * @verifies construct link correctly
     */
    @Test
    public void getQueryEscapedLink_shouldConstructLinkCorrectly() throws Exception {
        IFacetItem item = new FacetItem("FIELD:value", false);
        Assert.assertEquals("FIELD:value", item.getQueryEscapedLink());
    }

    /**
     * @see FacetItem#getQueryEscapedLink()
     * @verifies escape values containing whitespaces
     */
    @Test
    public void getQueryEscapedLink_shouldEscapeValuesContainingWhitespaces() throws Exception {
        IFacetItem item = new FacetItem("FIELD:foo bar", false);
        Assert.assertEquals("FIELD:\"foo\\ bar\"", item.getQueryEscapedLink());
    }

    /**
     * @see FacetItem#getQueryEscapedLink()
     * @verifies construct hierarchical link correctly
     */
    @Test
    public void getQueryEscapedLink_shouldConstructHierarchicalLinkCorrectly() throws Exception {
        IFacetItem item = new FacetItem("FIELD:value", true);
        Assert.assertEquals("(FIELD:value OR FIELD:value.*)", item.getQueryEscapedLink());
    }

    /**
     * @see FacetItem#getQueryEscapedLink()
     * @verifies construct range link correctly
     */
    @Test
    public void getQueryEscapedLink_shouldConstructRangeLinkCorrectly() throws Exception {
        IFacetItem item = new FacetItem("FIELD:[foo TO bar]", false);
        Assert.assertEquals("FIELD:[foo TO bar]", item.getQueryEscapedLink());
    }

    //    /**
    //     * @see FacetItem#getQueryEscapedLink()
    //     * @verifies construct polygon link correctly
    //     */
    //    @Test
    //    public void getQueryEscapedLink_shouldConstructPolygonLinkCorrectly() throws Exception {
    //        FacetItem item = new FacetItem("WKT_COORDS:0 0, 0 90, 90 90, 90 0, 0 0", false);
    //        Assert.assertEquals("WKT_:\"IsWithin(POLYGON((0 0, 0 90, 90 90, 90 0, 0 0))) distErrPct=0\"", item.getQueryEscapedLink());
    //    }

    /**
     * @see FacetItem#generateFacetItems(String,Map,boolean,boolean,boolean)
     * @verifies sort items correctly
     */
    @Test
    public void generateFacetItems_shouldSortItemsCorrectly() throws Exception {
        Map<String, Long> values = new TreeMap<>();
        values.put("Monograph", 1L);
        values.put("Article", 5L);
        values.put("Volume", 3L);
        {
            // asc
            List<IFacetItem> items = FacetItem.generateFacetItems(SolrConstants.DOCSTRCT, values, true, false, false, null);
            Assert.assertEquals(3, items.size());
            Assert.assertEquals("Article", items.get(0).getLabel());
            Assert.assertEquals("Monograph", items.get(1).getLabel());
            Assert.assertEquals("Volume", items.get(2).getLabel());
        }
        {
            // desc
            List<IFacetItem> items = FacetItem.generateFacetItems(SolrConstants.DOCSTRCT, values, true, true, false, null);
            Assert.assertEquals(3, items.size());
            Assert.assertEquals("Article", items.get(2).getLabel());
            Assert.assertEquals("Monograph", items.get(1).getLabel());
            Assert.assertEquals("Volume", items.get(0).getLabel());
        }
    }

    /**
     * @see FacetItem#getFullValue()
     * @verifies build full value correctly
     */
    @Test
    public void getFullValue_shouldBuildFullValueCorrectly() throws Exception {
        IFacetItem item = new FacetItem("FIELD:[foo TO bar]", false);
        Assert.assertEquals("foo", item.getValue());
        Assert.assertEquals("bar", item.getValue2());
        Assert.assertEquals("foo - bar", item.getFullValue());
    }

    /**
     * @see FacetItem#getEscapedValue(String)
     * @verifies escape value correctly
     */
    @Test
    public void getEscapedValue_shouldEscapeValueCorrectly() throws Exception {
        Assert.assertEquals("\\(foo\\)", FacetItem.getEscapedValue("(foo)"));
    }

    /**
     * @see FacetItem#getEscapedValue(String)
     * @verifies add quotation marks if value contains space
     */
    @Test
    public void getEscapedValue_shouldAddQuotationMarksIfValueContainsSpace() throws Exception {
        Assert.assertEquals("\"foo\\ bar\"", FacetItem.getEscapedValue("foo bar"));
    }

    /**
     * @see FacetItem#getEscapedValue(String)
     * @verifies preserve leading and trailing quotation marks
     */
    @Test
    public void getEscapedValue_shouldPreserveLeadingAndTrailingQuotationMarks() throws Exception {
        Assert.assertEquals("\"IsWithin\\(foobar\\)\\ disErrPct=0\"", FacetItem.getEscapedValue("\"IsWithin(foobar) disErrPct=0\""));
    }

    /**
     * @see FacetItem#compareTo(FacetItem)
     * @verifies return plus if count less than other count
     */
    @Test
    public void compareTo_shouldReturnPlusIfCountLessThanOtherCount() throws Exception {
        FacetItem facetItem1 = new FacetItem("field:foo", false).setCount(1);
        FacetItem facetItem2 = new FacetItem("field:foo", false).setCount(2);
        Assert.assertEquals(1, new FacetItem.CountComparator().compare(facetItem1, facetItem2));
    }

    /**
     * @see FacetItem#compareTo(FacetItem)
     * @verifies return minus if count more than other count
     */
    @Test
    public void compareTo_shouldReturnMinusIfCountMoreThanOtherCount() throws Exception {
        FacetItem facetItem1 = new FacetItem("field:foo", false).setCount(2);
        FacetItem facetItem2 = new FacetItem("field:foo", false).setCount(1);
        Assert.assertEquals(-1, new FacetItem.CountComparator().compare(facetItem1, facetItem2));
    }

    /**
     * @see FacetItem#compareTo(FacetItem)
     * @verifies compare by label if count equal
     */
    @Test
    public void compareTo_shouldCompareByLabelIfCountEqual() throws Exception {
        {
            FacetItem facetItem1 = new FacetItem("field:foo", false).setLabel("foo").setCount(1);
            FacetItem facetItem2 = new FacetItem("field:bar", false).setLabel("bar").setCount(1);
            Assert.assertTrue(new FacetItem.CountComparator().compare(facetItem1, facetItem2) > 0);
        }
        {
            FacetItem facetItem1 = new FacetItem("field:bar", false).setLabel("bar").setCount(1);
            FacetItem facetItem2 = new FacetItem("field:foo", false).setLabel("foo").setCount(1);
            Assert.assertTrue(new FacetItem.CountComparator().compare(facetItem1, facetItem2) < 0);
        }
        {
            FacetItem facetItem1 = new FacetItem("field:foo", false).setLabel("foo").setCount(1);
            FacetItem facetItem2 = new FacetItem("field:foo", false).setLabel("foo").setCount(1);
            Assert.assertEquals(0, new FacetItem.CountComparator().compare(facetItem1, facetItem2));
        }
    }

    /**
     * @see FacetItem#generateFilterLinkList(String,Map,boolean,Locale,Map)
     * @verifies set label from separate field if configured and found
     */
    @Test
    public void generateFilterLinkList_shouldSetLabelFromSeparateFieldIfConfiguredAndFound() throws Exception {
        Map<String, String> labelMap = new HashMap<>(1);
        List<IFacetItem> facetItems =
                FacetItem.generateFilterLinkList("MD_CREATOR", Collections.singletonMap("Groos, Karl", 1L), false, null, labelMap);
        Assert.assertEquals(1, facetItems.size());
        Assert.assertEquals("Karl", facetItems.get(0).getLabel());
    }

    /**
     * @see FacetItem#parseLink(String)
     * @verifies set label to value if label empty
     */
    @Test
    public void parseLink_shouldSetLabelToValueIfLabelEmpty() throws Exception {
       FacetItem item = new FacetItem(false);
       Assert.assertNull(item.getLabel());
       item.setLink("foo:bar");
       item.parseLink();
       Assert.assertEquals("bar", item.getLabel());
    }
}