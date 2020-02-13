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
package io.goobi.viewer.managedbeans;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.managedbeans.SitelinkBean;

public class SitelinkBeanTest extends AbstractDatabaseAndSolrEnabledTest {

    /**
     * @see SitelinkBean#getAvailableValuesForField(String,String)
     * @verifies return all existing values for the given field
     */
    @Test
    public void getAvailableValuesForField_shouldReturnAllExistingValuesForTheGivenField() throws Exception {
        SitelinkBean sb = new SitelinkBean();
        List<String> values = sb.getAvailableValuesForField("MD_YEARPUBLISH", SolrConstants.ISWORK + ":true");
        Assert.assertEquals(63, values.size());
    }
}
