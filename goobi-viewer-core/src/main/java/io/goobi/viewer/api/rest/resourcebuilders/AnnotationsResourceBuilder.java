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
package io.goobi.viewer.api.rest.resourcebuilders;

import static io.goobi.viewer.api.rest.v1.ApiUrls.ANNOTATIONS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.ANNOTATIONS_ANNOTATION;
import static io.goobi.viewer.api.rest.v1.ApiUrls.ANNOTATIONS_COMMENT;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_MANIFEST;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES_CANVAS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_RECORD;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.intranda.api.annotation.AgentType;
import de.intranda.api.annotation.IAnnotation;
import de.intranda.api.annotation.IAnnotationCollection;
import de.intranda.api.annotation.IResource;
import de.intranda.api.annotation.ISelector;
import de.intranda.api.annotation.oa.Motivation;
import de.intranda.api.annotation.oa.OpenAnnotation;
import de.intranda.api.annotation.oa.TextualResource;
import de.intranda.api.annotation.wa.Agent;
import de.intranda.api.annotation.wa.FragmentSelector;
import de.intranda.api.annotation.wa.SpecificResource;
import de.intranda.api.annotation.wa.TypedResource;
import de.intranda.api.annotation.wa.WebAnnotation;
import de.intranda.api.annotation.wa.collection.AnnotationCollection;
import de.intranda.api.annotation.wa.collection.AnnotationCollectionBuilder;
import de.intranda.api.annotation.wa.collection.AnnotationPage;
import de.intranda.api.iiif.presentation.AnnotationList;
import de.intranda.api.iiif.presentation.Canvas;
import de.intranda.api.iiif.presentation.Manifest;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.annotation.Comment;
import io.goobi.viewer.model.annotation.PersistentAnnotation;

/**
 * @author florian
 *
 */
public class AnnotationsResourceBuilder {

    private final AbstractApiUrlManager urls;

    private final static Logger logger = LoggerFactory.getLogger(AnnotationsResourceBuilder.class);

    private final static int MAX_ANNOTATIONS_PER_PAGE = 100;

    public AnnotationsResourceBuilder(AbstractApiUrlManager urls) {
        this.urls = urls;
    }

    public AnnotationCollection getWebnnotationCollection() throws DAOException {
        long count = DataManager.getInstance().getDao().getAnnotationCount(null);
        URI uri = URI.create(urls.path(ANNOTATIONS).build());
        AnnotationCollectionBuilder builder = new AnnotationCollectionBuilder(uri, count);
        AnnotationCollection collection = builder.setItemsPerPage(MAX_ANNOTATIONS_PER_PAGE).buildCollection();
        return collection;
    }

    public AnnotationPage getWebAnnotationPage(Integer page) throws DAOException, IllegalRequestException {
        if (page == null || page < 1) {
            throw new IllegalRequestException("Page number must be at least 1");
        }
        int first = (page - 1) * MAX_ANNOTATIONS_PER_PAGE;
        String sortField = "id";
        List<PersistentAnnotation> data = DataManager.getInstance().getDao().getAnnotations(first, MAX_ANNOTATIONS_PER_PAGE, sortField, true, null);
        if (data.isEmpty()) {
            throw new IllegalRequestException("Page number is out of bounds");
        }
        URI uri = URI.create(urls.path(ANNOTATIONS).build());
        AnnotationCollectionBuilder builder = new AnnotationCollectionBuilder(uri, data.size());
        List<IAnnotation> annos = data.stream().map(this::getAsWebAnnotation).collect(Collectors.toList());
        AnnotationPage annoPage = builder.setItemsPerPage(MAX_ANNOTATIONS_PER_PAGE).buildPage(annos, page);
        return annoPage;
    }

    /**
     * @param pi
     * @param uri
     * @return
     * @throws DAOException
     */
    public AnnotationCollection getWebAnnotationCollectionForRecord(String pi, URI uri) throws DAOException {
        long count = DataManager.getInstance().getDao().getAnnotationCountForWork(pi);
        AnnotationCollectionBuilder builder = new AnnotationCollectionBuilder(uri, count);
        AnnotationCollection collection = builder.setItemsPerPage((int) count).buildCollection();
        if (count > 0) {
            try {
                collection.setFirst(getWebAnnotationPageForRecord(pi, uri, 1));
            } catch (IllegalRequestException e) {
                //no items
            }
        }
        return collection;
    }

    public AnnotationCollection getWebAnnotationCollectionForPage(String pi, Integer pageNo, URI uri) throws DAOException {
        long count = DataManager.getInstance().getDao().getAnnotationCountForTarget(pi, pageNo);
        AnnotationCollectionBuilder builder = new AnnotationCollectionBuilder(uri, count);
        AnnotationCollection collection = builder.setItemsPerPage((int) count).buildCollection();
        if (count > 0) {
            try {
                collection.setFirst(getWebAnnotationPageForPage(pi, pageNo, uri, 1));
            } catch (IllegalRequestException e) {
                //no items
            }
        }
        return collection;
    }

    /**
     * @param pi
     * @param uri
     * @return
     * @throws DAOException
     */
    public AnnotationList getOAnnotationListForRecord(String pi, URI uri) throws DAOException {
        List<PersistentAnnotation> data = DataManager.getInstance().getDao().getAnnotationsForWork(pi);
        AnnotationList list = new AnnotationList(uri);
        data.stream().map(this::getAsOpenAnnotation).forEach(oa -> list.addResource(oa));
        return list;
    }

    /**
     * @param pi
     * @param pageNo
     * @param uri
     * @return
     * @throws DAOException
     */
    public IAnnotationCollection getOAnnotationListForPage(String pi, Integer pageNo, URI uri) throws DAOException {
        List<PersistentAnnotation> data = DataManager.getInstance().getDao().getAnnotationsForTarget(pi, pageNo);
        AnnotationList list = new AnnotationList(uri);
        data.stream().map(this::getAsOpenAnnotation).forEach(oa -> list.addResource(oa));
        return list;
    }

    /**
     * @param uri
     * @param page
     * @return
     * @throws DAOException
     * @throws IllegalRequestException
     */
    public AnnotationPage getWebAnnotationPageForRecord(String pi, URI uri, Integer page) throws DAOException, IllegalRequestException {
        if (page == null || page < 1) {
            throw new IllegalRequestException("Page number must be at least 1");
        }
        List<PersistentAnnotation> data = DataManager.getInstance().getDao().getAnnotationsForWork(pi);
        if (data.isEmpty()) {
            throw new IllegalRequestException("Page number is out of bounds");
        }
        AnnotationCollectionBuilder builder = new AnnotationCollectionBuilder(uri, data.size());
        List<IAnnotation> annos = data.stream().map(this::getAsWebAnnotation).collect(Collectors.toList());
        AnnotationPage annoPage = builder.setItemsPerPage(annos.size()).buildPage(annos, page);
        return annoPage;
    }

    public AnnotationPage getWebAnnotationPageForPage(String pi, Integer pageNo, URI uri, Integer page) throws DAOException, IllegalRequestException {
        if (page == null || page < 1) {
            throw new IllegalRequestException("Page number must be at least 1");
        }
        List<PersistentAnnotation> data = DataManager.getInstance().getDao().getAnnotationsForTarget(pi, pageNo);
        if (data.isEmpty()) {
            throw new IllegalRequestException("Page number is out of bounds");
        }
        AnnotationCollectionBuilder builder = new AnnotationCollectionBuilder(uri, data.size());
        List<IAnnotation> annos = data.stream().map(this::getAsWebAnnotation).collect(Collectors.toList());
        AnnotationPage annoPage = builder.setItemsPerPage(annos.size()).buildPage(annos, page);
        return annoPage;
    }

    /**
     * @param format
     * @param uri
     * @return
     * @throws DAOException
     */
    public AnnotationCollection getWebAnnotationCollectionForRecordComments(String pi, URI uri) throws DAOException {
        List<Comment> data = DataManager.getInstance().getDao().getCommentsForWork(pi);

        AnnotationCollectionBuilder builder = new AnnotationCollectionBuilder(uri, data.size());
        AnnotationCollection collection = builder.setItemsPerPage(data.size()).buildCollection();
        if (data.size() > 0) {
            try {
                collection.setFirst(getWebAnnotationPageForRecordComments(pi, uri, 1));
            } catch (IllegalRequestException e) {
                //no items
            }
        }
        return collection;
    }

    public AnnotationPage getWebAnnotationPageForRecordComments(String pi, URI uri, Integer page) throws DAOException, IllegalRequestException {
        if (page == null || page < 1) {
            throw new IllegalRequestException("Page number must be at least 1");
        }
        List<Comment> data = DataManager.getInstance().getDao().getCommentsForWork(pi);
        if (data.isEmpty()) {
            throw new IllegalRequestException("Page number is out of bounds");
        }
        AnnotationCollectionBuilder builder = new AnnotationCollectionBuilder(uri, data.size());
        AnnotationPage annoPage = builder.setItemsPerPage(data.size())
                .buildPage(
                        data.stream()
                                .map(c -> getAsWebAnnotation(c))
                                .collect(Collectors.toList()),
                        page);

        return annoPage;
    }

    public AnnotationList getOAnnotationListForRecordComments(String pi, URI uri) throws DAOException {
        List<Comment> data = DataManager.getInstance().getDao().getCommentsForWork(pi);

        AnnotationList list = new AnnotationList(uri);
        data.stream().map(c -> getAsOpenAnnotation(c)).forEach(oa -> list.addResource(oa));
        return list;
    }

    public AnnotationList getOAnnotationListForPageComments(String pi, Integer pageNo, URI uri) throws DAOException {
        List<Comment> data = DataManager.getInstance().getDao().getCommentsForPage(pi, pageNo);

        AnnotationList list = new AnnotationList(uri);
        data.stream().map(c -> getAsOpenAnnotation(c)).forEach(oa -> list.addResource(oa));
        return list;
    }

    public AnnotationCollection getWebAnnotationCollectionForPageComments(String pi, Integer pageNo, URI uri) throws DAOException {
        List<Comment> data = DataManager.getInstance().getDao().getCommentsForPage(pi, pageNo);

        AnnotationCollectionBuilder builder = new AnnotationCollectionBuilder(uri, data.size());
        AnnotationCollection collection = builder.setItemsPerPage(MAX_ANNOTATIONS_PER_PAGE).buildCollection();
        if (data.size() > 0) {
            try {
                collection.setFirst(getWebAnnotationPageForPageComments(pi, pageNo, uri, 1));
            } catch (IllegalRequestException e) {
                //no items
            }
        }
        return collection;
    }

    public AnnotationPage getWebAnnotationPageForPageComments(String pi, Integer pageNo, URI uri, Integer page)
            throws DAOException, IllegalRequestException {
        if (page == null || page < 1) {
            throw new IllegalRequestException("Page number must be at least 1");
        }
        List<Comment> data = DataManager.getInstance().getDao().getCommentsForPage(pi, pageNo);
        if (data.isEmpty()) {
            throw new IllegalRequestException("Page number is out of bounds");
        }
        AnnotationCollectionBuilder builder = new AnnotationCollectionBuilder(uri, data.size());
        AnnotationPage annoPage = builder.setItemsPerPage(data.size())
                .buildPage(
                        data.stream()
                                .map(c -> getAsWebAnnotation(c))
                                .collect(Collectors.toList()),
                        page);

        return annoPage;
    }

    public OpenAnnotation getAsOpenAnnotation(Comment comment) {
        OpenAnnotation anno = new OpenAnnotation(getOpenAnnotationCommentURI(comment.getPi(), comment.getPage(), comment.getId()));
        anno.setMotivation(Motivation.COMMENTING);
        if (comment.getPage() != null) {
            anno.setTarget(
                    new Canvas(URI.create(urls.path(RECORDS_RECORD, RECORDS_PAGES_CANVAS).params(comment.getPi(), comment.getPage()).build())));
        } else {
            anno.setTarget(new Manifest(URI.create(urls.path(RECORDS_RECORD, RECORDS_MANIFEST).params(comment.getPi()).build())));
        }
        TextualResource body = new TextualResource(comment.getText());
        anno.setBody(body);
        return anno;
    }

    public WebAnnotation getAsWebAnnotation(Comment comment) {
        WebAnnotation anno = new WebAnnotation(getWebAnnotationCommentURI(comment.getId()));
        anno.setMotivation(de.intranda.api.annotation.wa.Motivation.COMMENTING);
        if (comment.getPage() != null) {
            anno.setTarget(
                    new Canvas(URI.create(urls.path(RECORDS_RECORD, RECORDS_PAGES_CANVAS).params(comment.getPi(), comment.getPage()).build())));
        } else {
            anno.setTarget(new Manifest(URI.create(urls.path(RECORDS_RECORD, RECORDS_MANIFEST).params(comment.getPi()).build())));
        }
        TextualResource body = new TextualResource(comment.getText());
        anno.setBody(body);
        return anno;
    }

    private URI getWebAnnotationURI(Long id) {
        return URI.create(this.urls.path(ANNOTATIONS, ANNOTATIONS_ANNOTATION).params(id).build());
    }

    private URI getOpenAnnotationURI(Long id) {
        return URI.create(this.urls.path(ANNOTATIONS, ANNOTATIONS_ANNOTATION).params(id).query("format", "oa").build());
    }

    private URI getWebAnnotationCommentURI(Long id) {
        return URI.create(urls.path(ANNOTATIONS, ANNOTATIONS_COMMENT).params(id).build());
    }

    private URI getOpenAnnotationCommentURI(String pi, Integer page, Long id) {
        return URI.create(urls.path(ANNOTATIONS, ANNOTATIONS_COMMENT).params(id).query("format", "oa").build());
    }

    /**
     * Get the annotation target as an WebAnnotation {@link de.intranda.api.annotation.IResource} java object
     *
     * @return a {@link de.intranda.api.annotation.IResource} object.
     * @throws com.fasterxml.jackson.core.JsonParseException if any.
     * @throws com.fasterxml.jackson.databind.JsonMappingException if any.
     * @throws java.io.IOException if any.
     */
    public IResource getTargetAsResource(PersistentAnnotation anno) throws JsonParseException, JsonMappingException, IOException {
        if (anno.getTarget() != null) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
            IResource resource;
            if (anno.getTarget().contains("SpecificResource")) {
                resource = mapper.readValue(anno.getTarget(), SpecificResource.class);
            } else {
                resource = mapper.readValue(anno.getTarget(), TypedResource.class);
            }
            return resource;
        }
        return null;
    }

    /**
     * Get the annotation target as an OpenAnnotation {@link de.intranda.api.annotation.IResource} java object
     *
     * @return a {@link de.intranda.api.annotation.IResource} object.
     * @throws com.fasterxml.jackson.core.JsonParseException if any.
     * @throws com.fasterxml.jackson.databind.JsonMappingException if any.
     * @throws java.io.IOException if any.
     */
    public IResource getTargetAsOAResource(PersistentAnnotation anno) throws JsonParseException, JsonMappingException, IOException {
        IResource resource = getTargetAsResource(anno);
        if (resource != null) {
            if (resource instanceof SpecificResource && ((SpecificResource) resource).getSelector() instanceof FragmentSelector) {
                FragmentSelector selector = (FragmentSelector) ((SpecificResource) resource).getSelector();
                ISelector oaSelector = new de.intranda.api.annotation.oa.FragmentSelector(selector.getFragment());
                IResource oaResource = new de.intranda.api.annotation.oa.SpecificResource(resource.getId(), oaSelector);
                return oaResource;
            }
            return resource;
        }
        return null;

    }

    /**
     * Get the
     *
     * @return a {@link de.intranda.api.annotation.IResource} object.
     * @throws com.fasterxml.jackson.core.JsonParseException if any.
     * @throws com.fasterxml.jackson.databind.JsonMappingException if any.
     * @throws java.io.IOException if any.
     */
    public IResource getBodyAsResource(PersistentAnnotation anno) throws JsonParseException, JsonMappingException, IOException {
        if (anno.getBody() != null) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
            IResource resource = mapper.readValue(anno.getBody(), TextualResource.class);
            return resource;
        }
        return null;
    }

    /**
     * <p>
     * getBodyAsOAResource.
     * </p>
     *
     * @return a {@link de.intranda.api.annotation.IResource} object.
     * @throws com.fasterxml.jackson.core.JsonParseException if any.
     * @throws com.fasterxml.jackson.databind.JsonMappingException if any.
     * @throws java.io.IOException if any.
     */
    public IResource getBodyAsOAResource(PersistentAnnotation anno) throws JsonParseException, JsonMappingException, IOException {
        TextualResource resource = (TextualResource) getBodyAsResource(anno);
        if (resource != null) {
            IResource oaResource = new de.intranda.api.annotation.oa.TextualResource(resource.getText());
            return oaResource;
        }
        return null;
    }

    /**
     * Get the annotation as an {@link de.intranda.api.annotation.wa.WebAnnotation} java object
     *
     * @return a {@link de.intranda.api.annotation.wa.WebAnnotation} object.
     * @throws DAOException
     */
    public WebAnnotation getAsWebAnnotation(PersistentAnnotation anno) {
        URI uri = getWebAnnotationURI(anno.getId());
        WebAnnotation annotation = new WebAnnotation(uri);
        try {
            annotation.setCreated(DateTools.convertLocalDateTimeToDateViaInstant(anno.getDateCreated(), false));
            annotation.setModified(DateTools.convertLocalDateTimeToDateViaInstant(anno.getDateModified(), false));
            try {
                if (anno.getCreator() != null) {
                    annotation.setCreator(new Agent(anno.getCreator().getIdAsURI(), AgentType.PERSON, anno.getCreator().getDisplayName()));
                }
                if (anno.getGenerator() != null) {
                    annotation
                            .setGenerator(new Agent(anno.getGenerator().getIdAsURI(), AgentType.SOFTWARE, anno.getGenerator().getOwner().getTitle()));
                }
            } catch (DAOException e) {
                logger.error("Error getting author of web annotation for " + anno, e);
            }
            annotation.setBody(getBodyAsResource(anno));
            annotation.setTarget(getTargetAsResource(anno));
            annotation.setMotivation(anno.getMotivation());
            annotation.setRights(anno.getAccessCondition());
        } catch (IOException e) {
            logger.error("Error creating web annotation from " + anno, e);
        }
        return annotation;
    }

    /**
     * Get the annotation as an {@link de.intranda.api.annotation.oa.OpenAnnotation} java object
     *
     * @return a {@link de.intranda.api.annotation.oa.OpenAnnotation} object.
     * @throws com.fasterxml.jackson.core.JsonParseException if any.
     * @throws com.fasterxml.jackson.databind.JsonMappingException if any.
     * @throws java.io.IOException if any.
     */
    public OpenAnnotation getAsOpenAnnotation(PersistentAnnotation anno) {
        URI uri = getOpenAnnotationURI(anno.getId());
        OpenAnnotation annotation = new OpenAnnotation(uri);
        try {
            annotation.setBody(getBodyAsOAResource(anno));
            annotation.setTarget(getTargetAsOAResource(anno));
            annotation.setMotivation(anno.getMotivation());
        } catch (IOException e) {
            logger.error("Error creating open annotation from " + anno, e);
        }

        return annotation;
    }

}
