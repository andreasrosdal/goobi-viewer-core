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
package io.goobi.viewer.model.annotation;

import static org.junit.Assert.*;

import java.awt.Rectangle;
import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.Query;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.intranda.api.annotation.AgentType;
import de.intranda.api.annotation.IResource;
import de.intranda.api.annotation.wa.Agent;
import de.intranda.api.annotation.wa.FragmentSelector;
import de.intranda.api.annotation.wa.SpecificResource;
import de.intranda.api.annotation.wa.TextualResource;
import de.intranda.api.annotation.wa.WebAnnotation;
import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.impl.JPADAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.security.user.User;

/**
 * @author florian
 *
 */
public class PersistentAnnotationTest  extends AbstractDatabaseEnabledTest {

    private WebAnnotation annotation;
    private PersistentAnnotation daoAnno;
    private User creator;
    private Campaign generator;
    private IResource body;
    private IResource target;
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();
        
        creator = DataManager.getInstance().getDao().getUser(2);

        generator = new Campaign();
        generator.setId(4l);

        annotation = new WebAnnotation(URI.create("http://www.example.com/anno/1"));
        annotation.setCreated(new Date(2019, 01, 22, 12, 54));
        annotation.setModified(new Date(2019, 8, 11, 17, 13));
        annotation.setCreator(new Agent(URI.create(creator.getId().toString()), AgentType.PERSON, creator.getNickName()));
        annotation.setGenerator(new Agent(URI.create(generator.getId().toString()), AgentType.SOFTWARE, ""));

        body = new TextualResource("annotation text");
        target = new SpecificResource(URI.create("http://www.example.com/manifest/7/canvas/10"),
                new FragmentSelector(new Rectangle(10, 20, 100, 200)));

        annotation.setBody(body);
        annotation.setTarget(target);

        daoAnno = new PersistentAnnotation(annotation);

    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testIdConversion() throws JsonParseException, JsonMappingException, IOException {
       
        URI webAnnoURI = URI.create(DataManager.getInstance().getConfiguration().getRestApiUrl() + "annotations/562");
        
        PersistentAnnotation persAnno = new PersistentAnnotation();
        persAnno.setId(PersistentAnnotation.getId(webAnnoURI));
        
        Assert.assertEquals(persAnno.getId(), 562l, 0l);
        
        WebAnnotation webAnno = persAnno.getAsAnnotation();
        
        Assert.assertEquals(webAnno.getId(), webAnnoURI);
        
    }
    
    @Test
    public void testSerialize() throws DAOException, JsonParseException, JsonMappingException, IOException {

        String bodyString = daoAnno.getBody();
        Assert.assertEquals("{\"type\":\"TextualBody\",\"format\":\"text/plain\",\"value\":\"annotation text\"}", bodyString);

        String targetString = daoAnno.getTarget();
        Assert.assertEquals(
                "{\"type\":\"SpecificResource\",\"selector\":{\"value\":\"xywh=10,20,100,200\",\"type\":\"FragmentSelector\"},\"source\":\"http://www.example.com/manifest/7/canvas/10\"}"
                        + "",
                targetString);

        IResource retrievedBody = daoAnno.getBodyAsResource();
        Assert.assertEquals(body, retrievedBody);

        IResource retrievedTarget = daoAnno.getTargetAsResource();
        Assert.assertEquals(target, retrievedTarget);

    }

    @Test
    public void testSave() throws DAOException, JsonParseException, JsonMappingException, IOException {

        JPADAO dao = (JPADAO) DataManager.getInstance().getDao();
    
        EntityManager em = dao.getFactory().createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(this.daoAnno);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
        
        em = dao.getFactory().createEntityManager();
        try {
        Query q = em.createQuery("SELECT c FROM PersistentAnnotation c");
        q.setFlushMode(FlushModeType.COMMIT);
        List<PersistentAnnotation> list = q.getResultList();
        Assert.assertEquals(1, list.size());
        
        PersistentAnnotation retrieved = list.get(0);
        Assert.assertEquals(body, retrieved.getBodyAsResource());
        Assert.assertEquals(target, retrieved.getTargetAsResource());

        } finally {
            em.close();
        }
    }
    
    @Test
    public void testPersistAnnotation() throws DAOException, JsonParseException, JsonMappingException, IOException {
        boolean added = DataManager.getInstance().getDao().addAnnotation(daoAnno);
        Assert.assertTrue(added);
        
        PersistentAnnotation fromDAO = DataManager.getInstance().getDao().getAnnotation(daoAnno.getId());
        WebAnnotation webAnno = daoAnno.getAsAnnotation();
        WebAnnotation fromDAOWebAnno = fromDAO.getAsAnnotation();
        Assert.assertEquals(webAnno.getBody(), fromDAOWebAnno.getBody());
        Assert.assertEquals(webAnno.getTarget(), fromDAOWebAnno.getTarget());
        Assert.assertEquals(webAnno, fromDAOWebAnno);
        
        Date changed = new Date();
        fromDAO.setDateModified(changed);
        boolean updated = DataManager.getInstance().getDao().updateAnnotation(fromDAO);
        
        PersistentAnnotation fromDAO2 = DataManager.getInstance().getDao().getAnnotation(daoAnno.getId());
        Assert.assertEquals(fromDAO.getDateModified(), fromDAO2.getDateModified());
    }

}
