package org.esupportail.ecm;

import org.esupportail.ecm.versioning.EsupDocumentVersioningBean;
import org.jboss.seam.annotations.In;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.versioning.api.VersioningActions;

public class TestEsupDocumentVersioningBean extends TestAbstractDocument {
	
	@In(required = true)
	private EsupDocumentVersioningBean esupDocumentVersioning;

	@Override
	public void setUp() throws Exception {
		super.setUp();
	}
	
	/* TODO : get esupDocumentVersioning seam bean so that we can test it */
	public void testAvailableVersioningOptionsMap() throws Exception {
    	DocumentModel doc =  coreSession.getDocument(createSampleFile());
		/*
		assertNotNull("esupDocumentVersioning is null !", esupDocumentVersioning);
		assertFalse(
				"ACTION_NO_INCREMENT is present !",
				esupDocumentVersioning
				 .getAvailableVersioningOptionsMap()
				   .containsKey(VersioningActions.ACTION_NO_INCREMENT)
				);
		*/    	
    }
	
}
