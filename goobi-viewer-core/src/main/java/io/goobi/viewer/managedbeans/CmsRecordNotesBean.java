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
package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.tabledata.TableDataFilter;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider;
import io.goobi.viewer.managedbeans.tabledata.TableDataSource;
import io.goobi.viewer.managedbeans.tabledata.TableDataSourceException;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider.SortOrder;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.CMSMediaItem;
import io.goobi.viewer.model.cms.CMSPage;
import io.goobi.viewer.model.cms.CMSPageTemplate;
import io.goobi.viewer.model.cms.CMSRecordNote;
import io.goobi.viewer.model.cms.CategorizableTranslatedSelectable;
import io.goobi.viewer.model.cms.PageValidityStatus;
import io.goobi.viewer.model.cms.Selectable;

/**
 * 
 * Bean used for listing and retrieving {@link CMSRecordNote}s
 * 
 * @author florian
 *
 */
@Named
@SessionScoped
public class CmsRecordNotesBean implements Serializable{

    private static final long serialVersionUID = 1436349423447175132L;

    private static final Logger logger = LoggerFactory.getLogger(CmsRecordNotesBean.class);

    private static final int DEFAULT_ROWS_PER_PAGE = 10;

    public static final String PI_TITLE_FILTER = "PI_OR_TITLE";
    
    @Inject
    private ImageDeliveryBean images;
    
    private TableDataProvider<CMSRecordNote> dataProvider;
    
    @PostConstruct
    public void init() {
        if (dataProvider == null) {
            initDataProvider();
        }
    }
    
    /**
     * @return the dataProvider
     */
    public TableDataProvider<CMSRecordNote> getDataProvider() {
        return dataProvider;
    }
    
    /**
     * get the thumbnail url for the record related to the note
     * 
     * @param note
     * @return
     * @throws ViewerConfigurationException 
     * @throws PresentationException 
     * @throws IndexUnreachableException 
     */
    public String getThumbnailUrl(CMSRecordNote note) throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        if(StringUtils.isNotBlank(note.getRecordPi())) {
            return images.getThumbs().getThumbnailUrl(note.getRecordPi());
        } else {
            return "";
        }
    }
    
    public boolean deleteNote(CMSRecordNote note) throws DAOException {
        if(note != null && note.getId() != null) {
            return DataManager.getInstance().getDao().deleteRecordNote(note);
        } else {
            return false;
        }
    }
    
    private void initDataProvider() {
        dataProvider = new TableDataProvider<>(new TableDataSource<CMSRecordNote>() {

                private Optional<Long> numCreatedPages = Optional.empty();

                @Override
                public List<CMSRecordNote> getEntries(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
                    try {
                        if (StringUtils.isBlank(sortField)) {
                            sortField = "id";
                        }

                        List<CMSRecordNote> notes = DataManager.getInstance()
                                .getDao()
                                .getRecordNotes(first, pageSize, sortField, sortOrder.asBoolean(), filters);
                        return notes;
                    } catch (DAOException e) {
                        logger.error("Could not initialize lazy model: {}", e.getMessage());
                    }

                    return Collections.emptyList();
                }

                @Override
                public long getTotalNumberOfRecords(Map<String, String> filters) {
                    if (!numCreatedPages.isPresent()) {
                        try {
                            List<CMSRecordNote> notes = DataManager.getInstance()
                                    .getDao()
                                    .getRecordNotes(0, Integer.MAX_VALUE, null, false, filters);
                            numCreatedPages = Optional.of((long)notes.size());
                        } catch (DAOException e) {
                            logger.error("Unable to retrieve total number of cms pages", e);
                        }
                    }
                    return numCreatedPages.orElse(0L);
                }


                @Override
                public void resetTotalNumberOfRecords() {
                    numCreatedPages = Optional.empty();
                }
            });
            dataProvider.setEntriesPerPage(DEFAULT_ROWS_PER_PAGE);
            dataProvider.addFilter(PI_TITLE_FILTER);
            //            lazyModelPages.addFilter("CMSCategory", "name");
    }
    

}
