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
            //max:$("input[name=max]").val(),
            folder:folders,
            max:$("input[name=max]").val()
        },
        function (data) {
            var i = 1;
            var output = ""; // '<div id="accordion">';
            for (i in data) {
                output += "<h3>";
                output += data[i].fileSize / 1024.0 / 1024;
                output += "</h3>";
                output += "<div>";
                for (f in data[i].al) {
                    output += '<div id="imagePath">';
                    output += toFolderAndFileLink(data[i].al[f]) + "</div>";
                }
                output += "</div>";
            }
            updateAccordion(output);
        });
}

function toDirectLink(path) {
    return '<a href="' + path + '">[path]</a>'
}

function toFolderLink(path) {
    return path + '  <a href="explorer://rest/hello/folder/?path=' + path + '">[folder]</a>'
}


function toFolderAndFileLink(path) {
    var n = path.lastIndexOf('/');
    if (n == -1) {
        //ok, maybe it's a windows path
        n = path.lastIndexOf('\\');
    }
    //  var file = path.substring(n + 1);
    var folder = path.substring(0, n);

    return path + '  <a href="explorer://rest/hello/folder/?path=' + path + '">[file]</a>' +
        '  <a href="explorer://rest/hello/folder/?path=' + folder + '">[folder]</a>'
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
    $.getJSON('rest/hello/duplicateFolderDetails', {
            folder1:folder1,
            folder2:folder2
        },
        function (data) {
            var tab = { files:[ ] };
            for (var i = 0; i < data.file1.length; ++i)
                tab.files.push({
                    f1:data.file1[i],
                    f2:data.file2[i]
                });
            var templateFiles = ' {{#files}} ' + '<div class="paths">{{f1}}   <a href="explorer://rest/hello/folder/?path={{f1}}">[file]</a><br>' +
                '{{f2}}   <a href="explorer://rest/hello/folder/?path={{f2}}">[file]</a></div><br>' +
                '{{/files}}';
            var htmlFiles = Mustache.to_html(templateFiles, tab);
            $('#duplicate-folders-details').children().remove();
            $('#duplicate-folders-details').append(htmlFiles);
        });

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
    $('#accordion-duplicate-folders').children().remove();
    $('#duplicate-folders-details').children().remove();
    $('#duplicate-folder-table').children().remove();

    var html_table = '<thead> <tr> <th class="size ay-sort sorted-asc"><span>Size</span></th>'
        + ' <th class="files ay-sort"><span>#Files</span></th>  <th class="paths ay-sort"><span>Paths</span></th></tr></thead> <tbody>';

    $.getJSON('rest/hello/duplicateFolder', {
        folder:folders
    }, function (data) {
        $.each(data, function (key, val) {
            val['totalSize'] = val['totalSize'] / 1024.0 / 1024;
            html_table += ' <tr>'
//                +'<td class="size"><a href="#"  onclick="getDuplicateFolderDetails(\''+ val['folder1'].replace(/\\/g, "\\\\") + '\',\''  + val['folder2'].replace(/\\/g, "\\\\") + '\')"'+ '>'
                + '<td class="size"><a href="#"  onclick=""' + '>'
                + val['totalSize'].toFixed(4) + '</a></td>'
                + '<td class="files"> ' + val['occurences'] + '</td>'
                + '<td class="paths"> <div id="folder1">' + toFolderLink(val['folder1']) + toDirectLink(val['folder1']) + '</div> ' +
                '<div id="folder2">' + toFolderLink(val['folder2']) + toDirectLink(val['folder2']) + '</div>  </td>'
                + '</tr> ';
        });
        html_table += '</tbody>';
        $('#duplicate-folder-table').append(html_table);

        $("#duplicate-folder-table").delegate("tr", "click", function () {
            $(this).addClass("selected").siblings().removeClass("selected");
        });


        $(function () {
            $.ay.tableSort({target:$('table'), debug:false});

        });

        $(document).ready(function () {
            $('#duplicate-folder-table tr').click(function () {
                //ugly code to avoid Text object
                var tmp = $("#folder1",this).clone();
                tmp.children().remove();
                var folder1 = tmp.html().trim();

                tmp =  $("#folder2",this).clone();
                tmp.children().remove();
                var folder2 = tmp.html().trim();

                getDuplicateFolderDetails(folder1, folder2);
            });
        });
    });
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
        var template = '<img src="data:image;base64,{{base64Data}}" title="{{path}} "/>';
        var imgTag = Mustache.to_html(template, image);

        description = '<div class="description flt"> Distance:' + rmse + '<br>  ' + toFolderAndFileLink(image.path) + '</a><br></div>'

        $("#duplicate_upload_result").append('<div class="floated_img cls"><div class="nailthumb-container nailthumb-image-titles-animated-onhover square flt">' + imgTag + "</div>" + description + "</div>");
    }
    jQuery(document).ready(function () {
        jQuery('.nailthumb-container').nailthumb();
        jQuery('.nailthumb-image-titles-animated-onhover').nailthumb();
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
