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
package io.goobi.viewer.model.iiif.presentation.builder;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.api.annotation.LinkedAnnotation;
import de.intranda.api.annotation.TextualAnnotation;
import de.intranda.api.annotation.TextualAnnotationBody;
import de.intranda.api.iiif.IIIFUrlResolver;
import de.intranda.api.iiif.image.ImageInformation;
import de.intranda.api.iiif.presentation.AnnotationList;
import de.intranda.api.iiif.presentation.Canvas;
import de.intranda.api.iiif.presentation.ICanvas;
import de.intranda.api.iiif.presentation.Manifest;
import de.intranda.api.iiif.presentation.PartOfCanvas;
import de.intranda.api.iiif.presentation.Sequence;
import de.intranda.api.iiif.presentation.content.ImageContent;
import de.intranda.api.iiif.presentation.content.LinkingContent;
import de.intranda.api.iiif.presentation.enums.AnnotationType;
import de.intranda.api.iiif.presentation.enums.DcType;
import de.intranda.api.iiif.presentation.enums.Format;
import de.intranda.api.iiif.presentation.enums.Motivation;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.ImageDeliveryBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.annotation.Comment;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.pageloader.EagerPageLoader;
import io.goobi.viewer.model.viewer.pageloader.IPageLoader;
import io.goobi.viewer.model.viewer.pageloader.LeanPageLoader;
import io.goobi.viewer.servlets.rest.content.CommentAnnotation;
import io.goobi.viewer.servlets.rest.content.ContentResource;

/**
 * @author Florian Alpers
 *
 */
public class SequenceBuilder extends AbstractBuilder {

    private static final Logger logger = LoggerFactory.getLogger(SequenceBuilder.class);

    protected final ImageDeliveryBean imageDelivery = BeanUtils.getImageDeliveryBean();
    private BuildMode buildMode = BuildMode.IIIF;
    private PageType preferredView = PageType.viewObject;

    /**
     * @param request
     * @throws URISyntaxException
     */
    public SequenceBuilder(HttpServletRequest request) throws URISyntaxException {
        super(request);
    }

    /**
     * @param servletUri
     * @param requestURI
     */
    public SequenceBuilder(URI servletUri, URI requestURI) {
        super(servletUri, requestURI);
    }

    /**
     * Creates a sequence from all pages within the given doc and appends it to manifest
     * 
     * @param manifest The manifest to include the sequence. May be null
     * @param doc
     * @param string
     * @throws URISyntaxException
     * @throws DAOException
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws ViewerConfigurationException
     */
    public Map<AnnotationType, List<AnnotationList>> addBaseSequence(Manifest manifest, StructElement doc, String manifestId)
            throws URISyntaxException, PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {

        Map<AnnotationType, List<AnnotationList>> annotationMap = new HashMap<>();

        Sequence sequence = new Sequence(getSequenceURI(doc.getPi(), null));

        sequence.addWithin(manifest);

        IPageLoader pageLoader = new EagerPageLoader(doc);

        String dataRepository = ContentResource.getDataRepository(doc.getPi());

        Map<Integer, Canvas> canvasMap = new HashMap<>();
        for (int i = pageLoader.getFirstPageOrder(); i <= pageLoader.getLastPageOrder(); ++i) {
            PhysicalElement page = pageLoader.getPage(i);

            Canvas canvas = generateCanvas(doc, page);
            if (canvas != null && getBuildMode().equals(BuildMode.IIIF)) {
                Map<AnnotationType, AnnotationList> content = addOtherContent(doc, page, canvas, dataRepository, false);

                merge(annotationMap, content);
                canvasMap.put(i, canvas);
            }
            sequence.addCanvas(canvas);
        }
        if (getBuildMode().equals(BuildMode.IIIF)) {
            annotationMap.put(AnnotationType.COMMENT, addComments(canvasMap, doc.getPi(), false));
        }

        if (manifest != null && sequence.getCanvases() != null) {
            manifest.setSequence(sequence);
        }

        return annotationMap;
    }

    /**
     * Adds a comment annotation to all cavases which contain comments
     * 
     * @param canvases All canvases which may get comments, mapped by their page order
     * @param pi The pi of the work containing the pages
     * @param populate if true, the actual annotations will be included in the resources property
     * @return a map with the list of all annotationlists (one list per page)
     * @throws DAOException
     * @throws URISyntaxException
     * @throws ViewerConfigurationException
     */
    public List<AnnotationList> addComments(Map<Integer, Canvas> canvases, String pi, boolean populate)
            throws DAOException, URISyntaxException, ViewerConfigurationException {
        List<AnnotationList> list = new ArrayList<>();
        List<Integer> pages = DataManager.getInstance().getDao().getPagesWithComments(pi);
        for (Integer order : pages) {
            Canvas canvas = canvases.get(order);
            if (canvas != null) {
                AnnotationList annoList = new AnnotationList(getAnnotationListURI(pi, order, AnnotationType.COMMENT));
                annoList.setLabel(ViewerResourceBundle.getTranslations(AnnotationType.COMMENT.name()));
                if (populate) {
                    List<Comment> comments = DataManager.getInstance().getDao().getCommentsForPage(pi, order, false);
                    for (Comment comment : comments) {
                        TextualAnnotation anno = new TextualAnnotation(getCommentAnnotationURI(pi, order, comment.getId()));
                        anno.setMotivation(Motivation.COMMENTING);
//                        anno.setOn(canvas);
                        anno.setOn(createSpecificResource(canvas, 0, 0, canvas.getWidth(), canvas.getHeight()));
                        TextualAnnotationBody body = new TextualAnnotationBody();
                        body.setValue(comment.getText());
                        anno.setBody(body);
                        //                        CommentAnnotation anno = new CommentAnnotation(comment, getServletURI().toString(), false);
                        annoList.addResource(anno);
                    }
                }
                canvas.addOtherContent(annoList);
                list.add(annoList);
            }
        }
        return list;
    }

    /**
     * @param canvas
     * @param i
     * @param j
     * @param width
     * @param height
     * @return
     */
    private ICanvas createSpecificResource(Canvas canvas, int x, int y, int width, int height) {
        PartOfCanvas part = new PartOfCanvas();
        part.setCanvas(canvas);
        part.setArea(new Rectangle(x, y, width, height));
        return part;
    }

    /**
     * @param annotationMap
     * @param content
     */
    public void merge(Map<AnnotationType, List<AnnotationList>> annotationMap, Map<AnnotationType, AnnotationList> content) {
        for (AnnotationType type : content.keySet()) {
            List<AnnotationList> list = annotationMap.get(type);
            if (list == null) {
                list = new ArrayList<>();
                annotationMap.put(type, list);
            }
            list.add(content.get(type));
        }
    }

    public PhysicalElement getPage(StructElement doc, int order) throws IndexUnreachableException, DAOException {
        IPageLoader loader = new LeanPageLoader(doc, 1);
        return loader.getPage(order);
    }

    /**
     * @param doc
     * @param page
     * @return
     * @throws URISyntaxException
     * @throws ViewerConfigurationException
     * @throws IndexUnreachableException
     */
    public Canvas generateCanvas(StructElement doc, PhysicalElement page)
            throws URISyntaxException, ViewerConfigurationException, IndexUnreachableException {
        if (doc == null || page == null) {
            return null;
        }
        URI canvasId = getCanvasURI(doc.getPi(), page.getOrder());
        Canvas canvas = new Canvas(canvasId);
        canvas.setLabel(new SimpleMetadataValue(page.getOrderLabel()));
        canvas.setThumbnail(new ImageContent(new URI(imageDelivery.getThumbs().getThumbnailUrl(page))));

        Sequence parent = new Sequence(getSequenceURI(doc.getPi(), null));
        canvas.addWithin(parent);

        LinkingContent viewerPage = new LinkingContent(new URI(getViewUrl(page, getPreferredView())));
        viewerPage.setLabel(new SimpleMetadataValue("goobi viewer"));
        canvas.addRendering(viewerPage);

        if (getBuildMode().equals(BuildMode.IIIF)) {
            Dimension size = getSize(page);
            if (size.getWidth() * size.getHeight() > 0) {
                canvas.setWidth(size.width);
                canvas.setHeight(size.height);
            }

            if (page.getMimeType().toLowerCase().startsWith("image") && StringUtils.isNotBlank(page.getFilepath())) {

                String thumbnailUrl = page.getThumbnailUrl();
                ImageContent resource = new ImageContent(new URI(thumbnailUrl));
                if (size.getWidth() * size.getHeight() > 0) {
                    resource.setWidth(size.width);
                    resource.setHeight(size.height);
                    if (IIIFUrlResolver.isIIIFImageUrl(thumbnailUrl)) {
                        URI imageInfoURI = new URI(IIIFUrlResolver.getIIIFImageBaseUrl(thumbnailUrl));
                        resource.setService(new ImageInformation(imageInfoURI.toString()));
                    }
                } else {
                    try {
                        ImageInformation imageInfo = imageDelivery.getImages().getImageInformation(page);
                        resource.setService(imageInfo);
                    } catch (NoClassDefFoundError | ContentLibException e) {
                        logger.error("Error reading image information from {}: {}", thumbnailUrl, e.toString());
                        resource.setWidth(size.width);
                        resource.setHeight(size.height);

                    }
                }
                resource.setFormat(Format.fromMimeType(page.getDisplayMimeType()));

                LinkedAnnotation imageAnnotation = new LinkedAnnotation(getImageAnnotationURI(page.getPi(), page.getOrder()));
                imageAnnotation.setMotivation(Motivation.PAINTING);
                imageAnnotation.setOn(new Canvas(canvas.getId()));
                imageAnnotation.setResource(resource);
                canvas.addImage(imageAnnotation);
            }

        }
        return canvas;
    }

    public Map<AnnotationType, AnnotationList> addOtherContent(StructElement doc, PhysicalElement page, Canvas canvas, String dataRepository,
            boolean populate) throws URISyntaxException, IndexUnreachableException, ViewerConfigurationException {

        Map<AnnotationType, AnnotationList> annotationMap = new HashMap<>();

        if (StringUtils.isNotBlank(page.getFulltextFileName()) || StringUtils.isNotBlank(page.getAltoFileName())) {
            AnnotationList annoList = new AnnotationList(getAnnotationListURI(page.getPi(), page.getOrder(), AnnotationType.FULLTEXT));
            annoList.setLabel(ViewerResourceBundle.getTranslations(AnnotationType.FULLTEXT.name()));
            LinkedAnnotation fulltextAnnotation = new LinkedAnnotation(getAnnotationURI(page.getPi(), page.getOrder(), AnnotationType.FULLTEXT, 1));
            fulltextAnnotation.setMotivation(Motivation.PAINTING);
            fulltextAnnotation.setOn(canvas);
            annoList.addResource(fulltextAnnotation);
            annotationMap.put(AnnotationType.FULLTEXT, annoList);
            if (populate) {
                LinkingContent fulltextLink = new LinkingContent(ContentResource.getFulltextURI(page.getPi(), page.getFileName("txt")));
                fulltextLink.setFormat(Format.TEXT_PLAIN);
                fulltextLink.setType(DcType.TEXT);
                fulltextLink.setLabel(ViewerResourceBundle.getTranslations("FULLTEXT"));
                fulltextAnnotation.setResource(fulltextLink);
            }
        }

        if (StringUtils.isNotBlank(page.getAltoFileName())) {
            AnnotationList annoList = new AnnotationList(getAnnotationListURI(page.getPi(), page.getOrder(), AnnotationType.ALTO));
            annoList.setLabel(ViewerResourceBundle.getTranslations(AnnotationType.ALTO.name()));
            LinkedAnnotation altoAnnotation = new LinkedAnnotation(getAnnotationURI(page.getPi(), page.getOrder(), AnnotationType.ALTO, 1));
            altoAnnotation.setMotivation(Motivation.PAINTING);
            altoAnnotation.setOn(canvas);
            annoList.addResource(altoAnnotation);
            annotationMap.put(AnnotationType.ALTO, annoList);
            if (populate) {
                LinkingContent altoLink = new LinkingContent(ContentResource.getAltoURI(page.getPi(), page.getFileName("xml")));
                altoLink.setFormat(Format.TEXT_XML);
                altoLink.setType(DcType.TEXT);
                altoLink.setLabel(ViewerResourceBundle.getTranslations("ALTO"));
                altoAnnotation.setResource(altoLink);
            }
        }

        if (PhysicalElement.MIME_TYPE_AUDIO.equals(page.getMimeType())) {
            AnnotationList annoList = new AnnotationList(getAnnotationListURI(page.getPi(), page.getOrder(), AnnotationType.AUDIO));
            annoList.setLabel(ViewerResourceBundle.getTranslations(AnnotationType.AUDIO.name()));
            LinkedAnnotation annotation = new LinkedAnnotation(getAnnotationURI(page.getPi(), page.getOrder(), AnnotationType.AUDIO, 1));
            annotation.setMotivation(Motivation.PAINTING);
            annotation.setOn(canvas);
            annoList.addResource(annotation);
            annotationMap.put(AnnotationType.AUDIO, annoList);
            if (populate) {
                String url = page.getMediaUrl(page.getFileNames().keySet().stream().findFirst().orElse(""));
                Format format = Format.fromFilename(url);
                LinkingContent audioLink = new LinkingContent(new URI(url));
                audioLink.setFormat(format);
                audioLink.setType(DcType.SOUND);
                audioLink.setLabel(ViewerResourceBundle.getTranslations("AUDIO"));
                annotation.setResource(audioLink);
            }

        }

        AnnotationList videoList = new AnnotationList(getAnnotationListURI(page.getPi(), page.getOrder(), AnnotationType.VIDEO));
        videoList.setLabel(ViewerResourceBundle.getTranslations(AnnotationType.VIDEO.name()));
        if (PhysicalElement.MIME_TYPE_VIDEO.equals(page.getMimeType())) {
            LinkedAnnotation annotation = new LinkedAnnotation(getAnnotationURI(page.getPi(), page.getOrder(), AnnotationType.VIDEO, 1));
            annotation.setMotivation(Motivation.PAINTING);
            annotation.setOn(canvas);
            videoList.addResource(annotation);
            if (populate) {
                String url = page.getMediaUrl(page.getFileNames().keySet().stream().findFirst().orElse(""));
                Format format = Format.fromFilename(url);
                LinkingContent link = new LinkingContent(new URI(url));
                link.setFormat(format);
                link.setType(DcType.MOVING_IMAGE);
                link.setLabel(ViewerResourceBundle.getTranslations("VIDEO"));
                annotation.setResource(link);
            }

        }
        if (PhysicalElement.MIME_TYPE_SANDBOXED_HTML.equals(page.getMimeType())) {
            try {
                LinkedAnnotation annotation = new LinkedAnnotation(getAnnotationURI(page.getPi(), page.getOrder(), AnnotationType.VIDEO, 1));
                annotation.setMotivation(Motivation.PAINTING);
                annotation.setOn(canvas);
                videoList.addResource(annotation);
                if (populate) {
                    String url = page.getUrl();
                    if (url.startsWith("//")) {
                        url = "http:" + url;
                    }

                    LinkingContent link = new LinkingContent(new URI(url));
                    link.setFormat(Format.TEXT_HTML);
                    link.setType(DcType.MOVING_IMAGE);
                    link.setLabel(ViewerResourceBundle.getTranslations("VIDEO"));
                    annotation.setResource(link);
                }
            } catch (ViewerConfigurationException e) {
                logger.error(e.toString(), e);
            }
        }
        if (videoList.getResources() != null) {
            annotationMap.put(AnnotationType.VIDEO, videoList);
        }

        if (PhysicalElement.MIME_TYPE_APPLICATION.equals(page.getMimeType()) || PhysicalElement.MIME_TYPE_IMAGE.equals(page.getMimeType())) {
            AnnotationList annoList = new AnnotationList(getAnnotationListURI(page.getPi(), page.getOrder(), AnnotationType.PDF));
            annoList.setLabel(ViewerResourceBundle.getTranslations(AnnotationType.PDF.name()));

            LinkedAnnotation annotation = new LinkedAnnotation(getAnnotationURI(page.getPi(), page.getOrder(), AnnotationType.PDF, 1));
            annotation.setMotivation(Motivation.PAINTING);
            annotation.setOn(canvas);
            annoList.addResource(annotation);
            annotationMap.put(AnnotationType.PDF, annoList);
            if (populate) {
                String url = imageDelivery.getPdf().getPdfUrl(doc, page);
                LinkingContent link = new LinkingContent(new URI(url));
                link.setFormat(Format.APPLICATION_PDF);
                link.setType(DcType.SOFTWARE);
                link.setLabel(ViewerResourceBundle.getTranslations("PDF"));
                annotation.setResource(link);
            }
        }

        //        try {
        //            AnnotationList annoList = new AnnotationList(getAnnotationListURI(page.getPi(), page.getOrder(), AnnotationType.COMMENT));
        //            annoList.setLabel(IMetadataValue.getTranslations(AnnotationType.COMMENT.name()));
        //            List<Comment> comments = DataManager.getInstance().getDao().getCommentsForPage(page.getPi(), page.getOrder(), false);
        //            for (Comment comment : comments) {
        //                CommentAnnotation anno = new CommentAnnotation(comment, getServletURI().toString(), false);
        //                annoList.addResource(anno);
        //            }
        //            if(comments != null && !comments.isEmpty()) {                
        //                annotationMap.put(AnnotationType.COMMENT, annoList);
        //            }
        //        } catch (DAOException e) {
        //            logger.error(e.toString(), e);
        //        }

        for (AnnotationType type : annotationMap.keySet()) {
            canvas.addOtherContent(annotationMap.get(type));
        }
        return annotationMap;
    }

    /**
     * @param page
     * @return
     * @throws ViewerConfigurationException
     */
    private Dimension getSize(PhysicalElement page) throws ViewerConfigurationException {
        Dimension size = new Dimension(0, 0);
        if (page.getMimeType().toLowerCase().startsWith("video") || page.getMimeType().toLowerCase().startsWith("text")) {
            size.setSize(page.getVideoWidth(), page.getVideoHeight());
        } else if (page.getMimeType().toLowerCase().startsWith("image")) {
            if (page.hasIndividualSize()) {
                size.setSize(page.getImageWidth(), page.getImageHeight());
            } else {
                try {
                    ImageInformation info = imageDelivery.getImages().getImageInformation(page);
                    size.setSize(info.getWidth(), info.getHeight());
                    /*
                     * Catch NoClassDefFoundError which occurs if imageio-libs are missing. 
                     * Currently this is true in a testing environment, so we just catch it here to pass the tests
                     */
                } catch (NoClassDefFoundError | ContentLibException | URISyntaxException e) {
                    logger.error("Unable to retrieve image size for {}: {}", page, e.toString());
                }
            }
        }
        return size;
    }

    /**
     * @return the buildMode
     */
    public BuildMode getBuildMode() {
        return buildMode;
    }

    /**
     * @param buildMode the buildMode to set
     */
    public SequenceBuilder setBuildMode(BuildMode buildMode) {
        this.buildMode = buildMode;
        return this;
    }

    /**
     * @return the preferredView
     */
    public PageType getPreferredView() {
        return preferredView;
    }

    /**
     * @param preferredView the preferredView to set
     */
    public SequenceBuilder setPreferredView(PageType preferredView) {
        this.preferredView = preferredView;
        return this;
    }

}