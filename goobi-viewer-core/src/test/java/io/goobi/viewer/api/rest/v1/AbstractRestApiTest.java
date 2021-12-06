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
package io.goobi.viewer.api.rest.v1;

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;


/**
 * @author florian
 *
 */
public abstract class AbstractRestApiTest extends JerseyTest {

    private static AbstractDatabaseAndSolrEnabledTest DATA_FRAMEWORK = new AbstractDatabaseAndSolrEnabledTest() {
    };
    
//    private static final String BASE_URL = "http://localhost:9999/";

    protected ObjectMapper mapper = new ObjectMapper();
    protected ApiUrls urls = new ApiUrls("");
    
    @BeforeClass
    public static void setUpClass() throws Exception {
        AbstractDatabaseAndSolrEnabledTest.setUpClass();
    }
    
    /**
     * @throws java.lang.Exception
     */
    @Override
    @Before 
    public void setUp() throws Exception {
        super.setUp();
        DATA_FRAMEWORK.setUp();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        mapper.registerModule(new JavaTimeModule());
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        DATA_FRAMEWORK.tearDown();
    }
    
    @AfterClass
    public static void tearDownClass() throws Exception {
        AbstractDatabaseAndSolrEnabledTest.tearDownClass();
    }

    
    @Override
    protected TestContainerFactory getTestContainerFactory() {
        return new GrizzlyWebTestContainerFactory();
    }

    @Override
    protected DeploymentContext configureDeployment() {
        
        AbstractBinder binder = new AbstractBinder() {
            
            @Override
            protected void configure() {
                bind(urls).to(AbstractApiUrlManager.class);
                
            }
        };
        
        ResourceConfig config = new io.goobi.viewer.api.rest.v1.Application(binder);
        return ServletDeploymentContext.forServlet(new ServletContainer(config)).build();
    }


}
