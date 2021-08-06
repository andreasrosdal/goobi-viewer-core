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
package io.goobi.viewer.dao.update;

import java.sql.SQLException;

import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;

/**
 * extends cms_record_notes table by row 'note_type' which discriminates between the implementing classes.
 * Existing notes are set to note_type = 'SINGLE' 
 * Also adds default values to columns which only exist for one implementing class and sets columntype if neccessary
 * 
 * 
 * @author florian
 * 
 *
 */
public class CMSRecordNoteUpdate implements IModelUpdate {

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.update.IModelUpdate#update(io.goobi.viewer.dao.IDAO)
     */
    @Override
    public boolean update(IDAO dao) throws DAOException, SQLException {
        dao.startTransaction();
        dao.createNativeQuery("UPDATE cms_record_notes SET note_type=\"SINGLE\" WHERE note_type IS NULL").executeUpdate();
        dao.createNativeQuery("ALTER TABLE cms_record_notes MODIFY record_title varchar(4096)").executeUpdate();
        dao.createNativeQuery("ALTER TABLE cms_record_notes ALTER record_title SET DEFAULT ''").executeUpdate();
        dao.createNativeQuery("ALTER TABLE cms_record_notes MODIFY query varchar(4096)").executeUpdate();
        dao.createNativeQuery("ALTER TABLE cms_record_notes ALTER query SET DEFAULT ''").executeUpdate();
        dao.createNativeQuery("ALTER TABLE cms_record_notes ALTER record_pi SET DEFAULT ''").executeUpdate();
        dao.commitTransaction();
        return true;
    }

}
