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
package io.goobi.viewer.model.archives;

import java.io.IOException;
import java.io.StringReader;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ws.rs.core.UriBuilder;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.configuration2.tree.xpath.XPathExpressionEngine;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.Text;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.exceptions.HTTPException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrSearchIndex;
import io.goobi.viewer.solr.SolrTools;

/**
 * Loads and parses EAD documents from BaseX databases.
 */
public class BasexEADParser {

    private static final Logger logger = LoggerFactory.getLogger(BasexEADParser.class);

    public static final Namespace NAMESPACE_EAD = Namespace.getNamespace("ead", "urn:isbn:1-931666-22-9");

    private static final XPathFactory xFactory = XPathFactory.instance();

    private final String basexUrl;

    private String selectedDatabase;

    private List<ArchiveMetadataField> configuredFields;

    private final Map<String, Entry<String, Boolean>> associatedRecordMap;

    //    private List<StringPair> eventList;
    //    private List<String> editorList;

    /**
     * 
     * @param configFilePath
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws ConfigurationException
     */
    public BasexEADParser(String basexUrl, SolrSearchIndex searchIndex) throws PresentationException, IndexUnreachableException {
        this.basexUrl = basexUrl;
        this.associatedRecordMap = getAssociatedRecordPis(searchIndex);
    }

    private static Map<String, Entry<String, Boolean>> getAssociatedRecordPis(SolrSearchIndex searchIndex) throws PresentationException, IndexUnreachableException {
        if(searchIndex != null) {
        return searchIndex
                .search("+" + SolrConstants.ARCHIVE_ENTRY_ID + ":*" + " +" + SolrConstants.PI + ":*",
                        Arrays.asList(SolrConstants.ARCHIVE_ENTRY_ID, SolrConstants.PI, SolrConstants.BOOL_IMAGEAVAILABLE))
                .stream()
                .collect(Collectors.toMap(doc -> SolrTools.getAsString(doc.getFieldValue(SolrConstants.ARCHIVE_ENTRY_ID)),
                        doc -> new SimpleEntry<String, Boolean>(SolrTools.getAsString(doc.getFieldValue(SolrConstants.PI)),
                                SolrTools.getAsBoolean(doc.getFieldValue(SolrConstants.BOOL_IMAGEAVAILABLE)))));
        } else {
            return Collections.emptyMap();
        }
    }

    /**
     * Get the database names and file names from the basex databases
     * 
     * @return
     * @throws HTTPException
     * @throws IOException
     * @throws ClientProtocolException
     */
    public List<ArchiveResource> getPossibleDatabases() throws ClientProtocolException, IOException, HTTPException {
        String response = "";
            response = NetTools.getWebContentGET(basexUrl + "databases");
        if (StringUtils.isBlank(response)) {
            return Collections.emptyList();
        }

        try {
            Document document = openDocument(response);

            Element root = document.getRootElement();
            List<Element> databaseList = root.getChildren("database");
            List<ArchiveResource> ret = new ArrayList<>();
            for (Element db : databaseList) {
                String dbName = db.getChildText("name");

                Element details = db.getChild("details");
                for (Element resource : details.getChildren()) {
                    String resourceName = resource.getText();
                    String lastUpdated = resource.getAttributeValue("modified-date");
                    String size = resource.getAttributeValue("size");
                    ArchiveResource eadResource = new ArchiveResource(dbName, resourceName, lastUpdated, size);
                    ret.add(eadResource);
                }
            }
            return ret;

        } catch (JDOMException e) {
            logger.error("Failed to parse response from " + (basexUrl + "databases"), e);
            return Collections.emptyList();
        }
    }

    /**
     * 
     * @param database
     * @return
     * @throws IOException
     * @throws IllegalStateException
     * @throws HTTPException
     * @throws JDOMException
     */
    public Document retrieveDatabaseDocument(ArchiveResource archive) throws IOException, IllegalStateException, HTTPException, JDOMException {
        if (archive != null) {
            String response;
                String url = UriBuilder.fromPath(basexUrl).path("db").path(archive.getDatabaseName()).path(archive.getResourceName()).build().toString();
                logger.trace("URL: {}", url);
                response = NetTools.getWebContentGET(url);

            // get xml root element
                Document document = openDocument(response);
                return document;
        }
        throw new IllegalStateException("Must provide database name before loading database");
    }

    /**
     * Loads the given database and parses the EAD document.
     * 
     * @param database
     * @param document
     * @return Root element of the loaded tree
     * @throws IllegalStateException
     * @throws IOException
     * @throws HTTPException
     * @throws JDOMException
     * @throws ConfigurationException
     */
    public ArchiveEntry loadDatabase(ArchiveResource database, Document document)
            throws IllegalStateException, IOException, HTTPException, JDOMException, ConfigurationException {

        if (document == null) {
            document = retrieveDatabaseDocument(database);
        }

        // parse ead file
        return parseEadFile(document);
    }

    public List<String> getDistinctDatabaseNames() throws ClientProtocolException, IOException, HTTPException {
        List<String> answer = new ArrayList<>();
        List<ArchiveResource> completeList = getPossibleDatabases();
        for (ArchiveResource resource : completeList) {
            String dbName = resource.getCombinedName();
            if (!answer.contains(dbName)) {
                answer.add(dbName);
            }
        }

        return answer;
    }

    /**
     * Reads the hierarchy from the given EAD document.
     * 
     * @param document
     * @return Root element of the tree
     * @should parse document correctly
     */
    ArchiveEntry parseEadFile(Document document) {
        if (document == null) {
            throw new IllegalArgumentException("document may not be null");
        }

        Element collection = document.getRootElement();
        Element eadElement = collection.getChild("ead", NAMESPACE_EAD);
        ArchiveEntry rootElement = parseElement(1, 0, eadElement, configuredFields, associatedRecordMap);
        rootElement.setDisplayChildren(true);

        return rootElement;
    }

    /**
     * read the metadata for the current xml node. - create an {@link ArchiveEntry} - execute the configured xpaths on the current node - add the
     * metadata to one of the 7 levels - check if the node has sub nodes - call the method recursively for all sub nodes
     * 
     * @param order
     * @param hierarchy
     * @param element
     * @param configuredFields
     * @return
     */
    private static ArchiveEntry parseElement(int order, int hierarchy, Element element, List<ArchiveMetadataField> configuredFields,
            Map<String, Entry<String, Boolean>> associatedPIs) {
        if (element == null) {
            throw new IllegalArgumentException("element may not be null");
        }
        if (configuredFields == null) {
            throw new IllegalArgumentException("configuredFields may not be null");
        }

        ArchiveEntry entry = new ArchiveEntry(order, hierarchy);

        for (ArchiveMetadataField emf : configuredFields) {

            List<String> stringValues = new ArrayList<>();
            if ("text".equalsIgnoreCase(emf.getXpathType())) {
                XPathExpression<Text> engine = xFactory.compile(emf.getXpath(), Filters.text(), null, NAMESPACE_EAD);
                List<Text> values = engine.evaluate(element);
                for (Text value : values) {
                    String stringValue = value.getValue();
                    stringValues.add(stringValue);
                }
            } else if ("attribute".equalsIgnoreCase(emf.getXpathType())) {
                XPathExpression<Attribute> engine = xFactory.compile(emf.getXpath(), Filters.attribute(), null, NAMESPACE_EAD);
                List<Attribute> values = engine.evaluate(element);

                for (Attribute value : values) {
                    String stringValue = value.getValue();
                    stringValues.add(stringValue);
                }
            } else {
                XPathExpression<Element> engine = xFactory.compile(emf.getXpath(), Filters.element(), null, NAMESPACE_EAD);
                List<Element> values = engine.evaluate(element);
                for (Element value : values) {
                    String stringValue = value.getValue();
                    stringValues.add(stringValue);
                }
            }
            addFieldToEntry(entry, emf, stringValues);
        }

        Element eadheader = element.getChild("eadheader", NAMESPACE_EAD);

        entry.setId(element.getAttributeValue("id"));

        Optional.ofNullable(eadheader)
        .map(e -> e.getChild("filedesc", NAMESPACE_EAD))
        .map(e -> e.getChild("titlestmt", NAMESPACE_EAD))
        .map(e -> e.getChildText("titleproper", NAMESPACE_EAD))
        .ifPresent(s -> entry.setLabel(s));


        // nodeType
        // get child elements
        List<Element> clist = null;
        Element archdesc = element.getChild("archdesc", NAMESPACE_EAD);
        if (archdesc != null) {
            String type = archdesc.getAttributeValue("otherlevel");
            if (StringUtils.isBlank(type)) {
                type = archdesc.getAttributeValue("level");
            }
            entry.setNodeType(type);
            Element dsc = archdesc.getChild("dsc", NAMESPACE_EAD);
            if (dsc != null) {
                clist = dsc.getChildren("c", NAMESPACE_EAD);
            }

        } else {
            String type = element.getAttributeValue("level");
            entry.setNodeType(type);

        }

        if (entry.getNodeType() == null) {
            entry.setNodeType("folder");
        }

        Entry<String, Boolean> associatedRecordEntry = associatedPIs.get(entry.getId());
        if (associatedRecordEntry != null) {
            entry.setAssociatedRecordPi(associatedRecordEntry.getKey());
            entry.setContainsImage(associatedRecordEntry.getValue());
        }

        // Set description level value
        entry.setDescriptionLevel(element.getAttributeValue("level"));

        if (clist == null) {
            clist = element.getChildren("c", NAMESPACE_EAD);
        }
        if (clist != null) {
            int subOrder = 0;
            int subHierarchy = hierarchy + 1;
            for (Element c : clist) {
                ArchiveEntry child = parseElement(subOrder, subHierarchy, c, configuredFields, associatedPIs);
                entry.addSubEntry(child);
                child.setParentNode(entry);
                if (child.isContainsImage()) {
                    entry.setContainsImage(true);
                }
                subOrder++;
            }
        }

        // generate new id, if id is null
        if (entry.getId() == null) {
            entry.setId(String.valueOf(UUID.randomUUID()));
        }

        return entry;
    }

    /**
     * Add the metadata to the configured level
     * 
     * @param entry
     * @param emf
     * @param stringValue
     */

    private static void addFieldToEntry(ArchiveEntry entry, ArchiveMetadataField emf, List<String> stringValues) {
        if (StringUtils.isBlank(entry.getLabel()) && emf.getXpath().contains("unittitle") && stringValues != null && !stringValues.isEmpty()) {
            entry.setLabel(stringValues.get(0));
        }
        ArchiveMetadataField toAdd = new ArchiveMetadataField(emf.getLabel(), emf.getType(), emf.getXpath(), emf.getXpathType());
        toAdd.setEadEntry(entry);

        if (stringValues != null && !stringValues.isEmpty()) {

            // split single value into multiple fields
            for (String stringValue : stringValues) {
                FieldValue fv = new FieldValue(toAdd);
                fv.setValue(stringValue);
                toAdd.addFieldValue(fv);
            }
        } else {
            FieldValue fv = new FieldValue(toAdd);
            toAdd.addFieldValue(fv);
        }

        switch (toAdd.getType()) {
            case 1:
                entry.getIdentityStatementAreaList().add(toAdd);
                break;
            case 2:
                entry.getContextAreaList().add(toAdd);
                break;
            case 3:
                entry.getContentAndStructureAreaAreaList().add(toAdd);
                break;
            case 4:
                entry.getAccessAndUseAreaList().add(toAdd);
                break;
            case 5:
                entry.getAlliedMaterialsAreaList().add(toAdd);
                break;
            case 6:
                entry.getNotesAreaList().add(toAdd);
                break;
            case 7:
                entry.getDescriptionControlAreaList().add(toAdd);
                break;
        }

    }

    /**
     * Parse the string response from the basex database into a xml document
     * 
     * @param response
     * @return
     * @throws IOException
     * @throws JDOMException
     */
    private static Document openDocument(String response) throws JDOMException, IOException {
        // read response
        SAXBuilder builder = new SAXBuilder(XMLReaders.NONVALIDATING);
        builder.setFeature("http://xml.org/sax/features/validation", false);
        builder.setFeature("http://xml.org/sax/features/external-general-entities", false);
        builder.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        builder.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        builder.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        builder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        Document document = builder.build(new StringReader(response), "utf-8");
        return document;

    }

    /**
     * 
     * @param node
     * @param searchValue
     */
    static void searchInNode(ArchiveEntry node, String searchValue) {
        if (node.getId() != null && node.getId().equals(searchValue)) {
            // ID match
            node.markAsFound(true);
        } else if (node.getLabel() != null && node.getLabel().toLowerCase().contains(searchValue.toLowerCase())) {
            // mark element + all parents as displayable
            node.markAsFound(true);
        }
        if (node.getSubEntryList() != null) {
            for (ArchiveEntry child : node.getSubEntryList()) {
                searchInNode(child, searchValue);
            }
        }
    }

    /**
     * Loads fields from the given configuration node.
     * 
     * @param metadataConfig
     * @return
     * @throws ConfigurationException
     */
    public BasexEADParser readConfiguration(HierarchicalConfiguration<ImmutableNode> metadataConfig) throws ConfigurationException {
        if (metadataConfig == null) {
            throw new ConfigurationException("No basexMetadata configurations found");
        }

        // metadataConfig.setListDelimiter('&');
        metadataConfig.setExpressionEngine(new XPathExpressionEngine());

        try {
            List<HierarchicalConfiguration<ImmutableNode>> configurations = metadataConfig.configurationsAt("/metadata");
            if (configurations == null) {
                throw new ConfigurationException("No basexMetadata configurations found");
            }
            configuredFields = new ArrayList<>(configurations.size());
            for (HierarchicalConfiguration<ImmutableNode> hc : configurations) {
                ArchiveMetadataField field = new ArchiveMetadataField(hc.getString("@label"), hc.getInt("@type"), hc.getString("@xpath"),
                        hc.getString("@xpathType", "element"));
                configuredFields.add(field);
            }
        } catch (Exception e) {
            throw new ConfigurationException("Error reading basexMetadata configuration", e);
        }

        return this;
    }

    /**
     * @return the selectedDatabase
     */
    public String getSelectedDatabase() {
        return selectedDatabase;
    }

    /**
     * @return the basexUrl
     */
    public String getBasexUrl() {
        return basexUrl;
    }

    public static String getIdForName(String name) {
        return name.replaceAll("(?i)\\.xml", "");
    }
}
