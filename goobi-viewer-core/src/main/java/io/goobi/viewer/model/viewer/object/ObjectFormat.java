/*
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.model.viewer.object;

import org.apache.commons.io.FilenameUtils;

/**
 * <p>
 * ObjectFormat class.
 * </p>
 *
 * @author Florian Alpers
 */
public enum ObjectFormat {
    PLY,
    OBJ,
    STL,
    TDS,
    GLTF;

    /**
     * <p>
     * getByFileExtension.
     * </p>
     *
     * @param filename a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.viewer.object.ObjectFormat} object.
     */
    public static ObjectFormat getByFileExtension(String filename) {
        switch (FilenameUtils.getExtension(filename.toLowerCase())) {
            case "ply":
                return PLY;
            case "obj":
                return OBJ;
            case "stl":
                return STL;
            case "3ds":
                return TDS;
            case "gltf":
            case "glb":
                return GLTF;
            default:
                return null;
        }
    }
}
