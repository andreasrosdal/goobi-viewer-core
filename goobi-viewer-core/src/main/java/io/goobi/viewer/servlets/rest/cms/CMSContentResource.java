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
package io.goobi.viewer.servlets.rest.cms;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.Helper;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.CmsElementNotFoundException;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.CMSContentItem;
import io.goobi.viewer.model.cms.CMSPage;
import io.goobi.viewer.model.cms.CMSSidebarElement;
import io.goobi.viewer.servlets.rest.ViewerRestServiceBinding;

/**
 * Provides methods to access cms-content to be embedded into pages with <ui:include>
 *
 * getPageUrl(), getContentUrl() and getSidebarElementUrl() provide urls to cms-pages, content and sidebar-element respectively. The other methods
 * resolve these urls and return the appropriate html content. All urls are absolute urls including scheme information (http), as ui:include cannot
 * resolve them otherwise (only file urls can be resolved with relative paths)
 */
@Path("/cms")
@ViewerRestServiceBinding
public class CMSContentResource {

    private static final Logger logger = LoggerFactory.getLogger(CMSContentResource.class);

    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);
    private static final long REQUEST_TIMEOUT = 3000;//3s

    private enum TargetType {
        PAGE,
        CONTENT,
        SIDEBAR
    }

    @Context
    private HttpServletResponse servletResponse;

    /**
     * <p>getContentHtml.</p>
     *
     * @param pageId a {@link java.lang.Long} object.
     * @param language a {@link java.lang.String} object.
     * @param contentId a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws javax.servlet.ServletException if any.
     */
    @GET
    @Path("/content/{pageId}/{language}/{contentId}")
    @Produces({ MediaType.TEXT_HTML })
    public String getContentHtml(@PathParam("pageId") Long pageId, @PathParam("language") String language, @PathParam("contentId") String contentId)
            throws IOException, DAOException, ServletException {
        if (servletResponse != null) {
            servletResponse.setCharacterEncoding(Helper.DEFAULT_ENCODING);
        }
        String output = createResponseInThread(TargetType.CONTENT, pageId, language, contentId, REQUEST_TIMEOUT);
        return wrap(output, false);
    }

    /**
     * <p>getPageUrl.</p>
     *
     * @param pageId a {@link java.lang.Long} object.
     * @return a {@link java.lang.String} object.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws javax.servlet.ServletException if any.
     */
    @GET
    @Path("/page/{pageId}")
    @Produces({ MediaType.TEXT_HTML })
    public String getPageUrl(@PathParam("pageId") Long pageId) throws IOException, DAOException, ServletException {
        String output = createResponseInThread(TargetType.PAGE, pageId, null, null, REQUEST_TIMEOUT);
        return wrap(output, true);
    }

    /**
     * <p>getSidebarElementHtml.</p>
     *
     * @param elementId a {@link java.lang.Long} object.
     * @return a {@link java.lang.String} object.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws javax.servlet.ServletException if any.
     */
    @GET
    @Path("/sidebar/{elementId}")
    @Produces({ MediaType.TEXT_PLAIN })
    public String getSidebarElementHtml(@PathParam("elementId") Long elementId) throws IOException, DAOException, ServletException {
        String output = createResponseInThread(TargetType.SIDEBAR, elementId, null, null, REQUEST_TIMEOUT);
        return wrap(output, true);
    }

    /**
     * <p>wrap.</p>
     *
     * @param string a {@link java.lang.String} object.
     * @param escapeHtml a boolean.
     * @return a {@link java.lang.String} object.
     */
    protected String wrap(String string, boolean escapeHtml) {
        String output = "";
        if (StringUtils.isNotBlank(string)) {
            output = replaceHtmlCharacters(string, escapeHtml);
            output = "<span>" + output + "</span>";
        } else {
            output = "<span></span>";
        }
        // logger.trace("Sending cms content string '{}'", output);
        return output;
    }

    private static String createResponseInThread(final TargetType target, final Long pageId, final String language, final String contentId,
            final long timeout/*ms*/) throws IOException, DAOException, ServletException {

        logger.trace("Creating response for Target = '{}', pageId = '{}', language = '{}', fieldId = '{}'", target, pageId, language, contentId);

        Callable<String> job = new Callable<String>() {

            @Override
            public String call() throws Exception {
                return createResponse(target, pageId, language, contentId);
            }
        };

        Future<String> result = executor.submit(job);

        try {
            return result.get(timeout, TimeUnit.MILLISECONDS);
        } catch (CancellationException | InterruptedException e) {
            //servlet request cancelled or servlet thread interrupted. No answer needed
            return null;
        } catch (ExecutionException e) {
            //error creating response
            Throwable cause = e.getCause();
            if (cause != null && cause instanceof IOException) {
                throw (IOException) cause;
            } else if (cause != null && cause instanceof DAOException) {
                throw (DAOException) cause;
            } else {
                throw new ServletException(cause);
            }
        } catch (TimeoutException e) {
            result.cancel(true);
            throw new DAOException("Timeout while accessing database: " + e.getMessage());
        }
    }

    private static String createResponse(TargetType target, Long pageId, String language, String contentId) throws IOException, DAOException {
        String output = null;
        switch (target) {
            case CONTENT:
                output = getValue(pageId, contentId, language);
                break;
            case SIDEBAR:
                output = getSidebarElement(pageId);
                break;
            default: // nothing
        }
        return output;
    }

    /**
     * @param request
     * @return
     * @throws IOException
     * @throws DAOException
     */
    private static String getSidebarElement(Long elementId) throws IOException, DAOException {
        try {
            CMSSidebarElement element = DataManager.getInstance().getDao().getCMSSidebarElement(elementId);
            String html = element.getHtml();
            if (StringUtils.isNotBlank(html)) {
                return html;
            }
        } catch (NumberFormatException e) {
            logger.error("Value of 'element' parameter is not a long: {}", e.getMessage());
        }

        return null;
    }

    /**
     * @param input
     * @param escapeHtml
     * @return
     */
    private static String replaceHtmlCharacters(String input, boolean escapeHtml) {
        String output = input;
        if (escapeHtml) {
            // Full unescape
            output = StringEscapeUtils.unescapeHtml4(output);
            output = output.replace("&", "&amp;");
        } else {
            // Unescape everything except escaped <>
            output = output.replace("&lt;", "###LT###").replace("&gt;", "###GT###");
            output = StringEscapeUtils.unescapeHtml4(output);
            output = output.replace("&", "&amp;");
            output = output.replace("###LT###", "&lt;").replace("###GT###", "&gt;");
            output = output.replaceAll("<!--[\\w\\W]*?-->", "");
        }

        return output;
    }

    /**
     * @param pageId
     * @param fieldId
     * @return
     * @throws DAOException
     */
    private static String getValue(Long pageId, String fieldId, String language) throws DAOException {
        CMSPage page = DataManager.getInstance().getDao().getCMSPage(pageId);
        if (page != null) {
            CMSContentItem item;
            try {
                item = page.getContentItem(fieldId, language);
            } catch (CmsElementNotFoundException e) {
                item = null;
            }
            if (item == null || item.getHtmlFragment() == null) {
                try {
                    item = page.getDefaultLanguage().getContentItem(fieldId);
                } catch (CmsElementNotFoundException e) {
                    item = null;
                }
            }
            if (item != null) {
                return item.getHtmlFragment();
            }
        }
        return null;
    }

    /**
     * <p>getPageUrl.</p>
     *
     * @param cmsPage a {@link io.goobi.viewer.model.cms.CMSPage} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getPageUrl(CMSPage cmsPage) {
        if (cmsPage != null) {
            StringBuilder urlBuilder = new StringBuilder(BeanUtils.getServletPathWithHostAsUrlFromJsfContext());
            urlBuilder.append("/rest/cms/");
            urlBuilder.append(TargetType.PAGE.name().toLowerCase());
            urlBuilder.append("/");
            urlBuilder.append(cmsPage.getId()).append("/").append("/");
            urlBuilder.append("?timestamp=").append(System.currentTimeMillis());
            logger.debug("CMS rest api url = {}", urlBuilder.toString());
            return urlBuilder.toString();
        }
        return "";
    }

    /**
     * <p>getContentUrl.</p>
     *
     * @param item a {@link io.goobi.viewer.model.cms.CMSContentItem} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getContentUrl(CMSContentItem item) {
        if (item != null) {
            StringBuilder urlBuilder = new StringBuilder(BeanUtils.getServletPathWithHostAsUrlFromJsfContext());
            //	              StringBuilder urlBuilder = new StringBuilder(BeanUtils.getRequest().getContextPath());

            urlBuilder.append("/rest/cms/");
            urlBuilder.append(TargetType.CONTENT.name().toLowerCase());
            urlBuilder.append("/");
            urlBuilder.append(item.getOwnerPageLanguageVersion().getOwnerPage().getId());
            urlBuilder.append("/");
            urlBuilder.append(item.getOwnerPageLanguageVersion().getLanguage());
            urlBuilder.append("/");
            urlBuilder.append(item.getItemId()).append("/");
            urlBuilder.append("?timestamp=").append(System.currentTimeMillis());
            logger.debug("CMS rest api url = {}", urlBuilder.toString());
            return urlBuilder.toString();
        }
        return "";
    }

    /**
     * <p>getSidebarElementUrl.</p>
     *
     * @param item a {@link io.goobi.viewer.model.cms.CMSSidebarElement} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getSidebarElementUrl(CMSSidebarElement item) {
        if (item != null && item.hasHtml()) {
            StringBuilder urlBuilder = new StringBuilder(BeanUtils.getServletPathWithHostAsUrlFromJsfContext());
            urlBuilder.append("/rest/cms/");
            urlBuilder.append(TargetType.SIDEBAR.name().toLowerCase());
            urlBuilder.append("/");
            urlBuilder.append(item.getId()).append("/");
            urlBuilder.append("?timestamp=").append(System.currentTimeMillis());
            return urlBuilder.toString();
        }
        return "";
    }

}