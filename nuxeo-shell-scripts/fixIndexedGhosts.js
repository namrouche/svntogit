// Remove documents that are present in the index but not in the repository.
//
// This can happen in case of missing sync between the Compass index and the repo.
//
// arg1 = the path of the directory to check
//
// Script usage is:
// > script --file fixIndexedGhosts.js /default-domain
//
// Needed additional bundles
// (put them in the bundles or app/bundles directory
// before starting the nuxeo shell):
//
// - nuxeo-core-query-xyz.jar (for example nuxeo-core-query-1.4.1.jar
//   or nuxeo-core-query-1.5-SNAPSHOT.jar according to the nuxeo shell version)
// - nuxeo-platform-login-xyz.jar (for example nuxeo-platform-login-5.1.4.jar
//   or nuxeo-platform-login-5.2-SNAPSHOT.jar according to the nuxeo shell version)
//
// Additionally you might need to add the java-cup-0.11a.jar to the lib/ folder
// if using nuxeo shell 5.2.

importPackage(java.lang);
importPackage(org.nuxeo.ecm.client);
importPackage(org.nuxeo.ecm.core.client);
importPackage(org.nuxeo.ecm.core.api);
importPackage(org.nuxeo.ecm.core.search.api.client);
importPackage(org.nuxeo.ecm.core.search.api.client.common);
importPackage(org.nuxeo.ecm.core.search.api.client.query.impl);
importPackage(org.nuxeo.ecm.core.query.sql.model);
importPackage(org.apache.commons.logging);

var log = LogFactory.getLog("org.nuxeo.ecm.shell");

function usage() {
    log.info("Usage:\n"
             + "script --file fixIndexedGhosts.js PATH\n"
             + "Example:\n"
             + "script --file fixIndexedGhosts.js /default-domain\n");
}

function getChildrenViaSearch(parentIdRef) {
    var searchService = SearchServiceDelegate.getRemoteSearchService();
    query = new ComposedNXQueryImpl(
            "SELECT * FROM Document WHERE ecm:parentId = '" + parentIdRef
                    + "' AND ecm:isCheckedInVersion = 0");
    result = searchService.searchQuery(query, 0, 1000);
    log.info("found " + result.size() + " matching documents.");
    var idList = [];
    for (i = 0; i < result.size(); i++) {
        res = result.get(i).get("ecm:id").toString();
        idList.push(res + ''); // convert Java String to JS String
    }
    // log.info("found document ref=" + idList);
    return idList;
}

function getChildrenViaCore(parentIdRef) {
    var childrenList = repo.getChildren(new IdRef(parentIdRef));

    var idList = [];
    for (i = 0; i < childrenList.size(); i++) {
        doc = childrenList.get(i);
        // convert java String to js String
        idList.push(doc.getRef().toString() + '');
    }
    return idList;
}

function findGhosts(coreIdList, searchIdList) {
    var ghostList = [];

    for (i = 0; i < searchIdList.length; i++) {
        id = searchIdList[i];
        idx = coreIdList.indexOf(id);
        if (idx < 0) {
            // doc is present in index but not in repo : this is a ghost !
            log.info("doc "+ id
                    + " was found in search index but not in core listing : this is a ghost");
            ghostList.push(id);
        }
    }

    return ghostList;
}


var params = cmdLine.getParameters();
if (params.length != 1) {
    usage();
    throw new Error("Missing argument");
}
var parentPath = params[0];
log.info("Running batch ghost hunt from path " + parentPath + " ...");

var client = NuxeoClient.getInstance();
log.info("client = " + client);
var repo = client.openRepository();
log.info("repo = " + repo);

var parentRef = new PathRef(parentPath);
log.info("parentRef = " + parentRef);

if (repo.exists(parentRef)) {
    var parent = repo.getDocument(new PathRef(parentPath));
    var parentIdRef = parent.getRef();
} else {
    parentIdRef = parentPath;
}

childrenViaSearch = getChildrenViaSearch(parentIdRef.toString());
childrenViaCore = getChildrenViaCore(parentIdRef.toString());

log.info("search returned " + childrenViaSearch);
log.info("core returned " + childrenViaCore);

ghostList = findGhosts(childrenViaCore, childrenViaSearch);
if (ghostList.length == 0) {
    log.info("No ghost found!");
} else {
    log.info("found " + ghostList.length + " ghost(s)");
    var searchService = SearchServiceDelegate.getRemoteSearchService();
    for (i = 0; i < ghostList.length; i++) {
        log.info("unindexing doc with id=" + ghostList[i]);
        searchService.deleteAggregatedResources(ghostList[i]);
    }
}
