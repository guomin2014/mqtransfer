package com.gm.mqtransfer.provider.facade.util;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 获取本机有效IP地址
 * 
 * @author GM
 *
 */
public class NetUtils {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(NetUtils.class);
	private static final String LOCALHOST_IP = "127.0.0.1";
	private static final String EMPTY_IP = "0.0.0.0";
	/**
	 * 虚拟网卡
	 */
	private static final String virtualNetworkName = "lo:";
	/**
	 * 需要过滤的IP
	 */
	private static final List<String> FILTER_IP;

	private static final Pattern IP_PATTERN = Pattern.compile("[0-9]{1,3}(\\.[0-9]{1,3}){3,}");

	public static final String Network_Name = getNetworkName();

	private static String hostIP;

	static {
		FILTER_IP = new ArrayList<>();
		FILTER_IP.add("192.168.122.1");
		FILTER_IP.add("169.254.95.120");
		FILTER_IP.add("169.254.95.118");
		FILTER_IP.add("192.168.90.15");
		//FILTER_IP.add("192.168.90.247");
		FILTER_IP.add("172.17.0.1");
		FILTER_IP.add(LOCALHOST_IP);
		FILTER_IP.add(EMPTY_IP);


	}

	/**
	 * 获取本机IP 当第一次调用时通过服务器获取IP信息，后续调用直接返回第一获取的IP信息
	 *
	 * @return 本机IP
	 */
	public static String getLocalHostIP() {
		if (hostIP == null) {
			hostIP = getCurrentLocalHostIP();
		}
		return hostIP;
	}

	/**
	 * 获取本机IP 每次获取都会访问服务器网卡信息，不建议频繁调用 非特殊需求建议调用getLocalHostIP
	 *
	 * @return 本机IP
	 */
	public static String getCurrentLocalHostIP() {
		String localIP = null;
		try {
			InetAddress localAddress = InetAddress.getLocalHost();
			if (isValidHostAddress(localAddress)) {
				localIP = localAddress.getHostAddress();
			}
		} catch (Throwable e) {
			LOGGER.warn("Failed to get ip address, " + e.getMessage());
		}

		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			if (interfaces != null) {
				List<String> hostIPList = new ArrayList<>();
				while (interfaces.hasMoreElements()) {
					try {
						NetworkInterface network = interfaces.nextElement();
						String networkName = network.getName();
						if (virtualNetworkName.startsWith(networkName)){
							LOGGER.info("当前网卡是虚拟网卡,networkName is :{}",networkName);
							continue;
						}
						Enumeration<InetAddress> addresses = network.getInetAddresses();
						if (addresses != null) {
							while (addresses.hasMoreElements()) {
								try {
									InetAddress address = addresses.nextElement();
									if (isValidHostAddress(address)) {
										hostIPList.add(address.getHostAddress());
									}
								} catch (Throwable e) {
									LOGGER.warn("Failed to get network card ip address. cause:" + e.getMessage());
								}
							}
						}
					} catch (Throwable e) {
						LOGGER.warn("Failed to get network card ip address. cause:" + e.getMessage());
					}
				}
				if (localIP != null && hostIPList.contains(localIP)) {
					return localIP;
				} else {
					if (hostIPList.isEmpty()) {
						return LOCALHOST_IP;
					} else {
						return hostIPList.get(0);
					}
				}
			}
		} catch (IOException e) {
			LOGGER.error("Failed to get network card ip address. cause:" + e.getMessage());
		}

		return localIP;
	}

	/**
	 * 验证IP是否真实IP
	 * 
	 * @param address
	 * @return 是否是真实IP
	 */
	private static boolean isValidHostAddress(InetAddress address) {
		if (address == null || address.isLoopbackAddress()) {
			return false;
		}
		String name = address.getHostAddress();
		return isValidHostAddress(name);
	}

	private static boolean isValidHostAddress(String name) {

		if (FILTER_IP.contains(name)) {
			return false;
		}
		return  IP_PATTERN.matcher(name).matches();
	}

	private static Map<String, String> getHostNames() throws SocketException {
		Map<String, String> hostNames = new HashMap<>();
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		while (interfaces.hasMoreElements()) {
			NetworkInterface network = interfaces.nextElement();
			LOGGER.debug("networkInterface name is:{},displayName is:{},isVirtual:{} ", network.getName(),
					network.getDisplayName(), network.isVirtual());
			Enumeration<InetAddress> addresses = network.getInetAddresses();
			if (addresses != null && !network.isVirtual()) {
				while (addresses.hasMoreElements()) {
					try {
						InetAddress address = addresses.nextElement();
						String ip = address.getHostAddress();

						if ("127.0.0.1".equals(ip) || "Inet6Address".equals(address.getClass().getSimpleName())) {
							LOGGER.debug(
									"IP address is not valid,host address is:{},class name is:{},networkInterface name is:{}",
									address.getHostAddress(), address.getClass().getSimpleName(), network.getName());
						} else {
							// 只取Inet4Address的地址
							LOGGER.info(
									"Get to the valid IP address,host address is:{},class name is:{},networkInterface name is:{}",
									address.getHostAddress(), address.getClass().getSimpleName(), network.getName());
							if (ip != null && !"".equals(ip.trim())){
								hostNames.put(network.getName(), ip);	
							}
							// return address.getHostAddress();
						}
					} catch (Throwable e) {
						LOGGER.warn("Failed to get network card ip address. cause:{}", e.getMessage());
					}
				}
			}
		}
		return hostNames;
	}

	public static String getInet4Address() {
		String hostName = null;
		try {
			InetAddress localAddress = InetAddress.getLocalHost();
			hostName = localAddress.getHostAddress();
		} catch (Exception e) {
			LOGGER.warn("get localhost ip on error:{}", e.getMessage());
		}
		try {
			Map<String, String> hostNames = getHostNames();
			if (hostNames.size() > 0) {
				//物理机有可能有bond0
				if (hostNames.containsKey(network_name_bond0)) {
					return hostNames.get(network_name_bond0);
				}
				//当前物理机eth1一般是业务IP
				if (hostNames.containsKey(network_name_eth1)) {
					return hostNames.get(network_name_eth1);
				}
				//虚拟机，容器eth0一般是业务IP
				if (hostNames.containsKey(network_name_eth0)){
					return hostNames.get(network_name_eth0);
				}
				final String p = "172.";
				for(Map.Entry<String,String> host : hostNames.entrySet()){
					String ip = host.getValue();
					//网卡名称
					String networkName = host.getKey();
					if (!ip.startsWith(p)
							&& !FILTER_IP.contains(ip)
							&& !virtualNetworkName.startsWith(networkName)){
						return ip;
					}
				}
			} else {
				return hostName;
			}

		} catch (Exception e1) {
			LOGGER.warn("Error getting IP address:{}", e1.getMessage());
		}
		return hostName;
	}

	private static final String network_name_bond0 = "bond0";

	private static final String network_name_eth0 = "eth0";

	private static final String network_name_eth1 = "eth1";

	private static final String network_name_eth2 = "eth2";

	public static String getNetworkName() {

		String networkName = network_name_eth0;
		try {

			Map<String, String> hostNames = getHostNames();

			if (hostNames.size() > 0) {
				if (hostNames.containsKey(network_name_bond0)) {
					return network_name_bond0;
				} else if (hostNames.containsKey(network_name_eth0)) {
					return network_name_eth0;
				} else if (hostNames.containsKey(network_name_eth1)) {
					return network_name_eth1;
				} else if (hostNames.containsKey(network_name_eth2)) {
					return network_name_eth2;
				} else {
					for (String name : hostNames.keySet()) {
						return name;
					}
				}
			} else {
				return networkName;
			}
		} catch (Exception e1) {
			LOGGER.warn("Error getting IP address:{}", e1.getMessage());
		}
		return networkName;
	}
	
	/**
	 * 获取当前进程的PID
	 */
	public static int getPid() {
		String name = ManagementFactory.getRuntimeMXBean().getName();
		try {
			return Integer.parseInt(name.substring(0, name.indexOf('@')));
		} catch (Exception e) {
			return -1;
		}
	}
}
