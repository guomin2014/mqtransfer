package com.gm.mqtransfer.manager.support.helix;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.apache.helix.NotificationContext;
import org.apache.helix.messaging.handling.HelixTaskResult;
import org.apache.helix.messaging.handling.MessageHandler;
import org.apache.helix.messaging.handling.MultiTypeMessageHandlerFactory;
import org.apache.helix.model.Message;
import org.apache.helix.model.Message.MessageType;

import com.google.common.collect.ImmutableList;

/**
 * 自定义消息处理器，详见：org.apache.helix.examples.BootstrapProcess
 * @author GM
 * @date 2022-06-23
 */
public class CustomMessageHandlerFactory implements MultiTypeMessageHandlerFactory {
	
	private HelixMirrorMakerManager helixMirrorManager;
	public CustomMessageHandlerFactory(HelixMirrorMakerManager helixMirrorManager) {
		this.helixMirrorManager = helixMirrorManager;
	}
	@Override
	public MessageHandler createHandler(Message message, NotificationContext context) {

		return new CustomMessageHandler(message, context);
	}

	@Override
	public List<String> getMessageTypes() {
		return ImmutableList.of(MessageType.USER_DEFINE_MSG.name());
	}

	@Override
	public void reset() {

	}

	class CustomMessageHandler extends MessageHandler {

		public CustomMessageHandler(Message message, NotificationContext context) {
			super(message, context);
		}

		@Override
		public HelixTaskResult handleMessage() throws InterruptedException {
			System.out.println("**************************handleMessage");
			String hostName;
			HelixTaskResult result = new HelixTaskResult();
			try {
				hostName = InetAddress.getLocalHost().getCanonicalHostName();
			} catch (UnknownHostException e) {
				hostName = "UNKNOWN";
			}
			String msgSubType = _message.getMsgSubType();
			if (msgSubType.equals("REQUEST_RESOURCE_ADD")) {// 添加资源
				System.out.println("SrcSessionId:" + _message.getSrcSessionId());
//				String clusterName = _message.getSrcClusterName();
				String resourceName = _message.getResourceName();
//				List<String> partitionList = _message.getPartitionNames();
//				ForwardTaskEntity task = helixMirrorManager.getTaskByResourceName(resourceName);
//				helixMirrorManager.addResourceInMirrorMaker(task, false);
			}
			result.setSuccess(true);
			return result;
		}

		@Override
		public void onError(Exception e, ErrorCode code, ErrorType type) {
			e.printStackTrace();
		}
	}
}
