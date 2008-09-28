package org.esupportail.ecm;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;


/**
 * @author bourges
 * Test Class for UPortalDirectoryFactory
 */
public class Test extends NXRuntimeTestCase {

	private static final String OSGI_BUNDLE_NAME = "org.esup.ecm";
    private static final Log LOG = LogFactory.getLog(Test.class);
//	private UPortalDirectoryFactory factory;

	public Test() {
		super();
	}

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle(OSGI_BUNDLE_NAME);
    }

    public void testServiceRegistration() throws Exception {
    }
    
}

