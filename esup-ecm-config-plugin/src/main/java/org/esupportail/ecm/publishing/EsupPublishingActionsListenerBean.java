package org.esupportail.ecm.publishing;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.util.List;
import java.util.ArrayList;

import org.apache.commons.collections.iterators.ArrayListIterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.publishing.PublishingActionsListenerBean;
import org.nuxeo.ecm.platform.publishing.api.PublishingException;
import org.nuxeo.runtime.api.Framework;

@Name("publishingActions")
@Scope(CONVERSATION)
@Install(precedence = Install.DEPLOYMENT)
public class EsupPublishingActionsListenerBean extends
		PublishingActionsListenerBean {

	private static final long serialVersionUID = -5020410658744707150L;


	private static final Log log = LogFactory.getLog(EsupPublishingActionsListenerBean.class);


    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    
    
    
	public String publishDocument() throws PublishingException {
		
		log.debug("publishDocument :: ");
		log.debug("publishDocument :: getPrincipalId="+currentUser.getPrincipalId());
		
		DocumentModel currentDocument  = getCurrentDocument();
		
		String result = super.publishDocument();
    	
		try {
			DocumentEventContext ctx = new DocumentEventContext(documentManager, currentUser, currentDocument);
			
			// a qui est envoye le mail
			String[] actorIds = new String[1];
			actorIds[0] = (String)currentDocument.getProperty("dublincore", "creator");
			log.debug("publishDocument :: creator="+actorIds[0]);
			ctx.setProperty("recipients", actorIds);
			
			Framework.getService(EventProducer.class).fireEvent(ctx.newEvent("documentPublicationApproved"));
		}
		catch(Exception e) {
			log.error("publishDocument :: "+e, e);
		}
		
		return result;
	}

	public String rejectDocument() throws PublishingException {

		log.debug("rejectDocument :: ");
		
		DocumentModel currentDocument  = getCurrentDocument();
	    
		String result = super.rejectDocument();
    	
		try {
			DocumentEventContext ctx = new DocumentEventContext(documentManager, currentUser, currentDocument);
			
			// a qui est envoye le mail
			String[] actorIds = new String[1];
			actorIds[0] = (String)currentDocument.getProperty("dublincore", "creator");
			log.debug("publishDocument :: creator="+actorIds[0]);
			ctx.setProperty("recipients", actorIds);
			Framework.getService(EventProducer.class).fireEvent(ctx.newEvent("documentPublicationRejected"));
		}
		catch(Exception e) {
			log.error("rejectDocument :: "+e, e);
		}
		
		return result;
	}
	
}
