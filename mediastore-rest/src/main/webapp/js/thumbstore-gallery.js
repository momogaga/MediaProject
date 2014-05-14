function getGallery() {

    var folders = getSelectedFolders();
    $.getJSON('rest/hello/getAll', {
        filter: $("input[name=filter_path]").val(),
        folder: JSON.stringify(folders),
        gps: false
                //$.param(folders)
    }, function(data) {
        buildGallery(data);
        //   debugger;
    });
}

function buildGallery(array) {

    $('#gallery').children().remove();
    for (f in array) {
        var tag = array[f];

        $('#gallery').append("<a href='rest/hello/getThumbnail?path=" + tag.path + "&w=600&h=600' data-gallery=''><img src='rest/hello/getThumbnail?path=" + tag.path + "&w=70&h=70'></a>");

    }
}

