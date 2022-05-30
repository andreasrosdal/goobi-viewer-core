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
package io.goobi.viewer.model.translations.admin;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.model.translations.admin.MessageEntry.TranslationStatus;

public class MessageValueTest {

    /**
     * @see MessageValue#getTranslationStatus()
     * @verifies return none status correctly
     */
    @Test
    public void getTranslationStatus_shouldReturnNoneStatusCorrectly() throws Exception {
        Assert.assertEquals(TranslationStatus.NONE, new MessageValue("en", "", "value").getTranslationStatus());
    }

    /**
     * @see MessageValue#getTranslationStatus()
     * @verifies return partial status correctly
     */
    @Test
    public void getTranslationStatus_shouldReturnPartialStatusCorrectly() throws Exception {
        Assert.assertEquals(TranslationStatus.PARTIAL, new MessageValue("en", "value zzz", "value").getTranslationStatus());
    }

    /**
     * @see MessageValue#getTranslationStatus()
     * @verifies return full status correctly
     */
    @Test
    public void getTranslationStatus_shouldReturnFullStatusCorrectly() throws Exception {
        Assert.assertEquals(TranslationStatus.FULL, new MessageValue("en", "value", "value").getTranslationStatus());
    }

    /**
     * @see MessageValue#isDisplayHighlight()
     * @verifies return true if status none of partial
     */
    @Test
    public void isDisplayHighlight_shouldReturnTrueIfStatusNoneOfPartial() throws Exception {
        Assert.assertTrue(new MessageValue("en", "", "value").isDisplayHighlight());
        Assert.assertTrue(new MessageValue("en", "value zzz", "value").isDisplayHighlight());
    }

    /**
     * @see MessageValue#isDisplayHighlight()
     * @verifies return false if status full
     */
    @Test
    public void isDisplayHighlight_shouldReturnFalseIfStatusFull() throws Exception {
        Assert.assertFalse(new MessageValue("en", "value", "value").isDisplayHighlight());
    }
}
