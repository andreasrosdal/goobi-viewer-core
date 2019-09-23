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
package io.goobi.viewer.model.crowdsourcing.campaigns;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.goobi.viewer.model.security.user.User;

/**
 * Annotation status of a record in the context of a particular campaign.
 */
@Entity
@Table(name = "cs_campaign_record_statistics")
@JsonInclude(Include.NON_EMPTY)
public class CampaignRecordStatistic implements Serializable {

    /**
     * The status of a specific resource (iiif manifest or similar) within a campaign
     * 
     * @author florian
     *
     */
    public enum CampaignRecordStatus {
        /**
         * Annotations may be made to this resource
         */
        ANNOTATE,
        /**
         * Annotations are ready to be reviewed
         */
        REVIEW,
        /**
         * All annotations for this resource are accepted by the review process. 
         * The resource is not available for further annotating within this campaign; 
         * all annotations for this resource and campaign may be visible in iiif manifests and the viewer
         */
        FINISHED;

        public String getName() {
            return this.name();
        }

        public static CampaignRecordStatus forName(String name) {
            for (CampaignRecordStatus status : CampaignRecordStatus.values()) {
                if (status.getName().equalsIgnoreCase(name)) {
                    return status;
                }
            }
            return null;
        }
    }

    private static final long serialVersionUID = 8902904205183851565L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "campaign_record_statistic_id")
    private Long id;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_created", nullable = false)
    @JsonIgnore
    private Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_updated")
    @JsonIgnore
    private Date dateUpdated;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    @JsonIgnore
    private Campaign owner;

    @Column(name = "pi", nullable = false)
    private String pi;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @JsonIgnore
    private CampaignRecordStatus status;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "cs_campaign_record_statistic_annotators", joinColumns = @JoinColumn(name = "campaign_record_statistic_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private List<User> annotators = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "cs_campaign_record_statistic_reviewers", joinColumns = @JoinColumn(name = "campaign_record_statistic_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private List<User> reviewers = new ArrayList<>();

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((owner == null) ? 0 : owner.hashCode());
        result = prime * result + ((pi == null) ? 0 : pi.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CampaignRecordStatistic other = (CampaignRecordStatistic) obj;
        if (owner == null) {
            if (other.owner != null)
                return false;
        } else if (!owner.equals(other.owner))
            return false;
        if (pi == null) {
            if (other.pi != null)
                return false;
        } else if (!pi.equals(other.pi))
            return false;
        return true;
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
     * @return the dateCreated
     */
    public Date getDateCreated() {
        return dateCreated;
    }

    /**
     * @param dateCreated the dateCreated to set
     */
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    /**
     * @return the dateUpdated
     */
    public Date getDateUpdated() {
        return dateUpdated;
    }

    /**
     * @param dateUpdated the dateUpdated to set
     */
    public void setDateUpdated(Date dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    /**
     * @return the owner
     */
    public Campaign getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(Campaign owner) {
        this.owner = owner;
    }

    /**
     * @return the pi
     */
    public String getPi() {
        return pi;
    }

    /**
     * @param pi the pi to set
     */
    public void setPi(String pi) {
        this.pi = pi;
    }

    /**
     * @return the status
     */
    public CampaignRecordStatus getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(CampaignRecordStatus status) {
        this.status = status;
    }

    /**
     * @return the annotators
     */
    public List<User> getAnnotators() {
        return annotators;
    }

    /**
     * @param annotators the annotators to set
     */
    public void setAnnotators(List<User> annotators) {
        this.annotators = annotators;
    }

    /**
     * @return the reviewers
     */
    public List<User> getReviewers() {
        return reviewers;
    }

    /**
     * @param reviewers the reviewers to set
     */
    public void setReviewers(List<User> reviewers) {
        this.reviewers = reviewers;
    }
    
    public void addAnnotater(User user) {
        if(user != null && !getAnnotators().contains(user)) {
            getAnnotators().add(user);
        }
    }
    
    public void addReviewer(User user) {
        if(user != null && !getReviewers().contains(user)) {
            getReviewers().add(user);
        }
    }
}
