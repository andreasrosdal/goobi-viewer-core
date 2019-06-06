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
 * Module which includes mostly used helper functions.
 * 
 * @version 3.2.0
 * @module viewerJS.helper
 * @requires jQuery
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    // default variables
    var _debug = false;
    
    viewer.helper = {
        /**
         * saveSidebarTocPosition Method to truncate a string to a given length.
         * @method truncateString
         * @param {String} str The string to truncate.
         * @param {Number} size The number of characters after the string should be
         * croped.
         * @returns {String} The truncated string.
         * @example
         * 
         * <pre>
         * viewerJS.helper.truncateString( $( '.something' ).text(), 75 );
         * </pre>
         */
        truncateString: function( str, size ) {
            if ( _debug ) {
                console.log( '---------- viewer.helper.truncateString() ----------' );
                console.log( 'viewer.helper.truncateString: str = ', str );
                console.log( 'viewer.helper.truncateString: size = ', size );
            }
            
            var strSize = parseInt( str.length );
            
            if ( strSize > size ) {
                return str.substring( 0, size ) + '...';
            }
            else {
                return str;
            }
        },
        /**
         * @description Returns an JSON object from a API call. 
         * @method getRemoteData
         * @param {String} url The API call URL.
         * @returns {Object} A promise object, which tells about the success of receiving
         * data.
         * @example
         * 
         * <pre>
         * viewerJS.helper.getRemoteData( dataURL );
         * </pre>
         */
        getRemoteData: function( url ) {
            if ( _debug ) {
                console.log( '---------- viewer.helper.getRemoteData() ----------' );
                console.log( 'viewer.helper.getRemoteData: url = ', url );
            }
            
            var promise = Q( $.ajax( {
                url: decodeURI( url ),
                type: "GET",
                dataType: "JSON",
                async: true
            } ) );
            
            return promise;
        },
        /**
         * @description Returns a BS Modal with dynamic content.
         * @method renderModal
         * @param {Object} config An config object which includes the content of the
         * modal.
         * @param {String} config.id The ID of the modal.
         * @param {String} config.label The label of the modal.
         * @param {Object} config.string An object of strings for the modal content.
         * @param {String} config.string.title The title of the modal.
         * @param {String} config.string.body The content of the modal as HTML.
         * @param {String} config.string.closeBtn Buttontext
         * @param {String} config.string.saveBtn Buttontext
         * @returns {String} A HTML-String which renders the modal.
         */
        renderModal: function( config ) {
            if ( _debug ) {
                console.log( '---------- viewer.helper.renderModal() ----------' );
                console.log( 'viewer.helper.renderModal: config = ', config );
            }
            var _defaults = {
                id: 'myModal',
                label: 'myModalLabel',
                closeId: 'closeModal',
                submitId: 'submitModal',
                string: {
                    title: 'Modal title',
                    body: '',
                    closeBtn: 'Close',
                    saveBtn: 'Save changes',
                }
            };
            
            $.extend( true, _defaults, config );
            
            var modal = '';
            
            modal += '<div class="modal fade" id="' + _defaults.id + '" tabindex="-1" role="dialog" aria-labelledby="' + _defaults.label + '">';
            modal += '<div class="modal-dialog" role="document">';
            modal += '<div class="modal-content">';
            modal += '<div class="modal-header">';
            modal += '<button type="button" class="close" data-dismiss="modal" aria-label="' + _defaults.string.closeBtn + '">';
            modal += '<span aria-hidden="true">&times;</span>';
            modal += '</button>';
            modal += '<h4 class="modal-title" id="' + _defaults.label + '">' + _defaults.string.title + '</h4>';
            modal += '</div>';
            modal += '<div class="modal-body">' + _defaults.string.body + '</div>';
            modal += '<div class="modal-footer">';
            modal += '<button type="button" id="' + _defaults.closeId + '"  class="btn" data-dismiss="modal">' + _defaults.string.closeBtn + '</button>';
            modal += '<button type="button" id="' + _defaults.submitId + '" class="btn btn--success">' + _defaults.string.saveBtn + '</button>';
            modal += '</div></div></div></div>';
            
            return modal;
        },
        /**
         * @description Method to return a BS Alert with dynamic content.
         * @method renderAlert
         * @param {String} type The type of the alert.
         * @param {String} content The content of the alert.
         * @param {Boolean} dismissable Sets the option to make the alert dismissable,
         * true = dismissable.
         * @returns {String} A HTML-String which renders the alert.
         */
        renderAlert: function( type, content, dismissable ) {
            if ( _debug ) {
                console.log( '---------- viewer.helper.renderAlert() ----------' );
                console.log( 'viewer.helper.renderAlert: type = ', type );
                console.log( 'viewer.helper.renderAlert: content = ', content );
                console.log( 'viewer.helper.renderAlert: dismissable = ', dismissable );
            }
            var bsAlert = '';
            
            bsAlert += '<div role="alert" class="alert ' + type + ' alert-dismissible fade in">';
            if ( dismissable ) {
                bsAlert += '<button aria-label="Close" data-dismiss="alert" class="close" type="button"><span aria-hidden="true">×</span></button>';
            }
            bsAlert += content;
            bsAlert += '</div>';
            
            return bsAlert;
        },
        /**
         * @description Method to get the version number of the used MS Internet Explorer.
         * @method detectIEVersion
         * @returns {Number} The browser version.
         */
        detectIEVersion: function() {
            var ua = window.navigator.userAgent;
            
            // IE 10 and older
            var msie = ua.indexOf( 'MSIE ' );
            if ( msie > 0 ) {
                // IE 10 or older => return version number
                return parseInt( ua.substring( msie + 5, ua.indexOf( '.', msie ) ), 10 );
            }
            
            // IE 11
            var trident = ua.indexOf( 'Trident/' );
            if ( trident > 0 ) {
                // IE 11 => return version number
                var rv = ua.indexOf( 'rv:' );
                return parseInt( ua.substring( rv + 3, ua.indexOf( '.', rv ) ), 10 );
            }
            
            // IE 12+
            var edge = ua.indexOf( 'Edge/' );
            if ( edge > 0 ) {
                // Edge (IE 12+) => return version number
                return parseInt( ua.substring( edge + 5, ua.indexOf( '.', edge ) ), 10 );
            }
            
            // other browser
            return false;
        },        
        /**
         * @description Method to check if it´s possible to write to local Storage.
         * @method checkLocalStorage
         * @returns {Boolean} true or false
         */
        checkLocalStorage: function() {
            if ( typeof localStorage === 'object' ) {
                try {
                    localStorage.setItem( 'testLocalStorage', 1 );
                    localStorage.removeItem( 'testLocalStorage' );
                    
                    return true;
                }
                catch ( error ) {
                    console.error( 'Not possible to write in local Storage: ', error );
                    
                    return false;
                }
            }
        },        
        /**
         * @description Method to render a warning popover.
         * @method renderWarningPopover
         * @param {String} msg The message to show in the popover.
         * @returns {Object} An jQuery Object to append to DOM.
         */
        renderWarningPopover: function( msg ) {
            var popover = $( '<div />' );
            var popoverText = $( '<p />' );
            var popoverButton = $( '<button />' );
            var popoverButtonIcon = $( '<i aria-hidden="true" />' );
            
            popover.addClass( 'warning-popover' );
            
            // build button
            popoverButton.addClass( 'btn btn--clean' );
            popoverButton.attr( 'data-toggle', 'warning-popover' );
            popoverButtonIcon.addClass( 'fa fa-times' );
            popoverButton.append( popoverButtonIcon );
            popover.append( popoverButton );
            
            // build text
            popoverText.html( msg );
            popover.append( popoverText );
            
            return popover;
        },
        /**
         * @description Method to validate the reCAPTCHA response.
         * @method validateReCaptcha
         * @param {String} wrapper The reCAPTCHA widget wrapper.
         * @param {String} key The reCAPTCHA site key.
         * @returns {Boolean} Returns true if the response is valid.
         */
        validateReCaptcha: function( wrapper, key ) {
            var widget = grecaptcha.render( wrapper, {
                sitekey: key,
                callback: function() {
                    var response = grecaptcha.getResponse( widget );
                    
                    if ( response == 0 ) {
                        return false;
                    }
                    else {
                        return true;
                    }
                }
            } );
        },        
        /**
         * @description Method to get the current used browser.
         * @method getCurrentBrowser
         * @returns {String} The name of the current Browser.
         */
        getCurrentBrowser: function() {
            // Opera 8.0+
            var isOpera = ( !!window.opr && !!opr.addons ) || !!window.opera || navigator.userAgent.indexOf( ' OPR/' ) >= 0;
            // Firefox 1.0+
            var isFirefox = typeof InstallTrigger !== 'undefined';
            // Safari 3.0+ "[object HTMLElementConstructor]"
            var isSafari = /constructor/i.test( window.HTMLElement ) || ( function( p ) {
                return p.toString() === "[object SafariRemoteNotification]";
            } )( !window[ 'safari' ] || ( typeof safari !== 'undefined' && safari.pushNotification ) );
            // Internet Explorer 6-11
            var isIE = /* @cc_on!@ */false || !!document.documentMode;
            // Edge 20+
            var isEdge = !isIE && !!window.StyleMedia;
            // Chrome 1+
            var isChrome = !!window.chrome;
            // Blink engine detection
            // var isBlink = ( isChrome || isOpera ) && !!window.CSS;
            
            if ( isOpera ) {
                return 'Opera';
            }
            else if ( isFirefox ) {
                return 'Firefox';
            }
            else if ( isSafari ) {
                return 'Safari';
            }
            else if ( isIE ) {
                return 'IE';
            }
            else if ( isEdge ) {
                return 'Edge';
            }
            else if ( isChrome ) {
                return 'Chrome';
            }
        },
        /**
         * @description Method to initialize Bootstrap features.
         * @method initBsFeatures
         */
        initBsFeatures: function() {
        	if ( _debug ) {
                console.log( 'EXECUTE: viewerJS.helper.initBsFeatures' );
            }
        	
        	// enable BS tooltips
            $( '[data-toggle="tooltip"]' ).tooltip( {
                trigger : 'hover'
            } );
            if ( window.matchMedia( '(max-width: 768px)' ).matches ) {
            	$( '[data-toggle="tooltip"]' ).tooltip( 'destroy' );
            }        	
        },
    };
    
    viewer.localStoragePossible = viewer.helper.checkLocalStorage();
    
    viewer.getMetadataValue = function(object, language) {
        return viewer.getOrElse([language, 0], object);
    }    
    
    viewer.getOrElse = function(p, o) {  
        var reducer = function(xs, x) {
            return (xs && xs[x]) ? xs[x] : ((xs && xs[Object.keys(xs)[0]]) ? xs[Object.keys(xs)[0]] : null);
        }
        return p.reduce(reducer , o);
    };
    
    if(!Array.prototype.includes) {
        Array.prototype.includes = function(element) {
            for ( var int = 0; int < this.length; int++ ) {
                var listEle = this[int];
                if(listEle == element) {
                    return true;
                }
            }
            return false;
        }
    }
    
    return viewer;
    
} )( viewerJS || {}, jQuery );