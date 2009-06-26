// just search for some documents to rename them
// arg1 = title of the documents to be renamed
// arg2 = new title
//
// needed addtionnal bundles (in app/bundles)
// nuxeo-core-query-1.4.1.jar
// nuxeo-platform-login-5.1.4.jar

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

// get documents with select title using the searchService
function getDocuments(title) {
    var searchService = SearchServiceDelegate.getRemoteSearchService();
    query = new ComposedNXQueryImpl("select * from Document where dc:title='"
            + title + "'");
    result = searchService.searchQuery(query, 0, 1000);
    log.info("found " + result.size() + " matching documents.");
    var idList = [];
    for (i = 0; i < result.size(); i++) {
        res = result.get(i).get("ecm:id");
        idList.push(res);
    }
    // log.info("found document ref=" + idList);
    return idList;
}

// rename the documents identified by docIdList
function renameDocuments(docIdList, newTitle) {
    var client = NuxeoClient.getInstance();
    var repo = client.openRepository();

    for (idx = 0; idx < docIdList.length; idx++) {
        var docRef = docIdList[idx];
        var doc = repo.getDocument(new IdRef(docRef));
        log.info("updating doc " + docRef + " with dc:title " + newTitle);
        doc.setProperty("dublincore", "title", newTitle);
        repo.saveDocument(doc);
    }
    repo.save();
}

log.info("running batch rename script ...");

var params = cmdLine.getParameters();
var findTitle = params[0];
findTitle = findTitle.replace("\"", "")
findTitle = findTitle.replace("\"", "")
var replaceTitle = params[1];
replaceTitle = replaceTitle.replace("\"", "")
replaceTitle = replaceTitle.replace("\"", "")
docIdList = getDocuments(findTitle);
if (docIdList.length > 0) {
    log.info("updating documents ...");
    renameDocuments(docIdList, replaceTitle);
} else {
    log.info("no documents found ... nothing to replace");
}
