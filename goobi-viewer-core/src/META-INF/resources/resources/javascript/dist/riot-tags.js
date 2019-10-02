riot.tag2('adminmediaupload', '<div class="admin-cms-media__upload {isDragover ? \'is-dragover\' : \'\'}" ref="dropZone"><div class="admin-cms-media__upload-input"><p> {opts.msg.uploadText} <br><small>({opts.msg.allowedFileTypes}: {fileTypes})</small></p><label for="file" class="btn btn--default">{opts.msg.buttonUpload}</label><input id="file" class="admin-cms-media__upload-file" type="file" multiple="multiple" onchange="{buttonFilesSelected}"></div><div class="admin-cms-media__upload-messages"><div class="admin-cms-media__upload-message uploading"><i class="fa fa-spinner fa-pulse fa-fw"></i> {opts.msg.mediaUploading} </div><div class="admin-cms-media__upload-message success"><i class="fa fa-check-square-o" aria-hidden="true"></i> {opts.msg.mediaFinished} </div><div class="admin-cms-media__upload-message error"><i class="fa fa-exclamation-circle" aria-hidden="true"></i><span></span></div></div></div>', '', '', function(opts) {
        this.files = [];
        this.displayFiles = [];
        this.fileTypes = 'jpg, png, docx, doc, pdf, rtf, html, xhtml, xml';
        this.isDragover = false;

        this.on('mount', function () {
            var dropZone = (this.refs.dropZone);

            dropZone.addEventListener('dragover', function (e) {
                e.stopPropagation();
                e.preventDefault();
                e.dataTransfer.dropEffect = 'copy';

                $('.admin-cms-media__upload-messages, .admin-cms-media__upload-message.uploading, .admin-cms-media__upload-message.success, .admin-cms-media__upload-message.error').removeClass('in-progress');

                this.isDragover = true;
                this.update();
            }.bind(this));

            dropZone.addEventListener('dragleave', function (e) {
                this.isDragover = false;
                this.update();
            }.bind(this));

            dropZone.addEventListener('drop', (e) => {
                e.stopPropagation();
                e.preventDefault();
                this.files = [];

                for (var f of e.dataTransfer.files) {
                    this.files.push(f);
                    var sizeUnit = 'KB';
                    var size = f.size / 1000;

                    if (size > 1024) {
                        size = size / 1024;
                        sizeUnit = 'MB';
                    }

                    if (size > 1024) {
                        size = size / 1024;
                        sizeUnit = 'GB';
                    }

                    this.displayFiles.push({ name: f.name, size: Math.floor(size) + ' ' + sizeUnit, completed: 0 });
                }
    			this.uploadFiles();

            });
        }.bind(this));

        this.buttonFilesSelected = function(e) {
            for (var f of e.target.files) {

                this.files.push(f);
                var sizeUnit = 'KB';
                var size = f.size / 1000;

                if (size > 1024) {
                    size = size / 1024;
                    sizeUnit = 'MB';
                }
                if (size > 1024) {
                    size = size / 1024;
                    sizeUnit = 'GB';
                }

                this.displayFiles.push({ name: f.name, size: Math.floor(size) + ' ' + sizeUnit, completed: 0 });
            }

            this.uploadFiles();
        }.bind(this)

        this.uploadFiles = function() {
            var uploads = [];

            $('.admin-cms-media__upload-messages, .admin-cms-media__upload-message.success, .admin-cms-media__upload-message.error').removeClass('in-progress');
            $('.admin-cms-media__upload-messages, .admin-cms-media__upload-message.uploading').addClass('in-progress');

            for (i = 0; i < this.files.length; i++) {
                uploads.push(Q(this.uploadFile(i)));
            }

            Q.allSettled(uploads).then(function(results) {
             	var errorMsg = "";
                 results.forEach(function (result) {
                     if (result.state === "fulfilled") {
                     	var value = result.value;
                     	this.fileUploaded(value);
                     }
                     else {
                         var responseText = result.reason.responseText;
                         errorMsg += (responseText + "</br>");
                     }
                 }.bind(this));

                 if (errorMsg) {
                 	this.fileUploadError(errorMsg);
                 } else if(this.opts.onUploadSuccess) {
                     this.opts.onUploadSuccess();
                 }

            		if (this.opts.onUploadComplete) {
            			this.opts.onUploadComplete();
            		}
            }.bind(this))

        }.bind(this)

        this.fileUploaded = function(fileInfo) {
            console.log("file uploaded")
            $('.admin-cms-media__upload-messages, .admin-cms-media__upload-message.uploading').removeClass('in-progress');
            $('.admin-cms-media__upload-messages, .admin-cms-media__upload-message.success').addClass('in-progress');

            setTimeout( function() {
                $('.admin-cms-media__upload-messages, .admin-cms-media__upload-message.uploading, .admin-cms-media__upload-message.success').removeClass('in-progress');
        	}, 5000 );
        }.bind(this)

        this.fileUploadError = function(responseText) {
            $('.admin-cms-media__upload-messages, .admin-cms-media__upload-message.uploading').removeClass('in-progress');
        	if (responseText) {
                $('.admin-cms-media__upload-messages, .admin-cms-media__upload-message.error').addClass('in-progress');
                $('.admin-cms-media__upload-message.error span').html(responseText);
            }
        }.bind(this)

        this.uploadFile = function(i) {
            if (this.files.length <= i) {
                new Modal(this.refs.doneModal).show();

                return;
            }

            var displayFile = this.displayFiles[i];
            var config = {
                onUploadProgress: (progressEvent) => {
                    displayFile.completed = (progressEvent.loaded * 100) / progressEvent.total;
                    this.update();
                }
            };
            var data = new FormData();

            data.append('file', this.files[i])
            data.append("filename", this.files[i].name)

            return $.ajax({
                url: this.opts.postUrl,
                type: 'POST',
                data: data,
                dataType: 'json',
                cache: false,
                contentType: false,
                processData: false
            });
        }.bind(this)
});
riot.tag2('campaignitem', '<div if="{!opts.pi}" class="content"> {Crowdsourcing.translate(⁗crowdsourcing__error__no_item_available⁗)} </div><div if="{opts.pi}" class="content"><span if="{this.loading}" class="loader_wrapper"><img riot-src="{this.opts.loaderimageurl}"></span><span if="{this.error}" class="loader_wrapper"><span class="error_message">{this.error.message}</span></span></span><div class="content_left"><imageview if="{this.item}" id="mainImage" source="{this.item.getCurrentCanvas()}" item="{this.item}"></imageView><canvaspaginator if="{this.item}" item="{this.item}"></canvasPaginator></div><div if="{this.item}" class="content_right"><h1 class="content_right__title">{Crowdsourcing.translate(this.item.translations.title)}</h1><div class="questions_wrapper"><div each="{question, index in this.item.questions}" onclick="{setActive}" class="question_wrapper {question.isRegionTarget() ? \'area-selector-question\' : \'\'} {question.active ? \'active\' : \'\'}"><div class="question_wrapper__description">{Crowdsourcing.translate(question.translations.text)}</div><plaintextquestion if="{question.questionType == \'PLAINTEXT\'}" question="{question}" item="{this.item}" index="{index}"></plaintextQuestion><geolocationquestion if="{question.questionType == \'GEOLOCATION_POINT\'}" question="{question}" item="{this.item}" index="{index}"></geoLocationQuestion></div></div><div if="{!item.isReviewMode()}" class="options-wrapper options-wrapper-annotate"><button onclick="{saveAnnotations}" class="options-wrapper__option btn btn--default" id="save">{Crowdsourcing.translate(⁗button__save⁗)}</button><div>{Crowdsourcing.translate(⁗label__or⁗)}</div><button onclick="{submitForReview}" class="options-wrapper__option btn btn--success" id="review">{Crowdsourcing.translate(⁗action__submit_for_review⁗)}</button><div>{Crowdsourcing.translate(⁗label__or⁗)}</div><button if="{this.opts.nextitemurl}" onclick="{skipItem}" class="options-wrapper__option btn btn--link" id="skip">{Crowdsourcing.translate(⁗action__skip_item⁗)}</button></div><div if="{item.isReviewMode()}" class="options-wrapper options-wrapper-review"><button onclick="{acceptReview}" class="options-wrapper__option btn btn--success" id="accept">{Crowdsourcing.translate(⁗action__accept_review⁗)}</button><div>{Crowdsourcing.translate(⁗label__or⁗)}</div><button onclick="{rejectReview}" class="options-wrapper__option btn btn--danger" id="reject">{Crowdsourcing.translate(⁗action__reject_review⁗)}</button><div>{Crowdsourcing.translate(⁗label__or⁗)}</div><button if="{this.opts.nextitemurl}" onclick="{skipItem}" class="options-wrapper__option btn btn--link" id="skip">{Crowdsourcing.translate(⁗action__skip_item⁗)}</button></div></div></div>', '', '', function(opts) {

	this.itemSource = this.opts.restapiurl + "crowdsourcing/campaigns/" + this.opts.campaign + "/" + this.opts.pi + "/";
	this.annotationSource = this.itemSource + "annotations/";
	this.loading = true;
	console.log("item url ", this.itemSource);
	console.log("annotations url ", this.annotationSource);

	this.on("mount", function() {
	    console.log("mount campaignItem");
	    fetch(this.itemSource)
	    .then( response => response.json() )
	    .then( itemConfig => this.loadItem(itemConfig))
	    .then( () => this.fetch(this.annotationSource))
	    .then( response => response.json() )
	    .then( annotations => this.initAnnotations(annotations))
	    .then( () => this.item.notifyItemInitialized())
		.catch( error => {
		    console.error("ERROR ", error);
	    	this.error = error;
	    	this.update();
		})
	});

	this.loadItem = function(itemConfig) {

	    this.item = new Crowdsourcing.Item(itemConfig, 0);
	    this.item.setReviewMode(this.opts.itemstatus && this.opts.itemstatus.toUpperCase() == "REVIEW");
		fetch(this.item.imageSource + "simple/")
		.then( response => response.json() )
		.then( imageSource => this.initImageView(imageSource))
		.then( () => {this.loading = false; this.update()})
		.catch( error => {
		    this.loading = false;
		    console.error("ERROR ", error);
		})

		this.item.onImageRotated( () => this.update());
	}.bind(this)

	this.initImageView = function(imageSource) {
	    this.item.initViewer(imageSource)
	    this.update();
	}.bind(this)

	this.resolveCanvas = function(source) {
	    if(Crowdsourcing.isString(source)) {
	        return fetch(source)
	        .then( response => response.json() );
	    } else {
	        return Q.fcall(() => source);
	    }
	}.bind(this)

	this.initAnnotations = function(annotations) {
	    console.log("init campaign annotations");
	    let save = this.item.createAnnotationMap(annotations);
	    this.item.saveToLocalStorage(save);
	}.bind(this)

	this.resetItems = function() {
	    fetch(this.annotationSource)
	    .then( response => response.json() )
	    .then( annotations => this.initAnnotations(annotations))
	    .then( () => this.resetQuestions())
	    .then( () => this.item.notifyAnnotationsReload())
	    .then( () => this.update())
		.catch( error => console.error("ERROR ", error));
	}.bind(this)

	this.resetQuestions = function() {
	    this.item.questions.forEach(question => {
		    question.loadAnnotationsFromLocalStorage();
		    question.initAnnotations();
	    })
	}.bind(this)

	this.setActive = function(event) {
	    if(event.item.question.isRegionTarget()) {
		    this.item.questions.forEach(q => q.active = false);
		    event.item.question.active = true;
	    }
	}.bind(this)

	this.saveToServer = function() {
	    let pages = this.item.loadAnnotationPages();
	    this.loading = true;
	    this.update();
	    return fetch(this.annotationSource, {
            method: "PUT",
            headers: {
                'Content-Type': 'application/json'
            },
            cache: "no-cache",
            mode: 'cors',
            body: JSON.stringify(pages)
	    })
	}.bind(this)

	this.saveAnnotations = function() {
	    this.saveToServer()
	    .then(() => this.resetItems())
	    .then(() => this.setStatus("ANNOTATE"))
	    .catch((error) => {
	        console.error(error);
	    })
	    .then(() => {
	        this.loading = false;
		    this.update();
	    });
	}.bind(this)

	this.submitForReview = function() {
	    this.saveToServer()
	    .then(() => this.setStatus("REVIEW"))
	    .then(() => this.skipItem());

	}.bind(this)

	this.acceptReview = function() {
	    this.setStatus("FINISHED")
	    .then(() => this.skipItem());
	}.bind(this)

	this.rejectReview = function() {
	    this.setStatus("ANNOTATE")
	    .then(() => this.skipItem());
	}.bind(this)

	this.skipItem = function() {
	    console.log("skip to ", this.opts.nextitemurl);
	    window.location.href = this.opts.nextitemurl;
	}.bind(this)

	this.setStatus = function(status) {
	    let body = {
	            recordStatus: status,
	            creator: this.item.getCreator().id
	    }
	    return fetch(this.itemSource, {
            method: "PUT",
            headers: {
                'Content-Type': 'application/json',
            },
            cache: "no-cache",
            mode: 'cors',
            body: JSON.stringify(body)
	    })
	}.bind(this)

	this.fetch = function(url) {
	    return fetch(url, {
            method: "GET",
            cache: "no-cache",
            mode: 'cors',
	    })
	}.bind(this)

});
riot.tag2('canvaspaginator', '<nav class="numeric-paginator"><ul><li if="{getCurrentIndex() > 0}" class="numeric-paginator__navigate navigate_prev"><span onclick="{this.loadPrevious}"><i class="fa fa-angle-left" aria-hidden="true"></i></span></li><li each="{canvas in this.firstCanvases()}" class="group_left {this.getIndex(canvas) == this.getCurrentIndex() ? \'numeric-paginator__active\' : \'\'}"><span index="{this.getIndex(canvas)}" onclick="{this.loadFromEvent}">{this.getOrder(canvas)}</span></li><li class="numeric-paginator__separator" if="{this.useMiddleButtons()}">...</li><li each="{canvas in this.middleCanvases()}" class="group_middle {this.getIndex(canvas) == this.getCurrentIndex() ? \'numeric-paginator__active\' : \'\'}"><span index="{this.getIndex(canvas)}" onclick="{this.loadFromEvent}">{this.getOrder(canvas)}</span></li><li class="numeric-paginator__separator" if="{this.useLastButtons()}">...</li><li each="{canvas in this.lastCanvases()}" class="group_right {this.getIndex(canvas) == this.getCurrentIndex() ? \'numeric-paginator__active\' : \'\'}"><span index="{this.getIndex(canvas)}" onclick="{this.loadFromEvent}">{this.getOrder(canvas)}</span></li><li if="{getCurrentIndex() < getTotalImageCount()-1}" class="numeric-paginator__navigate navigate_next"><span onclick="{this.loadNext}"><i class="fa fa-angle-right" aria-hidden="true"></i></span></li></ul></nav>', '', '', function(opts) {

this.on( "mount", function() {

    var paginatorConfig = {
	        previous: () => this.load(this.getCurrentIndex()-1),
	        next: () => this.load(this.getCurrentIndex()+1),
	        first: () => this.load(0),
	        last: () => this.load(this.getTotalImageCount()-1),
	}
	viewerJS.paginator.init(paginatorConfig);

})

this.loadFromEvent = function(e) {
    let index = parseInt(e.target.attributes["index"].value);
	this.load(index);
}.bind(this)

this.load = function(index) {
    if(index != this.getCurrentIndex() && index >= 0 && index < this.getTotalImageCount()) {
		this.opts.item.loadImage(index);
		this.update();
    }
}.bind(this)

this.loadPrevious = function() {
    let index = this.getCurrentIndex()-1;
	this.load(index);
}.bind(this)

this.loadNext = function() {
    let index = this.getCurrentIndex()+1;
	this.load(index);
}.bind(this)

this.getCurrentIndex = function() {
    return this.opts.item.currentCanvasIndex;
}.bind(this)

this.getIndex = function(canvas) {
    return this.opts.item.canvases.indexOf(canvas);
}.bind(this)

this.getOrder = function(canvas) {
    return this.getIndex(canvas) + 1;
}.bind(this)

this.getTotalImageCount = function() {
    return this.opts.item.canvases.length;
}.bind(this)

this.useMiddleButtons = function() {
    return this.getTotalImageCount() > 9 && this.getCurrentIndex() > 4 && this.getCurrentIndex() < this.getTotalImageCount()-5;
}.bind(this)

this.useLastButtons = function() {
    return this.getTotalImageCount() > 9;
}.bind(this)

this.firstCanvases = function() {
    if(this.getTotalImageCount() < 10) {
        return this.opts.item.canvases;
    } else if(this.getCurrentIndex() < 5) {
        return this.opts.item.canvases.slice(0, this.getCurrentIndex()+3);
    } else {
        return this.opts.item.canvases.slice(0, 2);
    }
}.bind(this)

this.middleCanvases = function() {
    if(this.getTotalImageCount() < 10) {
        return [];
    } else if(this.getCurrentIndex() < this.getTotalImageCount()-5 && this.getCurrentIndex() > 4) {
        return this.opts.item.canvases.slice(this.getCurrentIndex()-2, this.getCurrentIndex()+3);
    } else {
        return [];
    }
}.bind(this)

this.lastCanvases = function() {
    if(this.getTotalImageCount() < 10) {
        return [];
    } else if(this.getCurrentIndex() < this.getTotalImageCount()-5) {
        return this.opts.item.canvases.slice(this.getTotalImageCount()-2);
    } else {
        return this.opts.item.canvases.slice(this.getCurrentIndex()-2);
    }
}.bind(this)

});














riot.tag2('slideshow', '<a if="{manifest === undefined}" data-linkid="{opts.pis}"></a><figure class="slideshow" if="{manifest !== undefined}" onmouseenter="{mouseenter}" onmouseleave="{mouseleave}"><div class="slideshow__image"><a href="{getLink(manifest)}" class="remember-scroll-position" data-linkid="{opts.pis}" onclick="{storeScrollPosition}"><img riot-src="{getThumbnail(manifest)}" class="{\'active\' : active}" alt="{getLabel(manifest)}" onload="{setImageActive}"></a></div><figcaption><h4>{getTitleOrLabel(manifest)}</h4><p><span each="{md in metadataList}"> {getMetadataValue(manifest, md)} <br></span></p><div if="{pis.length > 1}" class="slideshow__dots"><ul><li each="{imagepi in pis}"><button class="btn btn--clean {\'active\' : pi === imagepi}" onclick="{setPi}"></button></li></ul></div></figcaption></figure>', '', '', function(opts) {

    	$.fn.isInViewport = function() {
        	var elementTop = $( this ).offset().top;
        	var elementBottom = elementTop + $( this ).outerHeight();
        	var elementHeight = $( this ).outerHeight();
        	var viewportTop = $( window ).scrollTop();
        	var viewportBottom = viewportTop + $( window ).height();

        	return elementBottom > (viewportTop + elementHeight) && elementTop < (viewportBottom - elementHeight);
    	};

    	this.pis = this.opts.pis.split(/[\s,;]+/);
    	this.pis = this.pis.filter( function( pi ) {
    		return pi != undefined && pi.length > 0;
    	} );
        this.metadataList = this.opts.metadata.split(/[,;]+/);
        this.manifest = undefined;
        this.manifests = new Map();
        this.active = false;
        this.visible = false;
        this.mouseover = false;

        console.log("tag created")

        this.on( 'mount', function() {
            console.log("tag mounted")
        	this.loadManifest( this.pis[0] );
        }.bind( this ));

        this.mouseenter = function() {
        	this.mouseover = true;
        }.bind(this)

        this.mouseleave = function() {
        	this.mouseover = false;
        }.bind(this)

        this.checkPosition = function() {
        	var slideshow = $( '#' + this.opts.id + ' figure' );

        	if ( !this.visible && this.pis.length > 1 && slideshow.isInViewport() ) {
        		this.visible = true;
            	this.moveSlides( this.pis, true );
        	}
        	else if ( this.visible && !slideshow.isInViewport() ) {
        		this.visible = false;
        		this.moveSlides( this.pis, false );
        	}
        }.bind(this)

        this.moveSlides = function( pis, move ) {
        	var index = 1;

        	if ( move ) {
        		clearInterval( this.interval );

        		this.interval = setInterval( function() {
                	if ( index === pis.length ) {
                		index = 0;
                	}
                	if ( !this.mouseover ) {
            			this.loadManifest( pis[ index ] );
                    	index++;
                	}
                }.bind( this ), 3000 );
        	}
        	else {
        		clearInterval( this.interval );
        	}
        }.bind(this)

        this.setPi = function( event ) {
        	let pi = event.item.imagepi;

        	if ( pi != this.pi ) {
        		this.pi = pi;

        		return this.loadManifest( pi );
        	}
        }.bind(this)

        this.setImageActive = function() {
        	this.active = true;
        	this.update();
        }.bind(this)

        this.loadManifest = function( pi ) {
        	let url = this.opts.manifest_base_url.replace( "{pi}", pi );
        	let json = this.manifests.get( url );
        	this.pi = pi;
        	this.active = false;
        	this.update();

        	if ( !json ) {
        		$.getJSON( url, function( manifest ) {
        			if ( manifest ) {

        				this.manifest = manifest;
        				this.manifests.set( url, manifest );
        				this.update();
        				console.log("manifest loaded");
            			this.checkPosition();

        				$( window ).on( 'resize scroll', function() {
            				this.checkPosition();
        				}.bind( this ) );
        			}
        		}.bind( this ))
        		.then(function(data) {
        		})
        		.catch(function(error) {
        			console.error("error laoding ", url, ": ", error);
        		});
        	}
        	else {

            	setTimeout( function() {
            		this.manifest = json;
            		this.update();
            	}.bind( this ), 300 );
        	}
        }.bind(this)
        this.getThumbnail = function( manifest, width, height ) {
        	if( !manifest.thumbnail.service || ( !width && !height ) ) {
        		return manifest.thumbnail['@id'];
        	}
        	else {
        		let sizePrefix = width && height ? "!" : "";

        		return manifest.thumbnail.service['@id'] + "/full/" + sizePrefix + width + "," + height + "/0/default.jpg";
        	}
        }.bind(this)

        this.getLink = function( manifest ) {
        	rendering = manifest.rendering;

        	if ( Array.isArray( rendering ) ) {
        		rendering = rendering.find( ( rend ) => rend.format == "text/html" );
        	}
        	if ( rendering ) {
        		return rendering['@id'];
        	}
        	else {
        		return '';
        	}
        }.bind(this)

        this.getTitleOrLabel = function( manifest ) {
        	var title = this.getMetadataValue( manifest, 'Title' );

        	if(title) {
        		return title;
        	} else {
        		return getLabel( manifest );
        	}
        }.bind(this)

        this.getLabel = function( manifest ) {
        	return this.getValue(manifest.label, this.opts.locale);
        }.bind(this)

        this.getMetadataValue = function( manifest, metadataLabel ) {
        	if ( manifest && metadataLabel ) {
        		let metadata = manifest.metadata.find( ( md ) => {
        			let label = md.label;
        			if ( Array.isArray( label ) ) {
        				label = label.find( (l) => l['@value'].trim() == metadataLabel.trim());
        				if ( label ) {
        					label = label['@value']
        				}
        			}
        			return label && label.trim() == metadataLabel.trim();
        		});

        		if ( metadata ) {
        			let value = this.getValue( metadata.value, this.opts.locale );

        			return value;
        		}
        	}
        }.bind(this)

        this.getValue = function ( element, locale ) {
            if ( element ) {
            	if ( typeof element === 'string' ) {
            		return element;
            	}
        		else if ( Array.isArray( element ) ) {
            		var fallback;

            		for ( var index in element  ) {
            			var item = element[index];

            			if ( typeof item === 'string' ) {
            				return item;
            			}
            			else {
            				var value = item['@value'];
            				var language = item['@language'];

            				if ( locale == language ) {
            					return value;
            				}
            				else if ( !fallback || language == 'en' ) {
            					fallback = value;
            				}
            			}
            		}

            		return fallback;
            	}
            	else {
            		return element['@value'];
            	}
            }
        }.bind(this)

        this.storeScrollPosition = function(event) {
            $target = $(event.target).closest("a");
            viewerJS.handleScrollPositionClick($target);
        }.bind(this)
});

riot.tag2('fsthumbnailimage', '<div class="fullscreen__view-image-thumb-preloader" if="{preloader}"></div><img ref="image" alt="Thumbnail Image">', '', '', function(opts) {
    	this.preloader = false;

    	this.on('mount', function() {
    		this.createObserver();

    		this.refs.image.onload = function() {
        		this.refs.image.classList.add( 'in' );
				this.opts.observable.trigger( 'imageLoaded', this.opts.thumbnail );
        		this.preloader = false;
        		this.update();
    		}.bind(this);
    	}.bind(this));

    	this.createObserver = function() {
    		var observer;
    		var options = {
    			root: document.querySelector(this.opts.root),
    		    rootMargin: "1000px 0px 1000px 0px",
    		    threshold: 0.8
    		};

    		observer = new IntersectionObserver(this.loadImages, options);
    		observer.observe(this.refs.image);
    	}.bind(this)

    	this.loadImages = function(entries, observer) {
    		entries.forEach( entry => {
    			if (entry.isIntersecting) {
    				this.preloader = true;
    				this.refs.image.src = this.opts.imgsrc;
    				this.update();
    			}
    		} );
    	}.bind(this)
});
riot.tag2('fsthumbnails', '<div class="fullscreen__view-image-thumbs" ref="thumbnailWrapper"><div each="{thumbnail in thumbnails}" class="fullscreen__view-image-thumb"><figure class="fullscreen__view-image-thumb-image"><a href="{thumbnail.rendering[\'@id\']}"><fsthumbnailimage thumbnail="{thumbnail}" observable="{observable}" root=".fullscreen__view-image-thumbs-wrapper" imgsrc="{thumbnail.thumbnail[\'@id\']}"></fsThumbnailImage></a><figcaption><div class="fullscreen__view-image-thumb-image-order {thumbnail.loaded ? \'in\' : \'\'}">{thumbnail.label}</div></figcaption></figure></div></div>', '', '', function(opts) {
        function rmObservable() {
    		riot.observable( this );
    	}

    	this.observable = new rmObservable();
        this.thumbnails = [];
    	this.wrapper = document.getElementsByClassName( 'fullscreen__view-image-thumbs-wrapper' );
    	this.controls = document.getElementsByClassName( 'image-controls' );
    	this.image = document.getElementById( 'imageContainer' );
    	this.viewportWidth;
    	this.sidebarWidth;
    	this.thumbsWidth;

    	this.on( 'mount', function() {
        	$( '[data-show="thumbs"]' ).on( 'click', function(e) {
        		e.currentTarget.classList.toggle('in');

        		if ( e.currentTarget.classList.contains( 'in' ) ) {
            		$( '[data-show="thumbs"]' ).attr( 'title', opts.msg.hideThumbs ).tooltip( 'fixTitle' ).tooltip( 'show' );
        		}
        		else {
            		$( '[data-show="thumbs"]' ).attr( 'title', opts.msg.showThumbs ).tooltip( 'fixTitle' ).tooltip( 'show' );
        		}

        		this.controls[0].classList.toggle( 'faded' );

            	this.viewportWidth = document.getElementById( 'fullscreen' ).offsetWidth;
            	this.sidebarWidth = document.getElementById( 'fullscreenViewSidebar' ).offsetWidth;
            	if ( sessionStorage.getItem( 'fsSidebarStatus' ) === 'false' ) {
                	this.thumbsWidth = this.viewportWidth;
            	}
            	else {
                	this.thumbsWidth = this.viewportWidth - this.sidebarWidth;
            	}

            	$( this.image ).toggle();

        		$( this.wrapper ).width( this.thumbsWidth ).fadeToggle( 'fast' );

            	if ( this.thumbnails.length == 0 ) {

            		$.ajax( {
                        url: opts.thumbnailUrl,
                        type: "GET",
                        datatype: "JSON"
                    } ).then( function( data ) {
                    	this.thumbnails = data;
                    	this.update();
                    }.bind( this ) );
    			}
        	}.bind(this));
    	}.bind( this ) );

    	this.observable.on( 'imageLoaded', function( thumbnail ) {
    		thumbnail.loaded = true;
    		this.update();
    	}.bind( this ) );
});
riot.tag2('geolocationquestion', '<div if="{this.showInstructions()}" class="annotation_instruction"><label>{Crowdsourcing.translate(⁗crowdsourcing__help__create_rect_on_image⁗)}</label></div><div if="{this.showAddMarkerInstructions()}" class="annotation_instruction"><label>{Crowdsourcing.translate(⁗crowdsourcing__help__add_marker_to_image⁗)}</label></div><div if="{this.showInactiveInstructions()}" class="annotation_instruction annotation_instruction_inactive"><label>{Crowdsourcing.translate(⁗crowdsourcing__help__make_active⁗)}</label></div><div id="geoMap_{opts.index}" class="geo-map"></div><div id="annotation_{index}" each="{anno, index in this.annotations}"></div>', '', '', function(opts) {


this.question = this.opts.question;
this.markerIdCounter = 1;
this.addMarkerActive = !this.question.isRegionTarget() && !this.opts.item.isReviewMode();
this.annotationToMark = null;
this.markers = [];

this.on("mount", function() {
	this.opts.item.onItemInitialized( () => {
	    this.question.initializeView((anno) => new Crowdsourcing.Annotation.GeoJson(anno), this.addAnnotation, this.updateAnnotation, this.focusAnnotation);
	    this.initMap();
	    this.opts.item.onImageOpen(() => this.resetFeatures());
	    this.opts.item.onAnnotationsReload(() => this.resetFeatures());
	})
});

this.setView = function(view) {
    this.map.setView(view.center, view.zoom);
}.bind(this)

this.resetFeatures = function() {
    this.markerIdCounter = 1;
    this.setFeatures(this.question.annotations);
    if(this.markers.length > 0) {
   		this.setView(this.markers[0].view);
    }
}.bind(this)

this.setFeatures = function(annotations) {
    this.markers.forEach((marker) => {
        marker.remove();
    })
    this.markers = [];
    annotations.filter(anno => !anno.isEmpty()).forEach((anno) => {
        let markerId = this.addGeoJson(anno.body);
        anno.markerId = markerId;
    });
}.bind(this)

this.addAnnotation = function(anno) {
   this.addMarkerActive = true;
   this.annotationToMark = anno;
   if(this.question.areaSelector) {
       this.question.areaSelector.disableDrawer();
   }
   this.update();
}.bind(this)

this.updateAnnotation = function(anno) {
    this.focusAnnotation(this.question.getIndex(anno));
}.bind(this)

this.focusAnnotation = function(index) {
    let anno = this.question.getByIndex(index);
    if(anno) {
        let marker = this.getMarker(anno.markerId);
        if(marker) {
	        console.log("focus ", anno, marker);

        }
    }
}.bind(this)

this.showInstructions = function() {
    return !this.addMarkerActive && !this.opts.item.isReviewMode() &&  this.question.active && this.question.isRegionTarget();
}.bind(this)

this.showInactiveInstructions = function() {
    return !this.opts.item.isReviewMode() &&  !this.question.active && this.question.isRegionTarget() && this.opts.item.questions.filter(q => q.isRegionTarget()).length > 1;

}.bind(this)

this.showAddMarkerInstructions = function() {
    return this.addMarkerActive && !this.opts.item.isReviewMode() &&  this.question.active && this.question.isRegionTarget() ;

}.bind(this)

this.showAddAnnotationButton = function() {
    return !this.question.isReviewMode() && !this.question.isRegionTarget() && this.question.mayAddAnnotation();
}.bind(this)

this.setNameFromEvent = function(event) {
    event.preventUpdate = true;
    if(event.item.anno) {
        anno.setName(event.target.value);
        this.question.saveToLocalStorage();
    } else {
        throw "No annotation to set"
    }
}.bind(this)

this.initMap = function() {
    this.map = new L.Map('geoMap_' + this.opts.index);
    var osm = new L.TileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      minZoom: 0,
      maxZoom: 20,
      attribution: 'Map data &copy; <a href="https://openstreetmap.org">OpenStreetMap</a> contributors'
    });

    this.map.setView(new L.LatLng(49.451993, 11.073397), 5);
    this.map.addLayer(osm);

    this.locations = L.geoJSON([], {
        pointToLayer: function(geoJsonPoint, latlng) {
            let marker = L.marker(latlng, {
                draggable: !this.opts.item.isReviewMode()
            });

            marker.id = geoJsonPoint.id;
            marker.view = geoJsonPoint.view;

            marker.getId = function() {
                return this.id;
            }

            marker.on("dragend", function(event) {
                var position = marker.getLatLng();
                this.moveFeature(marker, position);
            }.bind(this));

            marker.on("click", function(event) {
                this.removeFeature(marker);
            }.bind(this));

            this.markers.push(marker);

            return marker;
        }.bind(this)
    }).addTo(this.map);

    this.map.on("click", function(e) {
        if(this.addMarkerActive && (this.question.targetFrequency == 0 || this.markers.length < this.question.targetFrequency)) {
	        var location= e.latlng;
	        this.createGeoJson(location, this.map.getZoom(), this.map.getCenter());
	        this.addMarkerActive = !this.question.isRegionTarget();
	        if(this.question.areaSelector) {
	            this.question.areaSelector.enableDrawer();
	        }
        }
    }.bind(this))

}.bind(this)

this.createGeoJson = function(location, zoom, center) {
	let id = this.markerIdCounter++;
    var geojsonFeature = {
        	"type": "Feature",
        	"id": id,
        	"properties": {
        		"name": "",
        	},
        	"geometry": {
        		"type": "Point",
        		"coordinates": [location.lng, location.lat]
        	},
        	"view": {
        	    "zoom": zoom,
        		"center": [location.lat, location.lng]
        	}
        };
    this.locations.addData(geojsonFeature);
    if(this.annotationToMark) {
        this.annotationToMark.markerId = id;
        this.updateFeature(id);
    } else {
    	this.addFeature(id);
    }
}.bind(this)

this.getAnnotation = function(id) {
    return this.question.annotations.find(anno => anno.markerId == id);
}.bind(this)

this.getMarker = function(id) {
    return this.markers.find(marker => marker.getId() == id);
}.bind(this)

this.addGeoJson = function(geoJson) {
    let id = this.markerIdCounter++;
    geoJson.id = id;
    this.locations.addData(geoJson);
    return id;
}.bind(this)

this.updateFeature = function(id) {
    let annotation = this.getAnnotation(id);
    let marker = this.getMarker(annotation.markerId);
    annotation.setBody(marker.toGeoJSON());
    annotation.setView(marker.view);
    this.question.saveToLocalStorage();
}.bind(this)

this.addFeature = function(id) {
    let annotation = this.question.addAnnotation();
    annotation.markerId = id;
    let marker = this.getMarker(id);
    annotation.setBody(marker.toGeoJSON());
    annotation.setView(marker.view);
    this.question.saveToLocalStorage();
}.bind(this)

this.moveFeature = function(marker, location) {
    marker.setLatLng(location);
    let annotation = this.getAnnotation(marker.getId());
    if(annotation) {
        annotation.setGeometry(marker.toGeoJSON().geometry);
        annotation.setView({zoom: this.map.getZoom(), center: [marker.getLatLng().lat, marker.getLatLng().lng]});
    }
    this.question.saveToLocalStorage();
}.bind(this)

this.removeFeature = function(marker) {
    marker.remove();
    let index = this.markers.indexOf(marker);
    this.markers.splice(index, 1);
    let annotation = this.getAnnotation(marker.getId());
    if(annotation) {
	    this.question.deleteAnnotation(annotation);
	    this.question.saveToLocalStorage();
    }
}.bind(this)

});


riot.tag2('imagecontrols', '<div class="image_controls"><div class="image-controls__actions"><div class="image-controls__action rotate-left"><a onclick="{rotateLeft}"><i class="image-rotate_left"></i></a></div><div class="image-controls__action rotate-right"><a onclick="{rotateRight}"><i class="image-rotate_right"></i></a></div><div class="image-controls__action zoom-slider-wrapper"><div class="zoom-slider"><div class="zoom-slider-handle"></div></div></div></div></div>', '', '', function(opts) {
    this.on( "mount", function() {

    } );

    this.rotateRight = function()
    {
        if ( this.opts.image ) {
            this.opts.image.controls.rotateRight();
        }
        if(this.opts.item) {
            this.opts.item.notifyImageRotated(90);
        }
    }.bind(this)

    this.rotateLeft = function()
    {
        if ( this.opts.image ) {
            this.opts.image.controls.rotateLeft();
        }
        if(this.opts.item) {
            this.opts.item.notifyImageRotated(-90);
        }
    }.bind(this)
});
/**
 * Takes a IIIF canvas object in opts.source. 
 * If opts.item exists, it creates the method opts.item.setImageSource(canvas) 
 * and provides an observable in opts.item.imageChanged triggered every time a new image source is loaded (including the first time)
 * The imageView itself is stored in opts.item.image
 */

riot.tag2('imageview', '<div id="wrapper_{opts.id}" class="imageview_wrapper"><span if="{this.error}" class="loader_wrapper"><span class="error_message">{this.error.message}</span></span><imagecontrols if="{this.image}" image="{this.image}" item="{this.opts.item}"></imageControls><div class="image_container"><div id="image_{opts.id}" class="image"></div></div></div>', '', '', function(opts) {

	this.getPosition = function() {
		let pos_os = this.dataPoint.getPosition();
		let pos_image = ImageView.CoordinateConversion.scaleToImage(pos_os, this.image.viewer, this.image.getOriginalImageSize());
		let pos_image_rot = ImageView.CoordinateConversion.convertPointFromImageToRotatedImage(pos_image, this.image.controls.getRotation(), this.image.getOriginalImageSize());
		return pos_image_rot;
	}.bind(this)

	this.on("mount", function() {
		$("#controls_" + opts.id + " .draw_overlay").on("click", function() {
			this.drawing=true;
		}.bind(this));
		try{
			imageViewConfig.image.tileSource = this.getImageInfo(opts.source);
			this.image = new ImageView.Image(imageViewConfig);
			this.image.load()
			.then( (image) => {
				if(this.opts.item) {
					this.opts.item.image = this.image;

				    var now = Rx.Observable.of(image);
					this.opts.item.setImageSource = function(source) {
					    this.image.setTileSource(this.getImageInfo(source));
					}.bind(this);
				    this.opts.item.notifyImageOpened(image.observables.viewerOpen.map(image).merge(now));
				}
				return image;
			})
			.then(function() {
			  	this.update();
			}.bind(this));
		} catch(error) {
		    console.error("ERROR ", error);
	    	this.error = error;
	    	this.update();
		}
	})

	this.getImageInfo = function(canvas) {
	    return canvas.images[0].resource.service["@id"] + "/info.json"
	}.bind(this)

	const imageViewConfig = {
			global : {
				divId : "image_" + opts.id,
				fitToContainer: true,
				adaptContainerWidth: false,
				adaptContainerHeight: false,
				footerHeight: 00,
				zoomSpeed: 1.3,
				allowPanning : true,
			},
			image : {}
	};

	const drawStyle = {
			borderWidth: 2,
			borderColor: "#2FEAD5"
	}

	const lineStyle = {
			lineWidth : 1,
			lineColor : "#EEC83B"
	}

	const pointStyle = ImageView.DataPoint.getPointStyle(20, "#EEC83B");

});


riot.tag2('pdfdocument', '<div class="pdf-container"><pdfpage each="{page, index in pages}" page="{page}" pageno="{index+1}"></pdfPage></div>', '', '', function(opts) {

		this.pages = [];

		var loadingTask = pdfjsLib.getDocument( this.opts.data );
	    loadingTask.promise.then( function( pdf ) {
	        var pageLoadingTasks = [];
	        for(var pageNo = 1; pageNo <= pdf.numPages; pageNo++) {
   		        var page = pdf.getPage(pageNo);
   		        pageLoadingTasks.push(Q(page));
   		    }
   		    return Q.allSettled(pageLoadingTasks);
	    }.bind(this))
	    .then(function(results) {
			results.forEach(function (result) {
			    if (result.state === "fulfilled") {
                	var page = result.value;
                	this.pages.push(page);
                } else {
                    logger.error("Error loading page: ", result.reason);
                }
			}.bind(this));
			this.update();
        }.bind(this))
	    .then( function() {
			$(".pdf-container").show();
            $( '#literatureLoader' ).hide();
		} );

});
riot.tag2('pdfpage', '<div class="page" id="page_{opts.pageno}"><canvas class="pdf-canvas" id="pdf-canvas_{opts.pageno}"></canvas><div class="text-layer" id="pdf-text_{opts.pageno}"></div><div class="annotation-layer" id="pdf-annotations_{opts.pageno}"></div></div>', '', '', function(opts) {
	this.on('mount', function () {
		console.log("load page ", this.opts.pageno, this.opts.page);

           this.container = document.getElementById( "page_" + this.opts.pageno );
           this.canvas = document.getElementById( "pdf-canvas_" + this.opts.pageno );
           this.textLayer = document.getElementById( "pdf-text_" + this.opts.pageno );
           this.annotationLayer = document.getElementById( "pdf-annotations_" + this.opts.pageno );

		var containerWidth = $(this.container).width();
		var pageWidth = this.opts.page._pageInfo.view[2];
           var scale = containerWidth/pageWidth;
		this.viewport = this.opts.page.getViewport( scale );

           if(this.container) {
               this.loadPage();
           }
	});

    this.loadPage = function() {
        var canvasOffset = $( this.canvas ).offset();
        var context = this.canvas.getContext( "2d" );
        this.canvas.height = this.viewport.height;
        this.canvas.width = this.viewport.width;

        console.log( "render ", this.opts.page, context, this.viewport );

        this.opts.page.render( {
            canvasContext: context,
            viewport: this.viewport
        } ).then( function() {
            return this.opts.page.getTextContent();
        }.bind( this ) ).then( function( textContent ) {
            console.log( "viewport ", this.viewport );
            $( this.textLayer ).css( {
                height: this.viewport.height + 'px',
                width: this.viewport.width + 'px',
            } );

            pdfjsLib.renderTextLayer( {
                textContent: textContent,
                container: this.textLayer,
                viewport: this.viewport,
                textDivs: []
            } );

            return this.opts.page.getAnnotations();
        }.bind( this ) ).then( function( annotationData ) {

            $( this.annotationLayer ).css( {
                width: this.viewport.width + 'px',
            } );

            pdfjsLib.AnnotationLayer.render( {
                viewport: this.viewport.clone( {
                    dontFlip: true
                } ),
                div: this.annotationLayer,
                annotations: annotationData,
                page: this.opts.page,
                linkService: {
                    getDestinationHash: function( dest ) {
                        return '#';
                    },
                    getAnchorUrl: function( hash ) {
                        return '#';
                    },
                    isPageVisible: function() {
                        return true;
                    },
                    externalLinkTarget: pdfjsLib.LinkTarget.BLANK,
                }
            } );

        }.bind( this ) )
    }.bind(this)

});
	
	
riot.tag2('plaintextquestion', '<div if="{this.showInstructions()}" class="annotation_instruction"><label>{Crowdsourcing.translate(⁗crowdsourcing__help__create_rect_on_image⁗)}</label></div><div if="{this.showInactiveInstructions()}" class="annotation_instruction annotation_instruction_inactive"><label>{Crowdsourcing.translate(⁗crowdsourcing__help__make_active⁗)}</label></div><div class="annotation_wrapper" id="question_{opts.index}_annotation_{index}" each="{anno, index in this.question.annotations}"><div class="annotation_area"><div if="{this.showAnnotationImages()}" class="annotation_area__image" riot-style="border-color: {anno.getColor()}"><img riot-src="{this.question.getImage(anno)}"></img></div><div class="annotation_area__text_input"><textarea disabled="{this.opts.item.isReviewMode() ? \'disabled\' : \'\'}" onchange="{setTextFromEvent}" riot-value="{anno.getText()}"></textarea></div></div><div class="cms-module__actions"><button if="{!this.opts.item.isReviewMode()}" onclick="{deleteAnnotationFromEvent}" class="annotation_area__button btn btn--clean delete">{Crowdsourcing.translate(⁗action__delete_annotation⁗)} </button></div></div><button if="{showAddAnnotationButton()}" onclick="{addAnnotation}" class="options-wrapper__option btn btn--default" id="add-annotation">{Crowdsourcing.translate(⁗action__add_annotation⁗)}</button>', '', '', function(opts) {

	this.question = this.opts.question;

	this.on("mount", function() {
	    this.question.initializeView((anno) => new Crowdsourcing.Annotation.Plaintext(anno), this.update, this.update, this.focusAnnotation);
	    this.opts.item.onImageOpen(function() {
	        switch(this.question.targetSelector) {
	            case Crowdsourcing.Question.Selector.WHOLE_PAGE:
	            case Crowdsourcing.Question.Selector.WHOLE_SOURCE:
	                if(this.question.annotations.length == 0 && !this.question.item.isReviewMode()) {
	                    this.question.addAnnotation();
	                }
	        }
	        this.update()
	    }.bind(this));
	});

	this.focusAnnotation = function(index) {
	    let id = "question_" + this.opts.index + "_annotation_" + index;
	    let inputSelector = "#" + id + " textarea";
	    this.root.querySelector(inputSelector).focus();
	}.bind(this)

	this.showAnnotationImages = function() {
	    return this.question.isRegionTarget();
	}.bind(this)

	this.showInstructions = function() {
	    return !this.opts.item.isReviewMode() &&  this.question.active && this.question.targetSelector == Crowdsourcing.Question.Selector.RECTANGLE && this.question.annotations.length == 0;
	}.bind(this)

	this.showInactiveInstructions = function() {
	    return !this.opts.item.isReviewMode() &&  !this.question.active && this.question.targetSelector == Crowdsourcing.Question.Selector.RECTANGLE && this.opts.item.questions.filter(q => q.isRegionTarget()).length > 1;

	}.bind(this)

	this.showAddAnnotationButton = function() {
	    return !this.question.isReviewMode() && !this.question.isRegionTarget() && this.question.mayAddAnnotation();
	}.bind(this)

    this.setTextFromEvent = function(event) {
        event.preventUpdate = true;
        if(event.item.anno) {
            event.item.anno.setText(event.target.value);
            this.question.saveToLocalStorage();
        } else {
            throw "No annotation to set"
        }
    }.bind(this)

    this.deleteAnnotationFromEvent = function(event) {
        if(event.item.anno) {
            this.question.deleteAnnotation(event.item.anno);
            this.update();
        }
    }.bind(this)

    this.addAnnotation = function() {
        this.question.addAnnotation();
        this.question.focusCurrentAnnotation();
    }.bind(this)

});
riot.tag2('progressbar', '<div class="goobi-progress-bar-wrapper"><div class="goobi-progress-bar"><div each="{value, index in this.values}" class="goobi-progress-bar__bar {styleClasses[index]}" riot-style="width: {getRelativeWidth(value)};"></div></div></div>', '', '', function(opts) {
	this.values = JSON.parse(this.opts.values);
	this.styleClasses = JSON.parse(this.opts.styleclasses);
	console.log("init progressbar ", this.values, this.styleClasses);

	this.on("mount", function() {
	    let bar = this.root.querySelector(".goobi-progress-bar");
	    this.totalBarWidth = bar.getBoundingClientRect().width;
		this.update();
	})

	this.getRelativeWidth = function(value) {
		    let barWidth = value/this.opts.total*this.totalBarWidth;
		    return barWidth + "px";
	}.bind(this)

	this.loaded = function() {
	    console.log("on load");
	}.bind(this)

});
riot.tag2('questiontemplate', '<div if="{showInstructions()}" class="annotation_instruction"><label>{Crowdsourcing.translate(⁗crowdsourcing__help__create_rect_on_image⁗)}</label></div><div if="{showInactiveInstructions()}" class="annotation_instruction annotation_instruction_inactive"><label>{Crowdsourcing.translate(⁗crowdsourcing__help__make_active⁗)}</label></div><div class="annotation_wrapper" id="question_{opts.index}_annotation_{index}" each="{anno, index in this.question.annotations}"><div class="annotation_area"></div></div><button if="{showAddAnnotationButton()}" onclick="{addAnnotation}" class="options-wrapper__option btn btn--default" id="add-annotation">{Crowdsourcing.translate(⁗action__add_annotation⁗)}</button>', '', '', function(opts) {

this.question = this.opts.question;

this.on("mount", function() {
    this.question.initializeView((anno) => new Crowdsourcing.Annotation.Implementation(anno), this.update, this.update, this.focusAnnotation);
    this.opts.item.onImageOpen(function() {
        this.update()
    }.bind(this));
});

this.focusAnnotation = function(index) {
    let id = "question_" + this.opts.index + "_annotation_" + index;
    let inputSelector = "#" + id + " textarea";
    this.root.querySelector(inputSelector).focus();
}.bind(this)

this.showInstructions = function() {
    return !this.opts.item.isReviewMode() &&  this.question.active && this.question.targetSelector == Crowdsourcing.Question.Selector.RECTANGLE && this.question.annotations.length == 0;
}.bind(this)

this.showInactiveInstructions = function() {
    return !this.opts.item.isReviewMode() &&  !this.question.active && this.question.targetSelector == Crowdsourcing.Question.Selector.RECTANGLE && this.opts.item.questions.filter(q => q.isRegionTarget()).length > 1;

}.bind(this)

this.showAddAnnotationButton = function() {
    return !this.question.isReviewMode() && !this.question.isRegionTarget() && this.question.mayAddAnnotation();
}.bind(this)

this.setBodyFromEvent = function(event) {
    event.preventUpdate = true;
    if(event.item.anno) {

        this.question.saveToLocalStorage();
    } else {
        throw "No annotation to set"
    }
}.bind(this)

this.deleteAnnotationFromEvent = function(event) {
    if(event.item.anno) {
        this.question.deleteAnnotation(event.item.anno);
        this.update();
    }
}.bind(this)

this.addAnnotation = function() {
    this.question.addAnnotation();
    this.question.focusCurrentAnnotation();
}.bind(this)

});


