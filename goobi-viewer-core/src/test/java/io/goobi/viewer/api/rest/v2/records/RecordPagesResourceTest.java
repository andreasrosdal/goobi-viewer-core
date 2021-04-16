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
package io.goobi.viewer.api.rest.v2.records;

import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_PAGES;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_PAGES_ANNOTATIONS;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_PAGES_CANVAS;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_PAGES_COMMENTS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.intranda.api.annotation.wa.collection.AnnotationPage;
import io.goobi.viewer.api.rest.v2.AbstractRestApiTest;

/**
 * @author florian
 *
 */
public class RecordPagesResourceTest extends AbstractRestApiTest {

    private static final String PI = "PPN743674162";
    private static final String PAGENO = "10";
    private static final String PI_ANNOTATIONS = "PI_1";
    private static final String PAGENO_ANNOTATIONS = "1";
    
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }
    
    @Test 
    public void testGetCanvas() throws JsonMappingException, JsonProcessingException {
        String url = urls.path(RECORDS_PAGES, RECORDS_PAGES_CANVAS).params(PI, PAGENO).build();
        try(Response response = target(url)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals("Should return status 200", 200, response.getStatus());
            assertNotNull("Should return user object as json", response.getEntity());
            String entity = response.readEntity(String.class);
            assertNotNull(entity);
            JSONObject canvas = new JSONObject(entity);
            assertEquals(url, canvas.getString("id"));
            assertEquals("Canvas", canvas.getString("type"));
        }
    }
    
    /**
     * Test method for {@link io.goobi.viewer.api.rest.v2.records.RecordResource#getAnnotationsForRecord(java.lang.String)}.
     * @throws JsonProcessingException 
     * @throws JsonMappingException 
     */
    @Test
    public void testGetAnnotationsForPage() throws JsonMappingException, JsonProcessingException {
        try(Response response = target(urls.path(RECORDS_PAGES, RECORDS_PAGES_ANNOTATIONS).params(PI_ANNOTATIONS, PAGENO_ANNOTATIONS).build())
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals("Should return status 200", 200, response.getStatus());
            assertNotNull("Should return user object as json", response.getEntity());
            String entity = response.readEntity(String.class);
            AnnotationPage annoPage = mapper.readValue(entity, AnnotationPage.class);
            assertNotNull(annoPage);
            assertEquals("AnnotationPage", annoPage.getType());
            assertEquals(0, annoPage.getItems().size()); //No annotations indexed
        }
    }
    
    /**
     * Test method for {@link io.goobi.viewer.api.rest.v2.records.RecordResource#getCommentsForRecord(java.lang.String)}.
     * @throws JsonProcessingException 
     * @throws JsonMappingException 
     */
    @Test
    public void testGetCommentsForPage() throws JsonMappingException, JsonProcessingException {
        try(Response response = target(urls.path(RECORDS_PAGES, RECORDS_PAGES_COMMENTS).params(PI_ANNOTATIONS, PAGENO_ANNOTATIONS).build())
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals("Should return status 200", 200, response.getStatus());
            assertNotNull("Should return user object as json", response.getEntity());
            String entity = response.readEntity(String.class);
            AnnotationPage annoPage = mapper.readValue(entity, AnnotationPage.class);
            assertNotNull(annoPage);
            assertEquals(3, annoPage.getItems().size());
        }
    }
}
