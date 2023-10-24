# sinkfinder

### 功能说明

执行jar,zip,war的分析,注意tomcat项目目前需要手动压缩下webapps目录。

### 规则说明

符号"*"仅可用于_includes相关的,表示允许所有。规则的白名单优先级高于黑名单。

```angular2html
{
  "jar_name_includes": ["*"], //jar文件白名单,包含关系,如设置为"test",test111.jar可通过检查
  "jar_name_exclusions": ["apache","sinkfinder"],//jar文件黑名单,包含关系,如设置为"test",test111.jar无法通过检查
  "jar_path_exclusions": ["AndroidSDK","target"],//jar文件文件路径黑名单,包含关系,如设置为"test",test/111.jar无法通过检查
  "class_includes": ["*"], //类白名单,包含关系,如设置为"test",com.test可通过检查
  "class_exclusions": ["org.springframework","apache","logback","lombok"], //类黑名单,包含关系,如设置为"test",com.test无法通过检查
  "rules": [
    {
      "sink_name": "Command Inject",
      "severity_level": "High",
      "sink_rules": ["java.lang.Runtime:exec","java.lang.ProcessBuilder:command","java.lang.ProcessBuilder:<init>","java.lang.ProcessBuilder:start"]
    },
    {
      "sink_name": "SSRF",
      "severity_level": "Medium",
      "sink_rules": ["java.net.URL:openConnection","java.net.URL:openStream","org.apache.http.client.fluent.Request:Get"]
    },{
      "sink_name": "Fastjson",
      "severity_level": "Medium",
      "sink_rules": ["com.alibaba.fastjson.JSON:parseObject","com.alibaba.fastjson.JSON:parse"]
    },{
      "sink_name": "XXE",
      "severity_level": "Medium",
      "sink_rules": ["javax.xml.parsers.DocumentBuilderFactory:parse","javax.xml.parsers.DocumentBuilder:parse"]
    },{
      "sink_name": "UNSERIALIZE",
      "severity_level": "High",
      "sink_rules": ["java.io.ObjectInputStream:readObject","java.io.ObjectInputStream:readUnshared","org.yaml.snakeyaml.Yaml:load","com.thoughtworks.xstream.XStream:fromXML"]
    },{
      "sink_name": "JNDI",
      "severity_level": "High",
      "sink_rules": ["javax.naming.InitialContext:lookup"]
    }
  ]
}

```