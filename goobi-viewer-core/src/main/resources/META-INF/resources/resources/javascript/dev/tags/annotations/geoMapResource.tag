<geoMapResource>

	<div id="geomap_{opts.annotationid}" class="annotation__body__geomap geomap" />

<script>

this.on("mount", () => {
	this.feature = this.opts.resource;
	this.config = {
	        popover: undefined,
	        mapId: "geomap_" + this.opts.annotationid,
	        fixed: true,
	        clusterMarkers: false,
	        initialView : this.opts.initialview,
	    };
    this.geoMap = new viewerJS.GeoMap(this.config);
    let view = this.feature.view;
    let features = [this.feature];
    this.geoMap.init(view, features);
    
});


</script>

</geoMapResource>