//used to store details of duplicateFolders
var duplicateFolderDetails = {};

function getSize() {
    $.get("rest/hello/db/size", function (data) {
        document.getElementById('db_size').innerHTML = data;
    });
}

function getPath() {
    $.get("rest/hello/db/path", function (data) {
        document.getElementById('db_path').innerHTML = data;
    });
}

function getIndexedPaths(div) {
    $.get("rest/hello/paths", function (data) {
        var val = 1;
        var cbh = document.getElementById('db_paths');
        for (i in data) {
            var cb = document.createElement('input');
            cb.type = 'checkbox';
            cb.checked = true;
            cbh.appendChild(cb);
            cb.name = "folder";
            cb.value = data[i];
            cbh.appendChild(document.createTextNode(data[i]));
            val++;
            cbh.appendChild(document.createElement('br'));
        }
    });
}


function getStatus() {
    $.get("rest/hello/status",function (data) {
        document.getElementById('db_status').innerHTML = data["stringStatus"];
    }).error(function () {
            document.getElementById('db_status').innerHTML = "Cannot connect to REST service";

        });
}


function getDuplicate() {
    var folders = getSelectedFolders();
    $.get(
        "rest/hello/identical",
        {
            folder:folders,
            max:$("input[name=max]").val()
        },
        function (data) {
            var i = 1;
            var html_table = '<thead> <tr> <th class="size ay-sort sorted-asc"><span>Size</span></th>'
                + ' <th class="files ay-sort"><span>#Files</span></th>  <th class="paths ay-sort"><span>Paths</span></th></tr></thead> <tbody>';

            var template = ' <tr >'
                + '<td class="size"><a href="#"  onclick=""> {{fileSize}}</a></td>'
                + '<td class="files">{{occurences}}</td><td class="paths"> ';

            for (i in data) {
                data[i]['occurences'] = data[i].al.length;
                data[i]['fileSize'] = data[i]['fileSize']/1024/1024;
                var rowTag = Mustache.to_html(template, data[i]);
                for (f in data[i].al) {
                    rowTag += '<div>' + toFolderAndFileLink(data[i].al[f]) + '</div> ';
                }
                html_table += rowTag + '</td></tr> ';
            }
            html_table += '</tbody>';
            updateDuplicateTable('#duplicate-file-table', html_table);
        });
}


function updateDuplicateTable(table, html_table) {
    $(table).children().remove();
    $(table).append(html_table);
    $(table).delegate("tr", "click", function () {
        $(this).addClass("selected").siblings().removeClass("selected");
    });
    $(document).ready(function () {
        $.ay.tableSort({target:$('table'), debug:false});
        generatePathLink();
    });
}


function toDirectLink(path) {
    return '<a  class="pathlink" href="#!' + path + '">[path]</a>'
}

function toFolderLink(path) {
    var a1 = '  <a class="pathlink" href="#!"  data-p1="' + path + '">';
    var a2 = ' [folder] <a>';


    return path + a1 + a2;
}


function toFolderAndFileLink(path) {
    var n = path.lastIndexOf('/');
    if (n == -1) {
        //ok, maybe it's a windows path
        n = path.lastIndexOf('\\');
    }
    //  var file = path.substring(n + 1);
    var folder = path.substring(0, n);


    return path + '  <a class="pathlink" href="#!"  data-p1="' + path + '">' + '  [open file]</a> ' +
        '  <a class="pathlink" href="#!"  data-p1="' + folder + '">' + '  [open folder]</a>'
}

function toDualFolderLink(path1, path2) {
    return  '  <a href="explorer://rest/hello/folder/?path1=' + path1 + '&path2=' + path2 + '">[folders]</a>'
}

function updateAccordion(output) {

    $('#accordion').children().remove();

    $('#accordion').append(output).accordion('destroy').accordion({
        collapsible:true,
        autoHeight:false,
        active:false,
        change:function (event, ui) {
            if ($(".nailthumb-container", ui.newContent).length == 0) {
                $.each($("[id=imagePath]", ui.newContent), function (index, data) {
                });
            }
        }

    });

}


function getDuplicateFolderDetails(folder1, folder2) {

    var d = duplicateFolderDetails[folder1 + folder2];
    console.log(d);
  //  debugger;

//    $.getJSON('rest/hello/duplicateFolderDetails', {
//            folder1:folder1,
//            folder2:folder2
//        },
//        function (data) {
            var tab = { files:[ ] };
    for (var i = 0; i < d[0].length; ++i) {
        tab.files.push({
            f1:d[0][i],
            f2:d[1][i]
        });
    }
    var templateFiles = ' {{#files}} ' + '<div class="paths">{{f1}}   <a class="pathlink" href="#!"  data-p1="{{f1}}">[file]</a><br>' +
        '{{f2}}   <a class="pathlink" href="#!"  data-p1="{{f2}}">[file]</a></div><br>' +
        '{{/files}}';
    var htmlFiles = Mustache.to_html(templateFiles, tab);
    $('#duplicate-folders-details').children().remove();
    $('#duplicate-folders-details').append(htmlFiles);
    $(document).ready(function () {
        generatePathLink();

    });
//        });

}


function getSelectedFolders() {
    var inputs = $("input[name=folder]");
    var folders = [];
    for (i = 0; i < inputs.length; i++) {
        if (inputs[i].checked) {
            folders.push(inputs[i].value);
        }

    }
    return folders;
}

function getDuplicateFolder() {

    var folders = getSelectedFolders();
    $('#duplicate-folders-details').children().remove();

    var html_table = '<thead> <tr> <th class="size ay-sort sorted-asc"><span>Size</span></th>'
        + ' <th class="files ay-sort"><span>#Files</span></th>'+
        '<th class="paths ay-sort"><span>&#37;F1</span></th>' +
        '<th class="paths ay-sort"><span>&#37;F2</span></th>' +
        '<th class="paths ay-sort"><span>Paths</span></th>' +
        '</tr></thead> <tbody>';


    var template = ' <tr data-p1="{{folder1}}" data-p2="{{folder2}}" >'
        + '<td class="size"><a href="#"  onclick=""> {{totalSize}}</a></td>'
        + '<td class="files">{{occurences}}</td>'
        + '<td class="f1">{{filesInFolder1}}</td>'
        + '<td class="f2">{{filesInFolder2}}</td>';

    $.getJSON('rest/hello/duplicateFolder', {
        folder:folders
    }, function (data) {
        $.each(data, function (key, val) {
            val['totalSize'] = val['totalSize'] / 1024.0 / 1024;
            val['totalSize'] = val['totalSize'].toFixed(4);

            val['filesInFolder1'] = (val['occurences'] * 100.0 / val['filesInFolder1']).toFixed(0);
            val['filesInFolder2'] = (val['occurences'] * 100.0 / val['filesInFolder2']).toFixed(0);

            var rowTag = Mustache.to_html(template, val);
            html_table += rowTag
                + '<td class="paths"> <div id="folder1">' + toFolderLink(val['folder1']) + '</div> ' +
                '<div id="folder2">' + toFolderLink(val['folder2']) + ' ' +
                '</div>  </td>'
                + '</tr> ';
            var fileArray = new Array();
            fileArray[0] = val['file1'];
            fileArray[1] = val['file2'];

            duplicateFolderDetails[val['folder1'] + val['folder2']] = fileArray;
        });
        html_table += '</tbody>';


        //  debugger;
        updateDuplicateTable('#duplicate-folder-table', html_table);

        $(document).ready(function () {
            $('#duplicate-folder-table tr').click(function () {
                var $this = $(this);
                var folder1 = $this.data('p1');
                var folder2 = $this.data('p2');
                getDuplicateFolderDetails(folder1, folder2);
            });
            $('.pathlink').click(function () {
                var $this = $(this);
                var p1 = $this.data('p1');
                var p2 = $this.data('p2');
                callOpen(p1, p2);
            });
        });
    });
}
function callOpen(para1, para2) {
    $.get("rest/hello/open", {path:para1});
}


function shrink() {
    var folders = getSelectedFolders();
    $.get("rest/hello/shrink", {  folder:folders}, function (data) {
    });
}

function update() {
    var folders = getSelectedFolders();
    $.get("rest/hello/update", {folder:folders}, function (data) {
    });
}

function shrinkUpdate() {
    var folders = getSelectedFolders();
    $.get("rest/hello/shrinkUpdate", {folder:folders}, function (data) {
    });
}


function index(currentForm) {

    prettyPrint(currentForm);
    val = document.getElementById("index_path").value;
    $.get("rest/hello/index", {
        path:val
    }, function (data) {
    });
}

function prettyPrint(object) {
    for (i in object) {
        console.log(i + " " + object[i]);
    }
}

function uploadFinished(object) {
    $('#duplicate_upload_result').children().remove();
    for (f in object) {
        var image = object[f];
        var rmse = (image.rmse);
        var template = '<img class="pathlink" src="data:image;base64,{{base64Data}}" title="{{path}} "/>';
        var imgTag = Mustache.to_html(template, image);

        description = '<div class="description flt"> Distance:' + rmse + '<br>  ' + toFolderAndFileLink(image.path) + '</a><br></div>'

        $("#duplicate_upload_result").append('<div class="floated_img cls"><div class="nailthumb-container nailthumb-image-titles-animated-onhover square flt">' + imgTag + "</div>" + description + "</div>");
    }
    jQuery(document).ready(function () {

        generatePathLink();
        jQuery('.nailthumb-container').nailthumb();
        jQuery('.nailthumb-image-titles-animated-onhover').nailthumb();
    });
}

function generatePathLink() {
    $('.pathlink').click(function () {
        var $this = $(this);
        var p1 = $this.data('p1');
        var p2 = $this.data('p2');
        callOpen(p1, p2);
    });
}

function getWithRMSE(param, rmse) {
    $.get("rest/hello/getThumbnail/", param.path, function (image) {
        var template = "<img src=\"data:image;base64,{{data}}\" title=\" {{title}} \"/>";
        var imgTag = Mustache.to_html(template, image);
        $("#duplicate_upload_result").prepend('<div class="floated_img"><div class="nailthumb-container nailthumb-image-titles-animated-onhover square">' + imgTag + "</div>" + rmse + "  " + image.title + "</div>");
        jQuery(document).ready(function () {
            jQuery('.nailthumb-container').nailthumb();
            jQuery('.nailthumb-image-titles-animated-onhover').nailthumb();
        });

    });
}
