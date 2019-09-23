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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordStatistic.CampaignRecordStatus;
import io.goobi.viewer.model.crowdsourcing.questions.Question;
import io.goobi.viewer.model.security.user.User;

/**
 *  An item containing a campaign and a source to be annotated. 
 *  Used to set up a frontend annotation view in javascript as well as process status changes created by that view
 *
 * @author florian
 *
 */
@JsonInclude(Include.NON_NULL)
public class CampaignItem {

    private URI source;
    private Campaign campaign;
    private CampaignRecordStatus recordStatus = null;
    @JsonProperty("creator")
    private URI creatorURI = null;

    /**
     * URI to a iiif manifest or other collection of iiif canvases. All generated annotations target either the source itself or one of its canvases
     * 
     * @return the source
     */
    public URI getSource() {
        return source;
    }

    /**
     * @param source the source to set
     */
    public void setSource(URI source) {
        this.source = source;
    }

    /**
     * The {@link Campaign} to create the annotations 
     * 
     * @return the campaign
     */
    public Campaign getCampaign() {
        return campaign;
    }

    /**
     * @param campaign the campaign to set
     */
    public void setCampaign(Campaign campaign) {
        this.campaign = campaign;
    }

    /**
     * @return a new list containing all queries
     */
    @JsonIgnore
    public List<Question> getQuestions() {
        return new ArrayList<>(campaign.getQuestions());
    }

    /**
     * The {@link CampaignRecordStatus status} of the resource within the {@link Campaign}
     * 
     * @return the recordStatus
     */
    public CampaignRecordStatus getRecordStatus() {
        return recordStatus;
    }
    
    /**
     * @param recordStatus the recordStatus to set
     */
    public void setRecordStatus(CampaignRecordStatus recordStatus) {
        this.recordStatus = recordStatus;
    }
    
    /**
     * @return true exactly if {@link #getRecordStatus()} is {@link CampaignRecordStatus.FINISHED FINISHED}
     */
    public boolean isFinished() {
        return CampaignRecordStatus.FINISHED.equals(getRecordStatus());
    }

    /**
     * @return true exactly if {@link #getRecordStatus()} is {@link CampaignRecordStatus.REVIEW REVIEW}
     */
    public boolean isInReview() {
        return CampaignRecordStatus.REVIEW.equals(getRecordStatus());
    }
    
    /**
     * URI for a user who edited the status of this item in the crowdsourcing frontend. The actual {@link User}-Id
     * may be determined by calling {@link User#getId(URI)}
     * 
     * @return the creatorURI
     */
    public URI getCreatorURI() {
        return creatorURI;
    }
    
    /**
     * @param creatorURI the creatorURI to set
     */
    public void setCreatorURI(URI creatorURI) {
        this.creatorURI = creatorURI;
    }
    
}
