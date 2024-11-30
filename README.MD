# SinkFinder + LLM
```shell
       _         _      __  _             _             
      (_)       | |    / _|(_)           | |            
___  _  _ __  | | __| |_  _  _ __    __| |  ___  _ __
/ __|| || '_ \ | |/ /|  _|| || '_ \  / _` | / _ \| '__|
\__ \| || | | ||   < | |  | || | | || (_| ||  __/| |   
|___/|_||_| |_||_|\_\|_|  |_||_| |_| \__,_| \___||_|   
                                         0.2@medi0cr1ty
```

### 功能说明

闭源系统半自动漏洞挖掘工具，针对 jar/war/zip 进行静态代码分析，增加LLM大模型能力验证路径是否能抵达，根据上下文代码逻辑给出该路径可信分数

### 规则说明

符号"*"仅可用于_inclusions相关的，表示允许所有。规则的白名单优先级高于黑名单。

```angular2html
{
    "depth": 3,  // 遍历深度
    "dashscope_api_key": "",  // 通义API_KEY配置 [sk-xxx]
    "path_exclusions": ["AndroidSDK",".idea","resources","java\\bin","META-INF"], // 文件路径黑名单，如设置为"test"，test/111.jar将不会被检索
    "jar_name_inclusions": ["*"], // jar文件名白名单，如设置为"test"，将仅检索包含test字符的jar包
    "jar_name_exclusions": ["SinkFinder","spring-","logback","lombok","META-INF","log4j","slf4j","tomcat-","mysql-connector-java","antlr-","commons-","dubbo-","jetty-","groovy-","netty-","collections-","jboss-","rxjava-","mybatis-","guava-","test","ehcache-","batik-"], // jar文件名黑名单
    "class_inclusions": ["*"], // 类白名单，如设置为"test"，com.test将进行检索
    "class_exclusions": ["logback","lombok"], // 类黑名单，如设置为"test"，com.test将无法检索
    "sink_rules": [
    {
    "sink_name": "RCE",
    "sink_desc": "任意代码执行漏洞",
    "severity_level": "High",
    "sinks": ["java.lang.Runtime:exec","java.lang.ProcessBuilder:<init>","java.lang.ProcessBuilder:start","javax.script.ScriptEngine:eval","javax.swing.plaf.synth.SynthLookAndFeel:load"]
    }, {
    "sink_name": "UNSERIALIZE",
    "sink_desc": "反序列化漏洞（包括原生反序列化、Yaml反序列化、XML反序列化、JDBC反序列化、json反序列化）",
    "severity_level": "High",
    "sinks": ["java.io.ObjectInputStream:readObject","java.io.ObjectInputStream:readUnshared",
    "org.yaml.snakeyaml.Yaml:load","java.beans.XMLDecoder:readObject",
    "org.apache.xmlrpc.parser.XmlRpcRequestParser:startElement","org.apache.xmlrpc.parser.XmlRpcRequestParser:endElement","com.thoughtworks.xstream.XStream:fromXML",
    "com.mysql.cj.jdbc.result.ResultSetImpl:getObject", "java.sql.DriverManager:getConnection","java.sql.Driver:connect",
    "com.alibaba.fastjson.JSON:parseObject","com.alibaba.fastjson.JSON:parse"]
    }, {
    "sink_name": "XSLT",
    "sink_desc": "XSLT注入漏洞",
    "severity_level": "High",
    "sinks": ["org.apache.xml.security.transforms.Transforms:performTransforms"]
    }, {
    "sink_name": "JNDI",
    "sink_desc": "JNDI注入漏洞",
    "severity_level": "High",
    "sinks": ["javax.naming.InitialContext:doLookup","javax.naming.InitialContext:lookup"]
    }, {
    "sink_name": "AuthBypass",
    "sink_desc": "身份认证绕过风险",
    "severity_level": "High",
    "sinks": ["javax.servlet.http.HttpServletRequest:getRequestURI","javax.servlet.http.HttpServletRequest:getRequestURL"]
    }, {
    "sink_name": "SSTI",
    "sink_desc": "模版注入漏洞（Velocity）",
    "severity_level": "High",
    "sinks": ["org.apache.velocity.app.Velocity:evaluate"]
    }, {
    "sink_name": "SPEL",
    "sink_desc": "表达式执行漏洞（SPEL）",
    "severity_level": "High",
    "sinks": ["org.springframework.expression.spel.standard.SpelExpression:getValue"]
    }, {
    "sink_name": "ZIPSLIP",
    "sink_desc": "ZIP目录穿越漏洞",
    "severity_level": "High",
    "sinks": ["java.util.zip.ZipInputStream:close"]
    }, {
    "sink_name": "DynamicInvoke",
    "sink_desc": "动态调用风险",
    "severity_level": "High",
    "sinks": ["java.lang.reflect.Constructor:newInstance","java.lang.reflect.Method:invoke"]
    }, {
    "sink_name": "XXE",
    "sink_desc": "外部实体注入漏洞",
    "severity_level": "Medium",
    "sinks": ["javax.xml.parsers.DocumentBuilder:parse","javax.xml.parsers.SAXParser:parse", "com.sun.org.apache.xerces.internal.parsers.DOMParser:parse","org.dom4j.io.SAXReader:read","org.xml.sax.XMLReader:parse","org.jdom2.input.SAXBuilder:build","org.apache.commons.digester3.Digester:parse","org.dom4j.DocumentHelper:parseText","org.apache.poi.xssf.usermodel.XSSFWorkbook:<init>"]
    }, {
    "sink_name": "SSRF",
    "sink_desc": "服务端请求伪造漏洞",
    "severity_level": "Medium",
    "sinks": ["java.net.URL:openConnection","java.net.URL:openStream","org.springframework.web.client.RestTemplate:exchange","org.apache.http.client.fluent.Request:Get","javax.imageio.ImageIO:read","com.squareup.okhttp.OkHttpClient:newCall","org.apache.http.impl.client.CloseableHttpClient:execute","org.jsoup.Jsoup:connect","org.apache.commons.io.IOUtils:toByteArray","org.apache.http.client.HttpClient:execute"]
    }
    ]
 }
```

### 运行说明
参数说明：
```
java -jar SinkFinder-1.0-SNAPSHOT-jar-with-dependencies.jar -h
       _         _      __  _             _             
      (_)       | |    / _|(_)           | |            
  ___  _  _ __  | | __| |_  _  _ __    __| |  ___  _ __ 
 / __|| || '_ \ | |/ /|  _|| || '_ \  / _` | / _ \| '__|
 \__ \| || | | ||   < | |  | || | | || (_| ||  __/| |   
 |___/|_||_| |_||_|\_\|_|  |_||_| |_| \__,_| \___||_|   
                                             0.2@medi0cr1ty
                                                        
usage: SinkFinder
 -cb,--class_exclusions <arg>         自定义class_exclusions规则，类黑名单
 -ci,--class_inclusions <arg>         自定义class_inclusions规则，类白名单
 -d,--depth <3>                       指定递归查找深度
 -h,--help                            帮助
 -jb,--jar_exclusions <arg>           自定义jar_exclusions规则，jar包黑名单
 -ji,--jar_inclusions <arg>           自定义jar_inclusions规则，jar包白名单
 -l,--llm                             启用通义大模型能力
 -lk,--llm_key <arg>                  配置通义大模型 API KEY（sk-xxx）
 -p,--path <arg>                      指定目标分析路径
 -r,--rule <rules.json>               指定Sink JSON规则路径，初始化默认resources/rules.json
 -s,--sink <arg>                      自定义sink规则
 -scb,--sink_category_block <arg>     禁用sink规则类别
 -sci,--sink_category_include <arg>   配置sink规则类别
```

* 运行配置可通过参数进行覆盖，跑第一次会吐出rule.json配置文件，-r可自定义配置
* LLM能力需要配置通义的APIKEY：更新rule.json中dashscope_api_key值/通过-lk参数指定（也可环境变量配置：export DASHSCOPE_API_KEY="sk-xxx"

参考运行命令：
```shell
java -jar SinkFinder-1.0-SNAPSHOT-jar-with-dependencies.jar -p 代码路径 -d 遍历路径递归深度
```

其他可参考文章：https://mp.weixin.qq.com/s/pKA0eG0B_yMkeV2-C1edWw

**欢迎Star&交流～**