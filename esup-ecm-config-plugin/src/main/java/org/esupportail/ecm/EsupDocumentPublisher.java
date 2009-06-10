package org.esupportail.ecm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.core.Events;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.platform.publishing.DocumentPublisher;

public class EsupDocumentPublisher extends DocumentPublisher {

	private static final Log log = LogFactory.getLog(EsupDocumentPublisher.class);
	
	private VersionModel versionModel;
	
	protected EsupDocumentPublisher(CoreSession coreSession, DocumentModel doc, VersionModel versionModel, DocumentModel section, String comment) throws ClientException {
		super(coreSession, doc, section, comment);
		log.info("EsupDocumentPublisher :: constructor done");
		this.versionModel = versionModel;
	}
	

    public void run() throws ClientException {
        DocumentModel doc = session.getDocument(docRef);
        DocumentModel section = session.getDocument(sectionRef);
        
        // BEGIN :: code nuxeo par defaut supprime
        if (setIssuedDate) {
            doc.setProperty("dublincore", "issued", Calendar.getInstance());
            // make sure that saveDocument doesn't create a snapshot,
            // as publishDocument will do it
            doc.putContextData(org.nuxeo.common.collections.ScopeType.REQUEST,
                    VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY,
                    Boolean.FALSE);
            session.saveDocument(doc);
        }
        // END :: begin code nuxeo par defaut supprime
        
        log.info("run :: doc="+doc);
        log.info("run :: versionModel="+versionModel);
        log.info("run :: doc.isProxy()="+doc.isProxy());
        
        // BEGIN :: code nuxeo par defaut supprime
        DocumentModel proxy = session.publishDocument(doc, section);
        // END :: begin code nuxeo par defaut supprime
        
        // BEGIN :: code ori-oai qui remplace
        /*log.info("run :: section="+section);
        DocumentModel proxy = session.createProxy(section.getRef(), doc.getRef(), versionModel, false);
        log.info("run :: proxy="+proxy);*/
        // END :: code ori-oai qui remplace
        
        session.save();
        proxyRef = proxy.getRef();
        
        // notify event
        Map<String, Serializable> eventInfo = new HashMap<String, Serializable>();
        eventInfo.put("proxy", proxy);
        eventInfo.put("targetSection", section.getName());
        eventInfo.put("sectionPath", section.getPathAsString());
        String eventId;
        log.info("run :: isUnrestricted="+isUnrestricted);
        if (isUnrestricted) {
            // submitted through workflow, this event starts the workflow
            eventId = org.nuxeo.ecm.webapp.helpers.EventNames.DOCUMENT_SUBMITED_FOR_PUBLICATION;
            log.info("run :: eventId="+eventId);
            
            // additional event info: recipients
            String[] validators = publishingService.getValidatorsFor(proxy);
            List<String> recipients = new ArrayList<String>(validators.length);
            for (String user : validators) {
                recipients.add((userManager.getGroup(user) == null ? "user:"
                        : "group:")
                        + user);
            }
            eventInfo.put("recipients", StringUtils.join(recipients, '|'));
        } else {
            // direct publishing
            eventId = org.nuxeo.ecm.webapp.helpers.EventNames.DOCUMENT_PUBLISHED;
            log.info("run :: eventId="+eventId);
        }
        
        log.info("run :: eventInfo="+eventInfo);

        // BEGIN :: code nuxeo par defaut supprime
        notifyEvent(eventId, eventInfo, comment, null, doc);
        // END :: begin code nuxeo par defaut supprime

        // BEGIN :: code ori-oai qui remplace
        //notifyEvent(eventId, eventInfo, comment, null, proxy);
        // END :: code ori-oai qui remplace
        
        if (isUnrestricted) {
            /*
             * Invalidate dashboard items using Seam since a publishing workflow
             * might have been started. XXX We need to do it here since the
             * workflow starts in a message driven bean in a async way. Not sure
             * we can optimize right now.
             */
            Events.instance().raiseEvent("WORKFLOW_NEW_STARTED");
            log.info("run :: raiseEvent="+"WORKFLOW_NEW_STARTED");
        }
    }
    
    
    
	
}
