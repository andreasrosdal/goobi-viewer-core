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

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.TestUtils;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.CMSMediaItem;

public class CmsMediaBeanTest extends AbstractDatabaseEnabledTest {

    CmsMediaBean bean;

    /**
     * @throws java.lang.Exception
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        bean = new CmsMediaBean();

        UserBean userBean = new UserBean();
        userBean.setAuthenticationProvider(TestUtils.testAuthenticationProvider);
        userBean.setEmail("1@users.org");
        userBean.setPassword("abcdef1");
        userBean.login();
        bean.userBean = userBean;
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testSelectedTag() {
        String tag = "sampleTag";
        bean.setSelectedTag(tag);
        Assert.assertEquals(tag, bean.getSelectedTag());
    }

    @Test
    public void testGetAllMediaCategories() throws DAOException {
        List<CMSCategory> tags = bean.getAllMediaCategories();
        Assert.assertEquals(7, tags.size());
    }

    @Test
    public void testGetMediaItems() throws DAOException {

        bean.setFilter("");
        Assert.assertEquals(4, bean.getMediaItems().size());
        bean.setFilter("tag1");
        Assert.assertEquals(3, bean.getMediaItems().size());
        bean.setFilter("");
        bean.setFilenameFilter(bean.getImageFilter());
        Assert.assertEquals(4, bean.getMediaItems().size());
        bean.setFilenameFilter(".*\\.xml");
        Assert.assertEquals(0, bean.getMediaItems().size());
    }

    @Test
    public void testGetImageFilter() {
        String file1 = "image.jpg";
        String file2 = "image.JPEG";
        String file3 = "image.xml";
        Assert.assertTrue(file1.matches(bean.getImageFilter()));
        Assert.assertTrue(file2.matches(bean.getImageFilter()));
        Assert.assertFalse(file3.matches(bean.getImageFilter()));
    }

    @Test
    public void testGetMediaUrlForGif() throws NumberFormatException, ViewerConfigurationException {
        CMSMediaItem item = new CMSMediaItem();
        item.setFileName("lorelai.gif");
        String url = bean.getMediaUrl(item);
        assertTrue(url.endsWith("lorelai.gif/full.gif"));
    }
}
