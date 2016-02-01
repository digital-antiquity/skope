var map;
// -108.86352539062499, 34.56085936708384) x (-108.86352539062499, 34.56085936708384)
var NORTH, SOUTH, EAST, WEST;
var marker = undefined;
var DEFAULT_START_TIME = 0;
var DEFAULT_END_TIME = 2000;
var $minX = $("#minx");
var $maxX = $("#maxx");
// events
// http://leafletjs.com/reference.html#events

/**
 * Initialize the javascript
 */
function init() {
    _initMap();
    _initSliderAtStartup();
    _initSlider();
}

/**
 * uses xColor/chroma.js to create a nice color gradient similar to gDAL
 */
function _buildColorScale() {
    var hot = chroma.scale([ '#2E9A58', '#FBFF80', '#E06C1F', '#C83737', 'D7F4F4' ], // colors
    [ 0, .25, .50, .75, 1 ] // positions
    )
    return hot;
}

/**
 * Initialize the base-map with leaflet. Option of two maps based on what's needed.
 */

function _initMap() {
    L.drawLocal.draw.toolbar.buttons.rectangle = 'Select Region to Export';

    map = L.map('map').setView([ 34.56085936708384, -108.86352539062499 ], 5);
    var tile = L.tileLayer('http://server.arcgisonline.com/ArcGIS/rest/services/NatGeo_World_Map/MapServer/tile/{z}/{y}/{x}', {
        attribution : 'Tiles &copy; Esri &mdash; National Geographic, Esri, DeLorme, NAVTEQ, UNEP-WCMC, USGS, NASA, ESA, METI, NRCAN, GEBCO, NOAA, iPC',
        maxZoom : 16
    });

    var Esri_WorldTopoMap = L
            .tileLayer(
                    'http://server.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/{z}/{y}/{x}',
                    {
                        attribution : 'Tiles &copy; Esri &mdash; Esri, DeLorme, NAVTEQ, TomTom, Intermap, iPC, USGS, FAO, NPS, NRCAN, GeoBase, Kadaster NL, Ordnance Survey, Esri Japan, METI, Esri China (Hong Kong), and the GIS User Community',
                        maxZoom : 16
                    });
    Esri_WorldTopoMap.addTo(map);

    // register event binds
    map.on('zoomend', function() {
        _drawRaster();
    });

    map.on('resize', function() {
        _drawRaster();
    });

    map.on('dragend', function() {
        _drawRaster();
    });

    var legend = L.control({
        position : 'bottomright'
    });

    // create the legend and bind it, legend will change based on the type of data
    legend.onAdd = function(map) {
        // for ranges between 0 & 25, add labels

        var div = L.DomUtil.create('div', 'info legend'), grades = [ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ], labels = [];

        // loop through our density intervals and generate a label with a colored square for each interval
        div.innerHTML += "<span id='lmin' style='display:inline-block;margin-top:-10px'>" + 0 + "&nbsp;</span>";
        for (var i = 0; i <= 10; i++) {
            var hot = _buildColorScale();
            var color = hot(i / 10).hex();
            div.innerHTML += '<i style="display:inline-block;margin-top:4px;width:10px;height:10px;background:' + color + '">&nbsp;</i> ';
        }
        div.innerHTML += "<span id='lmax'>" + 6000 + "</span>";

        return div;
    };

    legend.addTo(map);

    // new L.Control.RemoveAll();
    map.addControl(new L.Control.Command());
    map.on('click', _onMapClick);

}

// create the control for the data set sources, iterate over files array
L.Control.Command = L.Control.extend({
    options : {
        position : 'topright',
    },

    onAdd : function(map) {
        var controlDiv = L.DomUtil.create('div', 'leaflet-control-command');
        L.DomEvent.addListener(controlDiv, 'click', L.DomEvent.stopPropagation);

        var controlUI = L.DomUtil.create('div', 'leaflet-control-command-interior', controlDiv);
        controlUI.title = 'Map Commands';
        var first = 0;
        for (var i = files.length - 1; i >= 0; i--) {
			if (files[i].name.indexOf("PPT_may") > -1 || files[i].name.indexOf("PPT_annual") > -1) {
				continue;
			}
            var fldC = L.DomUtil.create("div", 'field-container');
            var rad = L.DomUtil.create("input");
            rad.setAttribute("type", "radio");
            rad.setAttribute("name", "vlayer");
            rad.setAttribute("value", files[i].name);
            rad.setAttribute("id", "r" + files[i].name);
            if (first == 0) {
                rad.setAttribute("checked", "true");
                first = 1;
            }
            var span = L.DomUtil.create("label", "rLabel");
            span.appendChild(rad);
            span.setAttribute("for", "r" + files[i].name);
            span.appendChild(document.createTextNode(" " + files[i].description));
            fldC.appendChild(span);
            L.DomEvent.addListener(rad, 'change', _drawRaster);
            // L.DomEvent.addListener(span,'mouseup',_drawRaster);
            controlUI.appendChild(fldC);
			
        }
        return controlDiv;
    }
});

function _initSliderAtStartup() {
    var $slider = $('#slider');
    // bind events
    $("#play").click(_clickAnimate);
    $("#pause").click(_pause);
    $("#resetslider").click(_reset);

    $minX.change(function() {
        _handleChartScaleChange();

        var value = $slider.slider("getValue");
        if (value > $maxX.val()) {
            value = parseInt($maxX.val());
        }
        if (value < $minX.val()) {
            value = parseInt($minX.val());
        }
        $slider.slider("destroy");
        _initSlider({
            max : parseInt($maxX.val()),
            min : parseInt($minX.val()),
            value : value
        });
        _setSliderTime(value);
    });

    $maxX.change(function() {
        _handleChartScaleChange();
    });

    $("#reset-time").click(function() {
        $minX.val(DEFAULT_START_TIME);
        $maxX.val(DEFAULT_END_TIME);
        $maxX.trigger("change");
    });
    _drawRectangle();


}

// initialize the slider that allows a user to move to a given date
function _initSlider(data) {
    var $slider = $('#slider');

    if (data == undefined) {
        data = {};
    }
    data.formatter = function(value) {
        return 'Current value: ' + value;
    }
    $("#slider").slider(data).on("slide", function(slideEvt) {
        $("#time").text(slideEvt.value);
        _drawRaster();
    });

}

function _handleChartScaleChange() {
    charts.forEach(function(chart){
        if (chart) {
        chart.zoom([ $minX.val(), $maxX.val() ]);
        chart.flush();
        }
    });
}

// initialize and handle Leaflet.draw
function _drawRectangle() {
	return;
    var drawnItems = new L.FeatureGroup();
    map.addLayer(drawnItems);
    var drawControl = new L.Control.Draw({
        draw : {
            position : 'topleft',
            polygon : false,
            polyline : false,
            circle : false,
            marker : false,

            rectangle : {
                shapeOptions : {
                    color : '#bada55'
                },
                showArea : true
            },
        },
        edit : {
            featureGroup : drawnItems,
            edit : false,
            remove : false
        },
    });
    map.addControl(drawControl);
    // handle rectangle create event
    map.on('draw:created', function(e) {
        var type = e.layerType;
        layer = e.layer;
        if (type === 'rectangle') {
            $("#exportModal").modal();
            bounds = layer.getBounds();
            $("#exstatustext").html("<i class='glyphicon glyphicon-refresh spinning'></i> Processing &hellip;");
            coordinates = bounds.toBBoxString();
            console.log(coordinates);
            $("#exrect").html(coordinates);
            var data = {
                bounds : coordinates,
                type : _getActiveSelection(),
                startTime : $minX.val(),
                endTime : $maxX.val()
            };
            ajax = $.post("/browse/extract", data, function(data) {
                $("#exstatustext").html("export complete. <a href='/browse/download?filename=" + data.filename + "'>Download</a>");
            }, "json");
        }
    });

}

var layer = undefined;

// get the active layer
function _getActiveSelection() {
    var sel = $('input[name=vlayer]:checked').val();
    if (sel) {
        return sel;
    }
    return files[0].name;
}

var currentTileLayer = [];

// strip out old tiles in animation
function _removeOldTiles() {
    // leave only the top tile
    var keep = new Array();
    map.eachLayer(function(l) { 
        keep.push(l._leaflet_id);
    });
    
    // prune entries not on DOM
    for (var i=currentTileLayer.length -1; i >= 0; i-- ) {
        var l = currentTileLayer[i];
        if (!$.inArray(l._leaflet_id, keep)) {
            currentTileLayer.splice(i,1);
            console.log("pruning:" + l);
        }
    }
    
    while (currentTileLayer.length > 1) {
        currentTileLayer[0].setOpacity(.1);
        currentTileLayer[0].setZIndex(900);

        map.removeLayer(currentTileLayer[0]);
        currentTileLayer.shift();
        console.log(currentTileLayer);
    }
}

// this is what loads the raster
function _drawRaster() {
    var type = _getActiveSelection();
    var $lmax = $("#lmax");
    var $lmin = $("#lmin");
    for (var i=0;i<files.length;i++) {
        if (type == files[i].name) {
            $lmax.html(files[i].max);
            $lmin.html(files[i].min);
        }
    }
    
    // build the tile URL path + filename + year + color + leaflet params
    var currentTileLayer_ = L.tileLayer('/browse/img/{tile}/tiles/{type}-{time}-color/{z}/{x}/{y}.png', {
        tms : true,
        tile : _getActiveSelection(),
        time : 1 + _getTime(),
        type : type,
        opacity : $("#opacity").val()
    });
    currentTileLayer_.setZIndex(1000);
    currentTileLayer_.addTo(map);
    currentTileLayer_.on("load", function() {
        setTimeout(_removeOldTiles, 300);
        currentTileLayer.push(currentTileLayer_);
    });
}


var charts = [];


// handle the click ona  point, render the graphs and setup the download link
function _getDetail(l1, l2) {
    var req = "/browse/detail?indexName=" + indexName + "&x1=" + l1.lng + "&y2=" + l2.lat + "&x2=" + l2.lng + "&y1=" + l1.lat + "&zoom=" + map.getZoom() +
            "&cols=" + detail;
    console.log(req);
    _pause();
    // remove old marker
    if (marker != undefined) {
        map.removeLayer(marker);
    }
    marker = L.marker([ l1.lat, l1.lng ]);
    marker.addTo(map);

    // print the coordinates
    $("#coordinates").html("Lat: " + l1.lat + " , Lon:" + l1.lng);

    var ret = $.Deferred();
    ajax = $.getJSON(req);
    // get the data
    ajax.success(function(data) {
    }).then(function(data) {
        $("#infodetail").removeClass("hidden");
        // make the download link
        $("#downloadLink").click(function(e) {
            var x1 = l1.lng;
            var y1 = l1.lat;
            var startTime = $minX.val();
            var endTime = $maxX.val();
            var url = "export?x1=" + x1 + "&y1=" + y1 + "&startTime=" + startTime + "&endTime=" + endTime;
            e.preventDefault(); // stop the browser from following
            window.location.href = url;
            return false;
        });
        charts = [];
        var graphData = new Array();
        var axes = {};
        $("#precip").html("");
        // build separate graphs for each data source
        var row;
        for (var i = 0; i < files.length; i++) {
            var data_ = {};
            data_[0] = new Array();
            for (var j = 0; j <= 2000; j++) {
                data_[0].push(j);
            }

            data_[0].splice(0, 0, 'x');
            if (files[i] == undefined) {
                continue;
            }
            var arr = data[files[i].name + ".tif"]; // files[i].name
            if (i % 2 == 0) {
                row = $("<div class='row'></div>");
                $("#precip").append(row);
            }
            row.append("<div style='height:250px' class='col-md-6' id=\"g" + files[i].name + "\"></div>");
            var descr = files[i].description;
            data_[1] = arr;
            console.log(files[i].name, data_);
            if (arr) {
                arr.splice(0, 0, descr);
                var axis = {
                    label : {
                        text : files[i].scaleName,
                        position : 'outer-middle',
                    },
                    show : true
                };
                var color = files[i].color;
                _buildChart(files[i].name, [ data_[0], data_[1] ], axis, color);
            }
        }
        if ($minX.val() != DEFAULT_START_TIME || $maxX.val() != DEFAULT_END_TIME) {
            $maxX.trigger("change");
        }
        ;
    });
}

// generate the c3chart
function _buildChart(file, data, yAxis, color) {
    var bound = "#g" + file;
    console.log(file, data, yAxis, color);
    var chart = c3.generate({
        padding : {
            top : 10,
            right : 20,
            bottom : 10,
            left : 80,
        },
        bindto : bound,
        data : {
            x : 'x',
            columns : data,
        },
        point: {
            show: false
        },
        color : {
            pattern : [ color ]
        },
        tooltip: {
            format: {
                title: function (d) { return 'Year ' + d + " C.E."; },
                value: function (value, ratio, id) {
                    var suffix = "mm";
                    if (!id.toLowerCase().indexOf('precip') > -1) {
                        suffix = "days";
                    }
                    return value + " " + suffix;
                }
            }
        },
        axis : {
            y : yAxis,
            x : {
                label : {
                    text : 'Time',
                    position : 'outer-center',
                },
                tick : {
                    values : function(x) {
                        var min = parseInt($minX.val());
                        var vals = [];
                        vals[0] = min;
                        vals[1] = _getTick(min, 1);
                        vals[2] = _getTick(min, 2);
                        vals[3] = _getTick(min, 3);
                        vals[4] = _getTick(min, 4);
                        // vals[5] = getTick(min,5);
                        vals[5] = parseInt($maxX.val());
                        // console.log(vals);
                        return vals;
                    }
                }
            }
        }
    });
    charts.push(chart);

}

function _getTick(val, times) {
    var rdiff = parseInt($maxX.val()) - parseInt($minX.val());
    rdiff = (rdiff / 5);
    var mod = 2;
    var y = (val) / rdiff;
    y = Math.ceil(val + rdiff * times);
    return y;

}
function _getTime() {
    return parseInt($("#slider").slider('getValue'));
}

function _setSliderTime(time) {
    $("#slider").slider('setValue', parseInt(time), true, true);
    $("#time").text("Year:" + time);
}

function _clickAnimate(e) {
    var $sld = $("#slider");
    var $btn = $("#play");
    if ($sld.data("status") == 'play') {
        $btn.html("<span class='glyphicon glyphicon-play' aria-hidden='true'></span>");
        $sld.data("status", "");
        $("#opacity").val(.5);
    } else {
        $sld.data("status", "play");
        $btn.html("<span class='glyphicon glyphicon-pause' aria-hidden='true'></span>");
        $("#opacity").val(1);
        _animate();
    }
    // e.event.preventDefault();
}

function _animate() {
    var time = _getTime();
    var sld = $("#slider");
    if (time <=	  $maxX.val() && sld.data("status") == 'play') {
        // console.log((sld.data("status") == 'play') + " | " + time + " |" + (maxTime - 1));
        time = parseInt(time) + 1;
        _setSliderTime(time);
        _drawRaster();
        setTimeout(_animate, 500);
    } else {
        sld.data("status", "");
        var $btn = $("#play");
        $btn.html("<span class='glyphicon glyphicon-play' aria-hidden='true'></span>");
    }
}

function _pause(e) {
    // e.event.preventDefault();
    $("#slider").data("status", "");
}

function _reset(e) {
    e.preventDefault();
    _setSliderTime($minX.val());
    $("#slider").data("status", "");
    var $btn = $("#play");
    $btn.html("<span class='glyphicon glyphicon-play' aria-hidden='true'></span>");
    _drawRaster();
    // return false
}

function _onMapClick(e) {
    _getDetail(e.latlng, e.latlng);
}

