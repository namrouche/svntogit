package org.esupportail.ecm.publishing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.platform.jbpm.NuxeoJbpmException;
import org.nuxeo.ecm.platform.publishing.api.DocumentWaitingValidationException;
import org.nuxeo.ecm.platform.publishing.api.PublishingException;
import org.nuxeo.ecm.platform.publishing.api.PublishingValidatorException;
import org.nuxeo.ecm.platform.publishing.jbpm.JbpmPublisher;


/**
 * Used in place of org.nuxeo.ecm.platform.publishing.jbpm.JbpmPublisher
 * @author Yohan Colmant
 *
 */
public class EsupJbpmPublisher extends JbpmPublisher {

	private static final Log log = LogFactory.getLog(EsupJbpmPublisher.class);
	
	
	
	
	
	public void submitToPublication(DocumentModel document, VersionModel versionModel, DocumentModel placeToPublishTo, NuxeoPrincipal principal) throws PublishingException, DocumentWaitingValidationException {
		log.debug("submitToPublication :: call method");
		
		DocumentModel newProxy;
        CoreSession coreSession;
        
        try {
            coreSession = getCoreSession(document.getRepositoryName(), principal);
            log.debug("submitToPublication :: coreSession="+coreSession);
        }
        catch (ClientException e2) {
        	log.error("submitToPublication :: ", e2);
            throw new IllegalStateException("No core session available.", e2);
        }
        
        try {
        	log.debug("submitToPublication :: going to publish");
            newProxy = publish(document, versionModel, placeToPublishTo, principal, coreSession);
            log.debug("submitToPublication :: newProxy = "+newProxy);
            notifyEvent(PublishingEvent.documentSubmittedForPublication, newProxy, coreSession);
            log.debug("submitToPublication :: event notified 1");
        }
        catch (ClientException e1) {
        	log.error("submitToPublication :: ", e1);
            throw new PublishingException(e1);
        }
        if (!isValidator(newProxy, principal)) {
            try {
                notifyEvent(PublishingEvent.documentWaitingPublication, newProxy, coreSession);
                log.debug("submitToPublication :: event notified 2");
                restrictPermission(newProxy, principal, coreSession);
                log.debug("submitToPublication :: restricted permission");
                createTask(newProxy, coreSession, principal);
                log.debug("submitToPublication :: created task");
                throw new DocumentWaitingValidationException();
            }
            catch (PublishingValidatorException e) {
            	log.error("submitToPublication :: ", e);
                throw new PublishingException(e);
            }
            catch (NuxeoJbpmException e) {
            	log.error("submitToPublication :: ", e);
                throw new PublishingException(e);
            }
        }
        else {
            notifyEvent(PublishingEvent.documentPublished, newProxy, coreSession);
            log.debug("submitToPublication :: event notified 3");
        }
	}
	
	
	
	protected DocumentModel publish(final DocumentModel document, VersionModel versionModel, final DocumentModel sectionToPublishTo, NuxeoPrincipal principal, CoreSession coreSession) throws ClientException {
		log.debug("publish :: call method");
		EsupPublishUnrestricted publisher = new EsupPublishUnrestricted(coreSession, document, versionModel, sectionToPublishTo);
		log.debug("publish :: publisher="+publisher);
        publisher.runUnrestricted();
        log.debug("publish :: runUnrestricted done");
        return publisher.getModel();
    }
	
}
