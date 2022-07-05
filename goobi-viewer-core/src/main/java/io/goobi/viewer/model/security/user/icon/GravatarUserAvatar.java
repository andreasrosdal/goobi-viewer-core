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
package io.goobi.viewer.model.security.user.icon;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author florian
 *
 */
public class GravatarUserAvatar implements UserAvatar {

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(GravatarUserAvatar.class);

    private static final String DEFAULT_GRAVATAR_ICON = "";
    private static final String DEFAULT_HTTP_404 = "404";
    private static final String DEFAULT_MYSTERY_MAN = "mm";
    private static final String DEFAULT_IDENTICON = "identicon";
    private static final String DEFAULT_MONSTERID = "monsterid";
    private static final String DEFAULT_WAVATAR = "wavatar";
    private static final String DEFAULT_RETRO = "retro";
    private static final String DEFAULT_BLANK = "blank";

    private static final String GRAVATAR_URL = "//www.gravatar.com/avatar/";

    private final String email;

    /**
     *
     * @param email
     */
    public GravatarUserAvatar(String email) {
        this.email = email;
    }

    /**
     *
     */
    @Override
    public String getIconUrl(int size, HttpServletRequest request) {
        return getGravatarUrl(size);
    }

    /**
     *
     * @param size
     * @return
     */
    private String getGravatarUrl(int size) {
        if (StringUtils.isNotEmpty(email)) {
            return GRAVATAR_URL + md5Hex(email) + "?rating=g&size=" + size + "&default=" + DEFAULT_IDENTICON;
        }

        return GRAVATAR_URL;
    }

    /**
     *
     * @param array
     * @return
     */
    static String hex(byte[] array) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < array.length; ++i) {
            sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
        }

        return sb.toString();
    }

    /**
     * Gravatar requires MD5.
     * 
     * @param message
     * @return
     */
    static String md5Hex(String message) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5"); //NOSONAR
            return hex(md.digest(message.getBytes("CP1252")));
        } catch (NoSuchAlgorithmException e) {
            logger.error(e.getMessage());
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage());
        }
        return null;
    }
}
