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
package io.goobi.viewer.model.calendar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CalendarRow implements Serializable {

    private static final long serialVersionUID = 1669202746505522856L;

    private List<ICalendarItem> itemList = new ArrayList<>();

    private boolean selected = false;

    public List<ICalendarItem> getItemList() {
        return itemList;
    }

    public void setItemList(List<ICalendarItem> itemList) {
        this.itemList = itemList;
    }

    public void addItem(ICalendarItem item) {
        itemList.add(item);
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

}