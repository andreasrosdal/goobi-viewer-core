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
package io.goobi.viewer.model.security;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.security.user.IpRange;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;

@Entity
@Table(name = "licenses")
public class License implements IPrivilegeHolder, Serializable {

    private static final long serialVersionUID = 1363557138283960150L;

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(License.class);

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        License other = (License) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "license_id")
    private Long id;

    @JoinColumn(name = "license_type_id", nullable = false)
    private LicenseType licenseType;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "user_group_id")
    private UserGroup userGroup;

    @ManyToOne
    @JoinColumn(name = "ip_range_id")
    private IpRange ipRange;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_start")
    private Date start;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_end")
    private Date end;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "license_privileges", joinColumns = @JoinColumn(name = "license_id"))
    @Column(name = "privilege_name")
    private Set<String> privileges = new HashSet<>();

    @Column(name = "conditions")
    private String conditions;

    @Column(name = "description")
    private String description;

    /** List of allowed subtheme discriminator values for CMS pages. */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "license_cms_subthemes", joinColumns = @JoinColumn(name = "license_id"))
    @Column(name = "subtheme_discriminator_value")
    private List<String> subthemeDiscriminatorValues = new ArrayList<>();

    /** List of allowed CMS categories. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "license_cms_categories", joinColumns = @JoinColumn(name = "license_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private List<CMSCategory> allowedCategories = new ArrayList<>();

    /** List of allowed CMS templates. */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "license_cms_templates", joinColumns = @JoinColumn(name = "license_id"))
    @Column(name = "template_id")
    private List<String> allowedCmsTemplates = new ArrayList<>();

    /**
     * Checks the validity of this license. A valid license is either not time limited (start and/or end) or the current date lies between the
     * license's start and and dates.
     *
     * @return true if valid; false otherwise;
     * @should return correct value
     */
    public boolean isValid() {
        Date now = new Date();
        return (start == null || start.before(now)) && (end == null || end.after(now));
    }

    @Override
    public boolean hasPrivilege(String privilege) {
        return privileges.contains(privilege);
    }

    @Override
    public boolean isPrivList() {
        return hasPrivilege(IPrivilegeHolder.PRIV_LIST);
    }

    @Override
    public void setPrivList(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_LIST);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_LIST);
        }
    }

    @Override
    public boolean isPrivViewImages() {
        return hasPrivilege(IPrivilegeHolder.PRIV_VIEW_IMAGES);
    }

    @Override
    public void setPrivViewImages(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_VIEW_IMAGES);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_VIEW_IMAGES);
        }
    }

    @Override
    public boolean isPrivViewThumbnails() {
        return hasPrivilege(IPrivilegeHolder.PRIV_VIEW_THUMBNAILS);
    }

    @Override
    public void setPrivViewThumbnails(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_VIEW_THUMBNAILS);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_VIEW_THUMBNAILS);
        }
    }

    @Override
    public boolean isPrivViewFulltext() {
        return hasPrivilege(IPrivilegeHolder.PRIV_VIEW_FULLTEXT);
    }

    @Override
    public void setPrivViewFulltext(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_VIEW_FULLTEXT);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_VIEW_FULLTEXT);
        }
    }

    @Override
    public boolean isPrivViewVideo() {
        return hasPrivilege(IPrivilegeHolder.PRIV_VIEW_VIDEO);
    }

    @Override
    public void setPrivViewVideo(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_VIEW_VIDEO);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_VIEW_VIDEO);
        }
    }

    @Override
    public boolean isPrivViewAudio() {
        return hasPrivilege(IPrivilegeHolder.PRIV_VIEW_AUDIO);
    }

    @Override
    public void setPrivViewAudio(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_VIEW_AUDIO);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_VIEW_AUDIO);
        }
    }

    @Override
    public boolean isPrivDownloadPdf() {
        return hasPrivilege(IPrivilegeHolder.PRIV_DOWNLOAD_PDF);
    }

    @Override
    public void setPrivDownloadPdf(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_DOWNLOAD_PDF);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_DOWNLOAD_PDF);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#isPrivDownloadPagePdf()
     */
    @Override
    public boolean isPrivDownloadPagePdf() {
        return hasPrivilege(IPrivilegeHolder.PRIV_DOWNLOAD_PAGE_PDF);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#setPrivDownloadPagePdf(boolean)
     */
    @Override
    public void setPrivDownloadPagePdf(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_DOWNLOAD_PAGE_PDF);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_DOWNLOAD_PAGE_PDF);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.user.IPrivilegeHolder#isPrivDownloadOriginalContent()
     */
    @Override
    public boolean isPrivDownloadOriginalContent() {
        return hasPrivilege(IPrivilegeHolder.PRIV_DOWNLOAD_ORIGINAL_CONTENT);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.user.IPrivilegeHolder#setPrivDownloadOriginalContent(boolean)
     */
    @Override
    public void setPrivDownloadOriginalContent(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_DOWNLOAD_ORIGINAL_CONTENT);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_DOWNLOAD_ORIGINAL_CONTENT);
        }

    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#isPrivDeleteOcrPage()
     */
    @Override
    public boolean isPrivDeleteOcrPage() {
        return hasPrivilege(IPrivilegeHolder.PRIV_DELETE_OCR_PAGE);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#setPrivDeleteOcrPage(boolean)
     */
    @Override
    public void setPrivDeleteOcrPage(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_DELETE_OCR_PAGE);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_DELETE_OCR_PAGE);
        }
    }

    @Override
    public boolean isPrivSetRepresentativeImage() {
        return hasPrivilege(IPrivilegeHolder.PRIV_SET_REPRESENTATIVE_IMAGE);
    }

    @Override
    public void setPrivSetRepresentativeImage(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_SET_REPRESENTATIVE_IMAGE);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_SET_REPRESENTATIVE_IMAGE);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#isPrivCms()
     */
    @Override
    public boolean isPrivCmsPages() {
        return hasPrivilege(IPrivilegeHolder.PRIV_CMS_PAGES);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#setPrivCmsPages(boolean)
     */
    @Override
    public void setPrivCmsPages(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_CMS_PAGES);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_CMS_PAGES);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#isPrivCmsAllSubthemes()
     */
    @Override
    public boolean isPrivCmsAllSubthemes() {
        return hasPrivilege(IPrivilegeHolder.PRIV_CMS_ALL_SUBTHEMES);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#setPrivCmsAllSubthemes(boolean)
     */
    @Override
    public void setPrivCmsAllSubthemes(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_CMS_ALL_SUBTHEMES);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_CMS_ALL_SUBTHEMES);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#isPrivCmsAllCategories()
     */
    @Override
    public boolean isPrivCmsAllCategories() {
        return hasPrivilege(IPrivilegeHolder.PRIV_CMS_ALL_CATEGORIES);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#setPrivCmsAllCategories(boolean)
     */
    @Override
    public void setPrivCmsAllCategories(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_CMS_ALL_CATEGORIES);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_CMS_ALL_CATEGORIES);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#isPrivCmsAllTemplates()
     */
    @Override
    public boolean isPrivCmsAllTemplates() {
        return hasPrivilege(IPrivilegeHolder.PRIV_CMS_ALL_TEMPLATES);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#setPrivCmsAllTemplates(boolean)
     */
    @Override
    public void setPrivCmsAllTemplates(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_CMS_ALL_TEMPLATES);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_CMS_ALL_TEMPLATES);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#isPrivCmsMenu()
     */
    @Override
    public boolean isPrivCmsMenu() {
        return hasPrivilege(IPrivilegeHolder.PRIV_CMS_MENU);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#setPrivCmsMenu(boolean)
     */
    @Override
    public void setPrivCmsMenu(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_CMS_MENU);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_CMS_MENU);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#isPrivCmsStaticPages()
     */
    @Override
    public boolean isPrivCmsStaticPages() {
        return hasPrivilege(IPrivilegeHolder.PRIV_CMS_STATIC_PAGES);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#setPrivCmsStaticPages(boolean)
     */
    @Override
    public void setPrivCmsStaticPages(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_CMS_STATIC_PAGES);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_CMS_STATIC_PAGES);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#isPrivCmsCollections()
     */
    @Override
    public boolean isPrivCmsCollections() {
        return hasPrivilege(IPrivilegeHolder.PRIV_CMS_COLLECTIONS);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#setPrivCmsCollections(boolean)
     */
    @Override
    public void setPrivCmsCollections(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_CMS_COLLECTIONS);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_CMS_COLLECTIONS);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#isPrivCmsCategories()
     */
    @Override
    public boolean isPrivCmsCategories() {
        return hasPrivilege(IPrivilegeHolder.PRIV_CMS_CATEGORIES);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#setPrivCmsCategories(boolean)
     */
    @Override
    public void setPrivCmsCategories(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_CMS_CATEGORIES);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_CMS_CATEGORIES);
        }
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the licenseType
     */
    public LicenseType getLicenseType() {
        return licenseType;
    }

    /**
     * @param licenseType the licenseType to set
     */
    public void setLicenseType(LicenseType licenseType) {
        this.licenseType = licenseType;
        logger.trace("setLicenseType: {}", licenseType.getName());
    }

    /**
     * @return the user
     */
    public User getUser() {
        return user;
    }

    /**
     * @param user the user to set
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * @return the userGroup
     */
    public UserGroup getUserGroup() {
        return userGroup;
    }

    /**
     * @param userGroup the userGroup to set
     */
    public void setUserGroup(UserGroup userGroup) {
        this.userGroup = userGroup;
    }

    /**
     * @return the ipRange
     */
    public IpRange getIpRange() {
        return ipRange;
    }

    /**
     * @param ipRange the ipRange to set
     */
    public void setIpRange(IpRange ipRange) {
        this.ipRange = ipRange;
    }

    /**
     * @return the start
     */
    public Date getStart() {
        return start;
    }

    /**
     * @param start the start to set
     */
    public void setStart(Date start) {
        this.start = start;
    }

    /**
     * @return the end
     */
    public Date getEnd() {
        return end;
    }

    /**
     * @param end the end to set
     */
    public void setEnd(Date end) {
        this.end = end;
    }

    /**
     * @return the privileges
     */
    public Set<String> getPrivileges() {
        return privileges;
    }

    /**
     * @param privileges the privileges to set
     */
    public void setPrivileges(Set<String> privileges) {
        this.privileges = privileges;
    }

    /**
     * @return the conditions
     */
    public String getConditions() {
        return conditions;
    }

    /**
     * @param conditions the conditions to set
     */
    public void setConditions(String conditions) {
        this.conditions = conditions;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the subthemeDiscriminatorValues
     */
    public List<String> getSubthemeDiscriminatorValues() {
        return subthemeDiscriminatorValues;
    }

    /**
     * @param subthemeDiscriminatorValues the subthemeDiscriminatorValues to set
     */
    public void setSubthemeDiscriminatorValues(List<String> subthemeDiscriminatorValues) {
        this.subthemeDiscriminatorValues = subthemeDiscriminatorValues;
    }

    /**
     * @return the allowedCategories
     */
    public List<CMSCategory> getAllowedCategories() {
        return allowedCategories;
    }

    /**
     * @param allowedCategories the allowedCategories to set
     */
    public void setAllowedCategories(List<CMSCategory> allowedCategories) {
        this.allowedCategories = allowedCategories;
    }

    /**
     * @return the allowedCmsTemplates
     */
    public List<String> getAllowedCmsTemplates() {
        return allowedCmsTemplates;
    }

    /**
     * @param allowedCmsTemplates the allowedCmsTemplates to set
     */
    public void setAllowedCmsTemplates(List<String> allowedCmsTemplates) {
        this.allowedCmsTemplates = allowedCmsTemplates;
    }
}