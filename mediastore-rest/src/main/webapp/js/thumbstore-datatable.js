/** Datatables function **/

var JSONObj = new Object();
var oTable;
var begin = 0;

//initialise la structure du tableau
function initDataTable() {

    var aoColumns1 = new Object();
    var aoColumns2 = new Object();
    var aoColumns3 = new Object();
    var aoColumns4 = new Object();
    var aoColumns5 = new Object();
    var aoColumns6 = new Object();

    aoColumns4.bVisible = false;
    aoColumns5.bVisible = false;
    aoColumns6.bVisible = false;

    JSONObj.sEcho = 1;
    JSONObj.iTotalRecords = 3;
    JSONObj.iTotalDisplayRecords = 3;
    JSONObj.bProcessing = true;
    JSONObj.iDisplayLength = 5;
    JSONObj.bPaginate = false; //enlève la pagination
    JSONObj.sPaginationType = "simple";
    JSONObj.bFilter = false; //enlève le search
    JSONObj.sDom = "<'row'<'span8'l><'span8'f>r>t<'row'<'span8'i><'span8'p>>";
    JSONObj.bLengthChange = false;
    JSONObj.bInfo = false;
    JSONObj.bSort = false;

    aoColumns1.sTitle = "Miniature";
    aoColumns2.sTitle = "Nom";
    aoColumns3.sTitle = "Taille";
    aoColumns4.sTitle = "Path";
    aoColumns5.sTitle = "Lat";
    aoColumns6.sTitle = "Lon";

    JSONObj.aoColumns = [aoColumns1, aoColumns2, aoColumns3, aoColumns4, aoColumns5, aoColumns6];
}

//ajoute les données au tableau
function rebuildData(array) {
    var aaData = new Array();
    for (i in array) {
        var row = new Object();
        row.DT_RowId = "row_" + i;

        var name = array[i].path.substring(array[i].path.lastIndexOf("\\"));
        var ext = name.substring(name.lastIndexOf("."));
        ext = ext.toLowerCase();

        if (ext === '.jpeg' || ext === '.jpg' || ext === '.bmp' || ext === '.gif' || ext === '.png' || ext === '.tiff') {
            row[0] = '<img src="rest/hello/getThumbnail?path=' + array[i].path + '&w=50&h=50"/>';
        } else {
            row[0] = '<i class="fa fa-file fa-3x"></i>';
        }
        row[1] = name;
        row[2] = array[i].size;
        row[3] = array[i].path;
        row[4] = array[i].lat;
        row[5] = array[i].lon;

        aaData.push(row);
    }
    // JSONObj.aaData = aaData;
    return aaData;
}

function constructTable(array) {

    var a = rebuildData(array);
    JSONObj.aaData = a;
    oTable = $('#example').dataTable(JSONObj);

    var table = $('#example').DataTable();

    $('#example tbody').on('click', 'tr', function() {
        if ($(this).hasClass('selected')) {
            $(this).removeClass('selected');

            $('#delete').attr("disabled", "disabled");
            $('#openFile').attr("disabled", "disabled");
            $('#openFolder').attr("disabled", "disabled");
            $('#viewMap').attr("disabled", "disabled");

            $("#infos").html(" ");
            $("#collapseOne").attr("class", "panel-collapse collapse");

        }
        else {
            table.$('tr.selected').removeClass('selected');
            $(this).addClass('selected');

            var pos = $(this).index();
            aData = oTable.fnGetData(pos);

            var infos = "Nom : " + aData[1].substring(1)
                    + "<br />" + "Taille : " + aData[2] + "kB"
                    + "<br />" + "Paths : " + aData[3]
                    + "<br />" + "Coordonn&eacutee maps : " + aData[4] + "," + aData[5];

            $("#infos").html(infos);
            $("#collapseOne").attr("class", "panel-collapse collapse in");

            $('#delete').removeAttr("disabled");
            $('#openFile').removeAttr("disabled");
            $('#openFolder').removeAttr("disabled");

            if (aData[4] !== 0 && aData[5] !== 0) {
                $('#viewMap').removeAttr("disabled");
            }
        }

    });
    $('#example').on('page.dt', function() {
        console.log('page');
    }).dataTable();

    //action du delete
    $('#delete').click(function() {
        callDelete(aData[3]);
        table.row('.selected').remove().draw(false);
    });
    //action du open file
    $('#openFile').click(function() {
        open(aData[3]);
    });
    //action du open folder
    $('#openFolder').click(function() {
        var folder = aData[3];
        folder = aData[3].substring(0, aData[3].lastIndexOf("\\"));
        open(folder);
    });
    $('#myModal').on('shown.bs.modal', function() {
        changeMarkerPosition(aData[4], aData[5]);
    });
    $("#nextPage").click(function() {
        begin += 5;
        loadData(begin);
    });
    $("#previousPage").click(function() {
        begin -= 5;
        loadData(begin);
        $("#nextPage").removeAttr("disabled");
    });
}

function callRestForDatatable() {
    var folders = getSelectedFolders();
    $.getJSON('rest/hello/getAll', {
        filter: $("input[name=filter]").val(),
        folder: JSON.stringify(folders),
        begin: 0,
        gps: $("input[name=gps]").is(":checked"),
        nb: 5
    }, function(data) {
        constructTable(data);
    });
}

function loadData(begin) {
    var folders = getSelectedFolders();
    $.getJSON('rest/hello/getAll', {
        filter: $("input[name=filter]").val(),
        folder: JSON.stringify(folders),
        begin: begin,
        gps: $("input[name=gps]").is(":checked"),
        nb: 5
    }, function(data) {
        if (data.length !== 0) {
            var t = $('#example').dataTable();
            t.fnClearTable();
            t.destroy();
            t.fnAddData(rebuildData(data));
            t.fnDraw();
            if (data.length < 5) {
                $('#nextPage').attr("disabled", "disabled");
            }
            if (begin > 0) {
                $('#previousPage').removeAttr("disabled");
            }
            if (begin === 0) {
                $('#previousPage').attr("disabled", "disabled");
            }
        }
        else{
            $('#nextPage').attr("disabled", "disabled");
        }
    });
}


