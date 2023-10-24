# sinkfinder

### 功能说明

执行jar,zip的分析。

### 规则说明

符号"*"仅可用于_inclusions相关的,表示允许所有。规则的白名单优先级高于黑名单。

```angular2html
{
    "path_exclusions": ["AndroidSDK",".idea","resources",".git"], //文件路径黑名单,如设置为"test",test/111.jar无法通过检查
    "jar_name_inclusions": ["*"], //jar文件名白名单,如设置为"test",test111.jar可通过检查
    "jar_name_exclusions": ["sinkfinder-2"], //jar文件名黑名单,如设置为"test",test111.jar无法通过检查
    "class_inclusions": ["*"], //类白名单,如设置为"test",com.test可通过检查
    "class_exclusions": ["logback","lombok"], //类黑名单,如设置为"test",com.test无法通过检查
    "sink_rules": [
    {
    "sink_name": "RCE",
    "severity_level": "High",
    "sinks": ["java.lang.Runtime:exec","java.lang.ProcessBuilder:start","javax.script.ScriptEngine:eval"]
    }, {
    "sink_name": "SSRF",
    "severity_level": "Medium",
    "sinks": ["java.net.URL:openConnection","java.net.URL:openStream","org.apache.http.client.fluent.Request:Get","javax.imageio.ImageIO:read","org.apache.http.impl.client.CloseableHttpClient:execute","org.apache.commons.httpclient.HttpClient:executeMethod","org.jsoup.Jsoup:connect","org.apache.commons.io.IOUtils:toByteArray"]
    },{
    "sink_name": "Fastjson",
    "severity_level": "Medium",
    "sinks": ["com.alibaba.fastjson.JSON:parseObject","com.alibaba.fastjson.JSON:parse"]
    },{
    "sink_name": "XXE",
    "severity_level": "Medium",
    "sinks": ["javax.xml.parsers.DocumentBuilder:parse","javax.xml.parsers.SAXParser:parse", "com.sun.org.apache.xerces.internal.parsers.DOMParser:parse","org.dom4j.io.SAXReader:read","org.xml.sax.XMLReader:parse","org.jdom2.input.SAXBuilder:build","org.apache.commons.digester3.Digester:parse","org.dom4j.DocumentHelper:parseText"]
    },{
    "sink_name": "UNSERIALIZE",
    "severity_level": "High",
    "sinks": ["java.io.ObjectInputStream:readObject","java.io.ObjectInputStream:readUnshared","org.yaml.snakeyaml.Yaml:load","com.thoughtworks.xstream.XStream:fromXML","com.mysql.cj.jdbc.result.ResultSetImpl:getObject","org.apache.xmlrpc.parser.XmlRpcRequestParser:startElement","java.beans.XMLDecoder:readObject","org.apache.xml.security.transforms.Transforms:performTransforms"]
    },{
    "sink_name": "JNDI",
    "severity_level": "High",
    "sinks": ["javax.naming.InitialContext:doLookup","javax.naming.InitialContext:lookup"]
    },{
    "sink_name": "SSTI",
    "severity_level": "High",
    "sinks": ["org.apache.velocity.app.Velocity:evaluate"]
    },{
    "sink_name": "SPEL",
    "severity_level": "High",
    "sinks": ["org.springframework.expression.spel.standard.SpelExpression:getValue"]
    }
  ]
}
```