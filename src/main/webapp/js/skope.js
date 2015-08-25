var map;
// -108.86352539062499, 34.56085936708384) x (-108.86352539062499, 34.56085936708384)
var NORTH, SOUTH, EAST, WEST;
var marker = undefined;
var DEFAULT_START_TIME = 0;
var DEFAULT_END_TIME = 2000;
var $minX = $("#minx");
var $maxX = $("#maxx");
var $temp = $("#ppt.annual");
var $prec = $("#ppt.water_year");

// events
// http://leafletjs.com/reference.html#events

function init() {
    _initMap();
    _initSlider();
}

function _initMap() {
    map = L.map('map').setView([ 34.56085936708384, -108.86352539062499 ], 8);
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

    map.on('zoomend', function() {
        resetGrid();
    });

    map.on('resize', function() {
    });

    map.on('dragend', function() {
    });

    // new L.Control.RemoveAll();
    map.addControl(new L.Control.Command());

}

L.Control.Command = L.Control.extend({
    options : {
        position : 'topleft',
    },

    onAdd : function(map) {
        var controlDiv = L.DomUtil.create('div', 'leaflet-control-command');
        L.DomEvent.addListener(controlDiv, 'click', L.DomEvent.stopPropagation);
        // .addListener(controlDiv, 'click', L.DomEvent.preventDefault);
        // .addListener(controlDiv, 'click', function () { MapShowCommand(); });

        var controlUI = L.DomUtil.create('div', 'leaflet-control-command-interior', controlDiv);
        controlUI.title = 'Map Commands';
        for (var i = 0; i < files.length; i++) {
            var fldC = L.DomUtil.create("div", 'field-container');
            var rad = L.DomUtil.create("input");
            rad.setAttribute("type", "radio");
            rad.setAttribute("name", "vlayer");
            rad.setAttribute("value", files[i].name);
            if (i == 0) {
                rad.setAttribute("checked", "true");
            }
            var span = L.DomUtil.create("span", "rLabel");
            span.appendChild(rad);
            span.appendChild(document.createTextNode(" " + files[i].description));
            fldC.appendChild(span);
            L.DomEvent.addListener(rad, 'change', drawRaster);
            // L.DomEvent.addListener(span,'mouseup',drawRaster);
            controlUI.appendChild(fldC);
        }
        return controlDiv;
    }
});

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
        drawRaster();
    });

    map.on('click', onMapClick);
    $("#play").click(clickAnimate);
    $("#pause").click(pause);
    $("#resetslider").click(reset);

    $minX.change(function() {
        if (chart) {
            chart.zoom([ $minX.val(), $maxX.val() ]);
            chart.flush();
        }

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
        setSliderTime(value);
    });

    $temp.change(function() {
        updateChartData();
    });
    $prec.change(function() {
        updateChartData();
    });

    $maxX.change(function() {
        if (chart) {
            chart.zoom([ $minX.val(), $maxX.val() ]);
            chart.flush();
        }
    });

    $("#reset-time").click(function() {
        $minX.val(DEFAULT_START_TIME);
        $maxX.val(DEFAULT_END_TIME);
        $maxX.trigger("change");
    });

}

function updateChartData() {
    var show = new Array();
    var hide = new Array();
    if ($temp.is(":checked")) {
        show.push("Temperature");
    } else {
        hide.push("Temperature");
    }
    if ($prec.is(":checked")) {
        show.push("Precipitation");
    } else {
        hide.push("Precipitation");
    }
    chart.hide(hide);
    chart.show(show);
    console.log("show: " + show + " hide: " + hide);
    chart.flush();
}

var layer = undefined;

function constructFilename(year) {
    return 'img/' + getActiveSelection() + year + '.png';
}

function getActiveSelection() {
    var sel = $('input[name=vlayer]:checked').val();
    if (sel) {
        return sel;
    }
    return files[0].name;
}

function drawRaster() {
    var imageUrl = constructFilename(getTime());
    console.log(imageUrl);
    var imageBounds = files[fileIdMap[getActiveSelection()]].bounds;
    var layer_ = L.imageOverlay(imageUrl, imageBounds).addTo(map);
    layer_.setOpacity(.8);
    // layer_.fadeTo(.3);
    if (layer != undefined) {
        // layer.fadeTo(10,0);
        map.removeLayer(layer);
    }
    layer = layer_;

    var min = getTime() - 5;
    if (min < 0) {
        min = 0;
    }
    for (var i = min; i <= min + 10; i++) {
        var sel = document.getElementById(getActiveSelection() + i);
        if (sel != undefined) {
            loadImage(sel);
        }
    }
}

function loadImage(el, fn) {
    var img = new Image(), src = el.getAttribute('data-src');
    img.onload = function() {
        if (!!el.parent)
            el.parent.replaceChild(img, el)
        else
            el.src = src;

        fn ? fn() : null;
    }
    img.src = src;
}

function highlightFeature(e) {
    var layer = e.target;

    layer.setStyle({
        weight : 5,
        strokeColor : '#666',
        dashArray : '',
        fillOpacity : 1
    });

    $("#info").html("temp:" + layer.feature.properties.temp);

    if (!L.Browser.ie && !L.Browser.opera) {
        layer.bringToFront();
    }
}

var chart;
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
    var req = "/browse/detail?indexName=" + indexName + "&x1=" + l1.lng + "&y2=" + l2.lat + "&x2=" + l2.lng + "&y1=" + l1.lat + "&zoom=" + map.getZoom() +
            "&cols=" + detail;
    console.log(req);
    pause();
    if (marker != undefined) {
        map.removeLayer(marker);
    }
    marker = L.marker([ l1.lat, l1.lng ]);
    marker.addTo(map);

    var ret = $.Deferred();
    ajax = $.getJSON(req);
    ajax.success(function(data) {
    }).then(function(data) {
        $("#infodetail").removeClass("hidden");

        $("#downloadLink").click(function(e) {
            var x1 = l1.lng;
            var y1 = l1.lat;
            var startTime = $minX.val();
            var endTime = $maxX.val();
            var vals = $(".chartform :checked").map(function(){
                return $(this).val();
            }).get();
            
            var url = "export?x1=" + x1 + "&y1=" + y1 + "&startTime=" + startTime + "&endTime=" + endTime + "&type=" + vals;
            e.preventDefault();  //stop the browser from following
            window.location.href = url;
            return false;
        });

        data['x'] = new Array();
        for (var i = 0; i <= 2000; i++) {
            data['x'].push(i);
        }
        data['x'].splice(0, 0, 'x');
        var graphData = new Array();
        for (var i=0; i < files.length; i++ ) {
            var arr = data[files[i].name];
            if (arr) {
                arr.splice(0, 0, files[i].description);
                graphData[graphData.length] = arr;
            }
        }
//        console.log(arr);
        chart = c3.generate({
            padding : {
                top : 40,
                right : 100,
                bottom : 40,
                left : 100,
            },
            bindto : "#precip",
            data : {
                columns : graphData ,
            },
            axis : {
                y : {
                    label : {
                        text : 'Precipitation / Temperature',
                        position : 'outer-middle',
                    }
                },
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
                            vals[1] = getTick(min, 1);
                            vals[2] = getTick(min, 2);
                            vals[3] = getTick(min, 3);
                            vals[4] = getTick(min, 4);
                            // vals[5] = getTick(min,5);
                            vals[5] = parseInt($maxX.val());
                            // console.log(vals);
                            return vals;
                        }
                    }
                }
            }
        });
        updateChartData();
        if ($minX.val() != DEFAULT_START_TIME || $maxX.val() != DEFAULT_END_TIME) {
            $maxX.trigger("change");
        }
        ;
    });
}

function getTick(val, times) {
    var rdiff = parseInt($maxX.val()) - parseInt($minX.val());
    rdiff = (rdiff / 5);
    var mod = 2;
    // if (rdiff < 50) {
    // mod = 5;
    // }
    // if (rdiff < 20) {
    // mod = 1;
    // }
    // rdiff = rdiff - rdiff % mod;
    var y = (val) / rdiff;
    y = Math.ceil(val + rdiff * times);
    return y;

}
function getTime() {
    return parseInt($("#slider").slider('getValue'));
}

function setSliderTime(time) {
    $("#slider").slider('setValue', parseInt(time), true, true);
    $("#time").text("Year:" + time);
}

function clickAnimate(e) {
    var sld = $("#slider");
    sld.data("status", "play");
    animate();
    // e.event.preventDefault();
}

function animate() {
    var time = getTime();
    var sld = $("#slider");
    if (time < maxTime - 1 && sld.data("status") == 'play') {
        // console.log((sld.data("status") == 'play') + " | " + time + " |" + (maxTime - 1));
        time = parseInt(time) + 1;
        setSliderTime(time);
        drawRaster();
        setTimeout(animate, 500);
    } else {
        sld.data("status", "");
    }
}

function pause(e) {
    // e.event.preventDefault();
    $("#slider").data("status", "");
}

function reset(e) {
    e.preventDefault();
    setSliderTime(0);
    $("#slider").data("status", "");
    drawRaster();
    // return false;
}

var popup = L.popup();

function onMapClick(e) {
    getDetail(e.latlng, e.latlng);
}
