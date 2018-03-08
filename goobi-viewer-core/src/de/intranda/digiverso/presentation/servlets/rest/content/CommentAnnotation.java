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
package de.intranda.digiverso.presentation.servlets.rest.content;

import java.io.IOException;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.intranda.digiverso.presentation.model.annotation.Comment;
import de.intranda.digiverso.presentation.model.security.user.User;
import de.intranda.digiverso.presentation.model.viewer.PageType;
import de.intranda.digiverso.presentation.servlets.utils.ServletUtils;

public class CommentAnnotation {

    private static final String CONTEXT_URI = "http://www.w3.org/ns/anno.jsonld";
    private static final String GENERATOR_URI = "https://www.intranda.com/en/digiverso/goobi-viewer/goobi-viewer-overview/";

    private Comment comment;
    private HttpServletRequest servletRequest;

    public CommentAnnotation(Comment comment, HttpServletRequest servletRequest) {
        this.comment = comment;
        this.servletRequest = servletRequest;
    }

    @JsonSerialize()
    @JsonProperty("@context")
    public String getContext() {
        return CONTEXT_URI;
    }

    @JsonSerialize()
    public String getId() {
        return new StringBuilder(ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest))
                .append(servletRequest.getRequestURI().substring(servletRequest.getContextPath().length()))
                .toString();
    }

    @JsonSerialize()
    public String getGenerator() {
        return GENERATOR_URI;
    }

    @JsonSerialize()
    public String getTarget() {
        return new StringBuilder(ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest)).append('/')
                .append(PageType.viewImage.getName())
                .append('/')
                .append(comment.getPi())
                .append('/')
                .append(comment.getPage())
                .append('/')
                .toString();
    }

    @JsonSerialize(using = BodySerializer.class)
    public Comment getBody() {
        return comment;
    }

    @JsonSerialize(using = UserSerializerObfuscated.class)
    public User getCreator() {
        return comment.getOwner();
    }

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssZ")
    public Date getCreated() {
        return comment.getDateCreated();
    }

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssZ")
    @JsonInclude(Include.NON_NULL)
    public Date getModified() {
        return comment.getDateUpdated();

    }

    public static class BodySerializer extends JsonSerializer<Comment> {

        /* (non-Javadoc)
         * @see com.fasterxml.jackson.databind.JsonSerializer#serialize(java.lang.Object, com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
         */
        @Override
        public void serialize(Comment comment, JsonGenerator generator, SerializerProvider provider) throws IOException, JsonProcessingException {
            generator.writeStartObject();
            generator.writeStringField("format", "test/plain");
            generator.writeStringField("type", "TextualBody");
            generator.writeStringField("value", comment.getText());
            generator.writeEndObject();
        }
    }

    public static class UserSerializerObfuscated extends JsonSerializer<User> {

        /* (non-Javadoc)
         * @see com.fasterxml.jackson.databind.JsonSerializer#serialize(java.lang.Object, com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
         */
        @Override
        public void serialize(User user, JsonGenerator generator, SerializerProvider provider) throws IOException, JsonProcessingException {
            //            generator.writeStartObject();
            generator.writeString(user.getDisplayNameObfuscated());
        }
    }
}
