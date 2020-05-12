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
package io.goobi.viewer.servlets.oembed;

import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.viewer.MimeType;

/**
 * <p>
 * RichOEmbedResponse class.
 * </p>
 */
public class RichOEmbedResponse extends OEmbedResponse {

    private String html;

    /**
     * Constructor.
     *
     * @param record a {@link io.goobi.viewer.servlets.oembed.OEmbedRecord} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public RichOEmbedResponse(OEmbedRecord record) throws ViewerConfigurationException {
        this.type = "rich";
        this.width = 300;
        this.height = 450;
        generateHtml(record, width);
    }

    /**
     * 
     * @param se
     * @throws ViewerConfigurationException
     */
    private void generateHtml(OEmbedRecord record, int size) throws ViewerConfigurationException {
        if (record == null) {
            throw new IllegalArgumentException("record may not be null");
        }
        if(record.isRichResponse()) {
            StringBuilder sb = new StringBuilder();
            sb.append("<iframe ");
            sb.append("src='");
            sb.append(record.getUri());
            sb.append("'></iframe>");
            html = sb.toString();
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("<div>");
            MimeType mimeType = MimeType.getByName(record.getPhysicalElement().getMimeType());
            if (mimeType != null) {
                switch (mimeType) {
                    case IMAGE:
                        sb.append("<img src=\"").append(record.getPhysicalElement().getImageUrl(size)).append("\"><br />");
                        break;
                    default:
                        break;
                }
            }
            sb.append("<h3>").append(record.getStructElement().getLabel()).append("</h3>");
            
            record.getStructElement().getPi();
            
            sb.append("</div>");
            html = sb.toString();
        }
    }

    /**
     * <p>
     * Getter for the field <code>html</code>.
     * </p>
     *
     * @return the html
     */
    public String getHtml() {
        return html;
    }

    /**
     * <p>
     * Setter for the field <code>html</code>.
     * </p>
     *
     * @param html the html to set
     */
    public void setHtml(String html) {
        this.html = html;
    }
}
