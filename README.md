

-----------------------------------------------------------------------------
## FastJson RCE
https://github.com/alibaba/fastjson/releases
-----------------------------------------------------------------------------
由一个报错
Caused by: com.alibaba.fastjson.JSONException: autoType is not support.
引起我查询到https://github.com/alibaba/fastjson/wiki/enable_autotype

注意：
	fastjson version/jdk version/RMI OR LADP 都会影响到具体漏洞利用,比如：
	fastjson	Feature.SupportNonPublicField;
	大部分的jndi注入都是基于jdk8u121之前的版本，之后因为trustURLCodebase=true需要更多工作（绕过）；
	
1.参考
文章解析参考：
这个讲了不少细节
https://aluvion.github.io/2019/03/17/Java%E5%8F%8D%E5%BA%8F%E5%88%97%E5%8C%96%E6%BC%8F%E6%B4%9E-Fastjson/
这个是讲weblogic的xmldecoder 不过类似
https://kylingit.com/blog/weblogic-xmldecoder-rce%E4%B9%8Brmi%E5%88%A9%E7%94%A8/

深入理解JNDI注入与Java反序列化漏洞利用 
https://security.tencent.com/index.php/blog/msg/131
https://kingx.me/Exploit-Java-Deserialization-with-RMI.html
fastjson低于1.2.60的远程拒绝服务漏洞
https://blog.csdn.net/lpf463061655/article/details/100695212
https://www.anquanke.com/post/id/185909
https://www.angelwhu.com/blog/?p=552

源码参考：
https://github.com/earayu/fastjson_jndi_poc
fastjson在1.2.24以及之前版本

https://github.com/Venscor/fastjsonvul
里面有很多pdf讲解

https://github.com/shengqi158/fastjson-remote-code-execute-poc
支持jdk1.7，1.8 该poc只能运行在fastjson-1.2.22到fastjson-1.2.24版本区间，因为fastjson从1.2.22版本才开始引入SupportNonPublicField

https://github.com/iBearcat/FastJson-JdbcRowSetImpl-RCE
CTF竞赛漏洞环境搭建

2.更深入问题

deploy on real host
http://www.yulegeyu.com/2018/12/22/RMI-ReferenceWrapper-Stub-With-Hostname/

jdk高版本绕过
Oracle在>=jdk8u121之后设置了com.sun.jndi.rmi.object.trustURLCodebase为 false
https://bl4ck.in/tricks/2019/01/04/JNDI-Injection-Bypass.html
https://kingx.me/Restrictions-and-Bypass-of-JNDI-Manipulations-RCE.html

https://github.com/kxcode/JNDI-Exploit-Bypass-Demo

浅谈Fastjson RCE漏洞的绕过史  https://www.freebuf.com/vuls/208339.html

fastjson的CVE bug监测较弱，很多CVE数据库网站上有关fastjson的CVE寥寥无几，例如近期的AutoType导致的高危漏洞，虽然和Jackson的PolymorphicDeserialization是同样的bug，但是CVE网站上几乎没有fastjson的bug报告。
https://www.cnblogs.com/larva-zhh/p/11544317.html

-----------------------------------------------------------------------------
## Other
-----------------------------------------------------------------------------