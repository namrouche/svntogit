/**
 * 
 */
package org.esupportail.ecm.web.auth;

import java.io.IOException;
import java.util.Map;

import javax.security.auth.login.LoginException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoAuthenticationFilter;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author bourges
 *
 */
public class CasLoginServlet extends HttpServlet {

	protected PluggableAuthenticationService service;
	private static final Log log = LogFactory.getLog(NuxeoAuthenticationFilter.class);

	/**
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException {
		String baseURL = service.getBaseURL(req);
		String redirectURL = "/";
		String validPluginName = null;
		UserIdentificationInfo userIdent = null;
		for (String pluginName : service.getAuthChain()) {
			NuxeoAuthenticationPlugin plugin = service.getPlugin(pluginName);
			if (plugin != null) {
				log.debug("Trying to retrieve userIndetification using plugin " + pluginName);
				userIdent = plugin.handleRetrieveIdentity(req, resp);
				if (userIdent != null && userIdent.containsValidIdentity()) {
					// fill information for the Login module
					validPluginName = pluginName;
					break;
				}
			} else {
				log.error("Auth plugin " + pluginName
						+ " can not be retrieved from service");
			}
		}
		if (validPluginName != null) {
			if (validPluginName.equals("ANONYMOUS_AUTH")) {
				redirectURL = "logout";
			}
		}   
		resp.sendRedirect(baseURL + redirectURL);
	}

	/**
	 * @see javax.servlet.GenericServlet#init()
	 */
	@Override
	public void init() throws ServletException {
		service = (PluggableAuthenticationService) Framework.getRuntime().getComponent(
				PluggableAuthenticationService.NAME);
		if (service == null) {
			log.error("Unable to get Service "
					+ PluggableAuthenticationService.NAME);
			throw new ServletException(
			"Can't initialize Nuxeo Pluggable Authentication Service");
		}
		super.init();
	}


}
