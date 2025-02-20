package com.gm.mqtransfer.manager.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.gm.mqtransfer.manager.support.helix.HelixMirrorMakerManager;


@RestController
public class IndexController {
	
	@Autowired
	private HelixMirrorMakerManager helixMirrorMakerManager;
	
	@RequestMapping(value = "/home", method = RequestMethod.GET)
	public String home() {
		return "home";
	}
	
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String index(HttpServletRequest request, HttpServletResponse response) {
		try {
			response.sendRedirect("ui/index.html");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "index";
	}
	
	@RequestMapping(value = "/rebalance", method = RequestMethod.GET)
	public String rebalance() {
		helixMirrorMakerManager.rebalanceByClusterTag();
		return "success";
	}
	
}
