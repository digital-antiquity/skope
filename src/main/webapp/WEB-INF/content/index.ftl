<!DOCTYPE html PUBLIC 
	"-//W3C//DTD XHTML 1.1 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
	
	<!DOCTYPE html>
<html>
<head>
    <title>SKOPE Prototype</title>
    <meta charset="utf-8" />

    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="components/bootstrap/dist/css/bootstrap.min.css" rel="stylesheet" media="screen">
    <link rel="stylesheet" href="components/c3/c3.css" />
    <link rel="stylesheet" href="components/seiyria-bootstrap-slider/dist/css/bootstrap-slider.min.css">
    <link rel="stylesheet" href="components/leaflet/dist/leaflet.css" /> 
   <style>
   
   .slider {padding-left:40px;;margin-left:50px}
   </style> 
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
</head>
<body>
	<div class="row">
    	<h3>Demo Application</h3>
    	<div><p>Zoom in to the Southwest, click on a point to see detailed information.</p>
    	</div>
		<div id="status" style="font-size:10pt" class="col-md-12"></div>
	</div>
	<div class="row">
	    <div id="mapbox"  class="col-md-6">
	    <div class="row">
	        <div id="map" class="col-md-11" style="height:600px"></div>
	        </div>
	    </div>
	    <div id="infobox" class="col-md-6">
	        <div id="infostatus" class="row">
	            <h3>Detailed Precipitation Information</h3>
	        </div>
	        <div id="precip">
			<p>Click on a point in the map to see detailed temperature and precipitation data</p>
	        </div>
	        <div id="temp"></div>
	        <div id="infodetail" class="hidden"><p>
	        <form class="form-inline" role="form">
	        <div class="row">
		        <div class="col-xs-4">
		            <label for="minx">From:</label>
		            <input name="minx" class="form-control" id="minx" value="0">
		         </div>
		         <div class="col-xs-4">
	 	          <label for="minx">To:</label>
		          <input name="maxx" id="maxx" value="2000"  class="form-control" />
		         </div>
		         <div class="col-xs-4">
		         <br>
		         <input name="reset" class="btn button btn-primary" id="reset-time" value="reset"/>.
		       	 </div>
	       	 </div>
	         </form></p></div>
	    </div>
	</div>
<div class="row">
    <div class="col-md-12">
    <div class="btn-group" role="group" aria-label="...">
        <button name="reset"  class="btn-default btn" id="reset"><span class="glyphicon glyphicon-fast-backward" aria-hidden="true"></span></button>
        <button name="play" class="btn-default btn" id="play"><span class="glyphicon glyphicon-play" aria-hidden="true"></span></button>
        <input id="slider" data-slider-id='ex1Slider' type="text" data-slider-min="0" data-slider-max="2000" data-slider-step="1" data-slider-value="0"/>
        <button name="pause"  class="btn-default btn" id="pause"><span class="glyphicon glyphicon-pause" aria-hidden="true"></span></button>
    </div><span id="time"></span>
    </div>
</div>

<div id="images" class="hidden">
<#assign maxTime = 2000 />

  <#list 0 .. maxTime as time>
    <img data-src="/browse/img/precip${time?c}.png" id="p${time?c}" src="blank.png" />
  </#list>
  <#list 0 .. maxTime as time>
    <img data-src="/browse/img/temp${time?c}.png" id="t${time?c}" src="blank.png" />
  </#list>
</div>
    <script src="components/jquery/dist/jquery.js"></script>
    <script src="components/seiyria-bootstrap-slider/dist/bootstrap-slider.min.js">
    <script src="components/bootstrap/dist/js/bootstrap.min.js"></script>
    <script src="components/leaflet/dist/leaflet.js"></script>
    <script src="components/d3/d3.js"></script>
    <script src="components/jquery.preload/jquery.preload.js"></script>
    <script src="components/c3/c3.js"></script>
    <script src="components/chroma-js/chroma.min.js"></script>
    <script src="js/skope.js"></script>

<script>
// GLOBALS:
var indexName = "skope";
var max = 800;
var detail = 160;
var maxTime = ${maxTime?c};
var shouldContinue = true;
var ajax;
if (indexName != "skope") {
    max = 120;
    detail = 20;
}
var lnks = new Array();



$( document ).ready(function() {
    var sld = $('#slider');
    //https://github.com/seiyria/bootstrap-slider
    sld.slider({
        formatter: function(value) {
            return 'Current value: ' + value;
        }
    });

    sld.on("slide", function(slideEvt) {
        $("#time").text(slideEvt.value);
        drawRaster();
	});
    resetGrid();
    
    drawRaster();
});
/*
    $(function () {

    var $links = $('#images a').each(function(l,m) {
        lnks.push($(m).attr("href"));
    });
    //http://flesler.blogspot.com/2008/01/jquerypreload.html    
    //
    setTimeout(lazyLoadImages,1000);
    });

function lazyLoadImages() {
    var sub = lnks.splice(0,10);
    $.preload(sub);
    console.log("lazyLoadImages:"+ sub);
    if (lnks.length > 0) {
    setTimeout(lazyLoadImages,1000);
    }
};
    */

</script>
Data is Copyright &copy; 2015, PRISM Climate Group, Oregon State University, http://prism.oregonstate.edu .
</div>
</body>
</html>
	