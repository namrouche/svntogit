Fonctionnent avec la version nuxeo-dm-5.2.0

Les patches présents ici sont destinés à corriger NX sans attendre de patch "officiel". 
Les patches sont compilés via mvn install.

nuxeo-core-storage-sql-1.5.0.jar :
SQL Storage: make MySQL connector useable in MySQL master/slave mode
cf. http://jira.nuxeo.org/browse/NXP-3691
cf. http://hg.nuxeo.org/nuxeo/nuxeo-core/rev/250750d56ec4

nuxeo-platform-publishing-workflow-5.2.0.jar :
[Publication] Action de validation de la publication seulement visible pour les utilisateurs en écriture et pas pour les groupes en écriture
cf. http://jira.nuxeo.org/browse/SUPNXP-1085


