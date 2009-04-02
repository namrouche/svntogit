package org.esupportail.ecm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class EsupVersionPojo implements Serializable {

	private static final long serialVersionUID = 1L;

	String versionLabel;
	
	String modelLabel;
	
	List<EsupPublishPojo> publishingInfos;

	public EsupVersionPojo(String versionLabel, String modelLabel) {
		super();
		this.versionLabel = versionLabel;
		this.modelLabel = modelLabel;
		publishingInfos = new ArrayList<EsupPublishPojo>();
	}

	public String getVersionLabel() {
		return versionLabel;
	}

	public void setVersionLabel(String versionLabel) {
		this.versionLabel = versionLabel;
	}

	public String getModelLabel() {
		return modelLabel;
	}

	public void setModelLabel(String modelLabel) {
		this.modelLabel = modelLabel;
	}

	public List<EsupPublishPojo> getPublishingInfos() {
		return publishingInfos;
	}

	public void setPublishingInfos(List<EsupPublishPojo> publishingInfos) {
		this.publishingInfos = publishingInfos;
	}
	
}
