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
package io.goobi.viewer.dao.impl;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.RollbackException;

import org.apache.commons.lang.StringUtils;
import org.eclipse.persistence.exceptions.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.AlphabetIterator;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.AccessDeniedException;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.annotation.Comment;
import io.goobi.viewer.model.bookshelf.Bookshelf;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.CMSCollection;
import io.goobi.viewer.model.cms.CMSContentItem;
import io.goobi.viewer.model.cms.CMSMediaItem;
import io.goobi.viewer.model.cms.CMSNavigationItem;
import io.goobi.viewer.model.cms.CMSPage;
import io.goobi.viewer.model.cms.CMSSidebarElement;
import io.goobi.viewer.model.cms.CMSStaticPage;
import io.goobi.viewer.model.download.DownloadJob;
import io.goobi.viewer.model.overviewpage.OverviewPage;
import io.goobi.viewer.model.overviewpage.OverviewPageUpdate;
import io.goobi.viewer.model.search.Search;
import io.goobi.viewer.model.security.LicenseType;
import io.goobi.viewer.model.security.Role;
import io.goobi.viewer.model.security.user.IpRange;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;
import io.goobi.viewer.model.security.user.UserRole;
import io.goobi.viewer.model.transkribus.TranskribusJob;
import io.goobi.viewer.model.transkribus.TranskribusJob.JobStatus;
import io.goobi.viewer.model.viewer.PageType;

public class JPADAO implements IDAO {

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(JPADAO.class);
    private static final String DEFAULT_PERSISTENCE_UNIT_NAME = "intranda_viewer_tomcat";
    private static final String MULTIKEY_SEPARATOR = "_";

    private final EntityManagerFactory factory;
    private EntityManager em;
    private Object cmsRequestLock = new Object();
    private Object overviewPageRequestLock = new Object();

    public JPADAO() throws DAOException {
        this(null);
    }

    public EntityManagerFactory getFactory() {
        return this.factory;
    }

    public EntityManager getEntityManager() {
        return em;
    }

    public JPADAO(String inPersistenceUnitName) throws DAOException {
        logger.trace("JPADAO({})", inPersistenceUnitName);
        //        logger.debug(System.getProperty(PersistenceUnitProperties.ECLIPSELINK_PERSISTENCE_XML));
        //        System.setProperty(PersistenceUnitProperties.ECLIPSELINK_PERSISTENCE_XML, DataManager.getInstance().getConfiguration().getConfigLocalPath() + "persistence.xml");
        //        logger.debug(System.getProperty(PersistenceUnitProperties.ECLIPSELINK_PERSISTENCE_XML));
        String persistenceUnitName = inPersistenceUnitName;
        if (StringUtils.isEmpty(persistenceUnitName)) {
            persistenceUnitName = DEFAULT_PERSISTENCE_UNIT_NAME;
        }
        logger.info("Using persistence unit: {}", persistenceUnitName);
        try {
            // Create EntityManagerFactory in a custom class loader
            final Thread currentThread = Thread.currentThread();
            final ClassLoader saveClassLoader = currentThread.getContextClassLoader();
            currentThread.setContextClassLoader(new JPAClassLoader(saveClassLoader));
            factory = Persistence.createEntityManagerFactory(persistenceUnitName);
            currentThread.setContextClassLoader(saveClassLoader);

            em = factory.createEntityManager();
            preQuery();
        } catch (DatabaseException | PersistenceException e) {
            logger.error(e.getMessage(), e);
            throw new DAOException(e.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getAllUsers(boolean)
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<User> getAllUsers(boolean refresh) throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT u FROM User u");
        if (refresh) {
            q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        }
        return q.getResultList();
    }

    /**
     * @see io.goobi.viewer.dao.IDAO#getUserCount(java.util.Map)
     * @should return correct count
     * @should filter correctly
     */
    @Override
    public long getUserCount(Map<String, String> filters) throws DAOException {
        return getRowCount("User", null, filters);
    }

    /**
     * @throws DAOException
     * @see io.goobi.viewer.dao.IDAO#getUsers(int, int, java.lang.String, boolean, java.util.Map)
     * @should sort results correctly
     * @should filter results correctly
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<User> getUsers(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters) throws DAOException {
        preQuery();
        StringBuilder sbQuery = new StringBuilder("SELECT o FROM User o");
        List<String> filterKeys = new ArrayList<>();
        if (filters != null && !filters.isEmpty()) {
            sbQuery.append(" WHERE ");
            filterKeys.addAll(filters.keySet());
            Collections.sort(filterKeys);
            int count = 0;
            for (String key : filterKeys) {
                if (count > 0) {
                    sbQuery.append(" AND ");
                }

                String[] keyParts = key.split(MULTIKEY_SEPARATOR);
                int keyPartCount = 0;
                sbQuery.append(" ( ");
                for (String keyPart : keyParts) {
                    if (keyPartCount > 0) {
                        sbQuery.append(" OR ");
                    }
                    sbQuery.append("UPPER(o.").append(keyPart).append(") LIKE :").append(key.replaceAll(MULTIKEY_SEPARATOR, ""));
                    keyPartCount++;
                }
                sbQuery.append(" ) ");
                count++;
            }
        }
        if (StringUtils.isNotEmpty(sortField)) {
            sbQuery.append(" ORDER BY o.").append(sortField);
            if (descending) {
                sbQuery.append(" DESC");
            }
        }
        logger.trace(sbQuery.toString());
        Query q = em.createQuery(sbQuery.toString());
        for (String key : filterKeys) {
            q.setParameter(key.replaceAll(MULTIKEY_SEPARATOR, ""), "%" + filters.get(key).toUpperCase() + "%");
        }
        q.setFirstResult(first);
        q.setMaxResults(pageSize);
        q.setHint("javax.persistence.cache.storeMode", "REFRESH");

        return q.getResultList();
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getUser(long)
     */
    @Override
    public User getUser(long id) throws DAOException {
        preQuery();
        try {
            User o = em.getReference(User.class, id);
            if (o != null) {
                em.refresh(o);
            }
            return o;
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getUserByEmail(java.lang.String)
     */
    @Override
    public User getUserByEmail(String email) throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT u FROM User u WHERE UPPER(u.email) = :email");
        if (email != null) {
            q.setParameter("email", email.toUpperCase());
        }
        try {
            User o = (User) q.getSingleResult();
            if (o != null) {
                em.refresh(o);
            }
            return o;
        } catch (NoResultException e) {
            return null;
        } catch (NonUniqueResultException e) {
            logger.warn(e.getMessage());
            return (User) q.getResultList().get(0);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getUserByOpenId(java.lang.String)
     */
    @Override
    public User getUserByOpenId(String identifier) throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT u FROM User u WHERE :claimed_identifier MEMBER OF u.openIdAccounts");
        q.setParameter("claimed_identifier", identifier);
        q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        try {
            User o = (User) q.getSingleResult();
            if (o != null) {
                em.refresh(o);
            }
            return o;
        } catch (NoResultException e) {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getUserByNickname(java.lang.String)
     */
    @Override
    public User getUserByNickname(String nickname) throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT u FROM User u WHERE UPPER(u.nickName) = :nickname");
        if (nickname != null) {
            q.setParameter("nickname", nickname.trim().toUpperCase());
        }
        q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        try {
            User o = (User) q.getSingleResult();
            if (o != null) {
                em.refresh(o);
            }
            return o;
        } catch (NoResultException e) {
            return null;
        } catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#addUser(io.goobi.viewer.model.user.User)
     */
    @Override
    public boolean addUser(User user) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(user);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#updateUser(io.goobi.viewer.model.user.User)
     */
    @Override
    public boolean updateUser(User user) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(user);
            em.getTransaction().commit();
            // Refresh the object from the DB so that any new licenses etc. have IDs
            if (this.em.contains(user)) {
                this.em.refresh(user);
            }
            return true;
        } finally {
            em.close();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#deleteUser(io.goobi.viewer.model.user.User)
     */
    @Override
    public boolean deleteUser(User user) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            User u = em.getReference(User.class, user.getId());
            em.remove(u);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
        }
    }

    // UserGroup

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getAllUserGroups()
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<UserGroup> getAllUserGroups() throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT ug FROM UserGroup ug");
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /**
     * @throws DAOException
     * @see io.goobi.viewer.dao.IDAO#getUserGroups(int, int, java.lang.String, boolean, java.util.Map)
     * @should sort results correctly
     * @should filter results correctly
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<UserGroup> getUserGroups(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException {
        preQuery();
        StringBuilder sbQuery = new StringBuilder("SELECT o FROM UserGroup o");
        List<String> filterKeys = new ArrayList<>();
        if (filters != null && !filters.isEmpty()) {
            sbQuery.append(" WHERE ");
            filterKeys.addAll(filters.keySet());
            Collections.sort(filterKeys);
            int count = 0;
            for (String key : filterKeys) {
                if (count > 0) {
                    sbQuery.append(" AND ");
                }
                sbQuery.append("UPPER(o.").append(key).append(") LIKE :").append(key);
                count++;
            }
        }
        if (StringUtils.isNotEmpty(sortField)) {
            sbQuery.append(" ORDER BY o.").append(sortField);
            if (descending) {
                sbQuery.append(" DESC");
            }
        }
        Query q = em.createQuery(sbQuery.toString());
        for (String key : filterKeys) {
            q.setParameter(key, "%" + filters.get(key).toUpperCase() + "%");
        }
        q.setFirstResult(first);
        q.setMaxResults(pageSize);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");

        return q.getResultList();
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getUserGroups(io.goobi.viewer.model.user.User)
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<UserGroup> getUserGroups(User owner) throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT ug FROM UserGroup ug WHERE ug.owner = :owner");
        q.setParameter("owner", owner);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getUserGroup(long)
     */
    @Override
    public UserGroup getUserGroup(long id) throws DAOException {
        preQuery();
        try {
            UserGroup o = em.getReference(UserGroup.class, id);
            if (o != null) {
                em.refresh(o);
            }
            return o;
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getUserGroup(java.lang.String)
     */
    @Override
    public UserGroup getUserGroup(String name) throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT ug FROM UserGroup ug WHERE ug.name = :name");
        q.setParameter("name", name);
        try {
            UserGroup o = (UserGroup) q.getSingleResult();
            if (o != null) {
                em.refresh(o);
            }
            return o;
        } catch (NoResultException e) {
            return null;
        } catch (NonUniqueResultException e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#addUserGroup(io.goobi.viewer.model.user.UserGroup)
     */
    @Override
    public boolean addUserGroup(UserGroup userGroup) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(userGroup);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
        return true;
    }

    /**
     * (non-Javadoc)
     *
     * @throws DAOException
     *
     * @see io.goobi.viewer.dao.IDAO#updateUserGroup(io.goobi.viewer.model.security.user.UserGroup)
     * @should set id on new license
     */
    @Override
    public boolean updateUserGroup(UserGroup userGroup) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(userGroup);
            em.getTransaction().commit();
            // Refresh the object from the DB so that any new licenses etc. have IDs
            if (this.em.contains(userGroup)) {
                this.em.refresh(userGroup);
            }
            return true;
        } finally {
            em.close();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#deleteUserGroup(io.goobi.viewer.model.user.UserGroup)
     */
    @Override
    public boolean deleteUserGroup(UserGroup userGroup) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            UserGroup o = em.getReference(UserGroup.class, userGroup.getId());
            em.remove(o);
            try {
                em.getTransaction().commit();
                return true;
            } catch (RollbackException e) {
                return false;
            }
        } finally {
            em.close();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getAllBookshelves()
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Bookshelf> getAllBookshelves() throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT bs FROM Bookshelf bs");
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getPublicBookshelves()
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Bookshelf> getPublicBookshelves() throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT o FROM Bookshelf o WHERE o.isPublic=true");
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getBookshelves(io.goobi.viewer.model.user.User)
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Bookshelf> getBookshelves(User user) throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT bs FROM Bookshelf bs WHERE bs.owner = :user");
        q.setParameter("user", user);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getBookshelf(long)
     */
    @Override
    public Bookshelf getBookshelf(long id) throws DAOException {
        preQuery();
        try {
            Bookshelf o = em.getReference(Bookshelf.class, id);
            if (o != null) {
                em.refresh(o);
            }
            return o;
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getBookshelf(java.lang.String)
     */
    @Override
    public Bookshelf getBookshelf(String name) throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT bs FROM Bookshelf bs WHERE bs.name = :name");
        q.setParameter("name", name);
        try {
            Bookshelf o = (Bookshelf) q.getSingleResult();
            if (o != null) {
                em.refresh(o);
            }
            return o;
        } catch (NoResultException e) {
            return null;
        } catch (NonUniqueResultException e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#addBookshelf(io.goobi.viewer.model.bookshelf.Bookshelf)
     */
    @Override
    public boolean addBookshelf(Bookshelf bookshelf) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(bookshelf);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#updateBookshelf(io.goobi.viewer.model.bookshelf.Bookshelf)
     */
    @Override
    public boolean updateBookshelf(Bookshelf bookshelf) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(bookshelf);
            em.getTransaction().commit();
            // Refresh the object from the DB so that any new items have IDs
            if (this.em.contains(bookshelf)) {
                this.em.refresh(bookshelf);
            }
            return true;
        } finally {
            em.close();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#deleteBookshelf(io.goobi.viewer.model.bookshelf.Bookshelf)
     */
    @Override
    public boolean deleteBookshelf(Bookshelf bookshelf) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            Bookshelf o = em.getReference(Bookshelf.class, bookshelf.getId());
            em.remove(o);
            em.getTransaction().commit();
            return true;
        } catch (RollbackException e) {
            return false;
        } finally {
            em.close();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getAllRoles()
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Role> getAllRoles() throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT r FROM Role r");
        q.setFlushMode(FlushModeType.COMMIT);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /**
     * @throws DAOException
     * @see io.goobi.viewer.dao.IDAO#getRoles(int, int, java.lang.String, boolean, java.util.Map)
     * @should sort results correctly
     * @should filter results correctly
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Role> getRoles(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters) throws DAOException {
        preQuery();
        StringBuilder sbQuery = new StringBuilder("SELECT o FROM Role o");
        List<String> filterKeys = new ArrayList<>();
        if (filters != null && !filters.isEmpty()) {
            sbQuery.append(" WHERE ");
            filterKeys.addAll(filters.keySet());
            Collections.sort(filterKeys);
            int count = 0;
            for (String key : filterKeys) {
                if (count > 0) {
                    sbQuery.append(" AND ");
                }
                sbQuery.append("UPPER(o.").append(key).append(") LIKE :").append(key);
                count++;
            }
        }
        if (StringUtils.isNotEmpty(sortField)) {
            sbQuery.append(" ORDER BY o.").append(sortField);
            if (descending) {
                sbQuery.append(" DESC");
            }
        }
        Query q = em.createQuery(sbQuery.toString());
        for (String key : filterKeys) {
            q.setParameter(key, "%" + filters.get(key).toUpperCase() + "%");
        }
        q.setFirstResult(first);
        q.setMaxResults(pageSize);
        q.setFlushMode(FlushModeType.COMMIT);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");

        return q.getResultList();
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getRole(long)
     */
    @Override
    public Role getRole(long id) throws DAOException {
        preQuery();
        try {
            Role o = em.getReference(Role.class, id);
            if (o != null) {
                em.refresh(o);
            }
            return o;
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getRole(java.lang.String)
     */
    @Override
    public Role getRole(String name) throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT r FROM Role r WHERE r.name = :name");
        q.setParameter("name", name);
        try {
            Role o = (Role) q.getSingleResult();
            if (o != null) {
                em.refresh(o);
            }
            return o;
        } catch (NoResultException e) {
            return null;
        } catch (NonUniqueResultException e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#addRole(io.goobi.viewer.model.user.Role)
     */
    @Override
    public boolean addRole(Role role) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(role);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#updateRole(io.goobi.viewer.model.user.Role)
     */
    @Override
    public boolean updateRole(Role role) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(role);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#deleteRole(io.goobi.viewer.model.user.Role)
     */
    @Override
    public boolean deleteRole(Role role) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            Role o = em.getReference(Role.class, role.getId());
            em.remove(o);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getAllUserRoles()
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<UserRole> getAllUserRoles() throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT ur FROM UserRole ur");
        q.setFlushMode(FlushModeType.COMMIT);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getUserRoles(io.goobi.viewer.model.user.UserGroup,
     * io.goobi.viewer.model.user.User, io.goobi.viewer.model.user.Role)
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<UserRole> getUserRoles(UserGroup userGroup, User user, Role role) throws DAOException {
        preQuery();
        StringBuilder sbQuery = new StringBuilder("SELECT ur FROM UserRole ur");
        if (userGroup != null || user != null || role != null) {
            sbQuery.append(" WHERE ");
            int args = 0;
            if (userGroup != null) {
                sbQuery.append("ur.userGroup = :userGroup");
                args++;
            }
            if (user != null) {
                if (args > 0) {
                    sbQuery.append(" AND ");
                }
                sbQuery.append("ur.user = :user");
                args++;
            }
            if (role != null) {
                if (args > 0) {
                    sbQuery.append(" AND ");
                }
                sbQuery.append("ur.role = :role");
                args++;
            }
        }
        Query q = em.createQuery(sbQuery.toString());
        // logger.debug(sbQuery.toString());
        if (userGroup != null) {
            q.setParameter("userGroup", userGroup);
        }
        if (user != null) {
            q.setParameter("user", user);
        }
        if (role != null) {
            q.setParameter("role", role);
        }
        q.setFlushMode(FlushModeType.COMMIT);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");

        return q.getResultList();
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#addUserRole(io.goobi.viewer.model.user.UserRole)
     */
    @Override
    public boolean addUserRole(UserRole userRole) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(userRole);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#updateUserRole(io.goobi.viewer.model.user.UserRole)
     */
    @Override
    public boolean updateUserRole(UserRole userRole) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(userRole);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#deleteUserRole(io.goobi.viewer.model.user.UserRole)
     */
    @Override
    public boolean deleteUserRole(UserRole userRole) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            UserRole o = em.getReference(UserRole.class, userRole.getId());
            em.remove(o);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getAllLicenseTypes()
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<LicenseType> getAllLicenseTypes() throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT lt FROM LicenseType lt");
        q.setFlushMode(FlushModeType.COMMIT);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /**
     * @throws DAOException
     * @see io.goobi.viewer.dao.IDAO#getOpenAccessLicenseTypes()
     * @should only return non open access license types
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<LicenseType> getNonOpenAccessLicenseTypes() throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            Query q = em.createQuery("SELECT lt FROM LicenseType lt WHERE lt.openAccess = :openAccess AND lt.core = false");
            q.setParameter("openAccess", false);
            q.setFlushMode(FlushModeType.COMMIT);
            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * @throws DAOException
     * @see io.goobi.viewer.dao.IDAO#getLicenseTypes(int, int, java.lang.String, boolean, java.util.Map)
     * @should sort results correctly
     * @should filter results correctly
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<LicenseType> getLicenseTypes(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException {
        preQuery();
        StringBuilder sbQuery = new StringBuilder("SELECT o FROM LicenseType o WHERE o.core=false");
        List<String> filterKeys = new ArrayList<>();
        if (filters != null && !filters.isEmpty()) {
            filterKeys.addAll(filters.keySet());
            Collections.sort(filterKeys);
            int count = 0;
            for (String key : filterKeys) {
                sbQuery.append(" AND ");
                sbQuery.append("UPPER(o.").append(key).append(") LIKE :").append(key);
                count++;
            }
        }
        if (StringUtils.isNotEmpty(sortField)) {
            sbQuery.append(" ORDER BY o.").append(sortField);
            if (descending) {
                sbQuery.append(" DESC");
            }
        }
        Query q = em.createQuery(sbQuery.toString());
        for (String key : filterKeys) {
            q.setParameter(key, "%" + filters.get(key).toUpperCase() + "%");
        }
        q.setFirstResult(first);
        q.setMaxResults(pageSize);
        q.setFlushMode(FlushModeType.COMMIT);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");

        return q.getResultList();
    }

    /**
     * @throws DAOException
     * @see io.goobi.viewer.dao.IDAO#getCoreLicenseTypes(int, int, java.lang.String, boolean, java.util.Map)
     * @should sort results correctly
     * @should filter results correctly
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<LicenseType> getCoreLicenseTypes(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException {
        preQuery();
        StringBuilder sbQuery = new StringBuilder("SELECT o FROM LicenseType o WHERE o.core=true");
        List<String> filterKeys = new ArrayList<>();
        if (filters != null && !filters.isEmpty()) {
            filterKeys.addAll(filters.keySet());
            Collections.sort(filterKeys);
            int count = 0;
            for (String key : filterKeys) {
                sbQuery.append(" AND ");
                sbQuery.append("UPPER(o.").append(key).append(") LIKE :").append(key);
                count++;
            }
        }
        if (StringUtils.isNotEmpty(sortField)) {
            sbQuery.append(" ORDER BY o.").append(sortField);
            if (descending) {
                sbQuery.append(" DESC");
            }
        }
        Query q = em.createQuery(sbQuery.toString());
        for (String key : filterKeys) {
            q.setParameter(key, "%" + filters.get(key).toUpperCase() + "%");
        }
        q.setFirstResult(first);
        q.setMaxResults(pageSize);
        q.setFlushMode(FlushModeType.COMMIT);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");

        return q.getResultList();
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getLicenseType(long)
     */
    @Override
    public LicenseType getLicenseType(long id) throws DAOException {
        preQuery();
        try {
            LicenseType o = em.getReference(LicenseType.class, id);
            if (o != null) {
                em.refresh(o);
            }
            return o;
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getLicenseType(java.lang.String)
     */
    @Override
    public LicenseType getLicenseType(String name) throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT lt FROM LicenseType lt WHERE lt.name = :name");
        q.setParameter("name", name);
        q.setFlushMode(FlushModeType.COMMIT);
        try {
            LicenseType o = (LicenseType) q.getSingleResult();
            if (o != null) {
                em.refresh(o);
            }
            return o;
        } catch (NoResultException e) {
            return null;
        } catch (NonUniqueResultException e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#addLicenseType(io.goobi.viewer.model.user.LicenseType)
     */
    @Override
    public boolean addLicenseType(LicenseType licenseType) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(licenseType);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#updateLicenseType(io.goobi.viewer.model.user.LicenseType)
     */
    @Override
    public boolean updateLicenseType(LicenseType licenseType) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(licenseType);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#deleteLicenseType(io.goobi.viewer.model.user.LicenseType)
     */
    @Override
    public boolean deleteLicenseType(LicenseType licenseType) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            LicenseType o = em.getReference(LicenseType.class, licenseType.getId());
            em.remove(o);
            try {
                em.getTransaction().commit();
                return true;
            } catch (RollbackException e) {
                return false;
            }
        } finally {
            em.close();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getAllIpRanges()
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<IpRange> getAllIpRanges() throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT ipr FROM IpRange ipr");
        q.setFlushMode(FlushModeType.COMMIT);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /**
     * @throws DAOException
     * @see io.goobi.viewer.dao.IDAO#getIpRanges(int, int, java.lang.String, boolean, java.util.Map)
     * @should sort results correctly
     * @should filter results correctly
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<IpRange> getIpRanges(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters) throws DAOException {
        preQuery();
        StringBuilder sbQuery = new StringBuilder("SELECT o FROM IpRange o");
        List<String> filterKeys = new ArrayList<>();
        if (filters != null && !filters.isEmpty()) {
            sbQuery.append(" WHERE ");
            filterKeys.addAll(filters.keySet());
            Collections.sort(filterKeys);
            int count = 0;
            for (String key : filterKeys) {
                if (count > 0) {
                    sbQuery.append(" AND ");
                }
                sbQuery.append("UPPER(o.").append(key).append(") LIKE :").append(key);
                count++;
            }
        }
        if (StringUtils.isNotEmpty(sortField)) {
            sbQuery.append(" ORDER BY o.").append(sortField);
            if (descending) {
                sbQuery.append(" DESC");
            }
        }
        Query q = em.createQuery(sbQuery.toString());
        for (String key : filterKeys) {
            q.setParameter(key, "%" + filters.get(key).toUpperCase() + "%");
        }
        q.setFirstResult(first);
        q.setMaxResults(pageSize);
        q.setFlushMode(FlushModeType.COMMIT);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");

        return q.getResultList();
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getIpRange(long)
     */
    @Override
    public IpRange getIpRange(long id) throws DAOException {
        preQuery();
        try {
            IpRange o = em.find(IpRange.class, id);
            if (o != null) {
                em.refresh(o);
            }
            return o;
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#getIpRange(java.lang.String)
     */
    @Override
    public IpRange getIpRange(String name) throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT ipr FROM IpRange ipr WHERE ipr.name = :name");
        q.setParameter("name", name);
        try {
            IpRange o = (IpRange) q.getSingleResult();
            if (o != null) {
                em.refresh(o);
            }
            return o;
        } catch (NoResultException e) {
            return null;
        } catch (NonUniqueResultException e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#addIpRange(io.goobi.viewer.model.user.IpRange)
     */
    @Override
    public boolean addIpRange(IpRange ipRange) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(ipRange);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#updateIpRange(io.goobi.viewer.model.user.IpRange)
     */
    @Override
    public boolean updateIpRange(IpRange ipRange) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(ipRange);
            em.getTransaction().commit();
            // Refresh the object from the DB so that any new licenses etc. have IDs
            if (this.em.contains(ipRange)) {
                this.em.refresh(ipRange);
            }
            return true;
        } finally {
            em.close();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#deleteIpRange(io.goobi.viewer.model.user.IpRange)
     */
    @Override
    public boolean deleteIpRange(IpRange ipRange) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            IpRange o = em.getReference(IpRange.class, ipRange.getId());
            em.remove(o);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getAllComments()
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Comment> getAllComments() throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT o FROM Comment o");
        q.setFlushMode(FlushModeType.COMMIT);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /**
     * @throws DAOException
     * @see io.goobi.viewer.dao.IDAO#getComments(int, int, java.lang.String, boolean, java.util.Map)
     * @should sort results correctly
     * @should filter results correctly
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Comment> getComments(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters) throws DAOException {
        preQuery();
        StringBuilder sbQuery = new StringBuilder("SELECT o FROM Comment o");
        List<String> filterKeys = new ArrayList<>();
        if (filters != null && !filters.isEmpty()) {
            sbQuery.append(" WHERE ");
            filterKeys.addAll(filters.keySet());
            Collections.sort(filterKeys);
            int count = 0;
            for (String key : filterKeys) {
                if (count > 0) {
                    sbQuery.append(" AND ");
                }
                sbQuery.append("UPPER(o.").append(key).append(") LIKE :").append(key);
                count++;
            }
        }
        if (StringUtils.isNotEmpty(sortField)) {
            sbQuery.append(" ORDER BY o.").append(sortField);
            if (descending) {
                sbQuery.append(" DESC");
            }
        }
        Query q = em.createQuery(sbQuery.toString());
        for (String key : filterKeys) {
            q.setParameter(key, "%" + filters.get(key).toUpperCase() + "%");
        }
        q.setFirstResult(first);
        q.setMaxResults(pageSize);
        q.setFlushMode(FlushModeType.COMMIT);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");

        return q.getResultList();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getCommentsForPage(java.lang.String, int)
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Comment> getCommentsForPage(String pi, int page, boolean topLevelOnly) throws DAOException {
        preQuery();
        StringBuilder sbQuery = new StringBuilder(80);
        sbQuery.append("SELECT o FROM Comment o WHERE o.pi = :pi AND o.page = :page");
        if (topLevelOnly) {
            sbQuery.append(" AND o.parent IS NULL");
        }
        Query q = em.createQuery(sbQuery.toString());
        q.setParameter("pi", pi);
        q.setParameter("page", page);
        q.setFlushMode(FlushModeType.COMMIT);
        q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getComment(long)
     */
    @Override
    public Comment getComment(long id) throws DAOException {
        preQuery();
        try {
            Comment o = em.getReference(Comment.class, id);
            if (o != null) {
                em.refresh(o);
            }
            return o;
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#addComment(io.goobi.viewer.model.annotation.Comment)
     */
    @Override
    public boolean addComment(Comment comment) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(comment);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
        return true;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#updateComment(io.goobi.viewer.model.annotation.Comment)
     */
    @Override
    public boolean updateComment(Comment comment) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(comment);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#deleteComment(io.goobi.viewer.model.annotation.Comment)
     */
    @Override
    public boolean deleteComment(Comment comment) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            Comment o = em.getReference(Comment.class, comment.getId());
            em.remove(o);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
        }
    }

    /**
     * Gets all page numbers (order) within a work with the given pi which contain comments
     * 
     * @param pi
     * @return
     * @throws DAOException
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Integer> getPagesWithComments(String pi) throws DAOException {
        preQuery();
        StringBuilder sbQuery = new StringBuilder(80);
        sbQuery.append("SELECT o.page FROM Comment o WHERE o.pi = :pi");
        Query q = em.createQuery(sbQuery.toString());
        q.setParameter("pi", pi);
        q.setFlushMode(FlushModeType.COMMIT);
        q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        List<Integer> results = q.getResultList();
        return results.stream().distinct().sorted().collect(Collectors.toList());
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getAllSearches()
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Search> getAllSearches() throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT o FROM Search o");
        q.setFlushMode(FlushModeType.COMMIT);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /**
     * @see io.goobi.viewer.dao.IDAO#getSearchCount(io.goobi.viewer.model.security.user.User, java.util.Map)
     * @should filter results correctly
     */
    @Override
    public long getSearchCount(User owner, Map<String, String> filters) throws DAOException {
        preQuery();
        StringBuilder sbQuery = new StringBuilder(50);
        sbQuery.append("SELECT COUNT(o) FROM Search o");
        if (owner != null) {
            sbQuery.append(" WHERE o.owner = :owner");
        }
        List<String> filterKeys = new ArrayList<>();
        if (filters != null && !filters.isEmpty()) {
            if (owner == null) {
                sbQuery.append(" WHERE ");
            } else {
                sbQuery.append(" AND ");
            }
            filterKeys.addAll(filters.keySet());
            Collections.sort(filterKeys);
            int count = 0;
            for (String key : filterKeys) {
                if (count > 0) {
                    sbQuery.append(" AND ");
                }
                sbQuery.append("UPPER(o.").append(key).append(") LIKE :").append(key);
                count++;
            }
        }
        Query q = em.createQuery(sbQuery.toString());
        if (owner != null) {
            q.setParameter("owner", owner);
        }
        for (String key : filterKeys) {
            q.setParameter(key, "%" + filters.get(key).toUpperCase() + "%");
        }
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");

        Object o = q.getResultList().get(0);
        // MySQL
        if (o instanceof BigInteger) {
            return ((BigInteger) q.getResultList().get(0)).longValue();
        }
        // H2
        return (long) q.getResultList().get(0);
    }

    /**
     * @throws DAOException
     * @see io.goobi.viewer.dao.IDAO#getSearches(int, int, java.lang.String, boolean, java.util.Map)
     * @should sort results correctly
     * @should filter results correctly
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Search> getSearches(User owner, int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException {
        preQuery();
        StringBuilder sbQuery = new StringBuilder(50);
        sbQuery.append("SELECT o FROM Search o");
        if (owner != null) {
            sbQuery.append(" WHERE o.owner = :owner");
        }
        List<String> filterKeys = new ArrayList<>();
        if (filters != null && !filters.isEmpty()) {
            if (owner == null) {
                sbQuery.append(" WHERE ");
            } else {
                sbQuery.append(" AND ");
            }
            filterKeys.addAll(filters.keySet());
            Collections.sort(filterKeys);
            int count = 0;
            for (String key : filterKeys) {
                if (count > 0) {
                    sbQuery.append(" AND ");
                }
                sbQuery.append("UPPER(o.").append(key).append(") LIKE :").append(key);
                count++;
            }
        }
        if (StringUtils.isNotEmpty(sortField)) {
            sbQuery.append(" ORDER BY o.").append(sortField);
            if (descending) {
                sbQuery.append(" DESC");
            }
        }
        Query q = em.createQuery(sbQuery.toString());
        if (owner != null) {
            q.setParameter("owner", owner);
        }
        for (String key : filterKeys) {
            q.setParameter(key, "%" + filters.get(key).toUpperCase() + "%");
        }
        q.setFirstResult(first);
        q.setMaxResults(pageSize);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");

        return q.getResultList();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getSearches(io.goobi.viewer.model.user.User)
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Search> getSearches(User owner) throws DAOException {
        preQuery();
        String query = "SELECT o FROM Search o WHERE o.owner = :owner";
        Query q = em.createQuery(query);
        q.setParameter("owner", owner);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getSearch(long)
     */
    @Override
    public Search getSearch(long id) throws DAOException {
        preQuery();
        try {
            Search o = em.find(Search.class, id);
            if (o != null) {
                em.refresh(o);
            }
            return o;
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#addSearch(io.goobi.viewer.model.search.Search)
     */
    @Override
    public boolean addSearch(Search search) throws DAOException {
        logger.debug("addSearch: {}", search.getQuery());
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(search);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
        return true;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#updateSearch(io.goobi.viewer.model.search.Search)
     */
    @Override
    public boolean updateSearch(Search search) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(search);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#deleteSearch(io.goobi.viewer.model.search.Search)
     */
    @Override
    public boolean deleteSearch(Search search) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            Search o = em.getReference(Search.class, search.getId());
            em.remove(o);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
        }
    }

    // Overview page

    /**
     * @throws DAOException
     * @see io.goobi.viewer.dao.IDAO#getOverviewPage(long)
     * @should load overview page correctly
     */
    @Override
    public OverviewPage getOverviewPage(long id) throws DAOException {
        preQuery();
        try {
            OverviewPage o = em.find(OverviewPage.class, id);
            if (o != null) {
                em.refresh(o);
            }
            return o;
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    /**
     * @throws DAOException
     * @see io.goobi.viewer.dao.IDAO#getOverviewPageForRecord(java.lang.String)
     * @should load overview page correctly
     * @should filter by date range correctly
     */
    @Override
    public OverviewPage getOverviewPageForRecord(String pi, Date fromDate, Date toDate) throws DAOException {
        if (pi == null) {
            throw new IllegalArgumentException("pi may not be null");
        }

        preQuery();
        StringBuilder sbQuery = new StringBuilder("SELECT o FROM OverviewPage o WHERE o.pi = :pi");
        if (fromDate != null || toDate != null) {
            // To filter by date, look up OverviewPageUpdate rows that have a datestamp in the requested time frame
            sbQuery.append(" AND o.pi = (SELECT DISTINCT u.pi FROM OverviewPageUpdate u WHERE u.pi = :pi");
            if (fromDate != null) {
                sbQuery.append(" AND u.dateUpdated >= :fromDate");
            }
            if (toDate != null) {
                sbQuery.append(" AND u.dateUpdated <= :toDate");
            }
            sbQuery.append(')');
        }
        //        logger.trace(sbQuery.toString());
        synchronized (overviewPageRequestLock) {
            Query q = em.createQuery(sbQuery.toString());
            q.setParameter("pi", pi);
            if (fromDate != null) {
                q.setParameter("fromDate", fromDate);
            }
            if (toDate != null) {
                q.setParameter("toDate", toDate);
            }
            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
            try {
                OverviewPage o = (OverviewPage) q.getSingleResult();
                if (o != null) {
                    em.refresh(o);
                }
                return o;
            } catch (NoResultException e) {
                return null;
            } catch (Exception e) {
                logger.error(e.getMessage());
                return null;
            }
        }
    }

    /**
     * @throws DAOException
     * @see io.goobi.viewer.dao.IDAO#addOverviewPage(io.goobi.viewer.model.overviewpage.OverviewPage)
     * @should add overview page correctly
     */
    @Override
    public boolean addOverviewPage(OverviewPage overviewPage) throws DAOException {
        logger.trace("addOverviewPage");
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(overviewPage);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
        return true;
    }

    /**
     * @throws DAOException
     * @see io.goobi.viewer.dao.IDAO#updateOverviewPage(io.goobi.viewer.model.overviewpage.OverviewPage)
     * @should update overview page correctly
     */
    @Override
    public boolean updateOverviewPage(OverviewPage overviewPage) throws DAOException {
        logger.trace("updateOverviewPage: {}", overviewPage.getId());
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(overviewPage);
            em.getTransaction().commit();
            logger.debug("New ID: {}", overviewPage.getId());
            return true;
        } finally {
            em.close();
        }
    }

    /**
     * @throws DAOException
     * @see io.goobi.viewer.dao.IDAO#deleteOverviewPage(io.goobi.viewer.model.overviewpage.OverviewPage)
     * @should delete overview page correctly
     */
    @Override
    public boolean deleteOverviewPage(OverviewPage overviewPage) throws DAOException {
        logger.trace("deleteOverviewPage: {}", overviewPage.getId());
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            OverviewPage o = em.getReference(OverviewPage.class, overviewPage.getId());
            em.remove(o);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getOverviewPageCount(java.util.Date, java.util.Date)
     */
    @Override
    public long getOverviewPageCount(Date fromDate, Date toDate) throws DAOException {
        preQuery();
        StringBuilder sbQuery = new StringBuilder("SELECT COUNT(o) FROM OverviewPage o");
        if (fromDate != null) {
            sbQuery.append(" WHERE o.dateUpdated >= :fromDate");
        }
        if (toDate != null) {
            sbQuery.append(fromDate == null ? " WHERE " : " AND ").append("o.dateUpdated <= :toDate");
        }
        Query q = em.createQuery(sbQuery.toString());
        if (fromDate != null) {
            q.setParameter("fromDate", fromDate);
        }
        if (toDate != null) {
            q.setParameter("toDate", toDate);
        }

        Object o = q.getResultList().get(0);
        // MySQL
        if (o instanceof BigInteger) {
            return ((BigInteger) q.getResultList().get(0)).longValue();
        }
        // H2
        return (long) q.getResultList().get(0);
    }

    /**
     * @see io.goobi.viewer.dao.IDAO#getRecordsWithOverviewPages(java.util.Date, java.util.Date)
     * @should return all overview pages
     * @should paginate results correctly
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<OverviewPage> getOverviewPages(int first, int pageSize, Date fromDate, Date toDate) throws DAOException {
        preQuery();
        StringBuilder sbQuery = new StringBuilder("SELECT o FROM OverviewPage o");
        if (fromDate != null) {
            sbQuery.append(" WHERE o.dateUpdated >= :fromDate");
        }
        if (toDate != null) {
            sbQuery.append(fromDate == null ? " WHERE " : " AND ").append("o.dateUpdated <= :toDate");
        }
        sbQuery.append(" ORDER BY o.dateUpdated DESC");
        Query q = em.createQuery(sbQuery.toString());
        if (fromDate != null) {
            q.setParameter("fromDate", fromDate);
        }
        if (toDate != null) {
            q.setParameter("toDate", toDate);
        }
        q.setFirstResult(first);
        q.setMaxResults(pageSize);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");

        return q.getResultList();
    }

    /**
     * (non-Javadoc)
     *
     * @throws DAOException
     *
     * @see io.goobi.viewer.dao.IDAO#getOverviewPageUpdatesForRecord(java.lang.String)
     * @should return all updates for record
     * @should sort updates by date descending
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<OverviewPageUpdate> getOverviewPageUpdatesForRecord(String pi) throws DAOException {
        if (pi == null) {
            throw new IllegalArgumentException("pi may not be null");
        }
        preQuery();

        StringBuilder sbQuery = new StringBuilder("SELECT o FROM OverviewPageUpdate o WHERE o.pi = :pi");
        sbQuery.append(" ORDER BY o.dateUpdated desc");
        Query q = em.createQuery(sbQuery.toString());
        q.setParameter("pi", pi);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /**
     * @see io.goobi.viewer.dao.IDAO#isOverviewPageHasUpdates(java.lang.String, java.util.Date, java.util.Date)
     * @should return status correctly
     */
    @Override
    public boolean isOverviewPageHasUpdates(String pi, Date fromDate, Date toDate) throws DAOException {
        if (pi == null) {
            throw new IllegalArgumentException("pi may not be null");
        }

        preQuery();
        StringBuilder sbQuery = new StringBuilder("SELECT COUNT(o) FROM OverviewPage o WHERE o.pi = :pi");
        if (fromDate != null) {
            sbQuery.append(" AND o.dateUpdated >= :fromDate");
        }
        if (toDate != null) {
            sbQuery.append(" AND o.dateUpdated <= :toDate");
        }
        Query q = em.createQuery(sbQuery.toString());
        q.setParameter("pi", pi);
        if (fromDate != null) {
            q.setParameter("fromDate", fromDate);
        }
        if (toDate != null) {
            q.setParameter("toDate", toDate);
        }
        q.setMaxResults(1);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");

        return (long) q.getSingleResult() != 0;
    }

    /**
     * @throws DAOException
     * @see io.goobi.viewer.dao.IDAO#getOverviewPageUpdate(long)
     * @should load object correctly
     */
    @Override
    public OverviewPageUpdate getOverviewPageUpdate(long id) throws DAOException {
        preQuery();
        try {
            OverviewPageUpdate o = em.find(OverviewPageUpdate.class, id);
            if (o != null) {
                em.refresh(o);
            }
            return o;
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    /**
     * @throws DAOException
     * @see io.goobi.viewer.dao.IDAO#addOverviewPageUpdate(io.goobi.viewer.model.search.Search)
     * @should add update correctly
     */
    @Override
    public boolean addOverviewPageUpdate(OverviewPageUpdate update) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(update);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
        return true;
    }

    /**
     * @throws DAOException
     * @see io.goobi.viewer.dao.IDAO#deleteOverviewPageUpdate(io.goobi.viewer.model.search.Search)
     * @should delete update correctly
     */
    @Override
    public boolean deleteOverviewPageUpdate(OverviewPageUpdate update) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            OverviewPageUpdate o = em.getReference(OverviewPageUpdate.class, update.getId());
            em.remove(o);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
        }
    }

    // Downloads

    /**
     * @see io.goobi.viewer.dao.IDAO#getAllDownloadJobs()
     * @should return all objects
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<DownloadJob> getAllDownloadJobs() throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT o FROM DownloadJob o");
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /**
     * @see io.goobi.viewer.dao.IDAO#getDownloadJob(long)
     * @should return correct object
     */
    @Override
    public DownloadJob getDownloadJob(long id) throws DAOException {
        preQuery();
        try {
            DownloadJob o = em.getReference(DownloadJob.class, id);
            if (o != null) {
                em.refresh(o);
            }
            return o;
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    /**
     * @see io.goobi.viewer.dao.IDAO#getDownloadJobByIdentifier(java.lang.String)
     * @should return correct object
     */
    @Override
    public DownloadJob getDownloadJobByIdentifier(String identifier) throws DAOException {
        if (identifier == null) {
            throw new IllegalArgumentException("identifier may not be null");
        }

        preQuery();
        StringBuilder sbQuery = new StringBuilder();
        sbQuery.append("SELECT o FROM DownloadJob o WHERE o.identifier = :identifier");
        Query q = em.createQuery(sbQuery.toString());
        q.setParameter("identifier", identifier);
        q.setMaxResults(1);
        try {
            DownloadJob o = (DownloadJob) q.getSingleResult();
            if (o != null) {
                em.refresh(o);
            }
            return o;
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * @see io.goobi.viewer.dao.IDAO#getDownloadJobByMetadata(java.lang.String, java.lang.String)
     * @should return correct object
     */
    @Override
    public DownloadJob getDownloadJobByMetadata(String type, String pi, String logId) throws DAOException {
        if (type == null) {
            throw new IllegalArgumentException("type may not be null");
        }
        if (pi == null) {
            throw new IllegalArgumentException("pi may not be null");
        }

        preQuery();
        StringBuilder sbQuery = new StringBuilder();
        sbQuery.append("SELECT o FROM DownloadJob o WHERE o.type = :type AND o.pi = :pi");
        if (logId != null) {
            sbQuery.append(" AND o.logId = :logId");
        }
        Query q = em.createQuery(sbQuery.toString());
        q.setParameter("type", type);
        q.setParameter("pi", pi);
        if (logId != null) {
            q.setParameter("logId", logId);
        }
        q.setMaxResults(1);
        try {
            DownloadJob o = (DownloadJob) q.getSingleResult();
            if (o != null) {
                em.refresh(o);
            }
            return o;
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * @see io.goobi.viewer.dao.IDAO#addDownloadJob(io.goobi.viewer.model.download.DownloadJob)
     * @should add object correctly
     */
    @Override
    public boolean addDownloadJob(DownloadJob downloadJob) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(downloadJob);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
        return true;
    }

    /**
     * @see io.goobi.viewer.dao.IDAO#updateDownloadJob(io.goobi.viewer.model.download.DownloadJob)
     * @should update object correctly
     */
    @Override
    public boolean updateDownloadJob(DownloadJob downloadJob) throws DAOException {
        logger.trace("updateDownloadJob: {}", downloadJob.getId());
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(downloadJob);
            em.getTransaction().commit();

            if (this.em.contains(downloadJob)) {
                this.em.refresh(downloadJob);
            }

            return true;
        } finally {
            em.close();
        }
    }

    /**
     * @see io.goobi.viewer.dao.IDAO#deleteDownloadJob(io.goobi.viewer.model.download.DownloadJob)
     * @should delete object correctly
     */
    @Override
    public boolean deleteDownloadJob(DownloadJob downloadJob) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            DownloadJob o = em.getReference(DownloadJob.class, downloadJob.getId());
            em.remove(o);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
        }
    }

    /**
     * @throws DAOException
     * @see io.goobi.viewer.dao.IDAO#getAllCMSPages()
     * @should return all pages
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSPage> getAllCMSPages() throws DAOException {
        try {
            synchronized (cmsRequestLock) {
                preQuery();
                Query q = em.createQuery("SELECT o FROM CMSPage o");
                q.setFlushMode(FlushModeType.COMMIT);
                // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
                return q.getResultList();
            }
        } catch (PersistenceException e) {
            logger.error("Exception \"" + e.toString() + "\" when trying to get cms pages. Returning empty list");
            return new ArrayList<>();
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getCmsPageForStaticPage(java.lang.String)
     */
    @Override
    public CMSPage getCmsPageForStaticPage(String pageName) throws DAOException {
        synchronized (cmsRequestLock) {
            preQuery();
            Query q = em.createQuery("SELECT o FROM CMSPage o WHERE o.staticPageName = :pageName");
            q.setParameter("pageName", pageName);
            q.setHint("javax.persistence.cache.storeMode", "REFRESH");
            if (!q.getResultList().isEmpty()) {
                return (CMSPage) q.getSingleResult();
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getCMSPages(int, int, java.lang.String, boolean, java.util.Map)
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSPage> getCMSPages(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters,
            List<String> allowedTemplates, List<String> allowedSubthemes, List<String> allowedCategories) throws DAOException {
        synchronized (cmsRequestLock) {
            try {
                preQuery();
                StringBuilder sbQuery = new StringBuilder("SELECT DISTINCT a FROM CMSPage a");
                StringBuilder order = new StringBuilder();

                Map<String, String> params = new HashMap<>();

                String filterString = createFilterQuery(null, filters, params);
                String rightsFilterString;
                try {
                    rightsFilterString = createCMSPageFilter(params, "a", allowedTemplates, allowedSubthemes, allowedCategories);
                    if (!rightsFilterString.isEmpty()) {
                        rightsFilterString = (StringUtils.isBlank(filterString) ? " WHERE " : " AND ") + rightsFilterString;
                    }
                } catch (AccessDeniedException e) {
                    //may not request any cms pages at all
                    return Collections.emptyList();
                }

                if (StringUtils.isNotEmpty(sortField)) {
                    order.append(" ORDER BY a.").append(sortField);
                    if (descending) {
                        order.append(" DESC");
                    }
                }
                sbQuery.append(filterString).append(rightsFilterString).append(order);

                Query q = em.createQuery(sbQuery.toString());
                params.entrySet().forEach(entry -> q.setParameter(entry.getKey(), entry.getValue()));
                //            q.setParameter("lang", BeanUtils.getLocale().getLanguage());
                q.setFirstResult(first);
                q.setMaxResults(pageSize);
                q.setFlushMode(FlushModeType.COMMIT);
                // q.setHint("javax.persistence.cache.storeMode", "REFRESH");

                List<CMSPage> list = q.getResultList();
                return list;
            } catch (PersistenceException e) {
                logger.error("Exception \"" + e.toString() + "\" when trying to get cms pages. Returning empty list");
                return new ArrayList<>();
            }
        }
    }

    /**
     * Builds a query string to filter a query across several tables
     * 
     * @param filters The filters to use
     * @param params Empty map which will be filled with the used query parameters. These to be added to the query
     * @return A string consisting of a WHERE and possibly JOIN clause of a query
     */
    public String createFilterQuery(String staticFilterQuery, Map<String, String> filters, Map<String, String> params) {
        StringBuilder join = new StringBuilder();

        List<String> filterKeys = new ArrayList<>();
        StringBuilder where = new StringBuilder();
        if (StringUtils.isNotEmpty(staticFilterQuery)) {
            where.append(staticFilterQuery);
        }
        if (filters != null && !filters.isEmpty()) {
            AlphabetIterator abc = new AlphabetIterator();
            String pageKey = abc.next();
            filterKeys.addAll(filters.keySet());
            Collections.sort(filterKeys);
            int count = 0;
            for (String key : filterKeys) {
                String tableKey = pageKey;
                String value = filters.get(key);
                if (StringUtils.isNotBlank(value)) {
                    //separate join table statement from key
                    String joinTable = "";
                    if (key.contains("::")) {
                        joinTable = key.substring(0, key.indexOf("::"));
                        key = key.substring(key.indexOf("::") + 2);
                        tableKey = abc.next();
                    }
                    if (count > 0 || StringUtils.isNotEmpty(staticFilterQuery)) {
                        where.append(" AND (");
                    } else {
                        where.append(" WHERE ");
                    }
                    String[] keyParts = key.split(MULTIKEY_SEPARATOR);
                    int keyPartCount = 0;
                    where.append(" ( ");
                    for (String keyPart : keyParts) {
                        if (keyPartCount > 0) {
                            where.append(" OR ");
                        }
                        if ("CMSPageLanguageVersion".equalsIgnoreCase(joinTable) || "CMSSidebarElement".equalsIgnoreCase(joinTable)) {
                            where.append("UPPER(" + tableKey + ".").append(keyPart).append(") LIKE :").append(key.replaceAll(MULTIKEY_SEPARATOR, ""));
                        } else if ("categories".equals(joinTable)) {
                            where.append(tableKey).append(" LIKE :").append(key.replaceAll(MULTIKEY_SEPARATOR, ""));

                        } else {
                            where.append("UPPER(" + tableKey + ".").append(keyPart).append(") LIKE :").append(key.replaceAll(MULTIKEY_SEPARATOR, ""));
                        }
                        keyPartCount++;
                    }
                    where.append(" ) ");
                    count++;

                    //apply join table if neccessary
                    if ("CMSPageLanguageVersion".equalsIgnoreCase(joinTable) || "CMSSidebarElement".equalsIgnoreCase(joinTable)) {
                        join.append(" JOIN ")
                                .append(joinTable)
                                .append(" ")
                                .append(tableKey)
                                .append(" ON")
                                .append(" (")
                                .append(pageKey)
                                .append(".id = ")
                                .append(tableKey)
                                .append(".ownerPage.id)");
                        //                            if(joinTable.equalsIgnoreCase("CMSPageLanguageVersion")) {                                
                        //                                join.append(" AND ")
                        //                                .append(" (").append(tableKey).append(".language = :lang) ");
                        //                            }
                    } else if ("classifications".equals(joinTable)) {
                        join.append(" JOIN ").append(pageKey).append(".").append(joinTable).append(" ").append(tableKey);
                        //                            .append(" ON ").append(" (").append(pageKey).append(".id = ").append(tableKey).append(".ownerPage.id)");
                    }
                    params.put(key.replaceAll(MULTIKEY_SEPARATOR, ""), "%" + value.toUpperCase() + "%");
                }
                if (count > 1) {
                    where.append(" )");
                }
            }
        }
        String filterString = join.append(where).toString();
        return filterString;
    }

    /**
     * @throws DAOException
     * @see io.goobi.viewer.dao.IDAO#getCMSPage(long)
     * @should return correct page
     */
    @Override
    public CMSPage getCMSPage(long id) throws DAOException {
        synchronized (cmsRequestLock) {
            logger.trace("getCMSPage: {}", id);
            preQuery();
            try {
                CMSPage o = em.getReference(CMSPage.class, id);
                if (o != null) {
                    updateCMSPageFromDatabase(o.getId());
                }
                return o;
            } catch (EntityNotFoundException e) {
                return null;
            } finally {
                logger.trace("getCMSPage END");
            }
        }
    }

    /**
     * @throws DAOException
     * @see io.goobi.viewer.dao.IDAO#getCMSPage(long)
     * @should return correct page
     */
    @Override
    public CMSPage getCMSPageForEditing(long id) throws DAOException {
        CMSPage original = getCMSPage(id);
        CMSPage copy = new CMSPage(original);
        return copy;
    }

    @Override
    public CMSSidebarElement getCMSSidebarElement(long id) throws DAOException {

        synchronized (cmsRequestLock) {
            logger.trace("getCMSSidebarElement: {}", id);
            preQuery();
            try {
                CMSSidebarElement o = em.getReference(CMSSidebarElement.class, id);
                em.refresh(o);
                return o;
            } catch (EntityNotFoundException e) {
                return null;
            } finally {
                logger.trace("getCMSSidebarElement END");
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<CMSNavigationItem> getRelatedNavItem(CMSPage page) throws DAOException {
        synchronized (cmsRequestLock) {
            preQuery();
            Query q = em.createQuery("SELECT o FROM CMSNavigationItem o WHERE o.cmsPage = :page");
            q.setParameter("page", page);
            // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
            return q.getResultList();
        }
    }

    /**
     * @throws DAOException
     * @see io.goobi.viewer.dao.IDAO#addCMSPage(io.goobi.viewer.model.cms.CMSPage)
     * @should add page correctly
     */
    @Override
    public boolean addCMSPage(CMSPage page) throws DAOException {
        synchronized (cmsRequestLock) {

            preQuery();
            EntityManager em = factory.createEntityManager();
            try {
                em.getTransaction().begin();
                em.persist(page);
                em.getTransaction().commit();
                return updateCMSPageFromDatabase(page.getId());
            } finally {
                em.close();
            }
        }
    }

    /**
     * @throws DAOException
     * @see io.goobi.viewer.dao.IDAO#updateCMSPage(io.goobi.viewer.model.cms.CMSPage)
     * @should update page correctly
     */
    @Override
    public boolean updateCMSPage(CMSPage page) throws DAOException {
        synchronized (cmsRequestLock) {
            preQuery();
            EntityManager em = factory.createEntityManager();
            try {
                em.getTransaction().begin();
                em.merge(page);
                em.getTransaction().commit();
                return updateCMSPageFromDatabase(page.getId());
            } finally {
                em.close();
            }
        }
    }

    /**
     * Refresh the CMSPage with the given id from the database. If the page is not found or if the refresh fails, false is returned
     * 
     * @param id
     * @return
     */
    private boolean updateCMSPageFromDatabase(Long id) {
        Object o = null;
        try {
            o = this.em.getReference(CMSPage.class, id);
            this.em.refresh(o);
            return true;
        } catch (IllegalArgumentException e) {
            logger.error("CMSPage with ID '{}' has an invalid type, or is not persisted: {}", id, e.getMessage());
            return false;
        } catch (EntityNotFoundException e) {
            logger.debug("CMSPage with ID '{}' not found in database.", id);
            //remove from em as well
            if (o != null) {
                em.remove(o);
            }
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private boolean updateFromDatabase(Long id, Class clazz) {
        Object o = null;
        try {
            o = this.em.getReference(clazz, id);
            this.em.refresh(o);
            return true;
        } catch (IllegalArgumentException e) {
            logger.error("CMSPage with ID '{}' has an invalid type, or is not persisted: {}", id, e.getMessage());
            return false;
        } catch (EntityNotFoundException e) {
            logger.debug("CMSPage with ID '{}' not found in database.", id);
            //remove from em as well
            if (o != null) {
                em.remove(o);
            }
            return false;
        }
    }

    /**
     * @throws DAOException
     * @see io.goobi.viewer.dao.IDAO#deleteCMSPage(io.goobi.viewer.model.cms.CMSPage)
     * @should delete page correctly
     */
    @Override
    public boolean deleteCMSPage(CMSPage page) throws DAOException {
        synchronized (cmsRequestLock) {

            preQuery();
            EntityManager em = factory.createEntityManager();
            try {
                em.getTransaction().begin();
                CMSPage o = em.getReference(CMSPage.class, page.getId());
                em.remove(o);
                em.getTransaction().commit();
                return !updateCMSPageFromDatabase(o.getId());
            } catch (RollbackException e) {
                return false;
            } finally {
                em.close();
            }
        }
    }

    /**
     * @throws DAOException
     * @see io.goobi.viewer.dao.IDAO#getAllCMSMediaItems()
     * @should return all items
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSMediaItem> getAllCMSMediaItems() throws DAOException {
        synchronized (cmsRequestLock) {
            try {
                preQuery();
                Query q = em.createQuery("SELECT o FROM CMSMediaItem o");
                q.setFlushMode(FlushModeType.COMMIT);
                // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
                return q.getResultList();
            } catch (PersistenceException e) {
                logger.error("Exception \"" + e.toString() + "\" when trying to get cms pages. Returning empty list");
                return new ArrayList<>();
            }
        }
    }

    /**
     * @throws DAOException
     * @see io.goobi.viewer.dao.IDAO#getAllCMSMediaItems()
     * @should return all items
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSMediaItem> getAllCMSCollectionItems() throws DAOException {
        synchronized (cmsRequestLock) {
            try {
                preQuery();
                Query q = em.createQuery("SELECT o FROM CMSMediaItem o WHERE o.collection = true");
                q.setFlushMode(FlushModeType.COMMIT);
                // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
                return q.getResultList();
            } catch (PersistenceException e) {
                logger.error("Exception \"" + e.toString() + "\" when trying to get cms pages. Returning empty list");
                return new ArrayList<>();
            }
        }
    }

    /**
     * @throws DAOException
     * @see io.goobi.viewer.dao.IDAO#getCMSMediaItem(long)
     * @should return correct item
     */
    @Override
    public CMSMediaItem getCMSMediaItem(long id) throws DAOException {
        synchronized (cmsRequestLock) {

            preQuery();
            try {
                CMSMediaItem o = em.getReference(CMSMediaItem.class, id);
                em.refresh(o);
                return o;
            } catch (EntityNotFoundException e) {
                return null;
            }
        }
    }

    /**
     * @throws DAOException
     * @see io.goobi.viewer.dao.IDAO#addCMSMediaItem(io.goobi.viewer.model.cms.CMSMediaItem)
     * @should add item correctly
     */
    @Override
    public boolean addCMSMediaItem(CMSMediaItem item) throws DAOException {
        synchronized (cmsRequestLock) {

            preQuery();
            EntityManager em = factory.createEntityManager();
            try {
                em.getTransaction().begin();
                em.persist(item);
                em.getTransaction().commit();
                return true;
            } finally {
                em.close();
            }
        }
    }

    /**
     * @throws DAOException
     * @see io.goobi.viewer.dao.IDAO#updateCMSMediaItem(io.goobi.viewer.model.cms.CMSMediaItem)
     * @should update item correctly
     */
    @Override
    public boolean updateCMSMediaItem(CMSMediaItem item) throws DAOException {
        synchronized (cmsRequestLock) {

            preQuery();
            EntityManager em = factory.createEntityManager();
            try {
                em.getTransaction().begin();
                em.merge(item);
                em.getTransaction().commit();
                return updateFromDatabase(item.getId(), item.getClass());
            } finally {
                em.close();
            }
        }
    }

    /**
     * @throws DAOException
     * @see io.goobi.viewer.dao.IDAO#deleteCMSMediaItem(io.goobi.viewer.model.cms.CMSMediaItem)
     * @should delete item correctly
     * @should not delete referenced items
     */
    @Override
    public boolean deleteCMSMediaItem(CMSMediaItem item) throws DAOException {
        synchronized (cmsRequestLock) {

            preQuery();
            EntityManager em = factory.createEntityManager();
            try {
                em.getTransaction().begin();
                CMSMediaItem o = em.getReference(CMSMediaItem.class, item.getId());
                em.remove(o);
                em.getTransaction().commit();
                return true;
            } catch (RollbackException e) {
                logger.error(e.getMessage());
                return false;
            } finally {
                em.close();
            }
        }
    }

    @Override
    public List<CMSPage> getMediaOwners(CMSMediaItem item) throws DAOException {
        synchronized (cmsRequestLock) {

            List<CMSPage> ownerList = new ArrayList<>();
            try {
                preQuery();
                Query q = em.createQuery("SELECT o FROM CMSContentItem o WHERE o.mediaItem = :media");
                q.setParameter("media", item);
                q.setFlushMode(FlushModeType.COMMIT);
                // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
                for (Object o : q.getResultList()) {
                    if (o instanceof CMSContentItem) {
                        try {
                            CMSPage page = ((CMSContentItem) o).getOwnerPageLanguageVersion().getOwnerPage();
                            if (!ownerList.contains(page)) {
                                ownerList.add(page);
                            }
                        } catch (NullPointerException e) {
                        }
                    }
                }
                return ownerList;
            } catch (PersistenceException e) {
                logger.error("Exception \"" + e.toString() + "\" when trying to get cms pages. Returning empty list");
                return new ArrayList<>();
            }
        }
    }

    /**
     * @throws DAOException
     * @see io.goobi.viewer.dao.IDAO#getAllCMSNavigationItems()
     * @should return all top items
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSNavigationItem> getAllTopCMSNavigationItems() throws DAOException {
        preQuery();
        synchronized (cmsRequestLock) {
            try {
                Query q = em.createQuery("SELECT o FROM CMSNavigationItem o WHERE o.parentItem IS NULL");
                q.setHint("javax.persistence.cache.storeMode", "REFRESH");
                q.setFlushMode(FlushModeType.COMMIT);
                List<CMSNavigationItem> list = q.getResultList();
                Collections.sort(list);
                return list;
            } catch (PersistenceException e) {
                logger.error("Exception \"" + e.toString() + "\" when trying to get cms pages. Returning empty list");
                return new ArrayList<>();
            }
        }
    }

    /**
     * @throws DAOException
     * @see io.goobi.viewer.dao.IDAO#getCMSNavigationItem(long)
     * @should return correct item and child items
     */
    @Override
    public CMSNavigationItem getCMSNavigationItem(long id) throws DAOException {
        preQuery();
        synchronized (cmsRequestLock) {
            try {
                CMSNavigationItem o = em.find(CMSNavigationItem.class, id);
                em.refresh(o);
                return o;
            } catch (EntityNotFoundException e) {
                return null;
            }
        }
    }

    /**
     * @throws DAOException
     * @see io.goobi.viewer.dao.IDAO#addCMSNavigationItem(io.goobi.viewer.model.cms.CMSNavigationItem)
     * @should add item and child items correctly
     */
    @Override
    public boolean addCMSNavigationItem(CMSNavigationItem item) throws DAOException {
        synchronized (cmsRequestLock) {

            preQuery();
            EntityManager em = factory.createEntityManager();
            try {
                em.getTransaction().begin();
                em.persist(item);
                em.getTransaction().commit();
                return true;
            } finally {
                em.close();
            }
        }
    }

    /**
     * @throws DAOException
     * @see io.goobi.viewer.dao.IDAO#updateCMSNavigationItem(io.goobi.viewer.model.cms.CMSNavigationItem)
     * @should update item and child items correctly
     */
    @Override
    public boolean updateCMSNavigationItem(CMSNavigationItem item) throws DAOException {
        synchronized (cmsRequestLock) {

            preQuery();
            EntityManager em = factory.createEntityManager();
            try {
                em.getTransaction().begin();
                em.merge(item);
                em.getTransaction().commit();
                return true;
            } finally {
                em.close();
            }
        }
    }

    /**
     * @throws DAOException
     * @see io.goobi.viewer.dao.IDAO#deleteCMSNavigationItem(io.goobi.viewer.model.cms.CMSNavigationItem)
     * @should delete item and child items correctly
     */
    @Override
    public boolean deleteCMSNavigationItem(CMSNavigationItem item) throws DAOException {
        synchronized (cmsRequestLock) {

            preQuery();
            EntityManager em = factory.createEntityManager();
            try {
                em.getTransaction().begin();
                CMSNavigationItem o = em.getReference(CMSNavigationItem.class, item.getId());
                em.remove(o);
                em.getTransaction().commit();
                return true;
            } catch (RollbackException e) {
                return false;
            } finally {
                em.close();
            }
        }
    }

    // Transkribus

    /**
     * @see io.goobi.viewer.dao.IDAO#getAllTranskribusJobs()
     * @should return all jobs
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<TranskribusJob> getAllTranskribusJobs() throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT o FROM TranskribusJob o");
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getTranskribusJobs(java.lang.String, java.lang.String, io.goobi.viewer.model.transkribus.TranskribusJob.JobStatus)
     */
    @Override
    public List<TranskribusJob> getTranskribusJobs(String pi, String transkribusUserId, JobStatus status) throws DAOException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see io.goobi.viewer.dao.IDAO#addTranskribusJob(io.goobi.viewer.model.transkribus.TranskribusJob)
     * @should add job correctly
     */
    @Override
    public boolean addTranskribusJob(TranskribusJob job) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(job);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
        }
    }

    /**
     * @see io.goobi.viewer.dao.IDAO#updateTranskribusJob(io.goobi.viewer.model.transkribus.TranskribusJob)
     * @should update job correctly
     */
    @Override
    public boolean updateTranskribusJob(TranskribusJob job) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(job);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
        }
    }

    /**
     * @see io.goobi.viewer.dao.IDAO#deleteTranskribusJob(io.goobi.viewer.model.transkribus.TranskribusJob)
     * @should delete job correctly
     */
    @Override
    public boolean deleteTranskribusJob(TranskribusJob job) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            TranskribusJob o = em.getReference(TranskribusJob.class, job.getId());
            em.remove(o);
            em.getTransaction().commit();
            return true;
        } catch (RollbackException e) {
            return false;
        } finally {
            em.close();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.dao.IDAO#shutdown()
     */
    @Override
    public void shutdown() {
        if (em != null && em.isOpen()) {
            em.close();
        }
        if (factory != null && factory.isOpen()) {
            factory.close();
        }
        // This is MySQL specific, but needed to prevent OOMs when redeploying
        //        try {
        //            AbandonedConnectionCleanupThread.shutdown();
        //        } catch (InterruptedException e) {
        //            logger.error(e.getMessage(), e);
        //        }
    }

    public void preQuery() throws DAOException {
        if (em == null) {
            throw new DAOException("EntityManager is not initialized");
        }
        if (!em.isOpen()) {
            em = factory.createEntityManager();
        }
        //        EntityManager em = factory.createEntityManager();
        //        try {
        //            Query q = em.createNativeQuery("SELECT 1");
        //            q.getResultList();
        //        } finally {
        //            em.close();
        //        }

    }

    /**
     * @see io.goobi.viewer.dao.IDAO#getUserGroupCount()
     * @should return correct count
     */
    @Override
    public long getUserGroupCount(Map<String, String> filters) throws DAOException {
        return getRowCount("UserGroup", null, filters);
    }

    /**
     * @see io.goobi.viewer.dao.IDAO#getRoleCount()
     * @should return correct count
     */
    @Override
    public long getRoleCount(Map<String, String> filters) throws DAOException {
        return getRowCount("Role", null, filters);
    }

    /**
     * @see io.goobi.viewer.dao.IDAO#getLicenseTypeCount()
     * @should return correct count
     */
    @Override
    public long getLicenseTypeCount(Map<String, String> filters) throws DAOException {
        return getRowCount("LicenseType", " WHERE a.core=false", filters);
    }

    /**
     * @see io.goobi.viewer.dao.IDAO#getCoreLicenseTypeCount()
     * @should return correct count
     */
    @Override
    public long getCoreLicenseTypeCount(Map<String, String> filters) throws DAOException {
        return getRowCount("LicenseType", " WHERE a.core=true", filters);
    }

    /**
     * @see io.goobi.viewer.dao.IDAO#getIpRangeCount()
     * @should return correct count
     */
    @Override
    public long getIpRangeCount(Map<String, String> filters) throws DAOException {
        return getRowCount("IpRange", null, filters);
    }

    /**
     * @see io.goobi.viewer.dao.IDAO#getCommentCount(java.util.Map)
     * @should return correct count
     * @should filter correctly
     */
    @Override
    public long getCommentCount(Map<String, String> filters) throws DAOException {
        return getRowCount("Comment", null, filters);
    }

    /**
     * @see io.goobi.viewer.dao.IDAO#getCMSPagesCount(java.util.Map)
     */
    @Override
    public long getCMSPageCount(Map<String, String> filters, List<String> allowedTemplates, List<String> allowedSubthemes,
            List<String> allowedCategories) throws DAOException {
        preQuery();
        StringBuilder sbQuery = new StringBuilder("SELECT count(a) FROM CMSPage").append(" a");
        Map<String, String> params = new HashMap<>();
        sbQuery.append(createFilterQuery(null, filters, params));
        try {
            String rightsFilter = createCMSPageFilter(params, "a", allowedTemplates, allowedSubthemes, allowedCategories);
            if (!rightsFilter.isEmpty()) {
                if (filters.values().stream().anyMatch(v -> StringUtils.isNotBlank(v))) {
                    sbQuery.append(" AND ");
                } else {
                    sbQuery.append(" WHERE ");
                }
                sbQuery.append("(").append(createCMSPageFilter(params, "a", allowedTemplates, allowedSubthemes, allowedCategories)).append(")");
            }
        } catch (AccessDeniedException e) {
            return 0;
        }
        Query q = em.createQuery(sbQuery.toString());
        params.entrySet().forEach(entry -> q.setParameter(entry.getKey(), entry.getValue()));

        return (long) q.getSingleResult();
    }

    /**
     * Universal method for returning the row count for the given class and filters.
     * 
     * @param className
     * @param staticFilterQuery Optional filter query in case the fuzzy filters aren't sufficient
     * @param filters
     * @return
     * @throws DAOException
     */
    private long getRowCount(String className, String staticFilterQuery, Map<String, String> filters) throws DAOException {
        preQuery();
        StringBuilder sbQuery = new StringBuilder("SELECT count(a) FROM ").append(className).append(" a");
        Map<String, String> params = new HashMap<>();
        Query q = em.createQuery(sbQuery.append(createFilterQuery(staticFilterQuery, filters, params)).toString());
        params.entrySet().forEach(entry -> q.setParameter(entry.getKey(), entry.getValue()));

        return (long) q.getSingleResult();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getAllStaticPages()
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<CMSStaticPage> getAllStaticPages() throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT o FROM CMSStaticPage o");
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return q.getResultList();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#addStaticPage(io.goobi.viewer.model.cms.StaticPage)
     */
    @Override
    public void addStaticPage(CMSStaticPage page) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(page);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#updateStaticPage(io.goobi.viewer.model.cms.StaticPage)
     */
    @Override
    public void updateStaticPage(CMSStaticPage page) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(page);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#deleteStaticPage(io.goobi.viewer.model.cms.StaticPage)
     */
    @Override
    public boolean deleteStaticPage(CMSStaticPage page) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            CMSStaticPage o = em.getReference(CMSStaticPage.class, page.getId());
            em.remove(o);
            em.getTransaction().commit();
            return true;
        } catch (RollbackException | EntityNotFoundException e) {
            return false;
        } finally {
            em.close();
        }
    }

    /**
     * 
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<CMSStaticPage> getStaticPageForCMSPage(CMSPage page) throws DAOException, NonUniqueResultException {
        preQuery();
        Query q = em.createQuery("SELECT sp FROM CMSStaticPage sp WHERE sp.cmsPageId = :id");
        q.setParameter("id", page.getId());
        return q.getResultList();
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        //        return getSingleResult(q);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getStaticPageForTypeType(io.goobi.viewer.dao.PageType)
     */
    @Override
    public Optional<CMSStaticPage> getStaticPageForTypeType(PageType pageType) throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT sp FROM CMSStaticPage sp WHERE sp.pageName = :name");
        q.setParameter("name", pageType.name());
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return getSingleResult(q);
    }

    /**
     * Helper method to get the only result of a query. In contrast to {@link javax.persistence.Query#getSingleResult()} this does not throw an
     * exception if no results are found. Instead, it returns an empty Optional
     * 
     * @throws ClassCastException if the first result cannot be cast to the expected type
     * @throws NonUniqueResultException if the query matches more than one result
     * @param q the query to perform
     * @return an Optional containing the query result, or an empty Optional if no results are present
     */
    @SuppressWarnings("unchecked")
    private static <T> Optional<T> getSingleResult(Query q) throws ClassCastException, NonUniqueResultException {
        List<Object> results = q.getResultList();
        if (results == null || results.isEmpty()) {
            return Optional.empty();
        } else if (results.size() > 1) {
            throw new NonUniqueResultException("Query found " + results.size() + " results instead of only one");
        } else {
            return Optional.ofNullable((T) results.get(0));
        }
    }

    /**
     * Helper method to get the first result of the given query if any results are returned, or an empty Optional otherwise
     * 
     * @throws ClassCastException if the first result cannot be cast to the expected type
     * @param q the query to perform
     * @return an Optional containing the first query result, or an empty Optional if no results are present
     */
    @SuppressWarnings("unchecked")
    private static <T> Optional<T> getFirstResult(Query q) throws ClassCastException {
        List<Object> results = q.getResultList();
        if (results == null || results.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable((T) results.get(0));
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getCMSCollections(java.lang.String)
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<CMSCollection> getCMSCollections(String solrField) throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT c FROM CMSCollection c WHERE c.solrField = :field");
        q.setParameter("field", solrField);
        return q.getResultList();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#addCMSCollection(io.goobi.viewer.model.cms.CMSCollection)
     */
    @Override
    public boolean addCMSCollection(CMSCollection collection) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(collection);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
        return true;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#updateCMSCollection(io.goobi.viewer.model.cms.CMSCollection)
     */
    @Override
    public boolean updateCMSCollection(CMSCollection collection) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(collection);
            em.getTransaction().commit();
            // Refresh the object from the DB so that any new licenses etc. have IDs
            if (this.em.contains(collection)) {
                this.em.refresh(collection);
            }
            return true;
        } finally {
            em.close();
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getCMSCollection(java.lang.String, java.lang.String)
     */
    @Override
    public CMSCollection getCMSCollection(String solrField, String solrFieldValue) throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT c FROM CMSCollection c WHERE c.solrField = :field AND c.solrFieldValue = :value");
        q.setParameter("field", solrField);
        q.setParameter("value", solrFieldValue);
        return (CMSCollection) getSingleResult(q).orElse(null);
    }

    @Override
    public void refreshCMSCollection(CMSCollection collection) throws DAOException {
        preQuery();
        this.em.refresh(collection);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#deleteCMSCollection(io.goobi.viewer.model.cms.CMSCollection)
     */
    @Override
    public boolean deleteCMSCollection(CMSCollection collection) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            CMSCollection u = em.getReference(CMSCollection.class, collection.getId());
            em.remove(u);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getCMSPagesByCategory(io.goobi.viewer.model.cms.Category)
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<CMSPage> getCMSPagesByCategory(CMSCategory category) throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT DISTINCT page FROM CMSPage page JOIN page.categories category WHERE category.id = :id");
        q.setParameter("id", category.getId());
        List<CMSPage> pageList = q.getResultList();
        return pageList;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.IDAO#getCMSPagesForRecord(java.lang.String, io.goobi.viewer.model.cms.Category)
     */
    @Override
    public List<CMSPage> getCMSPagesForRecord(String pi, CMSCategory category) throws DAOException {
        preQuery();
        Query q;
        if (category != null) {
            q = em.createQuery(
                    "SELECT DISTINCT page FROM CMSPage page JOIN page.categories category WHERE category.id = :id AND page.relatedPI = :pi");
            q.setParameter("id", category.getId());
        } else {
        	StringBuilder sbQuery = new StringBuilder(70);
            sbQuery.append("SELECT o from CMSPage o WHERE o.relatedPI='").append(pi).append("'");

            q = em.createQuery("SELECT page FROM CMSPage page WHERE page.relatedPI = :pi");
        }
        q.setParameter("pi", pi);
        List<CMSPage> pageList = q.getResultList();
        return pageList;
    }

    public static String createCMSPageFilter(Map<String, String> params, String pageParameter, List<String> allowedTemplateIds,
            List<String> allowedSubthemes, List<String> allowedCategoryIds) throws AccessDeniedException {

        String query = "";

        int index = 0;
        if (allowedTemplateIds != null && !allowedTemplateIds.isEmpty()) {
            query += "(";
            for (String template : allowedTemplateIds) {
                String templateParameter = "tpl" + ++index;
                query += (":" + templateParameter + " = " + pageParameter + ".templateId");
                query += " OR ";
                params.put(templateParameter, template);
            }
            if (query.endsWith(" OR ")) {
                query = query.substring(0, query.length() - 4);
            }
            query += ") AND";
        } else if (allowedTemplateIds != null) {
            throw new AccessDeniedException("User may not view pages with any templates");
        }

        index = 0;
        if (allowedSubthemes != null && !allowedSubthemes.isEmpty()) {
            query += " (";
            for (String subtheme : allowedSubthemes) {
                String templateParameter = "thm" + ++index;
                query += (":" + templateParameter + " = " + pageParameter + ".subThemeDiscriminatorValue");
                query += " OR ";
                params.put(templateParameter, subtheme);
            }
            if (query.endsWith(" OR ")) {
                query = query.substring(0, query.length() - 4);
            }
            query += ") AND";
        } else if (allowedSubthemes != null) {
            query += " (" + pageParameter + ".subThemeDiscriminatorValue = \"\") AND";
        }

        index = 0;
        if (allowedCategoryIds != null && !allowedCategoryIds.isEmpty()) {
            query += " (";
            for (String category : allowedCategoryIds) {
                String templateParameter = "cat" + ++index;
                query += (":" + templateParameter + " IN (SELECT c.id FROM " + pageParameter + ".categories c)");
                query += " OR ";
                params.put(templateParameter, category);
            }
            if (query.endsWith(" OR ")) {
                query = query.substring(0, query.length() - 4);
            }
            query += ")";
        } else if (allowedCategoryIds != null) {
            query += " (SELECT COUNT(c) FROM " + pageParameter + ".categories c = 0)";
        }
        if (query.endsWith(" AND")) {
            query = query.substring(0, query.length() - 4);
        }

        return query.trim();
    }

    /**
     * @return a list of all persisted {@link CMSCategory CMSCategories}
     */
    @Override
    public List<CMSCategory> getAllCategories() throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT c FROM CMSCategory c ORDER BY c.name");
        q.setFlushMode(FlushModeType.COMMIT);
        List<CMSCategory> list = q.getResultList();
        return list;
    }

    /**
     * Persist a new {@link CMSCategory} object
     */
    @Override
    public void addCategory(CMSCategory category) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(category);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    /**
     * Update an existing {@link CMSCategory} object in the persistence context
     */
    @Override
    public void updateCategory(CMSCategory category) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(category);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    /**
     * Delete a {@link CMSCategory} object from the persistence context
     */
    @Override
    public boolean deleteCategory(CMSCategory category) throws DAOException {
        preQuery();
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            CMSCategory o = em.getReference(CMSCategory.class, category.getId());
            em.remove(o);
            em.getTransaction().commit();
            return true;
        } finally {
            em.close();
        }
    }

    /**
     * Search the persistence context for a {@link CMSCategory} with the given name.
     * 
     * @return A CMSCategory with the given name, or null if no matching entity was found
     * @throws NonUniqueResultException if the query matches more than one result
     * @throws DAOException if another error occurs
     */
    @Override
    public CMSCategory getCategoryByName(String name) throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT c FROM CMSCategory c WHERE c.name = :name");
        q.setParameter("name", name);
        q.setFlushMode(FlushModeType.COMMIT);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        CMSCategory category = (CMSCategory) getSingleResult(q).orElse(null);
        return category;
    }

    /**
     * Search the persistence context for a {@link CMSCategory} with the given unique id.
     * 
     * @return A CMSCategory with the given id, or null if no matching entity was found
     * @throws NonUniqueResultException if the query matches more than one result (should never happen since the id is the primary key
     * @throws DAOException if another error occurs
     */
    @Override
    public CMSCategory getCategory(Long id) throws DAOException {
        preQuery();
        Query q = em.createQuery("SELECT c FROM CMSCategory c WHERE c.id = :id");
        q.setParameter("id", id);
        q.setFlushMode(FlushModeType.COMMIT);
        // q.setHint("javax.persistence.cache.storeMode", "REFRESH");
        CMSCategory category = (CMSCategory) getSingleResult(q).orElse(null);
        return category;
    }

    /**
     * Check if the database contains a table of the given name. Used by backward-compatibility routines
     */
    @Override
    public boolean tableExists(String tableName) throws SQLException {
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();
        Connection connection = em.unwrap(Connection.class);
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet tables = metaData.getTables(null, null, tableName, null)) {
            return tables.next();
        } finally {
            transaction.commit();
        }
    }

    /**
     * Check if the database contains a column in a table with the given names. Used by backward-compatibility routines
     */
    @Override
    public boolean columnsExists(String tableName, String columnName) throws SQLException {
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();
        Connection connection = em.unwrap(Connection.class);
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet columns = metaData.getColumns(null, null, tableName, columnName)) {
            return columns.next();
        } finally {
            transaction.commit();
        }
    }

    /**
     * Start a persistence context transaction. Always needs to be succeeded with {@link #commitTransaction()} after the transaction is complete
     */
    @Override
    public void startTransaction() {
        em.getTransaction().begin();
    }

    /**
     * Commits a persistence context transaction Only to be used following a {@link #startTransaction()} call
     */
    @Override
    public void commitTransaction() {
        em.getTransaction().commit();
    }

    /**
     * Create a query in native sql syntax in the persistence context. Does not provide its own transaction. Use {@link #startTransaction()} and
     * {@link #commitTransaction()} for this
     */
    @Override
    public Query createNativeQuery(String string) {
        return em.createNativeQuery(string);
    }

    /**
     * Create a query in jpa query syntax in the persistence context. Does not provide its own transaction. Use {@link #startTransaction()} and
     * {@link #commitTransaction()} for this
     */
    @Override
    public Query createQuery(String string) {
        return em.createQuery(string);
    }

}