package org.esupportail.ecm.publishing;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.io.Serializable;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.esupportail.ecm.versioning.EsupVersionPojo;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.platform.publisher.web.PublishActionsBean;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublishingEvent;
import org.nuxeo.ecm.platform.publisher.impl.core.SimpleCorePublishedDocument;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.platform.versioning.api.VersioningManager;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

/**
 * This Seam bean manages the publishing tab in Esup-ECM.
 *
 *    
 * @see PublishActionsBean
 * @author Vincent Bonamy
 * @author Yohan Colmant
 * @author Raymond Bourges
 */

@Name("esupPublishActions")
@Scope(ScopeType.CONVERSATION)
@Transactional
public class EsupPublishActionsBean extends NuxeoPublishActionsBeanWithoutFactoryAnnotation implements Serializable  {

	private static final long serialVersionUID = 5062112520450522444L;

	private static final Log log = LogFactory.getLog(EsupPublishActionsBean.class);

	protected DocumentModelList filteredSectionsModel;

	//	@In(create = true)
	//	private PublishActionsBean publishActions;

	@In(create = true)
	protected transient NuxeoPrincipal currentUser;

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
		Map<String, EsupVersionPojo> ret = new TreeMap<String, EsupVersionPojo>();
		//get current document
		DocumentModel doc = navigationContext.getCurrentDocument();
		//get versions of current document
		List<VersionModel> versions = documentManager.getVersionsForDocument(doc.getRef());
		//for each version of current document
		for (VersionModel version : versions) {
			//get document from version
			DocumentModel versionDoc = documentManager.getDocumentWithVersion(doc.getRef(), version);
			if (versionDoc != null) {
				//get label of current version
				String versionLabel = versioningManager.getVersionLabel(versionDoc);
				//if this version not yet in return variable 
				if(!ret.containsKey(versionLabel)) {
					//create an EsupVersionPojo of the current version
					EsupVersionPojo esupVersionPojo = new EsupVersionPojo(versionLabel, version.getLabel(), version);
					//add this version as an EsupVersionPojo in return variable
					ret.put(versionLabel, esupVersionPojo);
				}
			}
		}
		return ret.values();
	}

	/** 
	 * Called by  esup_document_publish.xhtml
	 * -1 --> can not publish
	 * 0 --> can ask to publish
	 * 1 --> can write
	 */
	public int getCanPublishVersionToSection(VersionModel versionModel, PublicationNode publicationNode) throws ClientException {
		if (publicationNode == null || publicationNode.getParent() == null) {
			// we can't publish in the root node
			return -1;
		}
		boolean canAskForPublishing = false;
		boolean canWrite = false;
        DocumentRef publicationNodeRef = new PathRef(publicationNode.getPath());
        //Do I have CAN_ASK_FOR_PUBLISHING right ?
		if (documentManager.hasPermission(publicationNodeRef, CAN_ASK_FOR_PUBLISHING)) {
			canAskForPublishing = true;
		}
		//Do I have CAN_ASK_FOR_PUBLISHING right ?
		if (documentManager.hasPermission(publicationNodeRef, READ_WRITE)) {
			canWrite = true;
		}
		if (!canAskForPublishing && !canWrite) {
			return -1;
		}
		//Is this version version of current document already published ?
		//current document
		DocumentModel currentDocument = navigationContext.getCurrentDocument();
		//version of current document
		DocumentModel versionDocument  = documentManager.getDocumentWithVersion(currentDocument.getRef(), versionModel);
		//publication section
		DocumentModel section = documentManager.getDocument(publicationNodeRef);
		//for all proxies of version 
		for (DocumentModel proxy : documentManager.getProxies(versionDocument.getRef(), null)) {
			//if proxy published in section
			if (isProxyPublishedInSection(proxy, section)) {
				return -1;
			}			
		}
		if (canWrite) {
			return 1;
		}
		if (canAskForPublishing) {
			return 0;
		}
		return -1;
	}

	/**
	 * Publish a version to a publication node
	 * @param versionModelLabel - label of selected version
	 * @param publicationNode - note to publish to
	 * @return null
	 * @throws ClientException
	 */
	public String doPublish(String versionModelLabel, PublicationNode publicationNode) throws ClientException {
		//get current document
		DocumentModel currentDocument = navigationContext.getCurrentDocument();
		//create a VersionModel from versionModelLabel
		VersionModel versionModel = new VersionModelImpl();
		versionModel.setCreated(Calendar.getInstance());
		versionModel.setDescription("");
		versionModel.setLabel(versionModelLabel);
		//create proxy --> to publish required section  
		DocumentRef publicationNodeRef = new PathRef(publicationNode.getPath());
		DocumentModel newProxy = documentManager.createProxy(publicationNodeRef, currentDocument.getRef(), versionModel, false);
		//get publication tree
		PublicationTree tree = getCurrentPublicationTreeForPublishing();
		//create a publishedDocument from created proxy
		PublishedDocument publishedDocument = new SimpleCorePublishedDocument(newProxy);
		//cut/paste of NuxeoPublishActionsBeanWithoutFactoryAnnotation#doPublish
        FacesContext context = FacesContext.getCurrentInstance();
        if (publishedDocument.isPending()) {
            String comment = ComponentUtils.translate(context,
                    "publishing.waiting", tree.getConfigName(),
                    publicationNode.getPath());
            // Log event on live version
            notifyEvent(PublishingEvent.documentWaitingPublication.name(),
                    null, comment, null, currentDocument);
            Events.instance().raiseEvent(
                    EventNames.DOCUMENT_SUBMITED_FOR_PUBLICATION);
            facesMessages.add(FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get(
                            "document_submitted_for_publication"),
                    resourcesAccessor.getMessages().get(
                            currentDocument.getType()));
        } else {
            String comment = ComponentUtils.translate(context,
                    "publishing.done", tree.getConfigName(),
                    publicationNode.getPath());
            // Log event on live version
            notifyEvent(PublishingEvent.documentPublished.name(), null,
                    comment, null, currentDocument);
            Events.instance().raiseEvent(EventNames.DOCUMENT_PUBLISHED);
            facesMessages.add(FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get("document_published"),
                    resourcesAccessor.getMessages().get(
                            currentDocument.getType()));
        }
        return null;
	}

	/**
	 * @see org.esupportail.ecm.publishing.NuxeoPublishActionsBeanWithoutFactoryAnnotation#getPublishedDocuments()
	 */
	public List<PublishedDocument> getPublishedDocuments(EsupVersionPojo esupVersionPojo)
			throws ClientException {
		List<PublishedDocument> ret = new ArrayList<PublishedDocument>();
		//get all published documents
		List<PublishedDocument> documents = super.getPublishedDocuments();
		//look in each document if document version is published
		for (PublishedDocument document : documents) {
			if (esupVersionPojo.getVersionLabel().equals(document.getSourceVersionLabel())) {
				ret.add(document);
			}
		}
		return ret;
	}
	
	/**
	 * Return true if a proxy is published in a section
	 * @param section
	 * @param proxy
	 */
	private boolean isProxyPublishedInSection(DocumentModel proxy, DocumentModel section) {
		boolean ret = false;
		DocumentRef parentRef = proxy.getParentRef();
		DocumentRef sectionRef = section.getRef(); 
		if (parentRef.equals(sectionRef)) {
			ret=true;
		}
		log.debug("isPublished :: isPublished ? "+ret);
		return ret;
	}	
}
