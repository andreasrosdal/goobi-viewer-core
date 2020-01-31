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
package io.goobi.viewer.model.cms;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.TestUtils;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.CmsBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.CMSContentItem;
import io.goobi.viewer.model.cms.CMSMediaItem;
import io.goobi.viewer.model.cms.CMSPage;
import io.goobi.viewer.model.cms.CMSPageLanguageVersion;
import io.goobi.viewer.model.cms.CMSTemplateManager;
import io.goobi.viewer.model.cms.CMSContentItem.CMSContentItemType;
import io.goobi.viewer.model.cms.CMSPageLanguageVersion.CMSPageStatus;
import io.goobi.viewer.servlets.rest.cms.CMSContentResource;

//@RunWith(PowerMockRunner.class)
//@PrepareForTest(BeanUtils.class)
public class CMSPageTest extends AbstractDatabaseEnabledTest {

    /**
     * @throws java.lang.Exception
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
//        FacesContext facesContext = TestUtils.mockFacesContext();
//        ServletContext servletContext = (ServletContext) facesContext.getExternalContext().getContext();
//        Mockito.when(servletContext.getRealPath("/")).thenReturn("src/META-INF/resources/");

        DataManager.getInstance().injectConfiguration(new Configuration("src/test/resources/config_viewer.test.xml"));
        
        File webContent = new File("WebContent/").getAbsoluteFile();
        String webContentPath = webContent.toURI().toString();
        //        if (webContentPath.startsWith("file:/")) {
        //            webContentPath = webContentPath.replace("file:/", "");
        //        }
        CMSTemplateManager.getInstance(webContentPath, null);
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
    public void testGetTileGridUrl() {

        String allowedTags = "a$b$cde";
        List<CMSCategory> categories = new ArrayList<>();
        for (String catName : allowedTags.split("\\$")) {
			categories.add(new CMSCategory(catName));
		}
        boolean preferImportant = true;
        int numTiles = 12;

        CMSPage page = new CMSPage();
        CMSPageLanguageVersion global = new CMSPageLanguageVersion();
        global.setLanguage("global");
        global.setOwnerPage(page);

        CMSPageLanguageVersion de = new CMSPageLanguageVersion();
        de.setLanguage("de");
        de.setOwnerPage(page);

        page.getLanguageVersions().add(global);
        page.getLanguageVersions().add(de);

        CMSContentItem gridItem = new CMSContentItem();
        gridItem.setCategories(categories);
        gridItem.setNumberOfImportantTiles(preferImportant ? numTiles : 0);
        gridItem.setNumberOfTiles(numTiles);
        gridItem.setId(1l);
        gridItem.setItemId("grid01");
        gridItem.setType(CMSContentItemType.TILEGRID);
        global.addContentItem(gridItem);

        try {
            String url = page.getTileGridUrl("grid01");
            String viewerUrl = BeanUtils.getServletPathWithHostAsUrlFromJsfContext();
            String language = CmsBean.getCurrentLocale().getLanguage();
            String expecedUrl = viewerUrl + "/rest/tilegrid/" + language + "/" + numTiles + "/" + numTiles + "/" + allowedTags + "/";
            //            expecedUrl = expecedUrl.replace("//", "/");
            Assert.assertEquals(expecedUrl, url);
        } catch (IllegalRequestException e) {
            fail("Item not found");
        }

    }

    @Test
    public void testCMSPage() throws DAOException, IOException, ServletException, URISyntaxException, ViewerConfigurationException {
        //setup
        CMSPage page = new CMSPage();
        String templateId = "template";
        page.setTemplateId(templateId);

        CMSPageLanguageVersion german = new CMSPageLanguageVersion();
        german.setLanguage("de");
        german.setStatus(CMSPageStatus.FINISHED);
        page.addLanguageVersion(german);
        CMSPageLanguageVersion global = new CMSPageLanguageVersion();
        global.setLanguage("global");
        page.addLanguageVersion(global);

        Date created = new Date();
        created.setYear(created.getYear() - 2);
        Date updated = new Date();
        page.setDateCreated(created);
        page.setDateUpdated(updated);

        page.setCategories(DataManager.getInstance().getDao().getAllCategories());

        String altUrl = "test/page/";
        page.setPersistentUrl(altUrl);

        String textContent = "Text";
        String textId = "text";
        CMSContentItem text = new CMSContentItem(CMSContentItemType.TEXT);
        text.setItemId(textId);
        text.setHtmlFragment(textContent);
        page.addContentItem(text);

        String htmlContent = "<div>Content</div>";
        String htmlId = "html";
        CMSContentItem html = new CMSContentItem(CMSContentItemType.HTML);
        html.setItemId(htmlId);
        html.setHtmlFragment(htmlContent);
        page.addContentItem(html);

        CMSMediaItem media = new CMSMediaItem();
        String mediaFilename = "image 01.jpg";
        media.setFileName(mediaFilename);
        String imageId = "image";
        CMSContentItem image = new CMSContentItem(CMSContentItemType.MEDIA);
        image.setItemId(imageId);
        image.setMediaItem(media);
        page.addContentItem(image);

        String componentName = "sampleComponent";
        String componentId = "component";
        CMSContentItem component = new CMSContentItem(CMSContentItemType.COMPONENT);
        component.setItemId(componentId);
        component.setComponent(componentName);
        page.addContentItem(component);

        DataManager.getInstance().getDao().addCMSMediaItem(media);
        Long mediaId = media.getId();
        DataManager.getInstance().getDao().addCMSPage(page);
        Long pageId = page.getId();

        german.generateCompleteContentItemList();

        //tests
        Assert.assertEquals(created, page.getDateCreated());
        Assert.assertEquals(updated, page.getDateUpdated());
        Assert.assertEquals(DataManager.getInstance().getDao().getAllCategories(), page.getCategories());
        Assert.assertEquals(altUrl, page.getRelativeUrlPath(true));
        Assert.assertEquals("cms/" + pageId + "/", page.getRelativeUrlPath(false));

        Assert.assertEquals(textContent, page.getContent(textId));

        String htmlUrl = page.getContent(htmlId);
        Path htmlUrlPath = Paths.get(new URI(htmlUrl).getPath());
        String htmlResponse = new CMSContentResource().getContentHtml(Long.parseLong(htmlUrlPath.subpath(3, 4).toString()),
                htmlUrlPath.subpath(4, 5).toString(), htmlUrlPath.subpath(5, 6).toString());
        Assert.assertEquals("<span>" + htmlContent + "</span>", htmlResponse);

        String contentServerUrl = DataManager.getInstance().getConfiguration().getRestApiUrl();

        String filePath = media.getImageURI();
        filePath = BeanUtils.escapeCriticalUrlChracters(filePath, false);

        String imageUrl = contentServerUrl + "image/-/" + filePath + "/full/max/0/default.jpg";
        Assert.assertEquals(imageUrl, page.getContent(imageId).replaceAll("\\?.*", ""));
        Assert.assertEquals(componentName, page.getContent(componentId));

    }

}