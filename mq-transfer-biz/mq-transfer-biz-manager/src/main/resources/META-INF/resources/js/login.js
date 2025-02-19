(function($) { 
	var path = window.location.pathname;
	if (!path.endsWith("/ui/login.html")) {
		var paths = path.split("/");
		var contextPath;
		for (var i = 0; i < paths.length; i++) {
			if (paths[i] != null && paths[i].length > 0) {
				contextPath = paths[i];
				break;
			}
		}
		if (contextPath == "ui") {
			contextPath = "";
		} else {
			contextPath = "/" + contextPath;
		}
		window.location.href = contextPath + "/ui/login.html"
	}
	
})(jQuery);

$("#btnLogin").on("click", function(){
	var userName = $("#loginUsername").val();
	var password = $("#loginPassword").val();
	if (userName.length == 0) {
		$("#alertInfo").text("UserName is empty!")
		$("#alertInfo").show();
		return;
	}
	if (password.length == 0) {
		$("#alertInfo").text("Password is empty!")
		$("#alertInfo").show();
		return;
	}
	$.ajax({
        type: 'POST',
        url: "submitLogin",
        data: $("#loginForm").serialize(),
        success: function (data) {
            if ("success" == data)
                location.href = "index.html";
            else {
            	$("#alertInfo").text("The username or password you entered is incorrect.")
                $("#alertInfo").show();
                $("#loginForm")[0].reset();
                $("input[name=userName]").focus();
            }
        },
        dataType: "text"
    });
})