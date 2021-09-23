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

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.managedbeans.CmsCollectionsBean.CMSCollectionImageMode;
import io.goobi.viewer.model.cms.CMSCollection;
import io.goobi.viewer.model.cms.CMSMediaItem;
import io.goobi.viewer.model.translations.admin.MessageEntry;
import io.goobi.viewer.solr.SolrConstants;

public class CmsCollectionsBeanTest extends AbstractDatabaseAndSolrEnabledTest {

    /**
     * @see CmsCollectionsBean#initImageMode()
     * @verifies set imageMode correctly
     */
    @Test
    public void initImageMode_shouldSetImageModeCorrectly() throws Exception {
        CmsCollectionsBean bean = new CmsCollectionsBean();
        bean.setCurrentCollection(new CMSCollection(SolrConstants.DC, "varia"));

        bean.getCurrentCollection().setRepresentativeWorkPI("PPN123");
        bean.initImageMode();
        Assert.assertEquals(CMSCollectionImageMode.PI, bean.getImageMode());

        bean.getCurrentCollection().setRepresentativeWorkPI(null);
        bean.initImageMode();
        Assert.assertEquals(CMSCollectionImageMode.NONE, bean.getImageMode());

        bean.getCurrentCollection().setMediaItem(new CMSMediaItem());
        bean.initImageMode();
        Assert.assertEquals(CMSCollectionImageMode.IMAGE, bean.getImageMode());
    }

    /**
     * @see CmsCollectionsBean#isDisplayTranslationWidget()
     * @verifies return false if solrField not among configured translation groups
     */
    @Test
    public void isDisplayTranslationWidget_shouldReturnFalseIfSolrFieldNotAmongConfiguredTranslationGroups() throws Exception {
        CmsCollectionsBean bean = new CmsCollectionsBean();
        bean.solrField = "MD_NOPE"; // Do not use the setter, that'd require more test infrastructure
        Assert.assertFalse(bean.isDisplayTranslationWidget());
    }

    /**
     * @see CmsCollectionsBean#isDisplayTranslationWidget()
     * @verifies return true if solrField values not or partially translated
     */
    @Test
    public void isDisplayTranslationWidget_shouldReturnTrueIfSolrFieldValuesNotOrPartiallyTranslated() throws Exception {
        CmsCollectionsBean bean = new CmsCollectionsBean();
        bean.solrField = SolrConstants.DC; // Do not use the setter, that'd require more test infrastructure
        Assert.assertTrue(bean.isDisplayTranslationWidget());
    }

    /**
     * @see CmsCollectionsBean#isDisplaySolrFieldSelectionWidget()
     * @verifies return false if only one collection field is configured
     */
    @Test
    public void isDisplaySolrFieldSelectionWidget_shouldReturnFalseIfOnlyOneCollectionFieldIsConfigured() throws Exception {
        CmsCollectionsBean bean = new CmsCollectionsBean();
        Assert.assertEquals(3, bean.getAllCollectionFields().size());

        Assert.assertTrue(bean.isDisplaySolrFieldSelectionWidget());
    }

    /**
     * @see CmsCollectionsBean#isDisplayTranslationWidgetEdit()
     * @verifies return false if solrField not among configured translation groups
     */
    @Test
    public void isDisplayTranslationWidgetEdit_shouldReturnFalseIfSolrFieldNotAmongConfiguredTranslationGroups() throws Exception {
        CmsCollectionsBean bean = new CmsCollectionsBean();
        bean.solrField = "MD_NOPE"; // Do not use the setter, that'd require more test infrastructure
        Assert.assertFalse(bean.isDisplayTranslationWidgetEdit());
    }

    /**
     * @see CmsCollectionsBean#isDisplayTranslationWidgetEdit()
     * @verifies return true if solrFieldValue not or partially translated
     */
    @Test
    public void isDisplayTranslationWidgetEdit_shouldReturnTrueIfSolrFieldValueNotOrPartiallyTranslated() throws Exception {
        CmsCollectionsBean bean = new CmsCollectionsBean();
        bean.solrField = SolrConstants.DC; // Do not use the setter, that'd require more test infrastructure
        bean.setSolrFieldValue("dcmetadata");
        Assert.assertTrue(bean.isDisplayTranslationWidgetEdit());
    }

    /**
     * @see CmsCollectionsBean#getMessageEntryForFieldValue()
     * @verifies return empty MessageEntry if none found
     */
    @Test
    public void getMessageEntryForFieldValue_shouldReturnEmptyMessageEntryIfNoneFound() throws Exception {
        CmsCollectionsBean bean = new CmsCollectionsBean();
        bean.solrField = "foo";
        bean.setSolrFieldValue("bar");
        MessageEntry entry = bean.getMessageEntryForFieldValue();
        Assert.assertNotNull(entry);
        Assert.assertEquals(2, entry.getValues().size());
    }
}