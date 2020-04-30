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
package io.goobi.viewer.controller;

import java.util.Locale;

import io.goobi.viewer.messages.ViewerResourceBundle;

/**
 * This class provides constants for Lucene in alphabetical order.
 */
public class SolrConstants {

    public enum DocType {
        ACCESSDENIED,
        DOCSTRCT,
        PAGE,
        METADATA, // grouped metadata
        EVENT, // LIDO event
        UGC, // user-generated content
        GROUP; // convolute

        public static DocType getByName(String name) {
            if (name != null) {
                switch (name) {
                    case "ACCESSDENIED":
                        return ACCESSDENIED;
                    case "DOCSTRCT":
                        return DOCSTRCT;
                    case "PAGE":
                        return PAGE;
                    case "METADATA":
                        return METADATA;
                    case "EVENT":
                        return EVENT;
                    case "UGC":
                        return UGC;
                    case "GROUP":
                        return GROUP;
                    default:
                        return null;
                }
            }

            return null;
        }

        public String getLabel(Locale locale) {
            return ViewerResourceBundle.getTranslation(new StringBuilder("doctype_").append(name()).toString(), locale);
        }
    }

    public enum MetadataGroupType {
        PERSON,
        CORPORATION,
        CONFERENCE,
        LOCATION,
        SUBJECT,
        ORIGININFO,
        RECORD,
        SHAPE,
        OTHER;

        public static MetadataGroupType getByName(String name) {
            if (name != null) {
                switch (name) {
                    case "PERSON":
                        return PERSON;
                    case "CORPORATION":
                        return CORPORATION;
                    case "CONFERENCE":
                        return CONFERENCE;
                    case "LOCATION":
                        return LOCATION;
                    case "SUBJECT":
                        return SUBJECT;
                    case "ORIGININFO":
                        return ORIGININFO;
                    case "OTHER":
                        return OTHER;
                    default:
                        return null;
                }

            }

            return null;
        }
    }

    /** Constant <code>ACCESSCONDITION="ACCESSCONDITION"</code> */
    public static final String ACCESSCONDITION = "ACCESSCONDITION";
    /** Constant <code>BOOKMARKS="BOOKMARKS"</code> */
    public static final String BOOKMARKS = "BOOKMARKS"; // not an index field, just a constant
    /** Constant <code>CMS_TEXT_ALL="CMS_TEXT_ALL"</code> */
    public static final String CMS_TEXT_ALL = "CMS_TEXT_ALL";
    /** Constant <code>CURRENTNO="CURRENTNO"</code> */
    public static final String CURRENTNO = "CURRENTNO";
    /** Constant <code>CURRENTNOSORT="CURRENTNOSORT"</code> */
    public static final String CURRENTNOSORT = "CURRENTNOSORT";
    /** Constant <code>DATAREPOSITORY="DATAREPOSITORY"</code> */
    public static final String DATAREPOSITORY = "DATAREPOSITORY";
    /** Constant <code>DATECREATED="DATECREATED"</code> */
    public static final String DATECREATED = "DATECREATED";
    /** Constant <code>DATEDELETED="DATEDELETED"</code> */
    public static final String DATEDELETED = "DATEDELETED";
    /** Constant <code>DATEUPDATED="DATEUPDATED"</code> */
    public static final String DATEUPDATED = "DATEUPDATED";
    /** Constant <code>DEFAULT="DEFAULT"</code> */
    public static final String DEFAULT = "DEFAULT";
    /** Constant <code>DC="DC"</code> */
    public static final String DC = "DC";
    /** Constant <code>DOCSTRCT="DOCSTRCT"</code> */
    public static final String DOCSTRCT = "DOCSTRCT";
    /** Constant <code>DOCSTRCT_SUB="DOCSTRCT_SUB"</code> */
    public static final String DOCSTRCT_SUB = "DOCSTRCT_SUB";
    /** Constant <code>DOCSTRCT_TOP="DOCSTRCT_TOP"</code> */
    public static final String DOCSTRCT_TOP = "DOCSTRCT_TOP";
    /** Constant <code>DOCTYPE="DOCTYPE"</code> */
    public static final String DOCTYPE = "DOCTYPE";
    /** Constant <code>EVENTDATE="EVENTDATE"</code> */
    public static final String EVENTDATE = "EVENTDATE";
    /** Constant <code>EVENTDATESTART="EVENTDATESTART"</code> */
    public static final String EVENTDATESTART = "EVENTDATESTART";
    /** Constant <code>EVENTDATEEND="EVENTDATEEND"</code> */
    public static final String EVENTDATEEND = "EVENTDATEEND";
    /** Constant <code>EVENTTYPE="EVENTTYPE"</code> */
    public static final String EVENTTYPE = "EVENTTYPE";
    /** Constant <code>IDDOC="IDDOC"</code> */
    public static final String IDDOC = "IDDOC";
    /** Constant <code>IDDOC_OWNER="IDDOC_OWNER"</code> */
    public static final String IDDOC_OWNER = "IDDOC_OWNER";
    /** Constant <code>IDDOC_PARENT="IDDOC_PARENT"</code> */
    public static final String IDDOC_PARENT = "IDDOC_PARENT";
    /** Constant <code>IDDOC_TOPSTRUCT="IDDOC_TOPSTRUCT"</code> */
    public static final String IDDOC_TOPSTRUCT = "IDDOC_TOPSTRUCT";
    /** Constant <code>FILEIDROOT="FILEIDROOT"</code> */
    public static final String FILEIDROOT = "FILEIDROOT";
    /** Constant <code>FILENAME="FILENAME"</code> */
    public static final String FILENAME = "FILENAME";
    /** Constant <code>FILENAME_ALTO="FILENAME_ALTO"</code> */
    public static final String FILENAME_ALTO = "FILENAME_ALTO";
    /** Constant <code>FILENAME_FULLTEXT="FILENAME_FULLTEXT"</code> */
    public static final String FILENAME_FULLTEXT = "FILENAME_FULLTEXT";
    /** Constant <code>FILENAME_HTML_SANDBOXED="FILENAME_HTML-SANDBOXED"</code> */
    public static final String FILENAME_HTML_SANDBOXED = "FILENAME_HTML-SANDBOXED";
    /** Constant <code>FILENAME_MPEG="FILENAME_MPEG"</code> */
    public static final String FILENAME_MPEG = "FILENAME_MPEG";
    /** Constant <code>FILENAME_MPEG3="FILENAME_MPEG3"</code> */
    public static final String FILENAME_MPEG3 = "FILENAME_MPEG3";
    /** Constant <code>FILENAME_MP4="FILENAME_MP4"</code> */
    public static final String FILENAME_MP4 = "FILENAME_MP4";
    /** Constant <code>FILENAME_OGG="FILENAME_OGG"</code> */
    public static final String FILENAME_OGG = "FILENAME_OGG";
    /** Constant <code>FILENAME_TEI="FILENAME_TEI"</code> */
    public static final String FILENAME_TEI = "FILENAME_TEI";
    /** Constant <code>FILENAME_WEBM="FILENAME_WEBM"</code> */
    public static final String FILENAME_WEBM = "FILENAME_WEBM";
    /** Constant <code>FULLTEXT="FULLTEXT"</code> */
    public static final String FULLTEXT = "FULLTEXT";
    /** Constant <code>FULLTEXTAVAILABLE="FULLTEXTAVAILABLE"</code> */
    public static final String FULLTEXTAVAILABLE = "FULLTEXTAVAILABLE";
    /** Constant <code>GROUPFIELD="GROUPFIELD"</code> */
    public static final String GROUPFIELD = "GROUPFIELD";
    /** Constant <code>GROUPTYPE="GROUPTYPE"</code> */
    public static final String GROUPTYPE = "GROUPTYPE";
    /** Constant <code>HEIGHT="HEIGHT"</code> */
    public static final String HEIGHT = "HEIGHT";
    /** Constant <code>IMAGEURN="IMAGEURN"</code> */
    public static final String IMAGEURN = "IMAGEURN";
    /** Constant <code>IMAGEURN_OAI="IMAGEURN_OAI"</code> */
    public static final String IMAGEURN_OAI = "IMAGEURN_OAI";
    /** Constant <code>ISANCHOR="ISANCHOR"</code> */
    public static final String ISANCHOR = "ISANCHOR";
    /** Constant <code>ISWORK="ISWORK"</code> */
    public static final String ISWORK = "ISWORK";
    /** Constant <code>LABEL="LABEL"</code> */
    public static final String LABEL = "LABEL";
    /** Constant <code>LANGUAGE="LANGUAGE"</code> */
    public static final String LANGUAGE = "LANGUAGE";
    /** Constant <code>LOGID="LOGID"</code> */
    public static final String LOGID = "LOGID";
    /** Constant <code>METADATATYPE="METADATATYPE"</code> */
    public static final String METADATATYPE = "METADATATYPE";
    /** Constant <code>NORMDATATERMS="NORMDATATERMS"</code> */
    public static final String NORMDATATERMS = "NORMDATATERMS";
    /** Constant <code>MIMETYPE="MIMETYPE"</code> */
    public static final String MIMETYPE = "MIMETYPE";
    /** Constant <code>NUMPAGES="NUMPAGES"</code> */
    public static final String NUMPAGES = "NUMPAGES";
    /** Constant <code>NUMVOLUMES="NUMVOLUMES"</code> */
    public static final String NUMVOLUMES = "NUMVOLUMES";
    /** Constant <code>OPACURL="OPACURL"</code> */
    public static final String OPACURL = "OPACURL";
    /** Constant <code>ORDER="ORDER"</code> */
    public static final String ORDER = "ORDER";
    /** Constant <code>ORDERLABEL="ORDERLABEL"</code> */
    public static final String ORDERLABEL = "ORDERLABEL";
    /** Constant <code>PERSON_ONEFIELD="MD_CREATOR"</code> */
    public static final String PERSON_ONEFIELD = "MD_CREATOR";
    /** Constant <code>PHYSID="PHYSID"</code> */
    public static final String PHYSID = "PHYSID";
    /** Constant <code>PI="PI"</code> */
    public static final String PI = "PI";
    /** Constant <code>PI_ANCHOR="PI_ANCHOR"</code> */
    public static final String PI_ANCHOR = "PI_ANCHOR";
    /** Constant <code>PI_PARENT="PI_PARENT"</code> */
    public static final String PI_PARENT = "PI_PARENT";
    /** Constant <code>PI_TOPSTRUCT="PI_TOPSTRUCT"</code> */
    public static final String PI_TOPSTRUCT = "PI_TOPSTRUCT";
    /** Constant <code>PLACEPUBLISH="MD_PLACEPUBLISH"</code> */
    public static final String PLACEPUBLISH = "MD_PLACEPUBLISH";
    /** Constant <code>PUBLISHER="PUBLISHER"</code> */
    public static final String PUBLISHER = "PUBLISHER";
    /** Constant <code>RESOURCE="RESOURCE"</code> */
    public static final String RESOURCE = "RESOURCE";
    /** Constant <code>SOURCEDOCFORMAT="SOURCEDOCFORMAT"</code> */
    public static final String SOURCEDOCFORMAT = "SOURCEDOCFORMAT";
    /** Constant <code>SUBTITLE="SUBTITLE"</code> */
    public static final String SUBTITLE = "SUBTITLE";
    /** Constant <code>SUPERDEFAULT="SUPERDEFAULT"</code> */
    public static final String SUPERDEFAULT = "SUPERDEFAULT";
    /** Constant <code>SUPERFULLTEXT="SUPERFULLTEXT"</code> */
    public static final String SUPERFULLTEXT = "SUPERFULLTEXT";
    /** Constant <code>SUPERUGCTERMS="SUPERUGCTERMS"</code> */
    public static final String SUPERUGCTERMS = "SUPERUGCTERMS";
    /** Constant <code>TITLE="MD_TITLE"</code> */
    public static final String TITLE = "MD_TITLE";
    /** Constant <code>THUMBNAIL="THUMBNAIL"</code> */
    public static final String THUMBNAIL = "THUMBNAIL";
    /** Constant <code>THUMBPAGENO="THUMBPAGENO"</code> */
    public static final String THUMBPAGENO = "THUMBPAGENO";
    /** Constant <code>THUMBPAGENOLABEL="THUMBPAGENOLABEL"</code> */
    public static final String THUMBPAGENOLABEL = "THUMBPAGENOLABEL";
    /** Constant <code>UGCCOORDS="UGCCOORDS"</code> */
    public static final String UGCCOORDS = "UGCCOORDS";
    /** Constant <code>UGCTERMS="UGCTERMS"</code> */
    public static final String UGCTERMS = "UGCTERMS";
    /** Constant <code>UGCTYPE="UGCTYPE"</code> */
    public static final String UGCTYPE = "UGCTYPE";
    /** Constant <code>URN="URN"</code> */
    public static final String URN = "URN";
    /** Constant <code>WIDTH="WIDTH"</code> */
    public static final String WIDTH = "WIDTH";
    /** Constant <code>YEARPUBLISH="MD_YEARPUBLISH"</code> */
    public static final String YEARPUBLISH = "MD_YEARPUBLISH";

    /** Constant <code>OPEN_ACCESS_VALUE="OPENACCESS"</code> */
    public static final String OPEN_ACCESS_VALUE = "OPENACCESS";

    /** Constant <code>WKT_="WKT_"</code> */
    public static final String WKT_ = "WKT_";
    /** Constant <code>GROUPID_="GROUPID_"</code> */
    public static final String GROUPID_ = "GROUPID_";
    /** Constant <code>GROUPORDER_="GROUPORDER_"</code> */
    public static final String GROUPORDER_ = "GROUPORDER_";
    /** Constant <code>_LANG_="_LANG_"</code> */
    public static final String _LANG_ = "_LANG_";
    /** Constant <code>_NOESCAPE="_NOESCAPE"</code> */
    public static final String _NOESCAPE = "_NOESCAPE";
    /** Constant <code>_UNTOKENIZED="_UNTOKENIZED"</code> */
    public static final String _UNTOKENIZED = "_UNTOKENIZED";
    /** Constant <code>_DRILLDOWN_SUFFIX="_DD"</code> */
    public static final String _DRILLDOWN_SUFFIX = "_DD";
    /** Constant <code>_METS="METS"</code> */
    public static final String _METS = "METS";
    /** Constant <code>_LIDO="LIDO"</code> */
    public static final String _LIDO = "LIDO";
    /** Constant <code>_DENKXWEB="DENKXWEB"</code> */
    public static final String _DENKXWEB = "DENKXWEB";
    /** Constant <code>_DUBLINCORE="DUBLINCORE"</code> */
    public static final String _DUBLINCORE = "DUBLINCORE";
    /** Constant <code>_WORLDVIEWS="WORLDVIEWS"</code> */
    public static final String _WORLDVIEWS = "WORLDVIEWS";

    /** Constant <code>_CALENDAR_YEAR="YEAR"</code> */
    public static final String _CALENDAR_YEAR = "YEAR";
    /** Constant <code>_CALENDAR_MONTH="YEARMONTH"</code> */
    public static final String _CALENDAR_MONTH = "YEARMONTH";
    /** Constant <code>_CALENDAR_DAY="YEARMONTHDAY"</code> */
    public static final String _CALENDAR_DAY = "YEARMONTHDAY";

    /** Constant <code>MDNUM_FILESIZE="MDNUM_FILESIZE"</code> */
    public static final String MDNUM_FILESIZE = "MDNUM_FILESIZE";

    /** Constant <code>FACET_DC="FACET_DC"</code> */
    public static final String FACET_DC = "FACET_DC";

    /** Constant <code>MD_TEXT="MD_TEXT"</code> */
    public static final String MD_TEXT = "MD_TEXT"; //content of UGC docs
    /** Constant <code>MD_BODY="MD_BODY"</code> */
    public static final String MD_BODY = "MD_BODY"; //body of UGC docs from json annotations
    /** Field containing true if a record has a right-to-left reading direction. */
    public static final String BOOL_DIRECTION_RTL = "BOOL_DIRECTION_RTL";
    /** Field containing true if a page or any of the record's pages has an image. */
    public static final String BOOL_IMAGEAVAILABLE = "BOOL_IMAGEAVAILABLE";
    /** Field containing a list of dates as year **/
    public static final String YEAR = "YEAR";
    /** Single field containing a date as year for sorting**/
    public static final String SORTNUM_YEAR = "SORTNUM_YEAR";

}
