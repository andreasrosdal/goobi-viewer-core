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
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import io.goobi.viewer.controller.DataManager;
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
import io.goobi.viewer.model.translations.TranslatedText;
import io.goobi.viewer.model.viewer.StructElement;

@Named
@SessionScoped
public class HighlightedObjectBean implements Serializable {

    private static final int NUM_ITEMS_PER_PAGE = 12;
    private static final long serialVersionUID = -6647395682752991930L;
    private static final Logger logger = LogManager.getLogger(HighlightedObjectBean.class);
    
    private TableDataProvider<HighlightedObject> dataProvider;
    private HighlightedObject selectedObject = null;
    private MetadataElement metadataElement = null;

    @Inject
    private NavigationHelper navigationHelper;
    @Inject
    private transient IDAO dao;
    @Inject
    private ImageDeliveryBean imaging;
    
    @PostConstruct
    public void init() {
        if (dataProvider == null) {
            initDataProvider();
        }
    }

    /**
     * @return the dataProvider
     */
    public TableDataProvider<HighlightedObject> getDataProvider() {
        return dataProvider;
    }
    
    private void initDataProvider() {
        dataProvider = new TableDataProvider<>(new TableDataSource<HighlightedObject>() {

            private Optional<Long> numItems = Optional.empty();

            @Override
            public List<HighlightedObject> getEntries(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
                try {
                    if (StringUtils.isBlank(sortField)) {
                        sortField = "id";
                    }

                    return DataManager.getInstance()
                            .getDao()
                            .getHighlightedObjects(first, pageSize, sortField, sortOrder.asBoolean(), filters)
                            .stream().map(HighlightedObject::new).collect(Collectors.toList());
                } catch (DAOException e) {
                    logger.error("Could not initialize lazy model: {}", e.getMessage());
                }

                return Collections.emptyList();
            }

            @Override
            public long getTotalNumberOfRecords(Map<String, String> filters) {
                if (!numItems.isPresent()) {
                    try {
                        numItems = Optional.of( DataManager.getInstance()
                                .getDao()
                                .getHighlightedObjects(0, Integer.MAX_VALUE, null, false, filters)
                                .stream().count());
                    } catch (DAOException e) {
                        logger.error("Unable to retrieve total number of cms pages", e);
                    }
                }
                return numItems.orElse(0L);
            }

            @Override
            public void resetTotalNumberOfRecords() {
                numItems = Optional.empty();
            }
        });
        dataProvider.setEntriesPerPage(NUM_ITEMS_PER_PAGE);
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
    }
    public void setSelectedObjectId(long id) {

        try {
            HighlightedObjectData data = dao.getHighlightedObject(id);
            if(data != null) {
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
        if(object != null && object.getData().getId() != null) {
            saved = dao.updateHighlightedObject(object.getData());
        } else if(object != null) {
            saved = dao.addHighlightedObject(object.getData());
        }
        if(saved) {
            Messages.info("Successfully saved object " + object);
        } else {            
            Messages.error("Failed to save object " + object);
        }
    }
    
    public MetadataElement getMetadataElement() {
        if (this.metadataElement == null && this.selectedObject != null) {
            try {
                this.metadataElement = loadMetadataElement(this.selectedObject.getData().getRecordIdentifier(), 0);
            } catch (PresentationException | IndexUnreachableException e) {
                logger.error("Unable to reetrive metadata elemement for {}. Reason: {}", getSelectedObject().getData().getName().getTextOrDefault(), e.getMessage());
                Messages.error(null, "Unable to reetrive metadata elemement for {}. Reason: {}", getSelectedObject().getData().getName().getTextOrDefault(), e.getMessage());
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
    private MetadataElement loadMetadataElement(String recordPi, int index) throws PresentationException, IndexUnreachableException {
        if (StringUtils.isBlank(recordPi)) {
            return null;
        }

        SolrDocument solrDoc = DataManager.getInstance().getSearchIndex().getDocumentByPI(recordPi);
        if (solrDoc == null) {
            return null;
        }
        StructElement structElement = new StructElement(solrDoc);
        return new MetadataElement().init(structElement, index, BeanUtils.getLocale()).setSelectedRecordLanguage(this.selectedObject.getSelectedLocale().getLanguage());
    }

    /**
     * @param note2
     * @param metadataElement2
     */
    private TranslatedText createRecordTitle(SolrDocument solrDoc) {
        IMetadataValue label = TocMaker.buildTocElementLabel(solrDoc);
        return createRecordTitle(label);
    }

    /**
     * @param label
     * @return
     */
    public TranslatedText createRecordTitle(IMetadataValue label) {
        if (label instanceof MultiLanguageMetadataValue) {
            MultiLanguageMetadataValue mLabel = (MultiLanguageMetadataValue) label;
            return new TranslatedText(mLabel);
        }

        return new TranslatedText(((SimpleMetadataValue) label).getValue().orElse(""));
    }
    
    public HighlightedObject getCurrentHighlightedObject() throws DAOException {
        List<HighlightedObject> currentObjects = dao.getHighlightedObjectsForDate(LocalDateTime.now())
                .stream().map(HighlightedObject::new).collect(Collectors.toList());
        if(!currentObjects.isEmpty()) {            
            int randomIndex = new Random().nextInt(currentObjects.size());
            return currentObjects.get(randomIndex);
        } else {
            return null;
        }
    }
    
    public URI getRecordRepresentativeURI() throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        return getRecordRepresentativeURI(DataManager.getInstance().getConfiguration().getThumbnailsWidth(), DataManager.getInstance().getConfiguration().getThumbnailsHeight());
    }
    
    public URI getRecordRepresentativeURI(int width, int height) throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        if(getSelectedObject() != null && StringUtils.isNotBlank(getSelectedObject().getData().getRecordIdentifier())) {            
            return Optional.ofNullable(imaging.getThumbs().getThumbnailUrl(getSelectedObject().getData().getRecordIdentifier(), width, height)).map(URI::create).orElse(null);
        } else {
            return null;
        }
    }
    
}
