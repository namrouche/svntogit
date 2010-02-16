/**
 * 
 */
package org.esupportail.ecm.publishing;

import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.impl.core.CoreFolderPublicationNode;
import org.nuxeo.ecm.platform.publisher.jbpm.CoreProxyWithWorkflowFactory;

/**
 * @author bourges
 *
 */
public class EsupCoreProxyWithWorkflowFactory extends CoreProxyWithWorkflowFactory {
	
	/**
	 * @see org.nuxeo.ecm.platform.publisher.jbpm.CoreProxyWithWorkflowFactory#publish(org.nuxeo.ecm.core.api.DocumentModel, org.nuxeo.ecm.platform.publisher.api.PublicationNode, java.util.Map)
	 * use EsupPublishUnrestricted
	 */
	protected DocumentModel publish(DocumentModel doc,
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
        return publisher.getModel();
	}
	

}
