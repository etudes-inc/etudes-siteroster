/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/siteroster/trunk/siteroster-webapp/webapp/src/java/org/etudes/siteroster/cdp/SiteRosterServlet.java $
 * $Id: SiteRosterServlet.java 5253 2013-06-22 00:26:26Z ggolden $
 ***********************************************************************************
 *
 * Copyright (c) 2013 Etudes, Inc.
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

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.cdp.api.CdpHandler;
import org.etudes.cdp.api.CdpService;
import org.sakaiproject.component.cover.ComponentManager;

/**
 */
public class SiteRosterServlet extends HttpServlet
{
	/** Our log (commons). */
	private static Log M_log = LogFactory.getLog(SiteRosterServlet.class);

	private static final long serialVersionUID = 1L;

	protected CdpHandler handler = null;

	/**
	 * Shutdown the servlet.
	 */
	public void destroy()
	{
		M_log.info("destroy()");
		
		if (this.handler != null)
		{
			CdpService cdpService = (CdpService) ComponentManager.get(CdpService.class);
			if (cdpService != null)
			{
				cdpService.UnregisterCdpHandler(handler);
				M_log.info("destroy(): unregistered handler");
			}
		}
		
		super.destroy();
	}

	/**
	 * Access the Servlet's information display.
	 * 
	 * @return servlet information.
	 */
	public String getServletInfo()
	{
		return "SiteRoster";
	}

	/**
	 * Initialize the servlet.
	 * 
	 * @param config
	 *        The servlet config.
	 * @throws ServletException
	 */
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);

		// create and register the cdp handler - run when the CdpService is available
		final CdpHandler handler = new SiteRosterCdpHandler();
		this.handler = handler;
		ComponentManager.whenAvailable(CdpService.class, new Runnable()
		{
			public void run()
			{
				CdpService cdpService = (CdpService) ComponentManager.get(CdpService.class);
				cdpService.registerCdpHandler(handler);
				M_log.info("init(): registered handler");
			}
		});
	}
}
