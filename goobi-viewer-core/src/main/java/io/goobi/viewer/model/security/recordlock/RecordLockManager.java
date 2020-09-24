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
package io.goobi.viewer.model.security.recordlock;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.exceptions.RecordLimitExceededException;

public class RecordLockManager {

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(RecordLockManager.class);

    /** Currently viewed records */
    private final Map<String, Set<RecordLock>> loadedRecordMap = new ConcurrentHashMap<>();

    /**
     * 
     * @param pi Record identifier
     * @param sessionId HTTP session ID
     * @param limit Optional number of concurrent views for the record
     * @throws RecordLimitExceededException
     * @should add record lock to map correctly
     * @should do nothing if limit null
     * @should do nothing if session id already in list
     * @should throw RecordLimitExceededException if limit exceeded
     */
    public synchronized void lockRecord(String pi, String sessionId, Integer limit) throws RecordLimitExceededException {
        logger.trace("lockRecord: {}", pi);
        if (pi == null) {
            throw new IllegalArgumentException("pi may not be null");
        }
        if (sessionId == null) {
            logger.warn("No sessionId given");
            return;
        }
        // Record has unlimited views
        if (limit == null) {
            return;
        }
        Set<RecordLock> recordLocks = loadedRecordMap.get(pi);
        if (recordLocks == null) {
            recordLocks = new HashSet<>(limit);
            loadedRecordMap.put(pi, recordLocks);
        }
        RecordLock newLock = new RecordLock(pi, sessionId);
        logger.trace("{} is currently locked {} times", pi, recordLocks.size());
        if (recordLocks.size() == limit) {
            if (recordLocks.contains(newLock)) {
                return;
            }
            throw new RecordLimitExceededException(pi + ":" + limit);
        }

        recordLocks.add(newLock);
        logger.trace("Added lock: {}", newLock);
    }

    /**
     * 
     * @param sessionId HTTP session ID
     * @param skipPiList Optional list of identifiers to skip
     * @return true if session id removed from list successfully; false otherwise
     * @should return number of records if session id removed successfully
     * @should skip pi in list
     */
    public synchronized int removeLocksForSessionId(String sessionId, List<String> skipPiList) {
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId may not be null");
        }

        int count = 0;
        for (String pi : loadedRecordMap.keySet()) {
            if (skipPiList != null && skipPiList.contains(pi)) {
                continue;
            }
            if (removeLockForPiAndSessionId(pi, sessionId)) {
                count++;
            }
        }

        return count;
    }

    /**
     * 
     * @param pi
     * @param sessionId
     * @return
     */
    public synchronized boolean removeLockForPiAndSessionId(String pi, String sessionId) {
        Set<RecordLock> recordLocks = loadedRecordMap.get(pi);
        if (recordLocks == null) {
            return false;
        }

        boolean ret = false;
        RecordLock lock = new RecordLock(pi, sessionId);
        if (recordLocks.contains(lock)) {
            recordLocks.remove(lock);
            ret = true;
            logger.trace("Removed record lock: {}", lock);
        }

        return ret;
    }

    /**
     * Removes all record locks that are older that <code>maxAge</code> milliseconds. Can be used to periodically clean up locks that might have been
     * missed by the web socket mechanism.
     * 
     * @param maxAge
     * @return
     * @should remove locks older than maxAge
     */
    public synchronized int removeOldLocks(long maxAge) {
        if (loadedRecordMap.isEmpty()) {
            return 0;
        }

        long now = System.currentTimeMillis();
        int count = 0;
        Set<String> emptyPIs = new HashSet<>(loadedRecordMap.size());
        for (String pi : loadedRecordMap.keySet()) {
            if (loadedRecordMap.get(pi) == null) {
                continue;
            }
            Set<RecordLock> toRemove = new HashSet<>();
            for (RecordLock lock : loadedRecordMap.get(pi)) {
                if (now - lock.getTimeCreated() > maxAge) {
                    toRemove.add(lock);
                }
            }
            if (!toRemove.isEmpty() && loadedRecordMap.get(pi).removeAll(toRemove)) {
                count += toRemove.size();
            }
            if (loadedRecordMap.get(pi).isEmpty()) {
                emptyPIs.add(pi);
            }
        }

        // Remove empty entries
        if (!emptyPIs.isEmpty()) {
            for (String pi : emptyPIs) {
                loadedRecordMap.remove(pi);
            }
        }

        return count;
    }

    /**
     * @return the loadedRecordMap
     */
    Map<String, Set<RecordLock>> getLoadedRecordMap() {
        return loadedRecordMap;
    }
}
