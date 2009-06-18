package org.esupportail.ecm.view;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.versioning.api.VersioningManager;


/**
 * Used to get the document versions unrestricted
 * @author Yohan Colmant
 *
 */
public class EsupUnrestrictedGetPublishingVersion extends UnrestrictedSessionRunner {

	private static final Log log = LogFactory.getLog(EsupUnrestrictedGetPublishingVersion.class);
	

    private VersioningManager versioningManager;
    private EsupUnrestrictedPublishingVersionPojo publishingVersionPojo;
	private DocumentModel currentDocument;
	
	
	public EsupUnrestrictedGetPublishingVersion(CoreSession session, VersioningManager versioningManager, DocumentModel currentDocument) {
		super(session);
		this.currentDocument = currentDocument;
		this.versioningManager = versioningManager;
		
		this.publishingVersionPojo = new EsupUnrestrictedPublishingVersionPojo();
        log.debug("EsupUnrestrictedGetPublishingVersion :: constructor done");
	}
	
	
	
	
	public void run() throws ClientException {
		
		log.debug("run :: BEGIN method");
		
		DocumentRef currentVersionRef = session.getSourceDocument(currentDocument.getRef()).getRef();
		log.debug("run :: currentVersionRef="+currentVersionRef);
		
		DocumentModel sourceDocument = session.getSourceDocument(currentVersionRef);
		this.publishingVersionPojo.setSourceDocument(sourceDocument);
		log.debug("run :: sourceDocument="+sourceDocument);
		
		DocumentRef sourceDocumentRef = this.publishingVersionPojo.getSourceDocument().getRef();
		log.debug("run :: sourceDocumentRef="+sourceDocumentRef);
		
		for (VersionModel versionModel : session.getVersionsForDocument(sourceDocumentRef)) {
			DocumentRef tempDocumentWithVersionRef = session.getDocumentWithVersion(sourceDocumentRef, versionModel).getRef();
			
			if (currentVersionRef.equals(tempDocumentWithVersionRef)) {
				String label = versioningManager.getVersionLabel(session.getDocumentWithVersion(sourceDocumentRef, versionModel));
				this.publishingVersionPojo.setCurrentVersionLabel(label);
				log.debug("run :: equals :: label="+label);
			}
		}
		
    	log.debug("run :: END method");
    }




	public EsupUnrestrictedPublishingVersionPojo getPublishingVersionPojo() {
		return publishingVersionPojo;
	}



	
	
	
	
	
}
