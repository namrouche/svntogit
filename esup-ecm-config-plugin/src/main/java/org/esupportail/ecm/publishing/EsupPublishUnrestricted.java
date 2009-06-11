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
        
        log.info("EsupPublishUnrestricted :: constructor done");
	}
	
	
	
	
	@Override
    public void run() throws ClientException {
		
		log.info("run :: call method");
		
		log.info("run :: *** !!! *** docToPublish.isProxy() = "+docToPublish.isProxy());
		
		// false dans le code ecrit par Vincent
		boolean overwriteExistingProxy = false;
		
		// true dans le code d'origine
		//boolean overwriteExistingProxy = true;
		
		newProxy = session.createProxy(sectionToPublishTo.getRef(), docToPublish.getRef(), versionModel, overwriteExistingProxy);
		
		log.info("run :: newProxy="+newProxy);
        
        Locale locale = FacesContext.getCurrentInstance().getViewRoot().getLocale();
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.FULL, locale);
        newProxy.setProperty("dublincore", "dc:issued", dateFormat.getCalendar());

        log.info("run :: dc:issued added");
        
        session.save();
		
    }
	
	
	
	

	
	@Override
	public DocumentModel getModel() {
		return newProxy;
	}
	
	
	
	
	
	
	
}
