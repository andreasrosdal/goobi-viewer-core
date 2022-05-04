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
package io.goobi.viewer.model.job.upload;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.servlet.http.Part;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.glassfish.jersey.client.ClientProperties;
import org.jboss.weld.exceptions.IllegalArgumentException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.json.JSONException;
import org.json.JSONObject;
import org.omnifaces.util.Servlets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.HTTPException;
import io.goobi.viewer.exceptions.UploadException;
import io.goobi.viewer.model.job.JobStatus;
import io.goobi.viewer.model.job.download.AbstractTaskManagerRequest;

/**
 * <p>
 * Abstract DownloadJob class.
 * </p>
 */
@Entity
@Table(name = "upload_jobs")
@JsonInclude(Include.NON_NULL)
public class UploadJob implements Serializable {

    private static final long serialVersionUID = 2732786560804670250L;

    private static final Logger logger = LoggerFactory.getLogger(UploadJob.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "upload_job_id")
    private Long id;

    @Column(name = "date_created", nullable = false)
    private LocalDateTime dateCreated;

    /** User ID of the creator. */
    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    /** E-mail notification address. */
    @Column(name = "email", nullable = false)
    private String email;

    /** User consent for being contacted. */
    @Column(name = "consent", nullable = false)
    private boolean consent = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    protected JobStatus status = JobStatus.WAITING;

    /** Error messages, etc. */
    @Column(name = "message", nullable = true)
    protected String message;

    /** Assigned Goobi workflow process ID. */
    @Column(name = "process_id", nullable = false)
    protected Integer processId;

    @Column(name = "pi", nullable = false)
    protected String pi = String.valueOf(UUID.randomUUID());

    /** Title field. */
    @Column(name = "title", columnDefinition = "LONGTEXT", nullable = false)
    protected String title;

    /** Description field. */
    @Column(name = "description", columnDefinition = "LONGTEXT")
    protected String description;

    @Transient
    private String templateName = DataManager.getInstance().getConfiguration().getContentUploadTemplateName();

    @Transient
    private String docstruct = DataManager.getInstance().getConfiguration().getContentUploadDocstruct();

    @Transient
    private List<Part> files;

    public static Response postJobRequest(String url, AbstractTaskManagerRequest body) throws IOException {
        try {
            Client client = ClientBuilder.newClient();
            client.property(ClientProperties.CONNECT_TIMEOUT, 12000);
            client.property(ClientProperties.READ_TIMEOUT, 30000);
            return client
                    .target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .post(javax.ws.rs.client.Entity.entity(body, MediaType.APPLICATION_JSON));
        } catch (Throwable e) {
            throw new IOException("Error connecting to " + url, e);
        }
    }

    /**
     * 
     * @return {@link Document}
     * @should create xml document correctly
     */
    @Deprecated
    Document buildXmlBody() {
        Document doc = new Document();
        Element root = new Element("record")
                .addContent(new Element("identifier").setText(getPi()))
                .addContent(new Element("processtitle").setText(createAtstsl(title, null) + "_" + pi))
                .addContent(new Element("docstruct").setText(docstruct));
        doc.setRootElement(root);

        Element eleMetadataList = new Element("metadataList")
                .addContent(new Element("metadata").setAttribute("name", "TitleDocMain").setText(getTitle()))
                .addContent(new Element("metadata").setAttribute("name", "Description").setText(getDescription()));
        root.addContent(eleMetadataList);

        Element propertyList = new Element("propertyList").addContent(new Element("property").setAttribute("name", "email").setText(email));
        root.addContent(propertyList);

        return doc;
    }

    /**
     * 
     * @return {@link ProcessCreationRequest}
     * @should create request object correctly
     */
    ProcessCreationRequest buildProcessCreationRequest() {
        ProcessCreationRequest ret = new ProcessCreationRequest();
        ret.setTemplateName(templateName);
        ret.setIdentifier(pi);
        ret.setProcesstitle(createAtstsl(title, null) + "_" + pi);
        ret.setLogicalDSType(docstruct);

        ret.setMetadata(new HashMap<>(2));
        ret.getMetadata().put("TitleDocMain", title);
        ret.getMetadata().put("Description", description);

        ret.setProperties(new HashMap<>(1));
        ret.getProperties().put("email", email);

        try {
            logger.trace(new ObjectMapper().writeValueAsString(ret));
        } catch (JsonProcessingException e) {
            logger.trace(e.getMessage());
        }

        return ret;
    }

    /**
     * @throws UploadException
     */
    public void createProcess() throws UploadException {
        logger.trace("createProcess");
        String url = DataManager.getInstance().getConfiguration().getWorkflowRestUrl() + "processes";
        ProcessCreationRequest pcr = buildProcessCreationRequest();
        try {
            // Create new process via REST
            String body = new ObjectMapper().writeValueAsString(pcr);
            String response = NetTools.getWebContentPOST(url,
                    Collections.singletonMap("token", DataManager.getInstance().getConfiguration().getContentUploadToken()), null, null,
                    MediaType.APPLICATION_JSON, body, null);
            if (StringUtils.isEmpty(response)) {
                logger.error("No response received.");
                throw new UploadException("No XML response received.");
            }
            logger.trace(response);
            CreationResponse cr = new ObjectMapper().readValue(response, CreationResponse.class);
            if (cr == null) {
                logger.error("Could not parse response JSON.");
                throw new UploadException("Could not parse response JSON.");
            }
            if (!"success".equals(cr.getResult())) {
                throw new UploadException(cr.getErrorText());
            }

            try {
                // Persist UploadJob
                setProcessId(cr.getProcessId());
                setDateCreated(LocalDateTime.now());
                if (DataManager.getInstance().getDao().addUploadJob(this)) {
                    return;
                }
            } catch (NumberFormatException e) {
                logger.error("Cannot parse process ID: {}", processId);
                throw new UploadException("Cannot parse process ID: " + processId);
            } catch (DAOException e) {
                logger.error(e.getMessage());
                throw new UploadException(e.getMessage());
            }
        } catch (ClientProtocolException e) {
            logger.error(e.getMessage());
            throw new UploadException(e.getMessage());
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new UploadException(e.getMessage());
        } catch (HTTPException e) {
            logger.error(e.getMessage());
            throw new UploadException(e.getMessage());
        }
    }

    /**
     * 
     * @throws IOException
     * @throws HTTPException
     */
    public void uploadFiles() throws IOException, HTTPException {
        logger.trace("uploadFiles");
        if (getFiles() == null) {
            logger.debug("No files to upload.");
            return;
        }

        Path tempFolder = Paths.get(DataManager.getInstance().getConfiguration().getTempFolder());
        if (!Files.exists(tempFolder)) {
            Files.createDirectory(tempFolder);
        }

        for (Part file : getFiles()) {
            String fileName = Servlets.getSubmittedFileName(file);
            Path tempFile = Paths.get(tempFolder.toAbsolutePath().toString(), fileName);
            long bytes = Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
            if (bytes > 0) {
                logger.trace("Temp file: {}", tempFile.toAbsolutePath());
                uploadFile(tempFile.toFile());
            }
        }
    }

    /**
     * 
     * @param file
     * @throws HTTPException
     * @throws IOException
     * @throws ClientProtocolException
     */
    public void uploadFile(File file) throws ClientProtocolException, IOException, HTTPException {
        if (file == null) {
            throw new IllegalArgumentException("file may not be null");
        }
        String url = DataManager.getInstance().getConfiguration().getWorkflowRestUrl() + "processes/" + processId + "/images/master";
        String response = NetTools.getWebContentPOST(url,
                Collections.singletonMap("token", DataManager.getInstance().getConfiguration().getContentUploadToken()), null, null, null, null,
                file);
        if (StringUtils.isNotEmpty(response)) {
            logger.trace(response);
        }
    }

    /**
     * <p>
     * getJobStatus.
     * </p>
     *
     * @param processId a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getJobStatus(Integer processId) {
        StringBuilder url = new StringBuilder()
                .append(DataManager.getInstance().getConfiguration().getWorkflowRestUrl())
                .append("process/details/id/")
                .append(processId)
                .append('/');
        ResponseHandler<String> handler = new BasicResponseHandler();

        HttpGet httpGet = new HttpGet(url.toString());
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            CloseableHttpResponse response = httpclient.execute(httpGet);
            String ret = handler.handleResponse(response);
            logger.trace("TaskManager response: {}", ret);
            return ret;
        } catch (Throwable e) {
            logger.error("Error getting response from TaskManager", e);
            return "";
        }
    }

    /**
     * <p>
     * updateStatus.
     * </p>
     */
    public void updateStatus() {
        String result = getJobStatus(processId);
        try {
            JSONObject object = new JSONObject(result);
            String statusString = object.getString("status");
            JobStatus status = JobStatus.getByName(statusString);
            if (status != null && !status.equals(getStatus())) {
                setStatus(status);
                logger.trace("Status updated for job {}: {}", id, status);
            }
            if (JobStatus.ERROR.equals(status)) {
                String errorMessage = object.getString("errorMessage");
                setMessage(errorMessage);
            }
        } catch (JSONException e) {
            setStatus(JobStatus.ERROR);
            setMessage("Unable to parse TaskManager response");
        }
    }

    /**
     * 
     * @param title
     * @param author
     * @return
     */
    String createAtstsl(String title, String author) {
        StringBuilder result = new StringBuilder(8);
        if (author != null && author.trim().length() > 0) {
            result.append(author.length() > 4 ? author.substring(0, 4) : author);
            result.append(title.length() > 4 ? title.substring(0, 4) : title);
        } else {
            StringTokenizer titleWords = new StringTokenizer(title);
            int wordNo = 1;
            while (titleWords.hasMoreTokens() && wordNo < 5) {
                String word = titleWords.nextToken();
                switch (wordNo) {
                    case 1:
                        result.append(word.length() > 4 ? word.substring(0, 4) : word);
                        break;
                    case 2:
                    case 3:
                        result.append(word.length() > 2 ? word.substring(0, 2) : word);
                        break;
                    case 4:
                        result.append(word.length() > 1 ? word.substring(0, 1) : word);
                        break;
                }
                wordNo++;
            }
        }
        String res = convertUmlaut(result.toString()).toLowerCase();
        return res.replaceAll("[\\W]", ""); // delete umlauts etc.
    }

    /**
     * 
     * @param inString
     * @return
     */
    static String convertUmlaut(String inString) {
        String temp = inString;
        String filename = DataManager.getInstance().getConfiguration().getUmlautsFilePath();

        try (FileInputStream fis = new FileInputStream(filename); InputStreamReader isr = new InputStreamReader(fis, "UTF8");
                BufferedReader in = new BufferedReader(isr);) {
            String str;
            while ((str = in.readLine()) != null) {
                if (str.length() > 0) {
                    temp = temp.replaceAll(str.split(" ")[0], str.split(" ")[1]);
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        return temp;
    }

    /**
     * <p>
     * Getter for the field <code>id</code>.
     * </p>
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * <p>
     * Setter for the field <code>id</code>.
     * </p>
     *
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the dateCreated
     */
    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    /**
     * @param dateCreated the dateCreated to set
     */
    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    /**
     * @return the creatorId
     */
    public Long getCreatorId() {
        return creatorId;
    }

    /**
     * @param creatorId the creatorId to set
     */
    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * @return the consent
     */
    public boolean isConsent() {
        return consent;
    }

    /**
     * @param consent the consent to set
     */
    public void setConsent(boolean consent) {
        this.consent = consent;
    }

    /**
     * <p>
     * Getter for the field <code>status</code>.
     * </p>
     *
     * @return the status
     */
    public JobStatus getStatus() {
        if (status == null) {
            status = JobStatus.UNDEFINED;
        }
        return status;
    }

    /**
     * <p>
     * Setter for the field <code>status</code>.
     * </p>
     *
     * @param status the status to set
     */
    public void setStatus(JobStatus status) {
        this.status = status;
    }

    /**
     * <p>
     * Getter for the field <code>message</code>.
     * </p>
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * <p>
     * Setter for the field <code>message</code>.
     * </p>
     *
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * <p>
     * Getter for the field <code>pi</code>.
     * </p>
     *
     * @return the pi
     */
    public String getPi() {
        return pi;
    }

    /**
     * <p>
     * Setter for the field <code>pi</code>.
     * </p>
     *
     * @param pi the pi to set
     */
    public void setPi(String pi) {
        this.pi = pi;
    }

    /**
     * @return the processId
     */
    public Integer getProcessId() {
        return processId;
    }

    /**
     * @param processId the processId to set
     */
    public void setProcessId(Integer processId) {
        this.processId = processId;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * <p>
     * Getter for the field <code>description</code>.
     * </p>
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * <p>
     * Setter for the field <code>description</code>.
     * </p>
     *
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the templateName
     */
    String getTemplateName() {
        return templateName;
    }

    /**
     * @param templateName the templateName to set
     */
    void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    /**
     * @return the docstruct
     */
    String getDocstruct() {
        return docstruct;
    }

    /**
     * @param docstruct the docstruct to set
     */
    void setDocstruct(String docstruct) {
        this.docstruct = docstruct;
    }

    /**
     * @return the files
     */
    public List<Part> getFiles() {
        return files;
    }

    /**
     * @param files the files to set
     */
    public void setFiles(List<Part> files) {
        this.files = files;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("UploadJob process ID ").append(getProcessId()).append("; ");
        sb.append("Status ").append(getStatus()).append("; ");
        sb.append("PI ").append(getPi()).append("; ");
        return sb.toString();
    }
}
