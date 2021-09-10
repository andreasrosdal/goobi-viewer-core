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
package io.goobi.viewer.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.configuration2.BaseHierarchicalConfiguration;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.ConfigurationBuilderEvent;
import org.apache.commons.configuration2.builder.ReloadingFileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.event.Event;
import org.apache.commons.configuration2.event.EventListener;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unigoettingen.sub.commons.contentlib.imagelib.ImageType;
import io.goobi.viewer.controller.model.ProviderConfiguration;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.citation.CitationLink;
import io.goobi.viewer.model.download.DownloadOption;
import io.goobi.viewer.model.maps.GeoMapMarker;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.metadata.MetadataParameter;
import io.goobi.viewer.model.metadata.MetadataParameter.MetadataParameterType;
import io.goobi.viewer.model.metadata.MetadataReplaceRule;
import io.goobi.viewer.model.metadata.MetadataReplaceRule.MetadataReplaceRuleType;
import io.goobi.viewer.model.metadata.MetadataView;
import io.goobi.viewer.model.misc.EmailRecipient;
import io.goobi.viewer.model.search.AdvancedSearchFieldConfiguration;
import io.goobi.viewer.model.search.SearchFilter;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.security.SecurityQuestion;
import io.goobi.viewer.model.security.authentication.BibliothecaProvider;
import io.goobi.viewer.model.security.authentication.IAuthenticationProvider;
import io.goobi.viewer.model.security.authentication.LitteraProvider;
import io.goobi.viewer.model.security.authentication.LocalAuthenticationProvider;
import io.goobi.viewer.model.security.authentication.OpenIdProvider;
import io.goobi.viewer.model.security.authentication.SAMLProvider;
import io.goobi.viewer.model.security.authentication.VuFindProvider;
import io.goobi.viewer.model.security.authentication.XServiceProvider;
import io.goobi.viewer.model.termbrowsing.BrowsingMenuFieldConfig;
import io.goobi.viewer.model.transkribus.TranskribusUtils;
import io.goobi.viewer.model.translations.admin.TranslationGroup;
import io.goobi.viewer.model.translations.admin.TranslationGroup.TranslationGroupType;
import io.goobi.viewer.model.translations.admin.TranslationGroupItem;
import io.goobi.viewer.model.viewer.DcSortingList;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.solr.SolrConstants;

/**
 * <p>
 * Configuration class.
 * </p>
 */
public final class Configuration extends AbstractConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

    private Set<String> stopwords;

    /**
     * <p>
     * Constructor for Configuration.
     * </p>
     *
     * @param configFilePath a {@link java.lang.String} object.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Configuration(String configFilePath) {
        // Load default config file
        builder =
                new ReloadingFileBasedConfigurationBuilder<XMLConfiguration>(XMLConfiguration.class)
                        .configure(new Parameters().properties()
                                .setFileName(configFilePath)
                                .setListDelimiterHandler(new DefaultListDelimiterHandler(','))
                                .setThrowExceptionOnMissing(false));
        if (builder.getFileHandler().getFile().exists()) {
            try {
                builder.getConfiguration();
                logger.info("Default configuration file '{}' loaded.", builder.getFileHandler().getFile().getAbsolutePath());
            } catch (ConfigurationException e) {
                logger.error(e.getMessage(), e);
            }
            builder.addEventListener(ConfigurationBuilderEvent.CONFIGURATION_REQUEST,
                    new EventListener() {

                        @Override
                        public void onEvent(Event event) {
                            if (builder.getReloadingController().checkForReloading(null)) {
                                //
                            }
                        }
                    });
        } else {
            logger.error("Default configuration file not found: {}", Paths.get(configFilePath).toAbsolutePath());
        }

        // Load local config file
        File fileLocal = new File(getConfigLocalPath() + "config_viewer.xml");
        builderLocal =
                new ReloadingFileBasedConfigurationBuilder<XMLConfiguration>(XMLConfiguration.class)
                        .configure(new Parameters().properties()
                                .setFileName(fileLocal.getAbsolutePath())
                                .setListDelimiterHandler(new DefaultListDelimiterHandler('&')) // TODO Why '&'?
                                .setThrowExceptionOnMissing(false));
        if (builder.getFileHandler().getFile().exists()) {
            try {
                builder.getConfiguration();
                logger.info("Local configuration file '{}' loaded.", fileLocal.getAbsolutePath());
            } catch (ConfigurationException e) {
                logger.error(e.getMessage(), e);
            }
            builderLocal.addEventListener(ConfigurationBuilderEvent.CONFIGURATION_REQUEST,
                    new EventListener() {

                        @Override
                        public void onEvent(Event event) {
                            if (builderLocal.getReloadingController().checkForReloading(null)) {
                                //
                            }
                        }
                    });
        }

        // Load stopwords
        try {
            stopwords = loadStopwords(getStopwordsFilePath());
        } catch (IOException | IllegalArgumentException e) {
            logger.warn(e.getMessage());
            stopwords = new HashSet<>(0);
        }
    }

    /**
     * <p>
     * loadStopwords.
     * </p>
     *
     * @param stopwordsFilePath a {@link java.lang.String} object.
     * @return a {@link java.util.Set} object.
     * @throws java.io.FileNotFoundException if any.
     * @throws java.io.IOException if any.
     * @should load all stopwords
     * @should remove parts starting with pipe
     * @should not add empty stopwords
     * @should throw IllegalArgumentException if stopwordsFilePath empty
     * @should throw FileNotFoundException if file does not exist
     */
    protected static Set<String> loadStopwords(String stopwordsFilePath) throws FileNotFoundException, IOException {
        if (StringUtils.isEmpty(stopwordsFilePath)) {
            throw new IllegalArgumentException("stopwordsFilePath may not be null or empty");
        }

        if (StringUtils.isEmpty(stopwordsFilePath)) {
            logger.warn("'stopwordsFile' not configured. Stop words cannot be filtered from search queries.");
            return Collections.emptySet();
        }

        Set<String> ret = new HashSet<>();
        try (FileReader fr = new FileReader(stopwordsFilePath); BufferedReader br = new BufferedReader(fr)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (StringUtils.isNotBlank(line)) {
                    if (line.charAt(0) != '#') {
                        int pipeIndex = line.indexOf('|');
                        if (pipeIndex != -1) {
                            line = line.substring(0, pipeIndex).trim();
                        }
                        if (!line.isEmpty() && Character.getNumericValue(line.charAt(0)) != -1) {
                            ret.add(line);
                        }
                    }
                }
            }
        }

        return ret;
    }

    /**
     * Returns the stopwords loading during initialization.
     *
     * @should return all stopwords
     * @return a {@link java.util.Set} object.
     */
    public Set<String> getStopwords() {
        return stopwords;
    }

    /**
     * <p>
     * reloadingRequired.
     * </p>
     *
     * @return a boolean.
     */
    //    public boolean reloadingRequired() {
    //        boolean ret = false;
    //        if (getConfigLocal() != null) {
    //            ret = getConfigLocal().getReloadingStrategy().reloadingRequired() || config.getReloadingStrategy().reloadingRequired();
    //        }
    //        ret = config.getReloadingStrategy().reloadingRequired();
    //        return ret;
    //    }

    /*********************************** direct config results ***************************************/

    /**
     * <p>
     * getConfigLocalPath.
     * </p>
     *
     * @return the path to the local config_viewer.xml file.
     */
    public String getConfigLocalPath() {
        String configLocalPath = getConfig().getString("configFolder", "/opt/digiverso/viewer/config/");
        if (!configLocalPath.endsWith("/")) {
            configLocalPath += "/";
        }
        configLocalPath = FileTools.adaptPathForWindows(configLocalPath);
        return configLocalPath;
    }

    /**
     * <p>
     * getLocalRessourceBundleFile.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getLocalRessourceBundleFile() {
        return getConfigLocalPath() + "messages_de.properties";
    }

    /**
     * <p>
     * getViewerThumbnailsPerPage.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getViewerThumbnailsPerPage() {
        return getLocalInt("viewer.thumbnailsPerPage", 10);
    }

    /**
     * <p>
     * getViewerMaxImageWidth.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getViewerMaxImageWidth() {
        return getLocalInt("viewer.maxImageWidth", 2000);
    }

    /**
     * <p>
     * getViewerMaxImageHeight.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getViewerMaxImageHeight() {
        return getLocalInt("viewer.maxImageHeight", 2000);
    }

    /**
     * <p>
     * getViewerMaxImageScale.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getViewerMaxImageScale() {
        return getLocalInt("viewer.maxImageScale", 500);
    }

    /**
     * <p>
     * isRememberImageZoom.
     * </p>
     *
     * @return a boolean.
     * @should return correct value
     */
    public boolean isRememberImageZoom() {
        return getLocalBoolean("viewer.rememberImageZoom[@enabled]", false);
    }

    /**
     * <p>
     * isRememberImageRotation.
     * </p>
     *
     * @return a boolean.
     * @should return correct value
     */
    public boolean isRememberImageRotation() {
        return getLocalBoolean("viewer.rememberImageRotation[@enabled]", false);
    }

    /**
     * <p>
     * getDfgViewerUrl.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getDfgViewerUrl() {
        return getLocalString("urls.dfg-viewer", "https://dfg-viewer.de/v2?set[mets]=");
    }

    /**
     * 
     * @should return correct value
     * @return Connector URL
     */
    public String getConnectorVersionUrl() {
        return getLocalString("urls.connectorVersion", "http://localhost:8080/M2M/oai/tools?action=getVersion");
    }

    /**
     * Returns the list of configured metadata for the title bar component. TODO Allow templates and then retire this method.
     *
     * @should return all configured metadata elements
     * @return a {@link java.util.List} object.
     */
    public List<Metadata> getTitleBarMetadata() {
        List<HierarchicalConfiguration<ImmutableNode>> elements = getLocalConfigurationsAt("metadata.titleBarMetadataList.metadata");
        if (elements == null) {
            return Collections.emptyList();
        }

        List<Metadata> ret = new ArrayList<>(elements.size());
        for (Iterator<HierarchicalConfiguration<ImmutableNode>> it = elements.iterator(); it.hasNext();) {
            HierarchicalConfiguration<ImmutableNode> sub = it.next();
            String label = sub.getString("[@label]");
            String masterValue = sub.getString("[@value]");
            boolean group = sub.getBoolean("[@group]", false);
            int type = sub.getInt("[@type]", 0);
            List<HierarchicalConfiguration<ImmutableNode>> params = sub.configurationsAt("param");
            List<MetadataParameter> paramList = null;
            if (params != null) {
                paramList = new ArrayList<>();
                for (Iterator<HierarchicalConfiguration<ImmutableNode>> it2 = params.iterator(); it2.hasNext();) {
                    HierarchicalConfiguration<ImmutableNode> sub2 = it2.next();
                    String fieldType = sub2.getString("[@type]");
                    String source = sub2.getString("[@source]", null);
                    String key = sub2.getString("[@key]");
                    String altKey = sub2.getString("[@altKey]");
                    String masterValueFragment = sub2.getString("[@value]");
                    String defaultValue = sub2.getString("[@defaultValue]");
                    String prefix = sub2.getString("[@prefix]", "").replace("_SPACE_", " ");
                    String suffix = sub2.getString("[@suffix]", "").replace("_SPACE_", " ");
                    String condition = sub2.getString("[@condition]");
                    boolean addUrl = sub2.getBoolean("[@url]", false);
                    boolean topstructValueFallback = sub2.getBoolean("[@topstructValueFallback]", false);
                    boolean topstructOnly = sub2.getBoolean("[@topstructOnly]", false);
                    paramList.add(new MetadataParameter().setType(MetadataParameterType.getByString(fieldType))
                            .setSource(source)
                            .setKey(key)
                            .setAltKey(altKey)
                            .setMasterValueFragment(masterValueFragment)
                            .setDefaultValue(defaultValue)
                            .setPrefix(prefix)
                            .setSuffix(suffix)
                            .setCondition(condition)
                            .setAddUrl(addUrl)
                            .setTopstructValueFallback(topstructValueFallback)
                            .setTopstructOnly(topstructOnly));
                }
            }
            ret.add(new Metadata(label, masterValue, type, paramList, group));
        }

        return ret;
    }

    /**
     * Returns the list of configured metadata for search hit elements.
     *
     * @param template a {@link java.lang.String} object.
     * @should return correct template configuration
     * @should return default template configuration if requested not found
     * @should return default template if template is null
     * @return a {@link java.util.List} object.
     */
    public List<Metadata> getSearchHitMetadataForTemplate(String template) {
        List<HierarchicalConfiguration<ImmutableNode>> templateList = getLocalConfigurationsAt("metadata.searchHitMetadataList.template");
        if (templateList == null) {
            return Collections.emptyList();
        }

        return getMetadataForTemplate(template, templateList, true, true);
    }

    /**
     * 
     * @return
     * @should return all configured values
     */
    public List<MetadataView> getMetadataViews() {
        List<HierarchicalConfiguration<ImmutableNode>> metadataPageList = getLocalConfigurationsAt("metadata.metadataView");
        if (metadataPageList == null) {
            metadataPageList = getLocalConfigurationsAt("metadata.mainMetadataList");
            if (metadataPageList != null) {
                logger.warn("Old <mainMetadataList> configuration found - please migrate to <metadataView>.");
                return Collections.singletonList(new MetadataView());
            }
            return Collections.emptyList();
        }

        List<MetadataView> ret = new ArrayList<>(metadataPageList.size());
        for (HierarchicalConfiguration<ImmutableNode> metadataView : metadataPageList) {
            int index = metadataView.getInt("[@index]", 0);
            String label = metadataView.getString("[@label]");
            String url = metadataView.getString("[@url]", "");
            String condition = metadataView.getString("[@condition]");
            MetadataView view = new MetadataView().setIndex(index).setLabel(label).setUrl(url).setCondition(condition);
            ret.add(view);
        }

        return ret;
    }

    /**
     * 
     * @param index
     * @param template
     * @return List of configured <code>Metadata</code> fields for the given template
     * @should return correct template configuration
     * @should return default template configuration if template not found
     * @should return default template if template is null
     */
    public List<Metadata> getMainMetadataForTemplate(int index, String template) {
        logger.trace("getMainMetadataForTemplate: {}", template);
        List<HierarchicalConfiguration<ImmutableNode>> templateList = getLocalConfigurationsAt("metadata.metadataView(" + index + ").template");
        if (templateList == null) {
            templateList = getLocalConfigurationsAt("metadata.metadataView.template");
            if (templateList == null) {
                templateList = getLocalConfigurationsAt("metadata.mainMetadataList.template");
                // Old configuration fallback
                if (templateList != null) {
                    logger.warn("Old <mainMetadataList> configuration found - please migrate to <metadataView>.");
                } else {
                    return Collections.emptyList();
                }
            }
        }

        return getMetadataForTemplate(template, templateList, true, false);
    }

    /**
     * Returns the list of configured metadata for the sidebar.
     *
     * @param template Template name
     * @return List of configured metadata for configured fields
     * @should return correct template configuration
     * @should return empty list if template not found
     * @should return empty list if template is null
     */
    public List<Metadata> getSidebarMetadataForTemplate(String template) {
        List<HierarchicalConfiguration<ImmutableNode>> templateList = getLocalConfigurationsAt("metadata.sideBarMetadataList.template");
        if (templateList == null) {
            return Collections.emptyList();
        }

        return getMetadataForTemplate(template, templateList, false, false);
    }

    /**
     * Reads metadata configuration for the given template name if it's contained in the given template list.
     * 
     * @param template Requested template name
     * @param templateList List of templates in which to look
     * @param fallbackToDefaultTemplate If true, the _DEFAULT template will be loaded if the given template is not found
     * @param topstructValueFallbackDefaultValue If true, the default value for the parameter attribute "topstructValueFallback" will be the value
     *            passed here
     * @return
     */
    private static List<Metadata> getMetadataForTemplate(String template, List<HierarchicalConfiguration<ImmutableNode>> templateList,
            boolean fallbackToDefaultTemplate, boolean topstructValueFallbackDefaultValue) {
        if (templateList == null) {
            return Collections.emptyList();
        }

        HierarchicalConfiguration<ImmutableNode> usingTemplate = null;
        HierarchicalConfiguration<ImmutableNode> defaultTemplate = null;
        for (Iterator<HierarchicalConfiguration<ImmutableNode>> it = templateList.iterator(); it.hasNext();) {
            HierarchicalConfiguration<ImmutableNode> subElement = it.next();
            if (subElement.getString("[@name]").equals(template)) {
                usingTemplate = subElement;
                break;
            } else if ("_DEFAULT".equals(subElement.getString("[@name]"))) {
                defaultTemplate = subElement;
            }
        }

        // If the requested template does not exist in the config, use _DEFAULT
        if (usingTemplate == null && fallbackToDefaultTemplate) {
            usingTemplate = defaultTemplate;
        }
        if (usingTemplate == null) {
            return Collections.emptyList();
        }

        return getMetadataForTemplate(usingTemplate, topstructValueFallbackDefaultValue);
    }

    /**
     * Reads metadata configuration for the given template configuration item. Returns empty list if template is null.
     * 
     * @param usingTemplate
     * @param topstructValueFallbackDefaultValue Default value for topstructValueFallback, if not explicitly configured
     * @return
     */
    private static List<Metadata> getMetadataForTemplate(HierarchicalConfiguration<ImmutableNode> usingTemplate,
            boolean topstructValueFallbackDefaultValue) {
        if (usingTemplate == null) {
            return Collections.emptyList();
        }
        //                logger.debug("template requested: " + template + ", using: " + usingTemplate.getString("[@name]"));
        List<HierarchicalConfiguration<ImmutableNode>> elements = usingTemplate.configurationsAt("metadata");
        if (elements == null) {
            logger.warn("Template '{}' contains no metadata elements.", usingTemplate.getRootElementName());
            return Collections.emptyList();
        }

        List<Metadata> ret = new ArrayList<>(elements.size());
        for (Iterator<HierarchicalConfiguration<ImmutableNode>> it = elements.iterator(); it.hasNext();) {
            HierarchicalConfiguration<ImmutableNode> sub = it.next();

            Metadata md = getMetadataFromSubnodeConfig(sub, topstructValueFallbackDefaultValue, 0);
            if (md != null) {
                ret.add(md);
            }
        }

        return ret;
    }

    /**
     * Creates a {@link Metadata} instance from the given subnode configuration
     * 
     * @param sub The subnode configuration
     * @param topstructValueFallbackDefaultValue
     * @param indentation
     * @return the resulting {@link Metadata} instance
     * @should load parameters correctly
     * @should load replace rules correctly
     * @should load child metadata configurations recursively
     */
    static Metadata getMetadataFromSubnodeConfig(HierarchicalConfiguration<ImmutableNode> sub, boolean topstructValueFallbackDefaultValue,
            int indentation) {
        if (sub == null) {
            throw new IllegalArgumentException("sub may not be null");
        }

        String label = sub.getString("[@label]");
        String masterValue = sub.getString("[@value]");
        boolean group = sub.getBoolean("[@group]", false);
        boolean singleString = sub.getBoolean("[@singleString]", true);
        int number = sub.getInt("[@number]", -1);
        int type = sub.getInt("[@type]", 0);
        boolean hideIfOnlyMetadataField = sub.getBoolean("[@hideIfOnlyMetadataField]", false);
        String citationTemplate = sub.getString("[@citationTemplate]");
        String labelField = sub.getString("[@labelField]");
        List<HierarchicalConfiguration<ImmutableNode>> params = sub.configurationsAt("param");
        List<MetadataParameter> paramList = null;
        if (params != null) {
            paramList = new ArrayList<>(params.size());
            for (Iterator<HierarchicalConfiguration<ImmutableNode>> it2 = params.iterator(); it2.hasNext();) {
                HierarchicalConfiguration<ImmutableNode> sub2 = it2.next();
                String fieldType = sub2.getString("[@type]");
                String source = sub2.getString("[@source]", null);
                String dest = sub2.getString("[@dest]", null);
                String key = sub2.getString("[@key]");
                String altKey = sub2.getString("[@altKey]");
                String masterValueFragment = sub2.getString("[@value]");
                String defaultValue = sub2.getString("[@defaultValue]");
                String prefix = sub2.getString("[@prefix]", "").replace("_SPACE_", " ");
                String suffix = sub2.getString("[@suffix]", "").replace("_SPACE_", " ");
                String condition = sub2.getString("[@condition]");
                boolean addUrl = sub2.getBoolean("[@url]", false);
                boolean topstructValueFallback = sub2.getBoolean("[@topstructValueFallback]", topstructValueFallbackDefaultValue);
                boolean topstructOnly = sub2.getBoolean("[@topstructOnly]", false);
                List<MetadataReplaceRule> replaceRules = Collections.emptyList();
                List<HierarchicalConfiguration<ImmutableNode>> replaceRuleElements = sub2.configurationsAt("replace");
                if (replaceRuleElements != null) {
                    // Replacement rules can be applied to a character, a string or a regex
                    replaceRules = new ArrayList<>(replaceRuleElements.size());
                    for (Iterator<HierarchicalConfiguration<ImmutableNode>> it3 = replaceRuleElements.iterator(); it3.hasNext();) {
                        HierarchicalConfiguration<ImmutableNode> sub3 = it3.next();
                        String replaceCondition = sub3.getString("[@condition]");
                        Character character = null;
                        try {
                            int charIndex = sub3.getInt("[@char]");
                            character = (char) charIndex;
                        } catch (NoSuchElementException e) {
                        }
                        String string = null;
                        try {
                            string = sub3.getString("[@string]");
                        } catch (NoSuchElementException e) {
                        }
                        String regex = null;
                        try {
                            regex = sub3.getString("[@regex]");
                        } catch (NoSuchElementException e) {
                        }
                        String replaceWith = sub3.getString("");
                        if (replaceWith == null) {
                            replaceWith = "";
                        }
                        if (character != null) {
                            replaceRules.add(new MetadataReplaceRule(character, replaceWith, replaceCondition, MetadataReplaceRuleType.CHAR));
                        } else if (string != null) {
                            replaceRules.add(new MetadataReplaceRule(string, replaceWith, replaceCondition, MetadataReplaceRuleType.STRING));
                        } else if (regex != null) {
                            replaceRules.add(new MetadataReplaceRule(regex, replaceWith, replaceCondition, MetadataReplaceRuleType.REGEX));
                        }
                    }
                }

                paramList.add(new MetadataParameter().setType(MetadataParameterType.getByString(fieldType))
                        .setSource(source)
                        .setDestination(dest)
                        .setKey(key)
                        .setAltKey(altKey)
                        .setMasterValueFragment(masterValueFragment)
                        .setDefaultValue(defaultValue)
                        .setPrefix(prefix)
                        .setSuffix(suffix)
                        .setCondition(condition)
                        .setAddUrl(addUrl)
                        .setTopstructValueFallback(topstructValueFallback)
                        .setTopstructOnly(topstructOnly)
                        .setReplaceRules(replaceRules));
            }
        }

        Metadata ret = new Metadata(label, masterValue, type, paramList, group, number)
                .setSingleString(singleString)
                .setHideIfOnlyMetadataField(hideIfOnlyMetadataField)
                .setCitationTemplate(citationTemplate)
                .setLabelField(labelField)
                .setIndentation(indentation);

        // Recursively add nested metadata configurations
        List<HierarchicalConfiguration<ImmutableNode>> children = sub.configurationsAt("metadata");
        if (children != null && !children.isEmpty()) {
            for (HierarchicalConfiguration<ImmutableNode> child : children) {
                Metadata childMetadata = getMetadataFromSubnodeConfig(child, topstructValueFallbackDefaultValue, indentation + 1);
                childMetadata.setParentMetadata(ret);
                ret.getChildMetadata().add(childMetadata);
            }
        }

        return ret;
    }

    /**
     * <p>
     * getNormdataFieldsForTemplate.
     * </p>
     *
     * @param template Template name
     * @return List of normdata fields configured for the given template name
     * @should return correct template configuration
     */
    public List<String> getNormdataFieldsForTemplate(String template) {
        List<HierarchicalConfiguration<ImmutableNode>> templateList = getLocalConfigurationsAt("metadata.normdataList.template");
        if (templateList == null) {
            return Collections.emptyList();
        }

        HierarchicalConfiguration<ImmutableNode> usingTemplate = null;
        //        HierarchicalConfiguration<ImmutableNode> defaultTemplate = null;
        for (Iterator<HierarchicalConfiguration<ImmutableNode>> it = templateList.iterator(); it.hasNext();) {
            HierarchicalConfiguration<ImmutableNode> subElement = it.next();
            if (subElement.getString("[@name]").equals(template)) {
                usingTemplate = subElement;
                break;
            }
        }
        if (usingTemplate == null) {
            return Collections.emptyList();
        }

        return getLocalList(usingTemplate, null, "field", null);
    }

    /**
     * <p>
     * getTocLabelConfiguration.
     * </p>
     *
     * @should return correct template configuration
     * @should return default template configuration if template not found
     * @param template a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     */
    public List<Metadata> getTocLabelConfiguration(String template) {
        List<HierarchicalConfiguration<ImmutableNode>> templateList = getLocalConfigurationsAt("toc.labelConfig.template");
        if (templateList == null) {
            return Collections.emptyList();
        }

        return getMetadataForTemplate(template, templateList, true, false);
    }

    /**
     * Returns number of elements displayed per paginator page in a table of contents for anchors and groups. Values below 1 disable pagination (all
     * elements are displayed on the single page).
     *
     * @should return correct value
     * @return a int.
     */
    public int getTocAnchorGroupElementsPerPage() {
        return getLocalInt("toc.tocAnchorGroupElementsPerPage", 0);
    }

    /**
     * <p>
     * isDisplaySidebarBrowsingTerms.
     * </p>
     *
     * @return a boolean.
     * @should return correct value
     */
    public boolean isDisplaySidebarBrowsingTerms() {
        return getLocalBoolean("sidebar.sidebarBrowsingTerms[@enabled]", true);
    }

    /**
     * <p>
     * isDisplaySidebarRssFeed.
     * </p>
     *
     * @return a boolean.
     * @should return correct value
     */
    public boolean isDisplaySidebarRssFeed() {
        return getLocalBoolean("sidebar.sidebarRssFeed[@enabled]", true);
    }

    /**
     * <p>
     * isOriginalContentDownload.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isDisplaySidebarWidgetDownloads() {
        return getLocalBoolean("sidebar.sidebarWidgetDownloads[@enabled]", false);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getSidebarWidgetDownloadsIntroductionText() {
        return getLocalString("sidebar.sidebarWidgetDownloads[@introductionText]", "");
    }

    /**
     * <p>
     * Returns a regex such that all download files which filenames fit this regex should not be visible in the downloads widget. If an empty string
     * is returned, all downloads should remain visible
     * </p>
     *
     * @return a regex or an empty string if no downloads should be hidden
     */
    public String getHideDownloadFileRegex() {
        return getLocalString("sidebar.sidebarWidgetDownloads.hideFileRegex", "");
    }

    /**
     * <p>
     * isDisplayWidgetUsage.
     * </p>
     *
     * @return a boolean.
     * @should return correct value
     */
    public boolean isDisplayWidgetUsage() {
        return getLocalBoolean("sidebar.sidebarWidgetUsage[@enabled]", true);
    }

    /**
     * 
     * @return Boolean value
     * @should return correct value
     */
    public boolean isDisplaySidebarWidgetUsageCitationRecommendation() {
        return getLocalBoolean("sidebar.sidebarWidgetUsage.citationRecommendation[@enabled]", true);
    }

    /**
     * 
     * @return List of available citation style names
     * @should return all configured values
     */
    public List<String> getSidebarWidgetUsageCitationRecommendationStyles() {
        return getLocalList("sidebar.sidebarWidgetUsage.citationRecommendation.styles.style", Collections.emptyList());
    }

    /**
     * 
     * @return
     */
    public Metadata getSidebarWidgetUsageCitationRecommendationSource() {
        HierarchicalConfiguration<ImmutableNode> sub = null;
        try {
            sub = getLocalConfigurationAt("sidebar.sidebarWidgetUsage.citationRecommendation.source.metadata");
        } catch (IllegalArgumentException e) {
            // no or multiple occurrences 
        }
        if (sub != null) {
            Metadata md = getMetadataFromSubnodeConfig(sub, false, 0);
            return md;
        }

        return new Metadata();
    }

    /**
     * 
     * @return Boolean value
     * @should return correct value
     */
    public boolean isDisplaySidebarWidgetUsageCitationLinks() {
        return getLocalBoolean("sidebar.sidebarWidgetUsage.citationLinks[@enabled]", true);
    }

    /**
     * 
     * @return String
     * @should return correct value
     */
    public String getSidebarWidgetUsageCitationLinksRecordIntroText() {
        return getLocalString("sidebar.sidebarWidgetUsage.citationLinks[@recordIntroText]", "");
    }

    /**
     * 
     * @return String
     * @should return correct value
     */
    public String getSidebarWidgetUsageCitationLinksDocstructIntroText() {
        return getLocalString("sidebar.sidebarWidgetUsage.citationLinks[@docstructIntroText]", "");
    }

    /**
     * 
     * @return String
     * @should return correct value
     */
    public String getSidebarWidgetUsageCitationLinksImageIntroText() {
        return getLocalString("sidebar.sidebarWidgetUsage.citationLinks[@imageIntroText]", "");
    }

    /**
     * 
     * @return
     * @should return all configured values
     */
    public List<CitationLink> getSidebarWidgetUsageCitationLinks() {
        List<HierarchicalConfiguration<ImmutableNode>> links = getLocalConfigurationsAt("sidebar.sidebarWidgetUsage.citationLinks.links.link");
        if (links == null || links.isEmpty()) {
            return Collections.emptyList();
        }

        List<CitationLink> ret = new ArrayList<>();
        for (Iterator<HierarchicalConfiguration<ImmutableNode>> it = links.iterator(); it.hasNext();) {
            HierarchicalConfiguration<ImmutableNode> sub = it.next();
            String type = sub.getString("[@type]");
            String level = sub.getString("[@for]");
            String label = sub.getString("[@label]");
            String field = sub.getString("[@field]");
            String prefix = sub.getString("[@prefix]");
            String suffix = sub.getString("[@suffix]");
            boolean topstructValueFallback = sub.getBoolean("[@topstructValueFallback]", false);
            boolean appendImageNumberToSuffix = sub.getBoolean("[@appendImageNumberToSuffix]", false);
            try {
                ret.add(new CitationLink(type, level, label).setField(field)
                        .setPrefix(prefix)
                        .setSuffix(suffix)
                        .setTopstructValueFallback(topstructValueFallback)
                        .setAppendImageNumberToSuffix(appendImageNumberToSuffix));
            } catch (IllegalArgumentException e) {
                logger.error(e.getMessage());
            }
        }

        return ret;
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getSidebarWidgetUsageIntroductionText() {
        return getLocalString("sidebar.sidebarWidgetUsage[@introductionText]", "");
    }

    /**
     * Returns a list of configured page download options.
     * 
     * @return List of configured <code>DownloadOption</code> items
     * @should return all configured elements
     */
    public List<DownloadOption> getSidebarWidgetUsagePageDownloadOptions() {
        List<HierarchicalConfiguration<ImmutableNode>> configs = getLocalConfigurationsAt("sidebar.sidebarWidgetUsage.page.downloadOptions.option");
        if (configs == null || configs.isEmpty()) {
            return Collections.emptyList();
        }

        List<DownloadOption> ret = new ArrayList<>(configs.size());
        for (HierarchicalConfiguration<ImmutableNode> config : configs) {
            ret.add(new DownloadOption().setLabel(config.getString("[@label]"))
                    .setFormat(config.getString("[@format]"))
                    .setBoxSizeInPixel(config.getString("[@boxSizeInPixel]")));
        }

        return ret;
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isDisplayWidgetUsageDownloadOptions() {
        return getLocalBoolean("sidebar.sidebarWidgetUsage.page.downloadOptions[@enabled]", true);
    }

    /**
     * Returns the list of structure elements allowed to be shown in calendar view
     *
     * @should return all configured elements
     * @return a {@link java.util.List} object.
     */
    public List<String> getCalendarDocStructTypes() {
        return getLocalList("metadata.calendarDocstructTypes.docStruct");
    }

    /**
     * <p>
     * isBrowsingMenuEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isBrowsingMenuEnabled() {
        return getLocalBoolean("metadata.browsingMenu[@enabled]", false);
    }

    /**
     * <p>
     * getBrowsingMenuIndexSizeThreshold.
     * </p>
     *
     * @return Solr doc count threshold for browsing term calculation
     * @should return correct value
     */
    public int getBrowsingMenuIndexSizeThreshold() {
        return getLocalInt("metadata.browsingMenu.indexSizeThreshold", 100000);
    }

    /**
     * <p>
     * getBrowsingMenuHitsPerPage.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getBrowsingMenuHitsPerPage() {
        return getLocalInt("metadata.browsingMenu.hitsPerPage", 50);
    }

    /**
     * Returns the list of index fields to be used for term browsing.
     *
     * @return a {@link java.util.List} object.
     * @should return all configured elements
     */
    public List<BrowsingMenuFieldConfig> getBrowsingMenuFields() {
        List<HierarchicalConfiguration<ImmutableNode>> fields = getLocalConfigurationsAt("metadata.browsingMenu.luceneField");
        if (fields == null) {
            return Collections.emptyList();
        }

        List<BrowsingMenuFieldConfig> ret = new ArrayList<>(fields.size());
        for (Iterator<HierarchicalConfiguration<ImmutableNode>> it = fields.iterator(); it.hasNext();) {
            HierarchicalConfiguration<ImmutableNode> sub = it.next();
            String field = sub.getString(".");
            String sortField = sub.getString("[@sortField]");
            String filterQuery = sub.getString("[@filterQuery]");
            boolean translate = sub.getBoolean("[@translate]", false);
            boolean recordsAndAnchorsOnly = sub.getBoolean("[@recordsAndAnchorsOnly]", false);
            boolean alwaysApplyFilter = sub.getBoolean("[@alwaysApplyFilter]", false);
            BrowsingMenuFieldConfig bmfc =
                    new BrowsingMenuFieldConfig(field, sortField, filterQuery, translate, recordsAndAnchorsOnly, alwaysApplyFilter);
            ret.add(bmfc);
        }

        return ret;
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getBrowsingMenuSortingIgnoreLeadingChars() {
        return getLocalString("metadata.browsingMenu.sorting.ignoreLeadingChars");
    }

    /**
     * <p>
     * getDocstrctWhitelistFilterQuery.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getDocstrctWhitelistFilterQuery() {
        return getLocalString("search.docstrctWhitelistFilterQuery", SearchHelper.DEFAULT_DOCSTRCT_WHITELIST_FILTER_QUERY);
    }

    /**
     * <p>
     * getCollectionSplittingChar.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getCollectionSplittingChar(String field) {
        HierarchicalConfiguration<ImmutableNode> subConfig = getCollectionConfiguration(field);
        if (subConfig != null) {
            return subConfig.getString("splittingCharacter", ".");
        }

        return getLocalString("collections.splittingCharacter", ".");
    }

    /**
     * Returns the collection config block for the given field.
     *
     * @param field
     * @return
     */
    private HierarchicalConfiguration<ImmutableNode> getCollectionConfiguration(String field) {
        List<HierarchicalConfiguration<ImmutableNode>> collectionList = getLocalConfigurationsAt("collections.collection");
        if (collectionList == null) {
            return null;
        }

        for (Iterator<HierarchicalConfiguration<ImmutableNode>> it = collectionList.iterator(); it.hasNext();) {
            HierarchicalConfiguration<ImmutableNode> subElement = it.next();
            if (subElement.getString("[@field]").equals(field)) {
                return subElement;

            }
        }

        return null;
    }

    public List<String> getConfiguredCollectionFields() {
        List<String> list = getLocalList("collections.collection[@field]");
        if (list == null || list.isEmpty()) {
            return Collections.singletonList("DC");
        }

        return list;
    }

    /**
     * <p>
     * getCollectionSorting.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @should return all configured elements
     * @return a {@link java.util.List} object.
     */
    public List<DcSortingList> getCollectionSorting(String field) {
        List<DcSortingList> superlist = new ArrayList<>();
        HierarchicalConfiguration<ImmutableNode> collection = getCollectionConfiguration(field);
        if (collection == null) {
            return superlist;
        }

        superlist.add(new DcSortingList(getLocalList("sorting.collection")));
        List<HierarchicalConfiguration<ImmutableNode>> listConfigs = collection.configurationsAt("sorting.sortingList");
        for (HierarchicalConfiguration<ImmutableNode> listConfig : listConfigs) {
            String sortAfter = listConfig.getString("[@sortAfter]", null);
            List<String> collectionList = getLocalList(listConfig, null, "collection", Collections.<String> emptyList());
            superlist.add(new DcSortingList(sortAfter, collectionList));
        }
        return superlist;
    }

    /**
     * Returns collection names to be omitted from search results, listings etc.
     *
     * @param field a {@link java.lang.String} object.
     * @should return all configured elements
     * @return a {@link java.util.List} object.
     */
    public List<String> getCollectionBlacklist(String field) {
        HierarchicalConfiguration<ImmutableNode> collection = getCollectionConfiguration(field);
        if (collection == null) {
            return null;
        }
        return getLocalList(collection, null, "blacklist.collection", Collections.<String> emptyList());
    }

    /**
     * Returns the index field by which records in the collection with the given name are to be sorted in a listing.
     *
     * @param field a {@link java.lang.String} object.
     * @param name a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     * @should return correct field for collection
     * @should give priority to exact matches
     * @should return hyphen if collection not found
     */
    public String getCollectionDefaultSortField(String field, String name) {
        HierarchicalConfiguration<ImmutableNode> collection = getCollectionConfiguration(field);
        if (collection == null) {
            return "-";
        }

        List<HierarchicalConfiguration<ImmutableNode>> fields = collection.configurationsAt("defaultSortFields.field");
        if (fields == null) {
            return "-";
        }

        String exactMatch = null;
        String inheritedMatch = null;
        for (Iterator<HierarchicalConfiguration<ImmutableNode>> it = fields.iterator(); it.hasNext();) {
            HierarchicalConfiguration<ImmutableNode> sub = it.next();
            String key = sub.getString("[@collection]");
            if (name.equals(key)) {
                exactMatch = sub.getString("");
            } else if (key.endsWith("*") && name.startsWith(key.substring(0, key.length() - 1))) {
                inheritedMatch = sub.getString("");
            }
        }
        // Exact match is given priority so that it is possible to override the inherited sort field
        if (StringUtils.isNotEmpty(exactMatch)) {
            return exactMatch;
        }
        if (StringUtils.isNotEmpty(inheritedMatch)) {
            return inheritedMatch;
        }

        return "-";
    }

    /**
     * <p>
     * getCollectionDisplayNumberOfVolumesLevel.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @should return correct value
     * @return a int.
     */
    public int getCollectionDisplayNumberOfVolumesLevel(String field) {
        HierarchicalConfiguration<ImmutableNode> collection = getCollectionConfiguration(field);
        if (collection == null) {
            return 0;
        }
        return collection.getInt("displayNumberOfVolumesLevel", 0);
    }

    /**
     * <p>
     * getCollectionDisplayDepthForSearch.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @should return correct value
     * @should return -1 if no collection config was found
     * @return a int.
     */
    public int getCollectionDisplayDepthForSearch(String field) {
        HierarchicalConfiguration<ImmutableNode> collection = getCollectionConfiguration(field);
        if (collection == null) {
            return -1;
        }
        return collection.getInt("displayDepthForSearch", -1);
    }

    /**
     * <p>
     * getCollectionHierarchyField.
     * </p>
     *
     * @should return first field where hierarchy enabled
     * @return a {@link java.lang.String} object.
     */
    public String getCollectionHierarchyField() {
        for (String field : getConfiguredCollections()) {
            if (isAddCollectionHierarchyToBreadcrumbs(field)) {
                return field;
            }
        }

        return null;
    }

    /**
     * <p>
     * isAddCollectionHierarchyToBreadcrumbs.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @should return correct value
     * @should return false if no collection config was found
     * @return a boolean.
     */
    public boolean isAddCollectionHierarchyToBreadcrumbs(String field) {
        HierarchicalConfiguration<ImmutableNode> collection = getCollectionConfiguration(field);
        if (collection == null) {
            return false;
        }
        return collection.getBoolean("addHierarchyToBreadcrumbs", false);
    }

    /**
     * <p>
     * getSolrUrl.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getSolrUrl() {
        String value = getLocalString("urls.solr", "http://localhost:8089/solr");
        if (value.charAt(value.length() - 1) == '/') {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    /**
     * <p>
     * getContentServerWrapperUrl.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getContentServerWrapperUrl() {
        return getLocalString("urls.contentServerWrapper");
    }

    /**
     * <p>
     * getDownloadUrl.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getDownloadUrl() {
        String urlString = getLocalString("urls.download", "http://localhost:8080/viewer/download/");
        if (!urlString.endsWith("/")) {
            urlString = urlString + "/";
        }
        if (!urlString.endsWith("download/")) {
            urlString = urlString + "download/";
        }
        return urlString;
    }

    /**
     * <p>
     * getRestApiUrl.
     * </p>
     *
     * @return The url to the viewer rest api as configured in the config_viewer. The url always ends with "/"
     */
    public String getRestApiUrl() {
        String urlString = getLocalString("urls.rest");
        if (urlString == null) {
            urlString = "localhost:8080/default-viewer/rest";
        }

        if (!urlString.endsWith("/")) {
            urlString += "/";
        }

        return urlString;
    }

    /**
     * url to rest api url for record media files. Always ends with a slash
     * 
     * @return
     */
    public String getIIIFApiUrl() {
        String urlString = getLocalString("urls.iiif", getRestApiUrl());
        if (!urlString.endsWith("/")) {
            urlString += "/";
        }
        return urlString;
    }

    public boolean isUseIIIFApiUrlForCmsMediaUrls() {
        boolean use = getLocalBoolean("urls.iiif[@useForCmsMedia]", true);
        return use;
    }

    /**
     * <p>
     * getContentServerRealUrl.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getContentServerRealUrl() {
        return getLocalString("urls.contentServer");
    }

    /**
     * <p>
     * getMetsUrl.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getMetsUrl() {
        return getLocalString("urls.metadata.mets");
    }

    /**
     * <p>
     * getMarcUrl.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getMarcUrl() {
        return getLocalString("urls.metadata.marc");
    }

    /**
     * <p>
     * getDcUrl.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getDcUrl() {
        return getLocalString("urls.metadata.dc");
    }

    /**
     * <p>
     * getEseUrl.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getEseUrl() {
        return getLocalString("urls.metadata.ese");
    }

    /**
     * <p>
     * getSearchHitsPerPageValues.
     * </p>
     *
     * @should return all values
     * @return List of configured values
     */
    public List<Integer> getSearchHitsPerPageValues() {
        List<String> values = getLocalList("search.hitsPerPage.value");
        if (values.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> ret = new ArrayList<>(values.size());
        for (String value : values) {
            try {
                ret.add(Integer.valueOf(value));
            } catch (NumberFormatException e) {
                logger.error("Configured hits per page value not a number: {}", value);
            }
        }

        return ret;
    }

    /**
     * <p>
     * getSearchHitsPerPageDefaultValue.
     * </p>
     *
     * @should return correct value
     * @return value element that is marked as default value; 10 if none found
     */
    public int getSearchHitsPerPageDefaultValue() {
        List<HierarchicalConfiguration<ImmutableNode>> values = getLocalConfigurationsAt("search.hitsPerPage.value");
        if (values.isEmpty()) {
            return 10;
        }
        for (Iterator<HierarchicalConfiguration<ImmutableNode>> it = values.iterator(); it.hasNext();) {
            HierarchicalConfiguration<ImmutableNode> sub = it.next();
            if (sub.getBoolean("[@default]", false)) {
                return sub.getInt(".");
            }
        }

        return 10;
    }

    /**
     * <p>
     * getFulltextFragmentLength.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getFulltextFragmentLength() {
        return getLocalInt("search.fulltextFragmentLength", 200);
    }

    /**
     * <p>
     * isAdvancedSearchEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isAdvancedSearchEnabled() {
        return getLocalBoolean("search.advanced[@enabled]", true);
    }

    /**
     * <p>
     * getAdvancedSearchDefaultItemNumber.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getAdvancedSearchDefaultItemNumber() {
        return getLocalInt("search.advanced.defaultItemNumber", 2);
    }

    /**
     * <p>
     * getAdvancedSearchFields.
     * </p>
     *
     * @should return all values
     * @return a {@link java.util.List} object.
     */
    public List<AdvancedSearchFieldConfiguration> getAdvancedSearchFields() {
        List<HierarchicalConfiguration<ImmutableNode>> fieldList = getLocalConfigurationsAt("search.advanced.searchFields.field");
        if (fieldList == null) {
            return Collections.emptyList();
        }

        List<AdvancedSearchFieldConfiguration> ret = new ArrayList<>(fieldList.size());
        for (Iterator<HierarchicalConfiguration<ImmutableNode>> it = fieldList.iterator(); it.hasNext();) {
            HierarchicalConfiguration<ImmutableNode> subElement = it.next();
            String field = subElement.getString(".");
            if (StringUtils.isEmpty(field)) {
                logger.warn("No advanced search field name defined, skipping.");
                continue;
            }
            String label = subElement.getString("[@label]", null);
            boolean hierarchical = subElement.getBoolean("[@hierarchical]", false);
            boolean range = subElement.getBoolean("[@range]", false);
            boolean untokenizeForPhraseSearch = subElement.getBoolean("[@untokenizeForPhraseSearch]", false);

            ret.add(new AdvancedSearchFieldConfiguration(field)
                    .setLabel(label)
                    .setHierarchical(hierarchical)
                    .setRange(range)
                    .setUntokenizeForPhraseSearch(untokenizeForPhraseSearch)
                    .setDisabled(field.charAt(0) == '#' && field.charAt(field.length() - 1) == '#'));
        }

        return ret;
    }

    /**
     * <p>
     * isAggregateHits.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isAggregateHits() {
        return getLocalBoolean("search.aggregateHits", true);
    }

    /**
     * <p>
     * isDisplayAdditionalMetadataEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isDisplayAdditionalMetadataEnabled() {
        return getLocalBoolean("search.displayAdditionalMetadata[@enabled]", true);
    }

    /**
     * <p>
     * getDisplayAdditionalMetadataIgnoreFields.
     * </p>
     *
     * @return List of configured fields; empty list if none found.
     * @should return correct values
     */
    public List<String> getDisplayAdditionalMetadataIgnoreFields() {
        return getLocalList("search.displayAdditionalMetadata.ignoreField", Collections.emptyList());
    }

    /**
     * <p>
     * Returns a list of additional metadata fields thats are configured to have their values translated. Field names are normalized (i.e. things like
     * _UNTOKENIZED are removed).
     * </p>
     *
     * @return List of configured fields; empty list if none found.
     * @should return correct values
     */
    public List<String> getDisplayAdditionalMetadataTranslateFields() {
        List<String> fields = getLocalList("search.displayAdditionalMetadata.translateField", Collections.emptyList());
        if (fields.isEmpty()) {
            return fields;
        }

        List<String> ret = new ArrayList<>(fields.size());
        for (String field : fields) {
            ret.add(SearchHelper.normalizeField(field));
        }

        return ret;
    }

    /**
     * <p>
     * isAdvancedSearchFieldHierarchical.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @return a boolean.
     * @should return correct value
     */
    public boolean isAdvancedSearchFieldHierarchical(String field) {
        return isAdvancedSearchFieldHasAttribute(field, "hierarchical");
    }

    /**
     * <p>
     * isAdvancedSearchFieldRange.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @return a boolean.
     * @should return correct value
     */
    public boolean isAdvancedSearchFieldRange(String field) {
        return isAdvancedSearchFieldHasAttribute(field, "range");
    }

    /**
     * <p>
     * isAdvancedSearchFieldUntokenizeForPhraseSearch.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @return a boolean.
     * @should return correct value
     */
    public boolean isAdvancedSearchFieldUntokenizeForPhraseSearch(String field) {
        return isAdvancedSearchFieldHasAttribute(field, "untokenizeForPhraseSearch");
    }

    /**
     * <p>
     * isAdvancedSearchFieldHierarchical.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @return Label attribute value for the given field name
     * @should return correct value
     */
    public String getAdvancedSearchFieldSeparatorLabel(String field) {
        List<HierarchicalConfiguration<ImmutableNode>> fieldList = getLocalConfigurationsAt("search.advanced.searchFields.field");
        if (fieldList == null) {
            return null;
        }

        for (Iterator<HierarchicalConfiguration<ImmutableNode>> it = fieldList.iterator(); it.hasNext();) {
            HierarchicalConfiguration<ImmutableNode> subElement = it.next();
            if (subElement.getString(".").equals(field)) {
                return subElement.getString("[@label]", "");
            }
        }

        return null;
    }

    /**
     * 
     * @param field Advanced search field name
     * @param attribute Attribute name
     * @return
     */
    boolean isAdvancedSearchFieldHasAttribute(String field, String attribute) {
        List<HierarchicalConfiguration<ImmutableNode>> fieldList = getLocalConfigurationsAt("search.advanced.searchFields.field");
        if (fieldList == null) {
            return false;
        }

        for (Iterator<HierarchicalConfiguration<ImmutableNode>> it = fieldList.iterator(); it.hasNext();) {
            HierarchicalConfiguration<ImmutableNode> subElement = it.next();
            if (subElement.getString(".").equals(field)) {
                return subElement.getBoolean("[@" + attribute + "]", false);
            }
        }

        return false;
    }

    /**
     * <p>
     * isTimelineSearchEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isTimelineSearchEnabled() {
        return getLocalBoolean("search.timeline[@enabled]", true);
    }

    /**
     * <p>
     * isCalendarSearchEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isCalendarSearchEnabled() {
        return getLocalBoolean("search.calendar[@enabled]", true);
    }

    /**
     * <p>
     * getStaticQuerySuffix.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getStaticQuerySuffix() {
        return getLocalString("search.staticQuerySuffix");
    }

    /**
     * <p>
     * getPreviousVersionIdentifierField.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getPreviousVersionIdentifierField() {
        return getLocalString("search.versioning.previousVersionIdentifierField");
    }

    /**
     * <p>
     * getNextVersionIdentifierField.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getNextVersionIdentifierField() {
        return getLocalString("search.versioning.nextVersionIdentifierField");
    }

    /**
     * <p>
     * getVersionLabelField.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getVersionLabelField() {
        return getLocalString("search.versioning.versionLabelField");
    }

    /**
     * <p>
     * getIndexedMetsFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getIndexedMetsFolder() {
        return getLocalString("indexedMetsFolder", "indexed_mets");
    }

    /**
     * <p>
     * getIndexedLidoFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getIndexedLidoFolder() {
        return getLocalString("indexedLidoFolder", "indexed_lido");
    }

    /**
     * <p>
     * getIndexedDenkxwebFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getIndexedDenkxwebFolder() {
        return getLocalString("indexedDenkxwebFolder", "indexed_denkxweb");
    }

    /**
     * <p>
     * getIndexedDublinCoreFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getIndexedDublinCoreFolder() {
        return getLocalString("indexedDublinCoreFolder", "indexed_dublincore");
    }

    /**
     * <p>
     * getPageSelectionFormat.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getPageSelectionFormat() {
        return getLocalString("viewer.pageSelectionFormat", "{pageno}:{pagenolabel}");
    }

    /**
     * <p>
     * getMediaFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getMediaFolder() {
        return getLocalString("mediaFolder");
    }

    /**
     * <p>
     * getPdfFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getPdfFolder() {
        return getLocalString("pdfFolder", "pdf");
    }

    /**
     * <p>
     * getVocabulariesFolder.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getVocabulariesFolder() {
        return getLocalString("vocabularies", "vocabularies");
    }

    /**
     * <p>
     * getOrigContentFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getOrigContentFolder() {
        return getLocalString("origContentFolder", "source");
    }

    /**
     * <p>
     * getCmsTextFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getCmsTextFolder() {
        return getLocalString("cmsTextFolder");
    }

    /**
     * <p>
     * getAltoFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getAltoFolder() {
        return getLocalString("altoFolder", "alto");
    }

    /**
     * <p>
     * getAltoCrowdsourcingFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getAltoCrowdsourcingFolder() {
        return getLocalString("altoCrowdsourcingFolder", "alto_crowd");
    }

    /**
     * <p>
     * getAbbyyFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getAbbyyFolder() {
        return getLocalString("abbyyFolder", "abbyy");
    }

    /**
     * <p>
     * getFulltextFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getFulltextFolder() {
        return getLocalString("fulltextFolder", "fulltext");
    }

    /**
     * <p>
     * getFulltextCrowdsourcingFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getFulltextCrowdsourcingFolder() {
        return getLocalString("fulltextCrowdsourcingFolder", "fulltext_crowd");
    }

    /**
     * <p>
     * getTeiFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getTeiFolder() {
        return getLocalString("teiFolder", "tei");
    }

    /**
     * <p>
     * getCmdiFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getCmdiFolder() {
        return getLocalString("cmdiFolder", "cmdi");
    }

    /**
     * <p>
     * getAnnotationFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getAnnotationFolder() {
        return getLocalString("annotationFolder");
    }

    /**
     * <p>
     * getHotfolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getHotfolder() {
        return getLocalString("hotfolder");
    }

    /**
     * <p>
     * getTempFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    @SuppressWarnings("static-method")
    public String getTempFolder() {
        return Paths.get(System.getProperty("java.io.tmpdir"), "viewer").toString();
    }

    /**
     * <p>
     * isUrnDoRedirect.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isUrnDoRedirect() {
        return getLocalBoolean("urnresolver.doRedirectInsteadofForward", false);
    }

    /**
     * <p>
     * isUserRegistrationEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isUserRegistrationEnabled() {
        return getLocalBoolean("user.registration[@enabled]", true);
    }

    /**
     * 
     * @return
     * @should return all configured elements
     */
    public List<SecurityQuestion> getSecurityQuestions() {
        List<HierarchicalConfiguration<ImmutableNode>> nodes = getLocalConfigurationsAt("user.securityQuestions.question");
        if (nodes == null || nodes.isEmpty()) {
            return Collections.emptyList();
        }

        List<SecurityQuestion> ret = new ArrayList<>(nodes.size());
        for (HierarchicalConfiguration<ImmutableNode> node : nodes) {
            String questionKey = node.getString("[@key]");
            if (StringUtils.isEmpty(questionKey)) {
                logger.warn("Security question key not found, skipping...");
                continue;
            }
            List<Object> answerNodes = node.getList("allowedAnswer", Collections.emptyList());
            if (answerNodes.isEmpty()) {
                logger.warn("Security question '{}' has no configured answers, skipping...");
                continue;
            }
            Set<String> allowedAnswers = new HashSet<>(answerNodes.size());
            for (Object answer : answerNodes) {
                allowedAnswers.add(((String) answer).toLowerCase());
            }
            ret.add(new SecurityQuestion(questionKey, allowedAnswers));
        }

        return ret;
    }

    /**
     * <p>
     * isShowOpenIdConnect.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isShowOpenIdConnect() {
        return getAuthenticationProviders().stream().anyMatch(provider -> OpenIdProvider.TYPE_OPENID.equalsIgnoreCase(provider.getType()));
    }

    /**
     * <p>
     * getAuthenticationProviders.
     * </p>
     *
     * @should return all properly configured elements
     * @should load user group names correctly
     * @return a {@link java.util.List} object.
     */
    public List<IAuthenticationProvider> getAuthenticationProviders() {
        XMLConfiguration myConfigToUse = getConfig();
        // User local config, if available
        if (!getConfigLocal().configurationsAt("user.authenticationProviders").isEmpty()) {
            myConfigToUse = getConfigLocal();
        }

        int max = myConfigToUse.getMaxIndex("user.authenticationProviders.provider");
        List<IAuthenticationProvider> providers = new ArrayList<>(max + 1);
        for (int i = 0; i <= max; i++) {
            String label = myConfigToUse.getString("user.authenticationProviders.provider(" + i + ")[@label]");
            String name = myConfigToUse.getString("user.authenticationProviders.provider(" + i + ")[@name]");
            String endpoint = myConfigToUse.getString("user.authenticationProviders.provider(" + i + ")[@endpoint]", null);
            String image = myConfigToUse.getString("user.authenticationProviders.provider(" + i + ")[@image]", null);
            String type = myConfigToUse.getString("user.authenticationProviders.provider(" + i + ")[@type]", "");
            boolean visible = myConfigToUse.getBoolean("user.authenticationProviders.provider(" + i + ")[@enabled]", true);
            String clientId = myConfigToUse.getString("user.authenticationProviders.provider(" + i + ")[@clientId]", null);
            String clientSecret = myConfigToUse.getString("user.authenticationProviders.provider(" + i + ")[@clientSecret]", null);
            String idpMetadataUrl = myConfigToUse.getString("user.authenticationProviders.provider(" + i + ")[@idpMetadataUrl]", null);
            String relyingPartyIdentifier =
                    myConfigToUse.getString("user.authenticationProviders.provider(" + i + ")[@relyingPartyIdentifier]", null);
            String samlPublicKeyPath = myConfigToUse.getString("user.authenticationProviders.provider(" + i + ")[@publicKeyPath]", null);
            String samlPrivateKeyPath = myConfigToUse.getString("user.authenticationProviders.provider(" + i + ")[@privateKeyPath]", null);
            long timeoutMillis = myConfigToUse.getLong("user.authenticationProviders.provider(" + i + ")[@timeout]", 60000);

            if (visible) {
                IAuthenticationProvider provider = null;
                switch (type.toLowerCase()) {
                    case "saml":
                        providers.add(
                                new SAMLProvider(name, idpMetadataUrl, relyingPartyIdentifier, samlPublicKeyPath, samlPrivateKeyPath, timeoutMillis));
                        break;
                    case "openid":
                        providers.add(new OpenIdProvider(name, label, endpoint, image, timeoutMillis, clientId, clientSecret));
                        break;
                    case "userpassword":
                        switch (name.toLowerCase()) {
                            case "vufind":
                                provider = new VuFindProvider(name, label, endpoint, image, timeoutMillis);
                                break;
                            case "x-service":
                            case "xservice":
                                provider = new XServiceProvider(name, label, endpoint, image, timeoutMillis);
                                break;
                            case "littera":
                                provider = new LitteraProvider(name, label, endpoint, image, timeoutMillis);
                                break;
                            case "bibliotheca":
                                provider = new BibliothecaProvider(name, label, endpoint, image, timeoutMillis);
                                break;
                            default:
                                logger.error("Cannot add userpassword authentification provider with name {}. No implementation found", name);
                        }
                        break;
                    case "local":
                        provider = new LocalAuthenticationProvider(name);
                        break;
                    default:
                        logger.error("Cannot add authentification provider with name {} and type {}. No implementation found", name, type);
                }
                if (provider != null) {
                    // Look for user group configurations to which users shall be automatically added when logging in
                    List<String> addToUserGroupList =
                            getLocalList(myConfigToUse, null, "user.authenticationProviders.provider(" + i + ").addUserToGroup", null);
                    if (addToUserGroupList != null) {
                        provider.setAddUserToGroups(addToUserGroupList);
                        // logger.trace("{}: add to group: {}", provider.getName(), addToUserGroupList.toString());
                    }
                    providers.add(provider);
                }
            }
        }
        return providers;
    }

    /**
     * <p>
     * getSmtpServer.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getSmtpServer() {
        return getLocalString("user.smtpServer");
    }

    /**
     * <p>
     * getSmtpUser.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getSmtpUser() {
        return getLocalString("user.smtpUser");
    }

    /**
     * <p>
     * getSmtpPassword.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getSmtpPassword() {
        return getLocalString("user.smtpPassword");
    }

    /**
     * <p>
     * getSmtpSenderAddress.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getSmtpSenderAddress() {
        return getLocalString("user.smtpSenderAddress");
    }

    /**
     * <p>
     * getSmtpSenderName.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getSmtpSenderName() {
        return getLocalString("user.smtpSenderName");
    }

    /**
     * <p>
     * getSmtpSecurity.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getSmtpSecurity() {
        return getLocalString("user.smtpSecurity", "none");
    }

    /**
     * 
     * @return Configured SMTP port number; -1 if not configured
     * @should return correct value
     */
    public int getSmtpPort() {
        return getLocalInt("user.smtpPort", -1);
    }

    /**
     * <p>
     * getAnonymousUserEmailAddress.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getAnonymousUserEmailAddress() {
        return getLocalString("user.anonymousUserEmailAddress");
    }

    /**
     * <p>
     * isDisplayCollectionBrowsing.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isDisplayCollectionBrowsing() {
        return this.getLocalBoolean("webGuiDisplay.collectionBrowsing", true);
    }

    /**
     * <p>
     * isDisplayUserNavigation.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isDisplayUserNavigation() {
        return this.getLocalBoolean("webGuiDisplay.userAccountNavigation", true);
    }

    /**
     * <p>
     * isDisplayTagCloudNavigation.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isDisplayTagCloudNavigation() {
        return this.getLocalBoolean("webGuiDisplay.displayTagCloudNavigation", true);
    }

    /**
     * <p>
     * isDisplayStatistics.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isDisplayStatistics() {
        return this.getLocalBoolean("webGuiDisplay.displayStatistics", true);
    }

    /**
     * <p>
     * isDisplayTimeMatrix.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isDisplayTimeMatrix() {
        return this.getLocalBoolean("webGuiDisplay.displayTimeMatrix", false);
    }

    /**
     * <p>
     * isDisplayCrowdsourcingModuleLinks.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isDisplayCrowdsourcingModuleLinks() {
        return this.getLocalBoolean("webGuiDisplay.displayCrowdsourcingModuleLinks", false);
    }

    /**
     * <p>
     * getTheme.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getTheme() {
        return getSubthemeMainTheme();
    }

    /**
     * <p>
     * getThemeRootPath.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getThemeRootPath() {
        return getLocalString("viewer.theme.rootPath");
    }

    /**
     * <p>
     * getName.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getName() {
        return getLocalString("viewer.name", "Goobi viewer");
    }

    /**
     * <p>
     * getDescription.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getDescription() {
        return getLocalString("viewer.description", "Goobi viewer");
    }

    /**
     * TagCloud auf der Startseite anzeigen lassen
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isDisplayTagCloudStartpage() {
        return this.getLocalBoolean("webGuiDisplay.displayTagCloudStartpage", true);
    }

    /**
     * <p>
     * isDisplaySearchResultNavigation.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isDisplaySearchResultNavigation() {
        return this.getLocalBoolean("webGuiDisplay.displaySearchResultNavigation", true);
    }

    /**
     * <p>
     * isFoldout.
     * </p>
     *
     * @param sidebarElement a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean isFoldout(String sidebarElement) {
        return getLocalBoolean("sidebar." + sidebarElement + ".foldout", false);
    }

    /**
     * <p>
     * isSidebarPageLinkVisible.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSidebarPageViewLinkVisible() {
        return getLocalBoolean("sidebar.page[@enabled]", true);
    }

    /**
     * <p>
     * isSidebarCalendarViewLinkVisible.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSidebarCalendarViewLinkVisible() {
        return getLocalBoolean("sidebar.calendar[@enabled]", true);
    }

    /**
     * <p>
     * This method checks whether the TOC <strong>link</strong> in the sidebar views widget is enabled. To check whether the sidebar TOC
     * <strong>widget</strong> is enabled, use <code>isSidebarTocVisible()</code>.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSidebarTocViewLinkVisible() {
        return getLocalBoolean("sidebar.toc[@enabled]", true);
    }

    /**
     * <p>
     * isSidebarThumbsViewLinkVisible.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSidebarThumbsViewLinkVisible() {
        return getLocalBoolean("sidebar.thumbs[@enabled]", true);
    }

    /**
     * <p>
     * isSidebarMetadataViewLinkVisible.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSidebarMetadataViewLinkVisible() {
        return getLocalBoolean("sidebar.metadata[@enabled]", true);
    }

    /**
     * <p>
     * isShowSidebarEventMetadata.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isShowSidebarEventMetadata() {
        return getLocalBoolean("sidebar.metadata.showEventMetadata", true);
    }

    /**
     * <p>
     * isShowSidebarEventMetadata.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isShowRecordLabelIfNoOtherViews() {
        return getLocalBoolean("sidebar.metadata.showRecordLabelIfNoOtherViews", false);
    }

    /**
     * <p>
     * isSidebarFulltextLinkVisible.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSidebarFulltextLinkVisible() {
        return getLocalBoolean("sidebar.fulltext[@enabled]", true);
    }

    /**
     * <p>
     * This method checks whether the TOC <strong>widget</strong> is enabled. To check whether the sidebar TOC <strong>link</strong> in the views
     * widget is enabled, use <code>isSidebarTocVisible()</code>.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSidebarTocWidgetVisible() {
        return this.getLocalBoolean("sidebar.sidebarToc[@enabled]", true);
    }

    /**
     * <p>
     * This method checks whether the TOC <strong>widget</strong> is enabled. To check whether the sidebar TOC <strong>link</strong> in the views
     * widget is enabled, use <code>isSidebarTocVisible()</code>.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSidebarTocWidgetVisibleInFullscreen() {
        return this.getLocalBoolean("sidebar.sidebarToc.visibleInFullscreen", true);
    }

    /**
     * <p>
     * isSidebarOpacLinkVisible.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSidebarOpacLinkVisible() {
        return this.getLocalBoolean("sidebar.opac[@enabled]", false);
    }

    /**
     * <p>
     * getSidebarTocPageNumbersVisible.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean getSidebarTocPageNumbersVisible() {
        return this.getLocalBoolean("sidebar.sidebarToc.pageNumbersVisible", false);
    }

    /**
     * <p>
     * getSidebarTocLengthBeforeCut.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getSidebarTocLengthBeforeCut() {
        return this.getLocalInt("sidebar.sidebarToc.lengthBeforeCut", 10);
    }

    /**
     * <p>
     * getSidebarTocInitialCollapseLevel.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getSidebarTocInitialCollapseLevel() {
        return this.getLocalInt("sidebar.sidebarToc.initialCollapseLevel", 2);
    }

    /**
     * <p>
     * getSidebarTocCollapseLengthThreshold.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getSidebarTocCollapseLengthThreshold() {
        return this.getLocalInt("sidebar.sidebarToc.collapseLengthThreshold", 10);
    }

    /**
     * <p>
     * getSidebarTocLowestLevelToCollapseForLength.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getSidebarTocLowestLevelToCollapseForLength() {
        return this.getLocalInt("sidebar.sidebarToc.collapseLengthThreshold[@lowestLevelToTest]", 2);
    }

    /**
     * <p>
     * isSidebarTocTreeView.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSidebarTocTreeView() {
        return getLocalBoolean("sidebar.sidebarToc.useTreeView", true);
    }

    /**
     * <p>
     * isTocTreeView.
     * </p>
     *
     * @should return true for allowed docstructs
     * @should return false for other docstructs
     * @param docStructType a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean isTocTreeView(String docStructType) {
        List<HierarchicalConfiguration<ImmutableNode>> hcList = getLocalConfigurationsAt("toc.useTreeView");
        if (hcList == null || hcList.isEmpty()) {
            return false;
        }
        HierarchicalConfiguration<ImmutableNode> hc = hcList.get(0);
        String docStructTypes = hc.getString("[@showDocStructs]");
        boolean allowed = hc.getBoolean(".");
        if (!allowed) {
            logger.trace("Tree view disabled");
            return false;
        }

        if (docStructTypes != null) {
            String[] docStructTypesSplit = docStructTypes.split(";");
            for (String dst : docStructTypesSplit) {
                if (dst.equals("_ALL") || dst.equals(docStructType)) {
                    logger.trace("Tree view for {} allowed", docStructType);
                    return true;
                }
            }

        }

        // logger.trace("Tree view for {} not allowed", docStructType);
        return false;
    }

    /**
     * Returns the names of all configured facet fields in the order they appear in the list, no matter whether they're regular or hierarchical.
     *
     * @return List of regular and hierarchical fields in the order in which they appear in the config file
     * @should return correct order
     */
    public List<String> getAllFacetFields() {
        List<HierarchicalConfiguration<ImmutableNode>> facets = getLocalConfigurationsAt("search.facets");
        if (facets == null || facets.isEmpty()) {
            getLocalConfigurationAt("search.drillDown");
            logger.warn("Old configuration found: search.drillDown; please update to search.facets");
        }
        if (facets == null || facets.isEmpty()) {
            logger.warn("Config element not found: search.facets");
            return Collections.emptyList();
        }
        List<HierarchicalConfiguration<ImmutableNode>> nodes =
                facets.get(0).childConfigurationsAt("");
        if (nodes.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> ret = new ArrayList<>(nodes.size());
        for (HierarchicalConfiguration<ImmutableNode> node : nodes) {
            switch (node.getRootElementName()) {
                case "field":
                case "hierarchicalField":
                case "geoField":
                    ret.add(node.getString("."));
                    break;
            }
        }

        return ret;
    }

    /**
     * <p>
     * Returns a list containing all simple facet fields.
     * </p>
     *
     * @should return all values
     * @return a {@link java.util.List} object.
     */
    public List<String> getFacetFields() {
        return getLocalList("search.facets.field");
    }

    /**
     * <p>
     * getHierarchicalFacetFields.
     * </p>
     *
     * @should return all values
     * @return a {@link java.util.List} object.
     */
    public List<String> getHierarchicalFacetFields() {
        return getLocalList("search.facets.hierarchicalField");
    }

    /**
     * <p>
     * getGeoFacetFields.
     * </p>
     * 
     * @should return all values
     * @return a {@link java.util.List} object.
     */
    public String getGeoFacetFields() {
        return getLocalString("search.facets.geoField");
    }

    /**
     * @return
     */
    public boolean isShowSearchHitsInGeoFacetMap() {
        return getLocalBoolean("search.facets.geoField[@displayResultsOnMap]", true);
    }

    /**
     * <p>
     * getInitialFacetElementNumber.
     * </p>
     *
     * @should return correct value
     * @should return default value if field not found
     * @param field a {@link java.lang.String} object.
     * @return a int.
     */
    public int getInitialFacetElementNumber(String facetField) {
        if (StringUtils.isBlank(facetField)) {
            return getLocalInt("search.facets.initialElementNumber", 3);
        }

        String value = getPropertyForFacetField(facetField, "[@initialElementNumber]", "-1");
        return Integer.valueOf(value);
    }

    /**
     * <p>
     * getSortOrder.
     * </p>
     *
     * @param facetField a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getSortOrder(String facetField) {
        return getPropertyForFacetField(facetField, "[@sortOrder]", "default");
    }

    /**
     * Returns a list of values to prioritize for the given facet field.
     *
     * @param field a {@link java.lang.String} object.
     * @return List of priority values; empty list if none found for the given field
     * @should return return all configured elements for regular fields
     * @should return return all configured elements for hierarchical fields
     */
    public List<String> getPriorityValuesForFacetField(String field) {
        if (StringUtils.isBlank(field)) {
            return Collections.emptyList();
        }

        String priorityValues = getPropertyForFacetField(field, "[@priorityValues]", "");
        if (priorityValues == null) {
            return Collections.emptyList();
        }
        String[] priorityValuesSplit = priorityValues.split(";");

        return Arrays.asList(priorityValuesSplit);
    }

    /**
     * 
     * @param facetField
     * @return
     * @should return correct value
     * @should return null if no value found
     */
    public String getLabelFieldForFacetField(String facetField) {
        return getPropertyForFacetField(facetField, "[@labelField]", null);
    }

    /**
     * Boilerplate code for retrieving values from regular and hierarchical facet field configurations.
     * 
     * @param facetField Facet field
     * @param property Element or attribute name to check
     * @param defaultValue Value that is returned if none was found
     * @return Found value or defaultValue
     */
    String getPropertyForFacetField(String facetField, String property, String defaultValue) {
        if (StringUtils.isBlank(facetField)) {
            return defaultValue;
        }

        String facetifiedField = SearchHelper.facetifyField(facetField);
        // Regular fields
        List<HierarchicalConfiguration<ImmutableNode>> facetFields = getLocalConfigurationsAt("search.facets.field");
        if (facetFields != null && !facetFields.isEmpty()) {
            for (HierarchicalConfiguration<ImmutableNode> fieldConfig : facetFields) {
                String nodeText = fieldConfig.getString(".", "");
                if (nodeText.equals(facetField)
                        || nodeText.equals(facetField + SolrConstants._UNTOKENIZED)
                        || nodeText.equals(facetifiedField)) {
                    String ret = fieldConfig.getString(property);
                    if (ret != null) {
                        return ret;
                    }
                }
            }
        }
        // Hierarchical fields
        facetFields = getLocalConfigurationsAt("search.facets.hierarchicalField");
        if (facetFields != null && !facetFields.isEmpty()) {
            for (HierarchicalConfiguration<ImmutableNode> fieldConfig : facetFields) {
                String nodeText = fieldConfig.getString(".", "");
                if (nodeText.equals(facetField)
                        || nodeText.equals(facetField + SolrConstants._UNTOKENIZED)
                        || nodeText.equals(facetifiedField)) {
                    String ret = fieldConfig.getString(property);
                    if (ret != null) {
                        return ret;
                    }
                }
            }
        }

        return defaultValue;
    }

    /**
     * <p>
     * getRangeFacetFields.
     * </p>
     *
     * @return List of facet fields to be used as range values
     */
    @SuppressWarnings("static-method")
    public List<String> getRangeFacetFields() {
        return Collections.singletonList(SolrConstants._CALENDAR_YEAR);
    }

    /**
     * <p>
     * isSortingEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSortingEnabled() {
        return getLocalBoolean("search.sorting[@enabled]", true);
    }

    /**
     * <p>
     * getDefaultSortField.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getDefaultSortField() {
        return getLocalString("search.sorting.defaultSortField", null);
    }

    /**
     * <p>
     * getSortFields.
     * </p>
     *
     * @should return return all configured elements
     * @return a {@link java.util.List} object.
     */
    public List<String> getSortFields() {
        return getLocalList("search.sorting.luceneField");
    }

    /**
     * <p>
     * getStaticSortFields.
     * </p>
     *
     * @should return return all configured elements
     * @return a {@link java.util.List} object.
     */
    public List<String> getStaticSortFields() {
        return getLocalList("search.sorting.static.field");
    }

    /**
     * <p>
     * getUrnResolverUrl.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getUrnResolverUrl() {
        return getLocalString("urls.urnResolver",
                new StringBuilder(BeanUtils.getServletPathWithHostAsUrlFromJsfContext()).append("/resolver?urn=").toString());
    }

    /**
     * The maximal image size retrievable with only the permission to view thumbnails
     *
     * @should return correct value
     * @return the maximal image width
     */
    public int getThumbnailImageAccessMaxWidth() {
        return getLocalInt("accessConditions.thumbnailImageAccessMaxWidth", getLocalInt("accessConditions.unconditionalImageAccessMaxWidth", 120));
    }

    /**
     * The maximal image size retrievable with the permission to view images but without the permission to zoom images
     *
     * @should return correct value
     * @return the maximal image width, default ist 600
     */
    public int getUnzoomedImageAccessMaxWidth() {
        return getLocalInt("accessConditions.unzoomedImageAccessMaxWidth", 0);
    }

    /**
     * <p>
     * isFullAccessForLocalhost.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isFullAccessForLocalhost() {
        return getLocalBoolean("accessConditions.fullAccessForLocalhost", false);
    }

    /**
     * <p>
     * isGeneratePdfInTaskManager.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isGeneratePdfInTaskManager() {
        return getLocalBoolean("pdf.externalPdfGeneration", false);
    }

    /**
     * <p>
     * isPdfApiDisabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isPdfApiDisabled() {
        return getLocalBoolean("pdf.pdfApiDisabled", false);
    }

    /**
     * <p>
     * isTitlePdfEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isTitlePdfEnabled() {
        boolean enabled = getLocalBoolean("pdf.titlePdfEnabled", true);
        return enabled;
    }

    /**
     * <p>
     * isTocPdfEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isTocPdfEnabled() {
        return getLocalBoolean("pdf.tocPdfEnabled", true);
    }

    /**
     * <p>
     * isMetadataPdfEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isMetadataPdfEnabled() {
        return getLocalBoolean("pdf.metadataPdfEnabled", true);
    }

    /**
     * <p>
     * isPagePdfEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isPagePdfEnabled() {
        return getLocalBoolean("pdf.pagePdfEnabled", false);
    }

    /**
     * <p>
     * isDocHierarchyPdfEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isDocHierarchyPdfEnabled() {
        return getLocalBoolean("pdf.docHierarchyPdfEnabled", false);
    }

    /**
     * <p>
     * isTitleEpubEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isTitleEpubEnabled() {
        return getLocalBoolean("epub.titleEpubEnabled", false);
    }

    /**
     * <p>
     * isTocEpubEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isTocEpubEnabled() {
        return getLocalBoolean("epub.tocEpubEnabled", false);
    }

    /**
     * <p>
     * isMetadataEpubEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isMetadataEpubEnabled() {
        return getLocalBoolean("epub.metadataEpubEnabled", false);
    }

    /**
     * <p>
     * getDownloadFolder.
     * </p>
     *
     * @should return correct value for pdf
     * @should return correct value for epub
     * @should return empty string if type unknown
     * @param type a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getDownloadFolder(String type) {
        switch (type.toLowerCase()) {
            case "pdf":
                return getLocalString("pdf.downloadFolder", "/opt/digiverso/viewer/pdf_download");
            case "epub":
                return getLocalString("epub.downloadFolder", "/opt/digiverso/viewer/epub_download");
            default:
                return "";

        }
    }

    /**
     * <p>
     * getRssFeedItems.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getRssFeedItems() {
        return getLocalInt("rss.numberOfItems", 50);
    }

    /**
     * <p>
     * getRssTitle.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getRssTitle() {
        return getLocalString("rss.title", "viewer-rss");
    }

    /**
     * <p>
     * getRssDescription.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getRssDescription() {
        return getLocalString("rss.description", "latest imports");
    }

    /**
     * <p>
     * getRssCopyrightText.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getRssCopyrightText() {
        return getLocalString("rss.copyright");
    }

    /**
     * <p>
     * getThumbnailsWidth.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getThumbnailsWidth() {
        return getLocalInt("viewer.thumbnailsWidth", 100);
    }

    /**
     * <p>
     * getThumbnailsHeight.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getThumbnailsHeight() {
        return getLocalInt("viewer.thumbnailsHeight", 120);
    }

    /**
     * <p>
     * getThumbnailsCompression.
     * </p>
     *
     * @return a int.
     */
    public int getThumbnailsCompression() {
        return getLocalInt("viewer.thumbnailsCompression", 85);
    }

    /**
     * <p>
     * getAnchorThumbnailMode.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getAnchorThumbnailMode() {
        return getLocalString("viewer.anchorThumbnailMode", "GENERIC");
    }

    /**
     * <p>
     * getMultivolumeThumbnailWidth.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getMultivolumeThumbnailWidth() {
        return getLocalInt("toc.multiVolumeThumbnails.width", 50);
    }

    /**
     * <p>
     * getMultivolumeThumbnailHeight.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getMultivolumeThumbnailHeight() {
        return getLocalInt("toc.multiVolumeThumbnails.height", 60);
    }

    /**
     * <p>
     * getDisplayBreadcrumbs.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean getDisplayBreadcrumbs() {
        return this.getLocalBoolean("webGuiDisplay.displayBreadcrumbs", true);
    }

    /**
     * <p>
     * getDisplayMetadataPageLinkBlock.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean getDisplayMetadataPageLinkBlock() {
        return this.getLocalBoolean("webGuiDisplay.displayMetadataPageLinkBlock", true);
    }

    /**
     * <p>
     * isAddDublinCoreMetaTags.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isAddDublinCoreMetaTags() {
        return getLocalBoolean("metadata.addDublinCoreMetaTags", false);
    }

    /**
     * <p>
     * isAddHighwirePressMetaTags.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isAddHighwirePressMetaTags() {
        return getLocalBoolean("metadata.addHighwirePressMetaTags", false);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public int getMetadataParamNumber() {
        return getLocalInt("metadata.metadataParamNumber", 10);
    }

    /**
     * <p>
     * useTiles.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public boolean useTiles() throws ViewerConfigurationException {
        return useTiles(PageType.viewImage, null);
    }

    /**
     * <p>
     * useTilesFullscreen.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public boolean useTilesFullscreen() throws ViewerConfigurationException {
        return useTiles(PageType.viewFullscreen, null);
    }

    /**
     * <p>
     * useTiles.
     * </p>
     *
     * @param view a {@link io.goobi.viewer.model.viewer.PageType} object.
     * @param image a {@link de.unigoettingen.sub.commons.contentlib.imagelib.ImageType} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public boolean useTiles(PageType view, ImageType image) throws ViewerConfigurationException {
        return getZoomImageViewConfig(view, image).getBoolean("[@tileImage]", false);
    }

    /**
     * <p>
     * getFooterHeight.
     * </p>
     *
     * @return a int.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public int getFooterHeight() throws ViewerConfigurationException {
        return getFooterHeight(PageType.viewImage, null);
    }

    /**
     * <p>
     * getFullscreenFooterHeight.
     * </p>
     *
     * @return a int.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public int getFullscreenFooterHeight() throws ViewerConfigurationException {
        return getFooterHeight(PageType.viewFullscreen, null);
    }

    /**
     * <p>
     * getFooterHeight.
     * </p>
     *
     * @param view a {@link io.goobi.viewer.model.viewer.PageType} object.
     * @param image a {@link de.unigoettingen.sub.commons.contentlib.imagelib.ImageType} object.
     * @return a int.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public int getFooterHeight(PageType view, ImageType image) throws ViewerConfigurationException {
        return getZoomImageViewConfig(view, image).getInt("[@footerHeight]", 50);
    }

    /**
     * <p>
     * getImageViewType.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getImageViewType() throws ViewerConfigurationException {
        return getZoomImageViewType(PageType.viewImage, null);
    }

    /**
     * <p>
     * getZoomFullscreenViewType.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getZoomFullscreenViewType() throws ViewerConfigurationException {
        return getZoomImageViewType(PageType.viewFullscreen, null);
    }

    /**
     * <p>
     * getZoomImageViewType.
     * </p>
     *
     * @param view a {@link io.goobi.viewer.model.viewer.PageType} object.
     * @param image a {@link de.unigoettingen.sub.commons.contentlib.imagelib.ImageType} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getZoomImageViewType(PageType view, ImageType image) throws ViewerConfigurationException {
        return getZoomImageViewConfig(view, image).getString("[@type]");
    }

    /**
     * <p>
     * useOpenSeadragon.
     * </p>
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public boolean useOpenSeadragon() throws ViewerConfigurationException {
        return "openseadragon".equalsIgnoreCase(getImageViewType());
    }

    /**
     * <p>
     * getImageViewZoomScales.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public List<String> getImageViewZoomScales() throws ViewerConfigurationException {
        return getImageViewZoomScales(PageType.viewImage, null);
    }

    /**
     * <p>
     * getImageViewZoomScales.
     * </p>
     *
     * @param view a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public List<String> getImageViewZoomScales(String view) throws ViewerConfigurationException {
        return getImageViewZoomScales(PageType.valueOf(view), null);
    }

    /**
     * <p>
     * getImageViewZoomScales.
     * </p>
     *
     * @param view a {@link io.goobi.viewer.model.viewer.PageType} object.
     * @param image a {@link de.unigoettingen.sub.commons.contentlib.imagelib.ImageType} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public List<String> getImageViewZoomScales(PageType view, ImageType image) throws ViewerConfigurationException {
        List<String> defaultList = new ArrayList<>();
        //        defaultList.add("600");
        //        defaultList.add("900");
        //        defaultList.add("1500");

        BaseHierarchicalConfiguration zoomImageViewConfig = getZoomImageViewConfig(view, image);
        if (zoomImageViewConfig != null) {
            String[] scales = zoomImageViewConfig.getStringArray("scale");
            if (scales != null) {
                return Arrays.asList(scales);
            }
        }
        return defaultList;
    }

    /**
     * <p>
     * getTileSizes.
     * </p>
     *
     * @return the configured tile sizes for imageView as a hashmap linking each tile size to the list of resolutions to use with that size
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public Map<Integer, List<Integer>> getTileSizes() throws ViewerConfigurationException {
        return getTileSizes(PageType.viewImage, null);
    }

    /**
     * <p>
     * getTileSizes.
     * </p>
     *
     * @param view a {@link io.goobi.viewer.model.viewer.PageType} object.
     * @param image a {@link de.unigoettingen.sub.commons.contentlib.imagelib.ImageType} object.
     * @return a {@link java.util.Map} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public Map<Integer, List<Integer>> getTileSizes(PageType view, ImageType image) throws ViewerConfigurationException {
        Map<Integer, List<Integer>> map = new HashMap<>();
        List<HierarchicalConfiguration<ImmutableNode>> sizes = getZoomImageViewConfig(view, image).configurationsAt("tileSize");
        if (sizes != null && !sizes.isEmpty()) {
            for (HierarchicalConfiguration<ImmutableNode> sizeConfig : sizes) {
                int size = sizeConfig.getInt("size", 0);
                String[] resolutionString = sizeConfig.getStringArray("scaleFactors");
                List<Integer> resolutions = new ArrayList<>(resolutionString.length);
                for (String res : resolutionString) {
                    try {
                        int resolution = Integer.parseInt(res);
                        resolutions.add(resolution);
                    } catch (NullPointerException | NumberFormatException e) {
                        logger.warn("Cannot parse " + res + " as int");
                    }
                }
                map.put(size, resolutions);
            }
        }
        if (map.isEmpty()) {
            map.put(512, Arrays.asList(new Integer[] { 1, 32 }));
        }
        return map;
    }

    /**
     * <p>
     * getZoomImageViewConfig.
     * </p>
     *
     * @param pageType a {@link io.goobi.viewer.model.viewer.PageType} object.
     * @param imageType a {@link de.unigoettingen.sub.commons.contentlib.imagelib.ImageType} object.
     * @return a {@link org.apache.commons.configuration.SubnodeConfiguration} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public BaseHierarchicalConfiguration getZoomImageViewConfig(PageType pageType, ImageType imageType) throws ViewerConfigurationException {
        List<HierarchicalConfiguration<ImmutableNode>> configs = getLocalConfigurationsAt("viewer.zoomImageView");

        for (HierarchicalConfiguration<ImmutableNode> subConfig : configs) {

            if (pageType != null) {
                List<Object> views = subConfig.getList("useFor.view");
                if (views.isEmpty() || views.contains(pageType.name()) || views.contains(pageType.getName())) {
                    //match
                } else {
                    continue;
                }
            }

            if (imageType != null && imageType.getFormat() != null) {
                List<Object> mimeTypes = subConfig.getList("useFor.mimeType");
                if (mimeTypes.isEmpty() || mimeTypes.contains(imageType.getFormat().getMimeType())) {
                    //match
                } else {
                    continue;
                }
            }

            return (BaseHierarchicalConfiguration) subConfig;
        }
        throw new ViewerConfigurationException("Viewer config must define at least a generic <zoomImageView>");
    }

    /**
     * <p>
     * getBreadcrumbsClipping.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getBreadcrumbsClipping() {
        return getLocalInt("webGuiDisplay.breadcrumbsClipping", 50);
    }

    /**
     * <p>
     * isDisplayTopstructLabel.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isDisplayTopstructLabel() {
        return this.getLocalBoolean("metadata.searchHitMetadataList.displayTopstructLabel", false);
    }

    /**
     * <p>
     * getDisplayStructType.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean getDisplayStructType() {
        return this.getLocalBoolean("metadata.searchHitMetadataList.displayStructType", true);
    }

    /**
     * <p>
     * getSearchHitMetadataValueNumber.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getSearchHitMetadataValueNumber() {
        return getLocalInt("metadata.searchHitMetadataList.valueNumber", 1);
    }

    /**
     * <p>
     * getSearchHitMetadataValueLength.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getSearchHitMetadataValueLength() {
        return getLocalInt("metadata.searchHitMetadataList.valueLength", 0);
    }

    /**
     * 
     * @return true if enabled or not configured; false otherwise
     * @should return correct value
     */
    public boolean isWatermarkTextConfigurationEnabled() {
        return getLocalBoolean("viewer.watermarkTextConfiguration[@enabled]", true);
    }

    /**
     * Returns the preference order of data to be used as an image footer text.
     *
     * @should return all configured elements in the correct order
     * @return a {@link java.util.List} object.
     */
    public List<String> getWatermarkTextConfiguration() {
        return getLocalList("viewer.watermarkTextConfiguration.text");
    }

    /**
     * <p>
     * getWatermarkFormat.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getWatermarkFormat() {
        return getLocalString("viewer.watermarkFormat", "jpg");
    }

    /**
     * <p>
     * getStopwordsFilePath.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getStopwordsFilePath() {
        return getLocalString("stopwordsFile");
    }

    /**
     * Returns the locally configured page type name for URLs (e.g. "bild" instead of default "image").
     *
     * @param type a {@link io.goobi.viewer.model.viewer.PageType} object.
     * @should return the correct value for the given type
     * @should return null for non configured type
     * @return a {@link java.lang.String} object.
     */
    public String getPageType(PageType type) {
        return getLocalString("viewer.pageTypes." + type.name());
    }

    /**
     * <p>
     * getDocstructTargetPageType.
     * </p>
     *
     * @param docstruct a {@link java.lang.String} object.
     * @should return correct value
     * @should return null if docstruct not found
     * @return a {@link java.lang.String} object.
     */
    public String getDocstructTargetPageType(String docstruct) {
        return getLocalString("viewer.docstructTargetPageTypes." + docstruct);
    }

    public String getPageTypeExitView(PageType type) {
        return getLocalString("viewer.pageTypes." + type.name() + "[@exit]");
    }

    /**
     * <p>
     * getFulltextPercentageWarningThreshold.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getFulltextPercentageWarningThreshold() {
        return getLocalInt("viewer.fulltextPercentageWarningThreshold", 30);
    }

    /**
     * <p>
     * isUseViewerLocaleAsRecordLanguage.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isUseViewerLocaleAsRecordLanguage() {
        return getLocalBoolean("viewer.useViewerLocaleAsRecordLanguage", false);
    }

    /**
     * <p>
     * getFeedbackEmailAddresses.
     * </p>
     *
     * @should return correct values
     * @return a {@link java.lang.String} object.
     */
    public List<EmailRecipient> getFeedbackEmailRecipients() {
        List<EmailRecipient> ret = new ArrayList<>();
        List<HierarchicalConfiguration<ImmutableNode>> licenseNodes = getLocalConfigurationsAt("user.feedbackEmailAddressList.address");
        int counter = 0;
        for (HierarchicalConfiguration<ImmutableNode> node : licenseNodes) {
            String address = node.getString(".", "");
            if (StringUtils.isNotBlank(address)) {
                String id = node.getString("[@id]", "genId_" + (++counter));
                String label = node.getString("[@label]", address);
                boolean defaultRecipient = node.getBoolean("[@default]", false);
                ret.add(new EmailRecipient(id, label, address, defaultRecipient));
            }
        }

        return ret;
    }

    /**
     * 
     * @return
     */
    public String getDefaultFeedbackEmailAddress() {
        for (EmailRecipient recipient : getFeedbackEmailRecipients()) {
            if (recipient.isDefaultRecipient()) {
                return recipient.getEmailAddress();
            }
        }

        return "<NOT CONFIGURED>";
    }

    /**
     * <p>
     * isBookmarksEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isBookmarksEnabled() {
        return getLocalBoolean("bookmarks[@enabled]", true);
    }

    /**
     * <p>
     * isForceJpegConversion.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isForceJpegConversion() {
        return getLocalBoolean("viewer.forceJpegConversion", false);
    }

    /**
     * <p>
     * getPageLoaderThreshold.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getPageLoaderThreshold() {
        return getLocalInt("performance.pageLoaderThreshold", 1000);
    }

    /**
     * <p>
     * isPreventProxyCaching.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isPreventProxyCaching() {
        return getLocalBoolean(("performance.preventProxyCaching"), false);
    }

    /**
     * <p>
     * isSolrCompressionEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSolrCompressionEnabled() {
        return getLocalBoolean(("performance.solr.compressionEnabled"), true);
    }

    /**
     * <p>
     * isSolrBackwardsCompatible.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSolrBackwardsCompatible() {
        return getLocalBoolean(("performance.solr.backwardsCompatible"), false);
    }

    /**
     * <p>
     * isCommentsEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isCommentsEnabled() {
        return getLocalBoolean(("comments[@enabled]"), false);
    }

    /**
     * <p>
     * getCommentsCondition.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getCommentsCondition() {
        return getLocalString("comments.condition");
    }

    /**
     * <p>
     * getCommentsNotificationEmailAddresses.
     * </p>
     *
     * @should return all configured elements
     * @return a {@link java.util.List} object.
     */
    public List<String> getCommentsNotificationEmailAddresses() {
        return getLocalList("comments.notificationEmailAddress");
    }

    /**
     * <p>
     * getViewerHome.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getViewerHome() {
        return getLocalString("viewerHome");
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    String getDataRepositoriesHome() {
        return getLocalString("dataRepositoriesHome", "");
    }

    /**
     * <p>
     * getWatermarkIdField.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.util.List} object.
     */
    public List<String> getWatermarkIdField() {
        return getLocalList("viewer.watermarkIdField", Collections.singletonList(SolrConstants.DC));

    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isDocstructNavigationEnabled() {
        return getLocalBoolean("viewer.docstructNavigation[@enabled]", false);
    }

    /**
     * 
     * @param template
     * @param fallbackToDefaultTemplate
     * @return
     * @should return all configured values
     */
    public List<String> getDocstructNavigationTypes(String template, boolean fallbackToDefaultTemplate) {
        List<HierarchicalConfiguration<ImmutableNode>> templateList = getLocalConfigurationsAt("viewer.docstructNavigation.template");
        if (templateList == null) {
            return Collections.emptyList();
        }

        HierarchicalConfiguration<ImmutableNode> usingTemplate = null;
        HierarchicalConfiguration<ImmutableNode> defaultTemplate = null;
        for (Iterator<HierarchicalConfiguration<ImmutableNode>> it = templateList.iterator(); it.hasNext();) {
            HierarchicalConfiguration<ImmutableNode> subElement = it.next();
            if (subElement.getString("[@name]").equals(template)) {
                usingTemplate = subElement;
                break;
            } else if ("_DEFAULT".equals(subElement.getString("[@name]"))) {
                defaultTemplate = subElement;
            }
        }

        // If the requested template does not exist in the config, use _DEFAULT
        if (usingTemplate == null && fallbackToDefaultTemplate) {
            usingTemplate = defaultTemplate;
        }
        if (usingTemplate == null) {
            return Collections.emptyList();
        }

        String[] ret = usingTemplate.getStringArray("docstruct");
        if (ret == null) {
            logger.warn("Template '{}' contains no docstruct elements.", usingTemplate.getRootElementName());
            return Collections.emptyList();
        }

        return Arrays.asList(ret);
    }

    /**
     * <p>
     * getSubthemeMainTheme.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getSubthemeMainTheme() {
        String theme = getLocalString("viewer.theme[@mainTheme]");
        if (StringUtils.isEmpty(theme)) {
            logger.error("Theme name could not be read - config_viewer.xml may not be well-formed.");
        }
        return getLocalString("viewer.theme[@mainTheme]");
    }

    /**
     * <p>
     * getSubthemeDiscriminatorField.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getSubthemeDiscriminatorField() {
        return getLocalString("viewer.theme[@discriminatorField]", "");
    }

    /**
     * <p>
     * getTagCloudSampleSize.
     * </p>
     *
     * @should return correct value for existing fields
     * @should return INT_MAX for other fields
     * @param fieldName a {@link java.lang.String} object.
     * @return a int.
     */
    public int getTagCloudSampleSize(String fieldName) {
        return getLocalInt("tagclouds.sampleSizes." + fieldName, Integer.MAX_VALUE);
    }

    /**
     * <p>
     * getTocVolumeSortFieldsForTemplate.
     * </p>
     *
     * @param template a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     * @should return correct template configuration
     * @should return default template configuration if template not found
     * @should return default template configuration if template is null
     */
    public List<StringPair> getTocVolumeSortFieldsForTemplate(String template) {
        HierarchicalConfiguration<ImmutableNode> usingTemplate = null;
        List<HierarchicalConfiguration<ImmutableNode>> templateList = getLocalConfigurationsAt("toc.volumeSortFields.template");
        if (templateList == null) {
            return Collections.emptyList();
        }
        HierarchicalConfiguration<ImmutableNode> defaultTemplate = null;
        for (Iterator<HierarchicalConfiguration<ImmutableNode>> it = templateList.iterator(); it.hasNext();) {
            HierarchicalConfiguration<ImmutableNode> subElement = it.next();
            String templateName = subElement.getString("[@name]");
            //            String groupBy = subElement.getString("[@groupBy]");
            if (templateName != null) {
                if (templateName.equals(template)) {
                    usingTemplate = subElement;
                    break;
                } else if ("_DEFAULT".equals(templateName)) {
                    defaultTemplate = subElement;
                }
            }
        }

        // If the requested template does not exist in the config, use _DEFAULT
        if (usingTemplate == null) {
            usingTemplate = defaultTemplate;
        }
        if (usingTemplate == null) {
            return Collections.emptyList();
        }

        List<HierarchicalConfiguration<ImmutableNode>> fields = usingTemplate.configurationsAt("field");
        if (fields == null) {
            return Collections.emptyList();
        }

        List<StringPair> ret = new ArrayList<>(fields.size());
        for (Iterator<HierarchicalConfiguration<ImmutableNode>> it2 = fields.iterator(); it2.hasNext();) {
            HierarchicalConfiguration<ImmutableNode> sub = it2.next();
            String field = sub.getString(".");
            String order = sub.getString("[@order]");
            ret.add(new StringPair(field, "desc".equals(order) ? "desc" : "asc"));
        }

        return ret;
    }

    /**
     * Returns the grouping Solr field for the given anchor TOC sort configuration.
     *
     * @should return correct value
     * @param template a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getTocVolumeGroupFieldForTemplate(String template) {
        HierarchicalConfiguration<ImmutableNode> usingTemplate = null;
        List<HierarchicalConfiguration<ImmutableNode>> templateList = getLocalConfigurationsAt("toc.volumeSortFields.template");
        if (templateList == null) {
            return null;
        }
        HierarchicalConfiguration<ImmutableNode> defaultTemplate = null;
        for (Iterator<HierarchicalConfiguration<ImmutableNode>> it = templateList.iterator(); it.hasNext();) {
            HierarchicalConfiguration<ImmutableNode> subElement = it.next();
            String templateName = subElement.getString("[@name]");
            if (templateName != null) {
                if (templateName.equals(template)) {
                    usingTemplate = subElement;
                    break;
                } else if ("_DEFAULT".equals(templateName)) {
                    defaultTemplate = subElement;
                }
            }
        }

        // If the requested template does not exist in the config, use _DEFAULT
        if (usingTemplate == null) {
            usingTemplate = defaultTemplate;
        }
        if (usingTemplate == null) {
            return null;
        }
        String groupBy = usingTemplate.getString("[@groupBy]");
        if (StringUtils.isNotEmpty(groupBy)) {
            return groupBy;
        }

        return null;
    }

    /**
     * <p>
     * getDisplayTitleBreadcrumbs.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean getDisplayTitleBreadcrumbs() {
        return getLocalBoolean("webGuiDisplay.displayTitleBreadcrumbs", false);
    }

    /**
     * <p>
     * isDisplayTitlePURL.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isDisplayTitlePURL() {
        return this.getLocalBoolean("webGuiDisplay.displayTitlePURL", true);
    }

    /**
     * <p>
     * getTitleBreadcrumbsMaxTitleLength.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getTitleBreadcrumbsMaxTitleLength() {
        return this.getLocalInt("webGuiDisplay.displayTitleBreadcrumbs[@maxTitleLength]", 25);
    }

    /**
     * <p>
     * getIncludeAnchorInTitleBreadcrumbs.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean getIncludeAnchorInTitleBreadcrumbs() {
        return this.getLocalBoolean("webGuiDisplay.displayTitleBreadcrumbs[@includeAnchor]", false);
    }

    /**
     * <p>
     * isDisplaySearchRssLinks.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isDisplaySearchRssLinks() {
        return getLocalBoolean("rss.displaySearchRssLinks", true);
    }

    /**
     * <p>
     * showThumbnailsInToc.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean showThumbnailsInToc() {
        return this.getLocalBoolean("toc.multiVolumeThumbnails[@enabled]", true);
    }

    /**
     * <p>
     * getStartYearForTimeline.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getStartYearForTimeline() {
        return this.getLocalString("search.timeline.startyear", "1750");
    }

    /**
     * <p>
     * getEndYearForTimeline.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getEndYearForTimeline() {
        return this.getLocalString("search.timeline.endyear", "2014");
    }

    /**
     * <p>
     * getTimelineHits.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getTimelineHits() {
        return this.getLocalString("search.timeline.hits", "108");
    }

    /**
     * <p>
     * isPiwikTrackingEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isPiwikTrackingEnabled() {
        return getLocalBoolean("piwik[@enabled]", false);
    }

    /**
     * <p>
     * getPiwikBaseURL.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getPiwikBaseURL() {
        return this.getLocalString("piwik.baseURL", "");
    }

    /**
     * <p>
     * getPiwikSiteID.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getPiwikSiteID() {
        return this.getLocalString("piwik.siteID", "1");

    }

    /**
     * <p>
     * isSearchSavingEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSearchSavingEnabled() {
        return getLocalBoolean("search.searchSaving[@enabled]", true);
    }

    /**
     * <p>
     * isBoostTopLevelDocstructs.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isBoostTopLevelDocstructs() {
        return getLocalBoolean("search.boostTopLevelDocstructs", true);
    }

    /**
     * <p>
     * isGroupDuplicateHits.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isGroupDuplicateHits() {
        return getLocalBoolean("search.groupDuplicateHits", true);
    }

    /**
     * <p>
     * getRecordGroupIdentifierFields.
     * </p>
     *
     * @should return all configured values
     * @return a {@link java.util.List} object.
     */
    public List<String> getRecordGroupIdentifierFields() {
        return getLocalList("toc.recordGroupIdentifierFields.field");
    }

    /**
     * <p>
     * getAncestorIdentifierFields.
     * </p>
     *
     * @should return all configured values
     * @return a {@link java.util.List} object.
     */
    public List<String> getAncestorIdentifierFields() {
        return getLocalList("toc.ancestorIdentifierFields.field");
    }

    /**
     * <p>
     * isTocListSiblingRecords.
     * </p>
     *
     * @should return correctValue
     * @return a boolean.
     */
    public boolean isTocListSiblingRecords() {
        return getLocalBoolean("toc.ancestorIdentifierFields[@listSiblingRecords]", false);
    }

    /**
     * <p>
     * getSearchFilters.
     * </p>
     *
     * @should return all configured elements
     * @return a {@link java.util.List} object.
     */
    public List<SearchFilter> getSearchFilters() {
        List<String> filterStrings = getLocalList("search.filters.filter");
        List<SearchFilter> ret = new ArrayList<>(filterStrings.size());
        for (String filterString : filterStrings) {
            if (filterString.startsWith("filter_")) {
                ret.add(new SearchFilter(filterString, filterString.substring(7)));
            } else {
                logger.error("Invalid search filter definition: {}", filterString);
            }
        }

        return ret;
    }

    /**
     * <p>
     * getWebApiFields.
     * </p>
     *
     * @should return all configured elements
     * @return a {@link java.util.List} object.
     */
    public List<Map<String, String>> getWebApiFields() {
        List<HierarchicalConfiguration<ImmutableNode>> elements = getLocalConfigurationsAt("webapi.fields.field");
        if (elements == null) {
            return Collections.emptyList();
        }

        List<Map<String, String>> ret = new ArrayList<>(elements.size());
        for (Iterator<HierarchicalConfiguration<ImmutableNode>> it = elements.iterator(); it.hasNext();) {
            HierarchicalConfiguration<ImmutableNode> sub = it.next();
            Map<String, String> fieldConfig = new HashMap<>();
            fieldConfig.put("jsonField", sub.getString("[@jsonField]", null));
            fieldConfig.put("luceneField", sub.getString("[@luceneField]", null));
            fieldConfig.put("multivalue", sub.getString("[@multivalue]", null));
            ret.add(fieldConfig);
        }

        return ret;
    }

    /**
     * <p>
     * getDbPersistenceUnit.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getDbPersistenceUnit() {
        return getLocalString("dbPersistenceUnit", null);
    }

    /**
     * <p>
     * useCustomNavBar.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean useCustomNavBar() {
        return getLocalBoolean("cms.useCustomNavBar", false);
    }

    /**
     * <p>
     * getCmsTemplateFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getCmsTemplateFolder() {
        return getLocalString("cms.templateFolder", "resources/cms/templates/");
    }

    /**
     * <p>
     * getCmsMediaFolder.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getCmsMediaFolder() {
        return getLocalString("cms.mediaFolder", "cms_media");
    }

    /**
     * A folder for temporary storage of media files. Used by DC record creation to store uploaded files
     * 
     * @return "temp_media" unless otherwise configured in "tempMediaFolder"
     */
    public String getTempMediaFolder() {
        return getLocalString("tempMediaFolder", "temp_media");
    }

    /**
     * <p>
     * getCmsClassifications.
     * </p>
     *
     * @should return all configured elements
     * @return a {@link java.util.List} object.
     */
    public List<String> getCmsClassifications() {
        return getLocalList("cms.classifications.classification");
    }

    /**
     * <p>
     * getCmsMediaDisplayWidth.
     * </p>
     *
     * @return a int.
     */
    public int getCmsMediaDisplayWidth() {
        return getLocalInt("cms.mediaDisplayWidth", 0);
    }

    /**
     * <p>
     * getCmsMediaDisplayHeight.
     * </p>
     *
     * @return a int.
     */
    public int getCmsMediaDisplayHeight() {
        return getLocalInt("cms.mediaDisplayHeight", 0);
    }

    /**
     * <p>
     * isTranskribusEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isTranskribusEnabled() {
        return getLocalBoolean("transkribus[@enabled]", false);
    }

    /**
     * <p>
     * getTranskribusUserName.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getTranskribusUserName() {
        return getLocalString("transkribus.userName");
    }

    /**
     * <p>
     * getTranskribusPassword.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getTranskribusPassword() {
        return getLocalString("transkribus.password");
    }

    /**
     * <p>
     * getTranskribusDefaultCollection.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getTranskribusDefaultCollection() {
        return getLocalString("transkribus.defaultCollection");
    }

    /**
     * <p>
     * getTranskribusRestApiUrl.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getTranskribusRestApiUrl() {
        return getLocalString("transkribus.restApiUrl", TranskribusUtils.TRANSRIBUS_REST_URL);
    }

    /**
     * <p>
     * getTranskribusAllowedDocumentTypes.
     * </p>
     *
     * @should return all configured elements
     * @return a {@link java.util.List} object.
     */
    public List<String> getTranskribusAllowedDocumentTypes() {
        return getLocalList("transkribus.allowedDocumentTypes.docstruct");
    }

    /**
     * <p>
     * getTocIndentation.
     * </p>
     *
     * @should return correct value
     * @return a int.
     */
    public int getTocIndentation() {
        return getLocalInt("toc.tocIndentation", 20);
    }

    /**
     * <p>
     * isPageBrowseEnabled.
     * </p>
     *
     * @return a boolean.
     * @should return correct value
     */
    public boolean isPageBrowseEnabled() {
        return getLocalBoolean("viewer.pageBrowse[@enabled]", false);
    }

    /**
     * <p>
     * getPageBrowseSteps.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<Integer> getPageBrowseSteps() {
        List<String> defaultList = Collections.singletonList("1");
        List<String> stringList = getLocalList("viewer.pageBrowse.pageBrowseStep", defaultList);
        List<Integer> intList = new ArrayList<>();
        for (String s : stringList) {
            try {
                intList.add(Integer.valueOf(s));
            } catch (NullPointerException | NumberFormatException e) {
                logger.error("Illegal config at 'viewer.pageBrowse.pageBrowseStep': " + s);
            }
        }
        return intList;
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public int getPageSelectDropdownDisplayMinPages() {
        return getLocalInt("viewer.pageSelectDropdownDisplayMinPages", 3);
    }

    /**
     * <p>
     * getTaskManagerServiceUrl.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getTaskManagerServiceUrl() {
        return getLocalString("urls.taskManager", "http://localhost:8080/itm/") + "service";
    }

    /**
     * <p>
     * getTaskManagerRestUrl.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getTaskManagerRestUrl() {
        return getLocalString("urls.taskManager", "http://localhost:8080/itm/") + "rest";
    }

    /**
     * <p>
     * getReCaptchaSiteKey.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getReCaptchaSiteKey() {
        return getLocalString("reCaptcha.provider[@siteKey]");
    }

    /**
     * <p>
     * isUseReCaptcha.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isUseReCaptcha() {
        return getLocalBoolean("reCaptcha[@enabled]", true);
    }

    /**
     * <p>
     * isSearchInItemEnabled.
     * </p>
     *
     * @should return true if the search field to search the current item/work is configured to be visible
     * @return a boolean.
     */
    public boolean isSearchInItemEnabled() {
        return getLocalBoolean("sidebar.searchInItem[@enabled]", true);
    }

    /**
     * <p>
     * isSearchExcelExportEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSearchExcelExportEnabled() {
        return getLocalBoolean("search.export.excel[@enabled]", false);
    }

    /**
     * <p>
     * getSearchExcelExportFields.
     * </p>
     *
     * @should return all values
     * @return a {@link java.util.List} object.
     */
    public List<String> getSearchExcelExportFields() {
        return getLocalList("search.export.excel.field", new ArrayList<String>(0));
    }

    /**
     * <p>
     * getExcelDownloadTimeout.
     * </p>
     *
     * @return a int.
     */
    public int getExcelDownloadTimeout() {
        return getLocalInt("search.export.excel.timeout", 120);
    }

    /**
     * <p>
     * isDisplayEmptyTocInSidebar.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isDisplayEmptyTocInSidebar() {
        return getLocalBoolean("sidebar.sidebarToc.visibleIfEmpty", true);
    }

    /**
     * <p>
     * isDoublePageNavigationEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isDoublePageNavigationEnabled() {
        return getLocalBoolean("viewer.doublePageNavigation[@enabled]", false);
    }

    /**
     * <p>
     * getRestrictedImageUrls.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getRestrictedImageUrls() {
        return getLocalList("viewer.externalContent.restrictedUrls.url", Collections.emptyList());
    }

    public List<String> getIIIFLicenses() {
        return getLocalList("webapi.iiif.license", Collections.emptyList());
    }

    /**
     * <p>
     * getIIIFMetadataFields.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getIIIFMetadataFields() {
        return getLocalList("webapi.iiif.metadataFields.field", Collections.emptyList());
    }

    /**
     * <p>
     * getIIIFEventFields.
     * </p>
     *
     * @return the list of all configured event fields for IIIF manifests All fields must contain a "/" to separate the event type and the actual
     *         field name If no "/" is present in the configured field it is prepended to the entry to indicate that this field should be taken from
     *         all events
     */
    public List<String> getIIIFEventFields() {
        List<String> fields = getLocalList("webapi.iiif.metadataFields.event", Collections.emptyList());
        fields = fields.stream().map(field -> field.contains("/") ? field : "/" + field).collect(Collectors.toList());
        return fields;
    }

    /**
     * <p>
     * getIIIFMetadataLabel.
     * </p>
     *
     * @param field the value of the field
     * @return The attribute "label" of any children of webapi.iiif.metadataFields
     * @should return correct values
     */
    public String getIIIFMetadataLabel(String field) {
        List<HierarchicalConfiguration<ImmutableNode>> fieldsConfig = getLocalConfigurationsAt("webapi.iiif.metadataFields");
        if (fieldsConfig == null || fieldsConfig.isEmpty()) {
            return "";
        }

        List<HierarchicalConfiguration<ImmutableNode>> fields = fieldsConfig.get(0).childConfigurationsAt("");
        for (HierarchicalConfiguration<ImmutableNode> fieldNode : fields) {
            String value = fieldNode.getString(".");
            if (value != null && value.equals(field)) {
                return fieldNode.getString("[@label]", "");
            }
        }
        return "";
    }

    /**
     * Configured in webapi.iiif.discovery.activitiesPerPage. Default value is 100
     *
     * @return The number of activities to display per collection page in the IIIF discovery api
     */
    public int getIIIFDiscoveryAvtivitiesPerPage() {
        return getLocalInt("webapi.iiif.discovery.activitiesPerPage", 100);
    }

    /**
     * <p>
     * getIIIFLogo.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public List<String> getIIIFLogo() {
        return getLocalList("webapi.iiif.logo", new ArrayList<>());
    }

    /**
     * <p>
     * getIIIFNavDateField.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getIIIFNavDateField() {
        return getLocalString("webapi.iiif.navDateField", null);
    }

    /**
     * <p>
     * getIIIFAttribution.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public List<String> getIIIFAttribution() {
        return getLocalList("webapi.iiif.attribution", new ArrayList<>());
    }

    /**
     * <p>
     * getIIIFDescriptionFields.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getIIIFDescriptionFields() {
        return getLocalList("webapi.iiif.descriptionFields.field", new ArrayList<>());

    }

    public List<Locale> getIIIFTranslationLocales() {
        List<Locale> list = getLocalList("webapi.iiif.translations.locale", new ArrayList<>())
                .stream()
                .map(Locale::forLanguageTag)
                .filter(l -> StringUtils.isNotBlank(l.getLanguage()))
                .collect(Collectors.toList());

        if (list.isEmpty()) {
            return ViewerResourceBundle.getAllLocales();
        }

        return list;
    }

    /**
     * 
     * @return The SOLR field containing a rights url for a IIIF3 manifest if one is configured
     */
    public String getIIIFRightsField() {
        return getLocalString("webapi.iiif.rights", null);
    }

    /**
     * Uses {@link #getIIIFAttribution()} as fallback;
     * 
     * @return the message key to use for the IIIF3 requiredStatement value if the statement should be added to manifests.
     */
    public String getIIIFRequiredValue() {
        return getLocalString("webapi.iiif.requiredStatement.value", getIIIFAttribution().stream().findFirst().orElse(null));
    }

    /**
     * 
     * @return the message key to use for the IIIF3 requiredStatement label. Default is "Attribution"
     */
    public String getIIIFRequiredLabel() {
        return getLocalString("webapi.iiif.requiredStatement.label", "Attribution");
    }

    /**
     * 
     * @return The list of configurations for IIIF3 providers
     * @throws PresentationException if a provider or a homepage configuration misses the url or label element
     */
    public List<ProviderConfiguration> getIIIFProvider() throws PresentationException {
        List<ProviderConfiguration> provider = new ArrayList<>();
        List<HierarchicalConfiguration<ImmutableNode>> configs = getLocalConfigurationsAt("webapi.iiif.provider");
        for (HierarchicalConfiguration<ImmutableNode> config : configs) {
            provider.add(new ProviderConfiguration(config));
        }
        return provider;
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isVisibleIIIFRenderingPDF() {
        return getLocalBoolean("webapi.iiif.rendering.pdf[@enabled]", true);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isVisibleIIIFRenderingViewer() {
        return getLocalBoolean("webapi.iiif.rendering.viewer[@enabled]", true);
    }

    public String getLabelIIIFRenderingPDF() {
        return getLocalString("webapi.iiif.rendering.pdf.label", null);
    }

    public String getLabelIIIFRenderingViewer() {
        return getLocalString("webapi.iiif.rendering.viewer.label", null);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isVisibleIIIFRenderingPlaintext() {
        return getLocalBoolean("webapi.iiif.rendering.plaintext[@enabled]", true);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public boolean isVisibleIIIFRenderingAlto() {
        return getLocalBoolean("webapi.iiif.rendering.alto[@enabled]", true);
    }

    public String getLabelIIIFRenderingPlaintext() {
        return getLocalString("webapi.iiif.rendering.plaintext.label", null);
    }

    public String getLabelIIIFRenderingAlto() {
        return getLocalString("webapi.iiif.rendering.alto.label", null);
    }

    /**
     * <p>
     * getSitelinksField.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getSitelinksField() {
        return getLocalString("sitemap.sitelinksField");
    }

    /**
     * <p>
     * getSitelinksFilterQuery.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getSitelinksFilterQuery() {
        return getLocalString("sitemap.sitelinksFilterQuery");
    }

    /**
     * <p>
     * getConfiguredCollections.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getConfiguredCollections() {
        return getLocalList("collections.collection[@field]", Collections.emptyList());

    }

    /**
     * <p>
     * getWebApiToken.
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getWebApiToken() {
        String token = getLocalString("webapi.authorization.token", "");
        return token;
    }

    /**
     * <p>
     * isAllowRedirectCollectionToWork.
     * </p>
     *
     * @return true if opening a collection containing only a single work should redirect to that work
     * @should return correct value
     */
    public boolean isAllowRedirectCollectionToWork() {
        boolean redirect = getLocalBoolean("collections.redirectToWork", true);
        return redirect;
    }

    /**
     * <p>
     * getTwitterUserName.
     * </p>
     *
     * @return Configured value; null if none configured
     * @should return correct value
     */
    public String getTwitterUserName() {
        String token = getLocalString("embedding.twitter.userName");
        return token;
    }

    /**
     * <p>
     * getLimitImageHeightUpperRatioThreshold.
     * </p>
     *
     * @should return correct value
     * @return a float.
     */
    public float getLimitImageHeightUpperRatioThreshold() {
        return getLocalFloat("viewer.limitImageHeight[@upperRatioThreshold]", 0.3f);
    }

    /**
     * <p>
     * getLimitImageHeightLowerRatioThreshold.
     * </p>
     *
     * @should return correct value
     * @return a float.
     */
    public float getLimitImageHeightLowerRatioThreshold() {
        return getLocalFloat("viewer.limitImageHeight[@lowerRatioThreshold]", 3f);
    }

    /**
     * <p>
     * isLimitImageHeight.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isLimitImageHeight() {
        return getLocalBoolean("viewer.limitImageHeight", true);
    }

    /**
     * <p>
     * isAddCORSHeader.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isAddCORSHeader() {
        return getLocalBoolean("webapi.cors[@enabled]", false);
    }

    /**
     * <p>
     * Gets the value configured in webapi.cors. Default is "*"
     * </p>
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getCORSHeaderValue() {
        return getLocalString("webapi.cors", "*");
    }

    /**
     * @return
     */
    public boolean isDiscloseImageContentLocation() {
        return getLocalBoolean("webapi.iiif.discloseContentLocation", true);
    }

    public String getAccessConditionDisplayField() {
        return getLocalString("webGuiDisplay.displayCopyrightInfo.accessConditionField", null);
    }

    public String getCopyrightDisplayField() {
        return getLocalString("webGuiDisplay.displayCopyrightInfo.copyrightField", null);
    }

    public boolean isDisplayCopyrightInfo() {
        return getLocalBoolean("webGuiDisplay.displayCopyrightInfo.visible", false);
    }

    public boolean isDisplaySocialMediaShareLinks() {
        return getLocalBoolean("webGuiDisplay.displaySocialMediaShareLinks", false);
    }

    public boolean isDisplayAnchorLabelInTitleBar(String template) {
        List<HierarchicalConfiguration<ImmutableNode>> templateList = getLocalConfigurationsAt("toc.titleBarLabel.template");
        HierarchicalConfiguration<ImmutableNode> subConf = getMatchingConfig(templateList, template);
        if (subConf != null) {
            return subConf.getBoolean("displayAnchorTitle", false);
        }

        return false;
    }

    public String getAnchorLabelInTitleBarPrefix(String template) {
        List<HierarchicalConfiguration<ImmutableNode>> templateList = getLocalConfigurationsAt("toc.titleBarLabel.template");
        HierarchicalConfiguration<ImmutableNode> subConf = getMatchingConfig(templateList, template);
        if (subConf != null) {
            return subConf.getString("displayAnchorTitle[@prefix]", "");
        }

        return "";
    }

    public String getAnchorLabelInTitleBarSuffix(String template) {
        List<HierarchicalConfiguration<ImmutableNode>> templateList = getLocalConfigurationsAt("toc.titleBarLabel.template");
        HierarchicalConfiguration<ImmutableNode> subConf = getMatchingConfig(templateList, template);
        if (subConf != null) {
            return subConf.getString("displayAnchorTitle[@suffix]", " ");
        }

        return " ";
    }

    public String getMapBoxToken() {
        return getLocalString("maps.mapbox.token", "");
    }

    public String getMapBoxUser() {
        return getLocalString("maps.mapbox.user", "");
    }

    public String getMapBoxStyleId() {
        return getLocalString("maps.mapbox.styleId", "");
    }

    public boolean isDisplayAddressSearchInMap() {
        return getLocalBoolean("maps.mapbox.addressSearch[@enabled]", true);
    }

    /**
     * @param marker
     * @return
     */
    public GeoMapMarker getGeoMapMarker(String name) {
        return getGeoMapMarkers().stream().filter(m -> name.equalsIgnoreCase(m.getName())).findAny().orElse(null);
    }

    /**
     *
     * @return a list of solr field names containing GeoJson data used to create markers in maps
     */
    public List<String> getGeoMapMarkerFields() {
        return getLocalList("maps.coordinateFields.field", Arrays.asList("MD_GEOJSON_POINT", "NORM_COORDS_GEOJSON"));
    }

    public boolean includeCoordinateFieldsFromMetadataDocs() {
        return getLocalBoolean("maps.coordinateFields[@includeMetadataDocs]", false);
    }

    public List<GeoMapMarker> getGeoMapMarkers() {

        List<GeoMapMarker> markers = new ArrayList<>();
        List<HierarchicalConfiguration<ImmutableNode>> configs = getLocalConfigurationsAt("maps.markers.marker");
        for (HierarchicalConfiguration<ImmutableNode> config : configs) {
            GeoMapMarker marker = readGeoMapMarker(config);
            if (marker != null) {
                markers.add(marker);
            }
        }
        return markers;

    }

    /**
     * @param config
     * @param marker
     * @return
     */
    public static GeoMapMarker readGeoMapMarker(HierarchicalConfiguration<ImmutableNode> config) {
        GeoMapMarker marker = null;
        String name = config.getString(".");
        if (StringUtils.isNotBlank(name)) {
            marker = new GeoMapMarker(name);
            marker.setExtraClasses(config.getString("[@extraClasses]", marker.getExtraClasses()));
            marker.setIcon(config.getString("[@icon]", marker.getIcon()));
            marker.setIconColor(config.getString("[@iconColor]", marker.getIconColor()));
            marker.setIconRotate(config.getInt("[@iconRotate]", marker.getIconRotate()));
            marker.setMarkerColor(config.getString("[@markerColor]", marker.getMarkerColor()));
            marker.setHighlightColor(config.getString("[@highlightColor]", marker.getHighlightColor()));
            marker.setNumber(config.getString("[@number]", marker.getNumber()));
            marker.setPrefix(config.getString("[@prefix]", marker.getPrefix()));
            marker.setShape(config.getString("[@shape]", marker.getShape()));
            marker.setSvg(config.getBoolean("[@svg]", marker.isSvg()));
            marker.setShadow(config.getBoolean("[@shadow]", marker.isShadow()));
        }
        return marker;
    }

    /**
     * Find the template with the given name in the templateList. If no such template exists, find the template with name _DEFAULT. Failing that,
     * return null;
     * 
     * @param templateList
     * @param template
     * @return
     */
    private static HierarchicalConfiguration<ImmutableNode> getMatchingConfig(List<HierarchicalConfiguration<ImmutableNode>> templateList,
            String name) {
        if (name == null || templateList == null) {
            return null;
        }

        HierarchicalConfiguration<ImmutableNode> conf = null;
        HierarchicalConfiguration<ImmutableNode> defaultConf = null;
        for (HierarchicalConfiguration<ImmutableNode> subConf : templateList) {
            if (name.equalsIgnoreCase(subConf.getString("[@name]"))) {
                conf = subConf;
                break;
            } else if ("_DEFAULT".equalsIgnoreCase(subConf.getString("[@name]"))) {
                defaultConf = subConf;
            }
        }
        if (conf != null) {
            return conf;
        }

        return defaultConf;
    }

    /**
     * 
     * @return
     */
    public List<LicenseDescription> getLicenseDescriptions() {
        List<LicenseDescription> licenses = new ArrayList<>();
        List<HierarchicalConfiguration<ImmutableNode>> licenseNodes = getLocalConfigurationsAt("metadata.licenses.license");
        for (HierarchicalConfiguration<ImmutableNode> node : licenseNodes) {
            String url = node.getString("[@url]", "");
            if (StringUtils.isNotBlank(url)) {
                String label = node.getString("[@label]", url);
                String icon = node.getString("[@icon]", "");
                LicenseDescription license = new LicenseDescription(url);
                license.setLabel(label);
                license.setIcon(icon);
                licenses.add(license);
            }
        }

        return licenses;
    }

    /**
     * @return
     */
    public String getBaseXUrl() {
        return getLocalString("urls.basex.url");
    }

    /**
     * @return
     */
    public String getBaseXDatabase() {
        return getLocalString("urls.basex.defaultDatabase");

    }

    /**
     * @return
     */
    public HierarchicalConfiguration<ImmutableNode> getBaseXMetadataConfig() {
        return getLocalConfigurationAt("metadata.basexMetadataList");
    }

    public boolean isDisplayUserGeneratedContentBelowImage() {
        return getLocalBoolean("webGuiDisplay.displayUserGeneratedContentBelowImage", false);
    }

    /**
     * config: <code>&#60;iiif use-version="3.0"&#62;&#60;/iiif&#62;</code>
     * 
     * @return
     */
    public String getIIIFVersionToUse() {
        return getLocalString("webapi.iiif[@use-version]", "2.1.1");
    }

    /**
     * 
     * @return
     * @should read config items correctly
     */
    public List<TranslationGroup> getTranslationGroups() {
        List<TranslationGroup> ret = new ArrayList<>();
        List<HierarchicalConfiguration<ImmutableNode>> groupNodes = getLocalConfigurationsAt("translations.group");
        int id = 0;
        for (HierarchicalConfiguration<ImmutableNode> groupNode : groupNodes) {
            String typeValue = groupNode.getString("[@type]");
            if (StringUtils.isBlank(typeValue)) {
                logger.warn("translations/group/@type may not be empty.");
                continue;
            }
            TranslationGroupType type = TranslationGroupType.getByName(typeValue);
            if (type == null) {
                logger.warn("Unknown translations/group/@type: {}", typeValue);
                continue;
            }
            String name = groupNode.getString("[@name]");
            if (StringUtils.isBlank(name)) {
                logger.warn("translations/group/@name may not be empty.");
                continue;
            }
            String description = groupNode.getString("[@description]");
            List<HierarchicalConfiguration<ImmutableNode>> keyNodes = groupNode.configurationsAt("key");
            TranslationGroup group = TranslationGroup.create(id, type, name, description, keyNodes.size());
            for (HierarchicalConfiguration<ImmutableNode> keyNode : keyNodes) {
                String value = keyNode.getString(".");
                if (StringUtils.isBlank(value)) {
                    logger.warn("translations/group/key may not be empty.");
                    continue;
                }
                boolean regex = keyNode.getBoolean("[@regex]", false);
                group.getItems().add(TranslationGroupItem.create(type, value, regex));
            }
            ret.add(group);
            id++;
        }

        return ret;
    }

    public boolean isDisplayAnnotationTextInImage() {
        return getLocalBoolean("webGuiDisplay.displayAnnotationTextInImage", true);
    }

}
