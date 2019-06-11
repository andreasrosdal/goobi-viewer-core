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
package io.goobi.viewer.model.cms;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * A {@link TranslatedSelectable} which may also contain a list of {@link CMSCategory categories}
 * 
 * @author florian
 *
 */
public class CategorizableTranslatedSelectable<T> extends TranslatedSelectable<T> {

	private List<Selectable<CMSCategory>> categories;
	
	/**
	 * @param value
	 * @param selected
	 */
	public CategorizableTranslatedSelectable(T value, boolean selected, Locale defaultLocale, List<Selectable<CMSCategory>> categories) {
		super(value, selected, defaultLocale);
		this.categories = categories;
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * @return the categories
	 */
	public List<Selectable<CMSCategory>> getCategories() {
		return categories;
	}
	
	public void setCategories(List<Selectable<CMSCategory>> categories) {
		this.categories = categories;
	}
	
	public List<CMSCategory> getSelectedCategories() {
		return categories.stream().filter(Selectable::isSelected).map(Selectable::getValue).collect(Collectors.toList());
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Selectable<T> other) {
		return super.compareTo(other);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if(obj == null) {
			return false;
		} else if( obj == this) {
			return true;
		} else if(obj.getClass() == this.getClass()) {
			CategorizableTranslatedSelectable other = (CategorizableTranslatedSelectable)obj;
			return this.getValue().equals(other.getValue());
		} else {
			return false;
		}
	}

}
