  var fs = require('fs');
  var CodeGen = require('swagger-js-codegen').CodeGen;

  var clazzname = 'RulesApiSwaggerAngularClient'

  var file = '../src/main/resources/public/apidoc/api-client.json'
  var swagger = JSON.parse(fs.readFileSync(file, 'UTF-8'));
  //var nodejsSourceCode = CodeGen.getNodeCode({ className: 'Test', swagger: swagger });
  var angularjsSourceCode = CodeGen.getAngularCode({ moduleName: "rules-api-service", className: clazzname, swagger: swagger });

  var targetDir = "./target";

// ensure we clean out the folder every time this is run.
  var deleteRecursive = function(path) {
    if( fs.existsSync(path) ) {
      fs.readdirSync(path).forEach(function(file,index){
        var curPath = path + "/" + file;
        if(fs.lstatSync(curPath).isDirectory()) { // recurse
          deleteRecursive(curPath);
        } else { // delete file
          fs.unlinkSync(curPath);
        }
      });
      fs.rmdirSync(path);
    }
  };

  // Dumb path clean and creation
  var assetDir = "./src/main/public/js";
  deleteRecursive(assetDir);

  try {
  fs.mkdirSync("src");
  } catch (e) {
  // don't care
  }

  try {
    fs.mkdirSync("src/main");
  } catch (e2) {
    // dont care
  }

  try {
    fs.mkdirSync("src/main/public");
  } catch (e3) {
    // dont care
  }

  try {
    fs.mkdirSync(assetDir);
  } catch (e4) {
    // dont care
  }

  var fd = fs.openSync(assetDir + "/" + clazzname+ ".js",'w');

  fs.writeSync(fd, angularjsSourceCode);

