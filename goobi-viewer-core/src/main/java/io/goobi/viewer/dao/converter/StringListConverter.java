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
package io.goobi.viewer.dao.converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.apache.commons.lang3.StringUtils;

/**
 * Store simple strings in single database field
 *
 * @author florian
 *
 */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    /* (non-Javadoc)
     * @see javax.persistence.AttributeConverter#convertToDatabaseColumn(java.lang.Object)
     */
    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if(attribute == null || attribute.isEmpty()) {
            return null;
        } else {
            return attribute.stream().map(s -> s.replace(",", "&comma;")).collect(Collectors.joining(","));
        }
    }

    /* (non-Javadoc)
     * @see javax.persistence.AttributeConverter#convertToEntityAttribute(java.lang.Object)
     */
    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if(StringUtils.isBlank(dbData)) {
            return new ArrayList<>();
        } else {
            return Arrays.asList(dbData.split(",")).stream().map(s -> s.replace("&comma;", ",")).collect(Collectors.toList());
        }
    }

}
