package org.esupportail.ecm.publishing;

import java.text.DateFormat;
import java.util.Locale;

import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.publishing.PublishUnrestricted;


/**
 * Used in place of org.nuxeo.ecm.platform.publishing.PublishUnrestricted
 * @author Yohan Colmant
 *
 */
public class EsupPublishUnrestricted extends PublishUnrestricted {

	private static final Log log = LogFactory.getLog(EsupPublishUnrestricted.class);
	
	private DocumentModel newProxy;
    final private DocumentModel docToPublish;
    final private VersionModel versionModel;
    final private DocumentModel sectionToPublishTo;
	
	public EsupPublishUnrestricted(CoreSession session, DocumentModel docToPublish, VersionModel versionModel, DocumentModel sectionToPublishTo) {
		super(session, docToPublish, sectionToPublishTo);
		
        this.docToPublish = docToPublish;
        this.versionModel = versionModel;
        
        this.sectionToPublishTo = sectionToPublishTo;
        
        log.debug("EsupPublishUnrestricted :: constructor done");
	}
	
    public void run() throws ClientException {
		log.debug("run :: call method");
		log.debug("run :: *** !!! *** docToPublish.isProxy() = "+docToPublish.isProxy());
		// false in Vincent's code (true in nuxeo's code)
		boolean overwriteExistingProxy = false;
		//boolean overwriteExistingProxy = true;		
		newProxy = session.createProxy(sectionToPublishTo.getRef(), docToPublish.getRef(), versionModel, overwriteExistingProxy);
		log.debug("run :: newProxy="+newProxy);
        Locale locale = FacesContext.getCurrentInstance().getViewRoot().getLocale();
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.FULL, locale);
        newProxy.setProperty("dublincore", "dc:issued", dateFormat.getCalendar());
        log.debug("run :: dc:issued added");
        session.save();
    }

	public DocumentModel getModel() {
		return newProxy;
	}
}
