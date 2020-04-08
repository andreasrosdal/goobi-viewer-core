<timematrix>

	<div class="timematrix__objects">
		<div each="{image in imageList}" class="timematrix__content">
			
			<div class="timematrix__img">
				<a href="{image.url}">
					<img src="{image.mediumimage}"> 
					<div class="timematrix__text">	
						<p class="timetext">{image.title[0]}</p>
					</div>
				</a>
			</div>	
		</div> 
	</div>
	 
 <script> 
 this.on( 'mount', function() {
 	$(this.opts.button).on("click", this.updateRange)
 	this.imageList=[]
 })
 
 updateRange(event){
	console.log('event', event)
	this.getTimematrix()
}
 getTimematrix(){
     
     // build api target
     var apiTarget = this.opts.contextPath;
     apiTarget += 'rest/records/timematrix/range/';
     apiTarget += $(this.opts.startInput).val();
     apiTarget += "/";
     apiTarget += $(this.opts.endInput).val();
     apiTarget += '/';
     apiTarget += $(this.opts.count).val();
     apiTarget += '/';
     console.log('apiTarget', apiTarget)
     
     // get data from api
     var promise = fetch(apiTarget)
     
     // render thumbnails
	fetch(apiTarget)
	.then( function(result) {
	    return result.json(); 
	})
	.then( function(json) {
	    console.log("answer", json);
	    this.imageList=json;
	    this.update()
	}.bind(this))
 }
 
 
 console.log('Hello', this.opts);
 </script>

</timematrix>