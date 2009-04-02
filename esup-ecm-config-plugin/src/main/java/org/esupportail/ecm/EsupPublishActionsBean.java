package org.esupportail.ecm;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.remoting.WebRemote;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentModelTreeNode;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.publishing.PublishActions;
import org.nuxeo.ecm.platform.publishing.PublishActionsBean;
import org.nuxeo.ecm.platform.publishing.api.PublishingInformation;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModel;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModelRow;
import org.nuxeo.ecm.platform.ui.web.model.impl.SelectDataModelRowEvent;
import org.nuxeo.ecm.platform.ui.web.model.impl.SelectDataModelImpl;
import org.nuxeo.ecm.platform.versioning.api.VersioningManager;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

/**
 * This Seam bean manages the publishing tab in Esup-ECM.
 *
 *    
 * not extend PublishActionsBean to avoid exception :
 *  java.lang.IllegalStateException: duplicate factory for: currentPublishingSectionsModel (duplicates are specified in publishActions and esupPublishActions)
 *
 * @see PublishActionsBean
 * @author Vincent Bonamy
 */


@Name("esupPublishActions")
@Scope(ScopeType.CONVERSATION)
@Transactional
public class EsupPublishActionsBean extends PublishActionsBean {

	private static final long serialVersionUID = 5062112520450522444L;

	private static final Log log = LogFactory.getLog(EsupPublishActionsBean.class);

    protected DocumentModelList filteredSectionsModel;

    

    /**
     * Build a array list structure as follow :
     * List of versionItem
     *   |-[0]versionLabel
     *   |-[1]List
     *   		|-[0]versionModel
     *   		|-[1]List
     *   		|		|-[0]proxyModel 
     *   		|		|-[1]sectionModel (proxy.getParent)
     *   		|		|-[2]Map<String,Object> (proxy.getProperties("dublincore"))
     *   		|-[2]ModelLabel
     * @return list of versionItems as described above
     * @throws ClientException
     */
    public List getVersionsSelectModel() throws ClientException {
		
    	Set a = new HashSet();
    	Map<String, List> versionsDoc = new HashMap<String, List>();
    	
		DocumentModel doc = navigationContext.getCurrentDocument();

        List<VersionModel> versions = documentManager.getVersionsForDocument(doc.getRef());
        for (VersionModel model : versions) {
            DocumentModel tempDoc = documentManager.getDocumentWithVersion(
                    doc.getRef(), model);
            if (tempDoc != null) {
                VersioningDocument docVer = tempDoc.getAdapter(VersioningDocument.class);

                String versionLabel = versioningManager.getVersionLabel(tempDoc);
                if(!versionsDoc.containsKey(versionLabel)) {
                	List values = new ArrayList();
                	values.add(tempDoc);
                	values.add(new ArrayList<PublishingInformation>());
                	values.add(model.getLabel());
                	versionsDoc.put(versionLabel, values);
                }
                
                for(DocumentModel proxy : documentManager.getProxies(tempDoc.getRef(), null)) {
                	DocumentRef parentRef = proxy.getParentRef();
                    DocumentModel section = documentManager.getDocument(parentRef);
                    PublishingInformation info = new PublishingInformation(proxy, section);
                    //l.add(dublincoreProperties);
                    ((List)versionsDoc.get(versionLabel).get(1)).add(info);
                }
            }
            
        }
        
       List versionsDocList = new ArrayList();
        
       List<String> labels = new ArrayList<String>(versionsDoc.keySet());
       Collections.sort(labels);
       Collections.reverse(labels);
       
       for(String key: labels) {
    	   List v = new ArrayList();
    	   v.add(key);
    	   v.add(versionsDoc.get(key));
    	   versionsDocList.add(v);
       }
       
       return versionsDocList;
	}
    
    
    
    /*
     * Called by  esup_document_publish.xhtml
     */
    public String publishVersion(String versionModelLabel, DocumentModel section) throws ClientException {
    	
    	DocumentModel docToPublish = navigationContext.getCurrentDocument();

        VersionModel versionModel = new VersionModelImpl();

        versionModel.setCreated(Calendar.getInstance());
        versionModel.setDescription("");
        versionModel.setLabel(versionModelLabel);
            
        DocumentModel proxy = documentManager.createProxy(section.getRef(), docToPublish.getRef(), versionModel, false);
            
        Locale locale = FacesContext.getCurrentInstance().getViewRoot().getLocale();
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.FULL,
                    locale);
        proxy.setProperty("dublincore", "dc:issued",
                    dateFormat.getCalendar());
            
        documentManager.save();

        facesMessages.add(FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get("document_published"),
                    resourcesAccessor.getMessages().get(docToPublish.getType()));

        
        return null;
    }

    
    /*
     * Called by document_publish.xhtml
     */
    public String unPublishProxy(DocumentModel proxyToUnpublish) throws ClientException {

    	this.unPublishDocument(proxyToUnpublish);
    	facesMessages.add(FacesMessage.SEVERITY_INFO,
                                resourcesAccessor.getMessages().get(
                                        "document_unpublished"),
                                resourcesAccessor.getMessages().get(
                                		proxyToUnpublish.getType()));

        return null;
    }

}
