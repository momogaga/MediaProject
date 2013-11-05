var test=["http://vero61.v.e.pic.centerblog.net/ujtu06to.jpg","http://stock.wikimini.org/w/images/9/94/Chien.jpg","http://colibri45.c.o.pic.centerblog.net/xjgootmq.jpg","http://img1.gtsstatic.com/wallpapers/41835ef1bf01cb24857cb3187b458011_large.jpeg"];

self.port.on('url', function(url) {
  // get the url from the content script
  console.log("panel.js : received message " + url);
   callServer(url);

});


self.port.on('result', function(json) {
    // get the url from the content script
    console.log("panel.js : received message " + json);
    processJSON(json);

});


function getFolder(path) {
    var n = path.lastIndexOf('/');
    if (n == -1) {
        //ok, maybe it's a windows path
        n = path.lastIndexOf('\\');
    }
    //  var file = path.substring(n + 1);
    var folder = path.substring(0, n);
    return folder;
}

function processJSON(json) {
    //update the status
    var status = document.getElementById("status");
    status.innerHTML="Found " + json.lshCandidates + " candidates <br>";

   console.log("processJSON " + json);
    console.log(json.images)
    var myPhotos = document.getElementById("photos");
    console.log("photos found " + myPhotos);
    for (i in json.images) {
        var tagA = document.createElement("a");
        var folder = getFolder(json.images[i].path);
        tagA.href='http://localhost:8080/rest/hello/open?path='+ encodeURIComponent('{"folders":["'+ folder + '"]}');
        var tagImg = document.createElement("img");
        tagImg.src = "data:image;base64,"+json.images[i].base64Data;
       // console.log(tagImg.src);
        tagA.appendChild(tagImg);
        myPhotos.appendChild(tagA);
    }
}


function callServer(url) {
 console.log("callServer on " + url);

}


