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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.goobi.viewer.Version;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.servlet.model.ApplicationInfo;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ApplicationResource;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.JsonTools;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.controller.SolrConstants.DocType;
import io.goobi.viewer.exceptions.HTTPException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.search.SearchHelper;

/**
 * Bean for the statistics page.
 */
@Named
@ApplicationScoped
public class StatisticsBean implements Serializable {

    private static final long serialVersionUID = -1530519697198096431L;

    private static final Logger logger = LoggerFactory.getLogger(ActiveDocumentBean.class);

    /** Constant <code>SEPARATOR="::"</code> */
    public static final String SEPARATOR = "::";
    private static final int DAY_MS = 86400000;

    private Map<String, Long> lastUpdateMap = new HashMap<>();
    private Map<String, Object> valueMap = new HashMap<>();

    /**
     * <p>
     * getImportedRecordsTrend.
     * </p>
     *
     * @param days a int.
     * @param dataPoints a int.
     * @return a {@link java.util.List} object.
     */
    public List<String> getImportedRecordsTrend(int days, int dataPoints) {
        logger.debug("getImportedRecordsTrend start");

        List<Integer> countList = new ArrayList<>();
        List<String> dateList = new ArrayList<>();
        // Facetting
        try {
            QueryResponse resp = DataManager.getInstance()
                    .getSearchIndex()
                    .search(new StringBuilder(SolrConstants.PI).append(":*").append(SearchHelper.getAllSuffixes()).toString(), 0, 0, null,
                            Collections.singletonList(SolrConstants.DATECREATED), "count", Collections.singletonList(SolrConstants.DATECREATED), null,
                            null);
            if (resp != null && resp.getFacetField(SolrConstants.DATECREATED) != null
                    && resp.getFacetField(SolrConstants.DATECREATED).getValues() != null) {
                List<Count> counts = resp.getFacetField(SolrConstants.DATECREATED).getValues();
                countList = new ArrayList<>(counts.size() + dataPoints + 1);
                dateList = new ArrayList<>(counts.size());
                for (Count count : counts) {
                    String name = ViewerResourceBundle.getTranslation(count.getName(), null);
                    // TODO limit the number of results?
                    // ret.add(new String[] { count.getName(), String.valueOf(count.getCount()) });
                    dateList.add(name);
                }
            }
        } catch (PresentationException e) {
            logger.debug("PresentationException thrown here: {}", e.getMessage());
            return null;
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
            return null;
        }

        logger.debug("getImportedRecordsTrend mid");

        if (dataPoints > days) {
            dataPoints = days;
        }
        List<Long> dataPointList = new ArrayList<>();
        int dataPointDiv = days / dataPoints;
        days = dataPoints * dataPointDiv;
        dataPointList.add(new Date().getTime());
        countList.add(0);
        GregorianCalendar cal = new GregorianCalendar();
        for (int i = 1; i < dataPoints; i++) {
            cal.add(Calendar.DAY_OF_MONTH, -dataPointDiv);
            dataPointList.add(cal.getTime().getTime());
            countList.add(0);
        }
        Collections.sort(dataPointList);
        Collections.reverse(dataPointList);

        for (String string : dateList) {
            long creationTime = Long.valueOf(string);
            int count = 0;
            for (Long time : dataPointList) {
                if (creationTime < time) {
                    countList.set(count, countList.get(count) + 1);
                    //                    break;
                }
                count++;
            }
        }

        List<String> ret = new ArrayList<>(countList.size());
        for (int i = 0; i < countList.size(); i++) {
            ret.add(new StringBuilder(String.valueOf(dataPointList.get(i))).append(SEPARATOR).append(countList.get(i)).toString());
        }

        logger.debug("getImportedRecordsTrend end");
        //        Collections.reverse(ret);
        return ret;
    }

    /**
     * Returns a list of size two arrays which each contain the name and total number of imported works of a type of work (DocStructType).
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getTopStructTypesByNumber() {
        logger.debug("getTopStructTypesByNumber start");

        try {
            QueryResponse resp = DataManager.getInstance()
                    .getSearchIndex()
                    .search(new StringBuilder(SolrConstants.PI).append(":*")
                            .append(" AND (")
                            .append(SolrConstants.ISWORK)
                            .append(":true OR ")
                            .append(SolrConstants.ISANCHOR)
                            .append(":true)")
                            .append(SearchHelper.getAllSuffixes())
                            .toString(), 0, 0, null, Collections.singletonList(SolrConstants.DOCSTRCT), "count",
                            Collections.singletonList(SolrConstants.DOCSTRCT), null, null);
            if (resp != null && resp.getFacetField(SolrConstants.DOCSTRCT) != null
                    && resp.getFacetField(SolrConstants.DOCSTRCT).getValues() != null) {
                List<Count> counts = resp.getFacetField(SolrConstants.DOCSTRCT).getValues();
                List<String> ret = new ArrayList<>(counts.size());
                for (Count count : counts) {
                    String name = ViewerResourceBundle.getTranslation(count.getName(), null).replaceAll(",", "");
                    // TODO limit the number of results?
                    //                    ret.add(new String[] { count.getName(), String.valueOf(count.getCount()) });
                    ret.add(name + SEPARATOR + count.getCount() + SEPARATOR + count.getName());
                }
                return ret;
            }
        } catch (PresentationException e) {
            logger.debug("PresentationException thrown here: {}", e.getMessage());
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
        } finally {
            logger.debug("getTopStructTypesByNumber end");
        }

        return Collections.emptyList();
    }

    /**
     * Returns the total number of imported pages.
     *
     * @return a {@link java.lang.Long} object.
     */
    public Long getImportedPages() {
        logger.debug("getImportedPages start");
        long now = System.currentTimeMillis();
        try {
            if (lastUpdateMap.get("getImportedPages") == null || now - lastUpdateMap.get("getImportedPages") >= DAY_MS) {
                logger.debug("Refreshing number of imported pages...");
                // TODO filter query might not work for PAGE documents
                long pages = DataManager.getInstance()
                        .getSearchIndex()
                        .getHitCount(SolrConstants.DOCTYPE + ":" + DocType.PAGE.name() + SearchHelper.getAllSuffixes());
                // Fallback for older indexes that do not have the DOCTYPE field (slower)
                if (pages == 0) {
                    pages = DataManager.getInstance()
                            .getSearchIndex()
                            .getHitCount(SolrConstants.FILENAME + ":['' TO *]" + SearchHelper.getAllSuffixes());
                }
                valueMap.put("getImportedPages", pages);
                lastUpdateMap.put("getImportedPages", now);
            }
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
        } catch (PresentationException e) {
            logger.debug("PresentationException thrown here: {}", e.getMessage());
        } finally {
            logger.debug("getImportedPages end");
        }

        return (Long) valueMap.get("getImportedPages");
    }

    /**
     * Returns the total number of pages with OCR data.
     *
     * @return a {@link java.lang.Long} object.
     */
    public Long getImportedFullTexts() {
        logger.debug("getImportedFullTexts start");
        long now = System.currentTimeMillis();
        try {
            if (lastUpdateMap.get("getImportedFullTexts") == null || now - lastUpdateMap.get("getImportedFullTexts") >= DAY_MS) {
                logger.debug("Refreshing number of imported fulltexts...");
                String query = "+" + SolrConstants.DOCTYPE + ":" + SolrConstants.DocType.PAGE.name() + " +" + SolrConstants.FULLTEXTAVAILABLE
                        + ":true" + SearchHelper.getAllSuffixes();
                long pages = DataManager.getInstance().getSearchIndex().getHitCount(query);
                valueMap.put("getImportedFullTexts", pages);
                lastUpdateMap.put("getImportedFullTexts", now);
            }
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
        } catch (PresentationException e) {
            logger.debug("PresentationException thrown here: {}", e.getMessage());
        } finally {
            logger.debug("getImportedFullTexts end");
        }

        return (Long) valueMap.get("getImportedFullTexts");
    }

    /**
     * Checks whether there are no records in the index.
     *
     * @return true if record count is 0; false otherwise.
     */
    public boolean isIndexEmpty() {
        try {
            return DataManager.getInstance().getSearchIndex().getHitCount(new StringBuilder(SolrConstants.PI).append(":*").toString()) == 0;
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
        } catch (PresentationException e) {
            logger.debug("PresentationException thrown here: {}", e.getMessage());
        }

        return true;
    }

    /**
     * 
     * @return goobi-viewer-core version
     */
    public String getCoreVersion() {
        return JsonTools.shortFormatVersionString(Version.asJSON());
    }

    /**
     * @return goobi-viewer-connector version
     */
    public String getConnectorVersion() {
        String version;
        try {
            String json = NetTools.getWebContentGET(DataManager.getInstance().getConfiguration().getConnectorVersionUrl());
            return JsonTools.shortFormatVersionString(json);
        } catch (IOException | HTTPException e) {
            logger.error(e.getMessage());
            return e.getMessage();
        }
    }

    /**
     * @return intrandaContentServer version
     */
    public String getContentServerVersion() {
        try {
            ApplicationInfo info = new ApplicationResource().getApplicationInfo();
            String json = new ObjectMapper().writeValueAsString(info);
            return JsonTools.shortFormatVersionString(json);
            //            String output = String.format("%s (%s)", info.getVersion(), info.getGitRevision());
            //            return output;
        } catch (ContentNotFoundException | IOException e) {
            logger.error(e.getMessage());
            return "";
        }
    }

    /**
     * @return goobi-viewer-indexer version
     */
    public String getIndexerVersion() {
        return JsonTools.shortFormatVersionString(DataManager.getInstance().getIndexerVersion());
    }

    /**
     * <p>
     * main.
     * </p>
     *
     * @param args an array of {@link java.lang.String} objects.
     */
    public static void main(String[] args) {
        //        for (String[] pair : new StatisticsBean().getMostEditedRecords(10)) {
        //            System.out.println(pair[0] + ": " + pair[1]);
        //        }
    }
}
