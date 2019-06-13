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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author florian
 *
 */
@Entity
@Table(name = "cms_categories")
public class CMSCategory implements Comparable<CMSCategory> {

    /** Unique database ID. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long id;

    @Column(name = "name", columnDefinition = "LONGTEXT")
    private String name;

    @Column(name = "description", columnDefinition = "LONGTEXT")
    private String description;

    public CMSCategory() {

    }

    public CMSCategory(String name) {
        this.name = name;
        this.id = null;
        this.description = "";
    }

    public CMSCategory(CMSCategory blueprint, boolean keepId) {
        this.id = keepId ? blueprint.id : null;
        this.name = blueprint.name;
        this.description = blueprint.description;
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
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
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

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj.getClass().equals(CMSCategory.class)) {
            if (((CMSCategory) obj).getId() != null && this.getId() != null) {
                return ((CMSCategory) obj).getId().equals(this.getId());
            }
            return false;
        }
        return false;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        if (getId() != null) {
            return getId().hashCode();
        } else if (getName() != null) {
            return getName().hashCode();
        } else {
            return super.hashCode();
        }
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
    	return getName();
    }

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(CMSCategory other) {
		if(other != null) {
			return this.getName().compareTo(other.getName());
		} else {
			return 0;
		}
	}

}