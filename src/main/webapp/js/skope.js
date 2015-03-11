var map = L.map('map').setView([ 37.43997, -100.54687 ], 4);
var time = 0;
var NORTH, SOUTH, EAST, WEST;
var grid = false;
resetGrid();
drawGrid();

drawRaster();

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
    maxZoom : 17,
    attribution : 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, '
            + '<a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, ' + 'Imagery © <a href="http://mapbox.com">Mapbox</a>',
    id : 'examples.map-i875mjb7'
});

tile.addTo(map);

var layer = undefined;

function constructFilename(year) {
    return 'img/out' + year + '.png';
}

function drawRaster() {
    if (grid === true) {
        return;
    }
    var imageUrl = constructFilename(time);
    var imageBounds = [ [ 35.42500000033333, -109.75833333333406 ], [ 33.88333333366667, -107.85833333366594 ] ];
    if (layer != undefined) {
        map.removeLayer(layer);
    }
    layer = L.imageOverlay(imageUrl, imageBounds).addTo(map);
    layer.setOpacity(.3);
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

function drawGrid() {
    if (grid === false) {
        return;
    }
    var bounds = map.getBounds();
    var lat = NORTH;
    var lng = WEST;
    var lat_ = SOUTH;
    var lng_ = EAST;
    var height = Math.abs(Math.abs(lat) - Math.abs(lat_)) / detail;
    var width = Math.abs(Math.abs(lng) - Math.abs(lng_)) / detail;

    var neLat = bounds._northEast.lat;
    var swLng = bounds._southWest.lng;

    var dlat = Math.ceil((neLat - lat) / height) * height;
    var dlong = Math.ceil((swLng - lng) / width) * width;
    if (dlat != 0) {
        lat += dlat;
        // lat_ -= dlat;
    }
    if (dlong != 0) {
        lng -= dlong;
        // lng_ -= dlong;
    }
    /*
     */

    if (ajax != undefined) {
        ajax.abort();
    }

    var req = "/browse/json.action?indexName=" + indexName + "&x1=" + lng + "&y2=" + lat + "&x2=" + lng_ + "&y1=" + lat_ + "&zoom=" + map.getZoom() + "&cols=" +
            detail + "&time=" + time;
    console.log(req);
    var ret = $.Deferred();
    ajax = $.getJSON(req);
    shouldContinue = false;

    ajax.success(function(data) {
        shouldContinue = true;
    }).then(
            function(data) {
                $("#status").html(
                        "timeCode:" + time + " zoom: " + map.getZoom() + " (" + bounds._northEast.lng + ", " + bounds._northEast.lat + ") x (" +
                                bounds._southWest.lng + ", " + bounds._southWest.lat + ")");
                var json = data;
                var layer_ = L.geoJson(json, {
                    onEachFeature : onEachFeature,
                    style : function(feature) {
                        var bezInterpolator = chroma.interpolate.bezier([ 'white', 'red', 'yellow', 'green' ]);

                        var scale = chroma.scale(bezInterpolator).mode('lab');
                        var temp = parseFloat(feature.properties.temp) / parseFloat(max);
                        var tempColor = scale(temp).hex();
                        return {
                            color : tempColor,
                            background : tempColor,
                            fillOpacity : .50,
                            stroke : 0
                        };
                    }
                });
                if (layer != undefined) {
                    map.removeLayer(layer);
                }
                layer = layer_;
                layer.addTo(map);
                ajax = undefined;
                ret.resolve(req);
            });

    return ret;
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
    var ret = $.Deferred();
    ajax = $.getJSON(req);

    ajax.success(function(data) {
    }).then(
            function(data) {
                $("#infostatus").html("<h3>details</h3>");
                var json = data;
                data.unshift("data");
                var chart = c3.generate({
                    data : {
                        columns : [ data ],
                        type : 'bar'
                    },
                    bar : {
                        width : {
                            ratio : 0.5
                        // this makes bar width 50% of length between ticks
                        }
                    },
                    subchart : {
                        show : true
                    }
                });

                $("#infodetail").html(
                        "<p>" + "timeCode:" + time + " zoom: " + map.getZoom() + " (" + l1.lng + ", " + l1.lat + ") x (" + l2.lng + ", " + l2.lat + ")</p>");
                ret.resolve(req);
            });

}

function animate() {
    console.log("animate:" + time);
    if (time < maxTime - 1 && shouldContinue === true) {
        if (drawGrid === true) {
            setTime(time + 1);
            var res = drawGrid();
            $.when(res).done(function() {
                if (shouldContinue === true) {
                    setTimeout(animate, 2);
                }
            });
        } else {
            setTime(time + 1);
            drawRaster();
            setTimeout(animate, 500);
        }
    }
}

function pause() {
    shouldContinue = false;
}

function setTime(year) {
    time = year;
    $("#time").html("year:" + year);
}
function reset() {
    setTime(0);
    shouldContinue = true;
    drawGrid();
}

var popup = L.popup();

function onMapClick(e) {
    if (drawGrid === true) {
        popup.setLatLng(e.latlng).setContent("You clicked the map at " + e.latlng.toString()).openOn(map);
    } else {
        getDetail(e.latlng, e.latlng);
    }
}

map.on('click', onMapClick);
$("#play").click(animate);
$("#pause").click(pause);
$("#reset").click(reset);
