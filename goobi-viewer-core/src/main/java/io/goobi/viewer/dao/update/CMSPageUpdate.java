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
package io.goobi.viewer.dao.update;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.CMSTemplateManager;
import io.goobi.viewer.model.cms.media.CMSMediaItem;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.cms.pages.content.CMSComponent;
import io.goobi.viewer.model.cms.pages.content.CMSContent;
import io.goobi.viewer.model.cms.pages.content.PersistentCMSComponent;
import io.goobi.viewer.model.translations.IPolyglott;
import io.goobi.viewer.model.translations.TranslatedText;

public class CMSPageUpdate implements IModelUpdate {

    private static final Logger logger = LogManager.getLogger(CMSPageUpdate.class);
    CMSContentConverter contentConverter;
    CMSTemplateManager templateManager;

    @Override
    public boolean update(IDAO dao) throws DAOException, SQLException {

        if (!dao.tableExists("cms_content_items")){
            return false;
        }
        
        List<Map<String, Object>> languageVersions = getTableData(dao, "cms_page_language_versions");
        List<Map<String, Object>> contentItems = getTableData(dao, "cms_content_items");

        if(languageVersions.isEmpty() || contentItems.isEmpty()) {
            return false;
        }
        
        List<Map<String, Object>> pages = getTableData(dao, "cms_pages");
        
        
        contentConverter = new CMSContentConverter(dao);
        templateManager = CMSTemplateManager.getInstance();

        /*Map page ids to a map of all owned languageVersions mapped to language*/
        Map<Long, Map<String, Map<String, Object>>> languageVersionMap = languageVersions.stream()
                .collect(Collectors.toMap(map -> (Long) map.get("owner_page_id"), map -> Map.of((String) map.get("language"), map),
                        (map1, map2) -> combineMaps(map1, map2)));

        /*Map language version ids to a list of all owned contentItems*/
        Map<Long, List<Map<String, Object>>> contentItemMap = contentItems.stream()
                .collect(Collectors.toMap(map -> (Long) map.get("owner_page_language_version_id"), map -> List.of(map),
                        (map1, map2) -> ListUtils.union(map1, map2)));

        List<CMSPage> updatedPages = new ArrayList<>();
        for (Map<String, Object> pageValues : pages) {
            Long pageId = (Long) pageValues.get("cms_page_id");
            CMSPage page = dao.getCMSPage(pageId);
            try {
                Map<String, Map<String, Object>> pageLanguageVersions = languageVersionMap.get(pageId);
                Map<String, List<Map<String, Object>>> pageContentItemsMap = getContentItemsForPage(pageLanguageVersions, contentItemMap);

                TranslatedText title = getTranslatedText(pageLanguageVersions, "title");
                TranslatedText menuTitle = getTranslatedText(pageLanguageVersions, "menu_title");
                Boolean published = (Boolean) pageValues.get("published");
                Long topbarSliderId = getTopbarSliderId(contentItemMap, pageLanguageVersions);
                String legacyPageTemplateId = (String) pageValues.get("template_id");

                
                Map<String, Map<String, Object>> previewTexts = getContentItemsOfItemId(pageLanguageVersions, contentItemMap, "preview01");
                Map<String, String> previewValues = previewTexts.entrySet()
                        .stream()
                        .filter(e -> StringUtils.isNotBlank((String) e.getValue().get("html_fragment")))
                        .collect(Collectors.toMap(e -> e.getKey(), e -> (String) e.getValue().get("html_fragment")));
                TranslatedText previewText = new TranslatedText(new MultiLanguageMetadataValue(previewValues), IPolyglott.getDefaultLocale());

                Map<String, Object> previewImageItem = getContentItemsOfItemId(pageLanguageVersions, contentItemMap, "image01").get("global");
                CMSMediaItem previewImage = null;
                if (previewImageItem != null) {
                    Long previewImageId = (Long) previewImageItem.get("media_item_id");
                    if (previewImageId != null) {
                        previewImage = dao.getCMSMediaItem(previewImageId);
                    }
                }
                
                if(title.isEmpty()) {
                    title.setText(legacyPageTemplateId, IPolyglott.getDefaultLocale());
                }
                page.setTitle(title);
                page.setMenuTitle(menuTitle);
                page.setTopbarSliderId(topbarSliderId);
                page.setPreviewText(previewText);
                page.setPreviewImage(previewImage);
                page.setPublished(published);

                Map<String, CMSContent> contentMap = createContentObjects(pageContentItemsMap, dao);

                CMSComponent componentTemplate = templateManager.getLegacyComponent(legacyPageTemplateId);
                if (componentTemplate != null) {
                    PersistentCMSComponent component = new PersistentCMSComponent(componentTemplate, contentMap.values());
                    component.setOwnerPage(page);
                    page.setPersistentComponents(Collections.singletonList(component));
                } else {
                    logger.warn("No legacy template found with id {}: Cannot update cmsPage {}", legacyPageTemplateId, page.getId());
                }
            } catch (Throwable e) {
                logger.error("Error updating page {}", page.getId(), e);
                throw e;
            }
            updatedPages.add(page);
        }
        


        for (CMSPage cmsPage : updatedPages) {
            try {                
                if(!dao.updateCMSPage(cmsPage)) {
                    throw new DAOException("Saving page failed");
                }
            } catch (Throwable e) {
                logger.error("Error updating page {}", cmsPage.getId(), e);
                throw e;
            }
        }
        
        dao.executeUpdate("DROP TABLE cms_content_item_cms_categories;");
        dao.executeUpdate("DROP TABLE cms_content_items;");
        dao.executeUpdate("DROP TABLE cms_page_language_versions;");
        dao.executeUpdate("ALTER TABLE cms_pages DROP COLUMN template_id;");

        return true;
    }

    private Map<String, CMSContent> createContentObjects(Map<String, List<Map<String, Object>>> contentItemsMap, IDAO dao) {
        Map<String, CMSContent> contentMap = new HashMap<>();
        for (Entry<String, List<Map<String, Object>>> legacyItemEntry : contentItemsMap.entrySet()) {
            String language = legacyItemEntry.getKey();
            for (Map<String, Object> legacyItem : legacyItemEntry.getValue()) {
                String type = (String) legacyItem.get("type");
                String legacyItemId = (String) legacyItem.get("item_id");
                try {
                    CMSContent content = createContent(legacyItem, type, Optional.ofNullable(contentMap.get(legacyItemId)), language);
                    if (content != null) {
                        content.setItemId(legacyItemId);
                        contentMap.put(legacyItemId, content);
                    }
                } catch (DAOException | IllegalArgumentException e) {
                    logger.error("Error creating content from legacy item of tye {} with item-id {}", type, legacyItemId);
                }
            }
        }
        return contentMap;
    }

    private CMSContent createContent(Map<String, Object> legacyItem, String type, Optional<CMSContent> existingContent, String language)
            throws DAOException {
        switch (type) {
            case "TEXT":
                return contentConverter.createShortTextContent(language, legacyItem, existingContent);
            case "HTML":
                return contentConverter.createMediumTextContent(language, legacyItem, existingContent);
            case "SOLRQUERY":
                return contentConverter.createRecordListContent(legacyItem);
            case "PAGELIST":
                return contentConverter.createPageListContent(legacyItem);
            case "COLLECTION":
                return contentConverter.createCollectionContent(legacyItem);
            case "TILEGRID":
                return contentConverter.createImageListContent(legacyItem);
            case "RSS":
                return contentConverter.createRSSContent(legacyItem);
            case "SEARCH":
                return contentConverter.createSearchContent(legacyItem);
            case "GLOSSARY":
                return contentConverter.createGlossaryContent(legacyItem);
            case "MEDIA":
                return contentConverter.createMediaContent(legacyItem);
            case "GEOMAP":
                return contentConverter.createGeomapContent(legacyItem);
            case "SLIDER":
                return contentConverter.createSliderContent(legacyItem);
            case "METADATA":
                return contentConverter.createMetadataContent(legacyItem);
            default:
                return null;
        }
    }


    /**
     * 
     * @param pageLanguageVersions language versions of a page mapped by language string
     * @param contentItemMap List of contentItems belonging to a single language version, mapped by language version id
     * @return
     */
    private Map<String, List<Map<String, Object>>> getContentItemsForPage(Map<String, Map<String, Object>> pageLanguageVersions,
            Map<Long, List<Map<String, Object>>> contentItemMap) {
        Map<String, List<Map<String, Object>>> map = new HashMap<>();
        for (Entry<String, Map<String, Object>> langEntry : pageLanguageVersions.entrySet()) {
            String langauge = langEntry.getKey();
            Long langVersionId = (Long) langEntry.getValue().get("cms_page_language_version_id");
            List<Map<String, Object>> contentItems = contentItemMap.get(langVersionId);
            if (contentItems != null) {
                contentItems = contentItems.stream().filter(i -> {
                    String itemId = (String) i.get("item_id");
                    return !"topbar_slider".equals(itemId) && !"preview01".equals(itemId);
                }).collect(Collectors.toList());
                map.put(langauge, contentItems);
            }
        }
        return map;
    }

    private Long getTopbarSliderId(Map<Long, List<Map<String, Object>>> contentItemMap, Map<String, Map<String, Object>> pageLanguageVersions) {
        Map<String, Map<String, Object>> topbarSliderItems = getContentItemsOfItemId(pageLanguageVersions, contentItemMap, "topbar_slider");
        if (!topbarSliderItems.isEmpty()) {
            Map<String, Object> sliderItem = topbarSliderItems.values().iterator().next();
            Long sliderId = (Long) sliderItem.get("slider_id");
            return sliderId;
        } else {
            return null;
        }
    }

    private Map<String, Map<String, Object>> getContentItemsOfItemId(
            Map<String, Map<String, Object>> pageLanguageVersions,
            Map<Long, List<Map<String, Object>>> contentItemMap,
            String itemId) {

        Map<Long, String> languageVersionIdsAndLanguages = pageLanguageVersions.entrySet()
                .stream()
                .collect(Collectors.toMap(e -> (Long) e.getValue().get("cms_page_language_version_id"), e -> e.getKey()));
        Map<String, Map<String, Object>> pageContentItems = new HashMap<>();
        for (Long langVersionId : languageVersionIdsAndLanguages.keySet()) {
            String language = languageVersionIdsAndLanguages.get(langVersionId);
            List<Map<String, Object>> items = contentItemMap.get(langVersionId);
            if (items != null) {
                Map<String, Object> item = items.stream().filter(map -> Objects.equals(map.get("item_id"), itemId)).findAny().orElse(null);
                if (item != null) {
                    pageContentItems.put(language, item);
                }
            }
        }
        return pageContentItems;
    }

    private TranslatedText getTranslatedText(Map<String, Map<String, Object>> pageLanguageVersions, String field) {
        Map<String, String> titleValues = pageLanguageVersions.entrySet()
                .stream()
                .filter(e -> !e.getKey().equals("global"))
                .collect(Collectors.toMap(e -> e.getKey(), e -> (String) e.getValue().get(field)));
        TranslatedText title = new TranslatedText(new MultiLanguageMetadataValue(titleValues), IPolyglott.getDefaultLocale());
        return title;
    }

    private <K, V> Map<K, V> combineMaps(Map<K, V> map1, Map<K, V> map2) {
        Map<K, V> union = new HashMap<>();
        union.putAll(map1);
        union.putAll(map2);
        return union;
    }

    private List<Map<String, Object>> getTableData(IDAO dao, String tableName) throws DAOException {
        List<Object[]> info = dao.getNativeQueryResults("SHOW COLUMNS FROM " + tableName);

        List<Object[]> rows = dao.getNativeQueryResults("SELECT * FROM " + tableName);

        List<String> columnNames = info.stream().map(o -> (String) o[0]).collect(Collectors.toList());

        List<Map<String, Object>> table = new ArrayList<>();

        for (Object[] row : rows) {
            Map<String, Object> columns = IntStream.range(0, columnNames.size())
                    .boxed()
                    .filter(i -> row[i] != null)
                    .collect(Collectors.toMap(i -> columnNames.get(i), i -> row[i]));
            table.add(columns);
        }
        return table;
    }

}
