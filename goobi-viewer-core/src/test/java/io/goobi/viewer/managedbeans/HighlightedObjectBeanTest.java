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
package io.goobi.viewer.managedbeans;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Before;
import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;

public class HighlightedObjectBeanTest extends AbstractDatabaseEnabledTest {

    HighlightedObjectBean bean;
    
    @Before
    public void setup() throws Exception {
        super.setUp();
        bean = new HighlightedObjectBean(DataManager.getInstance().getDao());
        bean.init();
    }
    
    @Test
    public void test_listObjecs() throws DAOException {
        
        LocalDateTime now = LocalDate.of(2023, 3, 15).atStartOfDay();
        bean.initProviders(now);
        assertEquals(2, bean.getPastObjectsProvider().getPaginatorList().size());
        assertEquals(0, bean.getFutureObjectsProvider().getPaginatorList().size());
        assertEquals(1, bean.getCurrentObjects().size());
    }
    
    @Test
    public void test_filterList() {
        
        LocalDateTime now = LocalDate.of(2023, 4, 15).atStartOfDay();
        bean.initProviders(now);
        
        bean.getPastObjectsProvider().getFilter("name").setValue("Monat");
        assertEquals(2, bean.getPastObjectsProvider().getSizeOfDataList());
        
        bean.getPastObjectsProvider().getFilter("name").setValue("Januar");
        assertEquals(1, bean.getPastObjectsProvider().getSizeOfDataList());

        bean.getPastObjectsProvider().getFilter("name").setValue("");
        assertEquals(3, bean.getPastObjectsProvider().getSizeOfDataList());
    }

}
