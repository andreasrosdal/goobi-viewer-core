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
package io.goobi.viewer.controller;

import static org.junit.Assert.assertEquals;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.intranda.digiverso.ocr.alto.model.structureclasses.lineelements.Word;
import de.intranda.digiverso.ocr.alto.utils.AltoCoords;
import io.goobi.viewer.AbstractTest;

public class ALTOToolsTest extends AbstractTest {

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        AbstractTest.setUpClass();
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testRotate() {
        Rectangle rect = new Rectangle(589, 502, 948 - 589, 654 - 502);
        Dimension canvasSize = new Dimension(1792, 2747);
        Rectangle expectedRotatedRect_270 = new Rectangle(502, 844, 654 - 502, 1203 - 844);
        rect = ALTOTools.rotate(rect, 270, canvasSize);
        assertEquals(expectedRotatedRect_270, rect);

    }

    @Test
    public void testGetWordCoords() throws IOException {
        File testFile = new File("src/test/resources/data/sample_alto.xml");
        int rotation = 0;

        String altoString = FileUtils.readFileToString(testFile, StringTools.DEFAULT_ENCODING);
        List<String> coords = ALTOTools.getWordCoords(altoString, StringTools.DEFAULT_ENCODING, Collections.singleton("hinauf"), rotation);
        Assert.assertFalse(coords.isEmpty());
        Assert.assertEquals("1133,2549,1263,2584", coords.get(0));

        Set<String> terms = new LinkedHashSet<>();
        terms.add("Santa");
        terms.add("Monica.");
        terms.add("puh");
        coords = ALTOTools.getWordCoords(altoString, StringTools.DEFAULT_ENCODING, terms, rotation);
        Assert.assertEquals(2, coords.size());
        Assert.assertEquals("1032,2248,1136,2280", coords.get(0));

        terms = new LinkedHashSet<>();
        terms.add("Santa Monica.");
        coords = ALTOTools.getWordCoords(altoString, StringTools.DEFAULT_ENCODING, terms, rotation);
        Assert.assertTrue(coords.size() == 2);
        Assert.assertEquals("1032,2248,1136,2280", coords.get(0));

        terms = new LinkedHashSet<>();
        terms.add("Santa Monica. puh");
        coords = ALTOTools.getWordCoords(altoString, StringTools.DEFAULT_ENCODING, terms, rotation);
        Assert.assertTrue(coords.size() == 0);
    }

    /**
     * @see ALTOTools#getWordCoords(String,Set,int,int)
     * @verifies match hyphenated words
     */
    @Test
    public void getWordCoords_shouldMatchHyphenatedWords() throws Exception {
        File file = new File("src/test/resources/data/viewer/alto/0230L.xml");
        Assert.assertTrue(file.isFile());
        String altoString = FileTools.getStringFromFile(file, StringTools.DEFAULT_ENCODING);
        List<String> result = ALTOTools.getWordCoords(altoString, StringTools.DEFAULT_ENCODING, Collections.singleton("wappen"), 0);
        Assert.assertFalse(result.isEmpty());
    }

    /**
     * @see ALTOTools#getWordCoords(String,Set,int)
     * @verifies match phrases
     */
    @Test
    public void getWordCoords_shouldMatchPhrases() throws Exception {
        File file = new File("src/test/resources/data/sample_alto.xml");
        Assert.assertTrue(file.isFile());
        String altoString = FileTools.getStringFromFile(file, StringTools.DEFAULT_ENCODING);
        {
            List<String> result =
                    ALTOTools.getWordCoords(altoString, StringTools.DEFAULT_ENCODING, Collections.singleton("ein neues doppelpyramidenrätsel"), 0);
            Assert.assertEquals(3, result.size());
            Assert.assertEquals("843,478,904,509", result.get(0));
            Assert.assertEquals("923,489,1021,509", result.get(1));
            Assert.assertEquals("1039,477,1482,516", result.get(2));
        }
        {
            List<String> result =
                    ALTOTools.getWordCoords(altoString, StringTools.DEFAULT_ENCODING,
                            Collections.singletonMap("ein neues dreifachpyramidenrätsel", null).keySet(), 0);
            Assert.assertEquals(0, result.size());
        }
    }

    /**
     * @see ALTOTools#getWordCoords(String,Set,int)
     * @verifies match diacritics via base letter
     */
    @Test
    public void getWordCoords_shouldMatchDiacriticsViaBaseLetter() throws Exception {
        File file = new File("src/test/resources/data/sample_alto.xml");
        Assert.assertTrue(file.isFile());
        String altoString = FileTools.getStringFromFile(file, StringTools.DEFAULT_ENCODING);

        List<String> result = ALTOTools.getWordCoords(altoString, StringTools.DEFAULT_ENCODING, Collections.singleton("doppelpyramidenratsel"), 0);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("1039,477,1482,516", result.get(0));
    }

    /**
     * @see ALTOTools#alto2Txt(String)
     * @verifies use extract fulltext correctly
     */
    @Test
    public void alto2Txt_shouldUseExtractFulltextCorrectly() throws Exception {
        File file = new File("src/test/resources/data/viewer/alto/LIWZ_1877_01_05_001.xml");
        Assert.assertTrue(file.isFile());
        String alto = FileTools.getStringFromFile(file, StringTools.DEFAULT_ENCODING);
        Assert.assertNotNull(alto);
        String text = ALTOTools.alto2Txt(alto, StringTools.DEFAULT_ENCODING, false, null);
        Assert.assertNotNull(text);
        Assert.assertTrue(text.length() > 100);
    }

    /**
     * @see ALTOTools#alto2Txt(String,boolean,HttpServletRequest)
     * @verifies concatenate word at line break correctly
     */
    @Test
    public void alto2Txt_shouldConcatenateWordAtLineBreakCorrectly() throws Exception {
        File file = new File("src/test/resources/data/viewer/alto/0230L.xml");
        Assert.assertTrue(file.isFile());
        String alto = FileTools.getStringFromFile(file, StringTools.DEFAULT_ENCODING);
        Assert.assertNotNull(alto);
        String text = ALTOTools.alto2Txt(alto, StringTools.DEFAULT_ENCODING, true, null);
        Assert.assertNotNull(text);
        Assert.assertTrue(text.contains("Wappen"));
    }

    /**
     * @see ALTOTools#getFullText(String,HttpServletRequest)
     * @verifies extract fulltext correctly
     */
    @Test
    public void getFullText_shouldExtractFulltextCorrectly() throws Exception {
        File file = new File("src/test/resources/data/viewer/data/1/alto/00000010.xml");
        Assert.assertTrue(file.isFile());
        String text = ALTOTools.getFulltext(file.toPath(), StringTools.DEFAULT_ENCODING);
        Assert.assertNotNull(text);
        Assert.assertTrue(text.length() > 100);
    }

    @Test
    public void getMatchALTOWord_findFuzzyTerms() {
        String[] searchTerms = new String[] {"Steigbügel~1", "Halter~1"};
        {
            Word word = new Word("Steigbugle", new AltoCoords(10, 10, 12, 12));
            assertEquals(1, ALTOTools.getMatchALTOWord(word, searchTerms));
        }
        {
            Word word = new Word("Ich war beim Steigbügel Häälter", new AltoCoords(10, 10, 12, 12));
            assertEquals(2, ALTOTools.getMatchALTOWord(word, searchTerms));
        }
    }
}
