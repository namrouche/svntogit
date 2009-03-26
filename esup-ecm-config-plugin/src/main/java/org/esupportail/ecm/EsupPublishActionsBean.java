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
import org.nuxeo.ecm.core.api.DocumentModelTreeNode;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.publishing.PublishActions;
import org.nuxeo.ecm.platform.publishing.PublishActionsBean;
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
 *
 * @author Vincent Bonamy
 */


@Name("esupPublishActions")
@Scope(ScopeType.CONVERSATION)
@Transactional
public class EsupPublishActionsBean implements PublishActions, Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 5062112520450522444L;



	private static final Log log = LogFactory.getLog(EsupPublishActionsBean.class);
    
	
	
    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected transient CoreSession documentManager;
    
    @In(create = true)
    protected transient VersioningManager versioningManager;
  
    @RequestParameter
    private String proxySelectedRef;
    
    @RequestParameter  
    private String versionModelLabel;
    
    @In(create = true)
    protected transient PublishActions publishActions;
    
    private SelectDataModel filteredSectionsModel;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;
    
    @In(create = true)
    protected transient ResourcesAccessor resourcesAccessor;
    

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
                	values.add(new ArrayList());
                	values.add(model.getLabel());
                	versionsDoc.put(versionLabel, values);
                }
                
                for(DocumentModel proxy : documentManager.getProxies(tempDoc.getRef(), null)) {
                	DocumentRef parentRef = proxy.getParentRef();
                    DocumentModel section = documentManager.getDocument(parentRef);
                    Map<String, Object> dublincoreProperties = proxy.getProperties("dublincore");
                    List l = new ArrayList();
                    l.add(proxy);
                    l.add(section);
                    l.add(dublincoreProperties);
                    ((List)versionsDoc.get(versionLabel).get(1)).add(l);
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
    public String publishVersion() throws ClientException {
    	
    	DocumentModel docToPublish = navigationContext.getCurrentDocument();
    	
        if (documentManager.getLock(docToPublish.getRef()) != null) {
            facesMessages.add(FacesMessage.SEVERITY_WARN,
                    resourcesAccessor.getMessages().get(
                            "error.document.locked.for.publish"));
            return null;
        }

        List<DocumentModelTreeNode> selectedSections = getSelectedSections();
        if (selectedSections.isEmpty()) {
            facesMessages.add(FacesMessage.SEVERITY_WARN,
                    resourcesAccessor.getMessages().get(
                            "publish.no_section_selected"));

            return null;
        }

        log.debug("selected " + selectedSections.size() + " sections");

        /**
         * Proxies for which we need a moderation. Let's request the moderation
         * after the document manager session has been saved to avoid conflicts
         * in between sync txn and async txn that can start before the end of
         * the sync txn.
         */
        List<DocumentModel> forModeration = new ArrayList<DocumentModel>();

        for (DocumentModelTreeNode section : selectedSections) {
            boolean moderation = !((PublishActionsBean)publishActions).isAlreadyPublishedInSection(docToPublish,
                    section.getDocument());

            VersionModel versionModel = new VersionModelImpl();

            versionModel.setCreated(Calendar.getInstance());
            versionModel.setDescription("");
            versionModel.setLabel(versionModelLabel);
            
            DocumentModel proxy = documentManager.createProxy(section.getDocument().getRef(), docToPublish.getRef(), versionModel, false);
            
            Locale locale = FacesContext.getCurrentInstance().getViewRoot().getLocale();
            DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.FULL,
                    locale);
            proxy.setProperty("dublincore", "dc:issued",
                    dateFormat.getCalendar());
            
            documentManager.save();
            
            if (moderation && !isReviewer(proxy)) {
                forModeration.add(proxy);
            }
        }

        // A document is considered published if it doesn't have
        // approval from section's manager
        boolean published = false;
        if (selectedSections.size() > forModeration.size()) {
            published = true;
        }

        if (published) {

            // notifyEvent(org.nuxeo.ecm.webapp.helpers.EventNames.DOCUMENT_PUBLISHED,
            // null, comment,
            // null, docToPublish);

            facesMessages.add(FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get("document_published"),
                    resourcesAccessor.getMessages().get(docToPublish.getType()));
        }

        if (!forModeration.isEmpty()) {
            // notifyEvent(org.nuxeo.ecm.webapp.helpers.EventNames.DOCUMENT_SUBMITED_FOR_PUBLICATION,
            // null, comment,
            // null, docToPublish);
            facesMessages.add(FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get(
                            "document_submitted_for_publication"),
                    resourcesAccessor.getMessages().get(docToPublish.getType()));
            for (DocumentModel proxy : forModeration) {
                notifyEvent(
                        org.nuxeo.ecm.webapp.helpers.EventNames.PROXY_PUSLISHING_PENDING,
                        null, null, null, proxy);
            }
        }

        initSelectedSections();
        
        return null;
    }

    
    /*
     * Called by document_publish.xhtml
     */
    public String unPublishProxy() throws ClientException {

    	DocumentModel proxyToUnpublish = documentManager.getDocument(new IdRef(proxySelectedRef));
    
    	((PublishActionsBean)publishActions).unPublishDocument(proxyToUnpublish);
    	facesMessages.add(FacesMessage.SEVERITY_INFO,
                                resourcesAccessor.getMessages().get(
                                        "document_unpublished"),
                                resourcesAccessor.getMessages().get(
                                		proxyToUnpublish.getType()));
    	
    	initSelectedSections();
        
        return null;
    }
    
    
    
    /**
     * Return a filtered list of sections 
     * to avoid double items, only sections where this version is not yet published are in returned list
     * @param versionRef
     * @return
     * @throws ClientException
     */
    public SelectDataModel getSimpleSectionsModel(DocumentRef versionRef) throws ClientException {
    	SelectDataModel selectDataModels = getSectionsModel();
    	List<SelectDataModelRow> sections = getSectionsModel().getRows();
    	List<DocumentModelTreeNode> filteredSections = new ArrayList<DocumentModelTreeNode>();
    	DocumentModel version = documentManager.getDocument(versionRef);
   	 	
   	 		for (SelectDataModelRow section : sections) {
   	 		DocumentModelTreeNode sectionModel = (DocumentModelTreeNode)section.getData();
   	 			
   	 		boolean moderation = !((PublishActionsBean)publishActions).isAlreadyPublishedInSection(version,
                sectionModel.getDocument());
   	 		if(moderation){
   	 			filteredSections.add(sectionModel);
   	 		}
   	 	}
   	 	filteredSectionsModel = new SelectDataModelImpl("filteredSections", filteredSections, null);
   	  	return filteredSectionsModel;
    }
    
       
    /**
    protected List<String> getPublishedSectionsNames() throws ClientException{
    	DocumentModel doc = navigationContext.getCurrentDocument();
    	List<String> result = new ArrayList<String>();
         List<VersionModel> versions = documentManager.getVersionsForDocument(doc.getRef());
        for (VersionModel model : versions) {
            DocumentModel tempDoc = documentManager.getDocumentWithVersion(
                    doc.getRef(), model);
            if (tempDoc != null) {
            	  for(DocumentModel proxy : documentManager.getProxies(tempDoc.getRef(), null)) {
                  	DocumentRef parentRef = proxy.getParentRef();
                      DocumentModel section = documentManager.getDocument(parentRef);
                      result.add(section.getPathAsString());
            	  }
            }
        }
         return result;   
       
    }
    */

    /*
     * Called by Seam remoting.
     */
    @WebRemote
    public String initSelectedSections() throws ClientException {
    	List<SelectDataModelRow> sections = getSectionsModel().getRows();
    	 for (SelectDataModelRow d : sections) {
            	 d.setSelected(false);
         }
    	 ((PublishActionsBean)publishActions).setSelectedSections(null);
        return "OK";
    }
    
    /*
     * Called by Seam remoting.
     */
    @WebRemote
    public String processRemoteSelectRowEvent(String docRef, Boolean selection)
            throws ClientException {
    	return publishActions.processRemoteSelectRowEvent(docRef, selection);
    }

    

    @Factory(autoCreate = true, scope = ScopeType.EVENT, value = "esupCurrentPublishingSectionsModel")
    public SelectDataModel getSectionsModel() throws ClientException {
        return publishActions.getSectionsModel();
    }


	public void cancelTheSections() {
		publishActions.cancelTheSections();
	}



	public void destroy() {
		publishActions.destroy();
	}



	public List<Action> getActionsForPublishDocument() {
		return publishActions.getActionsForPublishDocument();
	}



	public List<Action> getActionsForSectionSelection() {
		return publishActions.getActionsForSectionSelection();
	}



	public String getComment() {
		return publishActions.getComment();
	}


	public Set<String> getSectionRootTypes() {
		return publishActions.getSectionRootTypes();
	}



	public Set<String> getSectionTypes() {
		return publishActions.getSectionTypes();
	}



	public List<DocumentModelTreeNode> getSelectedSections() {
		return publishActions.getSelectedSections();
	}



	public boolean hasValidationTask() {
		return publishActions.hasValidationTask();
	}



	public boolean isPublished() {
		return publishActions.isPublished();
	}



	public boolean isReviewer(DocumentModel dm) throws ClientException {
		return publishActions.isReviewer(dm);
	}



	public void notifyEvent(String eventId,
			Map<String, Serializable> properties, String comment,
			String category, DocumentModel dm) throws ClientException {
		publishActions.notifyEvent(eventId, properties, comment, category, dm);
	}



	public String publishDocument() throws ClientException {
		return publishActions.publishDocument();
	}



	public DocumentModel publishDocument(DocumentModel docToPublish,
			DocumentModel section) throws ClientException {
		return publishActions.publishDocument(docToPublish, section);
	}



	public String publishDocumentList(String listName) throws ClientException {
		return publishActions.publishDocumentList(listName);
	}



	public String publishWorkList() throws ClientException {
		return publishActions.publishWorkList();
	}



	public void setComment(String comment) {
		publishActions.setComment(comment);
	}



	public String unPublishDocument() throws ClientException {
		return publishActions.unPublishDocument();
	}



	public void unPublishDocumentsFromCurrentSelection() throws ClientException {
		publishActions.unPublishDocumentsFromCurrentSelection();
	}



	public void processSelectRowEvent(SelectDataModelRowEvent event)
			throws ClientException {
		publishActions.processSelectRowEvent(event);
	}
    

    /*
     * Called by Seam remoting. Process multiselected sections
     */
    @WebRemote
    public String processRemoteSelectedSections(Boolean selection) throws ClientException {
    	List<SelectDataModelRow> sections = null;
    	if(filteredSectionsModel != null){
    		sections = filteredSectionsModel.getRows();
    	}else{
    		sections = getSectionsModel().getRows();
    	}
    	 
    	 for (SelectDataModelRow d : sections) {
    		    DocumentModelTreeNode section = (DocumentModelTreeNode)d.getData();
    		    
    		    
    		    if (selection) {
    		    	log.debug("SECTION ADDED"+section.getDocument().getRef().toString() );
    	            getSelectedSections().add(section);
    	        } else {
    	        	log.debug("SECTION REMOVED"+section.getDocument().getRef().toString() );
    	            getSelectedSections().remove(section);
    	        }
         }
        return "OK";
    }
    

}
