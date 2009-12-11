package org.esupportail.ecm.publishing;

import java.util.Calendar;
import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.platform.publisher.helper.VersioningHelper;

/**
 * Used in place of org.nuxeo.ecm.platform.publishing.PublishUnrestricted
 * @author Yohan Colmant
 *
 */
public class EsupPublishUnrestricted extends UnrestrictedSessionRunner {

    private DocumentModel newProxy;

    private final DocumentModel docToPublish;

    private final DocumentModel sectionToPublishTo;

    private final boolean overwriteProxy;

    public EsupPublishUnrestricted(CoreSession session, DocumentModel docToPublish,
            DocumentModel sectionToPublishTo) {
        this(session, docToPublish, sectionToPublishTo, true);
    }

    public EsupPublishUnrestricted(CoreSession session, DocumentModel docToPublish,
            DocumentModel sectionToPublishTo, boolean overwriteProxy) {
        super(session);
        this.sectionToPublishTo = sectionToPublishTo;
        this.docToPublish = docToPublish;
        this.overwriteProxy = overwriteProxy;
    }

    /**
     * @see org.nuxeo.ecm.core.api.UnrestrictedSessionRunner#run()
     * here we create a proxy of version (old doc param) before calling nuxeo code in order to avoid version creation 
     */
    @Override
    public void run() throws ClientException {
    	if (!docToPublish.isVersion()) {
    		throw new ClientException("You try to publish an non versioned document");
    	}
    	//Retrieve liveVersion from version
    	DocumentModel liveVersion = session.getDocument(new IdRef(docToPublish.getSourceId()));
    	//get versionModels from liveVersion
    	List<VersionModel> versionModels = session.getVersionsForDocument(liveVersion.getRef());
    	//find in all VersionModel the one corresponding docToPublish
    	VersionModel versionModel = null;
    	for (VersionModel tmp : versionModels) {
    		//get version document
    		DocumentModel versionDoc = session.getDocumentWithVersion(liveVersion.getRef(), tmp);
    		if (versionDoc.equals(docToPublish)) {
				versionModel = tmp;
			}
		}
//    	//get versionLabel from version
//    	String versionLabel = VersioningHelper.getVersionLabelFor(docToPublish);
//    	//create a VersionModel from versionModelLabel
//    	VersionModel versionModel = new VersionModelImpl();
//    	versionModel.setCreated(Calendar.getInstance());
//    	versionModel.setDescription("");
//    	versionModel.setLabel(versionLabel);
    	//create proxy --> to publish required section  
    	//this.newProxy is used because it have to be returned by getModel()
    	this.newProxy = session.createProxy(sectionToPublishTo.getRef(), liveVersion.getRef(), versionModel, false);
//    	newProxy = session.publishDocument(tmpProxy, sectionToPublishTo, overwriteProxy);
    	session.save();
    }

    public DocumentModel getModel() {
        return newProxy;
    }

}
