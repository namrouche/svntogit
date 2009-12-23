package org.esupportail.ecm.versioning;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.versioning.api.VersioningActions;
import org.nuxeo.ecm.webapp.versioning.DocumentVersioningBean;

/**
 * This Seam bean manages specific actions in versionning tab of Esup-ECM.
 *    
 * @see DocumentVersioningBean
 * @author Raymond Bourges
 */


@Name("esupDocumentVersioning")
@Scope(CONVERSATION)
public class EsupDocumentVersioningBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final Log log = LogFactory.getLog(EsupDocumentVersioningBean.class);
	
	@In(create = true)
	private DocumentVersioningBean documentVersioning;

	@In(create = true)
	protected transient NavigationContext navigationContext;

	@In(create = true, required = false)
	protected transient CoreSession documentManager;
	
	/**
	 * @see org.nuxeo.ecm.webapp.versioning.DocumentVersioningBean#getAvailableVersioningOptionsMap()
	 * remove ACTION_NO_INCREMENT from returned Map
	 */
	public Map<String, String> getAvailableVersioningOptionsMap() {
		Map<String, String> originalMap = documentVersioning.getAvailableVersioningOptionsMap();
		Map<String, String> ret = new LinkedHashMap<String, String>();
		for (String key : originalMap.keySet()) {
			if (log.isDebugEnabled()) {
				log.debug("ret("+key+") ----> " + originalMap.get(key));
			}
			
			String originalValue = originalMap.get(key);
			if (!originalValue.equals(VersioningActions.ACTION_NO_INCREMENT.name()) && !originalValue.equals(VersioningActions.ACTION_INCREMENT_MINOR.name())) {
				ret.put(key, originalMap.get(key));
			}
			
			
		}
		return ret;
	}

	/**
	 * @see org.nuxeo.ecm.webapp.versioning.DocumentVersioningBean#getVersioningOptionInstanceId()
	 * change return value in case of ACTION_NO_INCREMENT from returned Map
	 */
    public String getVersioningOptionInstanceId() {
    	String ret = documentVersioning.getVersioningOptionInstanceId();
    	if (ret.equals(VersioningActions.ACTION_NO_INCREMENT.name())) {
			//ret = VersioningActions.ACTION_INCREMENT_MINOR.name();
			ret = VersioningActions.ACTION_INCREMENT_MAJOR.name();
			
		} 
        return ret;
    }
	
	/**
	 * @see org.nuxeo.ecm.webapp.versioning.DocumentVersioningBean#setVersioningOptionInstanceId()
	 * needed by JSF
	 */
    public void setVersioningOptionInstanceId(String optionId)
    throws ClientException {
    	documentVersioning.setVersioningOptionInstanceId(optionId);
    }
    
	/**
	 * @return the esupVersionURL
	 */
	public String getEsupVersionURL(VersionModel versionModel) throws ClientException {
		//find uid of version
		//get current document
		DocumentModel doc = navigationContext.getCurrentDocument();
		//get document from version
		DocumentModel versionDoc = documentManager.getDocumentWithVersion(doc.getRef(), versionModel);
		String docID = versionDoc.getId();
		return "site/esupversions/" + docID;
	}    
	
}
