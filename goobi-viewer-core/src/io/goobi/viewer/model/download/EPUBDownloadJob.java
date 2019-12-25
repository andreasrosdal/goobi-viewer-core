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
package io.goobi.viewer.model.download;

import java.io.File;
import java.util.Date;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.Helper;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.exceptions.DownloadException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.CmsBean;

/**
 * <p>EPUBDownloadJob class.</p>
 *
 */
@Entity
@DiscriminatorValue(EPUBDownloadJob.TYPE)
public class EPUBDownloadJob extends DownloadJob {

    private static final long serialVersionUID = 5799943793394080870L;

    /** Constant <code>TYPE="epub"</code> */
    public static final String TYPE = "epub";

    private static final Logger logger = LoggerFactory.getLogger(EPUBDownloadJob.class);

    /**
     * <p>Constructor for EPUBDownloadJob.</p>
     */
    public EPUBDownloadJob() {
        type = TYPE;
    }

    /**
     * <p>Constructor for EPUBDownloadJob.</p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param logid a {@link java.lang.String} object.
     * @param lastRequested a {@link java.util.Date} object.
     * @param ttl a long.
     */
    public EPUBDownloadJob(String pi, String logid, Date lastRequested, long ttl) {
        type = TYPE;
        this.pi = pi;
        this.logId = logid;
        this.lastRequested = lastRequested;
        this.ttl = ttl;
        this.setStatus(JobStatus.INITIALIZED);
        generateDownloadIdentifier();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.misc.DownloadJob#generateDownloadIdentifier()
     */
    /** {@inheritDoc} */
    @Override
    public final void generateDownloadIdentifier() {
        this.identifier = generateDownloadJobId(TYPE, pi, logId);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.misc.DownloadJob#getMimeType()
     */
    /** {@inheritDoc} */
    @Override
    public String getMimeType() {
        return "application/epub+zip";
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.misc.DownloadJob#getFileExtension()
     */
    /** {@inheritDoc} */
    @Override
    public String getFileExtension() {
        return ".epub";
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.download.DownloadJob#getDisplayName()
     */
    /** {@inheritDoc} */
    @Override
    public String getDisplayName() {
        return "EPUB";
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.download.DownloadJob#getSize()
     */
    /** {@inheritDoc} */
    @Override
    public long getSize() {
        File downloadFile = getDownloadFileStatic(identifier, type, getFileExtension());
        if (downloadFile.isFile()) {
            return downloadFile.length();
        }

        return getEpubSizeFromTaskManager(identifier);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.download.DownloadJob#getQueuePosition()
     */
    /** {@inheritDoc} */
    @Override
    public int getQueuePosition() {
        switch (status) {
            case ERROR:
                return -1;
            case READY:
                return 0;
            default:
                return getEPUBJobsInQueue(identifier);
        }
    }

    /**
     * <p>getEpubSizeFromTaskManager.</p>
     *
     * @param identifier a {@link java.lang.String} object.
     * @return a long.
     */
    protected static long getEpubSizeFromTaskManager(String identifier) {
        StringBuilder url = new StringBuilder();
        url.append(DataManager.getInstance().getConfiguration().getTaskManagerRestUrl());
        url.append("/viewerepub/epubSize/");
        url.append(identifier);
        ResponseHandler<String> handler = new BasicResponseHandler();
        HttpGet httpGet = new HttpGet(url.toString());
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            CloseableHttpResponse response = httpclient.execute(httpGet);
            String ret = handler.handleResponse(response);
            logger.trace("TaskManager response: {}", ret);
            return Long.parseLong(ret);
        } catch (Throwable e) {
            logger.error("Error getting response from TaskManager", e);
            return -1;
        }
    }

    /**
     * <p>getEPUBJobsInQueue.</p>
     *
     * @param identifier a {@link java.lang.String} object.
     * @return a int.
     */
    public static int getEPUBJobsInQueue(String identifier) {
        StringBuilder url = new StringBuilder();
        url.append(DataManager.getInstance().getConfiguration().getTaskManagerRestUrl());
        url.append("/viewerepub/numJobsUntil/");
        url.append(identifier);
        ResponseHandler<String> handler = new BasicResponseHandler();
        HttpGet httpGet = new HttpGet(url.toString());
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            CloseableHttpResponse response = httpclient.execute(httpGet);
            String ret = handler.handleResponse(response);
            logger.trace("TaskManager response: {}", ret);
            return Integer.parseInt(ret);
        } catch (Throwable e) {
            logger.error("Error getting response from TaskManager", e);
            return -1;
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.download.DownloadJob#triggerCreation(java.lang.String, java.lang.String, java.lang.String)
     */
    /** {@inheritDoc} */
    @Override
    protected void triggerCreation() throws PresentationException, IndexUnreachableException {
        triggerCreation(pi, identifier, DataManager.getInstance().getConfiguration().getDownloadFolder(EPUBDownloadJob.TYPE));
    }

    /**
     * <p>triggerCreation.</p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param downloadIdentifier a {@link java.lang.String} object.
     * @param targetFolderPath a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.PresentationException
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException
     * @throws io.goobi.viewer.exceptions.DownloadException if any.
     */
    public static void triggerCreation(String pi, String downloadIdentifier, String targetFolderPath)
            throws PresentationException, IndexUnreachableException, DownloadException {
        File targetFolder = new File(DataManager.getInstance().getConfiguration().getDownloadFolder(EPUBDownloadJob.TYPE));
        if (!targetFolder.isDirectory() && !targetFolder.mkdir()) {
            throw new DownloadException("Cannot create download folder: " + targetFolder);
        }
        String title = pi;

        int priority = 10;
        HttpClient client = HttpClients.createDefault();
        String taskManagerUrl = DataManager.getInstance().getConfiguration().getTaskManagerServiceUrl();
        File metsFile = new File(Helper.getSourceFilePath(pi + ".xml", SolrConstants._METS));
        String mediaRepository = Helper.getDataRepositoryPathForRecord(pi);
        HttpPost post = TaskClient.createPost(taskManagerUrl, metsFile.getAbsolutePath(), targetFolder.getAbsolutePath(),
                CmsBean.getCurrentLocale().getLanguage(), "", priority, "", title, mediaRepository, "VIEWEREPUB", downloadIdentifier,
                "noServerTypeInTaskClient", "", "", "", CmsBean.getCurrentLocale().getLanguage(), false);
        try {
            JSONObject response = TaskClient.getJsonResponse(client, post);
            logger.trace(response.toString());
            if (response.get("STATUS").equals("ERROR")) {
                if (response.get("ERRORMESSAGE").equals("Job already in DB, not adding it!")) {
                    logger.debug("Job is already being processed");
                } else {
                    throw new DownloadException(
                            "Failed to start pdf creation for PI=" + pi + ": TaskManager returned error " + response.get("ERRORMESSAGE"));
                    //                    logger.error("Failed to start pdf creation for PI={} and LOGID={}: TaskManager returned error", pi, logId);
                    //                    return false;
                }
            }
        } catch (Exception e) {
            // Had to catch generic exception here because a ParseException triggered by Tomcat error HTML getting parsed as JSON cannot be caught
            throw new DownloadException("Failed to start pdf creation for PI=" + pi + ": " + e.getMessage());
            //            logger.error("Failed to start epub creation for PI={}: {}", pi, e.getMessage());
            //            logger.error(e.getMessage(), e);
            //            return false;
        }
    }
}
