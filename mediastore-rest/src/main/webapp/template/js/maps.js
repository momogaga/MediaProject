var marker = new google.maps.Marker();
function init(latitude, longitude, info, bool) {
    var centre = new google.maps.LatLng(43.69, 7.26); // Correspond au coordonnées central

// Options de la maps
    var map = new google.maps.Map(document.getElementById('map'), {
        zoom: 12,
        zoomControl: true,
        zoomControlOptions: {
            style: google.maps.ZoomControlStyle.DEFAULT
        },
        center: centre,
        mapTypeControl: true,
        mapTypeControlOptions: {
            style: google.maps.MapTypeControlStyle.DROPDOWN_MENU
        },
        navigationControl: true,
        navigationControlOptions: {
            style: google.maps.NavigationControlStyle.MEDIUM,
            position: google.maps.ControlPosition.TOP_RIGHT
        },
        scaleControl: true,
        streetViewControl: true,
        mapTypeId: google.maps.MapTypeId.ROADMAP
    });
    var infowindow = new google.maps.InfoWindow({
    });
    var i;
    for (i = 0; i < latitude.length; i++) {
        // Placer un point sur la carte
        marker = new google.maps.Marker({
            position: new google.maps.LatLng(latitude[i], longitude[i]),
            map: map,
            title: '',
            icon: ''
        });
        if (bool == 1) { // Créer une info-bulle
            google.maps.event.addListener(marker, 'click', (function(marker, i) {
                return function() {
                    infowindow.setContent(info[i]);
                    infowindow.open(map, marker);

                };
            })(marker, i));
        }
    }
}


function changeMarkerPosition(lat, lon) {
    var latlng = new google.maps.LatLng(lat, lon);
    marker.setPosition(latlng);
}

$(document).ready(function() {
    var latitude = new Array();
    latitude[0] = "43.69";
    var longitude = new Array();
    longitude[0] = "7.27";
    var info = new Array();
    info[0] = "Tof";

    // map = init(latitude, longitude, info, 1);


    $('#mapGlobal').click(function() {
        console.log("click")
        map = init(latitude, longitude, info, 1);
        //google.maps.event.trigger(map, "resize");
    });

    $("#accordion").bind('accordionchange', function(event, ui) {
        if (ui.newContent.attr('id') == 'mapGlobal')
        {
            google.maps.event.trigger(map, 'resize');
        }
    });
});