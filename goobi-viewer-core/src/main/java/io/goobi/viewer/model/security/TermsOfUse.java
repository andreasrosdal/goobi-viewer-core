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
package io.goobi.viewer.model.security;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.persistence.annotations.PrivateOwned;

import io.goobi.viewer.model.misc.IPolyglott;
import io.goobi.viewer.model.misc.Translation;

/**
 * @author florian
 *
 */
@Entity
@Table(name = "terms_of_use")
public class TermsOfUse {

    //labels for the translations
    private static final String TITLE_TAG = "label";
    private static final String DESCRIPTION_TAG = "description";
    
    
    /** Unique database ID. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "terms_of_use_id")
    protected Long id;
    
    /**
     * Contains texts and titles
     */
    @OneToMany(mappedBy = "owner", fetch = FetchType.EAGER, cascade = { CascadeType.ALL })
    @PrivateOwned
    private List<TermsOfUseTranslation> translations = new ArrayList<>();
    
    @Column(name = "active")
    private boolean active = false;
    
    public TermsOfUse() {
        
    }
    
    public TermsOfUse(TermsOfUse orig) {
        this.active = orig.active;
        this.id = orig.id;
        for(TermsOfUseTranslation translation : orig.translations) {
            TermsOfUseTranslation copy = new TermsOfUseTranslation(translation);
            this.translations.add(copy);
        }
    }
    
    /**
     * @param active the active to set
     */
    public void setActive(boolean active) {
        this.active = active;
    }
    
    /**
     * @return the active
     */
    public boolean isActive() {
        return active;
    }
    
    public TermsOfUseTranslation getTitle(String language) {
        TermsOfUseTranslation translation = getForLanguage(getTitles(), language).findAny().orElse(null);
        return translation;
    }
    
    public Optional<String> getTitleIfExists(String language) {
        return getForLanguage(getTitles(), language).findAny().map(Translation::getValue);
    }
    
    public TermsOfUseTranslation setTitle(String language, String value) {
        TermsOfUseTranslation translation = getTitle(language);
        if(translation == null) {
            translation = new TermsOfUseTranslation(language, value, this);
            translation.setTag(TITLE_TAG);
            this.translations.add(translation);
        } else {
            translation.setValue(value);
        }
        return translation;
    }
    
    public TermsOfUseTranslation getDescription(String language) {
        TermsOfUseTranslation translation = getForLanguage(getDescriptions(), language).findAny().orElse(null);
        return translation;
    }
    
    public Optional<String> getDescriptionIfExists(String language) {
        return getForLanguage(getDescriptions(), language).findAny().map(Translation::getValue);
    }
    
    public TermsOfUseTranslation setDescription(String language, String value) {
        TermsOfUseTranslation translation = getDescription(language);
        if(translation == null) {
            translation = new TermsOfUseTranslation(language, value, this);
            translation.setTag(DESCRIPTION_TAG);
            this.translations.add(translation);
        } else {
            translation.setValue(value);
        }
        return translation;
    }
    
    private Stream<TermsOfUseTranslation> getTitles() {
        return this.translations.stream().filter(t -> TITLE_TAG.equals(t.getTag()));
    }
    
    private Stream<TermsOfUseTranslation> getDescriptions() {
        return this.translations.stream().filter(t -> DESCRIPTION_TAG.equals(t.getTag()));
    }

    private Stream<TermsOfUseTranslation> getForLanguage(Stream<TermsOfUseTranslation> translations, String language) {
        if(StringUtils.isBlank(language)) {
            throw new IllegalArgumentException("Must provide non-empty language parameter to filter translations for language");
        }
        return translations.filter(t -> language.equals(t.getLanguage()));
    }
    
    public Long getId() {
        return id;
    }

    /**
     * Remove all empty translations from the translations list
     */
    public void cleanTranslations() {
        Iterator<TermsOfUseTranslation> i = this.translations.iterator();
        while(i.hasNext()) {
            TermsOfUseTranslation t = i.next();
            if(StringUtils.isBlank(t.getValue())) {
                i.remove();
            }
        }
    }


}
