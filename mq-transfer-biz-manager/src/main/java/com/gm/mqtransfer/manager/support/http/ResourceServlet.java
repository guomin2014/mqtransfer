package com.gm.mqtransfer.manager.support.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gm.mqtransfer.manager.support.http.util.IPAddress;
import com.gm.mqtransfer.manager.support.http.util.IPRange;
import com.gm.mqtransfer.manager.support.http.util.Utils;
import com.gm.mqtransfer.provider.facade.util.StringUtils;


public abstract class ResourceServlet extends HttpServlet {
	
	private static final Log LOG = LogFactory.getLog(ResourceServlet.class);

	public static final String SESSION_USER_KEY = "transfer-user";
	public static final String PARAM_NAME_USERNAME = "loginUsername";
	public static final String PARAM_NAME_PASSWORD = "loginPassword";
	public static final String PARAM_NAME_ALLOW = "allow";
	public static final String PARAM_NAME_DENY = "deny";
	public static final String PARAM_REMOTE_ADDR = "remoteAddress";

	protected final ResourceHandler handler;

    public ResourceServlet(String resourcePath) {
        handler = new ResourceHandler(resourcePath);
    }
    
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String servletPath = request.getServletPath();
        handler.service(request, response, servletPath, new ProcessCallback() {
            @Override
            public String process(String url) {
                return ResourceServlet.this.process(url);
            }
        });
    }
    
    public void init() throws ServletException {
        initAuthEnv();
    }
    
    private void initAuthEnv() {
        String paramUserName = getInitParameter(PARAM_NAME_USERNAME);
        if (!StringUtils.isEmpty(paramUserName)) {
            handler.username = paramUserName;
        }

        String paramPassword = getInitParameter(PARAM_NAME_PASSWORD);
        if (!StringUtils.isEmpty(paramPassword)) {
            handler.password = paramPassword;
        }

        String paramRemoteAddressHeader = getInitParameter(PARAM_REMOTE_ADDR);
        if (!StringUtils.isEmpty(paramRemoteAddressHeader)) {
            handler.remoteAddressHeader = paramRemoteAddressHeader;
        }

        try {
            String param = getInitParameter(PARAM_NAME_ALLOW);
            if (param != null && param.trim().length() != 0) {
                param = param.trim();
                String[] items = param.split(",");

                for (String item : items) {
                    if (item == null || item.length() == 0) {
                        continue;
                    }

                    IPRange ipRange = new IPRange(item);
                    handler.allowList.add(ipRange);
                }
            }
        } catch (Exception e) {
            String msg = "initParameter config error, allow : " + getInitParameter(PARAM_NAME_ALLOW);
            LOG.error(msg, e);
        }

        try {
            String param = getInitParameter(PARAM_NAME_DENY);
            if (param != null && param.trim().length() != 0) {
                param = param.trim();
                String[] items = param.split(",");

                for (String item : items) {
                    if (item == null || item.length() == 0) {
                        continue;
                    }

                    IPRange ipRange = new IPRange(item);
                    handler.denyList.add(ipRange);
                }
            }
        } catch (Exception e) {
            String msg = "initParameter config error, deny : " + getInitParameter(PARAM_NAME_DENY);
            LOG.error(msg, e);
        }
    }
    
    protected abstract String process(String url);

    public static interface ProcessCallback {
        String process(String url);
    }
    
    
    public static class ResourceHandler {
        protected String username;
        protected String password;

        protected List<IPRange> allowList = new ArrayList<IPRange>();
        protected List<IPRange> denyList = new ArrayList<IPRange>();

        protected String resourcePath;

        protected String remoteAddressHeader;

        public ResourceHandler(String resourcePath) {
            this.resourcePath = resourcePath;
        }

        protected void returnResourceFile(String fileName, String uri, HttpServletResponse response)
                throws ServletException,
                IOException {
            String filePath = getFilePath(fileName);

            if (filePath.endsWith(".html")) {
                response.setContentType("text/html; charset=utf-8");
            }
            if (fileName.endsWith(".jpg")) {
                byte[] bytes = Utils.readByteArrayFromResource(filePath);
                if (bytes != null) {
                    response.getOutputStream().write(bytes);
                }

                return;
            }

            String text = Utils.readFromResource(filePath);
            if (text == null) {
                return;
            }

            if (fileName.endsWith(".css")) {
                response.setContentType("text/css;charset=utf-8");
            } else if (fileName.endsWith(".js")) {
                response.setContentType("text/javascript;charset=utf-8");
            }
            response.getWriter().write(text);
        }

        protected String getFilePath(String fileName) {
            return resourcePath + fileName;
        }

        public boolean checkLoginParam(HttpServletRequest request) {
            String usernameParam = request.getParameter(PARAM_NAME_USERNAME);
            String passwordParam = request.getParameter(PARAM_NAME_PASSWORD);
            if (null == username || null == password) {
                return false;
            } else if (username.equals(usernameParam) && password.equals(passwordParam)) {
                return true;
            }
            return false;
        }

        protected String getRemoteAddress(HttpServletRequest request) {
            String remoteAddress = null;

            if (remoteAddressHeader != null) {
                remoteAddress = request.getHeader(remoteAddressHeader);
            }

            if (remoteAddress == null) {
                remoteAddress = request.getRemoteAddr();
            }

            return remoteAddress;
        }

        public boolean containsUser(HttpServletRequest request) {
            HttpSession session = request.getSession(false);
            return session != null && session.getAttribute(SESSION_USER_KEY) != null;
        }

        public boolean isRequireAuth() {
            return username != null;
        }

        public boolean isPermittedRequest(HttpServletRequest request) {
            String remoteAddress = getRemoteAddress(request);
            return isPermittedRequest(remoteAddress);
        }

		public boolean isPermittedRequest(String remoteAddress) {
			boolean ipV6 = remoteAddress != null && remoteAddress.indexOf(':') != -1;
			if (ipV6) {
				return "0:0:0:0:0:0:0:1".equals(remoteAddress) || (denyList.isEmpty() && allowList.isEmpty());
			}
			IPAddress ipAddress = new IPAddress(remoteAddress);
			for (IPRange range : denyList) {
				if (range.isIPAddressInRange(ipAddress)) {
					return false;
				}
			}
			if (allowList.size() > 0) {
				for (IPRange range : allowList) {
					if (range.isIPAddressInRange(ipAddress)) {
						return true;
					}
				}
				return false;
			}
			return true;
		}

        public void service(HttpServletRequest request, HttpServletResponse response, String servletPath, ProcessCallback processCallback) throws ServletException, IOException {
            String contextPath = request.getContextPath();
            String requestURI = request.getRequestURI();

            response.setCharacterEncoding("utf-8");

            if (contextPath == null) { // root context
                contextPath = "";
            }
            String uri = contextPath + servletPath;
            String path = requestURI.substring(contextPath.length() + servletPath.length());

            if (!isPermittedRequest(request)) {
                path = "/nopermit.html";
                returnResourceFile(path, uri, response);
                return;
            }

            if ("/submitLogin".equals(path)) {
                String usernameParam = request.getParameter(PARAM_NAME_USERNAME);
                String passwordParam = request.getParameter(PARAM_NAME_PASSWORD);
                if (username.equals(usernameParam) && password.equals(passwordParam)) {
                    request.getSession().setAttribute(SESSION_USER_KEY, username);
                    response.getWriter().print("success");
                } else {
                    response.getWriter().print("error");
                }
                return;
            }

            if (isRequireAuth() //
                    && !containsUser(request)//
                    && !checkLoginParam(request)//
                    && !("/login.html".equals(path) //
                    || path.startsWith("/css")//
                    || path.startsWith("/js") //
                    || path.startsWith("/img"))) {
                if (contextPath.equals("") || contextPath.equals("/")) {
                    response.sendRedirect("/ui/login.html");
                } else {
                    if ("".equals(path)) {
                        response.sendRedirect("ui/login.html");
                    } else {
                        response.sendRedirect("login.html");
                    }
                }
                return;
            }

            if ("".equals(path) || "/".equals(path)) {
                returnResourceFile("/index.html", uri, response);
                return;
            }

            if (path.contains(".json")) {
                String fullUrl = path;
                if (request.getQueryString() != null && request.getQueryString().length() > 0) {
                    fullUrl += "?" + request.getQueryString();
                }
                response.getWriter().print(processCallback.process(fullUrl));
                return;
            }

            // find file in resources path

            returnResourceFile(path, uri, response);
        }

    }
}
