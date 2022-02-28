<campaignItem>

	<div if="{!opts.pi}" class="crowdsourcing-annotations__content-wrapper">
		{Crowdsourcing.translate("crowdsourcing__error__no_item_available")}
	</div>

	<div if="{opts.pi}" class="crowdsourcing-annotations__content-wrapper">
	<span if="{ this.loading }" class="crowdsourcing-annotations__loader-wrapper">
		<img src="{this.opts.loaderimageurl}" /> 
	</span>
	</span>
		<div class="crowdsourcing-annotations__content-left" >
			<imageView if="{this.item}" id="mainImage" source="{this.item.getCurrentCanvas()}" item="{this.item}"></imageView>
		</div> 
		<div if="{this.item}" class="crowdsourcing-annotations__content-right">
			<!-- <h1 class="crowdsourcing-annotations__content-right-title">{Crowdsourcing.translate(this.item.translations.title)}</h1>-->
			<div class="crowdsourcing-annotations__questions-wrapper" >
				<div each="{question, index in this.item.questions}" 
					onclick="{setActive}"
					class="crowdsourcing-annotations__question-wrapper {question.isRegionTarget() ? 'area-selector-question' : ''} {question.active ? 'active' : ''}" >
					<div class="crowdsourcing-annotations__question-wrapper-description">{Crowdsourcing.translate(question.text)}</div>
					<plaintextQuestion if="{question.questionType == 'PLAINTEXT'}" question="{question}" item="{this.item}" index="{index}"></plaintextQuestion>
					<richtextQuestion if="{question.questionType == 'RICHTEXT'}" question="{question}" item="{this.item}" index="{index}"></richtextQuestion>
					<geoLocationQuestion if="{question.questionType == 'GEOLOCATION_POINT'}" question="{question}" item="{this.item}" index="{index}"></geoLocationQuestion>
					<authorityResourceQuestion if="{question.questionType == 'NORMDATA'}" question="{question}" item="{this.item}" index="{index}"></authorityResourceQuestion>
					<metadataQuestion if="{question.questionType == 'METADATA'}" question="{question}" item="{this.item}" index="{index}"></metadataQuestion>
				</div>
			</div>
			<campaignItemLog if="{item.showLog}" item="{item}"></campaignItemLog>
			
			<!-- RECORD STATISTIC MODE -->
			<div if="{!item.pageStatisticMode && !item.isReviewMode()}" class="crowdsourcing-annotations__options-wrapper crowdsourcing-annotations__options-wrapper-annotate">
				<button onclick="{saveAnnotations}" class="crowdsourcing-annotations__options-wrapper__option btn btn--default" id="save">{Crowdsourcing.translate("button__save")}</button>
				<div>{Crowdsourcing.translate("label__or")}</div>
				<button if="{item.isReviewActive()}" onclick="{submitForReview}" class="options-wrapper__option btn btn--success" id="review">{Crowdsourcing.translate("action__submit_for_review")}</button>
				<button if="{!item.isReviewActive()}" onclick="{saveAndAcceptReview}" class="options-wrapper__option btn btn--success" id="review">{Crowdsourcing.translate("action__accept_review")}</button>
				<div>{Crowdsourcing.translate("label__or")}</div>
				<button if="{this.opts.nextitemurl}" onclick="{skipItem}" class="options-wrapper__option btn btn--link" id="skip">{Crowdsourcing.translate("action__skip_item")}</button>
			</div>
			<div if="{!item.pageStatisticMode && item.isReviewActive() && item.isReviewMode()}" class="crowdsourcing-annotations__options-wrapper crowdsourcing-annotations__options-wrapper-review">
				<button onclick="{acceptReview}" class="options-wrapper__option btn btn--success" id="accept">{Crowdsourcing.translate("action__accept_review")}</button>
				<div>{Crowdsourcing.translate("label__or")}</div>
				<button onclick="{rejectReview}" class="options-wrapper__option btn btn--default" id="reject">{Crowdsourcing.translate("action__reject_review")}</button>
				<div>{Crowdsourcing.translate("label__or")}</div>
				<button if="{this.opts.nextitemurl}" onclick="{skipItem}" class="options-wrapper__option btn btn--link" id="skip">{Crowdsourcing.translate("action__skip_item")}</button>
			</div>
			
			<!-- PAGE STATISTIC MODE -->
			<div if="{item.pageStatisticMode && !item.isReviewMode()}" class="crowdsourcing-annotations__options-wrapper crowdsourcing-annotations__options-wrapper-annotate">
				<button onclick="{savePageAnnotations}" class="crowdsourcing-annotations__options-wrapper__option btn btn--default" id="save">{Crowdsourcing.translate("button__save_page")}</button>
				<div>{Crowdsourcing.translate("label__or")}</div>
				<button if="{item.isReviewActive()}" onclick="{submitPageForReview}" class="options-wrapper__option btn btn--success" id="review">{Crowdsourcing.translate("action__submit_page_for_review")}</button>
				<button if="{!item.isReviewActive()}" onclick="{saveAndAcceptReviewForPage}" class="options-wrapper__option btn btn--success" id="review">{Crowdsourcing.translate("action__accept_page_review")}</button>
				<div>{Crowdsourcing.translate("label__or")}</div>
				<button if="{this.opts.nextitemurl}" onclick="{skipItem}" class="options-wrapper__option btn btn--link" id="skip">{Crowdsourcing.translate("action__skip_item")}</button>
			</div>
			<div if="{item.pageStatisticMode && item.isReviewActive() && item.isReviewMode()}" class="crowdsourcing-annotations__options-wrapper crowdsourcing-annotations__options-wrapper-review">
				<button onclick="{acceptReviewForPage}" class="options-wrapper__option btn btn--success" id="accept">{Crowdsourcing.translate("action__accept_page_review")}</button>
				<div>{Crowdsourcing.translate("label__or")}</div>
				<button onclick="{rejectReviewForPage}" class="options-wrapper__option btn btn--default" id="reject">{Crowdsourcing.translate("action__reject_page_review")}</button>
				<div>{Crowdsourcing.translate("label__or")}</div>
				<button if="{this.opts.nextitemurl}" onclick="{skipItem}" class="options-wrapper__option btn btn--link" id="skip">{Crowdsourcing.translate("action__skip_item")}</button>
			</div>
			
		</div>
	 </div>

<script>

	this.itemSource = this.opts.restapiurl + "crowdsourcing/campaigns/" + this.opts.campaign + "/" + this.opts.pi + "/";
	this.annotationSource = this.itemSource + "annotations/";
	this.loading = true;
		
	this.on("mount", function() {
	    fetch(this.itemSource)
	    .then(response => this.handleServerResponse(response))
	    .then( itemConfig => this.loadItem(itemConfig))
	    .then( () => this.fetch(this.annotationSource))
	    .then(response => this.handleServerResponse(response))
	    .then( annotations => this.initAnnotations(annotations))
	    .then( () => this.item.notifyItemInitialized())
		.catch( error => {
		   	this.handleError(error);
			this.item = undefined;
	    	this.loading = false;
	    	this.update();
		})
	});


	loadItem(itemConfig) {
	    this.item = new Crowdsourcing.Item(itemConfig, 0);
	    this.item.logEndpoint = this.item.id + "/" + this.opts.pi + "/log/";
	    if(this.opts.currentuserid) {
	        this.item.setCurrentUser(this.opts.currentuserid, this.opts.currentusername, this.opts.currentuseravatar);
	    }
	    this.item.nextItemUrl = this.opts.nextitemurl;
	    this.item.setReviewMode(this.opts.itemstatus && this.opts.itemstatus.toUpperCase() == "REVIEW");
		this.item.onImageRotated( () => this.update());
		return fetch(this.item.imageSource)
		.then(response => this.handleServerResponse(response))
		.then( imageSource => this.item.initViewer(imageSource))
		.then( () => this.loading = false)
		.then( () => this.update())
		.then( () => this.item.onImageOpen( () => {this.loading = false; this.update()}))
		.then( () => this.item.statusMapUpdates.subscribe( statusMap => this.update()))

	}

	
	resetQuestions() {
	    this.item.questions.forEach(question => {
		    question.loadAnnotationsFromLocalStorage();
		    question.initAnnotations();
	    })
	}
	
	setActive(event) {
	    if(event.item.question.isRegionTarget()) {	        
		    this.item.questions.forEach(q => q.active = false);
		    event.item.question.active = true;
	    }
	}
	
	/**
	* Write all given annotations to local storage
	*/
	initAnnotations(annotations) {
	    let save = this.item.createAnnotationMap(annotations);
	    this.item.saveToLocalStorage(save);
	}
	
	/**
	*	Replace the annnotations for the given pageId in localStorage with the given annotations
	*/
	initAnnotationsForPage(annotations, pageId) {
	    annotations = annotations.filter( (anno) => pageId == Crowdsourcing.getResourceId(anno.target) );
	    let save = this.item.getFromLocalStorage();
	    this.item.deleteAnnotations(save, pageId);
        this.item.addAnnotations(annotations, save);
	    //let save = this.item.createAnnotationMap(annotations);
	    this.item.saveToLocalStorage(save);
	}
	
	/**
	* Load all annotations from REST endpoint
	*/
	resetItems() {
	    fetch(this.annotationSource)
	    .then( response => response.json() )
	    .then( annotations => this.initAnnotations(annotations))
	    .then( () => this.resetQuestions())
	    .then( () => this.item.notifyAnnotationsReload())
	    .then( () => this.update())
		.catch( error => console.error("ERROR ", error));  
	}
	
	resetItemsForPage() {
	    fetch(this.annotationSource)
	    .then( response => response.json() )
	    .then( annotations => this.initAnnotationsForPage(annotations, this.item.getCurrentPageId()))
	    .then( () => this.resetQuestions())
	    .then( () => this.item.notifyAnnotationsReload())
	    .then( () => this.update())
		.catch( error => console.error("ERROR ", error));  
	}
	

 	
	saveToServer() {
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
	    .then( res => res.ok ? res : Promise.reject(res) )
	    .then(res => {this.item.dirty=false; return res});
	}
	
	savePageToServer() {
	    let pages = this.item.loadAnnotationPages(undefined, this.item.getCurrentPageId());
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
	    .then( res => res.ok ? res : Promise.reject(res) )
	    .then(res => {this.item.dirty=false; return res});

	}
	
	saveAnnotations() {
	    this.saveToServer()
	    .then(() => this.resetItems())
	    .then(() => this.setStatus("ANNOTATE"))
	    .then((res) => {
	        this.loading = false;
	        viewerJS.notifications.success(Crowdsourcing.translate("crowdsourcing__save_annotations__success"));
		    this.update();
	    })
	    .catch((error) => {
	        this.loading = false;
	        console.log("Error saving page annotations ", error);
	        viewerJS.notifications.error(Crowdsourcing.translate("crowdsourcing__save_annotations__error"));
		    this.update();
	    })
	}
	
	savePageAnnotations() {
	    this.savePageToServer()
	    .then(() => this.resetItemsForPage())
	    .then(() => this.setStatus("ANNOTATE"))
	    .then((res) => {
	        this.loading = false;
	        viewerJS.notifications.success(Crowdsourcing.translate("crowdsourcing__save_annotations__success"));
		    this.update();
	    })
	    .catch((error) => {
	        this.loading = false;
	        console.log("Error saving page annotations ", error);
	        viewerJS.notifications.error(Crowdsourcing.translate("crowdsourcing__save_annotations__error"));
		    this.update();
	    })
	}
	
	submitForReview() {
	    this.saveToServer()
	    .then(() => this.setStatus("REVIEW"))
	    .then(() => this.skipItem());
	}
	
	submitPageForReview() {
	    this.savePageToServer()
	    .then(() => this.setPageStatus("REVIEW"))
	    .then(() => this.skipPage());
	}


	saveAndAcceptReview() {
	    this.saveToServer()
	    .then(() => this.setStatus("FINISHED"))
	    .then(() => this.skipItem());
	}
	
	saveAndAcceptReviewForPage() {
	    this.savePageToServer()
	    .then(() => this.setPageStatus("FINISHED"))
	    .then(() => this.skipPage());
	}
	
	acceptReview() {
	    this.setStatus("FINISHED")
	    .then(() => this.skipItem());
	}
	
	acceptReviewForPage() {
	    this.setPageStatus("FINISHED")
	    .then(() => this.skipPage());
	}

	rejectReview() {
	    this.setStatus("ANNOTATE")
	    .then(() => this.skipItem());
	}
	
	rejectReviewForPage() {
	    this.setPageStatus("ANNOTATE")
	    .then(() => this.skipPage());
	}
	
	skipItem() {
		this.item.loadNextItem(true);
	}
	
	skipPage() {
	    let index = this.item.getNextAccessibleIndex(this.item.currentCanvasIndex);
	    if(index == undefined) {
	        this.skipItem();
	    } else {
	        this.item.loadImage(index);
	    }
	}
	
	/**
	* The same as setStatus since the server decides if status of record or page should be updated
	*/
	setPageStatus(status) {
	    return this.setStatus(status);
	}
	
	/**
	* Set the status of the current page or record (depending on pageStatisticMode) to the given status
	*/
	setStatus(status) {
	    let body = {
	            recordStatus: status,
	            creator: this.item.getCreator().id,
	    }
	    return fetch(this.itemSource + (this.item.currentCanvasIndex + 1 ) + "/", {
            method: "PUT",
            headers: {
                'Content-Type': 'application/json',
            },
            cache: "no-cache",
            mode: 'cors', // no-cors, cors, *same-origin
            body: JSON.stringify(body)
	    })
	}

	fetch(url) {
	    return fetch(url, {
            method: "GET",
            cache: "no-cache",
            mode: 'cors', // no-cors, cors, *same-origin
	    })
	}
	
	handleServerResponse(response) {
   		if(!response.ok){
   			try {
   				throw response.json()
   			} catch(error) {
   				response.message = error;
   				throw response;
   			}
   		} else {
   			try {
   				return response.json();
   			} catch(error) {
   				response.message = error;
   				throw response;
   			}
   		}
	}
	
	handleError(error) {
		 console.error("ERROR", error);
		    if(viewerJS.isString(error)) {
		    	viewerJS.notifications.error(error);
		    } else if(error.message && error.message.then) {
		    	error.message.then((err) => {
			    	console.log("error ", err)
			    	let errorMessage = "Error retrieving data from <br/>";
			    	errorMessage += error.url + "<br/><br/>";
			    	if(err.message) {
			    		errorMessage += "Message = " + err.message + "<br/><br/>";
			    	}
			    	errorMessage += "Status = " + error.status;
			    	viewerJS.notifications.error(errorMessage);
		    	})
		    } else {		    	
		    	let errorMessage = "Error retrieving data from\n\n";
		    	errorMessage += error.url + "\n\n";
		    	if(error.message) {
		    		errorMessage += "Message = " + error.message + "\n\n";
		    	}
		    	errorMessage += "Status = " + error.status;
		    	viewerJS.notifications.error(errorMessage);
		    } 
	}


</script>

</campaignItem>