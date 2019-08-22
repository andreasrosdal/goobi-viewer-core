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
package io.goobi.viewer.model.annotation;

import java.awt.Rectangle;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.intranda.api.annotation.AbstractAnnotation;
import de.intranda.api.annotation.IAnnotation;
import de.intranda.api.annotation.IResource;
import de.intranda.api.annotation.oa.Motivation;
import de.intranda.api.annotation.oa.OpenAnnotation;
import de.intranda.api.annotation.oa.SpecificResource;
import de.intranda.api.annotation.oa.TextualResource;
import de.intranda.api.annotation.wa.FragmentSelector;

import de.intranda.api.iiif.presentation.AnnotationList;
import de.intranda.api.iiif.presentation.Canvas;
import de.intranda.digiverso.ocr.alto.model.structureclasses.Page;
import de.intranda.digiverso.ocr.alto.model.superclasses.GeometricData;

/**
 * Creates an {@link AnnotationList} of {@link TextualAnnotation}s from the content of an ALTO document. Depending on selected granularity,
 * it is either one annotation per page, per TextBlock, per line or per word 
 * 
 * @author Florian
 *
 */
public class AltoAnnotationBuilder {

    
    public List<IAnnotation> createAnnotations(Page alto, Canvas canvas, Granularity granularity, String baseUrl) {
        
        List<GeometricData> elementsToInclude = new ArrayList<>();
        switch(granularity) {
            case PAGE:
                elementsToInclude.add(alto);
                break;
            case PAGEAREA:
                elementsToInclude.addAll(alto.getChildren());
                break;
            case BLOCK:
                elementsToInclude.addAll(alto.getAllTextBlocksAsList());
                break;
            case LINE:
                elementsToInclude.addAll(alto.getAllLinesAsList());
                break;
            case WORD:
                elementsToInclude.addAll(alto.getAllWordsAsList());
                break;
        }
        
        List<IAnnotation> annoList = elementsToInclude.stream().map(element -> createAnnotation(element, canvas, baseUrl)).collect(Collectors.toList());
        return annoList;
    }
    
    private IAnnotation createAnnotation(GeometricData element, Canvas canvas,String baseUrl) {
        AbstractAnnotation anno = new OpenAnnotation(createAnnotationId(baseUrl, element.getId()));
        anno.setMotivation(Motivation.PAINTING);
        anno.setTarget(createSpecificResource(canvas, element.getBounds()));
        TextualResource body = new TextualResource(element.getContent());
        anno.setBody(body);
        return anno;
    }
    
    /**
     * @param canvas
     * @param i
     * @param j
     * @param width
     * @param height
     * @return
     */
        private IResource createSpecificResource(Canvas canvas, Rectangle area) {
            SpecificResource part = new SpecificResource(canvas.getId(), new FragmentSelector(area));
            return part;
        }

    /**
     * @param listId
     * @param id
     * @return
     */
    private URI createAnnotationId(String baseUrl, String id) {
        if(baseUrl.endsWith("/")) {
            return URI.create(baseUrl + id);
        } else {
            return URI.create(baseUrl + "/" + id);
        }
    }

    public static enum Granularity {
        PAGE,
        PAGEAREA,
        BLOCK,
        LINE,
        WORD
    }
}
