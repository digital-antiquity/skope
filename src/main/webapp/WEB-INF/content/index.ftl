<!DOCTYPE html PUBLIC 
	"-//W3C//DTD XHTML 1.1 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
	
	<!DOCTYPE html>
<html>
<head>
    <title>Leaflet Quick Start Guide Example</title>
    <meta charset="utf-8" />

    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <link rel="stylesheet" href="components/leaflet/dist/leaflet.css" />
</head>
<body>
<div id="status" style="font-size:10pt"></div>
    <div id="map" style="width: 600px; height: 600px"></div>
    <button name="play" id="play">play</button>
    <button name="reset" id="reset">reset</button>

    <script src="components/leaflet/dist/leaflet.js"></script>
    <script src="components/jquery/dist/jquery.js"></script>
    <script src="components/chroma-js/chroma.min.js"></script>
    <script>

        var map = L.map('map').setView([37.43997, -100.54687], 4);
   var time = 0;
    var NORTH,SOUTH,EAST,WEST;
    var indexName = "skope";
    var max = 1500;
    var detail = 50;
    var maxTime = 60;
    if (indexName != "skope") {
        max = 120;
        detail = 20;
    }
    resetGrid();
 drawGrid();
// events
// http://leafletjs.com/reference.html#events
map.on('zoomend', function() {
    resetGrid();
    drawGrid();
});

map.on('resize', function() {
    drawGrid();
});

map.on('dragend', function() {
    drawGrid();
});

        var tile = L.tileLayer('https://{s}.tiles.mapbox.com/v3/{id}/{z}/{x}/{y}.png', {
            maxZoom: 17,
            attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, ' +
                '<a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, ' +
                'Imagery © <a href="http://mapbox.com">Mapbox</a>',
            id: 'examples.map-i875mjb7'
        });
        
        tile.addTo(map);

var layer = undefined;


function resetGrid() {
    NORTH = map.getBounds()._northEast.lat;
    WEST = map.getBounds()._southWest.lng;
    SOUTH = map.getBounds()._southWest.lat;
    EAST = map.getBounds()._northEast.lng;
  //L.marker([NORTH, WEST]).addTo(map);
  //L.marker([SOUTH, EAST]).addTo(map);
}

function drawGrid() {
  var bounds = map.getBounds();
  var lat = NORTH;
  var lng = WEST;
  var lat_ = SOUTH;
  var lng_ = EAST;
  // FIXME: for Panning support in the future
  if (bounds._northEast.lat != lat) {
  }
  if (bounds._southWest.lng != lng) {
  }
  
  console.log(lat_ + " / " + lat + " / " + bounds._southWest.lat + " / " + bounds._northEast.lat);
  var req = "/browse/json.action?indexName="+indexName+"&x1=" +lng + "&y2=" + lat + "&x2=" + lng_ + "&y1=" + lat_ + "&zoom=" + map.getZoom() + "&cols="+detail + "&time=" + time; 
  console.log(req);
  var ret = $.Deferred();
  
  
$.getJSON(req).success(function(data) {
}).then(function(data) {
    $("#status").html("timeCode:" + time + " zoom: " + map.getZoom() + " (" + bounds._northEast.lng + ", " + bounds._northEast.lat + ") x ("+ bounds._southWest.lng + ", " + bounds._southWest.lat + ")");
        var json = data;        var layer_ =  L.geoJson(json, { 
            style: function(feature) {
            var scale = chroma.scale(['blue', 'red']).mode('lab');
            var temp = parseFloat(feature.properties.temp) / parseFloat(max);
            //console.log(temp + " " + scale(temp).hex());
            var tempColor = scale(temp).hex();
            return {color: tempColor , background: tempColor, fillOpacity: .50 ,stroke:0 };          } 
        });
    if (layer != undefined) {
        map.removeLayer(layer);
    }
    layer = layer_;
    layer.addTo(map);
    ret.resolve(req);
});

    return ret;
}


function animate() {
    if (time < maxTime - 1) {
        time++;
        var res = drawGrid();
        $.when(res).done(function(){
            setTimeout(animate, 10);
        });
        
    }
}

function reset() {
time = 0;
}

/*        L.marker([51.5, -0.09]).addTo(map)
            .bindPopup("<b>Hello world!</b><br />I am a popup.").openPopup();

        L.circle([51.508, -0.11], 500, {
            color: 'red',
            fillColor: '#f03',
            fillOpacity: 0.5
        }).addTo(map).bindPopup("I am a circle.");

        L.polygon([
            [51.509, -0.08],
            [51.503, -0.06],
            [51.51, -0.047]
        ]).addTo(map).bindPopup("I am a polygon.");
*/

        var popup = L.popup();

        function onMapClick(e) {
            popup
                .setLatLng(e.latlng)
                .setContent("You clicked the map at " + e.latlng.toString())
                .openOn(map);
        }

        map.on('click', onMapClick);
    $("#play").click(animate);
    $("#reset").click(reset);
        
    </script>
    
Data is Copyright &copy; 2015, PRISM Climate Group, Oregon State University, http://prism.oregonstate.edu .
</body>
</html>
	