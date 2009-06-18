package org.esupportail.ecm.view;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.esupportail.ecm.publishing.EsupJbpmPublisher;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.versioning.api.VersioningManager;


/**
 * Used to check is version if published unrestricted
 * @author Yohan Colmant
 *
 */
public class EsupUnrestrictedIsPublished extends UnrestrictedSessionRunner {

	private static final Log log = LogFactory.getLog(EsupUnrestrictedIsPublished.class);
	

    private boolean published = false;
    
    private VersioningManager versioningManager;
	private DocumentModel sourceDocumentRef;
	private String proxyVersionLabel;
	private DocumentModel section;
	
	
	public EsupUnrestrictedIsPublished(CoreSession session, VersioningManager versioningManager, DocumentModel sourceDocumentRef, String proxyVersionLabel, DocumentModel section) {
		super(session);
		this.versioningManager = versioningManager;
		this.sourceDocumentRef = sourceDocumentRef;
		this.proxyVersionLabel = proxyVersionLabel;
		this.section = section;
		
        log.debug("EsupUnrestrictedIsPublished :: constructor done");
	}
	
	
	
	
	public void run() throws ClientException {
		
		log.debug("run :: BEGIN method");
		
		//try {
        	
        	List<VersionModel> versions = session.getVersionsForDocument(sourceDocumentRef.getRef());
            for (VersionModel model : versions) {
                DocumentModel tempDoc = session.getDocumentWithVersion(sourceDocumentRef.getRef(), model);
                if (tempDoc != null) {
                    String versionLabel = versioningManager.getVersionLabel(tempDoc);
                    if (proxyVersionLabel.equals(versionLabel)) {
                    	
	                    for(DocumentModel proxy : session.getProxies(tempDoc.getRef(), null)) {
	                    	DocumentRef parentRef = proxy.getParentRef();
	                    	
	                    	if (parentRef.equals(section.getRef())) {
	                    		log.debug("run :: found section parentRef :: "+parentRef);
	                    		EsupJbpmPublisher publisher = new EsupJbpmPublisher();
	                    		boolean isPublished = publisher.isPublished(proxy);
	                    		log.debug("run :: isPublished ? "+isPublished);
	                    		if (isPublished == true) {
	                    			published = isPublished;
	                    		}
	                    	}
	                    }
                    }
                }
            }
        	
        /*}
        catch (PublishingException e) {
        	log.debug("isPublished :: PublishingException", e);
            throw new IllegalStateException("Publishing service not deployed properly.", e);
        }
        catch (ClientException e) {
        	log.debug("isPublished :: ClientException", e);
            throw new IllegalStateException("Publishing service not deployed properly.", e);
        }*/
		
    	log.debug("run :: END method");
    }




	public boolean isPublished() {
		return published;
	}



	
	
	
	
	
}
