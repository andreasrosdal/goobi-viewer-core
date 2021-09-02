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
package io.goobi.viewer.model.annotation;

import static io.goobi.viewer.api.rest.v1.ApiUrls.*;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.intranda.api.annotation.AgentType;
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
import de.intranda.api.iiif.presentation.v2.Canvas2;
import de.intranda.api.iiif.presentation.v2.Manifest2;
import de.intranda.api.iiif.presentation.v3.Canvas3;
import de.intranda.api.iiif.presentation.v3.Manifest3;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.security.user.User;

/**
 * @author florian
 *
 */
public class AnnotationConverter {

    private static final Logger logger = LoggerFactory.getLogger(AnnotationConverter.class);
    private final AbstractApiUrlManager urls;
    
    public AnnotationConverter() {
        this(DataManager.getInstance().getRestApiManager().getDataApiManager().orElseThrow(() -> new IllegalStateException("No api manager available")));
    }

    public AnnotationConverter(AbstractApiUrlManager urls) {
        this.urls = urls;
    }
    
    /**
     * Used for backwards compatibility
     */
    public OpenAnnotation getAsOpenAnnotation(Comment comment) {
        OpenAnnotation anno = new OpenAnnotation(getOpenAnnotationCommentURI(comment.getPi(), comment.getPage(), comment.getId()));
        anno.setMotivation(Motivation.COMMENTING);
        if (comment.getPage() != null) {
            anno.setTarget(
                    new Canvas2(URI.create(urls.path(RECORDS_RECORD, RECORDS_PAGES_CANVAS).params(comment.getPi(), comment.getPage()).build())));
        } else {
            anno.setTarget(new Manifest2(URI.create(urls.path(RECORDS_RECORD, RECORDS_MANIFEST).params(comment.getPi()).build())));
        }
        TextualResource body = new TextualResource(comment.getText());
        anno.setBody(body);
        return anno;
    }

    /**
     * Used for backwards compatibility
     */
    public WebAnnotation getAsWebAnnotation(Comment comment) {
        WebAnnotation anno = new WebAnnotation(getWebAnnotationCommentURI(comment.getId()));
        anno.setMotivation(de.intranda.api.annotation.wa.Motivation.COMMENTING);
        if (comment.getPage() != null) {
            anno.setTarget(
                    new Canvas3(URI.create(urls.path(RECORDS_PAGES, RECORDS_PAGES_CANVAS).params(comment.getPi(), comment.getPage()).build())));
        } else {
            anno.setTarget(new Manifest3(URI.create(urls.path(RECORDS_RECORD, RECORDS_MANIFEST).params(comment.getPi()).build())));
        }
        IResource body = new de.intranda.api.annotation.wa.TextualResource(comment.getText());
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
            annotation.setCreated(anno.getDateCreated());
            annotation.setModified(anno.getDateModified());
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
    
    public PersistentAnnotation getAsPersistentAnnotation(WebAnnotation anno) {
        
        return new PersistentAnnotation(anno, getPersistenceId(anno), getPI(anno.getTarget()).orElse(null), getPageNo(anno.getTarget()).orElse(null));
    
    }
    
    public Comment getAsComment(WebAnnotation anno) {
        String pi = getPI(anno.getTarget()).orElse(null);
        Integer page = getPageNo(anno.getTarget()).orElse(null);
        String text = anno.getBody().toString();
        Long annoId = getPersistenceId(anno);
        
        User user = Optional.ofNullable(anno.getCreator())
        .flatMap(this::getUserId)
        .map(id -> {
            try {
                return DataManager.getInstance().getDao().getUser(id);
            } catch (DAOException e) {
                return null;
            }
        })
        .orElse(null);
        
        Comment comment = new Comment(pi, page, user, text, null);
        comment.setId(annoId);
        return comment;
    }

    /**
     * Used for backwards compatibility
     */
    public PersistentAnnotation getAsPersistentAnnotation(Comment comment) {
        WebAnnotation anno = getAsWebAnnotation(comment);
        return getAsPersistentAnnotation(anno);
    }

    /**
     * @param creator
     * @return
     */
    private Optional<Long> getUserId(Agent creator) {
        String id = urls.parseParameter(urls.path(USERS, USERS_USERID).build(), creator.getId().toString(), "{userId}");
        if(StringUtils.isNotBlank(id) && id.matches("\\d")) {
            return Optional.of(Long.parseLong(id));
        } else {
            return Optional.empty();
        }
    }
    
    /**
     * @param target
     * @return
     */
    private Optional<String> getPI(IResource target) {
        if(target.getId() != null) {
            String uri = target.getId().toString();
            
            String pi = urls.parseParameter(urls.path(RECORDS_PAGES,  RECORDS_PAGES_CANVAS).build(), uri, "{pi}");
            if(StringUtils.isNotBlank(pi)) {
                return Optional.of(pi);
            }
            
            pi = urls.parseParameter(urls.path(RECORDS_RECORD,  RECORDS_MANIFEST).build(), uri, "{pi}");
            if(StringUtils.isNotBlank(pi)) {
                return Optional.of(pi);
            }
            
            pi = urls.parseParameter(urls.path(RECORDS_SECTIONS,  RECORDS_SECTIONS_RANGE).build(), uri, "{pi}");
            if(StringUtils.isNotBlank(pi)) {
                return Optional.of(pi);
            }
            
        }
        return Optional.empty();
    }
    
    private Optional<String> getDivId(IResource target) {
        if(target.getId() != null) {
            String uri = target.getId().toString();
            
            String id = urls.parseParameter(urls.path(RECORDS_SECTIONS,  RECORDS_SECTIONS_RANGE).build(), uri, "{divId}");
            if(StringUtils.isNotBlank(id)) {
                return Optional.of(id);
            }
            
        }
        return Optional.empty();
    }

    /**
     * @param target
     * @return
     */
    private Optional<Integer> getPageNo(IResource target) {
        if(target.getId() != null) {
            String uri = target.getId().toString();
            String pageNo = urls.parseParameter(urls.path(RECORDS_PAGES,  RECORDS_PAGES_CANVAS).build(), uri, "{pageNo}");
            if(StringUtils.isNotBlank(pageNo) && pageNo.matches("\\d+")) {
                return Optional.of(Integer.parseInt(pageNo));
            }
        }
        return Optional.empty();
    }

    /**
     * @param anno
     * @return
     */
    private Long getPersistenceId(WebAnnotation anno) {
        Long id = null;
        if (anno.getId() != null) {
            String uri = anno.getId().toString();
            String idString = urls.parseParameter(urls.path(ANNOTATIONS, ANNOTATIONS_ANNOTATION).build(), uri, "{id}");
            if (StringUtils.isNotBlank(idString)) {
                id = Long.parseLong(idString);
            } else {
                idString = urls.parseParameter(urls.path(ANNOTATIONS, ANNOTATIONS_COMMENT).build(), uri, "{id}");
                if (StringUtils.isNotBlank(idString)) {
                    id = Long.parseLong(idString);
                }
            }
        }
        if(id != null) {            
            return id;
        } else {
            throw new IllegalArgumentException("Cannot parse annotation-id from " + anno.getId());
        }
    }
    
}
