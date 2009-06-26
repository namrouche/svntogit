// Set permissions on the document denoted by the given UUID.
//
// This script is to be used on workspaces or folders to programmatically change
// their permissions.
//
// arg1 = the UUID of the document whose permissions are to be modified
// arg2 = "test" ; (optional) allow to view actual permissions without any modification applied
//      = the principal which the permission is  granted to ; ("members" by default) 
// arg3 = permission to grant : Read, Write, ReadWrite (defaut) , Everything
//
// Script usage is:
// > script --file modifyPermissions.js e890dbf8-64a4-4511-b3d3-a401749898a3 test
// > script --file modifyPermissions.js e890dbf8-64a4-4511-b3d3-a401749898a3 admin Everything
// 
// $Id$
//
// Exemple: 
// Comment repositionner les droits du workspace tmp ? 
// 1) utiliser nxshell 
//      cd <rep_nuxeo_52>/nuxeo-shell; ./nxshell.sh -h 127.0.0.1
// 2) dans nxshell faire :
// 2.1) se déplacer dans les dossiers 
//      cd default-domain/workspaces
// 2.2) retrouver UID du document 
//      view tmp
// 2.3) ajouter les droits ReadWrite sur le document pour tous ("members") :
//        script --file <rep_esup_ecm>/nuxeo-shell-scripts/modifyPermissions.js 4cb62b5b-7b6e-432a-8d46-76271d125ea1
//      ou
//      ajouter les droits Everything (gérer) sur le document pour l' administrateur ("admin") :
//        script --file <rep_esup_ecm>/nuxeo-shell-scripts/modifyPermissions.js 4cb62b5b-7b6e-432a-8d46-76271d125ea1 admin Everything


importPackage(java.lang);
// Using importClass instead of importPackage should work, but it doesn't :-(
// importClass(org.nuxeo.ecm.client.api.DocumentRef);
// importClass(org.nuxeo.ecm.client.api.IdRef);
// importClass(org.nuxeo.ecm.client.api.PathRef);
importPackage(org.nuxeo.ecm.core.api); // for IdRef, PathRef, DocumentRef
importPackage(org.nuxeo.ecm.core.client); // for NuxeoClient
importPackage(org.nuxeo.ecm.core.api.security); // for ACE
importPackage(org.nuxeo.ecm.core.api.security.impl); // for ACLImpl
importPackage(org.apache.commons.logging);

var log = LogFactory.getLog("org.nuxeo.ecm.shell");

function displayPermissions(documentRefi, repository) {
    var doc = repo.getDocument(documentRefi);
    log.info("doc: " + doc);
    log.info("==================================");
    var securitySummaryEntries = repo.getSecuritySummary(doc, false);
    for ( var i = 0; i < securitySummaryEntries.size(); i++) {
        log.info("Path: " + securitySummaryEntries.get(i).getDocPath());
        var acls = securitySummaryEntries.get(i).getAcp().getACLs();
        for ( var j = 0; j < acls.length; j++) {
            log.info("ACL " + j + ": " + acls[j]);
        }
    }
    log.info("==================================\n");
}

log.info("Starting ...");
// First retrieving the UUID of the document as given through the command line.
// The UUID is something like e890dbf8-64a4-4511-b3d3-a401749898a3
var params = cmdLine.getParameters();
// var docUuid = "e890dbf8-64a4-4511-b3d3-a401749898a3";
var docUuid = params[0];
var principal = params[1];
var permission = params[2];
log.info("Given doc UUID: " + docUuid);
log.info("Given principal: " + principal);
log.info("Given permission: " + permission);

// Establishing a connection to the repository,
// the methods on the repo variable being those of CoreSession.
var client = NuxeoClient.getInstance();
var repo = client.openRepository();
log.info("repo: " + repo);
// IdRef implements DocumentRef
var idRef = new IdRef(docUuid);
log.info("\n\nPermissions BEFORE:");
displayPermissions(idRef, repo);


if (params[1] != "test") {
    log.info("\n\principal: "+principal);
    if (principal == undefined) {
       principal = "members";
    }
    log.info("\n\permission: "+permission);
    if (permission == undefined) {
    	permission = "ReadWrite";
    }
    log.info("Modifying of permissions ...");
    var ace = new ACE(principal, permission, true);
    log.info("ace : " + ace);
    var acl = new ACLImpl();
    acl.setACEs( [ ace ]);
    log.info("acl : " + acl);
    var acp = new ACPImpl();
    log.info("acp : " + acp);
    acp.addACL(acl);
    
    repo.setACP(idRef, acp, false);
    log.info("\n\nPermissions AFTER:");
    displayPermissions(idRef, repo);
}
