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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.api.iiif.IIIFUrlResolver;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageType.Colortype;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.RegionRequest;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Rotation;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale;
import de.unigoettingen.sub.commons.util.PathConverter;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;

/**
 * <p>IIIFUrlHandler class.</p>
 *
 * @author Florian Alpers
 */
public class IIIFUrlHandler {

    /**
     * 
     */
    private static final String UTF_8 = "UTF-8";
    private static final Logger logger = LoggerFactory.getLogger(IIIFUrlHandler.class);

    
    public String getIIIFImageUrl(String fileUrl, String docStructIdentifier, String region, String size, String rotation, String quality, String format) {
        return getIIIFImageUrl(DataManager.getInstance().getConfiguration().getIIIFApiUrl(), fileUrl, docStructIdentifier, region, size, rotation, quality, format);
    }
    
    /**
     * Returns a link to the actual image of the given page, delivered via IIIF api using the given parameters
     *
     * @param region a {@link java.lang.String} object.
     * @param size a {@link java.lang.String} object.
     * @param rotation a {@link java.lang.String} object.
     * @param fileUrl a {@link java.lang.String} object.
     * @param docStructIdentifier a {@link java.lang.String} object.
     * @param quality a {@link java.lang.String} object.
     * @param format a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getIIIFImageUrl(String apiUrl, String fileUrl, String docStructIdentifier, String region, String size, String rotation, String quality,
            String format) {

        try {
            if (PathConverter.isInternalUrl(fileUrl) || ImageHandler.isRestrictedUrl(fileUrl)) {
                try {
                    URI uri = PathConverter.toURI(fileUrl);
                    if (StringUtils.isBlank(uri.getScheme())) {
                        uri = new URI("file", fileUrl, null);
                    }
                    fileUrl = uri.toString();
                } catch (URISyntaxException e) {
                    logger.warn("file url {} is not a valid url: {}", fileUrl, e.getMessage());
                }
                StringBuilder sb = new StringBuilder(apiUrl);
                sb.append("image/-/").append(BeanUtils.escapeCriticalUrlChracters(fileUrl, false));
                return IIIFUrlResolver.getIIIFImageUrl(sb.toString(), region, size, rotation, quality, format);
            } else if (ImageHandler.isExternalUrl(fileUrl)) {
                if (IIIFUrlResolver.isIIIFImageUrl(fileUrl)) {
                    return IIIFUrlResolver.getModifiedIIIFFUrl(fileUrl, region, size, rotation, quality, format);
                } else if (ImageHandler.isImageUrl(fileUrl, false)) {
                    StringBuilder sb = new StringBuilder(apiUrl);
                    sb.append("image/-/").append(BeanUtils.escapeCriticalUrlChracters(fileUrl, true)).append("/");
                    sb.append(region).append("/");
                    sb.append(size).append("/");
                    sb.append(rotation).append("/");
                    sb.append("default.").append(format);
//                  thumbCompression.ifPresent(compr -> sb.append("?compression=").append(thumbCompression));
                    return sb.toString();
                } else {
                    //assume its a iiif id
                    if (fileUrl.endsWith("info.json")) {
                        fileUrl = fileUrl.substring(0, fileUrl.length() - 9);
                    }
                    StringBuilder sb = new StringBuilder(fileUrl);
                    sb.append(region).append("/");
                    sb.append(size).append("/");
                    sb.append(rotation).append("/");
                    sb.append("default.").append(format);
                    return sb.toString();
                }
            } else {
                
                //if the fileUrl contains a "/", then the part before that is the actual docStructIdentifier
                int separatorIndex = fileUrl.indexOf("/");
                if(separatorIndex > 0) {
                    docStructIdentifier = fileUrl.substring(0, separatorIndex);
                    fileUrl = fileUrl.substring(separatorIndex+1);
                }
                
                StringBuilder sb = new StringBuilder(apiUrl);
                sb.append("image/").append(URLEncoder.encode(docStructIdentifier, UTF_8)).append("/").append(URLEncoder.encode(fileUrl, UTF_8)).append("/");
                sb.append(region).append("/");
                sb.append(size).append("/");
                sb.append(rotation).append("/");
                sb.append("default.").append(format);
//                thumbCompression.ifPresent(compr -> sb.append("?compression=").append(thumbCompression));
                return sb.toString();
            }
        } catch (URISyntaxException e) {
            logger.error("Not a valid url: " + fileUrl,e.getMessage());
            return "";
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage());
            return "";
        }
    }



    /**
     * Appends image request parameter paths to the given baseUrl
     *
     * @param baseUrl a {@link java.lang.String} object.
     * @param region a {@link de.unigoettingen.sub.commons.contentlib.imagelib.transform.RegionRequest} object.
     * @param size a {@link de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale} object.
     * @param rotation a {@link de.unigoettingen.sub.commons.contentlib.imagelib.transform.Rotation} object.
     * @param quality a {@link de.unigoettingen.sub.commons.contentlib.imagelib.ImageType.Colortype} object.
     * @param format a {@link de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat} object.
     * @return a {@link java.lang.String} object.
     */
    public String getIIIFImageUrl(String baseUrl, RegionRequest region, Scale size, Rotation rotation, Colortype quality, ImageFileFormat format) {
        if (StringUtils.isNotBlank(baseUrl) && !baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        StringBuilder url = new StringBuilder(baseUrl);
        url.append(region).append("/");
        url.append(size).append("/");
        url.append(Math.round(rotation.getRotation())).append("/");
        url.append(quality.getLabel()).append(".");
        url.append(format.getFileExtension()).append("/");

        return url.toString();
    }


    /**
     * Replaces the image request parameters in an IIIF URL with the given ones
     *
     * @param url a {@link java.lang.String} object.
     * @should replace dimensions correctly
     * @should do nothing if not iiif url
     * @param region a {@link de.unigoettingen.sub.commons.contentlib.imagelib.transform.RegionRequest} object.
     * @param size a {@link de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale} object.
     * @param rotation a {@link de.unigoettingen.sub.commons.contentlib.imagelib.transform.Rotation} object.
     * @param quality a {@link de.unigoettingen.sub.commons.contentlib.imagelib.ImageType.Colortype} object.
     * @param format a {@link de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat} object.
     * @return a {@link java.lang.String} object.
     */
    public String getModifiedIIIFFUrl(String url, RegionRequest region, Scale size, Rotation rotation, Colortype quality, ImageFileFormat format) {
        return IIIFUrlResolver.getModifiedIIIFFUrl(url, region == null ? null : region.toString(), size == null ? null : size.toString(),
                rotation == null ? null : rotation.toString(), quality == null ? null : quality.toString(),
                format == null ? null : format.getFileExtension());
    }


}
