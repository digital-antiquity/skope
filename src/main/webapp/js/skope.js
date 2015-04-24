var map = L.map('map').setView([ 34.56085936708384, -108.86352539062499], 8);
//-108.86352539062499, 34.56085936708384) x (-108.86352539062499, 34.56085936708384)
var NORTH, SOUTH, EAST, WEST;
var grid = false;
var marker = undefined;

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
            detail + "&time=" + getTime();
    console.log(req);
    var ret = $.Deferred();
    ajax = $.getJSON(req);
    shouldContinue = false;

    ajax.success(function(data) {
        shouldContinue = true;
    }).then(
            function(data) {
                $("#status").html(
                        "timeCode:" + getTime() + " zoom: " + map.getZoom() + " (" + bounds._northEast.lng + ", " + bounds._northEast.lat + ") x (" +
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

                if (marker != undefined) {
                    map.removeLayer(marker);
                }
                marker = L.marker([l1.lat, l1.lng]);
                marker.addTo(map);
                ret.resolve(req);
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
        if (drawGrid === true) {
            var res = drawGrid();
            $.when(res).done(function() {
                if (shouldContinue === true) {
                    setTimeout(animate, 2);
                }
            });
        } else {
            drawRaster();
            setTimeout(animate, 500);
        }
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
    drawGrid();
    drawRaster();
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
$("#play").click(clickAnimate);
$("#pause").click(pause);
$("#reset").click(reset);
