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

import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StructElement;

public class OEmbedRecord {

    private StructElement structElement;
    private PhysicalElement physicalElement;

    /**
     * @return the structElement
     */
    public StructElement getStructElement() {
        return structElement;
    }

    /**
     * @param structElement the structElement to set
     */
    public void setStructElement(StructElement structElement) {
        this.structElement = structElement;
    }

    /**
     * @return the physicalElement
     */
    public PhysicalElement getPhysicalElement() {
        return physicalElement;
    }

    /**
     * @param physicalElement the physicalElement to set
     */
    public void setPhysicalElement(PhysicalElement physicalElement) {
        this.physicalElement = physicalElement;
    }

}
