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
package io.goobi.viewer.model.citation;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import de.undercouch.citeproc.csl.CSLType;
import io.goobi.viewer.AbstractTest;

public class CitationTest extends AbstractTest {

    /**
     * @see Citation#getCitationString()
     * @verifies return apa citation correctly
     */
    @Test
    public void getCitationString_shouldReturnApaCitationCorrectly() throws Exception {
        Map<String, List<String>> fields = new HashMap<>();
        fields.put(CitationDataProvider.AUTHOR, Collections.singletonList("Zahn, Timothy"));
        fields.put(CitationDataProvider.TITLE, Collections.singletonList("Thrawn"));
        fields.put(CitationDataProvider.ISSUED, Collections.singletonList("2017-04-11"));
        fields.put(CitationDataProvider.ISBN, Collections.singletonList("9780606412148"));

        Citation cit = new Citation("id", "apa", CSLType.BOOK, fields);
        String s = cit.getCitationString();
        Assert.assertNotNull(s);
        Assert.assertTrue(s.contains("Zahn, T. (2017-04-11). <span style=\"font-style: italic\">Thrawn</span>."));
    }

}