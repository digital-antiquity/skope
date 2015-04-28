var map = L.map('map').setView([ 34.56085936708384, -108.86352539062499], 8);
//-108.86352539062499, 34.56085936708384) x (-108.86352539062499, 34.56085936708384)
var NORTH, SOUTH, EAST, WEST;
var marker = undefined;

// events
// http://leafletjs.com/reference.html#events
map.on('zoomend', function() {
    resetGrid();
});

map.on('resize', function() {
});

map.on('dragend', function() {
});

var tile = L.tileLayer('https://{s}.tiles.mapbox.com/v3/{id}/{z}/{x}/{y}.png', {
    maxZoom : 17,
    attribution : 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, '
            + '<a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, ' + 'Imagery © <a href="http://mapbox.com">Mapbox</a>',
    id : 'examples.map-i875mjb7'
});

tile.addTo(map);

var layer = undefined;

function constructFilename(year) {
    var type = "precip";
    var type_ = $("#map").data("type");
    if (type_ == "temp") {
        type = "temp";
    }
    return 'img/' + type + year + '.png';
}

function drawRaster() {
    var imageUrl = constructFilename(getTime());
    var imageBounds = [ [ 35.42500000033333, -109.75833333333406 ], [ 33.88333333366667, -107.85833333366594 ] ];
    var layer_ = L.imageOverlay(imageUrl, imageBounds).addTo(map);
    layer_.setOpacity(.3);
//    layer_.fadeTo(.3);
    if (layer != undefined) {
//        layer.fadeTo(10,0);
        map.removeLayer(layer);
    }
    layer = layer_;
}

function highlightFeature(e) {
    var layer = e.target;

    layer.setStyle({
        weight : 5,
        strokeColor : '#666',
        dashArray : '',
        fillOpacity : 1
    });

    console.log(layer.feature.properties);
    $("#info").html("temp:" + layer.feature.properties.temp);

    if (!L.Browser.ie && !L.Browser.opera) {
        layer.bringToFront();
    }
}

function resetHighlight(e) {
    layer.resetStyle(e.target);
}

function onEachFeature(feature, layer) {
    layer.on({
        mouseover : highlightFeature,
        mouseout : resetHighlight,
        click : clickFeature
    });
}

function resetGrid() {
    NORTH = map.getBounds()._northEast.lat;
    WEST = map.getBounds()._southWest.lng;
    SOUTH = map.getBounds()._southWest.lat;
    EAST = map.getBounds()._northEast.lng;
    // L.marker([NORTH, WEST]).addTo(map);
    // L.marker([SOUTH, EAST]).addTo(map);
}

function clickFeature(e) {
    var layer = e.target;
    var l1 = layer._latlngs[0];
    var l2 = layer._latlngs[2];
    getDetail(l1, l2);
}

function getDetail(l1, l2) {
    var req = "/browse/detail.action?indexName=" + indexName + "&x1=" + l1.lng + "&y2=" + l2.lat + "&x2=" + l2.lng + "&y1=" + l1.lat + "&zoom=" +
            map.getZoom() + "&cols=" + detail;
    console.log(req);
    pause();
    if (marker != undefined) {
        map.removeLayer(marker);
    }
    marker = L.marker([l1.lat, l1.lng]);
    marker.addTo(map);

    var ret = $.Deferred();
    ajax = $.getJSON(req);
    ajax.success(function(data) {
    }).then(
            function(data) {
                data['P'].splice(0,0,"Precipitation");
                data['T'].splice(0,0,"Temperature");
                var chart = c3.generate({
                    bindto: "#precip",
                    data : {
                        columns : [ data['P'],
                                    data['T']],
                    },
                });
            });
}

function getTime() {
    return parseInt($("#slider").slider('getValue'));
}

function setSliderTime(time) {
    $("#slider").slider('setValue', parseInt(time));
    $("#time").text(time);
}

function clickAnimate() {
    var sld = $("#slider");
    sld.data("status","play");
    animate();
}

function animate() {
    var time = getTime();
    var sld = $("#slider");
    if (time < maxTime - 1 && sld.data("status") == 'play') {
        //console.log((sld.data("status") == 'play') + " | " + time + " |" + (maxTime - 1));
        time = parseInt(time) + 1;
        setSliderTime(time);
        drawRaster();
        setTimeout(animate, 500);
    } else {
        sld.data("status","");
    }
}

function pause() {
    $("#slider").data("status","");
}

function reset() {
    setSliderTime(0);
    $("#slider").data("status","");
    drawRaster();
}

var popup = L.popup();

function onMapClick(e) {
    getDetail(e.latlng, e.latlng);
}

map.on('click', onMapClick);
$("#play").click(clickAnimate);
$("#pause").click(pause);
$("#reset").click(reset);
