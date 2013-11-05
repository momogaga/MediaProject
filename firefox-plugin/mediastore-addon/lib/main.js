var contextMenu = require("sdk/context-menu");
var tabs = require("sdk/tabs");
var data = require("sdk/self").data;
var Request = require("sdk/request").Request;

var menuItem = contextMenu.Item({
    label:"Image Selection",
    context: contextMenu.SelectorContext("img"),
    contentScript:'self.on("context", function (node) {' +
        '  return true;' +
        '});',
    contentScript:'self.on("click", function (node, data) {' +
        '  console.log("Item clicked!xxx " + node.src);' +
        '  self.postMessage(node.src);' +
        '});',
    onMessage:function toto(src) {
        handleMyMessage(src);
    }

});

//menuItem.port.on("openTab", function test(src) {alert(src);});


function handleMyMessage(src) {
    console.log(src);
    //tabs.open(src);
    // Handle the message
    var panel = require("sdk/panel").Panel({
        width: 300,
        height: 300,
        //contentURL: "http://localhost:8080/rest/hello/findSimilarFromURL?url="+src
        contentURL:data.url("panel.html"),
        contentScriptFile:data.url("panel.js")
    });

  //  src="http://www.iwallpapersfive.com/wp-content/uploads/2013/08/Stunning-Lamborghini-HD-Wallpapers-1080p-Cars.jpg"
    var latestTweetRequest = Request({
        url:"http://localhost:8080/rest/hello/findSimilarFromURL?url="+src,
        onComplete:function (response) {
            //var tweet = response.json[0];
            console.log("latestTweetRequest : got response! " + response.json);
            debugger;
            panel.port.emit("result", response.json);
        }
    });
    //  panel.port.emit("url", "toto titi");
    latestTweetRequest.get();

    panel.show();
}
