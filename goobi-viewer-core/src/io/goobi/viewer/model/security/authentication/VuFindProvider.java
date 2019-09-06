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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.glassfish.jersey.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.CharStreams;

import de.intranda.api.iiif.image.ImageInformation;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.security.Role;
import io.goobi.viewer.model.security.authentication.model.VuAuthenticationRequest;
import io.goobi.viewer.model.security.authentication.model.VuAuthenticationResponse;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;

/**
 * @author Florian Alpers
 *
 */
public class VuFindProvider extends HttpAuthenticationProvider {

    private static final Logger logger = LoggerFactory.getLogger(VuFindProvider.class);
    protected static final String DEFAULT_EMAIL = "{username}@nomail.com";
    protected static final String TYPE_USER_PASSWORD = "userPassword";
    private static final String USER_GROUP_ROLE_MEMBER = "member";

    private VuAuthenticationResponse authenticationResponse;

    /**
     * @param name
     * @param url
     * @param image
     */
    public VuFindProvider(String name, String label, String url, String image, long timeoutMillis) {
        super(name, label, TYPE_USER_PASSWORD, url, image, timeoutMillis);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#logout()
     */
    @Override
    public void logout() throws AuthenticationProviderException {
        //noop
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#login(java.lang.String, java.lang.String)
     */
    @Override
    public CompletableFuture<LoginResult> login(String loginName, String password) throws AuthenticationProviderException {
        try {
            VuAuthenticationRequest request = new VuAuthenticationRequest(loginName, password);
            String response = post(new URI(getUrl()), serialize(request));
            this.authenticationResponse = deserialize(response);
            Optional<User> user = getUser(request);
            LoginResult result =
                    new LoginResult(BeanUtils.getRequest(), BeanUtils.getResponse(), user, !this.authenticationResponse.getUser().getIsValid());
            return CompletableFuture.completedFuture(result);
        } catch (URISyntaxException e) {
            throw new AuthenticationProviderException("Cannot resolve authentication api url " + getUrl(), e);
        } catch (WebApplicationException e) {
            throw new AuthenticationProviderException("Error requesting authorizazion for user " + loginName, e);
        } catch (JsonProcessingException e) {
            throw new AuthenticationProviderException("Error requesting authorizazion for user " + loginName, e);
        } catch (IOException e) {
            throw new AuthenticationProviderException("Error requesting authorizazion for user " + loginName, e);
        }
    }

    private String serialize(VuAuthenticationRequest object) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(object);
        return json;
    }

    private VuAuthenticationResponse deserialize(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        VuAuthenticationResponse response = mapper.readValue(json, VuAuthenticationResponse.class);
        return response;
    }
    


    /**
     * @param request
     * @param response
     * @return
     * @throws AuthenticationProviderException
     */
    private Optional<User> getUser(VuAuthenticationRequest request) throws AuthenticationProviderException {

        if (request == null || StringUtils.isBlank(request.getUsername()) || StringUtils.isBlank(request.getPassword())
                || !Boolean.TRUE.equals(authenticationResponse.getUser().getExists())) {
            return Optional.empty();
        }

        User user = null;
        try {
            user = DataManager.getInstance().getDao().getUserByNickname(request.getUsername());
            if (user != null) {
                logger.debug("Found user {} via vuFind username '{}'.", user.getId(), request.getUsername());
            }
            // If not found, try email
            if (user == null) {
                user = DataManager.getInstance().getDao().getUserByEmail(request.getUsername());
                if (user != null) {
                    logger.debug("Found user {} via vuFind username '{}'.", user.getId(), request.getUsername());
                }
            }

            // If still not found, create a new user
            if (user == null) {
                user = new User();
                user.setNickName(request.getUsername());
                user.setActive(true);
                user.setEmail(DEFAULT_EMAIL.replace("{username}", request.getUsername()));
                logger.debug("Created new user with nickname " + request.getUsername());
            }

            // set user status
            if (!user.isSuspended()) {
                user.setSuspended(!authenticationResponse.getUser().getIsValid());
            }

            // Add to bean and persist
            if (user.getId() == null) {
                if (!DataManager.getInstance().getDao().addUser(user)) {
                    throw new AuthenticationProviderException("Could not add user to DB.");
                }
            } else {
                if (!DataManager.getInstance().getDao().updateUser(user)) {
                    throw new AuthenticationProviderException("Could not update user in DB.");
                }
            }

            // Add user to user group contained in the VuFind response
            if (authenticationResponse.getUser().getGroup() != null
                    && StringUtils.isNotBlank(authenticationResponse.getUser().getGroup().getDesc())) {
                String userGroupName = authenticationResponse.getUser().getGroup().getDesc();
                //                UserGroup userGroup = DataManager.getInstance().getDao().getUserGroup(userGroupName);
                //                if (userGroup != null && !userGroup.getMembers().contains(user)) {
                //                    //                DataManager.getInstance().getDao().updateUserGroup(userGroup);
                //                    Role role = DataManager.getInstance().getDao().getRole(USER_GROUP_ROLE_MEMBER);
                //                    if (role != null) {
                //                        userGroup.addMember(user, role);
                //                    }
                //                }
                if (!addUserToGroups.contains(userGroupName)) {
                    addUserToGroups.add(userGroupName);
                }
            }
        } catch (DAOException e) {
            throw new AuthenticationProviderException(e);
        }
        return Optional.ofNullable(user);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#allowsPasswordChange()
     */
    @Override
    public boolean allowsPasswordChange() {
        return false;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#allowsNicknameChange()
     */
    @Override
    public boolean allowsNicknameChange() {
        return false;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#allowsEmailChange()
     */
    @Override
    public boolean allowsEmailChange() {
        return true;
    }

}
