/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/siteroster/trunk/siteroster-webapp/webapp/src/java/org/etudes/siteroster/cdp/SiteRosterCdpHandler.java $
 * $Id: SiteRosterCdpHandler.java 11612 2015-09-15 19:15:56Z mallikamt $
 ***********************************************************************************
 *
 * Copyright (c) 2013, 2014 Etudes, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

package org.etudes.siteroster.cdp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.validator.routines.EmailValidator;
import org.etudes.cdp.api.CdpHandler;
import org.etudes.cdp.api.CdpStatus;
import org.etudes.cdp.util.CdpResponseHelper;
import org.sakaiproject.authz.api.AuthzGroup;
import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.authz.api.AuthzPermissionException;
import org.sakaiproject.authz.api.GroupNotDefinedException;
import org.sakaiproject.authz.api.Member;
import org.sakaiproject.authz.api.Role;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.email.api.EmailService;
import org.sakaiproject.entity.api.EntityPropertyNotDefinedException;
import org.sakaiproject.entity.api.EntityPropertyTypeException;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.api.SpecialAccessToolService;
import org.sakaiproject.site.api.UserSiteAccess;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserAlreadyDefinedException;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserEdit;
import org.sakaiproject.user.api.UserIdInvalidException;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.user.api.UserPermissionException;
import org.sakaiproject.util.StringUtil;

/**
 */
public class SiteRosterCdpHandler implements CdpHandler
{
	/** The property that needs to be set ("true") on a site group to distinguish it from a "section" */
	private static final String GROUP_PROP_WSETUP_CREATED = "group_prop_wsetup_created";

	/** Our log (commons). */
	private static Log M_log = LogFactory.getLog(SiteRosterCdpHandler.class);

	public String getPrefix()
	{
		return "siteroster";
	}

	public Map<String, Object> handle(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String requestPath,
			String path, String authenticatedUserId) throws ServletException, IOException
	{
		// if no authenticated user, we reject all requests
		if (authenticatedUserId == null)
		{
			Map<String, Object> rv = new HashMap<String, Object>();
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.notLoggedIn.getId());
			return rv;
		}

		else if (requestPath.equals("removeMembers"))
		{
			return dispatchRemoveMembers(req, res, parameters, path);
		}
		else if (requestPath.equals("blockMembers"))
		{
			return dispatchBlockMembers(req, res, parameters, path);
		}
		else if (requestPath.equals("unblockMembers"))
		{
			return dispatchUnblockMembers(req, res, parameters, path);
		}
		else if (requestPath.equals("assignMembersRole"))
		{
			return dispatchAssignMembersRole(req, res, parameters, path);
		}
		else if (requestPath.equals("addMembers"))
		{
			return dispatchAddMembers(req, res, parameters, path);
		}
		else if (requestPath.equals("deleteGroups"))
		{
			return dispatchDeleteGroups(req, res, parameters, path);
		}
		else if (requestPath.equals("saveGroup"))
		{
			return dispatchSaveGroup(req, res, parameters, path);
		}
		else if (requestPath.equals("groups"))
		{
			return dispatchGroups(req, res, parameters, path);
		}
		else if (requestPath.equals("getSpecialAccess"))
		{
			return dispatchGetSpecialAccess(req, res, parameters, path, authenticatedUserId);
		}
		else if (requestPath.equals("saveSpecialAccess"))
		{
			return dispatchSaveSpecialAccess(req, res, parameters, path, authenticatedUserId);
		}

		return null;
	}

	/**
	 * @return The AuthzGroupService, via the component manager.
	 */
	protected AuthzGroupService authzGroupService()
	{
		return (AuthzGroupService) ComponentManager.get(AuthzGroupService.class);
	}

	@SuppressWarnings("unchecked")
	protected Map<String, Object> dispatchAddMembers(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path)
			throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// get the site id parameter
		String siteId = (String) parameters.get("siteId");
		if (siteId == null)
		{
			M_log.warn("dispatchAddMembers - no siteId parameter");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// get the role parameter
		String roleId = (String) parameters.get("role");
		if (roleId == null)
		{
			M_log.warn("dispatchAddMembers - no role parameter");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// get the identifiers parameter
		String identifiers = (String) parameters.get("identifiers");
		if (identifiers == null)
		{
			M_log.warn("dispatchAddMembers - no identifiers parameter");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// parse the identifiers
		String[] ids = StringUtil.split(identifiers, "\r\n");

		String azgId = siteService().siteReference(siteId);
		if (!(authzGroupService().allowUpdate(azgId) || siteService().allowUpdateSiteMembership(siteId)))
		{
			M_log.warn("dispatchAddMembers - no permission to site: " + siteId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		Site site = null;
		try
		{
			site = siteService().getSite(siteId);
		}
		catch (IdUnusedException e)
		{
		}

		if (site == null)
		{
			M_log.warn("dispatchAddMembers - missing site: " + siteId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// build up a map to return - the main map has a single "results" object
		List<Map<String, String>> resultsMap = new ArrayList<Map<String, String>>();
		rv.put("results", resultsMap);

		Map<String, String> memberMap = null;

		try
		{
			boolean changed = false;
			AuthzGroup azgEdit = authzGroupService().getAuthzGroup(azgId);

			for (String uid : ids)
			{
				String returnUid = uid;
				String returnStatus = null;
				String returnDisplayName = null;
				boolean conflict = false;

				boolean newUser = false;
				String newUserPassword = null;

				// skip empty line entries, blanks
				uid = StringUtil.trimToNull(uid);
				if (uid == null) continue;

				// 1) try to find the user by eid
				User user = null;
				try
				{
					List<User> users = userDirectoryService().getUsersByEid(uid);
					if (users.size() == 1)
					{
						user = userDirectoryService().getUserByEid(uid);
						returnDisplayName = user.getDisplayName();
						returnStatus = "found";
					}
					else if (users.size() > 1)
					{
						conflict = true;
						returnStatus = "conflict-multiple";
					}
				}
				catch (UserNotDefinedException e)
				{
				}

				// 2) try to find the user by iid @ instCode
				if ((user == null) && !conflict)
				{
					try
					{
						// isolate the iid and instCode
						String[] parts = splitLast(uid, "@");
						if (parts != null)
						{
							user = userDirectoryService().getUserByIid(parts[1], parts[0]);
							returnDisplayName = user.getDisplayName();
							returnStatus = "found";
						}
					}
					catch (UserNotDefinedException e)
					{
					}
				}

				// 3) next try by just IID (no @instCode)
				if ((user == null) && !conflict)
				{
					try
					{
						user = userDirectoryService().getUserByIid(null, uid);
						returnDisplayName = user.getDisplayName();
						returnStatus = "found";
					}
					catch (UserNotDefinedException e)
					{
					}
				}

				// 4) try to find the user by email
				if ((user == null) && !conflict)
				{
					Collection<User> users = userDirectoryService().findUsersByEmail(uid);
					if (users.size() == 1)
					{
						user = users.iterator().next();
						returnDisplayName = user.getDisplayName();
						returnStatus = "found";
					}
					else if (users.size() > 1)
					{
						conflict = true;
						returnStatus = "conflict-multiple";
					}
				}

				// check if we can use this as an email address, if we need to create a user account
				if ((user == null) && !conflict)
				{
					// validate as an email address
					EmailValidator validator = EmailValidator.getInstance();
					if (!validator.isValid(uid))
					{
						conflict = true;
						returnStatus = "conflict-email";
					}
				}

				// add a new user if needed
				if ((user == null) && !conflict)
				{
					try
					{
						UserEdit uEdit = userDirectoryService().addUser(null, uid);
						uEdit.setEmail(uid);
						uEdit.setType("guest");

						// set password to a random positive number
						Random generator = new Random(System.currentTimeMillis());
						Integer num = new Integer(generator.nextInt(Integer.MAX_VALUE));
						if (num.intValue() < 0) num = new Integer(num.intValue() * -1);
						newUserPassword = num.toString();
						uEdit.setPassword(newUserPassword);

						// save
						userDirectoryService().commitEdit(uEdit);
						user = uEdit;
						newUser = true;
						returnStatus = "new";
					}
					catch (UserIdInvalidException e)
					{
					}
					catch (UserAlreadyDefinedException e)
					{
					}
					catch (UserPermissionException e)
					{
					}
				}

				if (user != null)
				{
					// the user's current membership
					Member m = azgEdit.getMember(user.getId());

					if (m == null)
					{
						azgEdit.addMember(user.getId(), roleId, true, false);
						changed = true;

						notifyMember(newUser, user, newUserPassword, site.getTitle(), roleId);
					}
					else
					{
						returnStatus = "member";
					}
				}

				memberMap = new HashMap<String, String>();
				resultsMap.add(memberMap);
				memberMap.put("status", returnStatus);
				memberMap.put("uid", returnUid);
				memberMap.put("displayName", returnDisplayName);
			}

			// save if needed
			if (changed)
			{
				authzGroupService().save(azgEdit);
			}
		}
		catch (GroupNotDefinedException e)
		{
			M_log.warn("dispatchAddMembers - site azg not defined: " + siteId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		catch (AuthzPermissionException e)
		{
			M_log.warn("dispatchAddMembers - no permission to site: " + siteId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchAssignMembersRole(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters,
			String path) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// get the site id parameter
		String siteId = (String) parameters.get("siteId");
		if (siteId == null)
		{
			M_log.warn("dispatchAssignMembersRole - no siteId parameter");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// get the role parameter
		String roleId = (String) parameters.get("role");
		if (roleId == null)
		{
			M_log.warn("dispatchAssignMembersRole - no role parameter");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// get the user ids parameter
		String userIds = (String) parameters.get("userIds");
		if (userIds == null)
		{
			M_log.warn("dispatchAssignMembersRole - no userIds parameter");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		String[] ids = StringUtil.split(userIds, "\t");

		String azgId = siteService().siteReference(siteId);
		if (!(authzGroupService().allowUpdate(azgId) || siteService().allowUpdateSiteMembership(siteId)))
		{
			M_log.warn("dispatchAssignMembersRole - no permission to site: " + siteId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// build up a map to return - the main map has a single "results" object
		List<Map<String, String>> resultsMap = new ArrayList<Map<String, String>>();
		rv.put("results", resultsMap);

		Map<String, String> memberMap = null;

		try
		{
			boolean changed = false;
			AuthzGroup azgEdit = authzGroupService().getAuthzGroup(azgId);

			// make sure the new role is defined
			if (azgEdit.getRole(roleId) != null)
			{
				for (String uid : ids)
				{
					// the user's current membership
					Member m = azgEdit.getMember(uid);

					// must be a member
					if (m == null) continue;

					String returnStatus = "failed";

					// if not provided, change role
					if (!m.isProvided())
					{
						// if changing from instructor, fail if there would be no instructors left
						if (m.getRole().getId().equals("Instructor") && m.isActive() && (!roleId.equals("Instructor"))
								&& azgEdit.getUsersHasRole("Instructor").size() < 2)
						{
							returnStatus = "failedInstructor";
						}
						// same for maintain
						else if (m.getRole().getId().equals("maintain") && m.isActive() && (!roleId.equals("maintain"))
								&& azgEdit.getUsersHasRole("maintain").size() < 2)
						{
							returnStatus = "failedInstructor";
						}
						else
						{
							azgEdit.removeMember(uid);
							azgEdit.addMember(uid, roleId, m.isActive(), m.isProvided());
							changed = true;
							returnStatus = "changed";
						}
					}
					else
					{
						returnStatus = "failedRegistrar";
					}

					memberMap = new HashMap<String, String>();
					resultsMap.add(memberMap);
					memberMap.put("status", returnStatus);
					memberMap.put("userId", uid);
					try
					{
						User u = userDirectoryService().getUser(uid);
						memberMap.put("displayName", u.getDisplayName());
						String iid = StringUtil.trimToNull(u.getIidDisplay());
						if (iid != null) memberMap.put("iid", iid);
						memberMap.put("role", roleId);
					}
					catch (UserNotDefinedException e)
					{
					}
				}

				// save if needed
				if (changed)
				{
					authzGroupService().save(azgEdit);
				}
			}
		}
		catch (GroupNotDefinedException e)
		{
			M_log.warn("dispatchAssignMembersRole - site azg not defined: " + siteId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		catch (AuthzPermissionException e)
		{
			M_log.warn("dispatchAssignMembersRole - no permission to site: " + siteId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchBlockMembers(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path)
			throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// get the site id parameter
		String siteId = (String) parameters.get("siteId");
		if (siteId == null)
		{
			M_log.warn("dispatchBlockMembers - no siteId parameter");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// get the user ids parameter
		String userIds = (String) parameters.get("userIds");
		if (userIds == null)
		{
			M_log.warn("dispatchBlockMembers - no userIds parameter");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		String[] ids = StringUtil.split(userIds, "\t");

		String azgId = siteService().siteReference(siteId);
		if (!(authzGroupService().allowUpdate(azgId) || siteService().allowUpdateSiteMembership(siteId)))
		{
			M_log.warn("dispatchBlockMembers - no permission to site: " + siteId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// build up a map to return - the main map has a single "results" object
		List<Map<String, String>> resultsMap = new ArrayList<Map<String, String>>();
		rv.put("results", resultsMap);

		Map<String, String> memberMap = null;

		try
		{
			boolean changed = false;
			AuthzGroup azgEdit = authzGroupService().getAuthzGroup(azgId);
			for (String uid : ids)
			{
				String returnStatus = "failed";

				// the user's current membership
				Member m = azgEdit.getMember(uid);

				// must be a member
				if (m == null) continue;

				// if provided, and if the role is student, and active (not dropped), change the user's role to blocked (if we have a blocked role)
				if (m.isProvided())
				{
					if (m.getRole().getId().equals("Student"))
					{
						if (m.isActive())
						{
							if (azgEdit.getRole("Blocked") != null)
							{
								azgEdit.removeMember(uid);
								azgEdit.addMember(uid, "Blocked", m.isActive(), m.isProvided());
								changed = true;
								returnStatus = "blocked";
							}
						}
						else
						{
							returnStatus = "alreadyDropped";
						}
					}
					else if (m.getRole().getId().equals("Blocked"))
					{
						returnStatus = "alreadyBlocked";
					}
					else if (m.getRole().getId().equals("Instructor"))
					{
						returnStatus = "failedRegistrarInstructor";
					}
					else if (m.getRole().getId().equals("Observer"))
					{
						azgEdit.removeMember(uid);
						azgEdit.addMember(uid, "Observer", false, m.isProvided());
						changed = true;
						returnStatus = "blocked";
					}
				}

				// if not provided, and the user is active, set the status to inactive
				else
				{
					if (m.isActive())
					{
						// unless this would inactivate the last instructor / maintain
						if (m.getRole().getId().equals("Instructor") && m.isActive() && azgEdit.getUsersHasRole("Instructor").size() < 2)
						{
							returnStatus = "failedInstructor";
						}
						// same for maintain
						else if (m.getRole().getId().equals("maintain") && m.isActive() && azgEdit.getUsersHasRole("maintain").size() < 2)
						{
							returnStatus = "failedInstructor";
						}
						else
						{
							azgEdit.removeMember(uid);
							azgEdit.addMember(uid, m.getRole().getId(), false, m.isProvided());
							changed = true;
							returnStatus = "blocked";
						}
					}
					else
					{
						returnStatus = "alreadyBlocked";
					}
				}

				memberMap = new HashMap<String, String>();
				resultsMap.add(memberMap);
				memberMap.put("status", returnStatus);
				memberMap.put("userId", uid);
				try
				{
					User u = userDirectoryService().getUser(uid);
					memberMap.put("displayName", u.getDisplayName());
					String iid = StringUtil.trimToNull(u.getIidDisplay());
					if (iid != null) memberMap.put("iid", iid);
				}
				catch (UserNotDefinedException e)
				{
				}
			}

			// save if needed
			if (changed)
			{
				authzGroupService().save(azgEdit);
			}
		}
		catch (GroupNotDefinedException e)
		{
			M_log.warn("dispatchBlockMembers - site azg not defined: " + siteId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		catch (AuthzPermissionException e)
		{
			M_log.warn("dispatchBlockMembers - no permission to site: " + siteId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchDeleteGroups(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path)
			throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// get the site id parameter
		String siteId = (String) parameters.get("siteId");
		if (siteId == null)
		{
			M_log.warn("dispatchDeleteGroups - no siteId parameter");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// get the group ids parameter
		String groupIds = (String) parameters.get("groupIds");
		if (groupIds == null)
		{
			M_log.warn("dispatchDeleteGroups - no groupIds parameter");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		String[] ids = StringUtil.split(groupIds, "\t");

		try
		{
			Site site = siteService().getSite(siteId);
			for (String id : ids)
			{
				Group group = site.getGroup(id);
				if (group != null)
				{
					site.removeGroup(group);
				}
			}

			siteService().save(site);
		}
		catch (IdUnusedException e)
		{
			M_log.warn("dispatchDeleteGroups - site not found: " + siteId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		catch (PermissionException e)
		{
			M_log.warn("dispatchDeleteGroups - cannot save site: " + siteId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchGetSpecialAccess(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters,
			String path, String authenticatedUserId) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// get the site id parameter
		String siteId = (String) parameters.get("siteId");
		if (siteId == null)
		{
			M_log.warn("dispatchGetSpecialAccess - no siteId parameter");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		// Site site = null;
		// try
		// {
		// site = this.siteService().getSite(siteId);
		// }
		// catch (IdUnusedException e)
		// {
		// M_log.warn("dispatchUploadToSite - site not found: " + siteId);
		//
		// // add status parameter
		// rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
		// return rv;
		// }

		// user id parameter
		String userId = (String) parameters.get("userId");
		if (userId == null)
		{
			M_log.warn("dispatchGetSpecialAccess - no userId parameter");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// get the user's special access details
		String days = "";
		String limit = "";
		String multiplier = "";
		String untimed = "";

		String msg = specialAccessToolService().getAccessTools(userId, siteId);
		if (msg != null)
		{
			rv.put("message", msg);

			UserSiteAccess access = specialAccessToolService().findUserSiteAccess(userId, siteId);

			if (access != null)
			{
				if (access.days > 0) days = CdpResponseHelper.formatInt(access.days);
				if (access.timelimit > 0) limit = msToTimeLimit(access.timelimit);
				if (access.timemult > 0) multiplier = CdpResponseHelper.formatFloat(access.timemult);
				if (access.untimed) untimed="true";
			}
		}

		rv.put("days", days);
		rv.put("limit", limit);
		rv.put("multiplier", multiplier);
		rv.put("untimed", untimed);

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	@SuppressWarnings("unchecked")
	protected Map<String, Object> dispatchGroups(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path)
			throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// get the site id parameter
		String siteId = (String) parameters.get("siteId");
		if (siteId == null)
		{
			M_log.warn("dispatchGroups - no siteId parameter");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// build up a map to return - the main map has a single "groups" object
		List<Map<String, Object>> groupsMap = new ArrayList<Map<String, Object>>();
		rv.put("groups", groupsMap);

		Map<String, Object> groupMap = null;
		List<Map<String, String>> membersMap = null;
		Map<String, String> memberMap = null;

		try
		{
			Site site = siteService().getSite(siteId);

			List<Group> groups = new ArrayList<Group>(site.getGroups());

			// sort by title
			Collections.sort(groups, new Comparator<Group>()
			{
				public int compare(Group arg0, Group arg1)
				{
					return arg0.getTitle().compareToIgnoreCase(arg1.getTitle());
				}
			});

			for (Group g : groups)
			{
				try
				{
					// let this throw exceptions if this property is not defined
					g.getProperties().getBooleanProperty(GROUP_PROP_WSETUP_CREATED);

					groupMap = new HashMap<String, Object>();
					groupsMap.add(groupMap);
					groupMap.put("groupId", g.getId());
					groupMap.put("title", g.getTitle());
					groupMap.put("description", g.getDescription());
					Set<Member> members = g.getActiveMembers();
					groupMap.put("size", members.size());
					membersMap = new ArrayList<Map<String, String>>();
					groupMap.put("members", membersMap);
					for (Member m : members)
					{
						memberMap = new HashMap<String, String>();
						membersMap.add(memberMap);
						memberMap.put("userId", m.getUserId());
					}
				}
				catch (EntityPropertyNotDefinedException e)
				{
				}
				catch (EntityPropertyTypeException e)
				{
				}
			}
		}
		catch (IdUnusedException e)
		{
			M_log.warn("dispatchGroups - site not found: " + siteId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchRemoveMembers(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path)
			throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// get the site id parameter
		String siteId = (String) parameters.get("siteId");
		if (siteId == null)
		{
			M_log.warn("dispatchRemoveMembers - no siteId parameter");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// get the user ids parameter
		String userIds = (String) parameters.get("userIds");
		if (userIds == null)
		{
			M_log.warn("dispatchRemoveMembers - no userIds parameter");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		String[] ids = StringUtil.split(userIds, "\t");

		String azgId = siteService().siteReference(siteId);
		if (!(authzGroupService().allowUpdate(azgId) || siteService().allowUpdateSiteMembership(siteId)))
		{
			M_log.warn("dispatchBlockMembers - no permission to site: " + siteId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// build up a map to return - the main map has a single "results" object
		List<Map<String, String>> resultsMap = new ArrayList<Map<String, String>>();
		rv.put("results", resultsMap);

		Map<String, String> memberMap = null;

		try
		{
			boolean changed = false;
			AuthzGroup azgEdit = authzGroupService().getAuthzGroup(azgId);
			for (String uid : ids)
			{
				String returnStatus = "failed";

				// the user's current membership
				Member m = azgEdit.getMember(uid);

				// must be a member
				if (m == null) continue;

				// if provided, and if the role is student, and active (not dropped), change the user's role to blocked (if we have a blocked role)
				if (m.isProvided())
				{
					if (m.getRole().getId().equals("Student"))
					{
						if (m.isActive())
						{
							if (azgEdit.getRole("Blocked") != null)
							{
								azgEdit.removeMember(uid);
								azgEdit.addMember(uid, "Blocked", m.isActive(), m.isProvided());
								changed = true;
								returnStatus = "blocked";
							}
						}
						else
						{
							returnStatus = "alreadyDropped";
						}
					}
					else if (m.getRole().getId().equals("Blocked"))
					{
						returnStatus = "alreadyBlocked";
					}
					else if (m.getRole().getId().equals("Instructor"))
					{
						returnStatus = "failedRegistrarInstructor";
					}
				}

				// if not provided, remove the member
				else
				{
					// unless this would remove the final instructor / maintain member
					if (m.getRole().getId().equals("Instructor") && m.isActive() && azgEdit.getUsersHasRole("Instructor").size() < 2)
					{
						returnStatus = "failedInstructor";
					}
					else if (m.getRole().getId().equals("maintain") && m.isActive() && azgEdit.getUsersHasRole("maintain").size() < 2)
					{
						returnStatus = "failedInstructor";
					}
					else
					{
						azgEdit.removeMember(uid);
						changed = true;
						returnStatus = "removed";
					}
				}

				memberMap = new HashMap<String, String>();
				resultsMap.add(memberMap);
				memberMap.put("status", returnStatus);
				memberMap.put("userId", uid);
				try
				{
					User u = userDirectoryService().getUser(uid);
					memberMap.put("displayName", u.getDisplayName());
					String iid = StringUtil.trimToNull(u.getIidDisplay());
					if (iid != null) memberMap.put("iid", iid);
				}
				catch (UserNotDefinedException e)
				{
				}
			}

			// save if needed
			if (changed)
			{
				authzGroupService().save(azgEdit);
			}
		}
		catch (GroupNotDefinedException e)
		{
			M_log.warn("dispatchBlockMembers - site azg not defined: " + siteId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		catch (AuthzPermissionException e)
		{
			M_log.warn("dispatchBlockMembers - no permission to site: " + siteId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	@SuppressWarnings("unchecked")
	protected Map<String, Object> dispatchSaveGroup(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path)
			throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// get the site id parameter
		String siteId = (String) parameters.get("siteId");
		if (siteId == null)
		{
			M_log.warn("dispatchSaveGroup - no siteId parameter");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// title
		String title = (String) parameters.get("title");
		if (title == null)
		{
			M_log.warn("dispatchSaveGroup - no title parameter");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// description (optional)
		String description = (String) parameters.get("description");

		// members (user ids tab separated)
		String members = (String) parameters.get("members");
		if (members == null)
		{
			M_log.warn("dispatchSaveGroup - no members parameter");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		String[] memberIds = StringUtil.split(members, "\t");

		// groupId
		String groupId = (String) parameters.get("groupId");

		try
		{
			Site site = siteService().getSite(siteId);
			Group group = null;

			if (groupId == null)
			{
				// add a new group
				group = site.addGroup();

				// mark is as not section
				group.getPropertiesEdit().addProperty(GROUP_PROP_WSETUP_CREATED, "true");
			}
			else
			{
				group = site.getGroup(groupId);
				if (group == null)
				{
					M_log.warn("dispatchSaveGroup - group not found: site: " + siteId + " group: " + groupId);

					// add status parameter
					rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
					return rv;
				}

				// replace just the active members
				Set<Member> activeMembers = group.getActiveMembers();
				for (Member member : activeMembers)
				{
					group.removeMember(member.getUserId());
				}
			}

			group.setTitle(title);
			group.setDescription(description);

			for (String memberId : memberIds)
			{
				Role r = site.getUserRole(memberId);
				Member m = site.getMember(memberId);
				group.addMember(memberId, r != null ? r.getId() : "", m != null ? m.isActive() : true, false);
			}

			siteService().save(site);
		}
		catch (IdUnusedException e)
		{
			M_log.warn("dispatchSaveGroup - site not found: " + siteId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		catch (PermissionException e)
		{
			M_log.warn("dispatchSaveGroup - cannot save save: " + siteId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchSaveSpecialAccess(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters,
			String path, String authenticatedUserId) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// get the site id parameter
		String siteId = (String) parameters.get("siteId");
		if (siteId == null)
		{
			M_log.warn("dispatchGetSpecialAccess - no siteId parameter");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		// Site site = null;
		// try
		// {
		// site = this.siteService().getSite(siteId);
		// }
		// catch (IdUnusedException e)
		// {
		// M_log.warn("dispatchUploadToSite - site not found: " + siteId);
		//
		// // add status parameter
		// rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
		// return rv;
		// }

		// user id parameter
		String userId = (String) parameters.get("userId");
		if (userId == null)
		{
			M_log.warn("dispatchGetSpecialAccess - no userId parameter");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// settings
		String daysStr = StringUtil.trimToNull((String) parameters.get("days"));
		String limitStr = StringUtil.trimToNull((String) parameters.get("limit"));
		String multiplierStr = StringUtil.trimToNull((String) parameters.get("multiplier"));
		String untimedStr = StringUtil.trimToNull((String) parameters.get("untimed"));
		Boolean untimedVal = Boolean.FALSE;
		if (untimedStr != null && untimedStr.equals("on")) untimedVal = Boolean.TRUE;
		
		int days = 0;
		if (daysStr != null)
		{
			days = Integer.parseInt(daysStr);
		}
		// limit in milliseconds
		long limit = 0;
		if (limitStr != null)
		{
			limit = timeLimitToMs(limitStr);
		}

		float multiplier = 0;
		if (multiplierStr != null)
		{
			multiplier = Float.parseFloat(multiplierStr);
		}
		if (untimedVal == Boolean.TRUE)
		{
			limit = 0;
			multiplier = 0;
		}

		// command - save or delete
		String command = (String) parameters.get("command");

		try
		{
			String result = specialAccessToolService().processAccess(command, userId, siteId, days, untimedVal.booleanValue(), limit, multiplier);
			if (result != null) rv.put("message", result);
		}
		catch (Exception e)
		{
			M_log.warn("dispatchSaveSpecialAccess: " + e);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchUnblockMembers(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path)
			throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// get the site id parameter
		String siteId = (String) parameters.get("siteId");
		if (siteId == null)
		{
			M_log.warn("dispatchUnblockMembers - no siteId parameter");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// get the user ids parameter
		String userIds = (String) parameters.get("userIds");
		if (userIds == null)
		{
			M_log.warn("dispatchUnblockMembers - no userIds parameter");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		String[] ids = StringUtil.split(userIds, "\t");

		String azgId = siteService().siteReference(siteId);
		if (!(authzGroupService().allowUpdate(azgId) || siteService().allowUpdateSiteMembership(siteId)))
		{
			M_log.warn("dispatchUnblockMembers - no permission to site: " + siteId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// build up a map to return - the main map has a single "results" object
		List<Map<String, String>> resultsMap = new ArrayList<Map<String, String>>();
		rv.put("results", resultsMap);

		Map<String, String> memberMap = null;

		try
		{
			boolean changed = false;
			AuthzGroup azgEdit = authzGroupService().getAuthzGroup(azgId);
			for (String uid : ids)
			{
				// the user's current membership
				Member m = azgEdit.getMember(uid);

				// must be a member
				if (m == null) continue;

				String returnStatus = "failed";

				// if provided, and if the role is blocked, change the user's role to student
				if (m.isProvided())
				{
					if (m.getRole().getId().equals("Blocked"))
					{
						azgEdit.removeMember(uid);
						azgEdit.addMember(uid, "Student", m.isActive(), m.isProvided());
						changed = true;
						returnStatus = "unblocked";
					}
					else if (m.getRole().getId().equals("Student") && (!m.isActive()))
					{
						returnStatus = "failedDropped";
					}
					else if (m.getRole().getId().equals("Observer") && (!m.isActive()))
					{
						azgEdit.removeMember(uid);
						azgEdit.addMember(uid, "Observer", true, m.isProvided());
						changed = true;
						returnStatus = "unblocked";
					}
					else if (m.isActive())
					{
						returnStatus = "already";
					}
				}

				// if not provided, and the user is inactive, set the status to active
				else
				{
					if (!m.isActive())
					{
						azgEdit.removeMember(uid);
						azgEdit.addMember(uid, m.getRole().getId(), true, m.isProvided());
						changed = true;
						returnStatus = "unblocked";
					}
					else
					{
						returnStatus = "already";
					}
				}

				memberMap = new HashMap<String, String>();
				resultsMap.add(memberMap);
				memberMap.put("status", returnStatus);
				memberMap.put("userId", uid);
				try
				{
					User u = userDirectoryService().getUser(uid);
					memberMap.put("displayName", u.getDisplayName());
					String iid = StringUtil.trimToNull(u.getIidDisplay());
					if (iid != null) memberMap.put("iid", iid);
				}
				catch (UserNotDefinedException e)
				{
				}
			}

			// save if needed
			if (changed)
			{
				authzGroupService().save(azgEdit);
			}
		}
		catch (GroupNotDefinedException e)
		{
			M_log.warn("dispatchUnblockMembers - site azg not defined: " + siteId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		catch (AuthzPermissionException e)
		{
			M_log.warn("dispatchUnblockMembers - no permission to site: " + siteId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	/**
	 * Format a 2 digit numeric display (at least), adding a leading '0' if needed.
	 * 
	 * @param value
	 *        The number to format.
	 * @return The 2 digit display.
	 */
	protected String formatTwoDigitNumber(Long value)
	{
		if (value < 10) return "0" + value.toString();
		return value.toString();
	}

	/**
	 * Format a # milliseconds into a time limit string (i.e. 01:30).
	 * 
	 * @param time
	 *        The time in milliseconds.
	 * @return The time limit string.
	 */
	protected String msToTimeLimit(long time)
	{
		// convert from ms to seconds
		time = time / 1000;

		// format the hours and minutes
		long hours = time / (60 * 60);
		long minutes = (time - (hours * (60 * 60))) / 60;

		return formatTwoDigitNumber(hours) + ":" + formatTwoDigitNumber(minutes);
	}

	/**
	 * Notify the member on being added to a site, possibly a new user.
	 * 
	 * @param newEmailInIdAccount
	 * @param emailId
	 * @param userName
	 * @param siteTitle
	 */
	protected void notifyMember(boolean newUser, User user, String newUserPassword, String siteTitle, String role)
	{
		String from = "\"" + serverConfigurationService().getString("ui.service", "Sakai") + "\"<no-reply@"
				+ serverConfigurationService().getServerName() + ">";
		String productionSiteName = serverConfigurationService().getString("ui.service", "");
		String productionSiteUrl = serverConfigurationService().getPortalUrl();

		String to = user.getEmail();
		String headerTo = user.getEmail();
		String replyTo = user.getEmail();
		String subject = null;
		if (newUser)
		{
			subject = "New " + productionSiteName + " User Account";
		}
		else
		{
			subject = "Added to " + productionSiteName + " Site " + siteTitle;
		}

		String content = "";

		if (from != null && user.getEmail() != null)
		{
			StringBuffer buf = new StringBuffer();

			buf.append("Dear " + user.getDisplayName() + ":\n\n");
			buf.append(userDirectoryService().getCurrentUser().getDisplayName() + " has added you to the " + productionSiteName + " site "
					+ siteTitle + ".\n\n");
			buf.append("To access this site, log on at: " + productionSiteUrl + "\n\n");

			if (newUser)
			{
				buf.append("Your Etudes user id is: " + user.getEid() + "\n\n");
				buf.append("Your temporary password is: " + newUserPassword + "\n\n");
				buf.append("Upon logging on, you will be asked to establish a new, \"strong\" password.\n\n");
				buf.append("Once you change your password, please enter your first and last name using the Account link up top.  Click on the \"Edit\" link in the \"Name\" section of the Account screen.  Enter your name and press \"Save\".\n\n");
			}
			else
			{
				buf.append("Log in using your user id " + user.getEid() + " and the password you have set for this account.\n\n");
				buf.append("If you don't remember your password, use \"Reset Password\" on the left of Etudes.\n\n");
			}

			buf.append("---------------------\n\nThis is an automatically generated email from Etudes. Do not reply to it!\n\n");

			content = buf.toString();
			emailService().send(from, to, subject, content, headerTo, replyTo, null);
		}
	}

	/**
	 * Split the source into two strings at the last occurrence of the splitter.<br />
	 * Previous occurrences are not treated specially, and may be part of the first string.
	 * 
	 * @param source
	 *        The string to split
	 * @param splitter
	 *        The string that forms the boundary between the two strings returned.
	 * @return An array of two strings split from source by splitter.
	 */
	protected String[] splitLast(String source, String splitter)
	{
		String start = null;
		String end = null;

		// find last splitter in source
		int pos = source.lastIndexOf(splitter);

		// if not found, return null
		if (pos == -1)
		{
			return null;
		}

		// take up to the splitter for the start
		start = source.substring(0, pos);

		// and the rest after the splitter
		end = source.substring(pos + splitter.length(), source.length());

		String[] rv = new String[2];
		rv[0] = start;
		rv[1] = end;

		return rv;
	}

	/**
	 * Convert the formatted time limit string (i.e. 01:30) to a # milliseconds.
	 * 
	 * @param value
	 *        the time format string.
	 * @return The # milliseconds.
	 */
	protected long timeLimitToMs(String value)
	{
		String[] parts = StringUtil.split(value, ":");
		if (parts.length == 2)
		{
			long duration = 0;
			// hours
			duration = Integer.parseInt(parts[0]) * 60l * 60l * 1000l;
			// minutes
			duration += Integer.parseInt(parts[1]) * 60l * 1000l;

			return duration;
		}

		return 0;
	}

	/**
	 * @return The AuthzGroupService, via the component manager.
	 */
	private EmailService emailService()
	{
		return (EmailService) ComponentManager.get(EmailService.class);
	}

	/**
	 * @return The ServerConfigurationService, via the component manager.
	 */
	private ServerConfigurationService serverConfigurationService()
	{
		return (ServerConfigurationService) ComponentManager.get(ServerConfigurationService.class);
	}

	/**
	 * @return The SiteService, via the component manager.
	 */
	private SiteService siteService()
	{
		return (SiteService) ComponentManager.get(SiteService.class);
	}

	/**
	 * @return The SpecialAccessToolService, via the component manager.
	 */
	private SpecialAccessToolService specialAccessToolService()
	{
		return (SpecialAccessToolService) ComponentManager.get(SpecialAccessToolService.class);
	}

	/**
	 * @return The UserDirectoryService, via the component manager.
	 */
	private UserDirectoryService userDirectoryService()
	{
		return (UserDirectoryService) ComponentManager.get(UserDirectoryService.class);
	}
}
