/*
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.faces.converters;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.TimeZone;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;

@FacesConverter("localDateTimeConverter")
public class LocalDateTimeConverter implements Converter<LocalDateTime> {

    @Override
    public LocalDateTime getAsObject(FacesContext context, UIComponent component, String submittedValue) {
        if (submittedValue == null || submittedValue.isEmpty()) {
            return null;
        }

        try {
            return ZonedDateTime.parse(submittedValue, getFormatter(context, component)).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } catch (DateTimeParseException e) {
            throw new ConverterException(new FacesMessage(submittedValue + " is not a valid local date time"), e);
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, LocalDateTime ldt) {
        if (ldt == null) {
            return "";
        }

        return getFormatter(context, component).format(ZonedDateTime.of(ldt, ZoneOffset.UTC));

    }

    private static DateTimeFormatter getFormatter(FacesContext context, UIComponent component) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(getPattern(component), getLocale(context, component));
        ZoneId zone = getZoneId(component);
        return (zone != null) ? formatter.withZone(zone) : formatter;
    }

    private static String getPattern(UIComponent component) {
        String pattern = (String) component.getAttributes().get("pattern");

        if (pattern == null) {
            throw new IllegalArgumentException("pattern attribute is required");
        }

        return pattern;
    }

    private static Locale getLocale(FacesContext context, UIComponent component) {
        Object locale = component.getAttributes().get("locale");
        return (locale instanceof Locale) ? (Locale) locale
                : (locale instanceof String) ? new Locale((String) locale)
                        : context.getViewRoot().getLocale();
    }

    private static ZoneId getZoneId(UIComponent component) {
        Object timeZone = component.getAttributes().get("timeZone");
        return (timeZone instanceof TimeZone) ? ((TimeZone) timeZone).toZoneId()
                : (timeZone instanceof String) ? ZoneId.of((String) timeZone)
                        : null;
    }

}
