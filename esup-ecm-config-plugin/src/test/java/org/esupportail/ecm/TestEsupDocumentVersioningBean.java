package org.esupportail.ecm;

import org.jboss.seam.annotations.In;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.versioning.api.VersioningActions;

public class TestEsupDocumentVersioningBean extends TestAbstractDocument {
	
	@In(required = true)
	private EsupDocumentVersioningBean esupDocumentVersioningBean;

	@Override
	public void setUp() throws Exception {
		super.setUp();
	}
	
	public void testAvailableVersioningOptionsMap() throws Exception {
    	DocumentModel doc =  coreSession.getDocument(createSampleFile());
		
		assertNotNull("esupDocumentVersioningBean is null !", esupDocumentVersioningBean);
		assertFalse(
				"ACTION_NO_INCREMENT is present !",
				esupDocumentVersioningBean
				 .getAvailableVersioningOptionsMap()
				   .containsKey(VersioningActions.ACTION_NO_INCREMENT)
				);    	
    }

	
}
