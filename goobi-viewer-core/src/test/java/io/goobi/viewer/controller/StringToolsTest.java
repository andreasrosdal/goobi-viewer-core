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
package io.goobi.viewer.controller;

import static org.junit.Assert.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.managedbeans.utils.BeanUtils;

public class StringToolsTest {

    /**
     * @see StringTools#escapeHtmlChars(String)
     * @verifies escape all characters correctly
     */
    @Test
    public void escapeHtmlChars_shouldEscapeAllCharactersCorrectly() throws Exception {
        Assert.assertEquals("&lt;i&gt;&quot;A&amp;B&quot;&lt;/i&gt;", StringTools.escapeHtmlChars("<i>\"A&B\"</i>"));
    }

    /**
     * @see StringTools#replaceCharacters(String,String[],String[])
     * @verifies replace characters correctly
     */
    @Test
    public void replaceCharacters_shouldReplaceCharactersCorrectly() throws Exception {
        Assert.assertEquals("|-|3110",
                StringTools.replaceCharacters("Hello", new String[] { "H", "e", "l", "o" }, new String[] { "|-|", "3", "1", "0" }));
    }

    /**
     * @see StringTools#removeDiacriticalMarks(String)
     * @verifies remove diacritical marks correctly
     */
    @Test
    public void removeDiacriticalMarks_shouldRemoveDiacriticalMarksCorrectly() throws Exception {
        Assert.assertEquals("aaaaoooouuuueeeeßn", StringTools.removeDiacriticalMarks("äáàâöóòôüúùûëéèêßñ"));
    }

    /**
     * @see StringTools#removeLineBreaks(String,String)
     * @verifies remove line breaks correctly
     */
    @Test
    public void removeLineBreaks_shouldRemoveLineBreaksCorrectly() throws Exception {
        Assert.assertEquals("foobar", StringTools.removeLineBreaks("foo\r\nbar", ""));
    }

    /**
     * @see StringTools#removeLineBreaks(String,String)
     * @verifies remove html line breaks correctly
     */
    @Test
    public void removeLineBreaks_shouldRemoveHtmlLineBreaksCorrectly() throws Exception {
        Assert.assertEquals("foo bar", StringTools.removeLineBreaks("foo<br>bar", " "));
        Assert.assertEquals("foo bar", StringTools.removeLineBreaks("foo<br/>bar", " "));
        Assert.assertEquals("foo bar", StringTools.removeLineBreaks("foo<br />bar", " "));
    }

    /**
     * @see StringTools#stripJS(String)
     * @verifies remove JS blocks correctly
     */
    @Test
    public void stripJS_shouldRemoveJSBlocksCorrectly() throws Exception {
        Assert.assertEquals("foo  bar", StringTools.stripJS("foo <script type=\"javascript\">\nfunction f {\n alert();\n}\n</script> bar"));
        Assert.assertEquals("foo  bar", StringTools.stripJS("foo <SCRIPT>\nfunction f {\n alert();\n}\n</ScRiPt> bar"));
        Assert.assertEquals("foo  bar", StringTools.stripJS("foo <SCRIPT src=\"http://dangerousscript.js\"/> bar"));
    }

    /**
     * @see StringTools#stripPatternBreakingChars(String)
     * @verifies remove chars correctly
     */
    @Test
    public void stripPatternBreakingChars_shouldRemoveCharsCorrectly() throws Exception {
        Assert.assertEquals("foo_bar__", StringTools.stripPatternBreakingChars("foo\tbar\r\n"));
    }

    @Test
    public void testEscapeQuotes() {
        String original = "Das ist ein 'String' mit \"Quotes\".";
        String reference = "Das ist ein \\'String\\' mit \\\"Quotes\\\".";

        String escaped = StringTools.escapeQuotes(original);
        Assert.assertEquals(reference, escaped);

        escaped = StringTools.escapeQuotes(reference);
        Assert.assertEquals(reference, escaped);
    }

    /**
     * @see StringTools#isImageUrl(String)
     * @verifies return true for image urls
     */
    @Test
    public void isImageUrl_shouldReturnTrueForImageUrls() throws Exception {
        Assert.assertTrue(StringTools.isImageUrl("https://example.com/default.jpg"));
        Assert.assertTrue(StringTools.isImageUrl("https://example.com/MASTER.TIFF"));
    }

    /**
     * @see StringTools#renameIncompatibleCSSClasses(String)
     * @verifies rename classes correctly
     */
    @Test
    public void renameIncompatibleCSSClasses_shouldRenameClassesCorrectly() throws Exception {
        Path file = Paths.get("src/test/resources/data/text_example_bad_classes.htm");
        Assert.assertTrue(Files.isRegularFile(file));

        String html = FileTools.getStringFromFile(file.toFile(), StringTools.DEFAULT_ENCODING);
        Assert.assertNotNull(html);
        Assert.assertTrue(html.contains(".20Formatvorlage"));
        Assert.assertTrue(html.contains("class=\"20Formatvorlage"));

        html = StringTools.renameIncompatibleCSSClasses(html);
        Assert.assertFalse(html.contains(".20Formatvorlage"));
        Assert.assertFalse(html.contains("class=\"20Formatvorlage"));
        Assert.assertTrue(html.contains(".Formatvorlage20"));
        Assert.assertTrue(html.contains("class=\"Formatvorlage20"));
    }

    /**
     * @see StringTools#getHierarchyForCollection(String,String)
     * @verifies create list correctly
     */
    @Test
    public void getHierarchyForCollection_shouldCreateListCorrectly() throws Exception {
        List<String> result = StringTools.getHierarchyForCollection("a.b.c.d", ".");
        Assert.assertEquals(4, result.size());
        Assert.assertEquals("a", result.get(0));
        Assert.assertEquals("a.b", result.get(1));
        Assert.assertEquals("a.b.c", result.get(2));
        Assert.assertEquals("a.b.c.d", result.get(3));
    }

    /**
     * @see StringTools#getHierarchyForCollection(String,String)
     * @verifies return single value correctly
     */
    @Test
    public void getHierarchyForCollection_shouldReturnSingleValueCorrectly() throws Exception {
        List<String> result = StringTools.getHierarchyForCollection("a", ".");
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("a", result.get(0));
    }

    /**
     * @see StringTools#normalizeWebAnnotationCoordinates(String)
     * @verifies normalize coordinates correctly
     */
    @Test
    public void normalizeWebAnnotationCoordinates_shouldNormalizeCoordinatesCorrectly() throws Exception {
        Assert.assertEquals("1, 2, 4, 6", StringTools.normalizeWebAnnotationCoordinates("xywh=1, 2, 3, 4"));
    }

    /**
     * @see StringTools#normalizeWebAnnotationCoordinates(String)
     * @verifies preserve legacy coordinates
     */
    @Test
    public void normalizeWebAnnotationCoordinates_shouldPreserveLegacyCoordinates() throws Exception {
        Assert.assertEquals("1, 2, 3, 4", StringTools.normalizeWebAnnotationCoordinates("1, 2, 3, 4"));
    }

    /**
     * @see StringTools#generateHash(String)
     * @verifies hash string correctly
     */
    @Test
    public void generateHash_shouldHashStringCorrectly() throws Exception {
        Assert.assertEquals("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08", StringTools.generateHash("test"));
    }

    /**
     * @see StringTools#checkValueEmptyOrInverted(String)
     * @verifies return true if value null or empty
     */
    @Test
    public void checkValueEmptyOrInverted_shouldReturnTrueIfValueNullOrEmpty() throws Exception {
        Assert.assertTrue(StringTools.checkValueEmptyOrInverted(null));
        Assert.assertTrue(StringTools.checkValueEmptyOrInverted(""));
    }

    /**
     * @see StringTools#checkValueEmptyOrInverted(String)
     * @verifies return true if value starts with 0x1
     */
    @Test
    public void checkValueEmptyOrInverted_shouldReturnTrueIfValueStartsWith0x1() throws Exception {
        Assert.assertTrue(StringTools.checkValueEmptyOrInverted("oof"));
    }

    /**
     * @see StringTools#checkValueEmptyOrInverted(String)
     * @verifies return true if value starts with #1;
     */
    @Test
    public void checkValueEmptyOrInverted_shouldReturnTrueIfValueStartsWith1() throws Exception {
        Assert.assertTrue(StringTools.checkValueEmptyOrInverted("#1;oof"));
    }

    /**
     * @see StringTools#checkValueEmptyOrInverted(String)
     * @verifies return false otherwise
     */
    @Test
    public void checkValueEmptyOrInverted_shouldReturnFalseOtherwise() throws Exception {
        Assert.assertFalse(StringTools.checkValueEmptyOrInverted("foo"));
    }

    /**
     * @see StringTools#filterStringsViaRegex(List,String)
     * @verifies return all matching keys
     */
    @Test
    public void filterStringsViaRegex_shouldReturnAllMatchingKeys() throws Exception {
        String[] keys = new String[] { "foo", "bar", "key0", "key1", "key2" };
        List<String> result = StringTools.filterStringsViaRegex(Arrays.asList(keys), "key[0-9]+");
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("key0", result.get(0));
        Assert.assertEquals("key1", result.get(1));
        Assert.assertEquals("key2", result.get(2));
    }

    /**
     * @see StringTools#isStringUrlEncoded(String,String)
     * @verifies return true if string contains url encoded characters
     */
    @Test
    public void isStringUrlEncoded_shouldReturnTrueIfStringContainsUrlEncodedCharacters() throws Exception {
        Assert.assertTrue(StringTools.isStringUrlEncoded("%28foo%29", StringTools.DEFAULT_ENCODING));
    }

    /**
     * @see StringTools#isStringUrlEncoded(String,String)
     * @verifies return false if string not encoded
     */
    @Test
    public void isStringUrlEncoded_shouldReturnFalseIfStringNotEncoded() throws Exception {
        Assert.assertFalse(StringTools.isStringUrlEncoded("(foo)", StringTools.DEFAULT_ENCODING));
    }

    /**
     * @see StringTools#escapeCriticalUrlChracters(String,boolean)
     * @verifies replace characters correctly
     */
    @Test
    public void escapeCriticalUrlChracters_shouldReplaceCharactersCorrectly() throws Exception {
        Assert.assertEquals("U002BAU002FU005CU007CU003FZ", StringTools.escapeCriticalUrlChracters("+A/\\|?Z", false));
        Assert.assertEquals("U007C", StringTools.escapeCriticalUrlChracters("%7C", true));
    }

    /**
     * @see StringTools#unescapeCriticalUrlChracters(String)
     * @verifies replace characters correctly
     */
    @Test
    public void unescapeCriticalUrlChracters_shouldReplaceCharactersCorrectly() throws Exception {
        Assert.assertEquals("+A/\\|?Z", StringTools.unescapeCriticalUrlChracters("U002BAU002FU005CU007CU003FZ"));
    }
    
}
