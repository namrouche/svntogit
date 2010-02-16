Les patches présents ici sont destinés à corriger NX sans attendre de version "officielle" de nuxeo. 

nuxeo-platform-publisher-jbpm-5.3.0-NXP-3764.jar :
[Publication] Action de validation de la publication seulement visible pour les utilisateurs en écriture et pas pour les groupes en écriture
cf. https://jira.nuxeo.org/browse/SUPNXP-1716 reproduction en 5.3 de https://jira.nuxeo.org/browse/NXP-3764

nuxeo-core-storage-sql-1.6.0-SUPNXP-1790.jar :
Problème de performance avec MySQL
Cf.  https://jira.nuxeo.org/browse/SUPNXP-1790
Le diff (des modifs esup par rapport à une version 5.3.0 de nuxeo) contenu dans le ticket est aussi, pour des questions de sauvegardes ESUP, dans diff/nuxeo-core-storage-sql.diff

nuxeo-platform-webapp-core-5.3.0-SOURCESUP-6414.jar :
Problème de lisibilité des boutons désactivés
Cf. https://sourcesup.cru.fr/tracker/index.php?func=detail&aid=6414&group_id=430&atid=1800
Le diff est dans diff/nuxeo-platform-webapp-core.diff

nuxeo-platform-web-common-*.jar :
Acces SharePoint avec authentification anonyme
Cf. https://jira.nuxeo.org/browse/SUPNXP-1865

