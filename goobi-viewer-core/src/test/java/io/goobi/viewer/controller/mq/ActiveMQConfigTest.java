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
package io.goobi.viewer.controller.mq;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;

public class ActiveMQConfigTest {

    private static final String activeMqConfigPath = "src/test/resources/config_activemq.xml";

    ActiveMQConfig config;
    
    @Before
    public void setup() throws IOException {
        this.config = new ActiveMQConfig(Paths.get(activeMqConfigPath));
    }
    
    @Test
    public void testReadURI() {
        assertEquals("tcp://0.0.0.0:61618", config.getConnectorURI());
    }
    
    @Test
    public void testReadUserName() {
        assertEquals("testadmin", config.getUsernameAdmin());
    }
    
    @Test
    public void testReadPassword() {
        assertEquals("test", config.getPasswordAdmin());
    }
    
    @Test
    public void testReadSchedulerDirectory() {
        assertEquals("src/test/resources/activemq/scheduler", config.getSchedulerDirectory());
    }

}
