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
 * Module for cms slider creation
 * 
 * @module cmsJS.createSlider
 * @requires jQuery
 * 
 */
var cmsJS = ( function( cms ) {
    'use strict';
    
    var _debug = true;
    
    cms.createSlider = {
        /**
         * Method which initializes the CMS create page module.
         * 
         * @method init
         * @param {Object} settings
         */
        init: function( settings ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'cmsJS.createSlider.init' );
                console.log( '##############################' );
            }
            
            cmsJS.createSlider.initStyleOptions();

        },
        
        /**
        *	Initializes the list of options for any elements with data-options="slider-styles" 
        */
        initStyleOptions: function() {
        	$("[data-options='slider-styles']").each((index, element) => {
        		if(_debug)console.log("add options to", element);
        		let $select = $(element);
	        	let styles = viewerJS.slideshows.styles.keys();
	        	let value = $select.attr("data-value");
	        	if(_debug)console.log("add options to dropdown", $select, styles);
    			for (let style of styles) { 
    				if(value == style) {
    					$select.append("<option value='"+style+"' selected='selected'>" +style+ "</option>");
    				} else {
	    				$select.append("<option value='"+style+"'>" +style+ "</option>");
    				}
    			}   		
        	});
        },

    };
    
    return cms;
    
} )( cmsJS || {}, jQuery );
