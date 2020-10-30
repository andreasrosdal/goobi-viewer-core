<annotationBody>
	<plaintextResource if="{isPlaintext()}" resource="{this.annotationBody}" annotationid="{this.opts.annotationid}" ></plaintextResource>
	<htmltextResource if="{isHtml()}" resource="{this.annotationBody}" annotationid="{this.opts.annotationid}"></htmltextResource>
	<geoMapResource if="{isGeoJson()}" resource="{this.annotationBody}" annotationid="{this.opts.annotationid}"></geoMapResource>
	<authorityResource if="{isAuthorityResource()}" resource="{this.annotationBody}" annotationid="{this.opts.annotationid}" currentlang="{this.opts.currentlang}" resturl="{this.opts.resturl}"></authorityResource>
<script>

this.on("mount", () => {
    if(this.opts.contentid) {
        this.annotationBody = JSON.parse(document.getElementById(this.opts.contentid).innerText);
        this.type = this.annotationBody.type;
        if(!this.type) {
            this.type = this.anotationBody["@type"];
        }
        this.format = this.annotationBody.format;
        this.update();
    }
})

isPlaintext() {
    if(this.type == "TextualBody" || this.type == "TextualResource") {
        return !this.format || this.format == "text/plain";
    }
    return false;
}

isHtml() {
    if(this.type == "TextualBody" || this.type == "TextualResource") {
        return this.format == "text/html";
    }
    return false;
}

isGeoJson() {
    return this.type == "Feature";
}

isAuthorityResource() {
    return this.type == "AuthorityResource";
}


</script>

</annotationBody>

