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

import static io.goobi.viewer.api.rest.v2.ApiUrls.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.jdom2.JDOMException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.goobi.viewer.api.rest.v2.AbstractRestApiTest;
import io.goobi.viewer.controller.DataManager;

/**
 * @author florian
 *
 */
public class RecordFileResourceTest extends AbstractRestApiTest {

    private static final String PI = "PPN743674162";
    private static final String PI_SPACE_IN_FILENAME = "ARVIErdm5";
    private static final String FILENAME = "00000010";

    /**
     * @throws java.lang.Exception
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link io.goobi.viewer.api.rest.v1.records.RecordFileResource#getAlto(java.lang.String)}.
     * 
     * @throws IOException
     * @throws JDOMException
     */
    @Test
    public void testGetAlto() throws JDOMException, IOException {
        String url = urls.path(RECORDS_FILES, RECORDS_FILES_ALTO).params(PI, FILENAME + ".xml").build();
        try (Response response = target(url)
                .request()
                .accept(MediaType.TEXT_XML)
                .get()) {
            assertEquals("Should return status 200", 200, response.getStatus());
            String data = response.readEntity(String.class);
            assertTrue(StringUtils.isNotBlank(data));
        }
    }

    /**
     * Test method for {@link io.goobi.viewer.api.rest.v1.records.RecordFileResource#getPlaintext(java.lang.String)}.
     * 
     * @throws IOException
     * @throws JDOMException
     */
    @Test
    public void testGetPlaintext() throws JDOMException, IOException {
        String url = urls.path(RECORDS_FILES, RECORDS_FILES_PLAINTEXT).params(PI, FILENAME + ".txt").build();
        try (Response response = target(url)
                .request()
                .accept(MediaType.TEXT_PLAIN)
                .get()) {
            assertEquals(response.getStatusInfo().getReasonPhrase(), 200, response.getStatus());
            String data = response.readEntity(String.class);
            assertTrue(StringUtils.isNotBlank(data));
        }
    }

    /**
     * Test method for {@link io.goobi.viewer.api.rest.v1.records.RecordFileResource#getTEI(java.lang.String)}.
     * 
     * @throws IOException
     * @throws JDOMException
     */
    @Test
    public void testGetTEI() throws JDOMException, IOException {
        String url = urls.path(RECORDS_FILES, RECORDS_FILES_TEI).params(PI, FILENAME + ".xml").build();
        try (Response response = target(url)
                .request()
                .accept(MediaType.TEXT_XML)
                .get()) {
            assertEquals(response.getStatusInfo().getReasonPhrase(), 200, response.getStatus());
            String data = response.readEntity(String.class);
            assertTrue(StringUtils.isNotBlank(data));
        }
    }


    @Test
    public void testGetSourceFile() {
        String url = urls.path(RECORDS_FILES, RECORDS_FILES_SOURCE).params(PI, "text.txt").build();
        try (Response response = target(url)
                .request()
                .get()) {
            assertEquals(response.getStatusInfo().getReasonPhrase(), 200, response.getStatus());
            String contentType = response.getHeaderString("Content-Type");
            String entity = response.readEntity(String.class);
            assertEquals("application/octet-stream", contentType);
            assertEquals("apples", entity.trim());
        }
    }

    @Test
    public void testGetMissingSourceFile() {
        String url = urls.path(RECORDS_FILES, RECORDS_FILES_SOURCE).params(PI, "bla.txt").build();
        try (Response response = target(url)
                .request()
                .get()) {
            assertEquals(response.getStatusInfo().getReasonPhrase(), 404, response.getStatus());

        }
    }

    @Test
    public void testGetSourceFilePathTraversalAttack() {
        String url = urls.path(RECORDS_FILES, RECORDS_FILES_SOURCE).params(PI, "/../../../../..//etc/passwd").build();
        try (Response response = target(url)
                .request()
                .get()) {
            assertEquals("Should return status 404", 404, response.getStatus());
        }
    }
    
    @Test
    public void testEscapeFilenamesInUrls() {
        DataManager.getInstance().getConfiguration().overrideValue("webapi.iiif.rendering.viewer[@enabled]", true);
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingViewer());
        DataManager.getInstance().getConfiguration().overrideValue("webapi.iiif.rendering.pdf[@enabled]", true);
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingPDF());
        
        String url = urls.path(RECORDS_PAGES, RECORDS_PAGES_CANVAS).params(PI_SPACE_IN_FILENAME, "1").build();
        try (Response response = target(url)
                .request()
                .get()) {
            assertEquals("Should return status 200", 200, response.getStatus());
            String entity = response.readEntity(String.class);
            assertNotNull(entity);
            JSONObject canvas = new JSONObject(entity);
            JSONArray renderings = canvas.getJSONArray("rendering");
            assertFalse(renderings.isEmpty());
            List linkList = renderings.toList();
            Map pdfLink = renderings.toList().stream().map(object -> (Map)object)
                    .filter(map -> "Text".equals(map.get("type")))
                    .findAny().orElse(null);
            assertNotNull("No PDF link in canvas", pdfLink);
            String id = (String) pdfLink.get("id");
            Assert.assertTrue("Wrong filename in " + id, id.contains("erdmagnetisches+observatorium+vi_blatt_5.tif"));
        }
    }

}
