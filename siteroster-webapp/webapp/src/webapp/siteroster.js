tool_obj =
{
	title: "ROSTER",
	showReset: true,

	currentMode: 0,

	modeBarElementId: "roster_mode_bar",

	// this is the site we are working on
	siteId : null,

	// these get set only from mysites - from a site, these are left null
	returnTo : null,
	siteIds : null,
	readonly: false,

	siteMembers : null,
	groups: null,

	modes:
	[
		{
			title: "Roster",
			elementId: "roster_members",
			element: null,
			navBarElementId: ["roster_members_navbar", "roster_members_navbar_top"],
			toolActionsElementId: "roster_members_actions",
			toolItemTableElementId: "roster_members_item_table",
			icon: "user_female.png",
			actions:
			[
				{title: "Add", icon: "document_add.png", click: function(){tool_obj.addUser(tool_obj);return false;}, selectRequired: null},
				{title: "Remove", icon: "remove.png", click: function(){tool_obj.removeUsers();return false;}, selectRequired: "selectMember"},
				{title: "Block", icon: "disconnect.png", click: function(){tool_obj.inactivateUsers();return false;}, selectRequired: "selectMember"},
				{title: "Unblock", icon: "connect.png", click: function(){tool_obj.activateUsers();return false;}, selectRequired: "selectMember"},
				{title: "Role", icon: "bricks.png", click: function(){tool_obj.changeRole(tool_obj);return false;}, selectRequired: "selectMember"}
			],
			headers:
			[
				{title: null, type: "checkbox", sort: false, checkboxId: "selectMember"},
				{title: "Name", type: null, sort: true},
				{title: "Section", type: null, sort: true},
				{title: "Role", type: null, sort: true},
				{title: "Status", type: null, sort: true},
				{title: "Special Access", type: null, sort: false}
			],
			navbar:
			[
				{title: "Return", icon: "return.png", access: "r", popup: "Return", click: function(){tool_obj.doReturn();}},
				{sid: "roster_nav_next0", title: "Next", icon: "next.png", iconRight: true, right: true, access: "n", popup: "Next Roster", click: function(){tool_obj.doNext(tool_obj, 0);}},
				{sid: "roster_nav_counts0", right: true, text: "1 of 1"},
				{sid: "roster_nav_prev0", title: "Prev", icon: "previous.png", right: true, access: "p", popup: "Previous Roster", click: function(){tool_obj.doPrev(tool_obj, 0);}}
			],
			start: function(obj, mode)
			{
				obj.loadMembers(obj, false);
				if (obj.returnTo == null)
				{
					clearToolNavbar(obj);
				}
				else
				{
					obj.adjustNextPrev(obj, 0);
				}
			},
			reset: function(obj, mode)
			{
				obj.loadMembers(obj, true);
			},
			stop: function(obj)
			{
				$("#roster_members_item_table").trigger("destroy");
			}
		},
		{
			title: "Groups",
			elementId: "roster_groups",
			element: null,
			navBarElementId: "roster_groups_navbar",
			toolActionsElementId: "roster_groups_actions",
			toolActionsElementPopulated: true,
			toolItemTableElementId: "roster_groups_item_table",
			icon: "group.png",
			headers:
			[
				{title: null, type: "checkbox", sort: false, checkboxId: "selectGroup"},
				{title: "Title", type: null, sort: true},
				{title: "Size", type: null, sort: false}
			],
			navbar:
			[
				{title: "Return", icon: "return.png", access: "r", popup: "Return", click: function(){tool_obj.doReturn();}},
				{sid: "roster_nav_next1", title: "Next", icon: "next.png", iconRight: true, right: true, access: "n", popup: "Next Roster", click: function(){tool_obj.doNext(tool_obj, 1);}},
				{sid: "roster_nav_counts1", right: true, text: "1 of 1"},
				{sid: "roster_nav_prev1", title: "Prev", icon: "previous.png", right: true, access: "p", popup: "Previous Roster", click: function(){tool_obj.doPrev(tool_obj, 1);}}
			],
			start: function(obj, mode)
			{
				obj.loadGroups(obj, false);
				if (obj.returnTo == null)
				{
					clearToolNavbar(obj);
				}
				else
				{
					obj.adjustNextPrev(obj, 1);
				}
			},
			reset: function(obj, mode)
			{
				obj.loadGroups(obj, true);
				if (obj.returnTo == null)
				{
					clearToolNavbar(obj);
				}
				else
				{
					obj.adjustNextPrev(obj, 1);
				}
			},
			stop: function(obj)
			{
				$("#roster_groups_item_table").trigger("destroy");
			}
		}
	],

	start: function(obj, data)
	{
		obj.siteId = data.siteId;

		obj.returnTo = data.returnTo;
		obj.siteIds = data.siteIds;

		obj.readonly = ((obj.returnTo !== undefined) && (obj.returnTo.toolMode !== undefined) && ($.isArray(obj.returnTo.toolMode)) && (obj.returnTo.toolMode[4] == "helpdesk"));
		if (obj.readonly)
		{
			obj.modes[0].actions = [];
			obj.modes[0].headers = obj.modes[0].headers.slice(1,5);
			obj.modes[1].headers = obj.modes[1].headers.slice(1,3);
			obj.modes[1].actions = [];
		}

		setTitle(obj.title);
		populateToolModes(obj);
		setupConfirm("roster_confirmRemove", "Remove Members", function(){obj.doRemove(obj);});
		setupConfirm("roster_confirmInactivate", "Block Members", function(){obj.doInactivate(obj);});
		setupConfirm("roster_confirmActivate", "Unblock Members", function(){obj.doActivate(obj);});
		setupDialog("roster_roleChange", "Done", function(){return obj.doChangeRole(obj);});
		setupDialog("roster_addUser", "Add", function(){return obj.doAddUser(obj);});
		setupAlert("roster_addUserResults");
		setupAlert("roster_removeUserResults");
		setupAlert("roster_activateUserResults");
		setupAlert("roster_inactivateUserResults");
		setupAlert("roster_changeRoleResults");
		setupAlert("roster_alertSelect");
		setupConfirm("roster_confirmDelete", "Delete Groups", function(){obj.doDelete(obj);});
		setupAlert("roster_alertSelectGroup");
		setupAlert("roster_alertNoSave");
		setupDialog("roster_editGroup", "Done", function(){return obj.doSaveGroup(obj);});
		setupAlert("roster_specialAccess_success");
		setupAlert("roster_specialAccess_failed");
		setupConfirm("roster_confirmDeleteSpecialAccess", "Delete Special Access", function(){obj.doDeleteSpecialAccess(obj);});
		$('#roster_specialAccess_days').unbind('change').change(function(){obj.validateSpecialAccess(obj, 1);return true;});
		$('#roster_specialAccess_timeLimit').unbind('change').change(function(){obj.validateSpecialAccess(obj, 2);return true;});
		$('#roster_specialAccess_timeMultiplier').unbind('change').change(function(){obj.validateSpecialAccess(obj, 3);return true;});
        $('#roster_specialAccess_untimed').unbind('change').change(function(){obj.validateSpecialAccess(obj, 3);return true;});


		startHeartbeat();

		// if we are in a site, disable the link to site setup and the site title
		if (obj.returnTo == null)
		{
			clearToolNavbar(obj);
			$("#roster_configure").addClass("e3_offstage");
			$("#roster_site_title_link").addClass("e3_offstage");
		}

		// if we are from mysites, we enable next/prev, site title and the site setup link, assume userSites is loaded (by mysites)
		else
		{
			if (!obj.readonly)
			{
				$("#roster_configure").unbind("click").click(function(){obj.configure(obj);return false;});
				$("#roster_configure").removeClass("e3_offstage");

				$("#roster_site_title_s").addClass("e3_offstage");
				$("#roster_site_title_l").removeClass("e3_offstage");
			}
			else
			{
				$("#roster_site_title_s").removeClass("e3_offstage");
				$("#roster_site_title_l").addClass("e3_offstage");
			}

			$("#roster_site_title_link").removeClass("e3_offstage");

			var site = userSites.find(data.siteId);
			obj.updateSiteTitle(obj, site.title);
			obj.adjustNextPrev(obj, 0);			
		}
	},

	stop: function(obj, save)
	{
		stopHeartbeat();
	},

	reset: function(obj)
	{
		obj.currentMode.reset(obj, obj.currentMode);
	},

	updateSiteTitle: function(obj, title)
	{
		if (obj.returnTo != null)
		{
			if (!obj.readonly)
			{
				$("#roster_site_title_l").html(title).unbind('click').click(function(){selectSite(obj.siteId); return false;});
			}
			else
			{
				$("#roster_site_title_s").html(title);				
			}
		}
	},

	doReturn: function()
	{
		var data = new Object();
		data.toolMode = tool_obj.returnTo.toolMode;
		selectStandAloneTool(tool_obj.returnTo.toolId, data);
	},

	doPrev: function(obj, mode)
	{
		// TODO: save!  or maybe this is covered by the selectMinorMode

		// find the prev
		var curPos = obj.siteIds.indexOf(obj.siteId);
		if (curPos == -1)
		{
			curPos = 0;
		}
		else if (curPos > 0)
		{
			curPos--;
		}
		else
		{
			curPos = obj.siteIds.length-1;
		}
		
		var site = userSites.find(obj.siteIds[curPos]);
		obj.siteMembers = null;
		obj.groups = null;
		obj.siteId = site.siteId;
		obj.updateSiteTitle(obj, site.title);

		selectToolMode(mode, obj);
		//obj.adjustNextPrev(obj);
	},

	doNext: function(obj, mode)
	{
		// TODO: save!  or maybe this is covered by the selectMinorMode

		// find the next
		var curPos = obj.siteIds.indexOf(obj.siteId);
		if (curPos == -1)
		{
			curPos = 0;
		}
		else if (curPos == obj.siteIds.length-1)
		{
			curPos = 0;
		}
		else
		{
			curPos++;
		}
		
		var site = userSites.find(obj.siteIds[curPos]);
		obj.siteMembers = null;
		obj.groups = null;
		obj.siteId = site.siteId;
		obj.updateSiteTitle(obj, site.title);

		selectToolMode(mode, obj);
		//obj.adjustNextPrev(obj);
	},
	
	adjustNextPrev: function(obj, mode)
	{
		var curPos = obj.siteIds.indexOf(obj.siteId);
		// to not wrap next and prev
/*			$("#roster_nav_prev" + mode).prop('disabled', true).removeClass("e3_hot").addClass("e3_disabled");
		$("#roster_nav_next" + mode).prop('disabled', true).removeClass("e3_hot").addClass("e3_disabled");

		if (curPos > 0)
		{
			$("#roster_nav_prev" + mode).prop('disabled', false).addClass("e3_hot").removeClass("e3_disabled");
		}
		if (curPos < obj.siteIds.length-1)
		{
			$("#roster_nav_next" + mode).prop('disabled', false).addClass("e3_hot").removeClass("e3_disabled");
		}
*/
		$('[sid="' + "roster_nav_counts" + mode + '"]').html((curPos+1) + " of " + obj.siteIds.length);;
		// $("#roster_nav_counts" + mode).html((curPos+1) + " of " + obj.siteIds.length);
	},

	populateMembers: function(obj, members)
	{
		var enrolled = 0;
		var added = 0;
		var blocked = 0;
		var dropped = 0;
		var tas = 0;
		var guests = 0;
		var observers = 0;
		var instructors = 0;

		if ($("#roster_members_item_table").hasClass("tablesorter")) $("#roster_members_item_table").trigger("destroy");
		$("#roster_members_item_table tbody").empty();

		$.each(members, function(index, value)
		{
			var tr = $("<tr />");
			
			if (value.status == 3)
			{
				$(tr).addClass("addedUser");
			}
			else if ((value.status == 1) || (value.status == 4))
			{
				$(tr).addClass("blockedUser");
			}

			$("#roster_members_item_table tbody").append(tr);
			
			// select box
			if (!obj.readonly)
			{
				createSelectCheckboxTd(obj, tr, "selectMember", value.userId);
			}

			// name
			var name = value.displayName;				
			if (value.iid != null)
			{
				name = name + " (" + value.iid + ")";			
			}
			else
			{
				name = name + " (" + value.eid + ")";
			}
			createTextTd(tr, name);

			// section
			createTextTd(tr, value.groupTitle);

			// role
			var role = value.role;
			if (role == "Blocked") role = "Student";
			createTextTd(tr, role);
			
			// status
			var status = "Enrolled";
			if (value.status == 1)
			{
				status = "Blocked";
				if (role == "Student") blocked++;
			}
			else if (value.status == 2)
			{
				status = "Dropped";
				if (role == "Student") dropped++;
			}
			else if (value.status == 3)
			{
				status = "Added";
				if (role == "Student") added++;
			}
			else if (value.status == 4)
			{
				status = "Blocked"; // inactive
				if (role == "Student") blocked++;
			}
			else
			{
				if (role == "Student") enrolled++;
			}

			var td = createTextTd(tr, status);
			if (value.status == 2)
			{
				$(td).addClass("droppedUser");	
			}
			
			// for actives
			if ((value.status == 0) || (value.status == 3))
			{
				if (role == "Teaching Assistant")
				{
					tas++;
				}
				else if (role == "Guest")
				{
					guests++;
				}
				else if (role == "Observer")
				{
					observers++;
				}
				else if (role == "Instructor")
				{
					instructors++;
				}
			}

			// special access
			if (!obj.readonly)
			{
				if (((value.status == 0) || (value.status == 3)) && (role == "Student"))
				{
					if (value.specialAccess == 0)
					{
						createIconTd(tr, "access_add.png", "Add Special Access", function(){return obj.doSpecialAccess(obj, value);});
					}
					else
					{
						createIconTd(tr, "access_view.png", "Replace Special Access", function(){return obj.doSpecialAccess(obj, value);});
					}
				}
				else
				{
					createTextTd(tr, "");
				}
			}
		});

		$("#roster_members_item_table").tablesorter(
		{
			headers: obj.readonly ? {0:{sorter:"text"},1:{sorter:"text"},2:{sorter:"text"},3:{sorter:"text"}} : {0:{sorter:false},1:{sorter:"text"},2:{sorter:"text"},3:{sorter:"text"},4:{sorter:"text"},5:{sorter:false}},
			sortList: obj.readonly ? [[0,0]] : [[1,0]],
			emptyTo:"zero",
			cancelSelection:false
		});
		enableSelectAllCheckbox(obj);

		updateSelectStatus(obj, "selectMember");
		
		$("#roster_members_enrolled_count").empty().text(enrolled.toString());
		$("#roster_members_enrolled_plural").empty();
		if (enrolled != 1) $("#roster_members_enrolled_plural").text("s");
		
		$("#roster_members_added_count").empty().text(added.toString());
		$("#roster_members_added_plural").empty();
		if (added != 1) $("#roster_members_added_plural").text("s");
		
		$("#roster_members_blocked_count").empty().text(blocked.toString());
		$("#roster_members_blocked_plural").empty();
		if (blocked != 1) $("#roster_members_blocked_plural").text("s");

		$("#roster_members_dropped_count").empty().text(dropped.toString());
		$("#roster_members_dropped_plural").empty();
		if (dropped != 1) $("#roster_members_dropped_plural").text("s");

		$("#roster_members_instructor_count").empty().text(instructors.toString());
		$("#roster_members_instructor_plural").empty();
		if (instructors != 1) $("#roster_members_instructor_plural").text("s");

		$("#roster_members_ta_count").empty().text(tas.toString());
		$("#roster_members_ta_plural").empty();
		if (tas != 1) $("#roster_members_ta_plural").text("s");

		$("#roster_members_guest_count").empty().text(guests.toString());
		$("#roster_members_guest_plural").empty();
		if (guests != 1) $("#roster_members_guest_plural").text("s");

		if (observers > 0)
		{
			$("#roster_members_observer_count").empty().text(observers.toString());
			$("#roster_members_observer_plural").empty();
			if (observers != 1) $("#roster_members_observer_plural").text("s");
			$("#roster_member_observer_item").removeClass("e3_offstage");
		}
		else
		{
			$("#roster_member_observer_item").addClass("e3_offstage");
		}

		adjustForNewHeight();
	},

	specialAccessMember: null,
	doSpecialAccess: function(obj, member)
	{
		obj.specialAccessMember = member;

		var params = new Object();
		params.userId = member.userId;
		params.siteId = obj.siteId;

		requestCdp("siteroster_getSpecialAccess", params, function(data)
		{
			$(".roster_specialAccess_name").empty().text(member.displayName);
			$("#roster_specialAccess_days").val(data.days);
			$("#roster_specialAccess_timeLimit").val(data.limit);
			$("#roster_specialAccess_timeMultiplier").val(data.multiplier);
			$("#roster_specialAccess_untimed").prop('checked', data.untimed);;
	
			if (data.message != null)
			{
				$("#roster_specialAccess_message_tools").empty().text(data.message);
				$("#roster_specialAccess_message").removeClass("e3_offstage");
			}
			else
			{
				$("#roster_specialAccess_message").addClass("e3_offstage");
			}

			$("#roster_specialAccess_alert_select1").addClass("e3_offstage");
			$("#roster_specialAccess_alert_multiplier").addClass("e3_offstage");
			$("#roster_specialAccess_alert_limit").addClass("e3_offstage");
			$("#roster_specialAccess_alert_days").addClass("e3_offstage");

			openDialog("roster_specialAccess", [{text:"Done", click:function()
			{
				if (obj.doSaveSpecialAccess(obj))
				{
					$("#roster_specialAccess").dialog("close");
				}
				return false;
			}},{text:"Delete", click:function()
			{
				obj.doConfirmDeleteSpecialAccess(obj);
				return false;
			}}]);
		});

		return false;
	},

	validateSpecialAccess: function(obj, code)
	{
		var rv = true;

		// 1 if days entered, assure positive integer
		if ((code == 1) || (code == 0))
		{
			$("#roster_specialAccess_alert_days").addClass("e3_offstage");
			var days = $.trim($("#roster_specialAccess_days").val());
			if (days != "")
			{
				var daysNum = parseInt(days);
				if (isNaN(daysNum) || (daysNum < 1))
				{
					$("#roster_specialAccess_alert_days").removeClass("e3_offstage");
					rv = false;
				}
				else
				{
					$("#roster_specialAccess_days").val(daysNum.toString());
				}
			}
		}

		if ((code == 2) || (code == 3) || (code == 0))
		{
			$("#roster_specialAccess_alert_select1").addClass("e3_offstage");
			$("#roster_specialAccess_alert_multiplier").addClass("e3_offstage");
			$("#roster_specialAccess_alert_limit").addClass("e3_offstage");

			var limit = $.trim($("#roster_specialAccess_timeLimit").val());
			var multiplier = $.trim($("#roster_specialAccess_timeMultiplier").val());
			var untimed = $.trim($("#roster_specialAccess_untimed:checked").val());
			
			// 2, 3 if limit and multiplier both entered, reject
			if (((limit != "") && (multiplier != ""))||((limit != "") && (untimed == "on"))||((untimed == "on") && (multiplier != "")))
			{
				$("#roster_specialAccess_alert_select1").removeClass("e3_offstage");
				rv = false;
			}

			// 2 if limit selected, assure time value (n:nn or nn:nn)
			if (limit != "")
			{
				var parts = limit.split(":");
				if (parts.length != 2)
				{
					$("#roster_specialAccess_alert_limit").removeClass("e3_offstage");
					rv = false;
				}
				else
				{
					var hoursNum = parseInt(parts[0]);
					if (isNaN(hoursNum))
					{
						$("#roster_specialAccess_alert_limit").removeClass("e3_offstage");
						rv = false;
					}
					else
					{
						var minsNum = parseInt(parts[1]);
						if (isNaN(minsNum))
						{
							$("#roster_specialAccess_alert_limit").removeClass("e3_offstage");
							rv = false;
						}
						else
						{
							if (minsNum >= 60)
							{
								var h = Math.floor(minsNum / 60);
								hoursNum += h;
								minsNum -= (h*60);
							}

							$("#roster_specialAccess_timeLimit").val(twoDigit(hoursNum) + ":" + twoDigit(minsNum));
						}
					}
				}
			}

			// 3 if multiplier selected, assure positive float > 1
			if (multiplier != "")
			{
				var multNum = parseFloat(multiplier);
				if (isNaN(multNum) || (multNum <= 1))
				{
					$("#roster_specialAccess_alert_multiplier").removeClass("e3_offstage");
					rv = false;
				}
				else
				{
					$("#roster_specialAccess_timeMultiplier").val(multNum.toString());
				}
			}
		}
		
		adjustDialogHeight();
		
		return rv;
	},

	doSaveSpecialAccess: function(obj)
	{
		if (obj.validateSpecialAccess(obj, 0))
		{
			var params = new Object();
			params.days = $.trim($("#roster_specialAccess_days").val());
			params.limit = $.trim($("#roster_specialAccess_timeLimit").val());
			params.multiplier = $.trim($("#roster_specialAccess_timeMultiplier").val());
			params.untimed = $.trim($("#roster_specialAccess_untimed:checked").val());
			
			params.userId = obj.specialAccessMember.userId;
			params.siteId = obj.siteId;
			params.command = "save";

			requestCdp("siteroster_saveSpecialAccess", params, function(data)
			{
				// reload the members
				obj.loadMembers(obj, true);
				
				if (data.message != null)
				{
					$("#roster_specialAccess_results_msg").empty().text(data.message);
					$("#roster_specialAccess_success").dialog('open');
				}
				else
				{
					$("#roster_specialAccess_failed").dialog('open');	
				}
			});

			return true;
		}

		return false;
	},

	doConfirmDeleteSpecialAccess: function(obj)
	{
		$("#roster_confirmDeleteSpecialAccess").dialog('open');

		return true;
	},

	doDeleteSpecialAccess: function(obj)
	{
		if (obj.validateSpecialAccess(obj, 0))
		{
			var params = new Object();
			params.userId = obj.specialAccessMember.userId;
			params.siteId = obj.siteId;
			params.command = "delete";

			requestCdp("siteroster_saveSpecialAccess", params, function(data)
			{
				// reload the members
				obj.loadMembers(obj, true);
				
				if (data.message != null)
				{
					$("#roster_specialAccess_results_msg").empty().text(data.message);
					$("#roster_specialAccess_success").dialog('open');
				}
				else
				{
					$("#roster_specialAccess_failed").dialog('open');	
				}

				$("#roster_specialAccess").dialog("close");
			});

			return true;
		}

		return false;
	},

	loadMembers: function(obj, force)
	{
		clearSelectAll("selectMember");

		// if we already have members, no need to re-load, unless forcing
		if ((!force) && (obj.siteMembers != null))
		{
			obj.populateMembers(obj, obj.siteMembers);
		}

		else
		{
			var params = new Object();
			params.siteId = obj.siteId;
			requestCdp("members", params, function(data)
			{
				obj.siteMembers = data.members;
				obj.populateMembers(obj, obj.siteMembers);
			});
		}
	},

	removeUsers: function()
	{
		if (anyOidsSelected("selectMember"))
		{
			// confirm
			$("#roster_confirmRemove").dialog('open');
		}
		
		else
		{
			// instruct
			$("#roster_alertSelect").dialog("open");
		}
	},

	doRemove: function(obj)
	{
		// get ids selected
		var params = new Object();
		params.userIds = collectSelectedOids("selectMember");
		params.siteId = obj.siteId;

		// if any selected
		if (params.userIds.length > 0)
		{
			requestCdp("siteroster_removeMembers", params, function(data)
			{
				// reload the members
				obj.loadMembers(obj, true);

				// confirm
				obj.removeUserResults(obj, data.results);
			});
		}
	},

	removeUserResults: function(obj, results)
	{
		$("#roster_removeUserResults_users tbody").empty();
		$.each(results, function(index, value)
		{
			var tr = $("<tr />");
			$("#roster_removeUserResults_users tbody").append(tr);

			// icon
			if (value.status == "removed")
			{
				createIconTd(tr, "publish.png", "Removed");
			}
			else if (value.status == "alreadyBlocked")
			{
				createIconTd(tr, "publish.png", "Already Blocked");
			}
			else if (value.status == "alreadyDropped")
			{
				createIconTd(tr, "publish.png", "Already Dropped");
			}
			else if (value.status == "blocked")
			{
				createIconTd(tr, "publish.png", "Blocked");
			}
			else if ((value.status == "failedInstructor") || (value.status == "failedRegistrarInstructor"))
			{
				createIconTd(tr, "user_suit.png", "Instructor");
			}
			else
			{
				createIconTd(tr, "error.png", "Problem");
			}

			// name
			if (value.displayName != undefined)
			{
				var name = value.displayName;				
				if (value.iid !== undefined) name = name + " (" + value.iid + ")";
				createTextTd(tr, name);
			}
			else
			{
				createEmptyTd(tr);
			}

			// status
			if (value.status == "removed")
			{
				createTextTd(tr, "Removed", "font-style:italic; padding:0 4px 0 4px;");
			}
			else if (value.status == "alreadyBlocked")
			{
				createTextTd(tr, "Already Blocked", "font-style:italic; padding:0 4px 0 4px;");
			}
			else if (value.status == "alreadyDropped")
			{
				createTextTd(tr, "Already Dropped", "font-style:italic; padding:0 4px 0 4px;");
			}
			else if (value.status == "blocked")
			{
				createTextTd(tr, "Blocked (Registrar users can only be removed through your campus Registrar)", "font-style:italic; padding:0 4px 0 4px;");
			}
			else if (value.status == "failedRegistrarInstructor")
			{
				createTextTd(tr, "Registrar 'Instructor' role members cannot be removed", "font-style:italic; padding:0 4px 0 4px;");
			}
			else if (value.status == "failedInstructor")
			{
				createTextTd(tr, "The sole 'Instructor' role member cannot be removed", "font-style:italic; padding:0 4px 0 4px;");
			}
			else
			{
				createTextTd(tr, "Could not be removed", "font-style:italic; padding:0 4px 0 4px;");					
			}
		});

		$("#roster_removeUserResults").dialog('open');
	},

	inactivateUsers: function()
	{
		if (anyOidsSelected("selectMember"))
		{
			// confirm
			$("#roster_confirmInactivate").dialog('open');
		}
		
		else
		{
			// instruct
			$("#roster_alertSelect").dialog("open");
		}
	},

	doInactivate: function(obj)
	{
		// get ids selected
		var params = new Object();
		params.userIds = collectSelectedOids("selectMember");
		params.siteId = obj.siteId;

		// if any selected
		if (params.userIds.length > 0)
		{
			requestCdp("siteroster_blockMembers", params, function(data)
			{
				// reload the members
				obj.loadMembers(obj, true);

				// confirm
				obj.inactivateUserResults(obj, data.results);

			});
		}
	},

	inactivateUserResults: function(obj, results)
	{
		$("#roster_inactivateUserResults_users tbody").empty();
		$.each(results, function(index, value)
		{
			var tr = $("<tr />");
			$("#roster_inactivateUserResults_users tbody").append(tr);

			// icon
			if (value.status == "blocked")
			{
				createIconTd(tr, "publish.png", "Blocked");
			}
			else if (value.status == "alreadyBlocked")
			{
				createIconTd(tr, "publish.png", "Already Blocked");
			}
			else if (value.status == "alreadyDropped")
			{
				createIconTd(tr, "publish.png", "Already Dropped");
			}
			else if ((value.status == "failedInstructor") || (value.status == "failedRegistrarInstructor"))
			{
				createIconTd(tr, "user_suit.png", "Instructor");
			}
			else
			{
				createIconTd(tr, "error.png", "Problem");
			}

			// name
			if (value.displayName != undefined)
			{
				var name = value.displayName;				
				if (value.iid !== undefined) name = name + " (" + value.iid + ")";
				createTextTd(tr, name);
			}
			else
			{
				createEmptyTd(tr);
			}

			// status
			if (value.status == "blocked")
			{
				createTextTd(tr, "Blocked", "font-style:italic; padding:0 4px 0 4px;");
			}
			else if (value.status == "alreadyBlocked")
			{
				createTextTd(tr, "Already Blocked", "font-style:italic; padding:0 4px 0 4px;");
			}
			else if (value.status == "alreadyDropped")
			{
				createTextTd(tr, "Already Dropped", "font-style:italic; padding:0 4px 0 4px;");
			}
			else if (value.status == "failedRegistrarInstructor")
			{
				createTextTd(tr, "Registrar 'Instructor' role members cannot be blocked", "font-style:italic; padding:0 4px 0 4px;");
			}
			else if (value.status == "failedInstructor")
			{
				createTextTd(tr, "The sole 'Instructor' role member cannot be blocked", "font-style:italic; padding:0 4px 0 4px;");
			}
			else
			{
				createTextTd(tr, "Could not be blocked", "font-style:italic; padding:0 4px 0 4px;");					
			}
		});

		$("#roster_inactivateUserResults").dialog('open');
	},

	activateUsers: function()
	{
		if (anyOidsSelected("selectMember"))
		{
			// confirm
			$("#roster_confirmActivate").dialog('open');
		}
		
		else
		{
			// instruct
			$("#roster_alertSelect").dialog("open");
		}
	},

	doActivate: function(obj)
	{
		// get ids selected
		var params = new Object();
		params.userIds = collectSelectedOids("selectMember");
		params.siteId = obj.siteId;

		// if any selected
		if (params.userIds.length > 0)
		{
			requestCdp("siteroster_unblockMembers", params, function(data)
			{
				// reload the members
				obj.loadMembers(obj, true);

				// confirm
				obj.activateUserResults(obj, data.results);
			});
		}
	},

	activateUserResults: function(obj, results)
	{
		$("#roster_activateUserResults_users tbody").empty();
		$.each(results, function(index, value)
		{
			var tr = $("<tr />");
			$("#roster_activateUserResults_users tbody").append(tr);

			// icon
			if (value.status == "unblocked")
			{
				createIconTd(tr, "publish.png", "Unblocked");
			}
			else if (value.status == "already")
			{
				createIconTd(tr, "publish.png", "Already Unblocked");
			}
			else if (value.status == "failedDropped")
			{
				createIconTd(tr, "error.png", "Already Dropped");
			}
			else
			{
				createIconTd(tr, "error.png", "Problem");
			}

			// name
			if (value.displayName != undefined)
			{
				var name = value.displayName;				
				if (value.iid !== undefined) name = name + " (" + value.iid + ")";
				createTextTd(tr, name);
			}
			else
			{
				createEmptyTd(tr);
			}

			// status
			if (value.status == "unblocked")
			{
				createTextTd(tr, "Unblocked", "font-style:italic; padding:0 4px 0 4px;");
			}
			else if (value.status == "already")
			{
				createTextTd(tr, "Already Unblocked", "font-style:italic; padding:0 4px 0 4px;");
			}
			else if (value.status == "failedDropped")
			{
				createTextTd(tr, "Dropped Registrar users cannot be re-instated", "font-style:italic; padding:0 4px 0 4px;");
			}
			else
			{
				createTextTd(tr, "Could not be unblocked", "font-style:italic; padding:0 4px 0 4px;");
			}
		});

		$("#roster_activateUserResults").dialog('open');
	},
	
	changeRole: function(obj)
	{
		if (anyOidsSelected("selectMember"))
		{
			// if one selected, set the role
			if (oneOidsSelected("selectMember"))
			{
				var curRole = obj.memberRole(obj, collectSelectedOidsArray("selectMember")[0]);
				$('input:radio[name=newRole][value="' + curRole + '"]').prop('checked', true);
			}
			// otherwise set student
			else
			{
				$('input:radio[name=newRole][value="Student"]').prop('checked', true);
			}

			// dialog
			$("#roster_roleChange").dialog('open');
		}
		
		else
		{
			// instruct
			$("#roster_alertSelect").dialog("open");
		}
	},

	doChangeRole: function(obj)
	{
		// get ids selected
		var params = new Object();
		params.userIds = collectSelectedOids("selectMember");
		params.role = $('input:radio[name=newRole]:checked').val();
		params.siteId = obj.siteId;

		// if any selected
		if (params.userIds.length > 0)
		{
			requestCdp("siteroster_assignMembersRole", params, function(data)
			{
				// reload the members
				obj.loadMembers(obj, true);

				// confirm
				obj.changeRoleResults(obj, data.results);
			});
		}
		
		return true;
	},

	changeRoleResults: function(obj, results)
	{
		$("#roster_changeRoleResults_users tbody").empty();
		$.each(results, function(index, value)
		{
			var tr = $("<tr />");
			$("#roster_changeRoleResults_users tbody").append(tr);

			// icon
			if (value.status == "changed")
			{
				createIconTd(tr, "publish.png", "Role Set");
			}
			else if (value.status == "failedInstructor")
			{
				createIconTd(tr, "user_suit.png", "Instructor");
			}
			else
			{
				createIconTd(tr, "error.png", "Problem");
			}

			// name
			if (value.displayName != undefined)
			{
				var name = value.displayName;				
				if (value.iid !== undefined) name = name + " (" + value.iid + ")";
				createTextTd(tr, name);
			}
			else
			{
				createEmptyTd(tr);
			}

			// status
			if (value.status == "changed")
			{
				createTextTd(tr, value.role + " role set", "font-style:italic; padding:0 4px 0 4px;");
			}
			else if (value.status == "failedRegistrar")
			{
				createTextTd(tr, "Registrar users cannot change role", "font-style:italic; padding:0 4px 0 4px;");
			}
			else if (value.status == "failedInstructor")
			{
				createTextTd(tr, "The sole 'Instructor' role member cannot change role", "font-style:italic; padding:0 4px 0 4px;");
			}
			else
			{
				createTextTd(tr, "Role could not be set", "font-style:italic; padding:0 4px 0 4px;");					
			}
		});

		$("#roster_changeRoleResults").dialog('open');
	},

	addUser: function(obj)
	{
		$("#roster_addUser_identifiers").val("");
		$('#roster_assignRoleStudent').prop("checked", true);
		$("#roster_addUser").dialog('open');
	},

	doAddUser: function(obj)
	{
		// get the info
		var params = new Object();
		params.identifiers = $('#roster_addUser_identifiers').val();
		params.role = $('input:radio[name=assignRole]:checked').val();
		params.siteId = obj.siteId;

		requestCdp("siteroster_addMembers", params, function(data)
		{
			// reload the members
			obj.loadMembers(obj, true);

			// confirm
			obj.addUserResults(obj, data.results);
		});
		return true;
	},

	addUserResults: function(obj, results)
	{
		$("#roster_addUserResults_users tbody").empty();
		var any = false;
		$.each(results, function(index, value)
		{
			var tr = $("<tr />");
			$("#roster_addUserResults_users tbody").append(tr);

			// icon
			if ((value.status == "new") || (value.status == "found"))
			{
				createIconTd(tr, "publish.png", "Added");
			}
			else if (value.status == "member")
			{
				createIconTd(tr, "user_enrolled.png", "Already a Member");
			}
			else
			{
				createIconTd(tr, "remove.png", "Not Added");
			}

			// id
			createTextTd(tr, value.uid);

			// status
			if (value.status == "new")
			{
				createTextTd(tr, "Added: New user account", "font-style:italic; padding:0 4px 0 4px;");
				any = true;
			}
			else if (value.status == "found")
			{
				createTextTd(tr, "Added: Existing Etudes user", "font-style:italic; padding:0 4px 0 4px;");
				any = true;
			}
			else if (value.status == "member")
			{
				createTextTd(tr, "Already a Member", "font-style:italic; padding:0 4px 0 4px;");
			}
			else if (value.status == "conflict-email")
			{
				createTextTd(tr, "Not added: No existing user found; Invalid email address for new user", "font-style:italic; color:#b11; padding:0 4px 0 4px;");
			}
			else
			{
				createTextTd(tr, "Not added: Identifies multiple Etudes users", "font-style:italic; color:#b11; padding:0 4px 0 4px;");
			}

			// name
			if (value.displayName != undefined)
			{
				createTextTd(tr, value.displayName);
			}
			else
			{
				createEmptyTd(tr);
			}
		});

		$("#roster_addUserResults").dialog('open');
	},

	populateGroups: function(obj, groups)
	{
		if ($("#roster_groups_item_table").hasClass("tablesorter")) $("#roster_groups_item_table").trigger("destroy");
		$("#roster_groups_item_table tbody").empty();
		$.each(groups, function(index, value)
		{
			var tr = $("<tr />");
			$("#roster_groups_item_table tbody").append(tr);
			
			// select box
			if (!obj.readonly)
			{
				createSelectCheckboxTd(obj, tr, "selectGroup", value.groupId);
			}

			// title
			if (obj.readonly)
			{
				createTextTd(tr, value.title);
			}
			else
			{
				createHotTd(tr, value.title, function(){obj.editGroup(obj, value.groupId);return false;});
			}

			// size
			createTextTd(tr, value.size);				
		});

		$("#roster_groups_item_table").tablesorter(
		{
			headers: obj.readonly ? {} : {0:{sorter:false}},
			sortList: obj.readonly ? [[0,0]] : [[1,0]]
		});
		enableSelectAllCheckbox(obj);

		updateSelectStatus(obj, "selectGroup");
		adjustForNewHeight();
	},

	loadGroups: function(obj, force)
	{
		clearSelectAll("selectGroup");
		
		// if we have groups and members, no need to re-load, unless forcing
		if ((!force) && (obj.siteMembers != null) && (obj.groups != null))
		{
			obj.populateGroups(obj, obj.groups);
		}

		// if we don't have site members yet, load them first
		if (obj.siteMembers == null)
		{
			var params = new Object();
			params.siteId = obj.siteId;
			requestCdp("members", params, function(data)
			{
				obj.siteMembers = data.members;
				
				// now get the groups
				// TODO: one CDP for both
				requestCdp("siteroster_groups", params, function(data)
				{
					obj.groups = data.groups;
					obj.populateGroups(obj, obj.groups);
				});
			});
		}

		// otherwise get just the groups
		else
		{
			var params = new Object();
			params.siteId = obj.siteId;
			requestCdp("siteroster_groups", params, function(data)
			{
				obj.groups = data.groups;
				obj.populateGroups(obj, obj.groups);
			});
		}
	},

	deleteGroups: function()
	{
		if (anyOidsSelected("selectGroup"))
		{
			// confirm
			$("#roster_confirmDelete").dialog('open');
		}
		
		else
		{
			// instruct
			$("#roster_alertSelectGroup").dialog("open");
		}
	},

	doDelete: function(obj)
	{
		// get ids selected
		var params = new Object();
		params.groupIds = collectSelectedOids("selectGroup");
		params.siteId = obj.siteId;

		// if any selected
		if (params.groupIds.length > 0)
		{
			requestCdp("siteroster_deleteGroups", params, function(data)
			{
				// reload the groups
				obj.loadGroups(obj, true);
			});
		}
	},

	// internal data for the add/edit group process
	groupSiteMembers : null,
	groupGroupMembers : null,
	groupGroupId : null,
	groupValid : true,

	// keep our internal data sorted alpha
	sortGroupSiteMembers: function(obj)
	{
		obj.groupSiteMembers.sort(function(a,b)
		{
			if (a.displayName.toUpperCase() < b.displayName.toUpperCase()) return -1;
			else if (a.displayName.toUpperCase() > b.displayName.toUpperCase()) return 1;
			return 0;
		});
	},

	// keep our internal data sorted alpha
	sortGroupGroupMembers: function(obj)
	{
		obj.groupGroupMembers.sort(function(a,b)
		{
			if (a.displayName.toUpperCase() < b.displayName.toUpperCase()) return -1;
			else if (a.displayName.toUpperCase() > b.displayName.toUpperCase()) return 1;
			return 0;
		});
	},

	// start the addGroup / editGroup process
	editGroup: function(obj, groupId)
	{
		obj.groupSiteMembers = new Array();
		obj.groupGroupMembers = new Array();
		obj.groupGroupId = groupId;
		obj.groupValid = true;

		$("#roster_group_title_used").addClass("e3_offstage");
		$("#roster_group_no_title").addClass("e3_offstage");

		var group = obj.findGroup(obj, groupId);
		if (group == null)
		{
			group = new Object();
			group.id = 0;
			group.size = 0;
			group.title = "";
			group.members = new Object();
			$("#roster_editGroup").dialog("option", "title", "Group: <i>new group</i>");
		}
		else
		{
			$("#roster_editGroup").dialog("option", "title", "Group: " + group.title);
		}

		$('#roster_editGroupTitle').val(group.title);

		$.each(group.members, function(index, value)
		{
			var user = obj.findMember(obj, value.userId);
			obj.groupGroupMembers.push(user);
		});
		obj.sortGroupGroupMembers(obj);
		obj.populateGroupGroupMembers(obj);

		$.each(obj.siteMembers, function(index, value)
		{
			// only for Student status=3 Active and status=0 Enrolled. 09/28/2016 - Added Teaching Assistant in order to show TA's in the users list to add to a group
			if (value.role == "Student" || value.role == "Teaching Assistant")
			{
				if ((value.status == 3) || (value.status == 0))
				{
					// only if not already in the group
					if (!obj.groupContains(obj, group, value.userId))
					{
						obj.groupSiteMembers.push(value);
					}
				}
			}
		});
		obj.sortGroupSiteMembers(obj);
		obj.populateGroupSiteMembers(obj);
		
		$('#roster_add_to_group').unbind("click").click(function(){obj.editGroupAdd(obj);return false;});
		$('#roster_remove_from_group').unbind("click").click(function(){obj.editGroupRemove(obj);return false;});
		$('#roster_editGroupTitle').unbind('change').change(function(){obj.validateGroupTitle(obj);return true;});
		$("#roster_editGroup").dialog('open');
	},

	validateGroupTitle: function(obj)
	{
		// the title must be unique among groups
		var unique = true;
		var defined = true;
		var newTitle = $.trim($('#roster_editGroupTitle').val().toLowerCase());
		if (newTitle == "")
		{
			defined = false;
		}

		if (defined && obj.groups != null)
		{
			$.each(obj.groups, function(index, value)
			{
				if (value.groupId != obj.groupGroupId)
				{
					if (value.title.toLowerCase() == newTitle) unique = false;
				}
			});
		}

		if (!unique)
		{
			$("#roster_group_title_used").removeClass("e3_offstage");
		}
		else
		{
			$("#roster_group_title_used").addClass("e3_offstage");				
		}
		
		if (!defined)
		{
			$("#roster_group_no_title").removeClass("e3_offstage");
		}
		else
		{
			$("#roster_group_no_title").addClass("e3_offstage");				
		}
		
		obj.groupValid = unique && defined;
	},

	// populate the addGroup / editGroup site members list
	populateGroupSiteMembers: function(obj)
	{
		$('#roster_site_members').empty();
		$.each(obj.groupSiteMembers, function(index, value)
		{
			var option = $('<option value="' + value.userId + '">' + value.displayName + '</option>');
			$("#roster_site_members").append(option);
		});		
	},

	// populate the addGroup / editGroup group members list
	populateGroupGroupMembers: function(obj)
	{
		$('#roster_group_members').empty();
		$.each(obj.groupGroupMembers, function(index, value)
		{
			var option = $('<option value="' + value.userId + '">' + value.displayName + '</option>');
			$("#roster_group_members").append(option);
		});		
	},

	// move the selected users from the site members list into the group members list, adding users to the group
	editGroupAdd: function(obj)
	{
		// find the selected members in the site members select
		$.each($('#roster_site_members').val(), function(i, userId)
		{
			var member = obj.findMember(obj, userId);

			// remove from the site members list
			var index = -1;
			$.each(obj.groupSiteMembers, function(i, value)
			{
				if (value.userId == userId) index = i;
			});	
			obj.groupSiteMembers.splice(index, 1);

			// add to the group members list
			obj.groupGroupMembers.push(member);
			obj.sortGroupGroupMembers(obj);
		});

		// reload the two lists
		obj.populateGroupGroupMembers(obj);
		obj.populateGroupSiteMembers(obj);
	},

	// move the selected users from the group members list into the site members list, removing users from the group
	editGroupRemove: function(obj)
	{
		// find the selected members in the group members select
		$.each($('#roster_group_members').val(), function(i, userId)
		{
			// find the member object in groupGroupMembers with this user id
			var member = obj.findMember(obj, userId);

			// remove from the group members list
			var index = -1;
			$.each(obj.groupGroupMembers, function(i, value)
			{
				if (value.userId == userId) index = i;
			});	
			obj.groupGroupMembers.splice(index, 1);

			// add to the site members list
			obj.groupSiteMembers.push(member);
			obj.sortGroupSiteMembers(obj);
		});

		// reload the two lists
		obj.populateGroupGroupMembers(obj);
		obj.populateGroupSiteMembers(obj);
	},

	// complete the addGroup / editGroup process by sending the change to the server
	doSaveGroup: function(obj)
	{
		obj.validateGroupTitle(obj);

		// if not valid...
		if (!obj.groupValid)
		{
			$("#roster_alertNoSave").dialog("open");
			return false;
		}

		// get the info
		var params = new Object();
		params.title = $.trim($('#roster_editGroupTitle').val());
		params.members = "";
		if (obj.groupGroupId !== null) params.groupId = "" + obj.groupGroupId;
		$.each(obj.groupGroupMembers, function(i, value)
		{
			params.members += value.userId + "\t";
		});
		params.siteId = obj.siteId;

		requestCdp("siteroster_saveGroup", params, function(data)
		{
			// reload the groups
			obj.loadGroups(obj, true);
		});
		
		return true;
	},

	// find the member object with this userId from our site members
	findMember: function(obj, userId)
	{
		var found = null;
		$.each(obj.siteMembers, function(index, value)
		{
			if (value.userId == userId) found = value;
		});
		return found;
	},

	// find the group object with this groupId from our groups
	findGroup: function(obj, groupId)
	{
		var found = null;
		if (obj.groups != null)
		{
			$.each(obj.groups, function(index, value)
			{
				if (value.groupId == groupId) found = value;
			});
		}
		return found;
	},
	
	// true if the user is in the group
	groupContains: function(obj, group, userId)
	{
		var found = false;
		$.each(group.members, function(index, value)
		{
			if (value.userId == userId) found = true;
		});
		return found;
	},
	
	// find the id in the siteMembers, return the role
	memberRole: function(obj, memberId)
	{
		var rv = "Student";
		$.each(obj.siteMembers, function(index, value)
		{
			if (value.userId == memberId)
			{
				var role = value.role;
				if (role == "Blocked") role = "Student";
				rv = role;
			}
		});

		return rv;
	},

	configure: function(obj)
	{
		var data = new Object();
		data.siteIds = obj.siteIds;
		data.siteId = obj.siteId;
		data.returnTo = obj.returnTo;
		selectStandAloneTool("/configure/configure", data);
	}
};

completeToolLoad();
