<!DOCTYPE html PUBLIC 
	"-//W3C//DTD XHTML 1.1 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
	
	<!DOCTYPE html>
<html>
<head>
    <title>Leaflet Quick Start Guide Example</title>
    <meta charset="utf-8" />

    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="components/bootstrap/dist/css/bootstrap.min.css" rel="stylesheet" media="screen">
    <link rel="stylesheet" href="components/c3/c3.css" />
    <link rel="stylesheet" href="components/leaflet/dist/leaflet.css" />
</head>
<body>
<div class="container-fluid">
<div class="row">
<div id="status" style="font-size:10pt" class="col-md-12"></div>
</div>
<div class="row">
    <div id="mapbox"  class="col-md-6">
        <div id="map" style="width: 600px; height: 600px"></div>
    </div>
    <div id="infobox" class="col-md-6">
        <div id="infostatus"></div>
        <div id="chart"></div>
        <div id="infodetail"></div>
    
    </div>
</div>
<div class="row">
    <div class="col-md-12">
    <div class="btn-group" role="group" aria-label="...">
        <button name="reset"  class="btn-default btn" id="reset"><span class="glyphicon glyphicon-fast-backward" aria-hidden="true"></span></button>
        <button name="play" class="btn-default btn" id="play"><span class="glyphicon glyphicon-play" aria-hidden="true"></span></button>
        <button name="pause"  class="btn-default btn" id="pause"><span class="glyphicon glyphicon-pause" aria-hidden="true"></span></button>
    </div><span id="time"></span>
    </div>
</div>

<script>
// GLOBALS:
var indexName = "skope";
var max = 800;
var detail = 160;
var maxTime = 2000;
var shouldContinue = true;
var ajax;
if (indexName != "skope") {
    max = 120;
    detail = 20;
}




</script>
    <script src="components/jquery/dist/jquery.js"></script>
    <script src="components/bootstrap/dist/js/bootstrap.min.js"></script>
    <script src="components/leaflet/dist/leaflet.js"></script>
    <script src="components/d3/d3.js"></script>
    <script src="components/c3/c3.js"></script>
    <script src="components/chroma-js/chroma.min.js"></script>
    <script src="js/skope.js"></script>
Data is Copyright &copy; 2015, PRISM Climate Group, Oregon State University, http://prism.oregonstate.edu .
</div>
</body>
</html>
	