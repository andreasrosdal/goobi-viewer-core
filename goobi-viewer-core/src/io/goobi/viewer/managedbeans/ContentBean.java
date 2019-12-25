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
package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.crowdsourcing.DisplayUserGeneratedContent;
import io.goobi.viewer.model.viewer.PhysicalElement;

/**
 * Supplies additional content for records (such contents produced by the crowdsourcing module).
 */
@Named
@SessionScoped
public class ContentBean implements Serializable {

    private static final long serialVersionUID = 3811544515374503924L;

    private static final Logger logger = LoggerFactory.getLogger(ContentBean.class);

    private String pi;
    /** Currently open page number. Used to make sure contents are reloaded if a new page is opened. */
    private int currentPage = -1;
    /** User generated contents to display on this page. */
    private List<DisplayUserGeneratedContent> userGeneratedContentsForDisplay;

    /**
     * Empty Constructor.
     */
    public ContentBean() {
        // the emptiness inside
    }

    /**
     * <p>init.</p>
     */
    @PostConstruct
    public void init() {
    }

    /**
     * <p>Getter for the field <code>userGeneratedContentsForDisplay</code>.</p>
     *
     * @return User-generated contents for the given page
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException
     * @throws io.goobi.viewer.exceptions.PresentationException
     * @param page a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     */
    public List<DisplayUserGeneratedContent> getUserGeneratedContentsForDisplay(PhysicalElement page)
            throws PresentationException, IndexUnreachableException {
        // logger.trace("getUserGeneratedContentsForDisplay");
        if (page != null && (userGeneratedContentsForDisplay == null || !page.getPi().equals(pi) || page.getOrder() != currentPage)) {
            loadUserGeneratedContentsForDisplay(page);
        }
        if (userGeneratedContentsForDisplay != null && userGeneratedContentsForDisplay.size() > 0) {
            return userGeneratedContentsForDisplay;
        }

        return null;
    }

    /**
     * <p>loadUserGeneratedContentsForDisplay.</p>
     *
     * @param page a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException
     * @throws io.goobi.viewer.exceptions.PresentationException
     */
    public void loadUserGeneratedContentsForDisplay(PhysicalElement page) throws PresentationException, IndexUnreachableException {
        logger.trace("loadUserGeneratedContentsForDisplay");
        if (page == null) {
            logger.debug("page is null, cannot load");
            return;
        }
        pi = page.getPi();
        currentPage = page.getOrder();
        userGeneratedContentsForDisplay = new ArrayList<>();
        List<DisplayUserGeneratedContent> allContent = DataManager.getInstance()
            .getSearchIndex()
            .getDisplayUserGeneratedContentsForPage(page.getPi(), page.getOrder());
        for (DisplayUserGeneratedContent ugcContent : allContent) {
            // Do not add empty comments
            if (!ugcContent.isEmpty()) {
                userGeneratedContentsForDisplay.add(ugcContent);
            }
        }
        logger.trace("Loaded {} user generated contents for page {}", userGeneratedContentsForDisplay.size(), currentPage);
    }

    /**
     * <p>getCurrentUGCCoords.</p>
     *
     * @param page a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public List<List<String>> getCurrentUGCCoords(PhysicalElement page) throws IndexUnreachableException, PresentationException {
        List<DisplayUserGeneratedContent> currentContents;
        currentContents = getUserGeneratedContentsForDisplay(page);
        if (currentContents == null) {
            return Collections.emptyList();
        }

        List<List<String>> coords = new ArrayList<>(currentContents.size());
        for (DisplayUserGeneratedContent content : currentContents) {
            if (content.hasArea()) {
                String rect = StringTools.normalizeWebAnnotationCoordinates(content.getAreaString());

                rect += (",\"" + content.getLabel() + "\"");
                rect += (",\"" + content.getId() + "\"");
                coords.add(Arrays.asList(rect.split(",")));
            }
        }
        logger.trace("getting ugc coordinates {}", coords);
        return coords;
    }

    /**
     * Removes script tags from the given string.
     *
     * @param value a {@link java.lang.String} object.
     * @return value sans any script tags
     */
    public String cleanUpValue(String value) {
        return StringTools.stripJS(value);
    }
}
