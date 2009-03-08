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

/* functions derivated from tabelSelections.js to handle multi selection for sections */

function ori_selectSectionsPage(tableId,selected) {
       if (proxyPublish == null) {
    	proxyPublish = Seam.Component.getInstance("esupPublishActions");
       }
      proxyPublish.processRemoteSelectedSections(selected);
      ori_handleAllCheckBoxes(tableId, selected);
}


function ori_handleAllCheckBoxes(tableName, checked) {
// here care to have correct table name (build by jsf form tree)
  var table = document.getElementById(tableName);
  var listOfInputs = table.getElementsByTagName("input");
  for(var i = 0; i < listOfInputs.length; i++ ){
    if (listOfInputs[i].type == "checkbox"){
      listOfInputs[i].checked = checked;
    }
  }
}
