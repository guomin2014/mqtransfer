package com.gm.mqtransfer.manager.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatConfig {
	
	@Autowired
    private ServletWebServerApplicationContext appContext;
	
	public int getPort(){
        return appContext.getWebServer().getPort();
    }

}
