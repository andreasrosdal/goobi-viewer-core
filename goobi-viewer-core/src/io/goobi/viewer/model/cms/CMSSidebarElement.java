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
package io.goobi.viewer.model.cms;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.Helper;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.misc.GeoLocation;
import io.goobi.viewer.model.misc.GeoLocationInfo;
import io.goobi.viewer.model.misc.NumberIterator;
import io.goobi.viewer.servlets.rest.cms.CMSContentResource;

/**
 * <p>CMSSidebarElement class.</p>
 *
 */
@Entity
@Table(name = "cms_sidebar_elements")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "widget_type")
public class CMSSidebarElement {

    /**
     * 
     */
    private static final String JSON_PROPERTYNAME_GEOLOCATIONS = "locations";
    private static final Logger logger = LoggerFactory.getLogger(CMSSidebarElement.class);
    /** Constant <code>HASH_MULTIPLIER=11</code> */
    protected static final int HASH_MULTIPLIER = 11;
    private static final NumberIterator ID_COUNTER = new NumberIterator();

    private static Pattern patternHtmlTag = Pattern.compile("<.*?>");
    private static Pattern patternHtmlAttribute = Pattern.compile("[ ].*?[=][\"].*?[\"]");
    private static Pattern patternCssClass = Pattern.compile("[0-9a-z-_]*");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cms_sidebar_element_id")
    private Long id;

    /** Reference to the owning <code>CMSPage</code>. */
    @ManyToOne
    @JoinColumn(name = "owner_page_id", nullable = false)
    private CMSPage ownerPage;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "value")
    private String value;

    @Column(name = "sort_order")
    private int order;

    @Column(name = "inner_html", columnDefinition = "LONGTEXT")
    private String html = null;

    @Column(name = "css_class")
    private String cssClass = "";

    @Enumerated(EnumType.STRING)
    @Column(name = "widget_mode", nullable = false)
    private WidgetMode widgetMode = WidgetMode.STANDARD;

    @Column(name = "linked_pages", nullable = true)
    private String linkedPagesString = "";
    @Transient
    private PageList linkedPages = null;

    @Column(name = "geo_locations", columnDefinition = "LONGTEXT")
    private String geoLocationsString = null;
    @Transient
    private GeoLocationInfo geoLocations = null;

    @Column(name = "widget_type", nullable = false)
    private String widgetType = this.getClass().getSimpleName();

    @Column(name = "widget_title")
    private String widgetTitle = null;

    @Transient
    private final int sortingId = ID_COUNTER.next();

    public enum WidgetMode {
        STANDARD,
        FOLDOUT;
    }

    /**
     * <p>Constructor for CMSSidebarElement.</p>
     */
    public CMSSidebarElement() {
        // the emptiness inside
    }

    /**
     * Creates a copy of the original CMSSidebarElement. Handles cases where the original is of a class inheriting from this class
     *
     * @param ownerPage May be null, if the sidebarelement is not (yet) associated with a CMSPage. Otherwise it should be the actual owning page, not
     *            the owner of the original (unless that is acutally desired)
     * @param original a {@link io.goobi.viewer.model.cms.CMSSidebarElement} object.
     * @return a {@link io.goobi.viewer.model.cms.CMSSidebarElement} object.
     */
    public static CMSSidebarElement copy(CMSSidebarElement original, CMSPage ownerPage) {
        CMSSidebarElement copy;
        if (!original.getClass().equals(CMSSidebarElement.class)) {
            if (CMSSidebarElementWithQuery.class.equals(original.getClass())) {
                copy = new CMSSidebarElementWithQuery((CMSSidebarElementWithQuery) original, ownerPage);
            } else if (CMSSidebarElementWithSearch.class.equals(original.getClass())) {
                copy = new CMSSidebarElementWithSearch((CMSSidebarElementWithSearch) original, ownerPage);
            } else {
                throw new IllegalArgumentException(
                        "Cannot create copy of " + original.getClass() + ": copy constructor for that class not implemented");
            }
        } else {
            copy = new CMSSidebarElement(original, ownerPage);
        }
        return copy;
    }

    /**
     * <p>Constructor for CMSSidebarElement.</p>
     *
     * @param original a {@link io.goobi.viewer.model.cms.CMSSidebarElement} object.
     * @param owner a {@link io.goobi.viewer.model.cms.CMSPage} object.
     */
    public CMSSidebarElement(CMSSidebarElement original, CMSPage owner) {
        if (original.id != null) {
            this.id = new Long(original.id);
        }
        this.ownerPage = owner;
        this.type = original.type;
        this.value = original.value;
        this.order = original.order;
        this.html = original.html;
        this.cssClass = original.cssClass;
        this.widgetMode = original.widgetMode;
        this.linkedPagesString = original.linkedPagesString;
        this.geoLocationsString = original.geoLocationsString;
        this.widgetType = original.widgetType;
        this.widgetTitle = original.widgetTitle;
        deSerialize();

    }

    /**
     * <p>compareTo.</p>
     *
     * @param o a {@link java.lang.Object} object.
     * @return a int.
     */
    public int compareTo(Object o) {
        CMSSidebarElement other = (CMSSidebarElement) o;
        if (other.order == order) {
            return 0;
        }
        if (other.order < order) {
            return 1;
        }

        return -1;
    }

    /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        int code = 21;
        code += HASH_MULTIPLIER * getType().hashCode();
        if (StringUtils.isNotBlank(getHtml())) {
            code += HASH_MULTIPLIER * getHtml().hashCode();
        }
        if (StringUtils.isNotBlank(getCssClass())) {
            code += HASH_MULTIPLIER * getCssClass().hashCode();
        }
        if (getLinkedPages() != null) {
            code += HASH_MULTIPLIER * getLinkedPages().hashCode();
        }
        return code;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        return o.getClass().equals(CMSSidebarElement.class) && bothNullOrEqual(getType(), ((CMSSidebarElement) o).getType())
                && bothNullOrEqual(getHtml(), ((CMSSidebarElement) o).getHtml())
                && bothNullOrEqual(getCssClass(), ((CMSSidebarElement) o).getCssClass())
                && bothNullOrEqual(getLinkedPages(), ((CMSSidebarElement) o).getLinkedPages());
    }

    /**
     * <p>bothNullOrEqual.</p>
     *
     * @param o1 a {@link java.lang.Object} object.
     * @param o2 a {@link java.lang.Object} object.
     * @return a boolean.
     */
    protected static boolean bothNullOrEqual(Object o1, Object o2) {
        if (o1 == null) {
            return o2 == null;
        }
        return o1.equals(o2);
    }

    /**
     * <p>Getter for the field <code>html</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getHtml() {
        return this.html;
    }

    /**
     * <p>Setter for the field <code>html</code>.</p>
     *
     * @param html a {@link java.lang.String} object.
     */
    public void setHtml(String html) {
        //        this.html = html;
        this.html = correctHtml(html);
    }

    /**
     * @param html2
     * @return
     */
    private static String correctHtml(String string) {
        for (String key : CMSSidebarManager.getInstance().getHtmlReplacements().keySet()) {
            String replacement = CMSSidebarManager.getInstance().getHtmlReplacements().get(key);
            string = string.replaceAll(key, replacement);
        }
        return string;
    }

    /**
     * <p>Getter for the field <code>id</code>.</p>
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * <p>Setter for the field <code>id</code>.</p>
     *
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * <p>Getter for the field <code>ownerPage</code>.</p>
     *
     * @return the ownerPage
     */
    public CMSPage getOwnerPage() {
        return ownerPage;
    }

    /**
     * <p>Setter for the field <code>ownerPage</code>.</p>
     *
     * @param ownerPage the ownerPage to set
     */
    public void setOwnerPage(CMSPage ownerPage) {
        this.ownerPage = ownerPage;
    }

    /**
     * <p>Getter for the field <code>type</code>.</p>
     *
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * <p>Setter for the field <code>type</code>.</p>
     *
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * <p>Getter for the field <code>value</code>.</p>
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * <p>Setter for the field <code>value</code>.</p>
     *
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * <p>Getter for the field <code>order</code>.</p>
     *
     * @return the order
     */
    public int getOrder() {
        return order;
    }

    /**
     * <p>Setter for the field <code>order</code>.</p>
     *
     * @param order the order to set
     */
    public void setOrder(int order) {
        this.order = order;
    }

    /**
     * <p>getContent.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getContent() {
        return CMSContentResource.getSidebarElementUrl(this);
    }

    /**
     * <p>hasHtml.</p>
     *
     * @return a boolean.
     */
    public boolean hasHtml() {
        return getHtml() != null;
    }

    /**
     * <p>Getter for the field <code>widgetMode</code>.</p>
     *
     * @return a {@link io.goobi.viewer.model.cms.CMSSidebarElement.WidgetMode} object.
     */
    public WidgetMode getWidgetMode() {
        if (widgetMode == null) {
            widgetMode = WidgetMode.STANDARD;
        }
        //	logger.trace("Get widget mode {}", widgetMode);
        return widgetMode;
    }

    /**
     * <p>Setter for the field <code>widgetMode</code>.</p>
     *
     * @param widgetMode a {@link io.goobi.viewer.model.cms.CMSSidebarElement.WidgetMode} object.
     */
    public void setWidgetMode(WidgetMode widgetMode) {
        //	logger.trace("Setting widget mode of {} to {}", type,  widgetMode);
        if (widgetMode == null) {
            widgetMode = WidgetMode.STANDARD;
        }
        this.widgetMode = widgetMode;
    }

    /**
     * <p>Getter for the field <code>cssClass</code>.</p>
     *
     * @return the cssClass
     */
    public String getCssClass() {
        return cssClass;
    }

    /**
     * <p>Setter for the field <code>cssClass</code>.</p>
     *
     * @param className a {@link java.lang.String} object.
     */
    public void setCssClass(String className) {
        if (!validateCssClass(className)) {
            String msg = Helper.getTranslation("cms_validationWarningCssClassInvalid", null);
            Messages.error(msg.replace("{0}", this.getType()));
        } else {
            this.cssClass = className;
        }
    }

    /**
     * @param className
     * @return
     */
    private static boolean validateCssClass(String className) {
        return patternCssClass.matcher(className).matches();
    }

    /**
     * Tests whether the html contains only the allowed html-tags
     *
     * @return a boolean.
     */
    public boolean isValid() {
        if (hasHtml()) {
            Matcher m = patternHtmlTag.matcher(html);
            //            Set<String> allowedTags = CMSSidebarManager.getInstance().getAllowedHtmlTags();
            Set<String> disallowedTags = CMSSidebarManager.getInstance().getDisallowedHtmlTags();
            while (m.find()) {
                String tag = m.group();
                if (tag.startsWith("<!--")) {
                    continue;
                }
                tag = cleanupHtmlTag(tag);
                logger.trace("Check tag '{}' for validity", tag);
                if (disallowedTags != null && disallowedTags.contains(tag)) {
                    logger.debug("Tag '{}' is not allowed in sidebar widget HTML.", tag);
                    return false;
                }

            }
        }
        return true;
    }

    /**
     * Normalizes the given HTML tag so that it can be matched against <code>CMSSidebarManager.getAllowedHtmlTags()</code>.
     *
     * @param tag a {@link java.lang.String} object.
     * @should remove attributes correctly
     * @should remove closing tag correctly
     * @return a {@link java.lang.String} object.
     */
    protected static String cleanupHtmlTag(String tag) {
        // Remove attributes
        Matcher m2 = patternHtmlAttribute.matcher(tag);
        while (m2.find()) {
            String attribute = m2.group();
            tag = tag.replace(attribute, "");
        }
        tag = tag.replace("</", "<").replace("/>", ">").replace(" ", "");

        return tag;
    }

    /**
     * <p>getCategory.</p>
     *
     * @return a {@link io.goobi.viewer.model.cms.SidebarElementType.Category} object.
     */
    public SidebarElementType.Category getCategory() {

        if (this instanceof CMSSidebarElementWithQuery) {
            return SidebarElementType.Category.fieldQuery;
        } else if (this instanceof CMSSidebarElementWithSearch) {
            return SidebarElementType.Category.search;
        } else if (this.getLinkedPages() != null) {
            return SidebarElementType.Category.pageLinks;
        } else if (this.getGeoLocations() != null) {
            return SidebarElementType.Category.geoLocations;
        }
        return this.getHtml() != null ? SidebarElementType.Category.custom : SidebarElementType.Category.standard;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getCanonicalName()).append('\n');
        sb.append(getType());
        sb.append(" (").append(getId()).append(") ");
        return sb.toString();
    }

    /**
     * <p>Getter for the field <code>sortingId</code>.</p>
     *
     * @return the sortingId
     */
    public int getSortingId() {
        return sortingId;
    }

    /**
     * <p>Getter for the field <code>linkedPages</code>.</p>
     *
     * @return the linkedPages
     */
    public PageList getLinkedPages() {
        //        this.linkedPages = new PageList(this.linkedPagesString);
        return linkedPages;
    }

    /**
     * <p>Setter for the field <code>linkedPages</code>.</p>
     *
     * @param linkedPages the linkedPages to set
     */
    public void setLinkedPages(PageList linkedPages) {
        this.linkedPages = linkedPages;
    }

    /**
     * <p>Getter for the field <code>linkedPagesString</code>.</p>
     *
     * @return the linkedPagesList
     */
    public String getLinkedPagesString() {
        //        this.linkedPagesString = linkedPages.toString();
        return linkedPagesString;
    }

    /**
     * <p>Setter for the field <code>linkedPagesString</code>.</p>
     *
     * @param linkedPagesList the linkedPagesList to set
     */
    public void setLinkedPagesString(String linkedPagesList) {
        this.linkedPagesString = linkedPagesList;
    }

    /**
     * <p>serialize.</p>
     */
    public void serialize() {
        if (this.linkedPages != null) {
            this.linkedPagesString = linkedPages.toString();
        } else {
            this.linkedPagesString = null;
        }
        if (geoLocations != null) {
            this.geoLocationsString = createGeoLocationsString(geoLocations);
        }

    }

    /**
     * <p>deSerialize.</p>
     */
    public void deSerialize() {
        if (StringUtils.isNotEmpty(this.linkedPagesString)) {
            this.linkedPages = new PageList(this.linkedPagesString);
        } else {
            this.linkedPages = null;
        }
        if (StringUtils.isNotBlank(this.geoLocationsString)) {
            this.geoLocations = createGeoLocationsFromString(this.geoLocationsString);
        }
    }

    /**
     * <p>initGeolocations.</p>
     *
     * @param info a {@link io.goobi.viewer.model.misc.GeoLocationInfo} object.
     */
    public void initGeolocations(GeoLocationInfo info) {
        if (info.getLocationList().isEmpty()) {
            info.getLocationList().add(new GeoLocation());
        }
        this.geoLocations = info;
        this.geoLocationsString = createGeoLocationsString(this.geoLocations);
    }

    /**
     * <p>Getter for the field <code>geoLocations</code>.</p>
     *
     * @return a {@link io.goobi.viewer.model.misc.GeoLocationInfo} object.
     */
    public GeoLocationInfo getGeoLocations() {
        return this.geoLocations;
    }

    /**
     * <p>addGeoLocation.</p>
     */
    public void addGeoLocation() {
        this.geoLocations.getLocationList().add(new GeoLocation());
    }

    /**
     * <p>removeGeoLocation.</p>
     */
    public void removeGeoLocation() {
        if (geoLocations != null) {
            this.geoLocations.getLocationList().remove(this.geoLocations.getLocationList().size() - 1);
            if (this.geoLocations.getLocationList().isEmpty()) {
                this.geoLocations.getLocationList().add(new GeoLocation());
            }
        }
    }

    /**
     * @param geoLocationsString2
     * @return
     */
    private GeoLocationInfo createGeoLocationsFromString(String string) {

        try {
            JSONObject json = new JSONObject(string);
            GeoLocationInfo info = new GeoLocationInfo(json);
            //            if(locations != null) {                
            //                for (int i = 0; i < locations.length(); i++) {
            //                    JSONObject obj = locations.getJSONObject(i);
            //                    list.add(new GeoLocation(obj));
            //                }
            //            }
            return info;
        } catch (JSONException e) {
            logger.error("Failed to create geolocation list from string \n" + string, e);
        }
        return new GeoLocationInfo();
        //        if(list.isEmpty()) {
        //            list.add(new GeoLocation());
        //        }
    }

    /**
     * @param geoLocations2
     * @return
     */
    private String createGeoLocationsString(GeoLocationInfo info) {

        JSONObject json = info.getAsJson();

        //        JSONArray locations = new JSONArray();
        //        list.stream()
        //        .filter(loc -> !loc.isEmpty())
        //        .map(loc -> loc.getAsJson())
        //        .forEach(loc -> locations.put(loc));
        //        
        //        JSONObject json = new JSONObject();
        //        json.put(JSON_PROPERTYNAME_GEOLOCATIONS, locations);

        return json.toString();
    }

    /**
     * <p>Getter for the field <code>geoLocationsString</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getGeoLocationsString() {
        return this.geoLocationsString;
    }

    /**
     * <p>Getter for the field <code>widgetTitle</code>.</p>
     *
     * @return the widgetTitle
     */
    public String getWidgetTitle() {
        return widgetTitle;
    }

    /**
     * <p>Setter for the field <code>widgetTitle</code>.</p>
     *
     * @param widgetTitle the widgetTitle to set
     */
    public void setWidgetTitle(String widgetTitle) {
        this.widgetTitle = widgetTitle;
    }

    /**
     * <p>isHasWidgetTitle.</p>
     *
     * @return a boolean.
     */
    public boolean isHasWidgetTitle() {
        return StringUtils.isNotBlank(getWidgetTitle());
    }

    /**
     * <p>isHasLinkedPages.</p>
     *
     * @return a boolean.
     */
    public boolean isHasLinkedPages() {
        return this.linkedPages != null && !this.linkedPages.isEmpty();
    }

}
