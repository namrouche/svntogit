package org.esupportail.ecm.publishing;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.publishing.PublishPojo;

public class EsupPublishPojo extends PublishPojo {

	DocumentModel proxy;
	
	public EsupPublishPojo(DocumentModel section, String proxyVersion, DocumentModel proxy) {
		super(section, proxyVersion);
		this.proxy = proxy;
	}

	public DocumentModel getProxy() {
		return proxy;
	}

	public void setProxy(DocumentModel proxy) {
		this.proxy = proxy;
	}

}
