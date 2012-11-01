<%@page import="ch.ethz.inf.vs.californium.endpoint.resources.RDNodeResource"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
    
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ page import="ch.ethz.inf.vs.californium.examples.AdminTool" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.text.DecimalFormat" %>     

<% 
	AdminTool at = AdminTool.getInstance();
%>

    
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<link type="text/css" href="css/ui-lightness/jquery-ui-1.9.0.custom.css" rel="stylesheet" />
<link type="text/css" href="css/jquery.dataTables_themeroller.css" rel="stylesheet" />
<script type="text/javascript" src="js/jquery-1.8.2.js"></script>
<script type="text/javascript" src="js/jquery-ui-1.9.0.custom.min.js"></script>
<script type="text/javascript" src="js/dygraph-combined.js"></script>
<script type="text/javascript" src="js/jquery.dataTables.min.js"></script>
<script type="text/javascript" src="js/jquery-timing.min.js"></script>



<title>Home</title>
<style type="text/css">
	div.tabouter{
		margin: 12px 0px;
		overflow:auto;
		}
	div.tableft {
		float:left;
		width:45%;
		}
	div.tabrefresh{
		float:left;
		text-align:center;
		width:12%;
		}
		
	div.tabset{
		float:right;
		text-align:center;
		width:12%;
		}
			
	div.sensorvalue, div.lastseenvalue, div.lastrssivalue, div.uptimevalue, div.versionvalue, div.lossratevalue{
		float:left;
		width:30%;
		text-align:right;
		}
		
	.endpointitem_inactive{
		background-color: rgba(255,0,0,0.4) !important
		}
		
	div.configvalue, div.setvalue, div.unsortedvalue{
		float:left;
		width:30%;
		text-align:right;
		}
	.redColor{
		background-color: rgba(255,0,0,0.4)
		}
	.yellowColor{
		background-color: rgba(255,255,0,0.4)
		}	
	.greenColor{
		background-color: rgba(0,255,0,0.4)
		}	
	
	.center {
		text-align: center; 
		}
	.warning {
		font-size: +2;
		color: red;
		}

	
</style>

<script type="text/javascript">

$.fn.dataTableExt.oApi.fnReloadAjax = function ( oSettings, sNewSource, fnCallback, bStandingRedraw )
{
    if ( typeof sNewSource != 'undefined' && sNewSource != null ) {
        oSettings.sAjaxSource = sNewSource;
    }
 
    // Server-side processing should just call fnDraw
    if ( oSettings.oFeatures.bServerSide ) {
        this.fnDraw();
        return;
    }
 
    this.oApi._fnProcessingDisplay( oSettings, true );
    var that = this;
    var iStart = oSettings._iDisplayStart;
    var aData = [];
  
    this.oApi._fnServerParams( oSettings, aData );
      
    oSettings.fnServerData.call( oSettings.oInstance, oSettings.sAjaxSource, aData, function(json) {
        /* Clear the old information from the table */
        that.oApi._fnClearTable( oSettings );
          
        /* Got the data - add it to the table */
        var aData =  (oSettings.sAjaxDataProp !== "") ?
            that.oApi._fnGetObjectDataFn( oSettings.sAjaxDataProp )( json ) : json;
          
        for ( var i=0 ; i<aData.length ; i++ )
        {
            that.oApi._fnAddData( oSettings, aData[i] );
        }
          
        oSettings.aiDisplay = oSettings.aiDisplayMaster.slice();
          
        if ( typeof bStandingRedraw != 'undefined' && bStandingRedraw === true )
        {
            oSettings._iDisplayStart = iStart;
            that.fnDraw( false );
        }
        else
        {
            that.fnDraw();
        }
          
        that.oApi._fnProcessingDisplay( oSettings, false );
          
        /* Callback user function - for event handlers etc */
        if ( typeof fnCallback == 'function' && fnCallback != null )
        {
            fnCallback( oSettings );
        }
    }, oSettings );
};
	
	
	
jQuery.fn.dataTableExt.oSort['date-norm-asc'] = function(a, b) {
	var x = new Date(a.replace(" ","T")).getTime();
	var y = new Date(b.replace(" ","T")).getTime();
    var z = ((x < y) ? -1 : ((x > y) ? 1 : 0));
    return z;
};
 
jQuery.fn.dataTableExt.oSort['date-norm-desc'] = function(a, b) {
	var y = new Date(a.replace(" ","T")).getTime();
	var x = new Date(b.replace(" ","T")).getTime();
    var z = ((x < y) ? -1 : ((x > y) ? 1 : 0));
    return z;
};


jQuery.fn.dataTableExt.oSort['percent-asc'] = function(a, b) {
	var x = parseFloat(a);
	var y = parseFloat(b);
    var z = ((x < y) ? -1 : ((x > y) ? 1 : 0));
    return z;
};
 
jQuery.fn.dataTableExt.oSort['percent-desc'] = function(a, b) {
	var y = parseFloat(a);
	var x = parseFloat(b);
    var z = ((x < y) ? -1 : ((x > y) ? 1 : 0));
    return z;
};


	function setupRefresh(){
		updateValuesMain();
		setInterval("updateValuesMain();",300000);
		setInterval("updateMain();",30000);
	}
	
	function updateValuesMain(){
		oTable.fnReloadAjax('query/table');
		return false;
	}
		
	function updateMain(){
		$('td.lastseenvalue').each($).wait(500,function() {
			var current = $(this);
			var id=current.parent().attr('id');
			if(current.parent().hasClass("endpointitem")){
				current.load('query/value?id='+id+'&type=lastseenvalue',function(){
				
					var value = current.text().replace(" ","T");
					current.removeClass("redColor greenColor yellowColor");
					
					if(new Date(value).getTime() < new Date().getTime()-1000*3600*1){
						current.addClass("redColor");
					}
					else if(new Date(value).getTime() < new Date().getTime()-1000*300){
						current.addClass("yellowColor");
					}
					else{
						current.addClass("greenColor");
					}
				});
			}
			else if(current.parent().hasClass("endpointitem_inactive")){
				current.removeClass("redColor greenColor yellowColor");
			}
		});
		$('td.lossratevalue').each($).wait(500,function() {
			var current = $(this);
			var id=current.parent().attr('id');
			if(current.parent().hasClass("endpointitem")){
				current.load('query/value?id='+id+'&type=lossratevalue',function(){
			
					current.removeClass("redColor greenColor yellowColor");
					
					if(parseFloat(current.text()) < 10){
						current.addClass("greenColor");
					}
					else if(parseFloat(current.text()) < 50){
						current.addClass("yellowColor");
					}
					else if(parseFloat(current.text()) < 100){
						current.addClass("redColor");
					}
					else{
						current.addClass("yellowColor");
					}
				});
			}
			else if(current.parent().hasClass("endpointitem_inactive")){
				current.removeClass("redColor greenColor yellowColor");
			}
			
		});
		return false;
	}
	
	
	function updateMainColor(){
		$('td.lastseenvalue').each(function() {
			var current = $(this);
			if(current.parent().hasClass("endpointitem")){
						
				var value = current.text().replace(" ","T");
				current.removeClass("redColor greenColor yellowColor");
				
				if(new Date(value).getTime() < new Date().getTime()-1000*3600*1){
					current.addClass("redColor");
				}
				else if(new Date(value).getTime() < new Date().getTime()-1000*300){
					current.addClass("yellowColor");
				}
				else{
					current.addClass("greenColor");
				}
			}
			else if(current.parent().hasClass("endpointitem_inactive")){
				current.removeClass("redColor greenColor yellowColor");
			}
		});
		$('td.lossratevalue').each(function() {
			var current = $(this);
			if(current.parent().hasClass("endpointitem")){
				
				current.removeClass("redColor greenColor yellowColor");
			
				if(parseFloat(current.text()) < 10){
					current.addClass("greenColor");
				}
				else if(parseFloat(current.text()) < 50){
					current.addClass("yellowColor");
				}
				else if(parseFloat(current.text()) < 100){
					current.addClass("redColor");
				}
				else{
					current.addClass("yellowColor");
				}
			}
			else if(current.parent().hasClass("endpointitem_inactive")){
				current.removeClass("redColor greenColor yellowColor");
			}
			
		});
		return false;
	}
	
	function refreshValue(button){
		var id=$(button).parent().attr('id');
		$(button).parent().find('*').each(function() {
			var place = $(this);
			var type = place.attr("class");
			if (type!=null && type.indexOf("value") >=0){
				place.html('Refreshing...');
				place.load('query/value?id='+id+'&type='+type, function(data){
					if(data.length==0){
						data="&nbsp";
					}
				});
			}
		});
		return false;
		
	}
	
	function setValue(button){
		var id=$(button).parent().attr('id');
		$(button).parent().find('*').each(function() {
			var place = $(this);
			var type = place.attr("class");
			if (type!=null && type.indexOf("value") >=0){
				var valuePrompt = prompt("Please Enter Value","");
				if(valuePrompt==""){
					return false;
				}
				$.post('query/value?id='+id+'&type='+type, {value: valuePrompt}, function(data) {
					  place.empty().append(data);
					});
				//place.load('query/value?id='+id+'&type='+type);
			}
			else if(type!=null && type.indexOf("reregister")>=0){
				$.post('query/value?id='+id+'&type='+type,function(data) {
					  alert("OK");
					});
			}
		});
		return false;
		
	}
	
	
	function updateValuesTabAll(currentEndpoint){
		currentEndpoint.find('*').each($).wait(300,function() {
					var current = $(this);
					var type = current.attr("class");
					if (type!=null && type.indexOf("value") >=0){
						var id=current.parent().attr("id");
						current.load('query/value?id='+id+'&type='+type);
					}
					
		});
		return false;
	}

	function openGraphDialog(resource){
		var node = $(resource).parents('.ui-dialog').find('.ui-dialog-title').text();
		var name = $(resource).parent('.tabouter').children('.tableft').text();
		var res = $(resource).parent().attr('id');
		var $dialog = $("<div></div>");
		var $graph = $("<div id=\"graph-"+res+"\"></div>");
		var values="";
		
		var $link = $(this).one('click', function(){
			$dialog
				.html($graph)
				.dialog({
					title: node+'/'+name,
					width: 800,
					height: 500,
					modal: true,
					draggable: false,
					resizable: false,
					close: function(){
						$dialog.remove();
					}
				});
			
		});
		var gid = "graph-"+res;
		$(document.getElementById(gid)).empty().append("Loading..");
		$.get("query/graph?id="+res, function(data){
			if(data.indexOf("\n")!=-1){
				values=data;
				g = new Dygraph(
						document.getElementById(gid),
						values
				)
			}
			else{
				$(document.getElementById(gid)).empty().append(data);
			}
			
			
		});
	}
	
	$(document).ready(function() {
		oTable = $('#endpointlist').dataTable({
	        "bJQueryUI": true,
	        "bPaginate": false,
	        "bSortClasses": false,
	        "aoColumns": [
	          			{ "sTitle": "Identifier",
	          				"sClass": "center"
	          			},
	          			{ "sTitle": "Domain",
	          				"sClass": "center"
	          			},
	          			{ "sTitle": "Endpoint Type",
	          				"sClass": "center"
	          			},
	          			{ "sTitle": "Address",
	          				"sClass": "center"
	          			},
	          			{ "sTitle": "Alive",
	          				"sClass": "center"
	          			},
	          			{ "sTitle": "Last HeartBeat",
	          				"sClass": "lastseenvalue center",
	          				"sType": "date-norm"
	          			},
	          			{ "sTitle": "Loss Rate",
	          				"sClass": "lossratevalue center",
	          				"sType": "percent"
	          			}
	          		],
	        
	        "fnRowCallback": function( nRow, aData, iDisplayIndex ) {
	            var id = aData[0];
	            $(nRow).attr("id",id);
	            var active = aData[4];
	            if (active=="true"){
	            	$(nRow).addClass("endpointitem");
	            }
	            else{
	            	$(nRow).addClass("endpointitem_inactive");
	            
	            }
	            return nRow;
	          },
			"fnDrawCallback": function( oSettings ){
				updateMainColor();
			}
	    });	
		setupRefresh();
		
	} );
	
    $('#endpointlist tbody tr.endpointitem').live("click", function(event){
    	var $dialog = $('<div></div>');
		$dialog
			.load('query/endpoint?id='+$(this).attr('id'), function(){
				$('.eptabbar').tabs();
				updateValuesTabAll($dialog);
			})
			.dialog({
				title: $(this).attr('id'),
				width: 800,
				height: 500,
				modal: true,
				draggable: false,
				resizable: false,
				close: function(){
					$dialog.remove();
				}
			});

		return false;
		
    });
    
    $('#endpointlist tbody tr.endpointitem_inactive').live("click", function(event){
    	var $dialog = $('<div></div>');
		$dialog
			.load('query/endpoint?id='+$(this).attr('id')+"&alive=false", function(){
				$('.eptabbar').tabs();
				updateValuesTabAll($dialog);
			})
			.dialog({
				title: $(this).attr('id'),
				width: 800,
				height: 500,
				modal: true,
				draggable: false,
				resizable: false,
				close: function(){
					$dialog.remove();
				}
			});

		return false;
		
    });
	
/*
	$(function() {
	    $('.endpointitem').hover(
	        function() {
	        	var old = $(this).css('background-color');
	        	var rgb = old.match(/^rgb\((\d+),\s*(\d+),\s*(\d+)\)$/);
	        	rgb[1] = rgb[1] & 0xaa;
	        	rgb[2] = rgb[2] & 0xaa;
	        	rgb[3] = rgb[3] & 0xaa;
	        	//alert(old);
	            $(this).css('background-color', 'rgb('+rgb[1]+','+rgb[2]+','+rgb[3]+')' );
	        },
	        function() {
	        	var old = $(this).css('background-color');
	        	var rgb = old.match(/^rgb\((\d+),\s*(\d+),\s*(\d+)\)$/);
	        	if (rgb[1]>0){
	        		rgb[1]= 0xff;
	        	}
	        	if (rgb[2]>0){
	        		rgb[2]= 0xff;
	        	}
	        	if (rgb[3]>0){
	        		rgb[3]= 0xff;
	        	}
	        	$(this).css('background-color', 'rgb('+rgb[1]+','+rgb[2]+','+rgb[3]+')' );
	        }
	    );
	});
*/
	
</script>
</head>
<body>
	<center>
		<h3>AdminTool Home</h3>
	</center>
	<table id="endpointlist">
	</table>



</body>
</html>

