//js实现方法
(function($) {  
  window.BoostrapExpand = function() { 
      var html = '<div class="modal fade modal-bootstrap-expand"  id="[Id]" tabindex="-1" role="dialog" aria-labelledby="modalLabel">' +  '<div class="modal-dialog">' +   '<div class="modal-content">' +   '<div class="modal-header">' +  '<h5 class="modal-title" id="modalLabel">[Title]</h5>' +   '<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>' +   '</div>' +   '<div class="modal-body modal-body-bootstrap-expand">' +   '<p>[Message]</p>' +   '</div>' +   '<div class="modal-footer modal-footer-bootstrap-expand">' +  '<button type="button" class="btn btn-default cancel" data-dismiss="modal">[BtnCancel]</button>' +  '<button type="button" class="btn btn-danger ok" data-dismiss="modal">[BtnOk]</button>' +  '</div>' +   '</div>' +  '</div>' +  '</div>';   
      var dialogdHtml = '<div class="modal fade modal-bootstrap-expand" id="[Id]"  tabindex="-1" role="dialog" aria-labelledby="modalLabel">' +  '<div class="modal-dialog">' +   '<div class="modal-content">' +   '<div class="modal-header">' +   '<button type="button" class="close" data-dismiss="modal"><span aria-hidden="true">×</span><span class="sr-only">Close</span></button>' +   '<h4 class="modal-title" id="modalLabel">[Title]</h4>' +   '</div>' +   '<div class="modal-body modal-body-bootstrap-expand">' +   '</div>' +   '</div>' +  '</div>' +  '</div>'; 
      var reg = new RegExp("\\[([^\\[\\]]*?)\\]", 'igm'); 
      var generateId = function() { 
          var date = new Date(); 
          return 'mdl' + date.valueOf(); 
      } 
      var init = function(options) { options = $.extend({},
          { title: "操作提示",
               message: "提示内容",
               btnok: "确定",
               btncl: "取消",
               width: 200,
               auto: false 
          },
          options || {}); 
          var modalId = generateId(); 
          var content = html.replace(reg,
          function(node, key) { 
              return { Id: modalId,
                   Title: options.title,
                   Message: options.message,
                   BtnOk: options.btnok,
                   BtnCancel: options.btncl 
              } [key]; 
          }); 
          //$('body').after(content); 
          $('body').append(content); 
          var mymodal = $('#' + modalId).modal({ width: options.width,
               backdrop: 'static' 
          }); 
          $('#' + modalId).modal('show');
          $('#' + modalId).on('hide.bs.modal',
          function(e) {
              setTimeout(function(){
                  $('body').find('#' + modalId).remove(); 
              },1000)
          }); 
          return modalId; 
      }  
      return { 
          alert: function(options) { 
              if (typeof options == 'string') { options = { message: options 
                  }; 
              } 
              var id = init(options); 
              var modal = $('#' + id); modal.find('.ok').removeClass('btn-danger').addClass('btn-primary'); modal.find('.cancel').hide();  
              return { id: id,
                  on: function(callback) { 
                      if (callback && callback instanceof Function) { modal.find('.ok').click(function() {
                              callback(true);
                          }); 
                      } 
                  },
                  hide: function(callback) { 
                      if (callback && callback instanceof Function) { modal.on('hide.bs.modal',
                          function(e) { callback(e); 
                          }); 
                      } 
                  } 
              }; 
          },
          confirm: function(options) { 
              var id = init(options); 
              var modal = $('#' + id); modal.find('.ok').removeClass('btn-primary').addClass('btn-danger'); modal.find('.cancel').show(); 
              return { 
                  id: id,
                  on: function(callback) { 
                      if (callback && callback instanceof Function) { modal.find('.ok').click(function() {
                              callback(true);
                              modal.modal('hide')
                          }); modal.find('.cancel').click(function() {
                              callback(false);
                              modal.modal('hide')
                          }); 
                      } 
                  },
                  hide: function(callback) { 
                      if (callback && callback instanceof Function) { modal.on('hide.bs.modal',
                          function(e) { callback(e); 
                          }); 
                      } 
                  } 
              }; 
          },
           dialog: function(options) { 
              options = $.extend({},{ 
                   title: 'title',
                   url: '',
                   width: 800,
                   height: 550,
                   onReady: function() {},
                   onShown: function(e) {} 
              },
              options || {}); 
              var modalId = generateId();  
              var content = dialogdHtml.replace(reg,
              function(node, key) { 
                  return { Id: modalId,
                       Title: options.title 
                  } [key]; 
              }); $('footer').after(content); 
              var target = $('#' + modalId); target.find('.modal-body').load(options.url); 
              if (options.onReady()) options.onReady.call(target); target.modal(); target.on('shown.bs.modal',
              function(e) { 
                  if (options.onReady(e)) options.onReady.call(target, e); 
              }); target.on('hide.bs.modal',
              function(e) { $('body').find(target).remove(); 
              }); 
          } 
      } 
  } ();
})(jQuery);