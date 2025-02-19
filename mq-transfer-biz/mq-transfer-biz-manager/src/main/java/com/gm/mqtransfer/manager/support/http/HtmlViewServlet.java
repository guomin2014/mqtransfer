package com.gm.mqtransfer.manager.support.http;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@SuppressWarnings("serial")
public class HtmlViewServlet extends ResourceServlet{
	
	private static final Log LOG = LogFactory.getLog(HtmlViewServlet.class);
	
	public static final String PARAM_NAME_RESET_ENABLE = "resetEnable";

	public HtmlViewServlet() {
		super("META-INF/resources");
	}
	
	public void init() throws ServletException {
        super.init();
    }
	
	/**
     * 读取servlet中的配置参数.
     *
     * @param key 配置参数名
     * @return 配置参数值，如果不存在当前配置参数，或者为配置参数长度为0，将返回null
     */
    public String readInitParam(String key) {
        String value = null;
        try {
            String param = getInitParameter(key);
            if (param != null) {
                param = param.trim();
                if (param.length() > 0) {
                    value = param;
                }
            }
        } catch (Exception e) {
            String msg = "initParameter config [" + key + "] error";
            LOG.warn(msg, e);
        }
        return value;
    }

	@Override
	protected String process(String url) {
		return url;
	}
	
	public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String contextPath = request.getContextPath();
        String servletPath = request.getServletPath();
        String requestURI = request.getRequestURI();

        response.setCharacterEncoding("utf-8");
        
        if (contextPath == null) { // root context
            contextPath = "";
        }
        if (servletPath.equalsIgnoreCase("/ui")) {
        	String path = requestURI.substring(contextPath.length() + servletPath.length());

            if ("".equals(path)) {
                if (contextPath.equals("") || contextPath.equals("/")) {
                    response.sendRedirect("/ui/index.html");
                } else {
                    response.sendRedirect("ui/index.html");
                }
                return;
            }

            if ("/".equals(path)) {
                response.sendRedirect("index.html");
                return;
            }
        }
        super.service(request, response);
    }

}
