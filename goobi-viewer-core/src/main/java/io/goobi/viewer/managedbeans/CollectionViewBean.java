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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.ocpsoft.logging.Logger;

import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.CmsElementNotFoundException;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.cms.pages.content.types.CMSCollectionContent;
import io.goobi.viewer.model.search.CollectionResult;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.viewer.collections.CollectionView;
import io.goobi.viewer.model.viewer.collections.CollectionView.BrowseDataProvider;

/**
 * Creates and stored {@link CollectionView}s for a session
 * @author florian
 *
 */
@Named
@SessionScoped
public class CollectionViewBean implements Serializable {

    private static final long serialVersionUID = 6707278968715712945L;

    private static final Logger logger = Logger.getLogger(CollectionView.class);
    
    /**
     * {@link CollectionView}s mapped to contentItem-Ids of {@link CMSCollectionContent} used to create the CollectionView
     */
    private Map<String, CollectionView> collections = new HashMap<>();

    /**
     * Solr statistics (in the form of {@link CollectionResult}) mapped to collection names
     * which are in turn mapped to contentItem-Ids because each contentItem may have different statistics for
     * its collections due to different filter queries and excluded subcollections
     */
    private Map<String, Map<String,CollectionResult>> collectionStatistics = new HashMap<>();
    
    /**
     * Get the {@link io.goobi.viewer.model.viewer.collections.CollectionView} of the given content item in the given page. If the view hasn't been initialized
     * yet, do so and add it to the Bean's CollectionView map
     *
     * @param id The ContentItemId of the ContentItem to look for
     * @param page The page containing the collection ContentItem
     * @return The CollectionView or null if no matching ContentItem was found
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws IllegalRequestException
     */
    public CollectionView getCollection(CMSCollectionContent content, int collectionBaseLevels, boolean openExpanded, boolean displayParents, boolean ignoreHierarchy) throws PresentationException, IndexUnreachableException, IllegalRequestException {
        String myId = getCollectionId(content);
        CollectionView collection = collections.get(myId);
        if (collection == null) {
            try {
                collection = initializeCollection(content, collectionBaseLevels, openExpanded, displayParents, ignoreHierarchy);
                collections.put(myId, collection);
            } catch(CmsElementNotFoundException e) {
                logger.debug("Not matching collection element for id {} on page {}", content.getItemId(), content.getOwningPage().getId());
            }
        }
        return collection;
    }

    public static String getCollectionId(CMSCollectionContent content) {
        return content.getOwningComponent().getOwnerPage().getId() + "_" + content.getItemId();
    }

    public Optional<CollectionView> getCollectionIfStored(CMSCollectionContent content) {
        String myId = getCollectionId(content);
        CollectionView collection = collections.get(myId);
        return Optional.ofNullable(collection);
    }
    
    public boolean removeCollection(CMSCollectionContent content) {
        String myId = getCollectionId(content);
        return removeCollection(myId);
    }

    private boolean removeCollection(String myId) {
        this.collectionStatistics.remove(myId);
        return collections.remove(myId) != null;
    }
    
    
    /**
     * Creates a collection view object from the item's collection related properties
     *
     * @return a {@link io.goobi.viewer.model.viewer.CollectionView} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws IllegalRequestException
     */
    public CollectionView initializeCollection(CMSCollectionContent content, int numBaseLevels, boolean displayParents, boolean openExpanded, boolean ignoreHierarchy) throws PresentationException, IllegalRequestException, IndexUnreachableException {
        if (StringUtils.isBlank(content.getSolrField())) {
            throw new PresentationException("No solr field provided to create collection view");
        }
        CollectionView collection = initializeCollection(content);
        collection.setBaseElementName(content.getCollectionName());
        collection.setBaseLevels(numBaseLevels);
        collection.setDisplayParentCollections(displayParents);
        collection.setIgnore(content.getIgnoreCollectionsAsList());
        collection.setIgnoreHierarchy(ignoreHierarchy);
        collection.setShowAllHierarchyLevels(openExpanded);
        collection.populateCollectionList();
        return collection;
    }
    
    /**
     * Adds a CollecitonView object for the given field to the map and populates its values.
     *
     * @param collectionField
     * @param facetField
     * @param sortField
     * @param filterQuery
     */
    private CollectionView initializeCollection(final CMSCollectionContent content) {
        // Use FACET_* instead of MD_*, otherwise the hierarchy may be broken
        String useCollectionField = SearchHelper.facetifyField(content.getSolrField());
        
        BrowseDataProvider provider = new BrowseDataProvider() {
            @Override
            public Map<String, CollectionResult> getData() throws IndexUnreachableException {
                Map<String, CollectionResult> dcStrings =
                        SearchHelper.findAllCollectionsFromField(useCollectionField, content.getGroupingField(), content.getCombinedFilterQuery(), true, true,
                                DataManager.getInstance().getConfiguration().getCollectionSplittingChar(content.getSolrField()));
                return dcStrings;
            }
        };
        
        CollectionView collection = new CollectionView(content.getSolrField(), provider);
        String subtheme = content.getOwningPage().getSubThemeDiscriminatorValue();
        if (StringUtils.isNotBlank(subtheme)) {
            try {
                Optional<CMSPage> searchPage = DataManager.getInstance()
                        .getDao()
                        .getCMSPagesForSubtheme(subtheme)
                        .stream()
                        .filter(p -> p.isPublished())
                        .filter(p -> p.hasSearchFunctionality())
                        .findFirst();
                searchPage.ifPresent(p -> {
                    collection.setSearchUrl(p.getPageUrl());
                });
            } catch (DAOException e) {
                logger.debug("Error getting subtheme search page: " + e.toString());
            }
        }
        return collection;
    }


    /**
     * Querys solr for a list of all values of the set collectionField which my serve as a collection
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public List<String> getPossibleIgnoreCollectionList(CMSCollectionContent content, boolean ignoreHierarchy) throws IndexUnreachableException {
        if (StringUtils.isBlank(content.getSolrField())) {
            return Collections.singletonList("");
        }
        Map<String, CollectionResult> dcStrings = getColletionMap(content);
        List<String> list = new ArrayList<>(dcStrings.keySet());
        list = list.stream()
                .filter(c -> StringUtils.isBlank(content.getCollectionName()) || c.startsWith(content.getCollectionName() + "."))
                .filter(c -> (ignoreHierarchy) ? true
                        : (StringUtils.isBlank(content.getCollectionName()) ? !c.contains(".") : !c.replace(content.getCollectionName() + ".", "").contains(".")))
                .collect(Collectors.toList());
        Collections.sort(list);
        return list;
    }
    
    
    /**
     * @return
     * @throws IndexUnreachableException
     */
    public Map<String, CollectionResult> getColletionMap(CMSCollectionContent content) throws IndexUnreachableException {
        
        String contentId = getCollectionId(content);
        Map<String, CollectionResult> map = this.collectionStatistics.get(contentId);
        if(map == null) {
            map = SearchHelper.findAllCollectionsFromField(content.getSolrField(), null, content.getCombinedFilterQuery(), true, true,
                    DataManager.getInstance().getConfiguration().getCollectionSplittingChar(content.getSolrField()));
            this.collectionStatistics.put(contentId, map);
        }
        return map;
    }

    public void removeCollectionsForPage(CMSPage page) {
        String idRegex = page.getId() + "_" + "\\w";
        new ArrayList<>(this.collections.keySet()).forEach(id -> {
            if(id.matches(idRegex)) {
                removeCollection(id);
            }
        });
        
    }
    
    public List<CollectionView> getLoadedCollectionsForPage(CMSPage page) {
        String idRegex = page.getId() + "_" + "\\w";
        return new ArrayList<>(this.collections.keySet()).stream()
        .filter(id -> id.matches(idRegex))
        .map(id -> this.collections.get(id))
        .collect(Collectors.toList());
    }
    
    /**
     * get a list of all {@link io.goobi.viewer.model.viewer.collections.CollectionView}s with the given solr field which are already loaded via
     * {@link #getCollection(CMSPage)} or {@link #getCollection(String, CMSPage)
     *
     * @param field The solr field the colleciton is based on
     * @return a {@link java.util.List} object.
     */
    public List<CollectionView> getCollections(String field) {
        return collections.values().stream().filter(collection -> field.equals(collection.getField())).collect(Collectors.toList());
    }

    public void invalidate() {
        this.collections = new HashMap<>();
        this.collectionStatistics = new HashMap<>();
    }
    
}
