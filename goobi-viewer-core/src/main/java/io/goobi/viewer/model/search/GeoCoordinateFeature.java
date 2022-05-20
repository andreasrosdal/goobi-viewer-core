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
package io.goobi.viewer.model.search;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author florian
 *
 */
public class GeoCoordinateFeature {

    private static final Logger logger = LoggerFactory.getLogger(GeoCoordinateFeature.class);

    private static final String REGEX_GEOCOORDS_SEARCH_STRING = "(IsWithin|Intersects|Contains|IsDisjointTo)\\((\\w+)\\(\\(((?:[\\d.-]+ [\\d.-]+,?\\s?)+)\\)\\)\\)";
    private static final int REGEX_GEOCOORDS_SEARCH_GROUP_RELATION = 1;
    private static final int REGEX_GEOCOORDS_SEARCH_GROUP_SHAPE = 2;
    private static final int REGEX_GEOCOORDS_SEARCH_GROUP_POINTS = 3;

    public static final String RELATION_PREDICATE_ISWITHIN = "ISWITHIN";
    public static final String RELATION_PREDICATE_INTERSECTS = "INTERSECTS";
    public static final String RELATION_PREDICATE_CONTAINS = "CONTAINS";
    public static final String RELATION_PREDICATE_ISDISJOINTTO = "ISDISJOINTTO";

    public static final String SHAPE_POLYGON = "POLYGON";


    private final JSONObject feature;
    private final String predicate;
    private final String shape;

    public GeoCoordinateFeature(String featureString, String predicate, String shape) throws JSONException {
        this.feature = new JSONObject(featureString);
        this.predicate = predicate;
        this.shape = shape;
    }

    /**
     * Initialize as a polygon feature with the given points as vertices
     * @param vertices
     */
    public GeoCoordinateFeature(double[][] points, String predicate, String shape) {
        JSONObject json = new JSONObject();
        json.put("type", shape);
        JSONArray vertices = new JSONArray();
        for (int i = 0; i < points.length; i++) {
            List<Double> pointList = Arrays.asList(points[i][1], points[i][0]);
            JSONArray point = new JSONArray(pointList);
            vertices.put(point);
        }
        json.put("vertices", vertices);
        this.feature = json;
        this.predicate = predicate;
        this.shape = shape;

    }

    public String getFeatureAsString() {
        return this.feature.toString();
    }

    public String getType() {
        return feature.getString("type");
    }

    public double[][] getVertices() {
        JSONArray vertices =  feature.getJSONArray("vertices");
        double[][] points = new double[vertices.length()][2];
        for (int i = 0; i < vertices.length(); i++) {
            JSONArray vertex = vertices.getJSONArray(i);
            points[i] = new double[]{vertex.getDouble(0), vertex.getDouble(1)};
        }
        return points;
    }

    public String getSearchString() {

        double[][] points = getVertices();
        String pointString = Arrays.stream(points).map(p -> Double.toString(p[1]) + " " + Double.toString(p[0])).collect(Collectors.joining(", "));

        String template = "$P($S(($V)))";
        String searchString = template
                .replace("$P", this.predicate)
                .replace("$S", this.shape)
                .replace("$V", pointString);
        return searchString;

    }

    public static String getPredicate(String searchString) {
        Matcher matcher = Pattern.compile(REGEX_GEOCOORDS_SEARCH_STRING, Pattern.CASE_INSENSITIVE).matcher(searchString);

        if(matcher.find()) {
            String relation = matcher.group(REGEX_GEOCOORDS_SEARCH_GROUP_RELATION);
            return relation;
        }
        return RELATION_PREDICATE_ISWITHIN;
    }

    public static String getShape(String searchString) {
        Matcher matcher = Pattern.compile(REGEX_GEOCOORDS_SEARCH_STRING, Pattern.CASE_INSENSITIVE).matcher(searchString);

        if(matcher.find()) {
            String shape = matcher.group(REGEX_GEOCOORDS_SEARCH_GROUP_SHAPE);
            return shape;
        }
        return SHAPE_POLYGON;
    }

    public static double[][] getGeoSearchPoints(String searchString) {

        Matcher matcher = Pattern.compile(REGEX_GEOCOORDS_SEARCH_STRING, Pattern.CASE_INSENSITIVE).matcher(searchString);

        if(matcher.find()) {
            String relation = matcher.group(REGEX_GEOCOORDS_SEARCH_GROUP_RELATION);
            String shape = matcher.group(REGEX_GEOCOORDS_SEARCH_GROUP_SHAPE);
            String allPoints = matcher.group(REGEX_GEOCOORDS_SEARCH_GROUP_POINTS);
            String[] strPoints = allPoints.split(", ");
            double[][] points = new double[strPoints.length][2];
            for (int i = 0; i < strPoints.length; i++) {
                try {
                    String[] strPoint = strPoints[i].split(" ");
                    points[i] = new double[]{Double.parseDouble(strPoint[0]), Double.parseDouble(strPoint[1])};
                } catch(NumberFormatException e) {
                    logger.warn("Unable to parse {} as double array", strPoints[i]);
                }
            }
            return points;
        } else {
            return new double[0][2];
        }

    }

    /**
     * @return
     */
    public boolean hasVertices() {
        return getVertices().length > 0;
    }

    /**
     *
     */
    public String getShape() {
       return this.shape;
    }

    public String getPredicate() {
        return this.predicate;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if(obj != null && obj.getClass().equals(this.getClass())) {
            GeoCoordinateFeature other = (GeoCoordinateFeature)obj;
            return  other.predicate.equals(this.predicate) &&
                    other.shape.equals(this.shape) &&
                    Arrays.deepEquals(other.getVertices(), this.getVertices());
        } else {
            return false;
        }
    }
}
