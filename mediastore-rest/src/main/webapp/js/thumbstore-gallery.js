var begin = 0;
function getGallery() {

    var folders = getSelectedFolders();
    $.getJSON('rest/hello/getAll', {
        filter: $("input[name=filter_path]").val(),
        folder: JSON.stringify(folders),
        gps: false,
        nb: 2
                //$.param(folders)
    }, function(data) {
        buildGallery(data);
        begin = 2;
    });
}

function buildGallery(array) {
    $('#gallery').children().remove();
    for (f in array) {
        var tag = array[f];
        var ext = tag.path.substring(tag.path.lastIndexOf("."));
        ext = ext.toLowerCase();

        if (ext === '.jpeg' || ext === '.jpg' || ext === '.bmp' || ext === '.gif' || ext === '.png' || ext === '.tiff') {
            $('#gallery').append("<a href='rest/hello/getThumbnail?path=" + tag.path + "&w=0&h=0' title='" + tag.path + "' data-gallery=''><img src='rest/hello/getThumbnail?path=" + tag.path + "&w=70&h=70' alt='" + tag.path + "'></a>");
        }
    }
}

function appendPictures() {
    var folders = getSelectedFolders();
    $.getJSON('rest/hello/getAll', {
        filter: $("input[name=filter_path]").val(),
        folder: JSON.stringify(folders),
        gps: false,
        begin: begin,
        nb: 2
                //$.param(folders)
    }, function(data) {
        addPictures(data);
        begin += 2;
    });
}

function addPictures(array) {
    for (f in array) {
        var tag = array[f];
        var ext = tag.path.substring(tag.path.lastIndexOf("."));
        ext = ext.toLowerCase();

        if (ext === '.jpeg' || ext === '.jpg' || ext === '.bmp' || ext === '.gif' || ext === '.png' || ext === '.tiff') {
            $('#gallery').append("<a href='rest/hello/getThumbnail?path=" + tag.path + "&w=0&h=0' title='" + tag.path + "' data-gallery=''><img src='rest/hello/getThumbnail?path=" + tag.path + "&w=70&h=70' alt='" + tag.path + "'></a>");
        }
    }
}

