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
package io.goobi.viewer.model.security.authentication;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.core.MediaType;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import io.goobi.viewer.AbstractDatabaseEnabledTest;

/**
 * @author florian
 *
 */
public class LitteraProviderTest extends AbstractDatabaseEnabledTest {

    private static String user_id = "test";
    private static String user_id_invalid = "blub";
    private static String user_pw = "test";
    private static String user_pw_invalid = "bla";

    private static final int SERVERPORT = 1080;
    private static final String SERVERURL = "127.0.0.1";
    private static final String RESPONSE_USER_VALID =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<Response authenticationSuccessful=\"true\" />";
    private static final String RESPONSE_USER_INVALID =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<Response authenticationSuccessful=\"false\" />";

    private LitteraProvider provider;

    //    private static ClientAndProxy proxy;
    private static ClientAndServer mockServer;
    private static MockServerClient serverClient;

    @BeforeClass
    public static void startProxy() {
        mockServer = ClientAndServer.startClientAndServer(SERVERPORT);

        serverClient = new MockServerClient(SERVERURL, SERVERPORT);

        //active user
        serverClient
                .when(HttpRequest.request().withPath("/externauth").withQueryStringParameter("id", user_id).withQueryStringParameter("pw", user_pw))
                .respond(HttpResponse.response().withHeader(new Header("Content-Type", MediaType.TEXT_XML)).withBody(RESPONSE_USER_VALID));

        //wrong login name
        serverClient.when(
                HttpRequest.request().withPath("/externauth").withQueryStringParameter("id", user_id_invalid).withQueryStringParameter("pw", user_pw))
                .respond(HttpResponse.response().withHeader(new Header("Content-Type", MediaType.TEXT_XML)).withBody(RESPONSE_USER_INVALID));

        //wrong password
        serverClient.when(
                HttpRequest.request().withPath("/externauth").withQueryStringParameter("id", user_id).withQueryStringParameter("pw", user_pw_invalid))
                .respond(HttpResponse.response().withHeader(new Header("Content-Type", MediaType.TEXT_XML)).withBody(RESPONSE_USER_INVALID));

    }

    @AfterClass
    public static void stopProxy() throws Exception {
        serverClient.stop();
        mockServer.stop();
        Path logFile = Paths.get("mockserver.log");
        if (Files.isRegularFile(logFile)) {
            Files.delete(logFile);
        }
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        provider = new LitteraProvider("external", "", "http://" + SERVERURL + ":" + SERVERPORT + "/externauth", "", 1000l);
    }

    @Test
    public void testLogin() throws AuthenticationProviderException, InterruptedException, ExecutionException {
        Assert.assertFalse(provider.login(user_id, user_pw).get().isRefused());
        Assert.assertTrue(provider.login(user_id_invalid, user_pw).get().isRefused());
        Assert.assertTrue(provider.login(user_id, user_pw_invalid).get().isRefused());
    }

}
