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
package io.goobi.viewer.model.annotation.notification;

import java.util.Locale;

import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.annotation.PersistentAnnotation;

/**
 * @author florian
 *
 */
public class JsfMessagesNotificator implements ChangeNotificator {

    /** {@inheritDoc} */
    @Override
    public void notifyCreation(PersistentAnnotation annotation, Locale locale, String viewerRootUrl) {
        Messages.info(null, "Successfully created comment '{}'", annotation.getBody().toString());
    }

    /** {@inheritDoc} */
    @Override
    public void notifyEdit(PersistentAnnotation oldAnnotation, PersistentAnnotation newAnnotation, Locale locale, String viewerRootUrl) {
        Messages.info(null, "Successfully changed comment '{}' to '{}'", oldAnnotation.getBody().toString(), newAnnotation.getBody().toString());

    }

    /** {@inheritDoc} */
    @Override
    public void notifyDeletion(PersistentAnnotation annotation, Locale locale) {
        Messages.info(null, "Successfully deleted comment '{}'", annotation.getBody().toString());
    }

    /** {@inheritDoc} */
    @Override
    public void notifyError(Exception exception, Locale locale) {
        Messages.error("Error changing notification: " + exception.getMessage().toString());
    }

}
