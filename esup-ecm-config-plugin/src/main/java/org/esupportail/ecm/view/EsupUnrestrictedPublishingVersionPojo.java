package org.esupportail.ecm.view;

import org.nuxeo.ecm.core.api.DocumentModel;

public class EsupUnrestrictedPublishingVersionPojo {



	private DocumentModel sourceDocument;
	private String currentVersionLabel="";
	
	
	

	public void setSourceDocument(DocumentModel sourceDocument) {
		this.sourceDocument = sourceDocument;
	}




	public void setCurrentVersionLabel(String currentVersionLabel) {
		this.currentVersionLabel = currentVersionLabel;
	}




	public DocumentModel getSourceDocument() {
		return sourceDocument;
	}




	public String getCurrentVersionLabel() {
		return currentVersionLabel;
	}
	
}
