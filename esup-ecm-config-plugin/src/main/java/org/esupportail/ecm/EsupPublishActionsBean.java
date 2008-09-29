package org.esupportail.ecm;

import java.util.ArrayList;
import java.util.HashMap;
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
import org.nuxeo.ecm.core.api.DocumentModelTree;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.api.impl.DocumentModelTreeImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelTreeNodeImpl;
import org.nuxeo.ecm.platform.publishing.PublishActionsBean;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.versioning.api.VersioningManager;

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
    
    @In(create = true)
    protected transient VersioningManager versioningManager;

    public List getVersionsSelectModel() throws ClientException {
		
    	
    	Map<String, List> versionsDoc = new HashMap<String, List>();
    	
		DocumentModel doc = navigationContext.getCurrentDocument();

        List<VersionModel> versions = documentManager.getVersionsForDocument(doc.getRef());
        for (VersionModel model : versions) {
            DocumentModel tempDoc = documentManager.getDocumentWithVersion(
                    doc.getRef(), model);
            if (tempDoc != null) {
                VersioningDocument docVer = tempDoc.getAdapter(VersioningDocument.class);

                String versionLabel = versioningManager.getVersionLabel(tempDoc);
                if(!versionsDoc.containsKey(versionLabel))
                	versionsDoc.put(versionLabel, new DocumentModelTreeImpl());
                
                for(DocumentModel proxy : documentManager.getProxies(tempDoc.getRef(), null)) {
                	DocumentRef parentRef = proxy.getParentRef();
                    DocumentModel section = documentManager.getDocument(parentRef);
                    Map<String, Object> dublincoreProperties = proxy.getProperties("dublincore");
                    List l = new ArrayList();
                    l.add(proxy);
                    l.add(section);
                    l.add(dublincoreProperties);
                    versionsDoc.get(versionLabel).add(l);
                }
            }
            
        }
        
       List versionsDocList = new ArrayList();
        
       for(String key: versionsDoc.keySet()) {
    	   List v = new ArrayList();
    	   v.add(key);
    	   v.add(versionsDoc.get(key));
    	   versionsDocList.add(v);
       }
       
       return versionsDocList;
	}
    
}
