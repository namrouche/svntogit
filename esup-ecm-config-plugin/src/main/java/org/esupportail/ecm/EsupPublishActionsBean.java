package org.esupportail.ecm;

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
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.RequestParameter;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.WebRemote;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelTreeNode;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.api.impl.DocumentModelTreeNodeImpl;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.platform.publishing.PublishActionsBean;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModel;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModelRow;
import org.nuxeo.ecm.platform.ui.web.model.impl.SelectDataModelImpl;
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
  
    @RequestParameter
    private String proxySelectedRef;
    
    @RequestParameter  
    private String versionModelLabel;
    
    

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
            boolean moderation = !isAlreadyPublishedInSection(docToPublish,
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
    
    	unPublishDocument(proxyToUnpublish);
    	facesMessages.add(FacesMessage.SEVERITY_INFO,
                                resourcesAccessor.getMessages().get(
                                        "document_unpublished"),
                                resourcesAccessor.getMessages().get(
                                		proxyToUnpublish.getType()));
    	
    	initSelectedSections();
        
        return null;
    }
    
    
    
    
    public SelectDataModel getSimpleSectionsModel() throws ClientException {
    	SelectDataModel selectDataModels = getSectionsModel();
    	List<SelectDataModelRow> sections = getSectionsModel().getRows();
    	List<String> publishedSectionsNames = getPublishedSectionsNames();
    	List<DocumentModel> filteredSections = new ArrayList<DocumentModel>();
   	 	
   	 		for (SelectDataModelRow section : sections) {
   	 		DocumentModelTreeNode sectionModel = (DocumentModelTreeNode)section.getData();
   	 			
   	 		boolean moderation = !isAlreadyPublishedInSection(navigationContext.getCurrentDocument(),
                sectionModel.getDocument());
   	 		if(!moderation){
   	 			filteredSections.add(sectionModel.getDocument());
   	 		}
   	 		/*
   	 		String sectionPath = ((DocumentModel)section.getData()).getPathAsString();
   	    	
   	 		if( !publishedSectionsNames.contains(sectionPath) ){
   	 			filteredSections.add((DocumentModel)section.getData());
   	 		}
   	 		*/
   	 	}
   	 	SelectDataModel filteredSectionsModel = new SelectDataModelImpl("filteredSections", filteredSections, null);
   	 	return filteredSectionsModel;
    }
    
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

    /*
     * Called by Seam remoting.
     */
    @WebRemote
    public String initSelectedSections() throws ClientException {
    	List<SelectDataModelRow> sections = getSectionsModel().getRows();
    	 for (SelectDataModelRow d : sections) {
            	 d.setSelected(false);
         }
    	this.setSelectedSections(null);
        return "OK";
    }
    
    /*
     * Called by Seam remoting.
     */
    @WebRemote
    public String processRemoteSelectRowEvent(String docRef, Boolean selection)
            throws ClientException {
    	return super.processRemoteSelectRowEvent(docRef, selection);
    }
    
    /*
     * Called by Seam remoting. Process multiselected sections
     */
    @WebRemote
    public String processRemoteSelectedSections(Boolean selection) throws ClientException {
    	List<SelectDataModelRow> sections = getSectionsModel().getRows();
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
