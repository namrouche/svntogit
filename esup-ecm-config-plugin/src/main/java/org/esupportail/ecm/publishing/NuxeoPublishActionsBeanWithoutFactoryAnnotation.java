/*
 * (C) Copyright 2007-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Narcis Paslaru
 *     Florent Guillaume
 *     Thierry Martins
 *     Thomas Roger
 */

package org.esupportail.ecm.publishing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublicationTreeNotAvailable;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublisherService;
import org.nuxeo.ecm.platform.publisher.api.PublishingEvent;
import org.nuxeo.ecm.platform.publisher.web.AbstractPublishActions;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.helpers.EventManager;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This Seam bean manages the publishing tab.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@Name("nuxeoPublishActionsBeanWithoutFactoryAnnotation")
@Scope(ScopeType.CONVERSATION)
public class NuxeoPublishActionsBeanWithoutFactoryAnnotation extends AbstractPublishActions implements
        Serializable {

    public static class PublicationTreeInformation {

        private final String name;

        private final String title;

        public PublicationTreeInformation(String treeName, String treeTitle) {
            this.name = treeName;
            this.title = treeTitle;
        }

        public String getName() {
            return name;
        }

        public String getTitle() {
            return title;
        }
    }

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(NuxeoPublishActionsBeanWithoutFactoryAnnotation.class);

    @In(create = true)
    protected transient DocumentsListsManager documentsListsManager;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    protected transient PublisherService publisherService;

    protected String currentPublicationTreeNameForPublishing;

    protected PublicationTree currentPublicationTree;

    protected String publishingComment;

    protected static Set<String> sectionTypes;

    @Create
    public void create() {
        try {
            publisherService = Framework.getService(PublisherService.class);
        } catch (Exception e) {
            throw new IllegalStateException("Publisher service not deployed.",
                    e);
        }
    }

    @Destroy
    public void destroy() {
        if (currentPublicationTree != null) {
            currentPublicationTree.release();
            currentPublicationTree = null;
        }
    }

    public List<PublicationTreeInformation> getAvailablePublicationTrees()
            throws ClientException {
        Map<String, String> trees = publisherService.getAvailablePublicationTrees();
        List<PublicationTreeInformation> treesInformation = new ArrayList<PublicationTreeInformation>();
        for (Map.Entry<String, String> entry : trees.entrySet()) {
            treesInformation.add(new PublicationTreeInformation(entry.getKey(),
                    entry.getValue()));
        }
        return treesInformation;
    }

    public String doPublish(PublicationNode publicationNode) throws ClientException {
        PublicationTree tree = getCurrentPublicationTreeForPublishing();
        return doPublish(tree, publicationNode);
    }

    public String doPublish(PublicationTree tree, PublicationNode publicationNode)
            throws ClientException {
        if (tree == null) {
            return null;
        }

        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        PublishedDocument publishedDocument = tree.publish(currentDocument,
                publicationNode);
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

    public void setCurrentPublicationTreeNameForPublishing(
            String currentPublicationTreeNameForPublishing)
            throws ClientException {
        this.currentPublicationTreeNameForPublishing = currentPublicationTreeNameForPublishing;
        if (currentPublicationTree != null) {
            currentPublicationTree.release();
            currentPublicationTree = null;
        }
        currentPublicationTree = getCurrentPublicationTreeForPublishing();
    }

    public String getCurrentPublicationTreeNameForPublishing()
            throws ClientException {
        if (currentPublicationTreeNameForPublishing == null) {
            List<String> publicationTrees = new ArrayList<String>(
                    publisherService.getAvailablePublicationTree());
            if (!publicationTrees.isEmpty()) {
                currentPublicationTreeNameForPublishing = publicationTrees.get(0);
            }
        }
        return currentPublicationTreeNameForPublishing;
    }

    public PublicationTree getCurrentPublicationTreeForPublishing()
            throws ClientException {
        if (currentPublicationTree == null) {
            try {
                currentPublicationTree = publisherService.getPublicationTree(
                        getCurrentPublicationTreeNameForPublishing(),
                        documentManager, null,
                        navigationContext.getCurrentDocument());
            } catch (PublicationTreeNotAvailable e) {
                currentPublicationTree = null;
            }
        }
        return currentPublicationTree;
    }

    public String getCurrentPublicationTreeIconExpanded()
            throws ClientException {
        PublicationTree tree = getCurrentPublicationTreeForPublishing();
        return tree != null ? tree.getIconExpanded() : "";
    }

    public String getCurrentPublicationTreeIconCollapsed()
            throws ClientException {
        PublicationTree tree = getCurrentPublicationTreeForPublishing();
        return tree != null ? tree.getIconCollapsed() : "";
    }

    public List<PublishedDocument> getPublishedDocuments()
            throws ClientException {
        PublicationTree tree = getCurrentPublicationTreeForPublishing();
        if (tree == null) {
            return Collections.emptyList();
        }

        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        return tree.getExistingPublishedDocument(new DocumentLocationImpl(
                currentDocument));
    }

    public List<PublishedDocument> getPublishedDocumentsFor(String treeName)
            throws ClientException {
        if (treeName==null || "".equals(treeName)) {
            return null;
        }
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        try {
            PublicationTree tree = publisherService.getPublicationTree(
                    treeName, documentManager, null);
            return tree.getExistingPublishedDocument(new DocumentLocationImpl(
                    currentDocument));
        } catch (PublicationTreeNotAvailable e) {
            return null;
        }
    }

    public String unPublish(PublishedDocument publishedDocument)
            throws ClientException {
        PublicationTree tree = getCurrentPublicationTreeForPublishing();
        if (tree != null) {
            tree.unpublish(publishedDocument);
        }
        return null;
    }

    public boolean canPublishTo(PublicationNode publicationNode)
            throws ClientException {
        PublicationTree tree = getCurrentPublicationTreeForPublishing();
        return tree != null ? tree.canPublishTo(publicationNode) : false;
    }

    public boolean canUnpublish(PublishedDocument publishedDocument)
            throws ClientException {
        PublicationTree tree = getCurrentPublicationTreeForPublishing();
        return tree != null ? tree.canUnpublish(publishedDocument) : false;
    }

    public boolean isPublishedDocument() {
        return publisherService.isPublishedDocument(navigationContext.getCurrentDocument());
    }

    public boolean canManagePublishing() throws ClientException {
        PublicationTree tree = publisherService.getPublicationTreeFor(
                navigationContext.getCurrentDocument(), documentManager);
        PublishedDocument publishedDocument = tree.wrapToPublishedDocument(navigationContext.getCurrentDocument());
        return tree.canManagePublishing(publishedDocument);
    }

    public boolean hasValidationTask() throws ClientException {
        PublicationTree tree = publisherService.getPublicationTreeFor(
                navigationContext.getCurrentDocument(), documentManager);
        PublishedDocument publishedDocument = tree.wrapToPublishedDocument(navigationContext.getCurrentDocument());
        return tree.hasValidationTask(publishedDocument);
    }

    public boolean isPending() throws ClientException {
        PublicationTree tree = publisherService.getPublicationTreeFor(
                navigationContext.getCurrentDocument(), documentManager);
        PublishedDocument publishedDocument = tree.wrapToPublishedDocument(navigationContext.getCurrentDocument());
        return publishedDocument.isPending();
    }

    public String getPublishingComment() {
        return publishingComment;
    }

    public void setPublishingComment(String publishingComment) {
        this.publishingComment = publishingComment;
    }

    public String approveDocument() throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        PublicationTree tree = publisherService.getPublicationTreeFor(
                currentDocument, documentManager);
        PublishedDocument publishedDocument = tree.wrapToPublishedDocument(currentDocument);
        tree.validatorPublishDocument(publishedDocument, ""); //TODO new second argument
        DocumentModel sourceDocument = documentManager.getDocument(publishedDocument.getSourceDocumentRef());
        FacesContext context = FacesContext.getCurrentInstance();
        String comment = publishingComment != null
                && publishingComment.length() > 0 ? ComponentUtils.translate(
                context, "publishing.approved.with.comment",
                tree.getConfigName(), publishedDocument.getParentPath(),
                publishingComment) : ComponentUtils.translate(context,
                "publishing.approved.without.comment", tree.getConfigName(),
                publishedDocument.getParentPath());
        notifyEvent(PublishingEvent.documentPublicationApproved.name(), null,
                comment, null, sourceDocument);

        DocumentModel liveVersion = documentManager.getDocument(new IdRef(
                sourceDocument.getSourceId()));
        if (!sourceDocument.getRef().equals(liveVersion.getRef())) {
            notifyEvent(PublishingEvent.documentPublicationApproved.name(),
                    null, comment, null, liveVersion);
        }

        Events.instance().raiseEvent(EventNames.DOCUMENT_PUBLISHED);
        Events.instance().raiseEvent(EventNames.DOCUMENT_PUBLICATION_APPROVED);
        return null;
    }

    public String rejectDocument() throws ClientException {
        if (publishingComment == null || "".equals(publishingComment)) {
            facesMessages.addToControl("publishingComment",
                    FacesMessage.SEVERITY_ERROR,
                    resourcesAccessor.getMessages().get(
                            "label.publishing.reject.user.comment.mandatory"));
            return null;
        }

        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        PublicationTree tree = publisherService.getPublicationTreeFor(
                currentDocument, documentManager);
        PublishedDocument publishedDocument = tree.wrapToPublishedDocument(currentDocument);
        tree.validatorRejectPublication(publishedDocument, publishingComment);
        DocumentModel sourceDocument = documentManager.getDocument(publishedDocument.getSourceDocumentRef());

        FacesContext context = FacesContext.getCurrentInstance();
        String comment = publishingComment != null
                && publishingComment.length() > 0 ? ComponentUtils.translate(
                context, "publishing.rejected.with.comment",
                tree.getConfigName(), publishedDocument.getParentPath(),
                publishingComment) : ComponentUtils.translate(context,
                "publishing.rejected.without.comment", tree.getConfigName(),
                publishedDocument.getParentPath());
        notifyEvent(PublishingEvent.documentPublicationRejected.name(), null,
                comment, null, sourceDocument);

        DocumentModel liveVersion = documentManager.getDocument(new IdRef(
                sourceDocument.getSourceId()));
        if (!sourceDocument.getRef().equals(liveVersion.getRef())) {
            notifyEvent(PublishingEvent.documentPublicationRejected.name(),
                    null, comment, null, liveVersion);
        }

        Events.instance().raiseEvent(EventNames.DOCUMENT_PUBLICATION_REJECTED);

        return navigationContext.navigateToRef(navigationContext.getCurrentDocument().getParentRef());
    }

    public void unpublishDocumentsFromCurrentSelection() throws ClientException {
        if (!documentsListsManager.isWorkingListEmpty(DocumentsListsManager.CURRENT_DOCUMENT_SECTION_SELECTION)) {
            unpublish(documentsListsManager.getWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SECTION_SELECTION));
        } else {
            log.debug("No selectable Documents in context to process unpublish on...");
        }
        log.debug("Unpublish the selected document(s) ...");
    }

    protected void unpublish(List<DocumentModel> documentModels)
            throws ClientException {
        for (DocumentModel documentModel : documentModels) {
            PublicationTree tree = publisherService.getPublicationTreeFor(
                    documentModel, documentManager);
            PublishedDocument publishedDocument = tree.wrapToPublishedDocument(documentModel);
            tree.unpublish(publishedDocument);
        }

        Object[] params = { documentModels.size() };
        // remove from the current selection list
        documentsListsManager.resetWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SECTION_SELECTION);
        facesMessages.add(FacesMessage.SEVERITY_INFO,
                resourcesAccessor.getMessages().get("n_unpublished_docs"),
                params);
    }

    public boolean isRemotePublishedDocument(PublishedDocument publishedDocument) {
        if (publishedDocument == null) {
            return false;
        }
        return publishedDocument.getType().equals(PublishedDocument.Type.REMOTE);
    }

    public boolean isFileSystemPublishedDocument(
            PublishedDocument publishedDocument) {
        if (publishedDocument == null) {
            return false;
        }
        return publishedDocument.getType().equals(
                PublishedDocument.Type.FILE_SYSTEM);
    }

    public boolean isLocalPublishedDocument(PublishedDocument publishedDocument) {
        if (publishedDocument == null) {
            return false;
        }
        return publishedDocument.getType().equals(PublishedDocument.Type.LOCAL);
    }

    public String publishWorkList() throws ClientException {
        return publishDocumentList(DocumentsListsManager.DEFAULT_WORKING_LIST);
    }

    public DocumentModel getDocumentModelFor(String path)
            throws ClientException {
        DocumentRef docRef = new PathRef(path);
        if (documentManager.exists(docRef) && hasReadRight(path)) {
            return documentManager.getDocument(docRef);
        }
        return null;
    }

    public boolean hasReadRight(String documentPath) throws ClientException {
        return documentManager.hasPermission(new PathRef(documentPath), SecurityConstants.READ);
    }

    public String getFormattedPath(String path) throws ClientException {
        DocumentModel docModel = getDocumentModelFor(path);
        return docModel != null ? getFormattedPath(docModel) : path;
    }

    public String publishDocumentList(String listName) throws ClientException {
        List<DocumentModel> docs2Publish = documentsListsManager.getWorkingList(listName);
        DocumentModel target = navigationContext.getCurrentDocument();

        if (!getSectionTypes().contains(target.getType())) {
            return null;
        }

        PublicationNode targetNode = publisherService.wrapToPublicationNode(
                target, documentManager);
        if (targetNode == null) {
            return null;
        }

        int nbPublishedDocs = 0;
        for (DocumentModel doc : docs2Publish) {
            if (!documentManager.hasPermission(doc.getRef(),
                    SecurityConstants.READ_PROPERTIES)) {
                continue;
            }

            if (doc.isProxy()) {
                // TODO copy also copies security. just recreate a proxy.
                documentManager.copy(doc.getRef(), target.getRef(),
                        doc.getName());
                nbPublishedDocs++;
            } else {
                if (doc.hasFacet(FacetNames.PUBLISHABLE)) {
                    publisherService.publish(doc, targetNode);
                    nbPublishedDocs++;
                } else {
                    log.info("Attempted to publish non-publishable document "
                            + doc.getTitle());
                }
            }
        }

        Object[] params = { nbPublishedDocs };
        facesMessages.add(FacesMessage.SEVERITY_INFO, "#0 "
                + resourcesAccessor.getMessages().get("n_published_docs"),
                params);

        if (nbPublishedDocs < docs2Publish.size()) {
            facesMessages.add(FacesMessage.SEVERITY_WARN,
                    resourcesAccessor.getMessages().get(
                            "selection_contains_non_publishable_docs"));
        }

        EventManager.raiseEventsOnDocumentChildrenChange(navigationContext.getCurrentDocument());
        return null;
    }

    public Set<String> getSectionTypes() {
        if (sectionTypes == null) {
            sectionTypes = getTypeNamesForFacet("PublishSpace");
            if (sectionTypes == null) {
                sectionTypes = new HashSet<String>();
                sectionTypes.add("Section");
            }
        }
        return sectionTypes;
    }

    protected static Set<String> getTypeNamesForFacet(String facetName) {
        SchemaManager schemaManager;
        try {
            schemaManager = Framework.getService(SchemaManager.class);
        } catch (Exception e) {
            log.error("Exception in retrieving publish spaces : ", e);
            return null;
        }

        Set<String> publishRoots = schemaManager.getDocumentTypeNamesForFacet(facetName);
        if (publishRoots == null || publishRoots.isEmpty()) {
            return null;
        }
        return publishRoots;
    }

    public void notifyEvent(String eventId,
            Map<String, Serializable> properties, String comment,
            String category, DocumentModel dm) throws ClientException {

        // Default category
        if (category == null) {
            category = DocumentEventCategories.EVENT_DOCUMENT_CATEGORY;
        }

        if (properties == null) {
            properties = new HashMap<String, Serializable>();
        }

        properties.put(CoreEventConstants.REPOSITORY_NAME,
                documentManager.getRepositoryName());
        properties.put(CoreEventConstants.SESSION_ID,
                documentManager.getSessionId());
        properties.put(CoreEventConstants.DOC_LIFE_CYCLE,
                dm.getCurrentLifeCycleState());

        DocumentEventContext ctx = new DocumentEventContext(documentManager,
                documentManager.getPrincipal(), dm);

        ctx.setProperties(properties);
        ctx.setComment(comment);
        ctx.setCategory(category);

        EventProducer evtProducer;
        try {
            evtProducer = Framework.getService(EventProducer.class);
        } catch (Exception e) {
            log.error("Unable to access EventProducer", e);
            return;
        }

        Event event = ctx.newEvent(eventId);

        try {
            evtProducer.fireEvent(event);
        } catch (Exception e) {
            log.error("Error while sending event", e);
        }
    }

    public String getDomainName(String treeName) throws ClientException {
        try {
            PublicationTree tree = publisherService.getPublicationTree(
                    treeName, documentManager, null);
            Map<String, String> parameters = publisherService.getParametersFor(tree.getConfigName());
            String domainName = parameters.get(PublisherService.DOMAIN_NAME_KEY);
            return domainName != null ? " (" + domainName + ")" : "";
        } catch (PublicationTreeNotAvailable e) {
            return "";
        }
    }

    @Observer(value = { EventNames.DOCUMENT_SELECTION_CHANGED }, create = false, inject = false)
    @BypassInterceptors
    public void documentChanged() {
        currentPublicationTreeNameForPublishing = null;
        currentPublicationTree = null;
    }

}
