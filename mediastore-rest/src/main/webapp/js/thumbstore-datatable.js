/** Datatables function **/

function getJsonForDT(array) {

    var JSONObj = new Object();
    var aoColumns1 = new Object();
    var aoColumns2 = new Object();
    var aoColumns3 = new Object();
    var aoColumns4 = new Object();
    var aoColumns5 = new Object();
    var aoColumns6 = new Object();

    aoColumns4.bVisible = false;

    JSONObj.sEcho = 1;
    JSONObj.iTotalRecords = 3;
    JSONObj.iTotalDisplayRecords = 3;
    JSONObj.bProcessing = true;
    JSONObj.iDisplayLength = 5;
    JSONObj.bPaginate = true; //enlève la pagination
    JSONObj.sPaginationType = "simple";
    JSONObj.bFilter = true; //enlève le search
    JSONObj.sDom = "<'row'<'span8'l><'span8'f>r>t<'row'<'span8'i><'span8'p>>";

    JSONObj.bLengthChange = false;

    aoColumns1.sTitle = "Miniature";
    aoColumns2.sTitle = "Nom";
    aoColumns3.sTitle = "Taille";
    aoColumns4.sTitle = "Path";
    aoColumns5.sTitle = "Lat";
    aoColumns6.sTitle = "Lon";

    JSONObj.aoColumns = [aoColumns1, aoColumns2, aoColumns3, aoColumns4, aoColumns5, aoColumns6];

    var aaData = new Array();

    for (i in array) {
        var row = new Object();
        row.DT_RowId = "row_" + i;
        row[0] = '<img src="rest/hello/getThumbnail?path=' + array[i].path + '&w=80&h=80"/>';
        row[1] = array[i].path.substring(array[i].path.lastIndexOf("\\"));
        row[2] = array[i].size;
        row[3] = array[i].path;
        row[4] = array[i].lat;
        row[5] = array[i].lon;

        aaData.push(row);
    }
    JSONObj.aaData = aaData;

    //pour debug
    var str = JSON.stringify(JSONObj, undefined, 3);
    //console.log(str);
    var oTable;

    $(document).ready(function() {
        oTable = $('#example').dataTable(JSONObj);
    });

    $(document).ready(function() {
        var table = $('#example').DataTable();

        $('#example tbody').on('click', 'tr', function() {
            if ($(this).hasClass('selected')) {
                $(this).removeClass('selected');
            }
            else {
                table.$('tr.selected').removeClass('selected');
                $(this).addClass('selected');

                var pos = $(this).index();
                aData = oTable.fnGetData(pos);

                console.log(aData[0]);
                console.log(aData[1]);
                console.log(aData[2]);
                console.log(aData[3]);
                console.log(aData[4]);
                console.log(aData[5]);

                //MAJ de la map
                changeMarkerPosition(aData[4], aData[5]);

                //bug
                // callDelete(aData[3]);
            }


        });

        $('#delete').click(function() {
            table.row('.selected').remove().draw(false);
        });
    });
}


function buildDataTables() {
    // debugger;
    var folders = getSelectedFolders();
    $.getJSON('rest/hello/getAll', {
        filter: $("input[name=filter]").val(),
        folder: JSON.stringify(folders),
        gps: $("input[name=gps]").is(":checked")
                //$.param(folders)
    }, function(data) {
        //var str = JSON.stringify(data, undefined, 2);
        // console.log("pretty print  : " + str);
        getJsonForDT(data);
        //   debugger;
    });
}





