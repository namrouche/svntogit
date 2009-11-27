package org.esupportail.ecm.view;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.esupportail.ecm.view.EsupUnrestrictedGetPublishingVersion;
import org.esupportail.ecm.view.EsupUnrestrictedPublishingVersionPojo;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.versioning.api.VersioningManager;

/**
 * This Seam bean manages the view tab in Esup-ECM.
 *
 *    
 * @see PublishActionsBean
 * @author Yohan Colmant
 */


@Name("esupViewActions")
@Scope(ScopeType.CONVERSATION)
@Transactional
public class EsupViewActionsBean implements Serializable  {
	private static final long serialVersionUID = -5052825564876344766L;
	private static final Log log = LogFactory.getLog(EsupViewActionsBean.class);
	@In(create = true, required = false)
	protected transient CoreSession documentManager;
	@In(create = true)
	protected transient VersioningManager versioningManager;
	
	/**
	 * @param currentDocument
	 * @return return all versions of current document without ACL control 
	 */
	public EsupUnrestrictedPublishingVersionPojo getPublishingVersion(DocumentModel currentDocument) {
		EsupUnrestrictedPublishingVersionPojo publishingVersionPojo = null;
		try {
			EsupUnrestrictedGetPublishingVersion versionsUnrestricted = new EsupUnrestrictedGetPublishingVersion(documentManager, versioningManager, currentDocument);
			versionsUnrestricted.runUnrestricted();
			publishingVersionPojo = versionsUnrestricted.getPublishingVersionPojo();
		}
		catch(ClientException e) {
			log.error("getPublishingVersion :: ClientException", e);
		}
		return publishingVersionPojo;
	}
}
