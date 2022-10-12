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
package io.goobi.viewer.model.cms.pages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.pages.content.CMSComponent;
import io.goobi.viewer.model.cms.pages.content.CMSPageContentManager;
import io.goobi.viewer.model.cms.pages.content.PersistentCMSComponent;
import io.goobi.viewer.model.cms.pages.content.types.CMSTextContent;

public class CMSPageTest extends AbstractDatabaseEnabledTest {

    Path componentTemplatesPath = Paths.get("src/test/resources/data/viewer/cms/component_templates");
    CMSPageContentManager contentManager;
    IDAO dao;
    
    @Before
    public void setup() throws Exception { 
        super.setUp();
        dao = DataManager.getInstance().getDao();
        contentManager = new CMSPageContentManager(componentTemplatesPath);
    }
    
    @Test
    public void testPersistPage() throws DAOException {
                
        CMSPage page = new CMSPage();
        page.getTitleTranslations().setValue("Titel", Locale.ENGLISH);
        assertEquals("Titel", page.getTitle(Locale.ENGLISH));
        
        assertTrue(dao.addCMSPage(page));
        
        CMSPage loaded = dao.getCMSPage(page.getId());
        
        assertEquals("Titel", loaded.getTitle(Locale.ENGLISH));
        
        CMSPage cloned = new CMSPage(loaded);
        
        assertEquals("Titel", cloned.getTitle(Locale.ENGLISH));
    }
    
    @Test
    public void testPersistPageWithContent() throws DAOException {
                
        CMSPage page = new CMSPage();
        page.getTitleTranslations().setValue("Titel", Locale.ENGLISH);
        
        CMSComponent textComponent = contentManager.getComponent("text").orElse(null);
        assertNotNull(textComponent);
        PersistentCMSComponent textComponentInPage = page.addComponent(textComponent);
        CMSTextContent textContent = (CMSTextContent) textComponentInPage.getContentItems().get(0);
        textContent.getText().setText("Entered Text", Locale.ENGLISH);
        
        assertTrue(dao.addCMSPage(page));
        assertTrue(page.removeComponent(page.getAsCMSComponent(textComponentInPage)));
        
        CMSPage loaded = dao.getCMSPage(page.getId());
        CMSPage cloned = new CMSPage(loaded);
        
        CMSTextContent clonedTextContent = (CMSTextContent)cloned.getPersistentComponents().get(0).getContentItems().get(0);
        assertEquals("Entered Text", clonedTextContent.getText().getText(Locale.ENGLISH));
    }

}
