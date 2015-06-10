<!DOCTYPE html PUBLIC 
	"-//W3C//DTD XHTML 1.1 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
	
	<!DOCTYPE html>
<html>
<head>
    <title>SKOPE: Synthesizing Knowledge of Past Environments</title>
    <meta charset="utf-8" />

    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="components/bootstrap/dist/css/bootstrap.min.css" rel="stylesheet" media="screen">
    <link rel="stylesheet" href="components/c3/c3.css" />
    <link rel="stylesheet" href="components/seiyria-bootstrap-slider/dist/css/bootstrap-slider.min.css">
    <link rel="stylesheet" href="components/leaflet/dist/leaflet.css" /> 
   <style>
    .leaflet-image-layer {cursor:initial}   
   .slider {padding-left:40px;;margin-left:50px}
   .disabled label {color:#aaa}
   </style> 
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
</head>
<body>
	<div class="row">
    	<h3>SKOPE Demo</h3>
    	<div><p><b>Reconstructed Annual Precipitation & Average Temperature using <a href="http://www.envirecon.org/?page_id=61">PaleoCAR</a></b></p>
    	US Southwest AD 1- AD 2000;  800m Resolution Data available for the shaded area.</b></p>
    	<p><ul>
                <li>Click on a location to graph reconstructed data for that point. Pan by dragging the map, zoom using the +/-.</li> 
                <li>Refine the temporal interval by entering From and To years and clicking Plot. </li> 
                <li>Placing the cursor on the graphed data will display the year’s exact reconstructed values.</li>  
                <li>Click the <span class="glyphicon glyphicon-play" aria-hidden="true"></span> button below, to play a map animation of the reconstructed data  for the entire shaded area within the map window.  This animation shows the extent to which the reconstructed values covary across the map.</li></ul></p>
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
	            <h3>Detailed Precipitation &amp; Temperature Information</h3>
	        </div>
	        <div id="precip">
			<p>Click on a point in the map to see detailed temperature and precipitation data</p>
	        </div>
	        <div id="temp"></div>
	        <div id="infodetail" class="hidden"><p>
	        <form class="form-inline" role="form">

                    <div class="checkbox">
                      <label>
                        <input type="checkbox" value="P" id="P" checked> Annual Precipitation (mm)
                      </label>
                    </div><br/>
                    <div class="checkbox disabled">
                      <label>
                        <input type="checkbox" value="growing_precip" disabled> Growing Season Precipitation (mm)
                      </label>
                    </div><br/>
                    <div class="checkbox">
                      <label>
                        <input type="checkbox" value="T" id="T" checked> Average Temperature (&deg;F)
                      </label>
                    </div><br/>
                    <div class="checkbox disabled">
                      <label>
                        <input type="checkbox" value="growing_temp_avg" disabled> Average Growing Season Temperature  (&deg;F)
                      </label>
                    </div><br/>
                    <div class="checkbox disabled">
                      <label>
                        <input type="checkbox" value="growing_deg_days" disabled> Growing Degree Days
                      </label>
                    </div><br/><br/>
                              
                 <button name="plot" class="btn button btn-primary input-sm" id="plot" style="width:70px" onClick="return false;">plot</button>

	           <a href="#" class="btn btn-default" id="downloadLink">Download Results</a>
	       	 </div>
	         </form></p><br>
	         </div>
	    </div>
	</div>
<div class="row">
    <div class="col-md-12">
            <form class="form-inline" role="form">
    <div class="btn-group" role="group" aria-label="...">
        <button name="pause"  class="btn-default btn" id="pause"><span class="glyphicon glyphicon-pause" aria-hidden="true"></span></button>
        <button name="play" class="btn-default btn" id="play"><span class="glyphicon glyphicon-play" aria-hidden="true"></span></button>
        <input id="slider" data-slider-id='ex1Slider' type="text" data-slider-min="0" data-slider-max="2000" data-slider-step="1" data-slider-value="0"/>
        <button name="reset"  class="btn-default btn" id="reset"><span class="glyphicon glyphicon-fast-backward" aria-hidden="true"></span></button>
    </div><span id="time"></span>
            <div class="form-group">
               <p><b>Display Dates 
                    <label for="minx">from </label>
                    <input name="minx" class="form-control input-sm" id="minx" value="0" style="width:70px" >
                  <label for="minx"> to </label>
                  <input name="maxx" id="maxx" value="2000"  class="form-control input-sm " style="width:70px" /></b>
                <button name="reset" class="btn button btn-default input-sm" id="reset-time" style="width:70px">reset</button>
.</p>
</div>
</div>
</form>
<div id="images" class="hidden">
<#assign maxTime = 2000 />

  <#list 0 .. maxTime as time>
    <img data-src="/browse/img/precip${time?c}.png" id="p${time?c}" src="blank.png" />
  </#list>
  <#list 0 .. maxTime as time>
    <img data-src="/browse/img/temp${time?c}.png" id="t${time?c}" src="blank.png" />
  </#list>
</div>

    <script src="components/leaflet/dist/leaflet.js"></script>
    <script src="components/jquery/dist/jquery.js"></script>
    <script src="components/bootstrap/dist/js/bootstrap.min.js"></script>
    <script src="components/d3/d3.js"></script>
    <script src="components/jquery.preload/jquery.preload.js"></script>
    <script src="components/blob-util/dist/blob-util.min.js"></script>
    <script src="components/seiyria-bootstrap-slider/dist/bootstrap-slider.min.js"></script>
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
    initSlider();
    resetGrid();
    
    drawRaster();
});


</script>
</div>
</body>
</html>
	