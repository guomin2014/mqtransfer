package com.gm.mqtransfer.manager.support.http.util;

import java.io.Closeable;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLRecoverableException;
import java.sql.Statement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class JdbcUtils {
	
	private static final Log LOG = LogFactory.getLog(JdbcUtils.class);

	 public static void close(Connection x) {
	        if (x == null) {
	            return;
	        }

	        try {
	            if (x.isClosed()) {
	                return;
	            }

	            x.close();
	        } catch (SQLRecoverableException e) {
	            // skip
	        } catch (Exception e) {
	            LOG.debug("close connection error", e);
	        }
	    }

	    public static void close(Statement x) {
	        if (x == null) {
	            return;
	        }
	        try {
	            x.close();
	        } catch (Exception e) {
	            boolean printError = true;

	            if (e instanceof java.sql.SQLRecoverableException
	                    && "Closed Connection".equals(e.getMessage())
	            ) {
	                printError = false;
	            }

	            if (printError) {
	                LOG.debug("close statement error", e);
	            }
	        }
	    }

	    public static void close(ResultSet x) {
	        if (x == null) {
	            return;
	        }
	        try {
	            x.close();
	        } catch (Exception e) {
	            LOG.debug("close result set error", e);
	        }
	    }

	    public static void close(Closeable x) {
	        if (x == null) {
	            return;
	        }

	        try {
	            x.close();
	        } catch (Exception e) {
	            LOG.debug("close error", e);
	        }
	    }

	    public static void close(Blob x) {
	        if (x == null) {
	            return;
	        }

	        try {
	            x.free();
	        } catch (Exception e) {
	            LOG.debug("close error", e);
	        }
	    }

	    public static void close(Clob x) {
	        if (x == null) {
	            return;
	        }

	        try {
	            x.free();
	        } catch (Exception e) {
	            LOG.debug("close error", e);
	        }
	    }
}
