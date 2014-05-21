var map;
var marker;

function initialize() {
    var mapOptions = {
        zoom: 8,
        center: new google.maps.LatLng(-25.363882, 131.044922)
    };
    map = new google.maps.Map(document.getElementById('map-canvas'), mapOptions);

    marker = new google.maps.Marker({
        position: new google.maps.LatLng(0, 0),
        map: map,
        animation: google.maps.Animation.DROP,
        title: 'coucou',
        icon: ''
    });
}

google.maps.event.addDomListener(window, 'load', initialize);

function resizeMap() {
    if (typeof map === "undefined")
        return;
    var center = map.getCenter();
    google.maps.event.trigger(map, "resize");

}

function changeMarkerPosition(lat, lon) {

    var latlng = new google.maps.LatLng(lat, lon);
    marker.setPosition(latlng);

    resizeMap();
    map.panTo(marker.getPosition());

}


