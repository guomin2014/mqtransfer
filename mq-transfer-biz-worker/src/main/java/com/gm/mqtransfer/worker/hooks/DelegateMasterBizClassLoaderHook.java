package com.gm.mqtransfer.worker.hooks;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import com.alipay.sofa.ark.api.ArkClient;
import com.alipay.sofa.ark.spi.model.Biz;
import com.alipay.sofa.ark.spi.service.classloader.ClassLoaderHook;
import com.alipay.sofa.ark.spi.service.classloader.ClassLoaderService;
import com.alipay.sofa.ark.spi.service.extension.Extension;

@Extension("biz-classloader-hook")
public class DelegateMasterBizClassLoaderHook implements ClassLoaderHook<Biz> {

	@Override
	public Class<?> preFindClass(String name, ClassLoaderService classLoaderService, Biz t)
			throws ClassNotFoundException {
		return null;
	}

	@Override
	public Class<?> postFindClass(String name, ClassLoaderService classLoaderService, Biz t)
			throws ClassNotFoundException {
		//按包名组织
		if (name.startsWith("com.gm.mqtransfer.facade")) {
			ClassLoader masterBizClassLoader = ArkClient.getMasterBiz().getBizClassLoader();
			return masterBizClassLoader.loadClass(name);
		}
		return null;
	}

	@Override
	public URL preFindResource(String name, ClassLoaderService classLoaderService, Biz t) {
		// 资源也委托
		if (name.startsWith("com/gm/mqtransfer/facade")) {
			ClassLoader masterBizClassLoader = ArkClient.getMasterBiz().getBizClassLoader();
			try {
				return masterBizClassLoader.getResource(name);
			} catch (Exception e) {
				return null;
			}
		}
		return null;
	}

	@Override
	public URL postFindResource(String name, ClassLoaderService classLoaderService, Biz t) {
		return null;
	}

	@Override
	public Enumeration<URL> preFindResources(String name, ClassLoaderService classLoaderService, Biz t)
			throws IOException {
		if (name.startsWith("com/gm/mqtransfer/facade")) {
			ClassLoader masterBizClassLoader = ArkClient.getMasterBiz().getBizClassLoader();
			try {
				return masterBizClassLoader.getResources(name);
			} catch (Exception e) {
				return null;
			}
		}
		return null;
	}

	@Override
	public Enumeration<URL> postFindResources(String name, ClassLoaderService classLoaderService, Biz t)
			throws IOException {
		return null;
	}

}
