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
 * <Short Module Description>
 * 
 * @version 21.04
 * @module viewerJS.notifications
 * @requires jQuery
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    var _debug = false; 
	
    viewer.notifications = {
		success : (message) => viewer.notifications.notify(message, "success"),
		error : (message) => viewer.notifications.notify(message, "error"),
		warn : (message) => viewer.notifications.notify(message, "warn"),
		confirm : (message, confirmText, denyText) => {
			return viewer.translator.addTranslations(["cancel", "ok"])
			.then( () => {
				confirmText = confirmText ? confirmText : viewerJS.translator.translate("ok");
				denyText = denyText ? denyText : viewerJS.translator.translate("cancel");
				if(typeof(Swal) !== 'undefined') {
					return Swal.fire({
						title: message,
						icon: 'warning',
						showCancelButton: true,
						confirmButtonText: confirmText,
	  					cancelButtonText: denyText,
					    buttonsStyling: false,
	  					customClass: {
						    confirmButton: 'btn btn--full',
						    cancelButton: 'btn btn--default'
						  }
					})
					.then(result => {
						return result.isConfirmed ? Promise.resolve() : Promise.reject();
					});
				} else {
					return window.confirm(message) ? Promise.resolve() : Promise.reject();
				}		
				
			});
		},
		notify : (message, type) => {
			if(typeof Swal !== 'undefined') {
				Swal.fire({
					title: message,
					icon: type,
				    buttonsStyling: false,
				    text: '',
					customClass: {
					    confirmButton: 'btn btn--full',
					    cancelButton: 'btn btn--default'
					  }
				});
			} else if(typeof sweetAlert !== 'undefined') {
				swal(message, "", type);
			} else if(jQuery().overhang) {
				$("body").overhang({
				  type: type,
				  message: message
				});

			} else {
				alert(message);
			}			
		}
    }
    
    return viewer;
    
} )( viewerJS || {}, jQuery );
