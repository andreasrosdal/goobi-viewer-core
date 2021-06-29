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
package io.goobi.viewer.model.translations.admin;

import java.util.List;

/**
 * A single message key with all its available translations for admin backend editing.
 */
public class MessageEntry implements Comparable<MessageEntry> {

    public enum TranslationStatus {
        NONE,
        PARTIAL,
        FULL;
    }

    private final String key;
    private final List<MessageValue> values;

    /**
     * 
     * @param key
     * @param values
     */
    public MessageEntry(String key, List<MessageValue> values) {
        this.key = key;
        this.values = values;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MessageEntry other = (MessageEntry) obj;
        if (key == null) {
            if (other.key != null)
                return false;
        } else if (!key.equals(other.key))
            return false;
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @should compare correctly
     */
    @Override
    public int compareTo(MessageEntry o) {
        return this.key.compareTo(o.key);
    }

    /**
     * 
     * @return appropriate {@link TranslationStatus}
     * @should return none status correctly
     * @should return partial status correctly
     * @should return full status correctly
     */
    public TranslationStatus getTranslationStatus() {
        int full = 0;
        int none = 0;
        for (MessageValue value : values) {
            switch (value.getTranslationStatus()) {
                case NONE:
                    none++;
                    break;
                case FULL:
                    full++;
                    break;
                default:
                    break;
            }
        }

        if (none == values.size()) {
            return TranslationStatus.NONE;
        }
        if (full == values.size()) {
            return TranslationStatus.FULL;
        }

        return TranslationStatus.PARTIAL;
    }

    /**
     * 
     * @param language
     * @return appropriate {@link TranslationStatus}
     * @should return correct status for language
     */
    public TranslationStatus getTranslationStatusForLanguage(String language) {
        if (language == null) {
            return TranslationStatus.NONE;
        }

        for (MessageValue value : getValues()) {
            if (language.equals(value.getLanguage())) {
                return value.getTranslationStatus();
            }
        }

        return TranslationStatus.NONE;
    }

    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @return the values
     */
    public List<MessageValue> getValues() {
        return values;
    }
}
