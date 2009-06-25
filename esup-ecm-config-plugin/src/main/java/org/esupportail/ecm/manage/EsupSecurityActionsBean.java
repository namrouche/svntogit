package org.esupportail.ecm.manage;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.webapp.security.SecurityActionsBean;
import org.nuxeo.ecm.webapp.table.cell.AbstractTableCell;
import org.nuxeo.ecm.webapp.table.cell.IconTableCell;
import org.nuxeo.ecm.webapp.table.cell.PermissionsTableCell;
import org.nuxeo.ecm.webapp.table.cell.SelectionTableCell;
import org.nuxeo.ecm.webapp.table.cell.UserTableCell;
import org.nuxeo.ecm.webapp.table.row.UserPermissionsTableRow;

/**
 * This Seam bean is used in the rights tab in Esup-ECM.
 *
 *    
 * @see SecurityActionsBean
 * @author Yohan Colmant
 */

@Name("securityActions")
@Scope(CONVERSATION)
@Install(precedence = Install.DEPLOYMENT)
public class EsupSecurityActionsBean extends SecurityActionsBean {


	
	private static final long serialVersionUID = 3308037316230385953L;

	private static final Log log = LogFactory.getLog(EsupSecurityActionsBean.class);





	public List<String> getCurrentDocumentUsers() throws ClientException {
        List<String> currentUsers = securityData.getCurrentDocumentUsers();
        return validateUserGroupList(currentUsers);
    }

    public List<String> getParentDocumentsUsers() throws ClientException {
        List<String> parentUsers = securityData.getParentDocumentsUsers();
        return validateUserGroupList(parentUsers);
    }

    
    
    
    
    
    

    private Boolean isUserGroupInCache(String entry) {
        if (cachedValidatedUserAndGroups == null) {
            return false;
        }
        return cachedValidatedUserAndGroups.contains(entry);
    }

    private void addUserGroupInCache(String entry) {
        if (cachedValidatedUserAndGroups == null) {
            cachedValidatedUserAndGroups = new ArrayList<String>();
        }
        cachedValidatedUserAndGroups.add(entry);
    }

    private Boolean isUserGroupInDeletedCache(String entry) {
        if (cachedDeletedUserAndGroups == null) {
            return false;
        }
        return cachedDeletedUserAndGroups.contains(entry);
    }

    private void addUserGroupInDeletedCache(String entry) {
        if (cachedDeletedUserAndGroups == null) {
            cachedDeletedUserAndGroups = new ArrayList<String>();
        }

        cachedDeletedUserAndGroups.add(entry);
    }

    
    
    
    
    /**
     * Validates user/group against userManager in order to remove obsolete
     * entries (ie: deleted groups/users).
     *
     * @param usersGroups2Validate
     * @return
     * @throws ClientException
     */
    private List<String> validateUserGroupList(List<String> usersGroups2Validate)
            throws ClientException {
        // TODO :
        // 1 -should add a clean cache system to avoid
        // calling the directory : this can be problematic for big ldaps
        // 2 - this filtering should at some point be applied to acp and saved
        // back in a batch?

        List<String> returnList = new ArrayList<String>();
        for (String entry : usersGroups2Validate) {
        	
        	log.debug("validateUserGroupList :: entry="+entry);
        	
            if (entry.equals(SecurityConstants.EVERYONE)) {
                returnList.add(entry);
                continue;
            } 
            if (isUserGroupInCache(entry)) {
                returnList.add(entry);
                continue;
            }
            if (isUserGroupInDeletedCache(entry)) {
                continue;
            }

            if (userManager.getPrincipal(entry) != null) {
                returnList.add(entry);
                addUserGroupInCache(entry);
                continue;
            } else if (userManager.getGroup(entry) != null) {
                returnList.add(entry);
                addUserGroupInCache(entry);
                continue;
            } else if ("administrators".equals(entry)) {
            	returnList.add(entry);
            	addUserGroupInCache(entry);
                continue;
            } else if ("members".equals(entry)) {
            	returnList.add(entry);
            	addUserGroupInCache(entry);
                continue;
            } else {
                addUserGroupInDeletedCache(entry);
            }
        }
        return returnList;
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
	
}
