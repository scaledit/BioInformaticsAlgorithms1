var fs = require('fs');
var CodeGen = require('swagger-js-codegen').CodeGen;

var clazzname = 'RulesApiSwaggerNodeClient'
var file = '/Users/nrussell/repos/kubitschek/src/main/resources/public/apidoc/api-client.json'
var swagger = JSON.parse(fs.readFileSync(file, 'UTF-8'));
var nodejsSourceCode = CodeGen.getNodeCode({ className: clazzname, swagger: swagger });

var targetDir = "./target";
try {
fs.mkdirSync(targetDir);
} catch (e) {
// don't care
}
var fd = fs.openSync("./target/" + clazzname + ".js",'w');
fs.writeSync(fd, nodejsSourceCode);
