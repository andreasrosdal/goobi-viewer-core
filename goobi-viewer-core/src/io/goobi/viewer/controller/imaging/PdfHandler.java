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
package io.goobi.viewer.controller.imaging;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StructElement;

/**
 * @author Florian Alpers
 *
 */
public class PdfHandler {

    private final WatermarkHandler watermarkHandler;
    private final String iiifUrl;

    public PdfHandler(WatermarkHandler watermarkHandler, Configuration configuration) throws ViewerConfigurationException {
        this.watermarkHandler = watermarkHandler;
        this.iiifUrl = configuration.getRestApiUrl();
    }

    /**
     * Return the pdf-download url for the given {@link StructElement} and {@link PhysicalElement}
     * 
     * @param doc
     * @param page
     * @return
     */
    public String getPdfUrl(StructElement doc, PhysicalElement page) {
        return getPdfUrl(doc, new PhysicalElement[] { page });
    }

    /**
     * Return the pdf-download url for the given {@link StructElement} and a number of {@link PhysicalElement}s
     * 
     * @param doc
     * @param pages
     * @return
     */
    public String getPdfUrl(StructElement doc, PhysicalElement[] pages) {

        final UrlParameterSeparator paramSep = new UrlParameterSeparator();
        StringBuilder sb = new StringBuilder(this.iiifUrl);
        sb.append("image")
                .append("/")
                .append(pages[0].getPi())
                .append("/")
                .append(Arrays.stream(pages).map(page -> page.getFileName()).collect(Collectors.joining("$")))
                .append("/")
                .append("full/max/0/")
                .append(pages[0].getPi())
                .append("_")
                .append(pages[0].getOrder());
        if (pages.length > 1) {
            sb.append("-").append(pages[pages.length - 1].getOrder());
        }
        sb.append(".pdf");

        String dataRepository = pages[0].getDataRepository();
        Path mediaRepository = Paths.get(DataManager.getInstance().getConfiguration().getViewerHome());
        if (StringUtils.isNotEmpty(dataRepository)) {
            mediaRepository = Paths.get(DataManager.getInstance().getConfiguration().getDataRepositoriesHome(), dataRepository);
        }
        
        Path indexedMetsPath = mediaRepository.resolve(DataManager.getInstance().getConfiguration().getIndexedMetsFolder()).resolve(pages[0].getPi() + ".xml");;
        if (Files.exists(indexedMetsPath)) {
            sb.append(paramSep.getChar()).append("metsFile=").append(indexedMetsPath.toUri());
        
            if (doc != null && StringUtils.isNotBlank(doc.getLogid())) {
                sb.append(paramSep.getChar()).append("divID=").append(doc.getLogid());
            }
        } else {
            //If there is no metsFile, prevent the contentServer from generating a title page by giving an invalid divID which it cannot find
            sb.append(paramSep.getChar()).append("divID=").append("NOTFOUND");
        }

        if (this.watermarkHandler != null) {
            this.watermarkHandler.getWatermarkTextIfExists(pages[0])
                    .ifPresent(text -> sb.append(paramSep.getChar()).append("watermarkText=").append(encode(text)));
            this.watermarkHandler.getFooterIdIfExists(doc)
                    .ifPresent(footerId -> sb.append(paramSep.getChar()).append("watermarkId=").append(footerId));
        }

        return sb.toString();
    }

    /**
     * Returns an existing pdf file from the media folder
     * 
     * @param pi
     * @param filename
     * @return
     */
    public String getPdfUrl(String pi, String filename) {

        final UrlParameterSeparator paramSep = new UrlParameterSeparator();

        StringBuilder sb = new StringBuilder(this.iiifUrl);
        sb.append("image").append("/").append(pi).append("/").append(filename).append("/").append("full/max/0/").append(filename);

        return sb.toString();
    }

    /**
     * Gets the url to the pdf for the given {@link StructElement}. The pi is the one of the topStruct element of the given StructElement
     * 
     * @param divId DivID (LogID) of the docstruct for which the pdf should be generated. If this is null or empty, a pdf for the complete work is
     *            generated
     * @param label The name for the output file (.pdf-extension excluded). If this is null or empty, the label will be generated from pi and divId
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public String getPdfUrl(StructElement doc, String label) throws PresentationException, IndexUnreachableException {
        String pi = doc.getTopStruct().getPi();
    	return getPdfUrl(doc, pi, label);
    }
    
    /**
     * Gets the url to the pdf for the given pi and divId
     * 
     * @param pi PI of the process from which to build pdf. Must be provided
     * @param divId DivID (LogID) of the docstruct for which the pdf should be generated. If this is null or empty, a pdf for the complete work is
     *            generated
     * @param label The name for the output file (.pdf-extension excluded). If this is null or empty, the label will be generated from pi and divId
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public String getPdfUrl(StructElement doc, String pi, String label) throws PresentationException, IndexUnreachableException {

        String divId = doc.isWork() ? null : doc.getLogid();

        return getPdfUrl(pi, Optional.ofNullable(divId), this.watermarkHandler.getFooterIdIfExists(doc),
                this.watermarkHandler.getWatermarkTextIfExists(doc), Optional.ofNullable(label));
    }

    /**
     * Returns the url to a PDF build from the mets file for the given {@code pi}
     * 
     * @param pi
     * @param divId
     * @param watermarkId
     * @param watermarkText
     * @param label
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public String getPdfUrl(String pi, Optional<String> divId, Optional<String> watermarkId, Optional<String> watermarkText, Optional<String> label) {

        final UrlParameterSeparator paramSep = new UrlParameterSeparator();

        divId = divId.filter(id -> StringUtils.isNotBlank(id));
        String filename = label.filter(l -> StringUtils.isNotBlank(l))
                .map(l -> l.replaceAll("[\\s]", "_"))
                .map(l -> l.replaceAll("[\\W]", ""))
                .map(l -> l.toLowerCase().endsWith(".pdf") ? l : (l + ".pdf"))
                .orElse(pi + divId.map(id -> "_" + id).orElse("") + ".pdf");

        StringBuilder sb = new StringBuilder(this.iiifUrl);
        sb.append("pdf/mets/").append(pi).append(".xml").append("/");
        divId.ifPresent(id -> sb.append(id).append("/"));
        sb.append(filename);

        watermarkText.ifPresent(text -> sb.append(paramSep.getChar()).append("watermarkText=").append(encode(text)));
        watermarkId.ifPresent(footerId -> sb.append(paramSep.getChar()).append("watermarkId=").append(footerId));

        return sb.toString();
    }

    /**
     * @param text
     * @return
     */
    private static Object encode(String text) {
        try {
            return URLEncoder.encode(text, "utf-8");
        } catch (UnsupportedEncodingException e) {
            return text;
        }
    }

}