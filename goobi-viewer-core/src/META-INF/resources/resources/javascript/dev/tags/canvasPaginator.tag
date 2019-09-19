<canvasPaginator>

<nav class="numeric-paginator">

	<ul>
		<li each="{canvas in this.firstCanvases()}" class="group_left {this.getIndex(canvas) == this.getCurrentIndex() ? 'numeric-paginator__active' : ''}" >
			<span  index="{this.getIndex(canvas)}" onclick="{this.loadFromEvent}">{this.getOrder(canvas)}</span>
		</li>
		<li class="numeric-paginator__separator" if="{this.useMiddleButtons()}">...</li>
		<li each="{canvas in this.middleCanvases()}" class="group_middle {this.getIndex(canvas) == this.getCurrentIndex() ? 'numeric-paginator__active' : ''}"  >
			<span index="{this.getIndex(canvas)}" onclick="{this.loadFromEvent}">{this.getOrder(canvas)}</span>
		</li>
		<li class="numeric-paginator__separator" if="{this.useLastButtons()}">...</li>
		<li each="{canvas in this.lastCanvases()}" class="group_right {this.getIndex(canvas) == this.getCurrentIndex() ? 'numeric-paginator__active' : ''}" >
			<span index="{this.getIndex(canvas)}"  onclick="{this.loadFromEvent}">{this.getOrder(canvas)}</span>
		</li>
	</ul>

</nav>

<script>

this.on( "mount", function() {
    
    var paginatorConfig = {
	        previous: () => this.load(this.getCurrentIndex()-1),
	        next: () => this.load(this.getCurrentIndex()+1),
	        first: () => this.load(0),
	        last: () => this.load(this.getTotalImageCount()-1),
	}
	viewerJS.paginator.init(paginatorConfig);
    
})

loadFromEvent(e) {
    let index = parseInt(e.target.attributes["index"].value);
	this.load(index);
}
    
load(index) {
    console.log("Loading image ",index+1);
    if(index != this.getCurrentIndex() && index >= 0 && index < this.getTotalImageCount()) {        
		this.opts.item.loadImage(index);
		this.update();
    }
}



getCurrentIndex() {
    return this.opts.item.currentCanvasIndex;
}

getIndex(canvas) {
    return this.opts.item.canvases.indexOf(canvas);
}

getOrder(canvas) {
    return this.getIndex(canvas) + 1;
}

getTotalImageCount() {
    return this.opts.item.canvases.length;
}

useMiddleButtons() {
    return this.getTotalImageCount() > 9 && this.getCurrentIndex() > 4 && this.getCurrentIndex() < this.getTotalImageCount()-5;
}

useLastButtons() {
    return this.getTotalImageCount() > 9;
}

firstCanvases() {
    if(this.getTotalImageCount() < 10) {
        return this.opts.item.canvases;
    } else if(this.getCurrentIndex() < 5) {
        return this.opts.item.canvases.slice(0, this.getCurrentIndex()+3);
    } else {
        return this.opts.item.canvases.slice(0, 2);
    }
}

middleCanvases() {
    if(this.getTotalImageCount() < 10) {
        return [];
    } else if(this.getCurrentIndex() < this.getTotalImageCount()-5 && this.getCurrentIndex() > 4) {
        return this.opts.item.canvases.slice(this.getCurrentIndex()-2, this.getCurrentIndex()+3);
    } else {
        return [];
    }
}

lastCanvases() {
    if(this.getTotalImageCount() < 10) {
        return [];
    } else if(this.getCurrentIndex() < this.getTotalImageCount()-5) {
        return this.opts.item.canvases.slice(this.getTotalImageCount()-2);
    } else {
        return this.opts.item.canvases.slice(this.getCurrentIndex()-2);
    }
}


</script>

</canvasPaginator>