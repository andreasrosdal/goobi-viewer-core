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
package io.goobi.viewer.model.translations.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.model.translations.admin.MessageEntry.TranslationStatus;

public class MessageEntryTest extends AbstractTest {

    /**
     * @see MessageEntry#create(String,List)
     * @verifies create MessageEntry correctly
     */
    @Test
    public void create_shouldCreateMessageEntryCorrectly() throws Exception {
        MessageEntry entry = MessageEntry.create(null, "foo", Arrays.asList(new Locale[] { Locale.ENGLISH, Locale.GERMAN }));
        Assert.assertNotNull(entry);
        Assert.assertEquals("foo", entry.getKey());
        Assert.assertEquals(2, entry.getValues().size());
        Assert.assertEquals("en", entry.getValues().get(0).getLanguage());
        Assert.assertEquals("de", entry.getValues().get(1).getLanguage());
    }

    /**
     * @see MessageEntry#compareTo(MessageEntry)
     * @verifies compare correctly
     */
    @Test
    public void compareTo_shouldCompareCorrectly() throws Exception {
        MessageEntry entry1 = new MessageEntry("one", Collections.emptyList());
        MessageEntry entry2 = new MessageEntry("two", Collections.emptyList());
        Assert.assertTrue(entry1.compareTo(entry2) < 0);
        Assert.assertTrue(entry1.compareTo(entry1) == 0);
        Assert.assertTrue(entry2.compareTo(entry1) > 0);
    }

    /**
     * @see MessageEntry#getTranslationStatus()
     * @verifies return none status correctly
     */
    @Test
    public void getTranslationStatus_shouldReturnNoneStatusCorrectly() throws Exception {
        List<MessageValue> values = new ArrayList<>(2);
        values.add(new MessageValue("en", null, "value"));
        values.add(new MessageValue("de", null, "value"));
        MessageEntry entry = new MessageEntry("key", values);
        Assert.assertEquals(2, entry.getValues().size());
        Assert.assertEquals(TranslationStatus.NONE, entry.getTranslationStatus());
    }

    /**
     * @see MessageEntry#getTranslationStatus()
     * @verifies return partial status correctly
     */
    @Test
    public void getTranslationStatus_shouldReturnPartialStatusCorrectly() throws Exception {
        {
            List<MessageValue> values = new ArrayList<>(2);
            values.add(new MessageValue("en", "value", "value"));
            values.add(new MessageValue("de", null, "wert"));
            MessageEntry entry = new MessageEntry("key", values);
            Assert.assertEquals(2, entry.getValues().size());
            Assert.assertEquals(TranslationStatus.PARTIAL, entry.getTranslationStatus());
        }
        {
            List<MessageValue> values = new ArrayList<>(2);
            values.add(new MessageValue("en", "value zzz", "value"));
            values.add(new MessageValue("de", "wert", "wert"));
            MessageEntry entry = new MessageEntry("key", values);
            Assert.assertEquals(2, entry.getValues().size());
            Assert.assertEquals(TranslationStatus.PARTIAL, entry.getTranslationStatus());
        }
    }

    /**
     * @see MessageEntry#getTranslationStatus()
     * @verifies return full status correctly
     */
    @Test
    public void getTranslationStatus_shouldReturnFullStatusCorrectly() throws Exception {
        List<MessageValue> values = new ArrayList<>(2);
        values.add(new MessageValue("en", "value", "value"));
        values.add(new MessageValue("de", "wert", "wert"));
        MessageEntry entry = new MessageEntry("key", values);
        Assert.assertEquals(2, entry.getValues().size());
        Assert.assertEquals(TranslationStatus.FULL, entry.getTranslationStatus());
    }

    /**
     * @see MessageEntry#getTranslationStatusForLanguage(String)
     * @verifies return correct status for language
     */
    @Test
    public void getTranslationStatusForLanguage_shouldRetutrnCorrectStatusForLanguage() throws Exception {
        List<MessageValue> values = new ArrayList<>(2);
        values.add(new MessageValue("en", "value", "value"));
        values.add(new MessageValue("de", "wert zzz", "wert"));
        values.add(new MessageValue("fr", "", ""));
        MessageEntry entry = new MessageEntry("key", values);
        Assert.assertEquals(3, entry.getValues().size());
        Assert.assertEquals(TranslationStatus.FULL, entry.getTranslationStatusForLanguage("en"));
        Assert.assertEquals(TranslationStatus.PARTIAL, entry.getTranslationStatusForLanguage("de"));
        Assert.assertEquals(TranslationStatus.NONE, entry.getTranslationStatusForLanguage("fr"));
    }
}