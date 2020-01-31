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

/**
 * <p>IPrivilegeHolder interface.</p>
 */
public interface IPrivilegeHolder {

    // Data access privileges
    /** Constant <code>_PRIV_PREFIX="PRIV_"</code> */
    public static final String _PRIV_PREFIX = "PRIV_";
    /** Constant <code>PRIV_LIST="LIST"</code> */
    public static final String PRIV_LIST = "LIST";
    /** Constant <code>PRIV_VIEW_IMAGES="VIEW_IMAGES"</code> */
    public static final String PRIV_VIEW_IMAGES = "VIEW_IMAGES";
    /** Constant <code>PRIV_VIEW_THUMBNAILS="VIEW_THUMBNAILS"</code> */
    public static final String PRIV_VIEW_THUMBNAILS = "VIEW_THUMBNAILS";
    /** Constant <code>PRIV_VIEW_FULLTEXT="VIEW_FULLTEXT"</code> */
    public static final String PRIV_VIEW_FULLTEXT = "VIEW_FULLTEXT";
    /** Constant <code>PRIV_VIEW_VIDEO="VIEW_VIDEO"</code> */
    public static final String PRIV_VIEW_VIDEO = "VIEW_VIDEO";
    /** Constant <code>PRIV_VIEW_AUDIO="VIEW_AUDIO"</code> */
    public static final String PRIV_VIEW_AUDIO = "VIEW_AUDIO";
    /** Constant <code>PRIV_DOWNLOAD_PDF="DOWNLOAD_PDF"</code> */
    public static final String PRIV_DOWNLOAD_PDF = "DOWNLOAD_PDF";
    /** Constant <code>PRIV_DOWNLOAD_PAGE_PDF="DOWNLOAD_PAGE_PDF"</code> */
    public static final String PRIV_DOWNLOAD_PAGE_PDF = "DOWNLOAD_PAGE_PDF";
    /** Constant <code>PRIV_DOWNLOAD_ORIGINAL_CONTENT="DOWNLOAD_ORIGINAL_CONTENT"</code> */
    public static final String PRIV_DOWNLOAD_ORIGINAL_CONTENT = "DOWNLOAD_ORIGINAL_CONTENT";

    // Role privileges
    /** Constant <code>PRIV_DELETE_OCR_PAGE="DELETE_OCR_PAGE"</code> */
    public static final String PRIV_DELETE_OCR_PAGE = "DELETE_OCR_PAGE";
    /** Constant <code>PRIV_SET_REPRESENTATIVE_IMAGE="SET_REPRESENTATIVE_IMAGE"</code> */
    public static final String PRIV_SET_REPRESENTATIVE_IMAGE = "SET_REPRESENTATIVE_IMAGE";
    /** Constant <code>PRIV_CMS_PAGES="CMS_PAGES"</code> */
    public static final String PRIV_CMS_PAGES = "CMS_PAGES";
    /** Constant <code>PRIV_CMS_ALL_SUBTHEMES="CMS_ALL_SUBTHEMES"</code> */
    public static final String PRIV_CMS_ALL_SUBTHEMES = "CMS_ALL_SUBTHEMES";
    /** Constant <code>PRIV_CMS_ALL_CATEGORIES="CMS_ALL_CATEGORIES"</code> */
    public static final String PRIV_CMS_ALL_CATEGORIES = "CMS_ALL_CATEGORIES";
    /** Constant <code>PRIV_CMS_ALL_TEMPLATES="CMS_ALL_TEMPLATES"</code> */
    public static final String PRIV_CMS_ALL_TEMPLATES = "CMS_ALL_TEMPLATES";
    /** Constant <code>PRIV_CMS_MENU="CMS_MENU"</code> */
    public static final String PRIV_CMS_MENU = "CMS_MENU";
    /** Constant <code>PRIV_CMS_STATIC_PAGES="CMS_STATIC_PAGES"</code> */
    public static final String PRIV_CMS_STATIC_PAGES = "CMS_STATIC_PAGES";
    /** Constant <code>PRIV_CMS_COLLECTIONS="CMS_COLLECTIONS"</code> */
    public static final String PRIV_CMS_COLLECTIONS = "CMS_COLLECTIONS";
    /** Constant <code>PRIV_CMS_CATEGORIES="CMS_CATEGORIES"</code> */
    public static final String PRIV_CMS_CATEGORIES = "CMS_CATEGORIES";
    /** Constant <code>PRIV_CROWDSOURCING_ALL_CAMPAIGNS="CROWDSOURCING_ALL_CAMPAIGNS"</code> */
    public static final String PRIV_CROWDSOURCING_ALL_CAMPAIGNS = "CROWDSOURCING_ALL_CAMPAIGNS";
    /** Constant <code>PRIV_CROWDSOURCING_ANNOTATE_CAMPAIGN="CROWDSOURCING_ANNOTATE_CAMPAIGN"</code> */
    public static final String PRIV_CROWDSOURCING_ANNOTATE_CAMPAIGN = "CROWDSOURCING_ANNOTATE_CAMPAIGN";
    /** Constant <code>PRIV_CROWDSOURCING_REVIEW_CAMPAIGN="CROWDSOURCING_REVIEW_CAMPAIGN"</code> */
    public static final String PRIV_CROWDSOURCING_REVIEW_CAMPAIGN = "CROWDSOURCING_REVIEW_CAMPAIGN";

    /**
     * <p>hasPrivilege.</p>
     *
     * @param privilege a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean hasPrivilege(String privilege);

    /**
     * <p>isPrivList.</p>
     *
     * @return a boolean.
     */
    public boolean isPrivList();

    /**
     * <p>setPrivList.</p>
     *
     * @param priv a boolean.
     */
    public void setPrivList(boolean priv);

    /**
     * <p>isPrivViewImages.</p>
     *
     * @return a boolean.
     */
    public boolean isPrivViewImages();

    /**
     * <p>setPrivViewImages.</p>
     *
     * @param priv a boolean.
     */
    public void setPrivViewImages(boolean priv);

    /**
     * <p>isPrivViewThumbnails.</p>
     *
     * @return a boolean.
     */
    public boolean isPrivViewThumbnails();

    /**
     * <p>setPrivViewThumbnails.</p>
     *
     * @param priv a boolean.
     */
    public void setPrivViewThumbnails(boolean priv);

    /**
     * <p>isPrivViewFulltext.</p>
     *
     * @return a boolean.
     */
    public boolean isPrivViewFulltext();

    /**
     * <p>setPrivViewFulltext.</p>
     *
     * @param priv a boolean.
     */
    public void setPrivViewFulltext(boolean priv);

    /**
     * <p>isPrivViewVideo.</p>
     *
     * @return a boolean.
     */
    public boolean isPrivViewVideo();

    /**
     * <p>setPrivViewVideo.</p>
     *
     * @param priv a boolean.
     */
    public void setPrivViewVideo(boolean priv);

    /**
     * <p>isPrivViewAudio.</p>
     *
     * @return a boolean.
     */
    public boolean isPrivViewAudio();

    /**
     * <p>setPrivViewAudio.</p>
     *
     * @param priv a boolean.
     */
    public void setPrivViewAudio(boolean priv);

    /**
     * <p>isPrivDownloadPdf.</p>
     *
     * @return a boolean.
     */
    public boolean isPrivDownloadPdf();

    /**
     * <p>setPrivDownloadPdf.</p>
     *
     * @param priv a boolean.
     */
    public void setPrivDownloadPdf(boolean priv);

    /**
     * <p>isPrivDownloadPagePdf.</p>
     *
     * @return a boolean.
     */
    public boolean isPrivDownloadPagePdf();

    /**
     * <p>setPrivDownloadPagePdf.</p>
     *
     * @param priv a boolean.
     */
    public void setPrivDownloadPagePdf(boolean priv);

    /**
     * <p>isPrivDownloadOriginalContent.</p>
     *
     * @return a boolean.
     */
    public boolean isPrivDownloadOriginalContent();

    /**
     * <p>setPrivDownloadOriginalContent.</p>
     *
     * @param priv a boolean.
     */
    public void setPrivDownloadOriginalContent(boolean priv);

    /**
     * <p>isPrivDeleteOcrPage.</p>
     *
     * @return a boolean.
     */
    public boolean isPrivDeleteOcrPage();

    /**
     * <p>setPrivDeleteOcrPage.</p>
     *
     * @param priv a boolean.
     */
    public void setPrivDeleteOcrPage(boolean priv);

    /**
     * <p>isPrivSetRepresentativeImage.</p>
     *
     * @return a boolean.
     */
    public boolean isPrivSetRepresentativeImage();

    /**
     * <p>setPrivSetRepresentativeImage.</p>
     *
     * @param priv a boolean.
     */
    public void setPrivSetRepresentativeImage(boolean priv);

    /**
     * <p>isPrivCmsPages.</p>
     *
     * @return a boolean.
     */
    public boolean isPrivCmsPages();

    /**
     * <p>setPrivCmsPages.</p>
     *
     * @param priv a boolean.
     */
    public void setPrivCmsPages(boolean priv);

    /**
     * <p>isPrivCmsMenu.</p>
     *
     * @return a boolean.
     */
    public boolean isPrivCmsMenu();

    /**
     * <p>setPrivCmsMenu.</p>
     *
     * @param priv a boolean.
     */
    public void setPrivCmsMenu(boolean priv);

    /**
     * <p>isPrivCmsAllSubthemes.</p>
     *
     * @return a boolean.
     */
    public boolean isPrivCmsAllSubthemes();

    /**
     * <p>setPrivCmsAllSubthemes.</p>
     *
     * @param priv a boolean.
     */
    public void setPrivCmsAllSubthemes(boolean priv);

    /**
     * <p>isPrivCmsAllCategories.</p>
     *
     * @return a boolean.
     */
    public boolean isPrivCmsAllCategories();

    /**
     * <p>setPrivCmsAllCategories.</p>
     *
     * @param priv a boolean.
     */
    public void setPrivCmsAllCategories(boolean priv);

    /**
     * <p>isPrivCmsAllTemplates.</p>
     *
     * @return a boolean.
     */
    public boolean isPrivCmsAllTemplates();

    /**
     * <p>setPrivCmsAllTemplates.</p>
     *
     * @param priv a boolean.
     */
    public void setPrivCmsAllTemplates(boolean priv);

    /**
     * <p>isPrivCmsStaticPages.</p>
     *
     * @return a boolean.
     */
    public boolean isPrivCmsStaticPages();

    /**
     * <p>setPrivCmsStaticPages.</p>
     *
     * @param priv a boolean.
     */
    public void setPrivCmsStaticPages(boolean priv);

    /**
     * <p>isPrivCmsCollections.</p>
     *
     * @return a boolean.
     */
    public boolean isPrivCmsCollections();

    /**
     * <p>setPrivCmsCollections.</p>
     *
     * @param priv a boolean.
     */
    public void setPrivCmsCollections(boolean priv);

    /**
     * <p>isPrivCmsCategories.</p>
     *
     * @return a boolean.
     */
    public boolean isPrivCmsCategories();

    /**
     * <p>setPrivCmsCategories.</p>
     *
     * @param priv a boolean.
     */
    public void setPrivCmsCategories(boolean priv);

    /**
     * <p>isPrivCrowdsourcingAllCampaigns.</p>
     *
     * @return a boolean.
     */
    public boolean isPrivCrowdsourcingAllCampaigns();

    /**
     * <p>setPrivCrowdsourcingAllCampaigns.</p>
     *
     * @param priv a boolean.
     */
    public void setPrivCrowdsourcingAllCampaigns(boolean priv);

    /**
     * <p>isPrivCrowdsourcingAnnotateCampaign.</p>
     *
     * @return a boolean.
     */
    public boolean isPrivCrowdsourcingAnnotateCampaign();

    /**
     * <p>setPrivCrowdsourcingAnnotateCampaign.</p>
     *
     * @param priv a boolean.
     */
    public void setPrivCrowdsourcingAnnotateCampaign(boolean priv);

    /**
     * <p>isPrivCrowdsourcingReviewCampaign.</p>
     *
     * @return a boolean.
     */
    public boolean isPrivCrowdsourcingReviewCampaign();

    /**
     * <p>setPrivCrowdsourcingReviewCampaign.</p>
     *
     * @param priv a boolean.
     */
    public void setPrivCrowdsourcingReviewCampaign(boolean priv);
}