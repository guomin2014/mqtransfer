package com.gm.mqtransfer.manager.config;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.gm.mqtransfer.manager.support.http.HtmlViewServlet;

@Configuration
@EnableWebMvc
@ComponentScan(basePackages= {"com.gm.mqtransfer"},includeFilters = { @ComponentScan.Filter(type = FilterType.ANNOTATION, value = Controller.class) },useDefaultFilters = false)
public class WebMvcConfig implements WebMvcConfigurer {

	@Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
//		System.out.println("***********addResourceHandlers*********");
//        registry.addResourceHandler("/css/**").addResourceLocations("/css/");
//        registry.addResourceHandler("/fonts/**").addResourceLocations("/fonts/");
//        registry.addResourceHandler("/js/**").addResourceLocations("/js/");
//        //swagger支持
//        registry.addResourceHandler("/*.html").addResourceLocations("/");
//        registry.addResourceHandler("/webjars/**").addResourceLocations("/webjars/");
		
//		registry.addResourceHandler("/index.html").addResourceLocations("classpath:index.html");
//		registry.addResourceHandler("/home/**").addResourceLocations("classpath:/html/*");
    }
	
	@Bean
    public ServletRegistrationBean<HtmlViewServlet> viewServlet() {
        ServletRegistrationBean<HtmlViewServlet> servletRegistrationBean = new ServletRegistrationBean<>(new HtmlViewServlet(), "/ui/*");
        // IP白名单 多个,隔开
        servletRegistrationBean.addInitParameter("allow", "192.168.2.25,127.0.0.1");
        // IP黑名单(共同存在时，deny优先于allow) 多个,隔开
        servletRegistrationBean.addInitParameter("deny", "192.168.1.100");
        //控制台管理用户
        servletRegistrationBean.addInitParameter("loginUsername", "admin");
        servletRegistrationBean.addInitParameter("loginPassword", "123456");
        //是否能够重置数据 禁用HTML页面上的“Reset All”功能
        servletRegistrationBean.addInitParameter("resetEnable", "false");
        return servletRegistrationBean;
    }
}
