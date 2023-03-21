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
package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import io.goobi.viewer.controller.DAOSearchFunction;
import io.goobi.viewer.controller.DAOSearchFunction;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.PrettyUrlTools;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider.SortOrder;
import io.goobi.viewer.managedbeans.tabledata.TableDataSource;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.cms.HighlightedObject;
import io.goobi.viewer.model.cms.HighlightedObjectData;
import io.goobi.viewer.model.metadata.MetadataElement;
import io.goobi.viewer.model.toc.TocMaker;
import io.goobi.viewer.model.translations.IPolyglott;
import io.goobi.viewer.model.translations.TranslatedText;
import io.goobi.viewer.model.viewer.StructElement;

@Named
@SessionScoped
public class HighlightedObjectBean implements Serializable {

    private static final long serialVersionUID = -6647395682752991930L;
    private static final Logger logger = LogManager.getLogger(HighlightedObjectBean.class);
    private static final int NUM_ITEMS_PER_PAGE = 2;
    private static final String ALL_OBJECTS_SORT_FIELD = "dateStart";
    private static final SortOrder ALL_OBJECTS_SORT_ORDER = SortOrder.DESCENDING;
    private static final String CURRENT_OBJECTS_SORT_FIELD = "dateStart";
    private static final SortOrder CURRENT_OBJECTS_SORT_ORDER = SortOrder.ASCENDING;

    private TableDataProvider<HighlightedObject> allObjectsProvider;
    private TableDataProvider<HighlightedObject> currentObjectsProvider;

    private transient HighlightedObject selectedObject = null;
    private MetadataElement metadataElement = null;
    private final Random random = new Random();

    @Inject
    private NavigationHelper navigationHelper;
    @Inject
    private transient IDAO dao;
    @Inject
    private ImageDeliveryBean imaging;

    public HighlightedObjectBean() {
        
    }
    
    public HighlightedObjectBean(IDAO dao) {
        this.dao = dao;
    }
    
    @PostConstruct
    public void init() {
        LocalDateTime now = LocalDateTime.now();
        if (allObjectsProvider == null || currentObjectsProvider == null) {
            initProviders(now);
        }
    }

    void initProviders(LocalDateTime now) {
        allObjectsProvider = TableDataProvider.initDataProvider(NUM_ITEMS_PER_PAGE, ALL_OBJECTS_SORT_FIELD, ALL_OBJECTS_SORT_ORDER,
                (first, pageSize, sortField, descending, filters) -> dao
                        .getHighlightedObjects(first, pageSize, sortField, descending, filters)
                        .stream()
                        .map(HighlightedObject::new)
                        .collect(Collectors.toList()));
        currentObjectsProvider = TableDataProvider.initDataProvider(Integer.MAX_VALUE, CURRENT_OBJECTS_SORT_FIELD, CURRENT_OBJECTS_SORT_ORDER,
                (first, pageSize, sortField, descending, filters) -> dao
                .getHighlightedObjectsForDate(now)
                .stream()
                .map(HighlightedObject::new)
                .collect(Collectors.toList()));
    }

    public TableDataProvider<HighlightedObject> getAllObjectsProvider() {
        return allObjectsProvider;
    }
    
    public TableDataProvider<HighlightedObject> getCurrentObjectsProvider() {
        return currentObjectsProvider;
    }

    public String getRecordUrl(HighlightedObject object) {
        if (object != null) {
            return navigationHelper.getImageUrl() + "/" + object.getData().getRecordIdentifier() + "/";
        }
        return "";
    }

    public void deleteObject(HighlightedObject object) {
        try {
            dao.deleteHighlightedObject(object.getData().getId());
            Messages.info("cms___highlighted_objects__delete__success");
        } catch (DAOException e) {
            logger.error("Error deleting object", e);
            Messages.error("cms___highlighted_objects__delete__error");
        }
    }

    public HighlightedObject getSelectedObject() {
        return selectedObject;
    }

    public void setSelectedObject(HighlightedObject selectedObject) {
        this.selectedObject = selectedObject;
        this.metadataElement = null;
        if (this.selectedObject != null) {
            this.selectedObject.setSelectedLocale(BeanUtils.getDefaultLocale());
        }
    }

    public void setSelectedObjectId(long id) {

        try {
            HighlightedObjectData data = dao.getHighlightedObject(id);
            if (data != null) {
                setSelectedObject(new HighlightedObject(data));
            } else {
                setSelectedObject(null);
            }
        } catch (DAOException e) {
            logger.error("Error setting highlighted object", e);
            setSelectedObject(null);
        }
    }

    public void setNewSelectedObject() {
        HighlightedObjectData data = new HighlightedObjectData();
        setSelectedObject(new HighlightedObject(data));
    }

    public boolean isNewObject() {
        return this.selectedObject != null && this.selectedObject.getData().getId() != null;
    }

    public void saveObject(HighlightedObject object) throws DAOException {
        boolean saved = false;
        boolean redirect = false;
        if (object != null && object.getData().getId() != null) {
            saved = dao.updateHighlightedObject(object.getData());
        } else if (object != null) {
            saved = dao.addHighlightedObject(object.getData());
            redirect = true;
        }
        if (saved) {
            Messages.info("Successfully saved object " + object);
        } else {
            Messages.error("Failed to save object " + object);
        }
        if (redirect) {
            PrettyUrlTools.redirectToUrl(PrettyUrlTools.getAbsolutePageUrl("adminCmsHighlightedObjectsEdit", object.getData().getId()));
        }
    }

    public MetadataElement getMetadataElement() {
        if (this.metadataElement == null && this.selectedObject != null) {
            try {
                SolrDocument solrDoc = loadSolrDocument(this.selectedObject.getData().getRecordIdentifier());
                if (solrDoc != null) {
                    this.metadataElement = loadMetadataElement(solrDoc, 0);
                    if (this.selectedObject.getData().getName().isEmpty()) {
                        this.selectedObject.getData().setName(createRecordTitle(solrDoc));
                    }
                }
            } catch (PresentationException | IndexUnreachableException e) {
                logger.error("Unable to reetrive metadata elemement for {}. Reason: {}", getSelectedObject().getData().getName().getTextOrDefault(),
                        e.getMessage());
                Messages.error(null, "Unable to reetrive metadata elemement for {}. Reason: {}",
                        getSelectedObject().getData().getName().getTextOrDefault(), e.getMessage());
            }
        }
        return this.metadataElement;
    }

    /**
     * @param recordPi
     * @param index Metadata view index
     * @return
     * @throws DAOException
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    private MetadataElement loadMetadataElement(SolrDocument solrDoc, int index) throws PresentationException, IndexUnreachableException {
        StructElement structElement = new StructElement(solrDoc);
        return new MetadataElement().init(structElement, index, BeanUtils.getLocale())
                .setSelectedRecordLanguage(this.selectedObject.getSelectedLocale().getLanguage());

    }

    SolrDocument loadSolrDocument(String recordPi) throws IndexUnreachableException, PresentationException {
        if (StringUtils.isBlank(recordPi)) {
            return null;
        }

        SolrDocument solrDoc = DataManager.getInstance().getSearchIndex().getDocumentByPI(recordPi);
        if (solrDoc == null) {
            return null;
        }
        return solrDoc;
    }

    /**
     * @param note2
     * @param metadataElement2
     */
    private TranslatedText createRecordTitle(SolrDocument solrDoc) {
        IMetadataValue label = TocMaker.buildTocElementLabel(solrDoc);
        TranslatedText text = createRecordTitle(label);
        text.setSelectedLocale(IPolyglott.getDefaultLocale());
        return text;
    }

    /**
     * @param label
     * @return
     */
    private TranslatedText createRecordTitle(IMetadataValue label) {
        if (label instanceof MultiLanguageMetadataValue) {
            MultiLanguageMetadataValue mLabel = (MultiLanguageMetadataValue) label;
            return new TranslatedText(mLabel);
        } else {
            TranslatedText title = new TranslatedText();
            title.setValue(label.getValue().orElse(""), IPolyglott.getDefaultLocale());
            return title;
        }
    }

    public HighlightedObject getCurrentHighlightedObject() throws DAOException {
        List<HighlightedObject> currentObjects = dao.getHighlightedObjectsForDate(LocalDateTime.now())
                .stream()
                .filter(HighlightedObjectData::isEnabled)
                .map(HighlightedObject::new)
                .collect(Collectors.toList());
        if (!currentObjects.isEmpty()) {
            int randomIndex = random.nextInt(currentObjects.size());
            return currentObjects.get(randomIndex);
        } else {
            return null;
        }
    }

    public URI getRecordRepresentativeURI() throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        return getRecordRepresentativeURI(DataManager.getInstance().getConfiguration().getThumbnailsWidth(),
                DataManager.getInstance().getConfiguration().getThumbnailsHeight());
    }

    public URI getRecordRepresentativeURI(int width, int height)
            throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        if (getSelectedObject() != null && StringUtils.isNotBlank(getSelectedObject().getData().getRecordIdentifier())) {
            return Optional.ofNullable(imaging.getThumbs().getThumbnailUrl(getSelectedObject().getData().getRecordIdentifier(), width, height))
                    .map(URI::create)
                    .orElse(null);
        } else {
            return null;
        }
    }

    public List<HighlightedObject> getCurrentObjects() throws DAOException {
        return this.dao.getHighlightedObjectsForDate(LocalDateTime.now()).stream().map(HighlightedObject::new).collect(Collectors.toList());
    }

}
