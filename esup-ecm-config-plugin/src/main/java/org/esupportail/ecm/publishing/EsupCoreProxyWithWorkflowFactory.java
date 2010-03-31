/**
 * 
 */
package org.esupportail.ecm.publishing;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublishingEvent;
import org.nuxeo.ecm.platform.publisher.impl.core.CoreFolderPublicationNode;
import org.nuxeo.ecm.platform.publisher.impl.core.SimpleCorePublishedDocument;
import org.nuxeo.ecm.platform.publisher.jbpm.CoreProxyWithWorkflowFactory;

/**
 * @author bourges
 *
 */
public class EsupCoreProxyWithWorkflowFactory extends CoreProxyWithWorkflowFactory {
	
     /**
     * @see org.nuxeo.ecm.platform.publisher.jbpm.CoreProxyWithWorkflowFactory#publishDocument(org.nuxeo.ecm.core.api.DocumentModel, org.nuxeo.ecm.platform.publisher.api.PublicationNode, java.util.Map)
     */
    public PublishedDocument publishDocument(DocumentModel doc,
            PublicationNode targetNode, Map<String, String> params)
            throws ClientException {
        DocumentModel targetDocModel;
        if (targetNode instanceof CoreFolderPublicationNode) {
            CoreFolderPublicationNode coreNode = (CoreFolderPublicationNode) targetNode;
            targetDocModel = coreNode.getTargetDocumentModel();
        } else {
            targetDocModel = coreSession.getDocument(new PathRef(
                    targetNode.getPath()));
        }
        boolean overwriteProxy = (!((params != null) && (params.containsKey("overwriteExistingProxy"))))
        	|| Boolean.parseBoolean(params.get("overwriteExistingProxy"));
        EsupPublishUnrestricted publisher = new EsupPublishUnrestricted(coreSession,
                doc, targetDocModel, overwriteProxy);
        publisher.runUnrestricted();
        return publisher.getPublishedDocument();
    }

    protected class EsupPublishUnrestricted extends UnrestrictedSessionRunner {

        private PublishedDocument result;
        private final DocumentModel docToPublish;
        private final DocumentModel sectionToPublishTo;
        private final boolean overwriteProxy;
        protected NuxeoPrincipal principal;


        public EsupPublishUnrestricted(CoreSession session, DocumentModel docToPublish,
                DocumentModel sectionToPublishTo) {
            this(session, docToPublish, sectionToPublishTo, true);
        }

        public EsupPublishUnrestricted(CoreSession session, DocumentModel docToPublish,
                DocumentModel sectionToPublishTo, boolean overwriteProxy) {
            super(session);
            this.sectionToPublishTo = sectionToPublishTo;
            this.docToPublish = docToPublish;
            this.overwriteProxy = overwriteProxy;
            this.principal = (NuxeoPrincipal) session.getPrincipal();
        }

        /**
         * @see org.nuxeo.ecm.core.api.UnrestrictedSessionRunner#run()
         */
        public void run() throws ClientException {
        	if (!docToPublish.isVersion()) {
        		throw new ClientException("You try to publish an non versioned document");
        	}
        	//Retrieve liveVersion from version
        	DocumentModel liveVersion = session.getDocument(new IdRef(docToPublish.getSourceId()));
        	//get versionModels from liveVersion
        	List<VersionModel> versionModels = session.getVersionsForDocument(liveVersion.getRef());
        	//find in all VersionModel the one corresponding docToPublish
        	VersionModel versionModel = null;
        	for (VersionModel tmp : versionModels) {
        		//get version document
        		DocumentModel versionDoc = session.getDocumentWithVersion(liveVersion.getRef(), tmp);
        		if (versionDoc.equals(docToPublish)) {
    				versionModel = tmp;
    			}
    		}
        	//create proxy --> to publish required section  
        	//this.newProxy is used because it have to be returned by getModel()
        	DocumentModel newProxy = session.createProxy(sectionToPublishTo.getRef(), liveVersion.getRef(), versionModel, overwriteProxy);
        	SimpleCorePublishedDocument publishedDocument = new SimpleCorePublishedDocument(newProxy);
        	session.save();
        	
        	//TODO: Ajouter les autres fonction de run()
            if (!isValidator(newProxy, principal)) {
                notifyEvent(PublishingEvent.documentWaitingPublication,
                		session.getDocument(newProxy.getRef()),
                		session);
                restrictPermission(newProxy, principal, session, null);
                createTask(newProxy, session, principal);
                publishedDocument.setPending(true);
            } else {
                notifyEvent(PublishingEvent.documentPublished, newProxy,
                		session);
            }
            result = publishedDocument;
        }

        public PublishedDocument getPublishedDocument() throws ClientException {
            return result;
        }

    }

}
