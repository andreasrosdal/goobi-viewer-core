/**
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 * 
 * Visit these websites for more information. - http://www.intranda.com -
 * http://digiverso.com
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Display a map backed by GeoMap.java object
 * GeoJson coordinates are always [lng, lat]
 * 
 * @version 3.4.0
 * @module viewerJS.geoMapCms
 * @requires jQuery
 */
 
 var viewerJS = ( function( viewer ) {
    'use strict'; 
        
    // default variables
    var _debug = false;
    
    var _defaults = {
    		mapType: "MANUAL", // or "SOLR_QUERY",
    		openSearchOnMarkerClick: true,
    		documentIdToHighlight: undefined,
            map: {
	            mapId : "geomap",
	            language: "en",
	            iconPath: "/resources/images/map",
	        }
    }
    
    viewer.GeoMapCms = function(config) {
 		this.config = $.extend( true, {}, _defaults, config );
 		if(_debug)console.log("Initialize CMS-Geomap with config", config);
 		//Hightlight the marker belonging to a given SOLR document
    	let highlightDocumentId = this.config.documentIdToHighlight;
    	if(highlightDocumentId) {
    		console.log("highlight", highlightDocumentId);
    	    this.config.map.layers.map(layer => layer.features).flat().filter(f => f.properties.documentId == highlightDocumentId).forEach(f => f.properties.highlighted = true);
    	}
		this.geoMap = new viewerJS.GeoMap(this.config.map);
   }
   
   viewer.GeoMapCms.prototype.init = function(view) {
   
	    this.geoMap.layers.forEach(layer => {
			layer.language = this.config.map.language;
			//when clicking on features with an associated link, open that link
	    	layer.onFeatureClick.subscribe(feature => {
	   	       if(feature.properties && feature.properties.link && !feature.properties.highlighted) {
	   	           window.location.assign(feature.properties.link);
	   	       }
	   	    });
	   	    //link to search url on feature click
	    	if(layer.config.search.openSearchOnMarkerClick) {
				let searchUrlTemplate = layer.config.search.searchUrlTemplate;
	            layer.onFeatureClick.subscribe( (feature) => {
					// viewerJS.notifications.confirm("Do you want to show search results for this location?")
					// .then(() => {
						$(layer.config.search.loader).show();
						let queryUrl = searchUrlTemplate.replace("{lng}", feature.geometry.coordinates[0]);
						queryUrl = queryUrl.replace("{lat}", feature.geometry.coordinates[1]);
						window.open(queryUrl, layer.config.search.linkTarget);
					// })
	            });
	        }
		});
	    this.geoMap.init(view);
	}
	
	return viewer;

} )( viewerJS || {}, jQuery );