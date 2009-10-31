package org.esupportail.ecm.publishing;

import org.nuxeo.ecm.core.api.DocumentModel;

public class EsupPublishPojo {

	DocumentModel proxy;
	
	public EsupPublishPojo(DocumentModel section, String proxyVersion, DocumentModel proxy) {
		//super(section, proxyVersion);
		//TODO: find new method!
		this.proxy = proxy;
	}

	public DocumentModel getProxy() {
		return proxy;
	}

	public void setProxy(DocumentModel proxy) {
		this.proxy = proxy;
	}

}
