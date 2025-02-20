var MSG_ERROR = "error";
var MSG_SUCCESS = "success";
var MSG_INFO = "info";
var MSG_WARN = "warn";
/**
 * 判断字符串是否为空
 * @param str
 * @returns
 */
function isEmpty(str) {
	if(str == null || typeof str == "undefined") {
		return true;
	}
	if ( str == "" ) {
		return true;
	}
	var regu = "^[\s]+$";
	var re = new RegExp(regu);
	return re.test(str);
}
function showWaiting(target) {
    if (target == null || typeof target == "undefined") {
    	target = "body"
    }
//    $(target).loading();
}

function hideWaiting(target) {
	if (target == null || typeof target == "undefined") {
    	target = "body"
    }
//	$(target).loading('stop');
}
/**
 * 显示提示消息
 * @param message
 * @param msgType
 * @param displayTime
 */
function showAlert(message, msgType, displayTime)
{
	var alertType = "";
	if(msgType == MSG_ERROR)
	{
		alertType = "alert-warning";
	}
	else if(msgType == MSG_SUCCESS)
	{
		alertType = "alert-success";
	}
	else if(msgType == MSG_INFO)
	{
		alertType = "alert-info";
	}
	else if(msgType == MSG_WARN)
	{
		alertType = "alert-warning";
	}
	$(".alert").slideUp("slow");
	$(".alert").remove();
	var showMessageDiv = $(".message-info").length > 0;
	var messageDiv = "<div class='alert " + alertType + " fade show'><button data-dismiss='alert' class='close' type='button'>×</button><strong>" + message + "</strong></div>";
 	if(showMessageDiv)
 	{
	 	$(".message-info").first().append(messageDiv);
 	}
 	else
 	{
 		$("body").prepend(messageDiv);
 	}
 	if(displayTime == 0)
 	{
 		$('.alert').slideDown("slow");
 	}
 	else
 	{
	 	//$('.alert').slideDown("slow").delay(displayTime * 1000).slideUp("slow");
 		$('.alert').slideDown("slow");
 		setTimeout("hideAlert()", displayTime * 1000);
 	}
}

function hideAlert()
{
	$('.alert').slideUp("slow");
}
function ajaxRequest(url, data, successFun, errorFun) {
	if (typeof data == 'undefined' || data == null || isEmpty(data)) {
		data = {}
	}
	$.ajax({
		type: "post",
		dataType: "json",
		contentType: "application/json",
		cache: false,
		url : url,
		data : JSON.stringify(data),
//		statusCode: {
//			404: function() {
//				showAlert("页面未找到")
//			},
//			302: function(response) {
//				console.log("302--->", response)
//			}
//		},
		complete: function() {
			hideWaiting();
		},
		beforeSend: function () {
			showWaiting();
		},
		success: function (data) {
			if (data && data.success) {
				if(successFun && typeof successFun != "undefined")
	             {
					 if(typeof successFun == "function") {
						 successFun.call(this, data.data);
					 } else {
						 window[successFun](data.data);
					 }
	             }
			} else {
				if(errorFun && typeof errorFun != "undefined")
	            {
					 if(typeof errorFun == "function") {
						 errorFun.call(this, null, data.msg, null);
					 } else {
						 window[errorFun](null, data.msg, null);
					 }
	            } else {
	            	showAlert(data.msg);
	            }
			}
		},
		error: function (XMLHttpRequest, textStatus, errorThrown) {
			console.log("textStatus:" + textStatus)
			hideWaiting();
			if(errorFun && typeof errorFun != "undefined")
            {
				 if(typeof errorFun == "function") {
					 errorFun.call(this, XMLHttpRequest, textStatus, errorThrown);
				 } else {
					 window[errorFun](XMLHttpRequest, textStatus, errorThrown);
				 }
            }
		}
	});
}

function formSerializeArray(form) {
	form.find("input,select").removeAttr("disabled");
	var formData = form.serializeArray();
	var jsonData = {};
	$(formData).each(function(index, item){
		var fieldName = item.name;
		var fieldValue = item.value;
		if (fieldValue == "") {
			return;
		}
		var fieldNames = [];
		if (fieldName.indexOf(".") != -1) {
			fieldNames = fieldName.split(".");
		} else {
			fieldNames.push(fieldName);
		}
		var currObj = jsonData;
		for (var i = 0; i < fieldNames.length; i++) {
			var name = fieldNames[i];
			if (i == fieldNames.length - 1) {//当前是最后一项
				if (currObj[name]) {
					currObj[name] = currObj[name] + "," + fieldValue;
        		} else {
        			currObj[name] = fieldValue;
        		}
			} else {
				if (currObj[name]) {
					currObj = currObj[name];
				} else {
					currObj[name] = {};
					currObj = currObj[name];
				}
			}
		}
	})
	return jsonData;
}

function formFillValue(form, item, parentKey) {
	if (form && item) {
		//填充值
		  for (key in item) {
			  var pkey = key;
			  if (typeof parentKey != "undefined") {
				  pkey = parentKey + "." + key;
			  }
			  var value = item[key];
			  if (value !== null && typeof value === 'object') {
				  formFillValue(form, value, pkey);
				  continue;
			  }
			  var eleName = pkey;
			  var ele = $(form).find("[name='" + eleName + "']");
			  if (ele && ele.length > 0) {
				  var eleType = ele.attr("type");
				  if (typeof eleType == "undefined") {
					  eleType = ele.prop("nodeName");
				  }
				  eleType = eleType.toLowerCase();
				  if (eleType == 'radio' || eleType == 'checkbox') {
					  if (value != null) {
						  var values = value.toString().split(",");
							ele.each(function(index, e){
								$(values).each(function(index, vv){
									if ($(e).val().toString() == vv.toString()) {
										$(e).prop("checked","checked");
									}
								});
							})
					  }
				  } else {
					  ele.val(value);
				  }
				  if (eleType == 'select') {
					  ele.trigger("change");
				  }
			  }
		  }
	}
}

function formatByte(value) {
	if (value == null || typeof value == "undefined") {
		return "0 Byte";
	}
	if (value >= 1073741824) {
		return (value / 1073741824).toFixed(1) + " GB";
	} else if (value >= 1048576) {
		return (value / 1048576).toFixed(1) + " MB";
	} else if (value >= 1024) {
		return (value / 1024).toFixed(1) + " KB";
	} else {
		return value + " Byte";
	}
}