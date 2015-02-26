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
    <div id="map" style="width: 600px; height: 400px"></div>

    <script src="components/leaflet/dist/leaflet.js"></script>
    <script src="components/jquery/dist/jquery.js"></script>
    <script src="components/chroma-js/chroma.min.js"></script>
    <script>

        var map = L.map('map').setView([37.43997, -100.54687], 4);


 drawGrid();
// events
// http://leafletjs.com/reference.html#events
map.on('zoomend', function() {
    drawGrid();
});

map.on('resize', function() {
    drawGrid();
});

map.on('dragend', function() {
    drawGrid();
});


        L.tileLayer('https://{s}.tiles.mapbox.com/v3/{id}/{z}/{x}/{y}.png', {
            maxZoom: 18,
            attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, ' +
                '<a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, ' +
                'Imagery © <a href="http://mapbox.com">Mapbox</a>',
            id: 'examples.map-i875mjb7'
        }).addTo(map);


var layer = undefined;

function drawGrid() {
  var bounds = map.getBounds();
  var req = "/browse/json.action?x1=" + bounds._northEast.lng + "&y2=" + bounds._northEast.lat + "&x2=" + bounds._southWest.lng + "&y1=" + bounds._southWest.lat + "&zoom=" + map.getZoom() + "&cols=25"; 
  console.log(req);
$.getJSON(req).success(function(data) {
}).then(function(data) {

  console.log("done");
        var json = data;        var layer_ =  L.geoJson(json, { 
            style: function(feature) {
            var scale = chroma.scale(['white', 'red']).mode('lab');
            var temp = parseFloat(feature.properties.temp) / parseFloat(100);
            //console.log(temp + " " + scale(temp).hex());
            var tempColor = scale(temp).hex();
            return {color: tempColor , background: tempColor, fillOpacity: .50, border:0.0 };          } 
        });
    if (layer != undefined) {
        map.removeLayer(layer);
    }
    layer = layer_;
    layer.addTo(map);

});

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
    </script>
Data is Copyright &copy; 2015, PRISM Climate Group, Oregon State University, http://prism.oregonstate.edu .
</body>
</html>
	