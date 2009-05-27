package org.esupportail.ecm;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.platform.publishing.PublishActionsBean;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.versioning.api.VersioningManager;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

/**
 * This Seam bean manages the publishing tab in Esup-ECM.
 *
 *    
 * @see PublishActionsBean
 * @author Vincent Bonamy
 */


@Name("esupPublishActions")
@Scope(ScopeType.CONVERSATION)
@Transactional
public class EsupPublishActionsBean {

	private static final long serialVersionUID = 5062112520450522444L;

	private static final Log log = LogFactory.getLog(EsupPublishActionsBean.class);

    protected DocumentModelList filteredSectionsModel;

	@In(create = true)
	private PublishActionsBean publishActions;
    
    @In(create = true)
    protected transient NavigationContext navigationContext;
    

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient VersioningManager versioningManager;
    
    protected static final String CAN_ASK_FOR_PUBLISHING = "CanAskForPublishing";
    
    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected transient ResourcesAccessor resourcesAccessor;
    
    
    /**
     * @return list of EsupVersionPojo
     * @throws ClientException
     * @see EsupVersionPojo
     */
    public Collection<EsupVersionPojo> getVersionsSelectModel() throws ClientException {
		
    	Map<String, EsupVersionPojo> esupVersionsMap = new TreeMap<String, EsupVersionPojo>();

		DocumentModel doc = navigationContext.getCurrentDocument();

        List<VersionModel> versions = documentManager.getVersionsForDocument(doc.getRef());
        for (VersionModel model : versions) {
            DocumentModel tempDoc = documentManager.getDocumentWithVersion(doc.getRef(), model);
            if (tempDoc != null) {
                String versionLabel = versioningManager.getVersionLabel(tempDoc);
                if(!esupVersionsMap.containsKey(versionLabel)) {
                	esupVersionsMap.put(versionLabel, new EsupVersionPojo(versionLabel, model.getLabel(), model));
                }
                
                for(DocumentModel proxy : documentManager.getProxies(tempDoc.getRef(), null)) {
                	DocumentRef parentRef = proxy.getParentRef();
                    DocumentModel section = documentManager.getDocument(parentRef);
                    EsupPublishPojo info = new EsupPublishPojo(section, versioningManager.getVersionLabel(proxy), proxy);
                    esupVersionsMap.get(versionLabel).getPublishingInfos().add(info);
                }
            }
            
        }
        
       return esupVersionsMap.values();
	}
    
    /*
     * Called by  esup_document_publish.xhtml
     * @see PublishActionsBean.getCanPublishToSection
     */   
    public boolean getCanPublishVersionToSection(VersionModel model, DocumentModel section) throws ClientException {
        Set<String> sectionRootTypes = publishActions.getSectionRootTypes();

        if (sectionRootTypes.contains(section.getType())) {
            return false;
        }

        if (!documentManager.hasPermission(section.getRef(),
        		CAN_ASK_FOR_PUBLISHING)) {
            return false;
        }
        
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        
        DocumentModel versionDocument  = documentManager.getDocumentWithVersion(currentDocument.getRef(), model);
        
        return !publishActions.isAlreadyPublishedInSection(versionDocument, section);
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

    	publishActions.unPublishDocument(proxyToUnpublish);
    	facesMessages.add(FacesMessage.SEVERITY_INFO,
                                resourcesAccessor.getMessages().get(
                                        "document_unpublished"),
                                resourcesAccessor.getMessages().get(
                                		proxyToUnpublish.getType()));

        return null;
    }

}
