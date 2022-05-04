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
package io.goobi.viewer.controller.imaging;

import java.net.URISyntaxException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.ConfigurationTest;
import io.goobi.viewer.controller.DataManager;

public class IIIFPresentationAPIHandlerTest extends AbstractTest {

    private static final String REST_API_URL = "http://localhost:8080/viewer/api/v1";
    
    private IIIFPresentationAPIHandler handler;
    private AbstractApiUrlManager urls;
    
    private String PI = "PI-SAMPLE";
    private String DC = "DC";
    private String COLLECTION = "sonstige.ocr";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.urls = new ApiUrls();
        handler = new IIIFPresentationAPIHandler(urls, DataManager.getInstance().getConfiguration());
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetManifestUrl() throws URISyntaxException {
        Assert.assertEquals(REST_API_URL + "/records/PI-SAMPLE/manifest/", handler.getManifestUrl("PI-SAMPLE"));
    }

    @Test
    public void testGetCollectionUrl() throws URISyntaxException {
        Assert.assertEquals(REST_API_URL + "/collections/DC/", handler.getCollectionUrl());

    }

    @Test
    public void testGetCollectionUrlString() throws URISyntaxException {
        Assert.assertEquals(REST_API_URL + "/collections/DC/", handler.getCollectionUrl("DC"));

    }

    @Test
    public void testGetCollectionUrlStringString() throws URISyntaxException {
        Assert.assertEquals(REST_API_URL + "/collections/DC/sonstige.ocr",
                handler.getCollectionUrl("DC", "sonstige.ocr"));

    }

    @Test
    public void testGetLayerUrl() throws URISyntaxException {
        Assert.assertEquals(REST_API_URL + "/records/PI-SAMPLE/layers/FULLTEXT/",
                handler.getLayerUrl("PI-SAMPLE", "fulltext"));

    }

    @Test
    public void testGetAnnotationsUrl() throws URISyntaxException {
        Assert.assertEquals(REST_API_URL + "/records/PI-SAMPLE/pages/12/annotations/",
                handler.getAnnotationsUrl("PI-SAMPLE", 12, "crowdsourcing"));

    }

    @Test
    public void testGetCanvasUrl() throws URISyntaxException {
        Assert.assertEquals(REST_API_URL + "/records/PI-SAMPLE/pages/12/canvas/",
                handler.getCanvasUrl("PI-SAMPLE", 12));

    }

    @Test
    public void testGetRangeUrl() throws URISyntaxException {
        Assert.assertEquals(REST_API_URL + "/records/PI-SAMPLE/sections/LOG_0007/range/",
                handler.getRangeUrl("PI-SAMPLE", "LOG_0007"));

    }

}
