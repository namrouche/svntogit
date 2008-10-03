var proxyPublish = null;

function selectSectionForPublish(sectionRef, selected) {
  if (proxyPublish == null) {
    proxyPublish = Seam.Component.getInstance("esupPublishActions");
  }
  proxyPublish.processRemoteSelectRowEvent(sectionRef,selected,selectDataTableRowCB);
}

function initSelectedSections() {
  if (proxyPublish == null) {
    proxyPublish = Seam.Component.getInstance("esupPublishActions");
  }
  proxyPublish.initSelectedSections();
}

function popupForPublishing(windowname) {
  initSelectedSections();
  esup_popup(windowname);
}
