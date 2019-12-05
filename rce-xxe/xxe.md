标题 Java XXE测试用例详解

创建: 2019-11-21 15:42
更新: 2019-11-22 16:10
链接: http://scz.617.cn:8/misc/201911211542.txt
      https://www.t00ls.net/thread-53947-1-1.html

--------------------------------------------------------------------------

目录:

    ☆ 前言
    ☆ Java XXE测试用例
        0) 最简HTTP Server
        1) DocumentBuilderXXE.java
        2) DocumentBuilderNoXXE.java
            2.1) 关于setExpandEntityReferences(false)
        3) SAXBuilderXXE.java
        4) SAXBuilderNoXXE.java
        5) SAXParserXXE.java
        6) SAXParserNoXXE.java
        7) SAXTransformerFactoryXXE.java
        8) SAXTransformerFactoryNoXXE.java
        9) SAXReaderXXE.java
       10) SAXReaderNoXXE.java
       11) XMLReaderXXE.java
       12) XMLReaderNoXXE.java
       13) SchemaFactoryXXE.java
       14) SchemaFactoryNoXXE.java
       15) XMLStreamReaderXXE.java
       16) XMLStreamReaderNoXXE.java
       17) TransformerFactoryXXE.java
       18) TransformerFactoryNoXXE.java
       19) ValidatorXXE.java
       20) ValidatorNoXXE.java
       21) Unmarshaller No XXE
            21.1) Customer.java
            21.2) MarshallerCustomer.java
            21.3) UnmarshallerCustomer.java
       22) 防御方式小结
       23) 讨论
            23.1) 寻找潜在XXE点
            23.2) 防御XXE成功时的调用栈回溯
            23.3) Xerces XML parsers
    ☆ 后记
    ☆ 参考资源

--------------------------------------------------------------------------

☆ 前言

本文是下面这篇的学习笔记:

Java XXE 总结 - l1nk3r [2019-10-31]
http://www.lmxspace.com/2019/10/31/Java-XXE-%E6%80%BB%E7%BB%93/
https://www.t00ls.net/thread-53607-1-3.html

由于本文原创部分极少，万不敢将上篇只放在参考资源中，行文之始就开宗明义点题。
感兴趣者请自行围观原作。

话说我是在吐司上看到后才找到他个人主页的，看来我的吐司帐号没有白注册。感谢
注册过程中提供帮助的、未曾直接打过交道的朋友们，bluerust就算了，他本来就肩
负给我身后烧几个华为工程师下去的重任。

l1nk3r给了若干Java XXE示例。

他这些示例给得极好，至少对于Java XXE初学者(比如我)来说。有很多现实世界的老
洞可供研究，但需要搭建测试环境，比较麻烦。初学者比较关心有哪些方式进行XML
解析时会触发XXE，l1nk3r的示例很好地回答了这个问题，他演示了很多种方式。相
比leadroyal、spoock的文章，l1nk3r的示例对于新手更具可操作性，我学到很多，
感谢l1nk3r。

☆ Java XXE测试用例

本节示例代码一般形如SomeXXE.java、SomeNoXXE.java，前者是存在XXE的示例，后
者是修补示例。

演示时很多关键信息由调用栈回溯给出，其中的行号信息是Java版本强相关的，本节
所用Java版本如下:

$ java -version
openjdk version "1.8.0_232"
OpenJDK Runtime Environment (build 1.8.0_232-b09)
OpenJDK 64-Bit Server VM (build 25.232-b09, mixed mode)

0) 最简HTTP Server

本节不考虑回显式XXE，考虑更一般的OOB XXE。需要一个最简HTTP Server，方便测
试XXE，Python缺省就带。

$ python2 -m SimpleHTTPServer 8080
$ python3 -m http.server 8080

WEB根目录就是当前目录。如果想绑定地址，如下:

python2 -c "import BaseHTTPServer, SimpleHTTPServer;BaseHTTPServer.HTTPServer(('127.0.0.1',8080),SimpleHTTPServer.SimpleHTTPRequestHandler).serve_forever()"
python3 -c "import http.server;http.server.HTTPServer(('127.0.0.1',8080),http.server.SimpleHTTPRequestHandler).serve_forever()"
python3 -m http.server --bind 127.0.0.1 8080
python3 -m http.server -b 127.0.0.1 8080

1) DocumentBuilderXXE.java

--------------------------------------------------------------------------
/*
 * javac -encoding GBK -g DocumentBuilderXXE.java
 */
import java.io.*;
import javax.xml.parsers.*;
import org.w3c.dom.Document;

public class DocumentBuilderXXE
{
    public static void main ( String[] argv ) throws Exception
    {
        /*
         * 与C不同，Java的argv[0]对应第一个参数
         */
        File                    f   = new File( argv[0] );
        InputStream             is  = new FileInputStream( f );
        DocumentBuilderFactory  dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder         db  = dbf.newDocumentBuilder();
        Document                d   = db.parse( is );
        is.close();
    }
}
--------------------------------------------------------------------------

l1nk3r的原始示例将包含DTD的XML内嵌在代码中，用ByteArrayInputStream()将
String转成InputStream。我改成由命令行参数指定外部文件，用FileInputStream获
取InputStream，这样可以测试其他XML。

这种测试代码不需要指定package，就丢在当前目录下即可。

$ cat xxe_0.txt
<!DOCTYPE any [
<!ELEMENT any ANY>
<!ENTITY some SYSTEM "http://127.0.0.1:8080/nonexist">
]>
<any>&some;</any>

在DTD中使用http，可以方便地观察到XXE是否成功触发。与l1nk3r不同，我用了不存
在的资源，好处是很多测试用例因404而直接抛出异常、显示调用栈回溯，减少动态
调试环节。

$ java DocumentBuilderXXE xxe_0.txt
Exception in thread "main" java.io.FileNotFoundException: http://127.0.0.1:8080/nonexist
        at sun.net.www.protocol.http.HttpURLConnection.getInputStream0(HttpURLConnection.java:1896)
        at sun.net.www.protocol.http.HttpURLConnection.getInputStream(HttpURLConnection.java:1498)
        at com.sun.org.apache.xerces.internal.impl.XMLEntityManager.setupCurrentEntity(XMLEntityManager.java:647)
        at com.sun.org.apache.xerces.internal.impl.XMLEntityManager.startEntity(XMLEntityManager.java:1304)
        at com.sun.org.apache.xerces.internal.impl.XMLEntityManager.startEntity(XMLEntityManager.java:1240)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanEntityReference(XMLDocumentFragmentScannerImpl.java:1908)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl$FragmentContentDriver.next(XMLDocumentFragmentScannerImpl.java:3061)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl.next(XMLDocumentScannerImpl.java:602)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanDocument(XMLDocumentFragmentScannerImpl.java:505)
        at com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse(XML11Configuration.java:842)
        at com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse(XML11Configuration.java:771)
        at com.sun.org.apache.xerces.internal.parsers.XMLParser.parse(XMLParser.java:141)
        at com.sun.org.apache.xerces.internal.parsers.DOMParser.parse(DOMParser.java:243)
        at com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderImpl.parse(DocumentBuilderImpl.java:339)
        at javax.xml.parsers.DocumentBuilder.parse(DocumentBuilder.java:121)
        at DocumentBuilderXXE.main(DocumentBuilderXXE.java:19)

HTTP Server收到GET请求，返回404，导致客户端抛出异常。

l1nk3r原文有很长篇幅跟踪分析XML解析流程，至少对我而言，没多大必要，只要看
到上述调用栈回溯就够了，需要了解细节时去看相应栈帧指示的类即可。

2) DocumentBuilderNoXXE.java

--------------------------------------------------------------------------
/*
 * javac -encoding GBK -g DocumentBuilderNoXXE.java
 */
import java.io.*;
import javax.xml.parsers.*;
import org.w3c.dom.Document;

public class DocumentBuilderNoXXE
{
    private static DocumentBuilderFactory GetDocumentBuilderFactory () throws ParserConfigurationException
    {
        DocumentBuilderFactory  dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware( true );
        dbf.setFeature( "http://apache.org/xml/features/disallow-doctype-decl", true );             // 禁用DTD
        dbf.setFeature( "http://xml.org/sax/features/external-general-entities", false );           // 禁用外部通用实体
        dbf.setFeature( "http://xml.org/sax/features/external-parameter-entities", false );         // 禁用外部参数实体
        dbf.setFeature( "http://apache.org/xml/features/nonvalidating/load-external-dtd", false );  // 禁用外部DTD
        dbf.setXIncludeAware( false );
        dbf.setExpandEntityReferences( false );
        return dbf;
    }

    public static void main ( String[] argv ) throws Exception
    {
        File                    f   = new File( argv[0] );
        InputStream             is  = new FileInputStream( f );
        /*
         * 原来是"DocumentBuilderFactory.newInstance()"，这样一变，就阻止
         * 了XXE的可能
         */
        DocumentBuilderFactory  dbf = GetDocumentBuilderFactory();
        DocumentBuilder         db  = dbf.newDocumentBuilder();
        Document                d   = db.parse( is );
        is.close();
    }
}
--------------------------------------------------------------------------

leadroyal强调，如果在newDocumentBuilder()之后再调setFeature()，防御失败。

GetDocumentBuilderFactory()演示的是充分非必要条件，可能有些设置显得冗余或
者不相关，这是Zimbra在修补CVE-2019-9670时用的方案。

$ java DocumentBuilderNoXXE xxe_0.txt
[Fatal Error] :1:10: DOCTYPE is disallowed when the feature "http://apache.org/xml/features/disallow-doctype-decl" set to true.
Exception in thread "main" org.xml.sax.SAXParseException; lineNumber: 1; columnNumber: 10; DOCTYPE is disallowed when the feature "http://apache.org/xml/features/disallow-doctype-decl" set to true.
        at com.sun.org.apache.xerces.internal.parsers.DOMParser.parse(DOMParser.java:257)
        at com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderImpl.parse(DocumentBuilderImpl.java:339)
        at javax.xml.parsers.DocumentBuilder.parse(DocumentBuilder.java:121)
        at DocumentBuilderNoXXE.main(DocumentBuilderNoXXE.java:33)

HTTP Server不会收到GET请求，db.parse()时直接抛出异常。

$ java -agentlib:jdwp=transport=dt_socket,address=127.0.0.1:8005,server=y,suspend=y DocumentBuilderNoXXE xxe_0.txt
$ jdb -connect com.sun.jdi.SocketAttach:hostname=127.0.0.1,port=8005

按照l1nk3r的说法，流程会到达reportFatalError()。

最初可能并不知道reportFatalError()的存在，可以通过捕捉异常逼近:

catch com.sun.org.apache.xerces.internal.xni.parser.XMLParseException
catch *.XMLParseException

  [1] com.sun.org.apache.xerces.internal.util.DefaultErrorHandler.fatalError (DefaultErrorHandler.java:85), pc = 8
  [2] com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError (XMLErrorReporter.java:400), pc = 270
  [3] com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError (XMLErrorReporter.java:327), pc = 9
  [4] com.sun.org.apache.xerces.internal.impl.XMLScanner.reportFatalError (XMLScanner.java:1,472), pc = 13
  [5] com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl$PrologDriver.next (XMLDocumentScannerImpl.java:914), pc = 570
  [6] com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl.next (XMLDocumentScannerImpl.java:602), pc = 4
  [7] com.sun.org.apache.xerces.internal.impl.XMLNSDocumentScannerImpl.next (XMLNSDocumentScannerImpl.java:112), pc = 31
  [8] com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanDocument (XMLDocumentFragmentScannerImpl.java:505), pc = 308
  [9] com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse (XML11Configuration.java:842), pc = 123
  [10] com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse (XML11Configuration.java:771), pc = 29
  [11] com.sun.org.apache.xerces.internal.parsers.XMLParser.parse (XMLParser.java:141), pc = 76
  [12] com.sun.org.apache.xerces.internal.parsers.DOMParser.parse (DOMParser.java:243), pc = 43
  [13] com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderImpl.parse (DocumentBuilderImpl.java:339), pc = 57
  [14] javax.xml.parsers.DocumentBuilder.parse (DocumentBuilder.java:121), pc = 25
  [15] DocumentBuilderNoXXE.main (DocumentBuilderNoXXE.java:33), pc = 33

2.1) 关于setExpandEntityReferences(false)

GetDocumentBuilderFactory()中有一句:

setExpandEntityReferences( false )

leadroyal较早指出它无法防护XXE，后来图南、gyyyy、c0ny1、l1nk3r依次解释为什
么无法防护。

http://hg.openjdk.java.net/jdk/jdk/file/07212a29787a/src/java.xml/share/classes/javax/xml/parsers/DocumentBuilderFactory.java
https://docs.oracle.com/javase/8/docs/api/javax/xml/parsers/DocumentBuilderFactory.html

--------------------------------------------------------------------------
public void setExpandEntityReferences ( boolean expandEntityRef )

Specifies that the parser produced by this code will expand entity
reference nodes. By default the value of this is set to true

expandEntityRef - true if the parser produced will expand entity reference
nodes; false otherwise.
--------------------------------------------------------------------------

图南的文章中写道:

expandEntityRef = true

    表示展开或解析实体引用，即没有EntityReference节点

expandEntityRef = false

    指示解析器将EntityReference节点保留在DOM树中

无论如何设置expandEntityRef，DTD都已经被解析，无法防护XXE注入。

2019.1.29，JDK官方决定解决这个容易带来的歧义理解的问题，计划当
expandEntityRef为false时，解析器不再读取和解析任何实体引用。

gyyyy从源码角度介绍了setExpandEntityReferences()与DOM树的变化。

c0ny1指出，在setExpandEntityReferences(false)对XML解析器起作用前，解析器就
已经开始处理DTD，故其无法防御XXE漏洞。

l1nk3r指出，setExpandEntityReferences(false)作用于XML解析完成后所生成的DOM
树。

前面几位的文章已经把这个问题讲透了，不再重复。

3) SAXBuilderXXE.java

http://www.jdom.org/downloads/
http://www.jdom.org/dist/binary/jdom-2.0.6.zip

--------------------------------------------------------------------------
/*
 * javac -encoding GBK -g -cp jdom-2.0.6.jar SAXBuilderXXE.java
 */
import java.io.*;
import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;

public class SAXBuilderXXE
{
    public static void main ( String[] argv ) throws Exception
    {
        File        f   = new File( argv[0] );
        InputStream is  = new FileInputStream( f );
        SAXBuilder  sb  = new SAXBuilder();
        Document    d   = sb.build( is );
        is.close();
    }
}
--------------------------------------------------------------------------

确保当前目录下存在"jdom-2.0.6.jar"。Windows上classpath用分号(;)做分隔符，
Linux上用冒号(:)做分隔符。

$ java -cp "jdom-2.0.6.jar:." SAXBuilderXXE xxe_0.txt
Exception in thread "main" java.io.FileNotFoundException: http://127.0.0.1:8080/nonexist
        at sun.net.www.protocol.http.HttpURLConnection.getInputStream0(HttpURLConnection.java:1896)
        at sun.net.www.protocol.http.HttpURLConnection.getInputStream(HttpURLConnection.java:1498)
        at com.sun.org.apache.xerces.internal.impl.XMLEntityManager.setupCurrentEntity(XMLEntityManager.java:647)
        at com.sun.org.apache.xerces.internal.impl.XMLEntityManager.startEntity(XMLEntityManager.java:1304)
        at com.sun.org.apache.xerces.internal.impl.XMLEntityManager.startEntity(XMLEntityManager.java:1240)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanEntityReference(XMLDocumentFragmentScannerImpl.java:1908)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl$FragmentContentDriver.next(XMLDocumentFragmentScannerImpl.java:3061)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl.next(XMLDocumentScannerImpl.java:602)
        at com.sun.org.apache.xerces.internal.impl.XMLNSDocumentScannerImpl.next(XMLNSDocumentScannerImpl.java:112)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanDocument(XMLDocumentFragmentScannerImpl.java:505)
        at com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse(XML11Configuration.java:842)
        at com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse(XML11Configuration.java:771)
        at com.sun.org.apache.xerces.internal.parsers.XMLParser.parse(XMLParser.java:141)
        at com.sun.org.apache.xerces.internal.parsers.AbstractSAXParser.parse(AbstractSAXParser.java:1213)
        at com.sun.org.apache.xerces.internal.jaxp.SAXParserImpl$JAXPSAXParser.parse(SAXParserImpl.java:643)
        at org.jdom2.input.sax.SAXBuilderEngine.build(SAXBuilderEngine.java:217)
        at org.jdom2.input.sax.SAXBuilderEngine.build(SAXBuilderEngine.java:253)
        at org.jdom2.input.SAXBuilder.build(SAXBuilder.java:1091)
        at SAXBuilderXXE.main(SAXBuilderXXE.java:15)

HTTP Server收到GET请求，返回404，导致客户端抛出异常。

4) SAXBuilderNoXXE.java

--------------------------------------------------------------------------
/*
 * javac -encoding GBK -g -cp jdom-2.0.6.jar SAXBuilderNoXXE.java
 */
import java.io.*;
import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;

public class SAXBuilderNoXXE
{
    private static SAXBuilder GetSAXBuilder ()
    {
        SAXBuilder  sb  = new SAXBuilder();
        sb.setFeature( "http://apache.org/xml/features/disallow-doctype-decl", true );              // 禁用DTD
        sb.setFeature( "http://xml.org/sax/features/external-general-entities", false );            // 禁用外部通用实体
        sb.setFeature( "http://xml.org/sax/features/external-parameter-entities", false );          // 禁用外部参数实体
        sb.setFeature( "http://apache.org/xml/features/nonvalidating/load-external-dtd", false );   // 禁用外部DTD
        return sb;
    }

    public static void main ( String[] argv ) throws Exception
    {
        File        f   = new File( argv[0] );
        InputStream is  = new FileInputStream( f );
        /*
         * 原来是"new SAXBuilder()"
         */
        SAXBuilder  sb  = GetSAXBuilder();
        Document    d   = sb.build( is );
        is.close();
    }
}
--------------------------------------------------------------------------

$ java -cp "jdom-2.0.6.jar:." SAXBuilderNoXXE xxe_0.txt
Exception in thread "main" org.jdom2.input.JDOMParseException: Error on line 1: DOCTYPE is disallowed when the feature "http://apache.org/xml/features/disallow-doctype-decl" set to true.
        at org.jdom2.input.sax.SAXBuilderEngine.build(SAXBuilderEngine.java:232)
        at org.jdom2.input.sax.SAXBuilderEngine.build(SAXBuilderEngine.java:253)
        at org.jdom2.input.SAXBuilder.build(SAXBuilder.java:1091)
        at SAXBuilderNoXXE.main(SAXBuilderNoXXE.java:28)
Caused by: org.xml.sax.SAXParseException; lineNumber: 1; columnNumber: 10; DOCTYPE is disallowed when the feature "http://apache.org/xml/features/disallow-doctype-decl" set to true.
        at com.sun.org.apache.xerces.internal.util.ErrorHandlerWrapper.createSAXParseException(ErrorHandlerWrapper.java:203)
        at com.sun.org.apache.xerces.internal.util.ErrorHandlerWrapper.fatalError(ErrorHandlerWrapper.java:177)
        at com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError(XMLErrorReporter.java:400)
        at com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError(XMLErrorReporter.java:327)
        at com.sun.org.apache.xerces.internal.impl.XMLScanner.reportFatalError(XMLScanner.java:1472)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl$PrologDriver.next(XMLDocumentScannerImpl.java:914)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl.next(XMLDocumentScannerImpl.java:602)
        at com.sun.org.apache.xerces.internal.impl.XMLNSDocumentScannerImpl.next(XMLNSDocumentScannerImpl.java:112)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanDocument(XMLDocumentFragmentScannerImpl.java:505)
        at com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse(XML11Configuration.java:842)
        at com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse(XML11Configuration.java:771)
        at com.sun.org.apache.xerces.internal.parsers.XMLParser.parse(XMLParser.java:141)
        at com.sun.org.apache.xerces.internal.parsers.AbstractSAXParser.parse(AbstractSAXParser.java:1213)
        at com.sun.org.apache.xerces.internal.jaxp.SAXParserImpl$JAXPSAXParser.parse(SAXParserImpl.java:643)
        at org.jdom2.input.sax.SAXBuilderEngine.build(SAXBuilderEngine.java:217)
        ... 3 more

HTTP Server不会收到GET请求，sb.build()时直接抛出异常。

$ java -agentlib:jdwp=transport=dt_socket,address=127.0.0.1:8005,server=y,suspend=y -cp "jdom-2.0.6.jar:." SAXBuilderNoXXE xxe_0.txt
$ jdb -connect com.sun.jdi.SocketAttach:hostname=127.0.0.1,port=8005

catch *.XMLParseException

  [1] com.sun.org.apache.xerces.internal.util.ErrorHandlerWrapper.fatalError (ErrorHandlerWrapper.java:183), pc = 34
  [2] com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError (XMLErrorReporter.java:400), pc = 270
  [3] com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError (XMLErrorReporter.java:327), pc = 9
  [4] com.sun.org.apache.xerces.internal.impl.XMLScanner.reportFatalError (XMLScanner.java:1,472), pc = 13
  [5] com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl$PrologDriver.next (XMLDocumentScannerImpl.java:914), pc = 570
  [6] com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl.next (XMLDocumentScannerImpl.java:602), pc = 4
  [7] com.sun.org.apache.xerces.internal.impl.XMLNSDocumentScannerImpl.next (XMLNSDocumentScannerImpl.java:112), pc = 31
  [8] com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanDocument (XMLDocumentFragmentScannerImpl.java:505), pc = 308
  [9] com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse (XML11Configuration.java:842), pc = 123
  [10] com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse (XML11Configuration.java:771), pc = 29
  [11] com.sun.org.apache.xerces.internal.parsers.XMLParser.parse (XMLParser.java:141), pc = 76
  [12] com.sun.org.apache.xerces.internal.parsers.AbstractSAXParser.parse (AbstractSAXParser.java:1,213), pc = 43
  [13] com.sun.org.apache.xerces.internal.jaxp.SAXParserImpl$JAXPSAXParser.parse (SAXParserImpl.java:643), pc = 53
  [14] org.jdom2.input.sax.SAXBuilderEngine.build (SAXBuilderEngine.java:217), pc = 5
  [15] org.jdom2.input.sax.SAXBuilderEngine.build (SAXBuilderEngine.java:253), pc = 9
  [16] org.jdom2.input.SAXBuilder.build (SAXBuilder.java:1,091), pc = 5
  [17] SAXBuilderNoXXE.main (SAXBuilderNoXXE.java:28), pc = 26

5) SAXParserXXE.java

--------------------------------------------------------------------------
/*
 * javac -encoding GBK -g -Xlint:deprecation SAXParserXXE.java
 */
import java.io.*;
import javax.xml.parsers.*;
import org.xml.sax.HandlerBase;

public class SAXParserXXE
{
    public static void main ( String[] argv ) throws Exception
    {
        File                f   = new File( argv[0] );
        InputStream         is  = new FileInputStream( f );
        SAXParserFactory    spf = SAXParserFactory.newInstance();
        SAXParser           sp  = spf.newSAXParser();
        sp.parse( is, ( HandlerBase )null );
        is.close();
    }
}
--------------------------------------------------------------------------

$ java SAXParserXXE xxe_0.txt
Exception in thread "main" java.io.FileNotFoundException: http://127.0.0.1:8080/nonexist
        at sun.net.www.protocol.http.HttpURLConnection.getInputStream0(HttpURLConnection.java:1896)
        at sun.net.www.protocol.http.HttpURLConnection.getInputStream(HttpURLConnection.java:1498)
        at com.sun.org.apache.xerces.internal.impl.XMLEntityManager.setupCurrentEntity(XMLEntityManager.java:647)
        at com.sun.org.apache.xerces.internal.impl.XMLEntityManager.startEntity(XMLEntityManager.java:1304)
        at com.sun.org.apache.xerces.internal.impl.XMLEntityManager.startEntity(XMLEntityManager.java:1240)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanEntityReference(XMLDocumentFragmentScannerImpl.java:1908)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl$FragmentContentDriver.next(XMLDocumentFragmentScannerImpl.java:3061)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl.next(XMLDocumentScannerImpl.java:602)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanDocument(XMLDocumentFragmentScannerImpl.java:505)
        at com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse(XML11Configuration.java:842)
        at com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse(XML11Configuration.java:771)
        at com.sun.org.apache.xerces.internal.parsers.XMLParser.parse(XMLParser.java:141)
        at com.sun.org.apache.xerces.internal.parsers.AbstractSAXParser.parse(AbstractSAXParser.java:1213)
        at com.sun.org.apache.xerces.internal.jaxp.SAXParserImpl$JAXPSAXParser.parse(SAXParserImpl.java:643)
        at com.sun.org.apache.xerces.internal.jaxp.SAXParserImpl.parse(SAXParserImpl.java:342)
        at javax.xml.parsers.SAXParser.parse(SAXParser.java:139)
        at SAXParserXXE.main(SAXParserXXE.java:16)

HTTP Server收到GET请求，返回404，导致客户端抛出异常。

6) SAXParserNoXXE.java

--------------------------------------------------------------------------
/*
 * javac -encoding GBK -g -Xlint:deprecation SAXParserNoXXE.java
 */
import java.io.*;
import javax.xml.parsers.*;
import org.xml.sax.*;

public class SAXParserNoXXE
{
    private static SAXParserFactory GetSAXParserFactory ()
        throws SAXNotRecognizedException, SAXNotSupportedException, ParserConfigurationException
    {
        SAXParserFactory    spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware( true );
        spf.setFeature( "http://apache.org/xml/features/disallow-doctype-decl", true );             // 禁用DTD
        spf.setFeature( "http://xml.org/sax/features/external-general-entities", false );           // 禁用外部通用实体
        spf.setFeature( "http://xml.org/sax/features/external-parameter-entities", false );         // 禁用外部参数实体
        spf.setFeature( "http://apache.org/xml/features/nonvalidating/load-external-dtd", false );  // 禁用外部DTD
        spf.setXIncludeAware( false );
        return spf;
    }

    public static void main ( String[] argv ) throws Exception
    {
        File                f   = new File( argv[0] );
        InputStream         is  = new FileInputStream( f );
        /*
         * 原来是"SAXParserFactory.newInstance()"
         */
        SAXParserFactory    spf = GetSAXParserFactory();
        SAXParser           sp  = spf.newSAXParser();
        sp.parse( is, ( HandlerBase )null );
        is.close();
    }
}
--------------------------------------------------------------------------

$ java SAXParserNoXXE xxe_0.txt
[Fatal Error] :1:10: DOCTYPE is disallowed when the feature "http://apache.org/xml/features/disallow-doctype-decl" set to true.
Exception in thread "main" org.xml.sax.SAXParseException; lineNumber: 1; columnNumber: 10; DOCTYPE is disallowed when the feature "http://apache.org/xml/features/disallow-doctype-decl" set to true.
        at com.sun.org.apache.xerces.internal.parsers.AbstractSAXParser.parse(AbstractSAXParser.java:1239)
        at com.sun.org.apache.xerces.internal.jaxp.SAXParserImpl$JAXPSAXParser.parse(SAXParserImpl.java:643)
        at com.sun.org.apache.xerces.internal.jaxp.SAXParserImpl.parse(SAXParserImpl.java:342)
        at javax.xml.parsers.SAXParser.parse(SAXParser.java:139)
        at SAXParserNoXXE.main(SAXParserNoXXE.java:32)

HTTP Server不会收到GET请求，sp.parse()时直接抛出异常。

$ java -agentlib:jdwp=transport=dt_socket,address=127.0.0.1:8005,server=y,suspend=y SAXParserNoXXE xxe_0.txt
$ jdb -connect com.sun.jdi.SocketAttach:hostname=127.0.0.1,port=8005

catch *.XMLParseException

  [1] com.sun.org.apache.xerces.internal.util.DefaultErrorHandler.fatalError (DefaultErrorHandler.java:85), pc = 8
  [2] com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError (XMLErrorReporter.java:400), pc = 270
  [3] com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError (XMLErrorReporter.java:327), pc = 9
  [4] com.sun.org.apache.xerces.internal.impl.XMLScanner.reportFatalError (XMLScanner.java:1,472), pc = 13
  [5] com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl$PrologDriver.next (XMLDocumentScannerImpl.java:914), pc = 570
  [6] com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl.next (XMLDocumentScannerImpl.java:602), pc = 4
  [7] com.sun.org.apache.xerces.internal.impl.XMLNSDocumentScannerImpl.next (XMLNSDocumentScannerImpl.java:112), pc = 31
  [8] com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanDocument (XMLDocumentFragmentScannerImpl.java:505), pc = 308
  [9] com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse (XML11Configuration.java:842), pc = 123
  [10] com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse (XML11Configuration.java:771), pc = 29
  [11] com.sun.org.apache.xerces.internal.parsers.XMLParser.parse (XMLParser.java:141), pc = 76
  [12] com.sun.org.apache.xerces.internal.parsers.AbstractSAXParser.parse (AbstractSAXParser.java:1,213), pc = 43
  [13] com.sun.org.apache.xerces.internal.jaxp.SAXParserImpl$JAXPSAXParser.parse (SAXParserImpl.java:643), pc = 53
  [14] com.sun.org.apache.xerces.internal.jaxp.SAXParserImpl.parse (SAXParserImpl.java:342), pc = 61
  [15] javax.xml.parsers.SAXParser.parse (SAXParser.java:139), pc = 26
  [16] SAXParserNoXXE.main (SAXParserNoXXE.java:32), pc = 37

7) SAXTransformerFactoryXXE.java

--------------------------------------------------------------------------
/*
 * javac -encoding GBK -g SAXTransformerFactoryXXE.java
 */
import java.io.*;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamSource;

public class SAXTransformerFactoryXXE
{
    public static void main ( String[] argv ) throws Exception
    {
        File                    f   = new File( argv[0] );
        InputStream             is  = new FileInputStream( f );
        SAXTransformerFactory   sf  = ( SAXTransformerFactory )SAXTransformerFactory.newInstance();
        StreamSource            ss  = new StreamSource( is );
        sf.newTransformerHandler( ss );
        is.close();
    }
}
--------------------------------------------------------------------------

$ java SAXTransformerFactoryXXE xxe_0.txt
ERROR:  'Could not compile stylesheet'
FATAL ERROR:  'http://127.0.0.1:8080/nonexist'
           :http://127.0.0.1:8080/nonexist
Exception in thread "main" javax.xml.transform.TransformerConfigurationException: http://127.0.0.1:8080/nonexist
        at com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl.newTemplates(TransformerFactoryImpl.java:988)
        at com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl.newTransformer(TransformerFactoryImpl.java:761)
        at com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl.newTransformerHandler(TransformerFactoryImpl.java:1068)
        at SAXTransformerFactoryXXE.main(SAXTransformerFactoryXXE.java:16)
Caused by: java.io.FileNotFoundException: http://127.0.0.1:8080/nonexist
        at sun.net.www.protocol.http.HttpURLConnection.getInputStream0(HttpURLConnection.java:1896)
        at sun.net.www.protocol.http.HttpURLConnection.getInputStream(HttpURLConnection.java:1498)
        at com.sun.org.apache.xerces.internal.impl.XMLEntityManager.setupCurrentEntity(XMLEntityManager.java:647)
        at com.sun.org.apache.xerces.internal.impl.XMLEntityManager.startEntity(XMLEntityManager.java:1304)
        at com.sun.org.apache.xerces.internal.impl.XMLEntityManager.startEntity(XMLEntityManager.java:1240)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanEntityReference(XMLDocumentFragmentScannerImpl.java:1908)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl$FragmentContentDriver.next(XMLDocumentFragmentScannerImpl.java:3061)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl.next(XMLDocumentScannerImpl.java:602)
        at com.sun.org.apache.xerces.internal.impl.XMLNSDocumentScannerImpl.next(XMLNSDocumentScannerImpl.java:112)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanDocument(XMLDocumentFragmentScannerImpl.java:505)
        at com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse(XML11Configuration.java:842)
        at com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse(XML11Configuration.java:771)
        at com.sun.org.apache.xerces.internal.parsers.XMLParser.parse(XMLParser.java:141)
        at com.sun.org.apache.xerces.internal.parsers.AbstractSAXParser.parse(AbstractSAXParser.java:1213)
        at com.sun.org.apache.xerces.internal.jaxp.SAXParserImpl$JAXPSAXParser.parse(SAXParserImpl.java:643)
        at com.sun.org.apache.xalan.internal.xsltc.compiler.Parser.parse(Parser.java:424)
        at com.sun.org.apache.xalan.internal.xsltc.compiler.Parser.parse(Parser.java:479)
        at com.sun.org.apache.xalan.internal.xsltc.compiler.XSLTC.compile(XSLTC.java:452)
        at com.sun.org.apache.xalan.internal.xsltc.compiler.XSLTC.compile(XSLTC.java:554)
        at com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl.newTemplates(TransformerFactoryImpl.java:947)
        ... 3 more
---------
java.io.FileNotFoundException: http://127.0.0.1:8080/nonexist
        at sun.net.www.protocol.http.HttpURLConnection.getInputStream0(HttpURLConnection.java:1896)
        at sun.net.www.protocol.http.HttpURLConnection.getInputStream(HttpURLConnection.java:1498)
        at com.sun.org.apache.xerces.internal.impl.XMLEntityManager.setupCurrentEntity(XMLEntityManager.java:647)
        at com.sun.org.apache.xerces.internal.impl.XMLEntityManager.startEntity(XMLEntityManager.java:1304)
        at com.sun.org.apache.xerces.internal.impl.XMLEntityManager.startEntity(XMLEntityManager.java:1240)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanEntityReference(XMLDocumentFragmentScannerImpl.java:1908)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl$FragmentContentDriver.next(XMLDocumentFragmentScannerImpl.java:3061)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl.next(XMLDocumentScannerImpl.java:602)
        at com.sun.org.apache.xerces.internal.impl.XMLNSDocumentScannerImpl.next(XMLNSDocumentScannerImpl.java:112)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanDocument(XMLDocumentFragmentScannerImpl.java:505)
        at com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse(XML11Configuration.java:842)
        at com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse(XML11Configuration.java:771)
        at com.sun.org.apache.xerces.internal.parsers.XMLParser.parse(XMLParser.java:141)
        at com.sun.org.apache.xerces.internal.parsers.AbstractSAXParser.parse(AbstractSAXParser.java:1213)
        at com.sun.org.apache.xerces.internal.jaxp.SAXParserImpl$JAXPSAXParser.parse(SAXParserImpl.java:643)
        at com.sun.org.apache.xalan.internal.xsltc.compiler.Parser.parse(Parser.java:424)
        at com.sun.org.apache.xalan.internal.xsltc.compiler.Parser.parse(Parser.java:479)
        at com.sun.org.apache.xalan.internal.xsltc.compiler.XSLTC.compile(XSLTC.java:452)
        at com.sun.org.apache.xalan.internal.xsltc.compiler.XSLTC.compile(XSLTC.java:554)
        at com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl.newTemplates(TransformerFactoryImpl.java:947)
        at com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl.newTransformer(TransformerFactoryImpl.java:761)
        at com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl.newTransformerHandler(TransformerFactoryImpl.java:1068)
        at SAXTransformerFactoryXXE.main(SAXTransformerFactoryXXE.java:16)

HTTP Server收到GET请求，返回404，导致客户端抛出异常。

8) SAXTransformerFactoryNoXXE.java

--------------------------------------------------------------------------
/*
 * javac -encoding GBK -g SAXTransformerFactoryNoXXE.java
 */
import java.io.*;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.XMLConstants;

public class SAXTransformerFactoryNoXXE
{
    private static SAXTransformerFactory GetSAXTransformerFactory ()
    {
        SAXTransformerFactory   sf  = ( SAXTransformerFactory )SAXTransformerFactory.newInstance();
        sf.setAttribute( XMLConstants.ACCESS_EXTERNAL_DTD, "" );
        sf.setAttribute( XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "" );
        return sf;
    }

    public static void main ( String[] argv ) throws Exception
    {
        File                    f   = new File( argv[0] );
        InputStream             is  = new FileInputStream( f );
        /*
         * 原来是"( SAXTransformerFactory )SAXTransformerFactory.newInstance()"
         */
        SAXTransformerFactory   sf  = GetSAXTransformerFactory();
        StreamSource            ss  = new StreamSource( is );
        sf.newTransformerHandler( ss );
        is.close();
    }
}
--------------------------------------------------------------------------

$ java SAXTransformerFactoryNoXXE xxe_0.txt
[Fatal Error] :5:12: External Entity: Failed to read external document 'nonexist', because 'http' access is not allowed due to restriction set by the accessExternalDTD property.
ERROR:  'Could not compile stylesheet'
FATAL ERROR:  'External Entity: Failed to read external document 'nonexist', because 'http' access is not allowed due to restriction set by the accessExternalDTD property.'
           :External Entity: Failed to read external document 'nonexist', because 'http' access is not allowed due to restriction set by the accessExternalDTD property.
Exception in thread "main" javax.xml.transform.TransformerConfigurationException: External Entity: Failed to read external document 'nonexist', because 'http' access is not allowed due to restriction set by the accessExternalDTD property.
        at com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl.newTemplates(TransformerFactoryImpl.java:988)
        at com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl.newTransformer(TransformerFactoryImpl.java:761)
        at com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl.newTransformerHandler(TransformerFactoryImpl.java:1068)
        at SAXTransformerFactoryNoXXE.main(SAXTransformerFactoryNoXXE.java:28)
Caused by: org.xml.sax.SAXParseException; lineNumber: 5; columnNumber: 12; External Entity: Failed to read external document 'nonexist', because 'http' access is not allowed due to restriction set by the accessExternalDTD property.
        at com.sun.org.apache.xerces.internal.parsers.AbstractSAXParser.parse(AbstractSAXParser.java:1239)
        at com.sun.org.apache.xerces.internal.jaxp.SAXParserImpl$JAXPSAXParser.parse(SAXParserImpl.java:643)
        at com.sun.org.apache.xalan.internal.xsltc.compiler.Parser.parse(Parser.java:424)
        at com.sun.org.apache.xalan.internal.xsltc.compiler.Parser.parse(Parser.java:479)
        at com.sun.org.apache.xalan.internal.xsltc.compiler.XSLTC.compile(XSLTC.java:452)
        at com.sun.org.apache.xalan.internal.xsltc.compiler.XSLTC.compile(XSLTC.java:554)
        at com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl.newTemplates(TransformerFactoryImpl.java:947)
        ... 3 more
---------
org.xml.sax.SAXParseException; lineNumber: 5; columnNumber: 12; External Entity: Failed to read external document 'nonexist', because 'http' access is not allowed due to restriction set by the accessExternalDTD property.
        at com.sun.org.apache.xerces.internal.parsers.AbstractSAXParser.parse(AbstractSAXParser.java:1239)
        at com.sun.org.apache.xerces.internal.jaxp.SAXParserImpl$JAXPSAXParser.parse(SAXParserImpl.java:643)
        at com.sun.org.apache.xalan.internal.xsltc.compiler.Parser.parse(Parser.java:424)
        at com.sun.org.apache.xalan.internal.xsltc.compiler.Parser.parse(Parser.java:479)
        at com.sun.org.apache.xalan.internal.xsltc.compiler.XSLTC.compile(XSLTC.java:452)
        at com.sun.org.apache.xalan.internal.xsltc.compiler.XSLTC.compile(XSLTC.java:554)
        at com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl.newTemplates(TransformerFactoryImpl.java:947)
        at com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl.newTransformer(TransformerFactoryImpl.java:761)
        at com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl.newTransformerHandler(TransformerFactoryImpl.java:1068)
        at SAXTransformerFactoryNoXXE.main(SAXTransformerFactoryNoXXE.java:28)

HTTP Server不会收到GET请求，sf.newTransformerHandler()时直接抛出异常。

$ java -agentlib:jdwp=transport=dt_socket,address=127.0.0.1:8005,server=y,suspend=y SAXTransformerFactoryNoXXE xxe_0.txt
$ jdb -connect com.sun.jdi.SocketAttach:hostname=127.0.0.1,port=8005

catch *.XMLParseException

  [1] com.sun.org.apache.xerces.internal.util.DefaultErrorHandler.fatalError (DefaultErrorHandler.java:85), pc = 8
  [2] com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError (XMLErrorReporter.java:400), pc = 270
  [3] com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError (XMLErrorReporter.java:327), pc = 9
  [4] com.sun.org.apache.xerces.internal.impl.XMLEntityManager.startEntity (XMLEntityManager.java:1,224), pc = 854
  [5] com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanEntityReference (XMLDocumentFragmentScannerImpl.java:1,908), pc = 373
  [6] com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl$FragmentContentDriver.next (XMLDocumentFragmentScannerImpl.java:3,061), pc = 1,813
  [7] com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl.next (XMLDocumentScannerImpl.java:602), pc = 4
  [8] com.sun.org.apache.xerces.internal.impl.XMLNSDocumentScannerImpl.next (XMLNSDocumentScannerImpl.java:112), pc = 31
  [9] com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanDocument (XMLDocumentFragmentScannerImpl.java:505), pc = 308
  [10] com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse (XML11Configuration.java:842), pc = 123
  [11] com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse (XML11Configuration.java:771), pc = 29
  [12] com.sun.org.apache.xerces.internal.parsers.XMLParser.parse (XMLParser.java:141), pc = 76
  [13] com.sun.org.apache.xerces.internal.parsers.AbstractSAXParser.parse (AbstractSAXParser.java:1,213), pc = 43
  [14] com.sun.org.apache.xerces.internal.jaxp.SAXParserImpl$JAXPSAXParser.parse (SAXParserImpl.java:643), pc = 53
  [15] com.sun.org.apache.xalan.internal.xsltc.compiler.Parser.parse (Parser.java:424), pc = 9
  [16] com.sun.org.apache.xalan.internal.xsltc.compiler.Parser.parse (Parser.java:479), pc = 143
  [17] com.sun.org.apache.xalan.internal.xsltc.compiler.XSLTC.compile (XSLTC.java:452), pc = 93
  [18] com.sun.org.apache.xalan.internal.xsltc.compiler.XSLTC.compile (XSLTC.java:554), pc = 8
  [19] com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl.newTemplates (TransformerFactoryImpl.java:947), pc = 622
  [20] com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl.newTransformer (TransformerFactoryImpl.java:761), pc = 2
  [21] com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl.newTransformerHandler (TransformerFactoryImpl.java:1,068), pc = 2
  [22] SAXTransformerFactoryNoXXE.main (SAXTransformerFactoryNoXXE.java:28), pc = 37

9) SAXReaderXXE.java

https://dom4j.github.io/

--------------------------------------------------------------------------
/*
 * javac -encoding GBK -g -cp dom4j-2.1.1.jar SAXReaderXXE.java
 */
import java.io.*;
import org.dom4j.io.SAXReader;

public class SAXReaderXXE
{
    public static void main ( String[] argv ) throws Exception
    {
        File        f   = new File( argv[0] );
        InputStream is  = new FileInputStream( f );
        SAXReader   sr  = new SAXReader();
        sr.read( is );
        is.close();
    }
}
--------------------------------------------------------------------------

$ java -cp "dom4j-2.1.1.jar:." SAXReaderXXE xxe_0.txt
Exception in thread "main" org.dom4j.DocumentException: http://127.0.0.1:8080/nonexist
        at org.dom4j.io.SAXReader.read(SAXReader.java:464)
        at org.dom4j.io.SAXReader.read(SAXReader.java:325)
        at SAXReaderXXE.main(SAXReaderXXE.java:14)
Caused by: java.io.FileNotFoundException: http://127.0.0.1:8080/nonexist
        at sun.net.www.protocol.http.HttpURLConnection.getInputStream0(HttpURLConnection.java:1896)
        at sun.net.www.protocol.http.HttpURLConnection.getInputStream(HttpURLConnection.java:1498)
        at com.sun.org.apache.xerces.internal.impl.XMLEntityManager.setupCurrentEntity(XMLEntityManager.java:647)
        at com.sun.org.apache.xerces.internal.impl.XMLEntityManager.startEntity(XMLEntityManager.java:1304)
        at com.sun.org.apache.xerces.internal.impl.XMLEntityManager.startEntity(XMLEntityManager.java:1240)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanEntityReference(XMLDocumentFragmentScannerImpl.java:1908)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl$FragmentContentDriver.next(XMLDocumentFragmentScannerImpl.java:3061)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl.next(XMLDocumentScannerImpl.java:602)
        at com.sun.org.apache.xerces.internal.impl.XMLNSDocumentScannerImpl.next(XMLNSDocumentScannerImpl.java:112)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanDocument(XMLDocumentFragmentScannerImpl.java:505)
        at com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse(XML11Configuration.java:842)
        at com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse(XML11Configuration.java:771)
        at com.sun.org.apache.xerces.internal.parsers.XMLParser.parse(XMLParser.java:141)
        at com.sun.org.apache.xerces.internal.parsers.AbstractSAXParser.parse(AbstractSAXParser.java:1213)
        at com.sun.org.apache.xerces.internal.jaxp.SAXParserImpl$JAXPSAXParser.parse(SAXParserImpl.java:643)
        at org.dom4j.io.SAXReader.read(SAXReader.java:445)
        ... 2 more

HTTP Server收到GET请求，返回404，导致客户端抛出异常。

10) SAXReaderNoXXE.java

--------------------------------------------------------------------------
/*
 * javac -encoding GBK -g -cp dom4j-2.1.1.jar SAXReaderNoXXE.java
 */
import java.io.*;
import org.xml.sax.SAXException;
import org.dom4j.io.SAXReader;

public class SAXReaderNoXXE
{
    private static SAXReader GetSAXReader () throws SAXException
    {
        SAXReader   sr  = new SAXReader();
        sr.setFeature( "http://apache.org/xml/features/disallow-doctype-decl", true );             // 禁用DTD
        sr.setFeature( "http://xml.org/sax/features/external-general-entities", false );           // 禁用外部通用实体
        sr.setFeature( "http://xml.org/sax/features/external-parameter-entities", false );         // 禁用外部参数实体
        sr.setFeature( "http://apache.org/xml/features/nonvalidating/load-external-dtd", false );  // 禁用外部DTD
        return sr;
    }

    public static void main ( String[] argv ) throws Exception
    {
        File        f   = new File( argv[0] );
        InputStream is  = new FileInputStream( f );
        /*
         * 原来是"new SAXReader()"
         */
        SAXReader   sr  = GetSAXReader();
        sr.read( is );
        is.close();
    }
}
--------------------------------------------------------------------------

$ java -cp "dom4j-2.1.1.jar:." SAXReaderNoXXE xxe_0.txt
Exception in thread "main" org.dom4j.DocumentException: Error on line 1 of document  : DOCTYPE is disallowed when the feature "http://apache.org/xml/features/disallow-doctype-decl" set to true.
        at org.dom4j.io.SAXReader.read(SAXReader.java:462)
        at org.dom4j.io.SAXReader.read(SAXReader.java:325)
        at SAXReaderNoXXE.main(SAXReaderNoXXE.java:28)
Caused by: org.xml.sax.SAXParseException; lineNumber: 1; columnNumber: 10; DOCTYPE is disallowed when the feature "http://apache.org/xml/features/disallow-doctype-decl" set to true.
        at com.sun.org.apache.xerces.internal.util.ErrorHandlerWrapper.createSAXParseException(ErrorHandlerWrapper.java:203)
        at com.sun.org.apache.xerces.internal.util.ErrorHandlerWrapper.fatalError(ErrorHandlerWrapper.java:177)
        at com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError(XMLErrorReporter.java:400)
        at com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError(XMLErrorReporter.java:327)
        at com.sun.org.apache.xerces.internal.impl.XMLScanner.reportFatalError(XMLScanner.java:1472)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl$PrologDriver.next(XMLDocumentScannerImpl.java:914)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl.next(XMLDocumentScannerImpl.java:602)
        at com.sun.org.apache.xerces.internal.impl.XMLNSDocumentScannerImpl.next(XMLNSDocumentScannerImpl.java:112)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanDocument(XMLDocumentFragmentScannerImpl.java:505)
        at com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse(XML11Configuration.java:842)
        at com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse(XML11Configuration.java:771)
        at com.sun.org.apache.xerces.internal.parsers.XMLParser.parse(XMLParser.java:141)
        at com.sun.org.apache.xerces.internal.parsers.AbstractSAXParser.parse(AbstractSAXParser.java:1213)
        at com.sun.org.apache.xerces.internal.jaxp.SAXParserImpl$JAXPSAXParser.parse(SAXParserImpl.java:643)
        at org.dom4j.io.SAXReader.read(SAXReader.java:445)
        ... 2 more

HTTP Server不会收到GET请求，sr.read()时直接抛出异常。

$ java -agentlib:jdwp=transport=dt_socket,address=127.0.0.1:8005,server=y,suspend=y -cp "dom4j-2.1.1.jar:." SAXReaderNoXXE xxe_0.txt
$ jdb -connect com.sun.jdi.SocketAttach:hostname=127.0.0.1,port=8005

catch *.XMLParseException

  [1] com.sun.org.apache.xerces.internal.util.ErrorHandlerWrapper.fatalError (ErrorHandlerWrapper.java:183), pc = 34
  [2] com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError (XMLErrorReporter.java:400), pc = 270
  [3] com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError (XMLErrorReporter.java:327), pc = 9
  [4] com.sun.org.apache.xerces.internal.impl.XMLScanner.reportFatalError (XMLScanner.java:1,472), pc = 13
  [5] com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl$PrologDriver.next (XMLDocumentScannerImpl.java:914), pc = 570
  [6] com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl.next (XMLDocumentScannerImpl.java:602), pc = 4
  [7] com.sun.org.apache.xerces.internal.impl.XMLNSDocumentScannerImpl.next (XMLNSDocumentScannerImpl.java:112), pc = 31
  [8] com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanDocument (XMLDocumentFragmentScannerImpl.java:505), pc = 308
  [9] com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse (XML11Configuration.java:842), pc = 123
  [10] com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse (XML11Configuration.java:771), pc = 29
  [11] com.sun.org.apache.xerces.internal.parsers.XMLParser.parse (XMLParser.java:141), pc = 76
  [12] com.sun.org.apache.xerces.internal.parsers.AbstractSAXParser.parse (AbstractSAXParser.java:1,213), pc = 43
  [13] com.sun.org.apache.xerces.internal.jaxp.SAXParserImpl$JAXPSAXParser.parse (SAXParserImpl.java:643), pc = 53
  [14] org.dom4j.io.SAXReader.read (SAXReader.java:445), pc = 130
  [15] org.dom4j.io.SAXReader.read (SAXReader.java:325), pc = 26
  [16] SAXReaderNoXXE.main (SAXReaderNoXXE.java:28), pc = 26

leadroyal提到dom4j低版本自带XXE:

https://www.leadroyal.cn/?p=562
https://github.com/dom4j/dom4j/issues/28

DocumentHelper.parseText()里直接用了SAXReader，未做保护。2.1.1版dom4j已修
补。

11) XMLReaderXXE.java

--------------------------------------------------------------------------
/*
 * javac -encoding GBK -g XMLReaderXXE.java
 */
import java.io.*;
import org.xml.sax.*;
import org.xml.sax.helpers.XMLReaderFactory;

public class XMLReaderXXE
{
    public static void main ( String[] argv ) throws Exception
    {
        File        f   = new File( argv[0] );
        InputStream is  = new FileInputStream( f );
        XMLReader   xr  = XMLReaderFactory.createXMLReader();
        xr.parse( new InputSource( is ) );
        is.close();
    }
}
--------------------------------------------------------------------------

$ java XMLReaderXXE xxe_0.txt
Exception in thread "main" java.io.FileNotFoundException: http://127.0.0.1:8080/nonexist
        at sun.net.www.protocol.http.HttpURLConnection.getInputStream0(HttpURLConnection.java:1896)
        at sun.net.www.protocol.http.HttpURLConnection.getInputStream(HttpURLConnection.java:1498)
        at com.sun.org.apache.xerces.internal.impl.XMLEntityManager.setupCurrentEntity(XMLEntityManager.java:647)
        at com.sun.org.apache.xerces.internal.impl.XMLEntityManager.startEntity(XMLEntityManager.java:1304)
        at com.sun.org.apache.xerces.internal.impl.XMLEntityManager.startEntity(XMLEntityManager.java:1240)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanEntityReference(XMLDocumentFragmentScannerImpl.java:1908)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl$FragmentContentDriver.next(XMLDocumentFragmentScannerImpl.java:3061)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl.next(XMLDocumentScannerImpl.java:602)
        at com.sun.org.apache.xerces.internal.impl.XMLNSDocumentScannerImpl.next(XMLNSDocumentScannerImpl.java:112)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanDocument(XMLDocumentFragmentScannerImpl.java:505)
        at com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse(XML11Configuration.java:842)
        at com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse(XML11Configuration.java:771)
        at com.sun.org.apache.xerces.internal.parsers.XMLParser.parse(XMLParser.java:141)
        at com.sun.org.apache.xerces.internal.parsers.AbstractSAXParser.parse(AbstractSAXParser.java:1213)
        at XMLReaderXXE.main(XMLReaderXXE.java:15)

HTTP Server收到GET请求，返回404，导致客户端抛出异常。

12) XMLReaderNoXXE.java

--------------------------------------------------------------------------
/*
 * javac -encoding GBK -g XMLReaderNoXXE.java
 */
import java.io.*;
import org.xml.sax.*;
import org.xml.sax.helpers.XMLReaderFactory;

public class XMLReaderNoXXE
{
    private static XMLReader GetXMLReader ()
        throws SAXNotRecognizedException, SAXNotSupportedException, SAXException
     {
        XMLReader   xr  = XMLReaderFactory.createXMLReader();
        xr.setFeature( "http://apache.org/xml/features/disallow-doctype-decl", true );             // 禁用DTD
        xr.setFeature( "http://xml.org/sax/features/external-general-entities", false );           // 禁用外部通用实体
        xr.setFeature( "http://xml.org/sax/features/external-parameter-entities", false );         // 禁用外部参数实体
        xr.setFeature( "http://apache.org/xml/features/nonvalidating/load-external-dtd", false );  // 禁用外部DTD
        return xr;
    }

    public static void main ( String[] argv ) throws Exception
    {
        File        f   = new File( argv[0] );
        InputStream is  = new FileInputStream( f );
        /*
         * 原来是"XMLReaderFactory.createXMLReader()"
         */
        XMLReader   xr  = GetXMLReader();
        xr.parse( new InputSource( is ) );
        is.close();
    }
}
--------------------------------------------------------------------------

$ java XMLReaderNoXXE xxe_0.txt
[Fatal Error] :1:10: DOCTYPE is disallowed when the feature "http://apache.org/xml/features/disallow-doctype-decl" set to true.
Exception in thread "main" org.xml.sax.SAXParseException; lineNumber: 1; columnNumber: 10; DOCTYPE is disallowed when the feature "http://apache.org/xml/features/disallow-doctype-decl" set to true.
        at com.sun.org.apache.xerces.internal.parsers.AbstractSAXParser.parse(AbstractSAXParser.java:1239)
        at XMLReaderNoXXE.main(XMLReaderNoXXE.java:29)

HTTP Server不会收到GET请求，xr.parse()时直接抛出异常。

$ java -agentlib:jdwp=transport=dt_socket,address=127.0.0.1:8005,server=y,suspend=y XMLReaderNoXXE xxe_0.txt
$ jdb -connect com.sun.jdi.SocketAttach:hostname=127.0.0.1,port=8005

catch *.XMLParseException

  [1] com.sun.org.apache.xerces.internal.util.DefaultErrorHandler.fatalError (DefaultErrorHandler.java:85), pc = 8
  [2] com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError (XMLErrorReporter.java:400), pc = 270
  [3] com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError (XMLErrorReporter.java:327), pc = 9
  [4] com.sun.org.apache.xerces.internal.impl.XMLScanner.reportFatalError (XMLScanner.java:1,472), pc = 13
  [5] com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl$PrologDriver.next (XMLDocumentScannerImpl.java:914), pc = 570
  [6] com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl.next (XMLDocumentScannerImpl.java:602), pc = 4
  [7] com.sun.org.apache.xerces.internal.impl.XMLNSDocumentScannerImpl.next (XMLNSDocumentScannerImpl.java:112), pc = 31
  [8] com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanDocument (XMLDocumentFragmentScannerImpl.java:505), pc = 308
  [9] com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse (XML11Configuration.java:842), pc = 123
  [10] com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse (XML11Configuration.java:771), pc = 29
  [11] com.sun.org.apache.xerces.internal.parsers.XMLParser.parse (XMLParser.java:141), pc = 76
  [12] com.sun.org.apache.xerces.internal.parsers.AbstractSAXParser.parse (AbstractSAXParser.java:1,213), pc = 43
  [13] XMLReaderNoXXE.main (XMLReaderNoXXE.java:29), pc = 33

13) SchemaFactoryXXE.java

--------------------------------------------------------------------------
/*
 * javac -encoding GBK -g SchemaFactoryXXE.java
 */
import java.io.*;
import javax.xml.validation.*;
import javax.xml.transform.stream.StreamSource;

public class SchemaFactoryXXE
{
    public static void main ( String[] argv ) throws Exception
    {
        File            f   = new File( argv[0] );
        InputStream     is  = new FileInputStream( f );
        SchemaFactory   sf  = SchemaFactory.newInstance( "http://www.w3.org/2001/XMLSchema" );
        StreamSource    ss  = new StreamSource( is );
        Schema          s   = sf.newSchema( ss );
        is.close();
    }
}
--------------------------------------------------------------------------

$ java SchemaFactoryXXE xxe_0.txt
Exception in thread "main" org.xml.sax.SAXParseException; schema_reference.4: Failed to read schema document 'null', because 1) could not find the document; 2) the document could not be read; 3) the root element of the document is not <xsd:schema>.
        at com.sun.org.apache.xerces.internal.util.ErrorHandlerWrapper.createSAXParseException(ErrorHandlerWrapper.java:203)
        at com.sun.org.apache.xerces.internal.util.ErrorHandlerWrapper.error(ErrorHandlerWrapper.java:134)
        at com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError(XMLErrorReporter.java:396)
        at com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError(XMLErrorReporter.java:306)
        at com.sun.org.apache.xerces.internal.impl.xs.traversers.XSDHandler.reportSchemaErr(XSDHandler.java:4158)
        at com.sun.org.apache.xerces.internal.impl.xs.traversers.XSDHandler.reportSchemaError(XSDHandler.java:4141)
        at com.sun.org.apache.xerces.internal.impl.xs.traversers.XSDHandler.getSchemaDocument1(XSDHandler.java:2480)
        at com.sun.org.apache.xerces.internal.impl.xs.traversers.XSDHandler.getSchemaDocument(XSDHandler.java:2193)
        at com.sun.org.apache.xerces.internal.impl.xs.traversers.XSDHandler.parseSchema(XSDHandler.java:578)
        at com.sun.org.apache.xerces.internal.impl.xs.XMLSchemaLoader.loadSchema(XMLSchemaLoader.java:610)
        at com.sun.org.apache.xerces.internal.impl.xs.XMLSchemaLoader.loadGrammar(XMLSchemaLoader.java:569)
        at com.sun.org.apache.xerces.internal.impl.xs.XMLSchemaLoader.loadGrammar(XMLSchemaLoader.java:535)
        at com.sun.org.apache.xerces.internal.jaxp.validation.XMLSchemaFactory.newSchema(XMLSchemaFactory.java:254)
        at javax.xml.validation.SchemaFactory.newSchema(SchemaFactory.java:638)
        at SchemaFactoryXXE.main(SchemaFactoryXXE.java:16)
Caused by: java.io.FileNotFoundException: http://127.0.0.1:8080/nonexist
        at sun.net.www.protocol.http.HttpURLConnection.getInputStream0(HttpURLConnection.java:1896)
        at sun.net.www.protocol.http.HttpURLConnection.getInputStream(HttpURLConnection.java:1498)
        at com.sun.org.apache.xerces.internal.impl.XMLEntityManager.setupCurrentEntity(XMLEntityManager.java:647)
        at com.sun.org.apache.xerces.internal.impl.XMLEntityManager.startEntity(XMLEntityManager.java:1304)
        at com.sun.org.apache.xerces.internal.impl.XMLEntityManager.startEntity(XMLEntityManager.java:1240)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanEntityReference(XMLDocumentFragmentScannerImpl.java:1908)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl$FragmentContentDriver.next(XMLDocumentFragmentScannerImpl.java:3061)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl.next(XMLDocumentScannerImpl.java:602)
        at com.sun.org.apache.xerces.internal.impl.XMLNSDocumentScannerImpl.next(XMLNSDocumentScannerImpl.java:112)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanDocument(XMLDocumentFragmentScannerImpl.java:505)
        at com.sun.org.apache.xerces.internal.impl.xs.opti.SchemaParsingConfig.parse(SchemaParsingConfig.java:630)
        at com.sun.org.apache.xerces.internal.impl.xs.opti.SchemaParsingConfig.parse(SchemaParsingConfig.java:686)
        at com.sun.org.apache.xerces.internal.impl.xs.opti.SchemaDOMParser.parse(SchemaDOMParser.java:530)
        at com.sun.org.apache.xerces.internal.impl.xs.traversers.XSDHandler.getSchemaDocument(XSDHandler.java:2181)
        ... 7 more

HTTP Server收到GET请求，返回404，导致客户端抛出异常。

14) SchemaFactoryNoXXE.java

--------------------------------------------------------------------------
/*
 * javac -encoding GBK -g SchemaFactoryNoXXE.java
 */
import java.io.*;
import org.xml.sax.*;
import javax.xml.validation.*;
import javax.xml.transform.stream.StreamSource;
import javax.xml.XMLConstants;

public class SchemaFactoryNoXXE
{
    private static SchemaFactory GetSchemaFactory ()
        throws SAXNotRecognizedException, SAXNotSupportedException
    {
        SchemaFactory   sf  = SchemaFactory.newInstance( "http://www.w3.org/2001/XMLSchema" );
        /*
         * 不是setAttribute()
         */
        sf.setProperty( XMLConstants.ACCESS_EXTERNAL_DTD, "" );
        /*
         * 不是ACCESS_EXTERNAL_STYLESHEET
         */
        sf.setProperty( XMLConstants.ACCESS_EXTERNAL_SCHEMA, "" );
        return sf;
    }

    public static void main ( String[] argv ) throws Exception
    {
        File            f   = new File( argv[0] );
        InputStream     is  = new FileInputStream( f );
        /*
         * 原来是"SchemaFactory.newInstance( "http://www.w3.org/2001/XMLSchema" )"
         */
        SchemaFactory   sf  = GetSchemaFactory();
        StreamSource    ss  = new StreamSource( is );
        Schema          s   = sf.newSchema( ss );
        is.close();
    }
}
--------------------------------------------------------------------------

$ java SchemaFactoryNoXXE xxe_0.txt
Exception in thread "main" org.xml.sax.SAXParseException; lineNumber: 5; columnNumber: 12; External Entity: Failed to read external document 'nonexist', because 'http' access is not allowed due to restriction set by the accessExternalDTD property.
        at com.sun.org.apache.xerces.internal.util.ErrorHandlerWrapper.createSAXParseException(ErrorHandlerWrapper.java:203)
        at com.sun.org.apache.xerces.internal.util.ErrorHandlerWrapper.fatalError(ErrorHandlerWrapper.java:177)
        at com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError(XMLErrorReporter.java:400)
        at com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError(XMLErrorReporter.java:327)
        at com.sun.org.apache.xerces.internal.impl.XMLEntityManager.startEntity(XMLEntityManager.java:1224)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanEntityReference(XMLDocumentFragmentScannerImpl.java:1908)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl$FragmentContentDriver.next(XMLDocumentFragmentScannerImpl.java:3061)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl.next(XMLDocumentScannerImpl.java:602)
        at com.sun.org.apache.xerces.internal.impl.XMLNSDocumentScannerImpl.next(XMLNSDocumentScannerImpl.java:112)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanDocument(XMLDocumentFragmentScannerImpl.java:505)
        at com.sun.org.apache.xerces.internal.impl.xs.opti.SchemaParsingConfig.parse(SchemaParsingConfig.java:630)
        at com.sun.org.apache.xerces.internal.impl.xs.opti.SchemaParsingConfig.parse(SchemaParsingConfig.java:686)
        at com.sun.org.apache.xerces.internal.impl.xs.opti.SchemaDOMParser.parse(SchemaDOMParser.java:530)
        at com.sun.org.apache.xerces.internal.impl.xs.traversers.XSDHandler.getSchemaDocument(XSDHandler.java:2181)
        at com.sun.org.apache.xerces.internal.impl.xs.traversers.XSDHandler.parseSchema(XSDHandler.java:578)
        at com.sun.org.apache.xerces.internal.impl.xs.XMLSchemaLoader.loadSchema(XMLSchemaLoader.java:610)
        at com.sun.org.apache.xerces.internal.impl.xs.XMLSchemaLoader.loadGrammar(XMLSchemaLoader.java:569)
        at com.sun.org.apache.xerces.internal.impl.xs.XMLSchemaLoader.loadGrammar(XMLSchemaLoader.java:535)
        at com.sun.org.apache.xerces.internal.jaxp.validation.XMLSchemaFactory.newSchema(XMLSchemaFactory.java:254)
        at javax.xml.validation.SchemaFactory.newSchema(SchemaFactory.java:638)
        at SchemaFactoryNoXXE.main(SchemaFactoryNoXXE.java:36)

HTTP Server不会收到GET请求，sf.newSchema()时直接抛出异常。

$ java -agentlib:jdwp=transport=dt_socket,address=127.0.0.1:8005,server=y,suspend=y SchemaFactoryNoXXE xxe_0.txt
$ jdb -connect com.sun.jdi.SocketAttach:hostname=127.0.0.1,port=8005

catch *.XMLParseException

  [1] com.sun.org.apache.xerces.internal.util.ErrorHandlerWrapper.fatalError (ErrorHandlerWrapper.java:183), pc = 34
  [2] com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError (XMLErrorReporter.java:400), pc = 270
  [3] com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError (XMLErrorReporter.java:327), pc = 9
  [4] com.sun.org.apache.xerces.internal.impl.XMLEntityManager.startEntity (XMLEntityManager.java:1,224), pc = 854
  [5] com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanEntityReference (XMLDocumentFragmentScannerImpl.java:1,908), pc = 373
  [6] com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl$FragmentContentDriver.next (XMLDocumentFragmentScannerImpl.java:3,061), pc = 1,813
  [7] com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl.next (XMLDocumentScannerImpl.java:602), pc = 4
  [8] com.sun.org.apache.xerces.internal.impl.XMLNSDocumentScannerImpl.next (XMLNSDocumentScannerImpl.java:112), pc = 31
  [9] com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanDocument (XMLDocumentFragmentScannerImpl.java:505), pc = 308
  [10] com.sun.org.apache.xerces.internal.impl.xs.opti.SchemaParsingConfig.parse (SchemaParsingConfig.java:630), pc = 128
  [11] com.sun.org.apache.xerces.internal.impl.xs.opti.SchemaParsingConfig.parse (SchemaParsingConfig.java:686), pc = 29
  [12] com.sun.org.apache.xerces.internal.impl.xs.opti.SchemaDOMParser.parse (SchemaDOMParser.java:530), pc = 5
  [13] com.sun.org.apache.xerces.internal.impl.xs.traversers.XSDHandler.getSchemaDocument (XSDHandler.java:2,181), pc = 169
  [14] com.sun.org.apache.xerces.internal.impl.xs.traversers.XSDHandler.parseSchema (XSDHandler.java:578), pc = 306
  [15] com.sun.org.apache.xerces.internal.impl.xs.XMLSchemaLoader.loadSchema (XMLSchemaLoader.java:610), pc = 85
  [16] com.sun.org.apache.xerces.internal.impl.xs.XMLSchemaLoader.loadGrammar (XMLSchemaLoader.java:569), pc = 70
  [17] com.sun.org.apache.xerces.internal.impl.xs.XMLSchemaLoader.loadGrammar (XMLSchemaLoader.java:535), pc = 14
  [18] com.sun.org.apache.xerces.internal.jaxp.validation.XMLSchemaFactory.newSchema (XMLSchemaFactory.java:254), pc = 368
  [19] javax.xml.validation.SchemaFactory.newSchema (SchemaFactory.java:638), pc = 9
  [20] SchemaFactoryNoXXE.main (SchemaFactoryNoXXE.java:36), pc = 37

15) XMLStreamReaderXXE.java

--------------------------------------------------------------------------
/*
 * javac -encoding GBK -g XMLStreamReaderXXE.java
 */
import java.io.*;
import javax.xml.stream.*;

public class XMLStreamReaderXXE
{
    public static void main ( String[] argv ) throws Exception
    {
        File            f   = new File( argv[0] );
        InputStream     is  = new FileInputStream( f );
        XMLInputFactory xf  = XMLInputFactory.newFactory();
        XMLStreamReader xr  = xf.createXMLStreamReader( is );
        while ( xr.hasNext() )
        {
            int event   = xr.next();
        }
        xr.close();
        is.close();
    }
}
--------------------------------------------------------------------------

$ java XMLStreamReaderXXE xxe_0.txt
Exception in thread "main" javax.xml.stream.XMLStreamException: ParseError at [row,col]:[5,12]
Message: http://127.0.0.1:8080/nonexist
        at com.sun.org.apache.xerces.internal.impl.XMLStreamReaderImpl.next(XMLStreamReaderImpl.java:599)
        at XMLStreamReaderXXE.main(XMLStreamReaderXXE.java:24)

HTTP Server收到GET请求，返回404，导致客户端抛出异常。

本例未能直接看到XXE相关的调用栈回溯，或者说未能看到HTTP请求相关的调用栈回
溯，需要动用调试手段查看。

$ java -agentlib:jdwp=transport=dt_socket,address=127.0.0.1:8005,server=y,suspend=y XMLStreamReaderXXE xxe_0.txt
$ jdb -connect com.sun.jdi.SocketAttach:hostname=127.0.0.1,port=8005

stop in sun.net.www.protocol.http.HttpURLConnection.getInputStream0

  [1] sun.net.www.protocol.http.HttpURLConnection.getInputStream0 (HttpURLConnection.java:1,896), pc = 1,746
  [2] sun.net.www.protocol.http.HttpURLConnection.getInputStream (HttpURLConnection.java:1,498), pc = 52
  [3] com.sun.org.apache.xerces.internal.impl.XMLEntityManager.setupCurrentEntity (XMLEntityManager.java:647), pc = 223
  [4] com.sun.org.apache.xerces.internal.impl.XMLEntityManager.startEntity (XMLEntityManager.java:1,304), pc = 8
  [5] com.sun.org.apache.xerces.internal.impl.XMLEntityManager.startEntity (XMLEntityManager.java:1,240), pc = 905
  [6] com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanEntityReference (XMLDocumentFragmentScannerImpl.java:1,908), pc = 373
  [7] com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl$FragmentContentDriver.next (XMLDocumentFragmentScannerImpl.java:3,061), pc = 1,813
  [8] com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl.next (XMLDocumentScannerImpl.java:602), pc = 4
  [9] com.sun.org.apache.xerces.internal.impl.XMLNSDocumentScannerImpl.next (XMLNSDocumentScannerImpl.java:112), pc = 31
  [10] com.sun.org.apache.xerces.internal.impl.XMLStreamReaderImpl.next (XMLStreamReaderImpl.java:553), pc = 40
  [11] XMLStreamReaderXXE.main (XMLStreamReaderXXE.java:24), pc = 43

16) XMLStreamReaderNoXXE.java

--------------------------------------------------------------------------
/*
 * javac -encoding GBK -g XMLStreamReaderNoXXE.java
 */
import java.io.*;
import javax.xml.stream.*;

public class XMLStreamReaderNoXXE
{
    private static XMLInputFactory GetXMLInputFactory ()
    {
        XMLInputFactory xf  = XMLInputFactory.newFactory();
        xf.setProperty( XMLInputFactory.SUPPORT_DTD, false );
        xf.setProperty( XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false );
        return xf;
    }

    public static void main ( String[] argv ) throws Exception
    {
        File            f   = new File( argv[0] );
        InputStream     is  = new FileInputStream( f );
        /*
         * 原来是"XMLInputFactory.newFactory()"
         */
        XMLInputFactory xf  = GetXMLInputFactory();
        XMLStreamReader xr  = xf.createXMLStreamReader( is );
        while ( xr.hasNext() )
        {
            int event   = xr.next();
        }
        xr.close();
        is.close();
    }
}
--------------------------------------------------------------------------

$ java XMLStreamReaderNoXXE xxe_0.txt
Exception in thread "main" javax.xml.stream.XMLStreamException: ParseError at [row,col]:[5,12]
Message: The entity "some" was referenced, but not declared.
        at com.sun.org.apache.xerces.internal.impl.XMLStreamReaderImpl.next(XMLStreamReaderImpl.java:604)
        at XMLStreamReaderNoXXE.main(XMLStreamReaderNoXXE.java:28)

HTTP Server不会收到GET请求，xr.next()时直接抛出异常。由于DTD被禁用，提示:

The entity "some" was referenced, but not declared.

本例未能直接看到XXE相关的调用栈回溯，需要动用调试手段查看。

$ java -agentlib:jdwp=transport=dt_socket,address=127.0.0.1:8005,server=y,suspend=y XMLStreamReaderNoXXE xxe_0.txt
$ jdb -connect com.sun.jdi.SocketAttach:hostname=127.0.0.1,port=8005

catch com.sun.org.apache.xerces.internal.xni.XNIException
catch *.XNIException

  [1] com.sun.xml.internal.stream.StaxErrorReporter.reportError (StaxErrorReporter.java:150), pc = 266
  [2] com.sun.org.apache.xerces.internal.impl.XMLScanner.reportFatalError (XMLScanner.java:1,472), pc = 13
  [3] com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanEntityReference (XMLDocumentFragmentScannerImpl.java:1,893), pc = 297
  [4] com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl$FragmentContentDriver.next (XMLDocumentFragmentScannerImpl.java:3,061), pc = 1,813
  [5] com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl.next (XMLDocumentScannerImpl.java:602), pc = 4
  [6] com.sun.org.apache.xerces.internal.impl.XMLNSDocumentScannerImpl.next (XMLNSDocumentScannerImpl.java:112), pc = 31
  [7] com.sun.org.apache.xerces.internal.impl.XMLStreamReaderImpl.next (XMLStreamReaderImpl.java:553), pc = 40
  [8] XMLStreamReaderNoXXE.main (XMLStreamReaderNoXXE.java:28), pc = 43

17) TransformerFactoryXXE.java

--------------------------------------------------------------------------
/*
 * javac -encoding GBK -g TransformerFactoryXXE.java
 */
import java.io.*;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.dom.DOMResult;

public class TransformerFactoryXXE
{
    public static void main ( String[] argv ) throws Exception
    {
        File                f   = new File( argv[0] );
        InputStream         is  = new FileInputStream( f );
        TransformerFactory  tf  = TransformerFactory.newInstance();
        StreamSource        ss  = new StreamSource( is );
        tf.newTransformer().transform( ss, new DOMResult() );
        is.close();
    }
}
--------------------------------------------------------------------------

$ java TransformerFactoryXXE xxe_0.txt
ERROR:  'http://127.0.0.1:8080/nonexist'
Exception in thread "main" javax.xml.transform.TransformerException: java.io.FileNotFoundException: http://127.0.0.1:8080/nonexist
        at com.sun.org.apache.xalan.internal.xsltc.trax.TransformerImpl.transform(TransformerImpl.java:740)
        at com.sun.org.apache.xalan.internal.xsltc.trax.TransformerImpl.transform(TransformerImpl.java:343)
        at TransformerFactoryXXE.main(TransformerFactoryXXE.java:17)
Caused by: java.io.FileNotFoundException: http://127.0.0.1:8080/nonexist
        at sun.net.www.protocol.http.HttpURLConnection.getInputStream0(HttpURLConnection.java:1896)
        at sun.net.www.protocol.http.HttpURLConnection.getInputStream(HttpURLConnection.java:1498)
        at com.sun.org.apache.xerces.internal.impl.XMLEntityManager.setupCurrentEntity(XMLEntityManager.java:647)
        at com.sun.org.apache.xerces.internal.impl.XMLEntityManager.startEntity(XMLEntityManager.java:1304)
        at com.sun.org.apache.xerces.internal.impl.XMLEntityManager.startEntity(XMLEntityManager.java:1240)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanEntityReference(XMLDocumentFragmentScannerImpl.java:1908)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl$FragmentContentDriver.next(XMLDocumentFragmentScannerImpl.java:3061)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl.next(XMLDocumentScannerImpl.java:602)
        at com.sun.org.apache.xerces.internal.impl.XMLNSDocumentScannerImpl.next(XMLNSDocumentScannerImpl.java:112)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanDocument(XMLDocumentFragmentScannerImpl.java:505)
        at com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse(XML11Configuration.java:842)
        at com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse(XML11Configuration.java:771)
        at com.sun.org.apache.xerces.internal.parsers.XMLParser.parse(XMLParser.java:141)
        at com.sun.org.apache.xerces.internal.parsers.AbstractSAXParser.parse(AbstractSAXParser.java:1213)
        at com.sun.org.apache.xerces.internal.jaxp.SAXParserImpl$JAXPSAXParser.parse(SAXParserImpl.java:643)
        at com.sun.org.apache.xalan.internal.xsltc.trax.TransformerImpl.transformIdentity(TransformerImpl.java:632)
        at com.sun.org.apache.xalan.internal.xsltc.trax.TransformerImpl.transform(TransformerImpl.java:728)
        ... 2 more
---------
java.io.FileNotFoundException: http://127.0.0.1:8080/nonexist
        at sun.net.www.protocol.http.HttpURLConnection.getInputStream0(HttpURLConnection.java:1896)
        at sun.net.www.protocol.http.HttpURLConnection.getInputStream(HttpURLConnection.java:1498)
        at com.sun.org.apache.xerces.internal.impl.XMLEntityManager.setupCurrentEntity(XMLEntityManager.java:647)
        at com.sun.org.apache.xerces.internal.impl.XMLEntityManager.startEntity(XMLEntityManager.java:1304)
        at com.sun.org.apache.xerces.internal.impl.XMLEntityManager.startEntity(XMLEntityManager.java:1240)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanEntityReference(XMLDocumentFragmentScannerImpl.java:1908)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl$FragmentContentDriver.next(XMLDocumentFragmentScannerImpl.java:3061)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl.next(XMLDocumentScannerImpl.java:602)
        at com.sun.org.apache.xerces.internal.impl.XMLNSDocumentScannerImpl.next(XMLNSDocumentScannerImpl.java:112)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanDocument(XMLDocumentFragmentScannerImpl.java:505)
        at com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse(XML11Configuration.java:842)
        at com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse(XML11Configuration.java:771)
        at com.sun.org.apache.xerces.internal.parsers.XMLParser.parse(XMLParser.java:141)
        at com.sun.org.apache.xerces.internal.parsers.AbstractSAXParser.parse(AbstractSAXParser.java:1213)
        at com.sun.org.apache.xerces.internal.jaxp.SAXParserImpl$JAXPSAXParser.parse(SAXParserImpl.java:643)
        at com.sun.org.apache.xalan.internal.xsltc.trax.TransformerImpl.transformIdentity(TransformerImpl.java:632)
        at com.sun.org.apache.xalan.internal.xsltc.trax.TransformerImpl.transform(TransformerImpl.java:728)
        at com.sun.org.apache.xalan.internal.xsltc.trax.TransformerImpl.transform(TransformerImpl.java:343)
        at TransformerFactoryXXE.main(TransformerFactoryXXE.java:17)

HTTP Server收到GET请求，返回404，导致客户端抛出异常。

18) TransformerFactoryNoXXE.java

--------------------------------------------------------------------------
/*
 * javac -encoding GBK -g TransformerFactoryNoXXE.java
 */
import java.io.*;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.dom.DOMResult;
import javax.xml.XMLConstants;

public class TransformerFactoryNoXXE
{
    private static TransformerFactory GetTransformerFactory ()
    {
        TransformerFactory  tf  = TransformerFactory.newInstance();
        tf.setAttribute( XMLConstants.ACCESS_EXTERNAL_DTD, "" );
        tf.setAttribute( XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "" );
        return tf;
    }

    public static void main ( String[] argv ) throws Exception
    {
        File                f   = new File( argv[0] );
        InputStream         is  = new FileInputStream( f );
        /*
         * 原来是"TransformerFactory.newInstance()"
         */
        TransformerFactory  tf  = GetTransformerFactory();
        StreamSource        ss  = new StreamSource( is );
        tf.newTransformer().transform( ss, new DOMResult() );
        is.close();
    }
}
--------------------------------------------------------------------------

$ java TransformerFactoryNoXXE xxe_0.txt
[Fatal Error] :5:12: External Entity: Failed to read external document 'nonexist', because 'http' access is not allowed due to restriction set by the accessExternalDTD property.
ERROR:  'External Entity: Failed to read external document 'nonexist', because 'http' access is not allowed due to restriction set by the accessExternalDTD property.'
Exception in thread "main" javax.xml.transform.TransformerException: org.xml.sax.SAXParseException; lineNumber: 5; columnNumber: 12; External Entity: Failed to read external document 'nonexist', because 'http' access is not allowed due to restriction set by the accessExternalDTD property.
        at com.sun.org.apache.xalan.internal.xsltc.trax.TransformerImpl.transform(TransformerImpl.java:740)
        at com.sun.org.apache.xalan.internal.xsltc.trax.TransformerImpl.transform(TransformerImpl.java:343)
        at TransformerFactoryNoXXE.main(TransformerFactoryNoXXE.java:29)
Caused by: org.xml.sax.SAXParseException; lineNumber: 5; columnNumber: 12; External Entity: Failed to read external document 'nonexist', because 'http' access is not allowed due to restriction set by the accessExternalDTD property.
        at com.sun.org.apache.xerces.internal.parsers.AbstractSAXParser.parse(AbstractSAXParser.java:1239)
        at com.sun.org.apache.xerces.internal.jaxp.SAXParserImpl$JAXPSAXParser.parse(SAXParserImpl.java:643)
        at com.sun.org.apache.xalan.internal.xsltc.trax.TransformerImpl.transformIdentity(TransformerImpl.java:632)
        at com.sun.org.apache.xalan.internal.xsltc.trax.TransformerImpl.transform(TransformerImpl.java:728)
        ... 2 more
---------
org.xml.sax.SAXParseException; lineNumber: 5; columnNumber: 12; External Entity: Failed to read external document 'nonexist', because 'http' access is not allowed due to restriction set by the accessExternalDTD property.
        at com.sun.org.apache.xerces.internal.parsers.AbstractSAXParser.parse(AbstractSAXParser.java:1239)
        at com.sun.org.apache.xerces.internal.jaxp.SAXParserImpl$JAXPSAXParser.parse(SAXParserImpl.java:643)
        at com.sun.org.apache.xalan.internal.xsltc.trax.TransformerImpl.transformIdentity(TransformerImpl.java:632)
        at com.sun.org.apache.xalan.internal.xsltc.trax.TransformerImpl.transform(TransformerImpl.java:728)
        at com.sun.org.apache.xalan.internal.xsltc.trax.TransformerImpl.transform(TransformerImpl.java:343)
        at TransformerFactoryNoXXE.main(TransformerFactoryNoXXE.java:29)

HTTP Server不会收到GET请求，tf.newTransformer().transform()时直接抛出异常。

$ java -agentlib:jdwp=transport=dt_socket,address=127.0.0.1:8005,server=y,suspend=y TransformerFactoryNoXXE xxe_0.txt
$ jdb -connect com.sun.jdi.SocketAttach:hostname=127.0.0.1,port=8005

catch *.XMLParseException

  [1] com.sun.org.apache.xerces.internal.util.DefaultErrorHandler.fatalError (DefaultErrorHandler.java:85), pc = 8
  [2] com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError (XMLErrorReporter.java:400), pc = 270
  [3] com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError (XMLErrorReporter.java:327), pc = 9
  [4] com.sun.org.apache.xerces.internal.impl.XMLEntityManager.startEntity (XMLEntityManager.java:1,224), pc = 854
  [5] com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanEntityReference (XMLDocumentFragmentScannerImpl.java:1,908), pc = 373
  [6] com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl$FragmentContentDriver.next (XMLDocumentFragmentScannerImpl.java:3,061), pc = 1,813
  [7] com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl.next (XMLDocumentScannerImpl.java:602), pc = 4
  [8] com.sun.org.apache.xerces.internal.impl.XMLNSDocumentScannerImpl.next (XMLNSDocumentScannerImpl.java:112), pc = 31
  [9] com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanDocument (XMLDocumentFragmentScannerImpl.java:505), pc = 308
  [10] com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse (XML11Configuration.java:842), pc = 123
  [11] com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse (XML11Configuration.java:771), pc = 29
  [12] com.sun.org.apache.xerces.internal.parsers.XMLParser.parse (XMLParser.java:141), pc = 76
  [13] com.sun.org.apache.xerces.internal.parsers.AbstractSAXParser.parse (AbstractSAXParser.java:1,213), pc = 43
  [14] com.sun.org.apache.xerces.internal.jaxp.SAXParserImpl$JAXPSAXParser.parse (SAXParserImpl.java:643), pc = 53
  [15] com.sun.org.apache.xalan.internal.xsltc.trax.TransformerImpl.transformIdentity (TransformerImpl.java:632), pc = 187
  [16] com.sun.org.apache.xalan.internal.xsltc.trax.TransformerImpl.transform (TransformerImpl.java:728), pc = 140
  [17] com.sun.org.apache.xalan.internal.xsltc.trax.TransformerImpl.transform (TransformerImpl.java:343), pc = 119
  [18] TransformerFactoryNoXXE.main (TransformerFactoryNoXXE.java:29), pc = 47

19) ValidatorXXE.java

--------------------------------------------------------------------------
/*
 * javac -encoding GBK -g ValidatorXXE.java
 */
import java.io.*;
import javax.xml.validation.*;
import javax.xml.transform.stream.StreamSource;

public class ValidatorXXE
{
    public static void main ( String[] argv ) throws Exception
    {
        File            f   = new File( argv[0] );
        InputStream     is  = new FileInputStream( f );
        SchemaFactory   sf  = SchemaFactory.newInstance( "http://www.w3.org/2001/XMLSchema" );
        Schema          s   = sf.newSchema();
        Validator       v   = s.newValidator();
        StreamSource    ss  = new StreamSource( is );
        v.validate( ss );
        is.close();
    }
}
--------------------------------------------------------------------------

$ java ValidatorXXE xxe_0.txt
Exception in thread "main" java.io.FileNotFoundException: http://127.0.0.1:8080/nonexist
        at sun.net.www.protocol.http.HttpURLConnection.getInputStream0(HttpURLConnection.java:1896)
        at sun.net.www.protocol.http.HttpURLConnection.getInputStream(HttpURLConnection.java:1498)
        at com.sun.org.apache.xerces.internal.impl.XMLEntityManager.setupCurrentEntity(XMLEntityManager.java:647)
        at com.sun.org.apache.xerces.internal.impl.XMLEntityManager.startEntity(XMLEntityManager.java:1304)
        at com.sun.org.apache.xerces.internal.impl.XMLEntityManager.startEntity(XMLEntityManager.java:1240)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanEntityReference(XMLDocumentFragmentScannerImpl.java:1908)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl$FragmentContentDriver.next(XMLDocumentFragmentScannerImpl.java:3061)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl.next(XMLDocumentScannerImpl.java:602)
        at com.sun.org.apache.xerces.internal.impl.XMLNSDocumentScannerImpl.next(XMLNSDocumentScannerImpl.java:112)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanDocument(XMLDocumentFragmentScannerImpl.java:505)
        at com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse(XML11Configuration.java:842)
        at com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse(XML11Configuration.java:771)
        at com.sun.org.apache.xerces.internal.jaxp.validation.StreamValidatorHelper.validate(StreamValidatorHelper.java:152)
        at com.sun.org.apache.xerces.internal.jaxp.validation.ValidatorImpl.validate(ValidatorImpl.java:116)
        at javax.xml.validation.Validator.validate(Validator.java:124)
        at ValidatorXXE.main(ValidatorXXE.java:18)

HTTP Server收到GET请求，返回404，导致客户端抛出异常。

20) ValidatorNoXXE.java

--------------------------------------------------------------------------
/*
 * javac -encoding GBK -g ValidatorNoXXE.java
 */
import java.io.*;
import org.xml.sax.*;
import javax.xml.validation.*;
import javax.xml.transform.stream.StreamSource;
import javax.xml.XMLConstants;

public class ValidatorNoXXE
{
    private static Validator GetValidator ( Schema s )
        throws SAXNotRecognizedException, SAXNotSupportedException
    {
        Validator   v   = s.newValidator();
        /*
         * 不是setAttribute()
         */
        v.setProperty( XMLConstants.ACCESS_EXTERNAL_DTD, "" );
        /*
         * 不是ACCESS_EXTERNAL_STYLESHEET
         */
        v.setProperty( XMLConstants.ACCESS_EXTERNAL_SCHEMA, "" );
        return v;
    }

    public static void main ( String[] argv ) throws Exception
    {
        File            f   = new File( argv[0] );
        InputStream     is  = new FileInputStream( f );
        SchemaFactory   sf  = SchemaFactory.newInstance( "http://www.w3.org/2001/XMLSchema" );
        Schema          s   = sf.newSchema();
        /*
         * 原来是"s.newValidator()"
         */
        Validator       v   = GetValidator( s );
        StreamSource    ss  = new StreamSource( is );
        v.validate( ss );
        is.close();
    }
}
--------------------------------------------------------------------------

$ java ValidatorNoXXE xxe_0.txt
Exception in thread "main" org.xml.sax.SAXParseException; lineNumber: 5; columnNumber: 12; External Entity: Failed to read external document 'nonexist', because 'http' access is not allowed due to restriction set by the accessExternalDTD property.
        at com.sun.org.apache.xerces.internal.util.ErrorHandlerWrapper.createSAXParseException(ErrorHandlerWrapper.java:203)
        at com.sun.org.apache.xerces.internal.util.ErrorHandlerWrapper.fatalError(ErrorHandlerWrapper.java:177)
        at com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError(XMLErrorReporter.java:400)
        at com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError(XMLErrorReporter.java:327)
        at com.sun.org.apache.xerces.internal.impl.XMLEntityManager.startEntity(XMLEntityManager.java:1224)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanEntityReference(XMLDocumentFragmentScannerImpl.java:1908)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl$FragmentContentDriver.next(XMLDocumentFragmentScannerImpl.java:3061)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl.next(XMLDocumentScannerImpl.java:602)
        at com.sun.org.apache.xerces.internal.impl.XMLNSDocumentScannerImpl.next(XMLNSDocumentScannerImpl.java:112)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanDocument(XMLDocumentFragmentScannerImpl.java:505)
        at com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse(XML11Configuration.java:842)
        at com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse(XML11Configuration.java:771)
        at com.sun.org.apache.xerces.internal.jaxp.validation.StreamValidatorHelper.validate(StreamValidatorHelper.java:152)
        at com.sun.org.apache.xerces.internal.jaxp.validation.ValidatorImpl.validate(ValidatorImpl.java:116)
        at javax.xml.validation.Validator.validate(Validator.java:124)
        at ValidatorNoXXE.main(ValidatorNoXXE.java:38)

HTTP Server不会收到GET请求，v.validate()时直接抛出异常。

$ java -agentlib:jdwp=transport=dt_socket,address=127.0.0.1:8005,server=y,suspend=y ValidatorNoXXE xxe_0.txt
$ jdb -connect com.sun.jdi.SocketAttach:hostname=127.0.0.1,port=8005

catch *.XMLParseException

  [1] com.sun.org.apache.xerces.internal.util.ErrorHandlerWrapper.fatalError (ErrorHandlerWrapper.java:183), pc = 34
  [2] com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError (XMLErrorReporter.java:400), pc = 270
  [3] com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError (XMLErrorReporter.java:327), pc = 9
  [4] com.sun.org.apache.xerces.internal.impl.XMLEntityManager.startEntity (XMLEntityManager.java:1,224), pc = 854
  [5] com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanEntityReference (XMLDocumentFragmentScannerImpl.java:1,908), pc = 373
  [6] com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl$FragmentContentDriver.next (XMLDocumentFragmentScannerImpl.java:3,061), pc = 1,813
  [7] com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl.next (XMLDocumentScannerImpl.java:602), pc = 4
  [8] com.sun.org.apache.xerces.internal.impl.XMLNSDocumentScannerImpl.next (XMLNSDocumentScannerImpl.java:112), pc = 31
  [9] com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanDocument (XMLDocumentFragmentScannerImpl.java:505), pc = 308
  [10] com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse (XML11Configuration.java:842), pc = 123
  [11] com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse (XML11Configuration.java:771), pc = 29
  [12] com.sun.org.apache.xerces.internal.jaxp.validation.StreamValidatorHelper.validate (StreamValidatorHelper.java:152), pc = 220
  [13] com.sun.org.apache.xerces.internal.jaxp.validation.ValidatorImpl.validate (ValidatorImpl.java:116), pc = 117
  [14] javax.xml.validation.Validator.validate (Validator.java:124), pc = 3
  [15] ValidatorNoXXE.main (ValidatorNoXXE.java:38), pc = 53

21) Unmarshaller No XXE

21.1) Customer.java

待序列化的类

--------------------------------------------------------------------------
/*
 * javac -encoding GBK -g Customer.java
 */
import javax.xml.bind.annotation.*;

@XmlRootElement
public class Customer
{
    String  name;
    int     age;
    int     id;

    public String getName()
    {
        return( this.name );
    }

    @XmlElement
    public void setName ( String name )
    {
        this.name   = name;
    }

    public int getAge ()
    {
        return( this.age );
    }

    @XmlElement
    public void setAge ( int age )
    {
        this.age    = age;
    }

    public int getId ()
    {
        return( this.id );
    }

    @XmlAttribute
    public void setId ( int id )
    {
        this.id     = id;
    }
}
--------------------------------------------------------------------------

21.2) MarshallerCustomer.java

序列化

--------------------------------------------------------------------------
/*
 * javac -encoding GBK -g MarshallerCustomer.java
 */
import java.io.*;
import javax.xml.bind.*;

public class MarshallerCustomer
{
    public static void main ( String[] argv ) throws Exception
    {
        File            f   = new File( argv[0] );
        Class           clz = Customer.class;
        JAXBContext     c   = JAXBContext.newInstance( clz );
        Marshaller      m   = c.createMarshaller();
        m.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, true );
        Customer        obj = new Customer();
        obj.setName( "yoda" );
        obj.setAge( 1024 );
        obj.setId( 9527 );
        m.marshal( obj, f );
    }
}
--------------------------------------------------------------------------

marshal()重载过，第二形参可以是File、OutputStream、Document等等，上例将对
象实例序列化之后输出到文件，内容是XML格式。

$ java MarshallerCustomer xxe_2.txt

$ cat xxe_2.txt
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<customer id="9527">
    <age>1024</age>
    <name>yoda</name>
</customer>

21.3) UnmarshallerCustomer.java

反序列化

--------------------------------------------------------------------------
/*
 * javac -encoding GBK -g UnmarshallerCustomer.java
 */
import java.io.*;
import javax.xml.bind.*;

public class UnmarshallerCustomer
{
    public static void main ( String[] argv ) throws Exception
    {
        File            f   = new File( argv[0] );
        Class           clz = Customer.class;
        JAXBContext     c   = JAXBContext.newInstance( clz );
        Unmarshaller    u   = c.createUnmarshaller();
        Customer        obj = ( Customer )u.unmarshal( f );
        System.out.println
        (
            String.format
            (
            "name    = %s\n" +
            "age     = %d\n" +
            "id      = %d",
            obj.getName(),
            obj.getAge(),
            obj.getId()
            )
        );
    }
}
--------------------------------------------------------------------------

$ java UnmarshallerCustomer xxe_2.txt
name    = yoda
age     = 1024
id      = 9527

为什么前面要搞个Customer.java、MarshallerCustomer.java出来？因为演示
Unmarshaller用法时，如果没有有效的xxe_2.txt，会触发反序列化本身相关的异常，
此时调用栈回溯中的信息与XXE无关。

在xxe_2.txt的基础上修改出xxe_3.txt，增加DTD:

$ cat xxe_3.txt
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<!DOCTYPE customer [
<!ELEMENT customer ANY>
<!ENTITY some SYSTEM "http://127.0.0.1:8080/nonexist">
]>
<customer id="9527">
    <age>1024</age>
    <name>&some;</name>
</customer>

$ java UnmarshallerCustomer xxe_3.txt
Exception in thread "main" javax.xml.bind.UnmarshalException
 - with linked exception:
[org.xml.sax.SAXParseException; systemId: file:/home/scz/src/XXE/xxe_3.txt; lineNumber: 8; columnNumber: 17; External Entity: Failed to read external document 'nonexist', because 'http' access is not allowed due to restriction set by the accessExternalDTD property.]
        at javax.xml.bind.helpers.AbstractUnmarshallerImpl.createUnmarshalException(AbstractUnmarshallerImpl.java:335)
        at com.sun.xml.internal.bind.v2.runtime.unmarshaller.UnmarshallerImpl.createUnmarshalException(UnmarshallerImpl.java:563)
        at com.sun.xml.internal.bind.v2.runtime.unmarshaller.UnmarshallerImpl.unmarshal0(UnmarshallerImpl.java:249)
        at com.sun.xml.internal.bind.v2.runtime.unmarshaller.UnmarshallerImpl.unmarshal(UnmarshallerImpl.java:214)
        at javax.xml.bind.helpers.AbstractUnmarshallerImpl.unmarshal(AbstractUnmarshallerImpl.java:157)
        at javax.xml.bind.helpers.AbstractUnmarshallerImpl.unmarshal(AbstractUnmarshallerImpl.java:162)
        at javax.xml.bind.helpers.AbstractUnmarshallerImpl.unmarshal(AbstractUnmarshallerImpl.java:171)
        at javax.xml.bind.helpers.AbstractUnmarshallerImpl.unmarshal(AbstractUnmarshallerImpl.java:189)
        at UnmarshallerCustomer.main(UnmarshallerCustomer.java:15)
Caused by: org.xml.sax.SAXParseException; systemId: file:/home/scz/src/XXE/xxe_3.txt; lineNumber: 8; columnNumber: 17; External Entity: Failed to read external document 'nonexist', because 'http' access is not allowed due to restriction set by the accessExternalDTD property.
        at com.sun.org.apache.xerces.internal.util.ErrorHandlerWrapper.createSAXParseException(ErrorHandlerWrapper.java:203)
        at com.sun.org.apache.xerces.internal.util.ErrorHandlerWrapper.fatalError(ErrorHandlerWrapper.java:177)
        at com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError(XMLErrorReporter.java:400)
        at com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError(XMLErrorReporter.java:327)
        at com.sun.org.apache.xerces.internal.impl.XMLEntityManager.startEntity(XMLEntityManager.java:1224)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanEntityReference(XMLDocumentFragmentScannerImpl.java:1908)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl$FragmentContentDriver.next(XMLDocumentFragmentScannerImpl.java:3061)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl.next(XMLDocumentScannerImpl.java:602)
        at com.sun.org.apache.xerces.internal.impl.XMLNSDocumentScannerImpl.next(XMLNSDocumentScannerImpl.java:112)
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanDocument(XMLDocumentFragmentScannerImpl.java:505)
        at com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse(XML11Configuration.java:842)
        at com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse(XML11Configuration.java:771)
        at com.sun.org.apache.xerces.internal.parsers.XMLParser.parse(XMLParser.java:141)
        at com.sun.org.apache.xerces.internal.parsers.AbstractSAXParser.parse(AbstractSAXParser.java:1213)
        at com.sun.org.apache.xerces.internal.jaxp.SAXParserImpl$JAXPSAXParser.parse(SAXParserImpl.java:643)
        at com.sun.xml.internal.bind.v2.runtime.unmarshaller.UnmarshallerImpl.unmarshal0(UnmarshallerImpl.java:243)
        ... 6 more

直接提示:

External Entity: Failed to read external document 'nonexist', because
'http' access is not allowed due to restriction set by the
accessExternalDTD property.

这意味着Unmarshaller方案内部缺省设置过ACCESS_EXTERNAL_DTD。

按l1nk3r的说法，Unmarshaller解析XML时不存在XXE问题。

$ java -agentlib:jdwp=transport=dt_socket,address=127.0.0.1:8005,server=y,suspend=y UnmarshallerCustomer xxe_3.txt
$ jdb -connect com.sun.jdi.SocketAttach:hostname=127.0.0.1,port=8005

catch *.XMLParseException

  [1] com.sun.org.apache.xerces.internal.util.ErrorHandlerWrapper.fatalError (ErrorHandlerWrapper.java:183), pc = 34
  [2] com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError (XMLErrorReporter.java:400), pc = 270
  [3] com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError (XMLErrorReporter.java:327), pc = 9
  [4] com.sun.org.apache.xerces.internal.impl.XMLEntityManager.startEntity (XMLEntityManager.java:1,224), pc = 854
  [5] com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanEntityReference (XMLDocumentFragmentScannerImpl.java:1,908), pc = 373
  [6] com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl$FragmentContentDriver.next (XMLDocumentFragmentScannerImpl.java:3,061), pc = 1,813
  [7] com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl.next (XMLDocumentScannerImpl.java:602), pc = 4
  [8] com.sun.org.apache.xerces.internal.impl.XMLNSDocumentScannerImpl.next (XMLNSDocumentScannerImpl.java:112), pc = 31
  [9] com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanDocument (XMLDocumentFragmentScannerImpl.java:505), pc = 308
  [10] com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse (XML11Configuration.java:842), pc = 123
  [11] com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse (XML11Configuration.java:771), pc = 29
  [12] com.sun.org.apache.xerces.internal.parsers.XMLParser.parse (XMLParser.java:141), pc = 76
  [13] com.sun.org.apache.xerces.internal.parsers.AbstractSAXParser.parse (AbstractSAXParser.java:1,213), pc = 43
  [14] com.sun.org.apache.xerces.internal.jaxp.SAXParserImpl$JAXPSAXParser.parse (SAXParserImpl.java:643), pc = 53
  [15] com.sun.xml.internal.bind.v2.runtime.unmarshaller.UnmarshallerImpl.unmarshal0 (UnmarshallerImpl.java:243), pc = 31
  [16] com.sun.xml.internal.bind.v2.runtime.unmarshaller.UnmarshallerImpl.unmarshal (UnmarshallerImpl.java:214), pc = 4
  [17] javax.xml.bind.helpers.AbstractUnmarshallerImpl.unmarshal (AbstractUnmarshallerImpl.java:157), pc = 25
  [18] javax.xml.bind.helpers.AbstractUnmarshallerImpl.unmarshal (AbstractUnmarshallerImpl.java:162), pc = 9
  [19] javax.xml.bind.helpers.AbstractUnmarshallerImpl.unmarshal (AbstractUnmarshallerImpl.java:171), pc = 24
  [20] javax.xml.bind.helpers.AbstractUnmarshallerImpl.unmarshal (AbstractUnmarshallerImpl.java:189), pc = 120
  [21] UnmarshallerCustomer.main (UnmarshallerCustomer.java:15), pc = 35

22) 防御方式小结

回顾所有的示例代码，Java XXE防御方式有两大类。

--------------------------------------------------------------------------
a)

setFeature( "http://apache.org/xml/features/disallow-doctype-decl", true );             // 禁用DTD
setFeature( "http://xml.org/sax/features/external-general-entities", false );           // 禁用外部通用实体
setFeature( "http://xml.org/sax/features/external-parameter-entities", false );         // 禁用外部参数实体
setFeature( "http://apache.org/xml/features/nonvalidating/load-external-dtd", false );  // 禁用外部DTD
--------------------------------------------------------------------------
b)

setAttribute( XMLConstants.ACCESS_EXTERNAL_DTD, "" );
setAttribute( XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "" );

或

setProperty( XMLConstants.ACCESS_EXTERNAL_DTD, "" );
setProperty( XMLConstants.ACCESS_EXTERNAL_SCHEMA, "" );
--------------------------------------------------------------------------

23) 讨论

23.1) 寻找潜在XXE点

前面有11种示例，除了Unmarshaller，前10种都有XXE风险。如果做源代码审计，至
少前10种不能放过。

从调用栈回溯看，前10种示例触发XXE时都会经过:

sun.net.www.protocol.http.HttpURLConnection.getInputStream0(HttpURLConnection.java:1896)
sun.net.www.protocol.http.HttpURLConnection.getInputStream(HttpURLConnection.java:1498)
com.sun.org.apache.xerces.internal.impl.XMLEntityManager.setupCurrentEntity(XMLEntityManager.java:647)
com.sun.org.apache.xerces.internal.impl.XMLEntityManager.startEntity(XMLEntityManager.java:1304)
com.sun.org.apache.xerces.internal.impl.XMLEntityManager.startEntity(XMLEntityManager.java:1240)
com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanEntityReference(XMLDocumentFragmentScannerImpl.java:1908)
com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl$FragmentContentDriver.next(XMLDocumentFragmentScannerImpl.java:3061)
com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl.next(XMLDocumentScannerImpl.java:602)

如果已经能控制输入的XML，只需要在外部接应的HTTP Server上观察，即可确定是否
存在XXE。如果不能控制输入的XML，或者正在寻找潜在的XXE点，可以用jdb、BTrace、
Arthas等技术手段动态拦截上面这些位置，正常使用目标系统，如有断点命中，可在
调用栈回溯中寻找上层切入点。

23.2) 防御XXE成功时的调用栈回溯

共有五大类。

--------------------------------------------------------------------------
a)

DocumentBuilderNoXXE
SAXParserNoXXE
XMLReaderNoXXE

在jdb中拦截:

catch com.sun.org.apache.xerces.internal.xni.parser.XMLParseException
catch *.XMLParseException

命中时的调用栈回溯:

com.sun.org.apache.xerces.internal.util.DefaultErrorHandler.fatalError (DefaultErrorHandler.java:85)
com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError (XMLErrorReporter.java:400)
com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError (XMLErrorReporter.java:327)
com.sun.org.apache.xerces.internal.impl.XMLScanner.reportFatalError (XMLScanner.java:1,472)
--------------------------------------------------------------------------
b)

SAXBuilderNoXXE
SAXReaderNoXXE

catch *.XMLParseException

com.sun.org.apache.xerces.internal.util.ErrorHandlerWrapper.fatalError (ErrorHandlerWrapper.java:183)
com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError (XMLErrorReporter.java:400)
com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError (XMLErrorReporter.java:327)
com.sun.org.apache.xerces.internal.impl.XMLScanner.reportFatalError (XMLScanner.java:1,472)

b类最上层的栈帧与a类不同。
--------------------------------------------------------------------------
c)

SAXTransformerFactoryNoXXE
TransformerFactoryNoXXE

catch *.XMLParseException

com.sun.org.apache.xerces.internal.util.DefaultErrorHandler.fatalError (DefaultErrorHandler.java:85)
com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError (XMLErrorReporter.java:400)
com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError (XMLErrorReporter.java:327)
com.sun.org.apache.xerces.internal.impl.XMLEntityManager.startEntity (XMLEntityManager.java:1,224)

c类由startEntity()调用reportError()，a类由reportFatalError()调用
reportError()。
--------------------------------------------------------------------------
d)

SchemaFactoryNoXXE
ValidatorNoXXE
UnmarshallerCustomer

catch *.XMLParseException

com.sun.org.apache.xerces.internal.util.ErrorHandlerWrapper.fatalError (ErrorHandlerWrapper.java:183)
com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError (XMLErrorReporter.java:400)
com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError (XMLErrorReporter.java:327)
com.sun.org.apache.xerces.internal.impl.XMLEntityManager.startEntity (XMLEntityManager.java:1,224)

d类最下层栈帧与b类不同。d类最上层的栈帧与c类不同。
--------------------------------------------------------------------------
e)

XMLStreamReaderNoXXE

在jdb中拦截:

catch com.sun.org.apache.xerces.internal.xni.XNIException
catch *.XNIException

e类拦截的异常与a至d类不同。

命中时的调用栈回溯:

com.sun.xml.internal.stream.StaxErrorReporter.reportError (StaxErrorReporter.java:150)
com.sun.org.apache.xerces.internal.impl.XMLScanner.reportFatalError (XMLScanner.java:1,472)

流程仍然到达reportFatalError()，但往下执行时不再经过:

com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError

而是经过:

com.sun.xml.internal.stream.StaxErrorReporter.reportError
--------------------------------------------------------------------------

对于a至d类，拦截这两个位置必有命中:

com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError (XMLErrorReporter.java:400)
com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError (XMLErrorReporter.java:327)

其中327行更佳。

--------------------------------------------------------------------------
public String reportError ( XMLLocator location, String domain, String key, Object[] arguments, short severity )
    throws XNIException
{
    /*
     * 327行
     */
    return reportError( location, domain, key, arguments, severity, null );
}
--------------------------------------------------------------------------

327行是1.8.0_232版Java的位置，如果不想去查行号，就得写个很长的断点:

stop in com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError(com.sun.org.apache.xerces.internal.xni.XMLLocator,java.lang.String,java.lang.String,java.lang.Object[],short)

因为reportError()重载过。

对于a至d类，可以直接捕捉异常，更省事:

catch com.sun.org.apache.xerces.internal.xni.parser.XMLParseException
catch *.XMLParseException

对于e类，捕捉异常:

catch com.sun.org.apache.xerces.internal.xni.XNIException
catch *.XNIException

看一下:

com.sun.org.apache.xerces.internal.impl.XMLScanner.reportFatalError (XMLScanner.java:1,472)

--------------------------------------------------------------------------
/*
 * 这个函数没有重载过，只此一份
 */
protected void reportFatalError ( String msgId, Object[] args )
    throws XNIException
{
    /*
     * 1472行
     */
    this.fErrorReporter.reportError( this.fEntityScanner, "http://www.w3.org/TR/1998/REC-xml-19980210", msgId, args, (short)2 );
}
--------------------------------------------------------------------------

stop in com.sun.org.apache.xerces.internal.impl.XMLScanner.reportFatalError

用这个断点可以拦住a、b、e类。

stop in com.sun.org.apache.xerces.internal.impl.XMLEntityManager.startEntity(boolean,java.lang.String,boolean)

用这个断点可以拦住c、d类。

前面很多技术方案是冗余的，仅仅出于完备性考虑列举出来。

23.3) Xerces XML parsers

23.1、23.2小节讨论的是Oracle XML parsers，这是rt.jar自带的。但很多大型Java
应用使用xercesImpl-2.9.1.jar提供的Xerces XML parsers，比如Zimbra就用Xerces
版本。二者的区别形如:

org.apache.xerces.jaxp.DocumentBuilderImpl                          // Xerces
com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderImpl         // Oracle

org.apache.xerces.impl.XMLDocumentScannerImpl                       // Xerces
com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl      // Oracle

org.apache.xerces.<something>                                       // Xerces
com.sun.org.apache.xerces.internal.<something>                      // Oracle

二者包名不一样，Xerces版本没有保留行号，据说功能比Oracle版本完善。

这是个坑，必须设法先搞清楚目标系统用哪个版本的XML parsers，否则断点都设不
上。另一个类似的是:

org.apache.xalan.processor.TransformerFactoryImpl                   // Xalan
com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl // Oracle

☆ 后记

这应该是从入门到入狱系列中文章。

我是"真·WEB小白"，2019年10底初次接触XXE，前面的内容很初阶，只是个人学习笔
记，贻笑大方。

参考资源里所有文章我都过了一遍，如果你跟我一样是初学者，看看无妨。我把文章
发布时间给出来了，这个信息有时很有意义。

最后再次感谢l1nk3r，特喜欢这种极具可操作性的文章。

☆ 参考资源

[1] Java服务XXE漏洞防御方法 - leadroyal [2018-08-03]
    https://www.leadroyal.cn/?p=562

    9102年Java里的XXE - leadroyal [2019-07-17]
    https://www.leadroyal.cn/?p=914
    https://github.com/LeadroyaL/java_xxe_2019

    9102年Java里的XXE的防御 - leadroyal [2019-07-18]
    https://www.leadroyal.cn/?p=930

[2] JAVA常见的XXE漏洞写法和防御 - spoock [2018-10-23]
    https://blog.spoock.com/2018/10/23/java-xxe/

[3] 从WxJava XXE注入漏洞中发现了一个对JDK的误会 - 图南 [2019-01-31]
    https://mp.weixin.qq.com/s/bTeJYzUN9T1u-KDZON5FiQ

    Java XXE注入修复问题填坑实录 - gyyyy [2019-02-02]
    https://mp.weixin.qq.com/s/sGcaDCokVxhELd63-0TmIw

    一个被广泛流传的XXE漏洞错误修复方案 - c0ny1 [2019-02-14]
    http://gv7.me/articles/2019/a-widely-circulated-xxe-bug-fix/

[4] Java XXE 总结 - l1nk3r [2019-10-31]
    http://www.lmxspace.com/2019/10/31/Java-XXE-%E6%80%BB%E7%BB%93/
    https://www.t00ls.net/thread-53607-1-3.html