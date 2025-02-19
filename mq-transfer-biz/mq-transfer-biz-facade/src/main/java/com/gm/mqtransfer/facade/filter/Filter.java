package com.gm.mqtransfer.facade.filter;

import com.gm.mqtransfer.facade.model.CustomMessage;

public interface Filter {
	public boolean doFilter(CustomMessage msg);
}
