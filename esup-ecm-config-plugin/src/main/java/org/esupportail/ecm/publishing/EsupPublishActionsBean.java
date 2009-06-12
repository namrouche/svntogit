package org.esupportail.ecm.publishing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.io.Serializable;

import javax.faces.application.FacesMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.esupportail.ecm.versioning.EsupVersionPojo;
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
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.platform.publishing.PublishActionsBean;
import org.nuxeo.ecm.platform.publishing.PublishingWebException;
import org.nuxeo.ecm.platform.publishing.api.DocumentWaitingValidationException;
import org.nuxeo.ecm.platform.publishing.api.PublishingException;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.versioning.api.VersioningManager;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

/**
 * This Seam bean manages the publishing tab in Esup-ECM.
 *
 *    
 * @see PublishActionsBean
 * @author Vincent Bonamy
 * @author Yohan Colmant
 */


@Name("esupPublishActions")
@Scope(ScopeType.CONVERSATION)
@Transactional
public class EsupPublishActionsBean implements Serializable {

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
    protected static final String READ_WRITE = "ReadWrite";
    
    protected static final String SCHEMA_PUBLISHING = "publishing";
    protected static final String SECTIONS_PROPERTY_NAME = "publish:sections";

    
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
     * Called by  esup_publication.xhtml
     * @see PublishActionsBean.isPublished
     */
    public boolean isPublished(DocumentModel sourceDocumentRef, String proxyVersionLabel, DocumentModel section) {
        try {
        	
        	List<VersionModel> versions = documentManager.getVersionsForDocument(sourceDocumentRef.getRef());
            for (VersionModel model : versions) {
                DocumentModel tempDoc = documentManager.getDocumentWithVersion(sourceDocumentRef.getRef(), model);
                if (tempDoc != null) {
                    String versionLabel = versioningManager.getVersionLabel(tempDoc);
                    if (proxyVersionLabel.equals(versionLabel)) {
                    	
	                    for(DocumentModel proxy : documentManager.getProxies(tempDoc.getRef(), null)) {
	                    	DocumentRef parentRef = proxy.getParentRef();
	                    	
	                    	if (parentRef.equals(section.getRef())) {
	                    		log.info("isPublished :: found section prentRef :: "+parentRef);
	                    		EsupJbpmPublisher publisher = new EsupJbpmPublisher();
	                    		boolean isPublished = publisher.isPublished(proxy);
	                    		log.info("isPublished :: isPublished ? "+isPublished);
	                    		return isPublished;
	                    	}
	                    }
                    }
                }
            }
        	
            log.info("isPublished :: not found, return false");
        	return false;
        }
        catch (PublishingException e) {
        	log.info("isPublished :: PublishingException", e);
            throw new IllegalStateException("Publishing service not deployed properly.", e);
        }
        catch (ClientException e) {
        	log.info("isPublished :: ClientException", e);
            throw new IllegalStateException("Publishing service not deployed properly.", e);
        }
    }
    
    
    
    
    /*
     * Called by  esup_document_publish.xhtml
     * @see PublishActionsBean.getCanPublishToSection
     */
    /**
	 * -1 --> can not publish
	 * 0 --> can ask to publish
	 * 1 --> can write
	 */
    public int getCanPublishVersionToSection(VersionModel model, DocumentModel section) throws ClientException {
    	
    	Set<String> sectionRootTypes = publishActions.getSectionRootTypes();

        if (sectionRootTypes.contains(section.getType())) {
            return -1;
        }

        boolean canAskForPublishing = false;
        boolean canWrite = false;
        
        if (documentManager.hasPermission(section.getRef(), CAN_ASK_FOR_PUBLISHING)) {
        	canAskForPublishing = true;
        }
        if (documentManager.hasPermission(section.getRef(), READ_WRITE)) {
        	canWrite = true;
        }

        if (!canAskForPublishing && !canWrite) {
        	return -1;
        }
        
        String sectionId = section.getId();
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument.hasSchema(SCHEMA_PUBLISHING)) {
            String[] sectionIdsArray = (String[]) currentDocument
                    .getPropertyValue(SECTIONS_PROPERTY_NAME);

            List<String> sectionIdsList = new ArrayList<String>();

            if (sectionIdsArray != null && sectionIdsArray.length > 0) {
                sectionIdsList = Arrays.asList(sectionIdsArray);
            }

            if (sectionIdsList.contains(sectionId)) {
                return -1;
            }
        }

    	
    	DocumentModel versionDocument  = documentManager.getDocumentWithVersion(currentDocument.getRef(), model);
        if (publishActions.isAlreadyPublishedInSection(versionDocument, section)) {
        	return -1;
        }
        if (canWrite) {
        	return 1;
        }
        if (canAskForPublishing) {
        	return 0;
        }
        
        return -1;
    }
    
    
    /*
     * Called by  esup_document_publish.xhtml
     */
    /*public String publishVersion(String versionModelLabel, DocumentModel section) throws ClientException {
    	
    	// COMMENTER ou DECOMMENTER l'une ou l'autre des methodes
    	
    	// methode qui fonctionne sans la demande de publication 
    	return publishVersionFonctionneSansDemandeDePublication(versionModelLabel, section);
    	
    	// methode en cours de developpement par Yohan pour remettre d'applom la demande de publication
    	//return publishVersionPourCorrectionFutureDeDemandeDePublication(versionModelLabel, section);
    }*/
    
    
   /* public String publishVersionFonctionneSansDemandeDePublication(String versionModelLabel, DocumentModel section) throws ClientException {
    	
    	DocumentModel docToPublish = navigationContext.getCurrentDocument();

        VersionModel versionModel = new VersionModelImpl();

        versionModel.setCreated(Calendar.getInstance());
        versionModel.setDescription("");
        versionModel.setLabel(versionModelLabel);
        
        
        DocumentModel proxy = documentManager.createProxy(section.getRef(), docToPublish.getRef(), versionModel, false);
       
        
        Locale locale = FacesContext.getCurrentInstance().getViewRoot().getLocale();
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.FULL, locale);
        proxy.setProperty("dublincore", "dc:issued", dateFormat.getCalendar());

        documentManager.save();

        facesMessages.add(FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get("document_published"),
                    resourcesAccessor.getMessages().get(docToPublish.getType()));
         
        
        return null;
    }*/
    
    
    
    /*
    public String publishVersionPourCorrectionFutureDeDemandeDePublication(String versionModelLabel, DocumentModel section) throws ClientException {
    	
    	DocumentModel docToPublish = navigationContext.getCurrentDocument();

        VersionModel versionModel = new VersionModelImpl();

        versionModel.setCreated(Calendar.getInstance());
        versionModel.setDescription("");
        versionModel.setLabel(versionModelLabel);
        
        
        // BEGIN :: code actuel ori-oai 
        //DocumentModel proxy = documentManager.createProxy(section.getRef(), docToPublish.getRef(), versionModel, false);
        // END :: code actuel ori-oai
        
        // BEGIN :: proposition de code pour moderation
        if (!documentManager.hasPermission(section.getRef(), SecurityConstants.READ)) {
        	throw new ClientException("Cannot publish because not enough rights");
        }
        
        EsupDocumentPublisher documentPublisher = new EsupDocumentPublisher(documentManager, docToPublish, versionModel, section, "demande de publication");
        
        // If not enough rights to creating content, bypass rights since READ is
        // enough for publishing.
        if (!documentManager.hasPermission(section.getRef(), SecurityConstants.ADD_CHILDREN)) {
        	log.info("publishVersion :: runUnrestricted :: PAS la permission de ADD_CHILDREN");
        	documentPublisher.runUnrestricted();
        } else {
        	log.info("publishVersion :: run :: permission de ADD_CHILDREN");
        	documentPublisher.run();
        }

        DocumentModel proxy =  documentManager.getDocument(documentPublisher.proxyRef);
        
        // teste methode d'origine
        //publishActions.publishDocument(docToPublish, section);
        
        // END :: proposition de code pour moderation
        
        /*Locale locale = FacesContext.getCurrentInstance().getViewRoot().getLocale();
        //DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.FULL, locale);
        //proxy.setProperty("dublincore", "dc:issued", dateFormat.getCalendar());

        // BEGIN :: proposition de code pour moderation
        //proxy.putContextData(org.nuxeo.common.collections.ScopeType.REQUEST, VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, Boolean.FALSE);
        // END :: proposition de code pour moderation
        
        //documentManager.save();

        //facesMessages.add(FacesMessage.SEVERITY_INFO, resourcesAccessor.getMessages().get("document_published"), resourcesAccessor.getMessages().get(docToPublish.getType()));
        
        
        return null;
    }
    */
    
    
    
    
    @In(create = true)
    protected transient NuxeoPrincipal currentUser;
    
    
    
    public String doPublish(String versionModelLabel, DocumentModel section) throws ClientException {
    	DocumentModel currentDocument = navigationContext.getCurrentDocument();
    	
    	VersionModel versionModel = new VersionModelImpl();
    	versionModel.setCreated(Calendar.getInstance());
    	versionModel.setDescription("");
    	versionModel.setLabel(versionModelLabel);
    	
    	//log.info("doPublish :: publishingService.getClass() = " + publishingService.getClass());
    	
    	boolean isPublished = false;
    	boolean isWaiting = false;
    	try {
    		log.info("doPublish :: going to submitToPublication");
    		
    		EsupJbpmPublisher publisher = new EsupJbpmPublisher();
    		publisher.submitToPublication(currentDocument, versionModel, section, currentUser);
    		
    		isPublished = true;
    		
    		log.info("doPublish :: isPublished = true");
    		}
    	catch (DocumentWaitingValidationException e) {
    		isWaiting = true;
    		log.info("doPublish :: isWaiting = true");
    		}
    	catch (PublishingException e) {
    		log.info("doPublish :: PublishingException ", e);
    		throw new PublishingWebException(e);
    		}
    	
    	if (isPublished) {
    		//comment = null;
    		facesMessages.add(FacesMessage.SEVERITY_INFO, resourcesAccessor.getMessages().get("document_published"), resourcesAccessor.getMessages().get(currentDocument.getType()));
    		}
    	
    	if (isWaiting) {
    		//comment = null;
    		facesMessages.add(FacesMessage.SEVERITY_INFO, resourcesAccessor.getMessages().get("document_submitted_for_publication"),resourcesAccessor.getMessages().get(currentDocument.getType()));
    		}
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
