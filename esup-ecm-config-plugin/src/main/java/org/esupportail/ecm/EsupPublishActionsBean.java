package org.esupportail.ecm;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentModelTree;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelTreeImpl;
import org.nuxeo.ecm.platform.publishing.PublishActionsBean;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;

/**
 * This Seam bean manages the publishing tab in Esup-ECM.
 *
 * @author Vincent Bonamy
 */

@Scope(ScopeType.CONVERSATION)
@Name("esupPublishActions")
@Transactional
public class EsupPublishActionsBean extends PublishActionsBean {

    private static final Log log = LogFactory.getLog(EsupPublishActionsBean.class);
    
	private static final long serialVersionUID = 1L;


    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected transient CoreSession documentManager;
    

    public List<DocumentModel> getVersionsSelectModel() throws ClientException {
		
		DocumentModelTree versions = new DocumentModelTreeImpl();
		
		DocumentModel currentDocument = navigationContext.getCurrentDocument();
        DocumentRef currentDocRef = currentDocument.getRef();
        DocumentRef currentParentRef = currentDocument.getParentRef();
        Map<String, DocumentModel> existingPublishedProxy; 
        DocumentModelList publishedProxies = documentManager.getProxies(
                currentDocRef, null);
       return publishedProxies;
	}
    
}
