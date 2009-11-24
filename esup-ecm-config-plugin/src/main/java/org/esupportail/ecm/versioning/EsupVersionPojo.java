package org.esupportail.ecm.versioning;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

//import org.esupportail.ecm.publishing.EsupPublishPojo;
import org.nuxeo.ecm.core.api.VersionModel;

public class EsupVersionPojo implements Serializable {

	private static final long serialVersionUID = 1L;

	String versionLabel;
	
	String modelLabel;
	
	VersionModel model;

	public EsupVersionPojo(String versionLabel, String modelLabel, VersionModel model) {
		super();
		this.versionLabel = versionLabel;
		this.modelLabel = modelLabel;
		this.model = model;
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

	public VersionModel getModel() {
		return model;
	}

	public void setModel(VersionModel model) {
		this.model = model;
	}

}
