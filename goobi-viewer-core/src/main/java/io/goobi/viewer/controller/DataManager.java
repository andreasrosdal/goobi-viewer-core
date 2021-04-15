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

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.monitoring.timer.TimeAnalysis;
import io.goobi.viewer.api.rest.model.tasks.TaskManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.dao.impl.JPADAO;
import io.goobi.viewer.dao.update.DatabaseUpdater;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.ModuleMissingException;
import io.goobi.viewer.model.bookmark.SessionStoreBookmarkManager;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.security.authentication.AuthResponseListener;
import io.goobi.viewer.model.security.authentication.OpenIdProvider;
import io.goobi.viewer.model.security.recordlock.RecordLockManager;
import io.goobi.viewer.model.translations.language.LanguageHelper;
import io.goobi.viewer.modules.IModule;
import io.goobi.viewer.modules.interfaces.DefaultURLBuilder;
import io.goobi.viewer.modules.interfaces.IURLBuilder;

/**
 * <p>
 * DataManager class.
 * </p>
 */
public final class DataManager {

    private static final Logger logger = LoggerFactory.getLogger(DataManager.class);

    private static final Object lock = new Object();

    private static DataManager instance = null;

    private final List<IModule> modules = new ArrayList<>();

    private final Map<String, Map<String, String>> sessionMap = new LinkedHashMap<>();

    private final RecordLockManager recordLockManager = new RecordLockManager();

    private Configuration configuration;

    private LanguageHelper languageHelper;

    private SolrSearchIndex searchIndex;

    private IDAO dao;

    private SessionStoreBookmarkManager bookmarkManager;

    private AuthResponseListener<OpenIdProvider> oAuthResponseListener;

    private IURLBuilder defaultUrlBuilder = new DefaultURLBuilder();

    private Map<String, List<Campaign>> recordCampaignMap = null;

    private String indexerVersion = "";

    private RestApiManager restApiManager;

    private TimeAnalysis timing = new TimeAnalysis();

    private FileResourceManager fileResourceManager = null;

    private final TaskManager restApiJobManager = new TaskManager(Duration.of(7, ChronoUnit.DAYS));

    /**
     * <p>
     * Getter for the field <code>instance</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.controller.DataManager} object.
     */
    public static DataManager getInstance() {
        DataManager dm = instance;
        if (dm == null) {
            synchronized (lock) {
                // Another thread might have initialized instance by now
                dm = instance;
                if (dm == null) {
                    dm = new DataManager();
                    instance = dm;
                }
            }
        }

        return dm;
    }

    private DataManager() {
    }

    /**
     * <p>
     * Getter for the field <code>modules</code>.
     * </p>
     *
     * @return the modules
     */
    public List<IModule> getModules() {
        return modules;
    }

    /**
     * <p>
     * getUrlBuilder.
     * </p>
     *
     * @return the urlBuilder
     */
    public IURLBuilder getUrlBuilder() {
        return getModules().stream()
                .map(module -> module.getURLBuilder())
                .filter(optional -> optional.isPresent())
                .map(optional -> optional.get())
                .findFirst()
                .orElse(defaultUrlBuilder);
    }

    /**
     * <p>
     * getModule.
     * </p>
     *
     * @param id a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.modules.IModule} object.
     * @throws io.goobi.viewer.exceptions.ModuleMissingException if any.
     */
    public IModule getModule(String id) throws ModuleMissingException {
        if (StringUtils.isEmpty(id)) {
            throw new IllegalArgumentException("name may not be null or empty");
        }

        for (IModule module : modules) {
            if (module.getId().equals(id)) {
                return module;
            }
        }

        throw new ModuleMissingException("Module not loaded: " + id);
    }

    /**
     * <p>
     * isModuleLoaded.
     * </p>
     *
     * @param id a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean isModuleLoaded(String id) {
        if (StringUtils.isEmpty(id)) {
            throw new IllegalArgumentException("name may not be null or empty");
        }

        for (IModule module : modules) {
            if (module.getId().equals(id)) {
                return true;
            }
        }

        return false;
    }

    /**
     * <p>
     * registerModule.
     * </p>
     *
     * @param module a {@link io.goobi.viewer.modules.IModule} object.
     * @should not add module if it's already registered
     * @return a boolean.
     */
    public boolean registerModule(IModule module) {
        if (module == null) {
            throw new IllegalArgumentException("module may not be null");
        }

        for (IModule m : modules) {
            if (m.getId().equals(module.getId())) {
                logger.warn(
                        "Module rejected because a module with the same ID is already registered.\nRegistered module: {} ({}) v{}\nRejected module: {} ({}) v{}",
                        m.getId(), m.getName(), m.getVersion(), module.getId(), module.getName(), module.getVersion());
                return false;
            }
        }

        modules.add(module);
        logger.info("Module registered: {} ({}) v{}", module.getId(), module.getName(), module.getVersion());
        return true;
    }

    /**
     * <p>
     * getSessionCount.
     * </p>
     *
     * @return a int.
     */
    public int getSessionCount() {
        return sessionMap.size();
    }

    /**
     * <p>
     * Getter for the field <code>sessionMap</code>.
     * </p>
     *
     * @return the sessionMap
     */
    public Map<String, Map<String, String>> getSessionMap() {
        return sessionMap;
    }

    /**
     * <p>
     * Getter for the field <code>configuration</code>.
     * </p>
     *
     * @return the configuration
     */
    public Configuration getConfiguration() {
        if (configuration == null) {
            synchronized (lock) {
                configuration = new Configuration("config_viewer.xml");
            }
        }

        return configuration;
    }

    /**
     * <p>
     * Getter for the field <code>languageHelper</code>.
     * </p>
     *
     * @return the languageHelper
     */
    public LanguageHelper getLanguageHelper() {
        if (languageHelper == null) {
            synchronized (lock) {
                languageHelper = new LanguageHelper("languages.xml");
            }
        }

        return languageHelper;
    }

    /**
     * <p>
     * Getter for the field <code>searchIndex</code>.
     * </p>
     *
     * @return the searchIndex
     */
    public SolrSearchIndex getSearchIndex() {
        if (searchIndex == null) {
            synchronized (lock) {
                searchIndex = new SolrSearchIndex(null);
            }
        }
        searchIndex.checkReloadNeeded();

        return searchIndex;
    }

    /**
     * <p>
     * Getter for the field <code>dao</code>.
     * </p>
     *
     * @return the dao
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public IDAO getDao() throws DAOException {
        if (dao == null) {
            synchronized (lock) {
                dao = new JPADAO(getConfiguration().getDbPersistenceUnit());
                new DatabaseUpdater(dao).update();
            }
        }

        return dao;
    }

    /**
     * Sets custom Configuration object (used for unit testing).
     *
     * @param configuration a {@link io.goobi.viewer.controller.Configuration} object.
     */
    public void injectConfiguration(Configuration configuration) {
        if (configuration != null) {
            this.configuration = configuration;
        }
    }

    /**
     * Sets custom SolrSearchIndex object (used for unit testing).
     *
     * @param searchIndex a {@link io.goobi.viewer.controller.SolrSearchIndex} object.
     */
    public void injectSearchIndex(SolrSearchIndex searchIndex) {
        if (searchIndex != null) {
            this.searchIndex = searchIndex;
        }
    }

    /**
     * Sets custom IDAO object (used for unit testing).
     *
     * @param dao a {@link io.goobi.viewer.dao.IDAO} object.
     */
    public void injectDao(IDAO dao) {
        this.dao = dao;
    }

    /**
     * <p>
     * Getter for the field <code>bookmarkManager</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.bookmark.SessionStoreBookmarkManager} object.
     */
    public SessionStoreBookmarkManager getBookmarkManager() {
        if (this.bookmarkManager == null) {
            synchronized (lock) {
                this.bookmarkManager = new SessionStoreBookmarkManager();
            }
        }
        return this.bookmarkManager;
    }

    /**
     * <p>
     * injectBookmarkManager.
     * </p>
     *
     * @param bookmarkManager a {@link io.goobi.viewer.model.bookmark.SessionStoreBookmarkManager} object.
     */
    public void injectBookmarkManager(SessionStoreBookmarkManager bookmarkManager) {
        this.bookmarkManager = bookmarkManager;
    }

    /**
     * <p>
     * injectOAuthResponseListener.
     * </p>
     *
     * @param listener a {@link io.goobi.viewer.model.security.authentication.IAuthResponseListener} object.
     */
    public void injectOAuthResponseListener(AuthResponseListener<OpenIdProvider> listener) {
        if (listener != null) {
            this.oAuthResponseListener = listener;
        }
    }

    /**
     * <p>
     * Getter for the field <code>oAuthResponseListener</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.security.authentication.IAuthResponseListener} object.
     */
    public AuthResponseListener<OpenIdProvider> getOAuthResponseListener() {
        if (oAuthResponseListener == null) {
            synchronized (lock) {
                oAuthResponseListener = new AuthResponseListener<>();
            }
        }

        return oAuthResponseListener;
    }

    /**
     * <p>
     * Getter for the field <code>recordCampaignMap</code>.
     * </p>
     *
     * @return the recordCampaignMap
     */
    public Map<String, List<Campaign>> getRecordCampaignMap() {
        return recordCampaignMap;
    }

    /**
     * <p>
     * Setter for the field <code>recordCampaignMap</code>.
     * </p>
     *
     * @param recordCampaignMap the recordCampaignMap to set
     */
    public void setRecordCampaignMap(Map<String, List<Campaign>> recordCampaignMap) {
        this.recordCampaignMap = recordCampaignMap;
    }

    /**
     * @return the indexerVersion
     */
    public String getIndexerVersion() {
        return indexerVersion;
    }

    /**
     * @param indexerVersion the indexerVersion to set
     */
    public void setIndexerVersion(String indexerVersion) {
        this.indexerVersion = indexerVersion;
        logger.trace(indexerVersion);
    }

    /**
     * @return the restApiManager
     */
    public RestApiManager getRestApiManager() {
        if (this.restApiManager == null) {
            this.restApiManager = new RestApiManager(getConfiguration());
        }
        return restApiManager;
    }

    /**
     * @param restApiManager the restApiManager to set
     */
    public void setRestApiManager(RestApiManager restApiManager) {
        this.restApiManager = restApiManager;
    }

    /**
     * @return the recordLockManager
     */
    public RecordLockManager getRecordLockManager() {
        return recordLockManager;
    }

    /**
     * @return the timing
     */
    public TimeAnalysis getTiming() {
        return timing;
    }

    /**
     * 
     */
    public void resetTiming() {
        this.timing = new TimeAnalysis();

    }

    public FileResourceManager getFileResourceManager() {
        if (this.fileResourceManager == null) {
            this.fileResourceManager = createFileResourceManager();
        }
        return this.fileResourceManager;
    }

    private FileResourceManager createFileResourceManager() {
        if (FacesContext.getCurrentInstance() != null) {
            ServletContext servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
            String themeName = getConfiguration().getTheme();
            return new FileResourceManager(servletContext, themeName);
        }

        throw new IllegalStateException("Must be called from within faces context");
    }

    /**
     * @return the restApiJobManager
     */
    public TaskManager getRestApiJobManager() {
        return restApiJobManager;
    }
}
