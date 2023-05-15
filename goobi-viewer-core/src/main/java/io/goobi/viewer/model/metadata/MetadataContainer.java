package io.goobi.viewer.model.metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.solr.common.SolrDocument;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import io.goobi.viewer.controller.GeoCoordinateConverter;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;

/**
 * An object containing a number  of translatable metadata values.
 * The values are mapped to a key which is a string corresponding to the SOLR field name the values are taken from
 * Each field name/key is mapped to a list of {@link IMetadataValue} objects which may contain a single string or translations in several languages
 * Used in {@link GeoCoordinateConverter} to add translatable metadata entites to geomap features
 * @author florian
 *
 */
public class MetadataContainer {

    private final String solrId;
    private final IMetadataValue label;
    
    private final Map<String, List<IMetadataValue>> metadata;

    public MetadataContainer(String solrId, IMetadataValue label, Map<String, List<IMetadataValue>> metadata) {
        this.solrId = solrId;
        this.label = label;
        this.metadata = metadata;
    }

    public MetadataContainer(String solrId, IMetadataValue label) {
        this(solrId, label, new HashMap<>());
    }
    
    public MetadataContainer(String solrId, String label) {
        this(solrId, new SimpleMetadataValue(label));
    }
    
    public MetadataContainer(MetadataContainer orig) {
        this.solrId = orig.solrId;
        this.label = orig.label.copy();
        this.metadata = orig.metadata.entrySet().stream().collect(Collectors.toMap(Entry::getKey, e -> e.getValue().stream().map(IMetadataValue::copy).collect(Collectors.toList())));
    }
    
    public String getSolrId() {
        return solrId;
    }
    
    public IMetadataValue getLabel() {
        return label;
    }
    
    public String getLabel(Locale locale) {
        return label.getValueOrFallback(locale);
    }
    
    public Map<String, List<IMetadataValue>> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }
    
    public List<IMetadataValue> get(String key) {
        return this.metadata.getOrDefault(key, Collections.emptyList());
    }
    
    public String getFirstValue(String key) {
        return this.get(key).stream().findFirst().flatMap(IMetadataValue::getValue).orElse("");
    }
    
    public void put(String key, List<IMetadataValue> values) {
        this.metadata.put(key, values);
    }
    
    public void add(String key, IMetadataValue value) {
        this.metadata.computeIfAbsent(key, a -> new ArrayList<>()).add(value);
    }
    
    public void add(String key, String value) {
        this.add(key, new SimpleMetadataValue(value));
    }
    
    public void addAll(String key, Collection<IMetadataValue> values) {
        values.forEach(v -> this.add(key, v));
    }

    public void remove(String key) {
        this.metadata.remove(key);
    }
    
    /**
     * Returns a {@link MetadataContainer} which includes all metadata fields matching the given fieldNameFilter from the given {@link SolrDocument} doc
     * as well as the {@link SolrConstants#MD_VALUE values} of those child documents which {@link SolrConstants#LABEL label} matches the fieldnameFilter
     * @param doc   The main DOCSTRUCT document
     * @param children  METADATA type documents belonging to the main doc
     * @param fieldNameFilter   A function which should return true for all metadata field names to be included in the return value
     * @return  a {@link MetadataContainer}
     */
    public static MetadataContainer createMetadataEntity(SolrDocument doc, List<SolrDocument> children, Predicate<String> fieldNameFilter) {
        Map<String, List<IMetadataValue>> translatedMetadata = SolrTools.getTranslatedMetadata(doc, fieldNameFilter::test);
        MetadataContainer entity = new MetadataContainer(
                SolrTools.getSingleFieldStringValue(doc, SolrConstants.IDDOC), 
                Optional.ofNullable(SolrTools.getSingleFieldStringValue(doc, SolrConstants.LABEL)).orElse(Optional.ofNullable(SolrTools.getSingleFieldStringValue(doc, SolrConstants.MD_VALUE)).orElse("")));
        
        Set<String> childLabels = children.stream().map(c -> SolrTools.getSingleFieldStringValue(c, SolrConstants.LABEL)).map(SolrTools::getBaseFieldName).collect(Collectors.toSet());
        translatedMetadata.entrySet().stream()
        .filter(e -> !childLabels.contains(SolrTools.getBaseFieldName(e.getKey())))
        .forEach(e -> entity.put(e.getKey(), e.getValue()));

        List<ComplexMetadata> childDocs = ComplexMetadata.getMetadataFromDocuments(children);
        List<Entry<String, List<IMetadataValue>>> allChildDocValues = childDocs.stream().map(mdDoc -> mdDoc.getMetadata().entrySet()).flatMap(Set::stream).filter(e -> fieldNameFilter.test(e.getKey())).collect(Collectors.toList());
        allChildDocValues.forEach(e -> entity.addAll(e.getKey(),  e.getValue()));
        return entity;
    }
    
    /**
     * Returns a {@link MetadataContainer} which includes all metadata fields matching the given fieldNameFilter from the given {@link SolrDocument} doc
     * @param doc   The main DOCSTRUCT document
     * @param fieldNameFilter   A function which should return true for all metadata field names to be included in the return value
     * @return  a {@link MetadataContainer}
     */
    public static MetadataContainer createMetadataEntity(SolrDocument doc, Predicate<String> fieldNameFilter) {
        return createMetadataEntity(doc, Collections.emptyList(), fieldNameFilter);
    }
    
    /**
     * Returns a {@link MetadataContainer} which includes all metadata fields matching the given fieldNameFilter from the given {@link SolrDocument} doc
     * @param doc   The main DOCSTRUCT document
     * @param fieldNameFilter   A function which should return true for all metadata field names to be included in the return value
     * @return  a {@link MetadataContainer}
     */
    public static MetadataContainer createMetadataEntity(SolrDocument doc) {
        return createMetadataEntity(doc, Collections.emptyList(), s -> true);
    }
    
}
