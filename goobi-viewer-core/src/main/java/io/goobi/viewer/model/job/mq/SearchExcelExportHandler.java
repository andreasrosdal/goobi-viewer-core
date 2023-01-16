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

package io.goobi.viewer.model.job.mq;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.mq.MessageHandler;
import io.goobi.viewer.controller.mq.MessageStatus;
import io.goobi.viewer.controller.mq.ViewerMessage;

public class SearchExcelExportHandler implements MessageHandler<MessageStatus> {

    private static final Logger logger = LogManager.getLogger(SearchExcelExportHandler.class);

    @Override
    public MessageStatus call(ViewerMessage message) {
        // TODO add logic, seems to be ignored so far
        return MessageStatus.FINISH;
    }

    @Override
    public String getMessageHandlerName() {
        return "SEARCH_EXCEL_EXPORT";
    }

}
