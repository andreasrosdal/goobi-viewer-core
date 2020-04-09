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
package io.goobi.viewer.model.termbrowsing;

import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.metadata.multilanguage.IMetadataValue;

/**
 * <p>
 * BrowseTerm class.
 * </p>
 */
public class BrowseTerm implements Serializable {

    private static final long serialVersionUID = -55691065713339706L;

    private static final Logger logger = LoggerFactory.getLogger(BrowseTerm.class);

    private final String term;
    private final String sortTerm;
    private final IMetadataValue translations;
    private long hitCount = 0;
    private final Set<String> piList = ConcurrentHashMap.newKeySet();

    /**
     * Constructor that sets <code>hitCount</code> to 1.
     *
     * @param term a {@link java.lang.String} object.
     * @param sortTerm a {@link java.lang.String} object.
     * @param translation
     */
    public BrowseTerm(String term, String sortTerm, IMetadataValue translations) {
        this.term = term;
        this.sortTerm = sortTerm;
        this.translations = translations;
        this.hitCount = 1;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (term == null ? 0 : term.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        BrowseTerm other = (BrowseTerm) obj;
        if (term == null) {
            if (other.term != null) {
                return false;
            }
        } else if (!term.equals(other.term)) {
            return false;
        }
        return true;
    }

    /**
     * <p>
     * Getter for the field <code>term</code>.
     * </p>
     *
     * @return the term
     */
    public String getTerm() {
        return term;
    }

    /**
     * <p>
     * Getter for the field <code>sortTerm</code>.
     * </p>
     *
     * @return the sortTerm
     */
    public String getSortTerm() {
        return sortTerm;
    }

    /**
     * @return the translations
     */
    public IMetadataValue getTranslations() {
        return translations;
    }

    /**
     * <p>
     * addToHitCount.
     * </p>
     *
     * @param num a int.
     */
    public void addToHitCount(int num) {
        synchronized (this) {
            hitCount += num;
        }
    }

    /**
     * <p>
     * Getter for the field <code>hitCount</code>.
     * </p>
     *
     * @return the hitCount
     */
    public long getHitCount() {
        return hitCount;
    }

    /**
     * <p>
     * Getter for the field <code>piList</code>.
     * </p>
     *
     * @return the piList
     */
    public Set<String> getPiList() {
        return piList;
    }

}
