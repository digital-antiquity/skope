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
    <link rel="stylesheet" href="components/leaflet-draw/dist/leaflet.draw.css" /> 
    <link rel="stylesheet" href="css/skope.css" /> 
    
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">

<script>
  (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
  (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
  m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
  })(window,document,'script','//www.google-analytics.com/analytics.js','ga');

  ga('create', 'UA-18619197-2', 'auto');
  ga('send', 'pageview');

</script>
</head>
<body>
	<div class="row">
    	<div><p><b>Reconstructed Annual Precipitation & Average Temperature using <a href="http://www.envirecon.org/?page_id=61">PaleoCAR</a></b></p>
    	US Southwest AD 1- AD 2000;  800m Resolution Data available for the shaded area.</b></p>
    	<p><ul>
                <li>Click on a location to graph reconstructed data for that point. Pan by dragging the map, zoom using the +/-.</li> 
                <li>Refine the temporal interval by entering From and To years. </li> 
                <!-- <li>Placing the cursor on the graphed data will display the year’s exact reconstructed values.</li> -->  
                <li>Click the <span class="glyphicon glyphicon-play" aria-hidden="true"></span> button below, to play a map animation of the reconstructed data  for the entire shaded area within the map window.  This animation shows the extent to which the reconstructed values covary across the map.</li></ul></p>
                <li>More info in the <a href="http://www.envirecon.org/skope-prototype-users-guide">User Guide</a>
    	</div>
		<div id="status" style="font-size:10pt" class="col-md-12"></div>
	</div>
	<div class="row">
	    <div id="mapbox"  class="col-md-4">
	    <div class="row">
	        <div id="map" class="col-md-11" style="height:600px"></div>
	        </div>
	    </div>
	    <div id="infobox" class="col-md-8">
	        <div id="infostatus" class="row">
	            <h3>Detailed Precipitation &amp; Temperature Information</h3>
	        </div>
	        <div class="row well">
	                    <form class="form-inline" role="form">
	                     <div class="form-group">
               <p><b>Display Dates 
                    <label for="minx">from </label>
                    <input name="minx" class="form-control" id="minx" value="0" style="width:70px" >
                  <label for="minx"> to </label>
                  <input name="maxx" id="maxx" value="2000"  class="form-control " style="width:70px" /></b>
                <button name="reset" type="button" class="btn button btn-default" id="reset-time" style="width:70px">
                <span>reset</span></button>.&nbsp;&nbsp;&nbsp;&nbsp;
	        <span id="infodetail" class="hidden">
                 <button name="plot" class="btn button btn-primary input-sm" id="plot" style="width:70px;display:none;visibility:hidden" onClick="return false;">plot</button>

	           <a href="#" class="btn btn-default" id="downloadLink">Download Results</a>
	       	 </span></p>

	        <div id="coordinates"></div>
         </div>
                
</div>
</form>	         
	        
	        <div id="precip" class="">
			<p>Click on a point in the map to see detailed temperature and precipitation data</p>
	        </div>
	    </div>
	</div>
	</div>
<div class="row">
    <div class="col-md-12">
            <form class="form-inline" role="form">
    <div class="btn-group" role="group" aria-label="...">
<!--         <button name="pause" type="button"  class="btn-default btn" id="pause"><span class="glyphicon glyphicon-pause" aria-hidden="true"></span></button> -->
        <button name="play"  type="button" class="btn-default btn" id="play"><span class="glyphicon glyphicon-play" aria-hidden="true"></span></button>
        <input id="slider" data-slider-id='ex1Slider' type="text" data-slider-min="0" data-slider-max="2000" data-slider-step="1" data-slider-value="0"/>
        <button name="resetslider"   type="button" class="btn-default btn" id="resetslider"><span class="glyphicon glyphicon-fast-backward" aria-hidden="true"></span></button>
    </div><span id="time"></span><br/>
</div>
<br/>
<input type="hidden" id="opacity" name="opacity" value=".75" / >
</form>
<div id="images" class="hidden">
<#assign maxTime = 2000 />

<#list fileNames as name>
  <#list 0 .. maxTime as time>
    <img data-src="/browse/img/${name}${time?c}.png" id="${name}${time?c}" src="blank.png" />
  </#list>
</#list>

</div>

    <script src="components/leaflet/dist/leaflet.js"></script>
    <script src="components/leaflet-draw/dist/leaflet.draw.js"></script>
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


var files = [
<#list fileNames as file>
<#assign description = "Fahrenheit GDD" />
<#assign scaleName = "Temperature" />
<#assign color="#880000 "/>
<#assign max="6000 "/>
<#assign min="0 "/>
<#if file?contains("PPT_water")>
	<#assign description="Water-year Precipitation"/>
	<#assign max="2000 "/>
	<#assign color="#006666"/>
	<#assign scaleName = "Precipitation (mm)" />
</#if>
<#if file?contains("PPT_may")>
	<#assign description="May-September Precipitation"/>
	<#assign color="#6699FF"/>
	<#assign scaleName = "Precipitation (mm)" />
	<#assign max="2000 "/>
</#if>
<#if file?contains("PPT_ann")>
	<#assign description="Annual Precipitation"/>
	<#assign scaleName = "Precipitation (mm)" />
	<#assign max="2000 "/>
	<#assign color="#CC6633"/>
</#if>
   {name:'${file}', id:'${file}',description:'${description}', color:'${color}',max:${max},min:${min},scaleName:'${scaleName}'}<#if file_has_next>,</#if></#list>
]; 

var fileIdMap = {
 <#list fileNames as file>
  '${file}': ${file_index}
<#if file_has_next>,</#if></#list>
}


$( document ).ready(function() {
    init();
    _drawRaster();
});


</script>

<div class="modal fade" id="exportModal">
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
        <h4 class="modal-title">Extract a Region</h4>
      </div>
      <div class="modal-body">
        <p>Exporting <span id="exrect"></span></p>
        
        <p id='exstatustext'></p>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
      </div>
    </div><!-- /.modal-content -->
  </div><!-- /.modal-dialog -->
</div><!-- /.modal -->
</div>
</body>
</html>
	
