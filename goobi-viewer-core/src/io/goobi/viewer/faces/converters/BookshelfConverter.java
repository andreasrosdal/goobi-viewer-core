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
package io.goobi.viewer.faces.converters;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.bookshelf.Bookshelf;

@FacesConverter("bookshelfConverter")
public class BookshelfConverter implements Converter {

    @Override
    public final Object getAsObject(final FacesContext context, final UIComponent component, final String value) {
        int id = Integer.valueOf(value);
        try {
            return DataManager.getInstance().getDao().getBookshelf(id);
        } catch (DAOException e) {
            return null;
        }
    }

    @Override
    public final String getAsString(final FacesContext context, final UIComponent component, final Object object) {
        if (object == null) {
            return null;
        }

        if (object instanceof Bookshelf) {
            Bookshelf user = (Bookshelf) object;
            try {
                return String.valueOf(user.getId());
            } catch (NumberFormatException nfe) {
                return null;
            }
        }
        throw new IllegalArgumentException("Object '" + object.getClass().getName() + "' is not a Bookshelf.");
    }
}