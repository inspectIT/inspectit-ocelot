import JSZip from 'jszip';
import { saveAs } from 'file-saver';
import { getFile } from '../redux/ducks/configuration/actions';

const params = {};
let zip = new JSZip();

/**
 * Attempts to download the currently selected data or handed in file or folder.
 * In case a single file is selected it will be downloaded immediately
 * In case a folder with files is selected the files content gets collected and compressed before downloading
 */
export function downloadSelection(selectionContent, downloadName) {
  if (!(selectionContent instanceof Array)) {
    // Create blob link to download
    const content = new Blob([selectionContent], { type: 'text/plain;charset=utf-8' });
    // Start download
    saveAs(content, `${downloadName}`);
  } else {
    const index = selectionContent.findIndex((value) => value.name === downloadName);
    selectionContent[index].children.forEach((child) => {
      const requestedChild = `/${selectionContent[index].name}/${child.name}`;
      if (child.type === 'directory') {
        const subDirIndex = selectionContent[index].children.indexOf(child);
        loadAndCompressSubFolderContent(requestedChild, child.name, selectionContent[index].children[subDirIndex]);
      } else {
        getFile(requestedChild, params)
          .then((payload) => {
            const fileContent = payload;
            // Create blob link to download
            const content = new Blob([fileContent], { type: 'text/plain;charset=utf-8' });
            zip.file(`${child.name}`, content);
          })
          .catch((err) => {
            console.error(err);
          })
          .finally(() => {
            zip
              .generateAsync({ type: 'blob' })
              .then(function (content) {
                // Start downloading compressed files
                saveAs(content, `${selectionContent[index].name}.zip`);
              })
              .finally(
                // Reset JSZip content cache
                (zip = new JSZip())
              );
          });
      }
    });
  }
}

/**
 * Attempts to compress the currently selected data of a sub folder.
 * In case the sub folder contains single files the data is compressed immediately
 * In case a sub folder within the sub folder is found all data gets collected and compressed recursively
 */
function loadAndCompressSubFolderContent(subFolderPath, subFolderName, subFolderContent) {
  // Create sub folder inside zip
  const subFolder = zip.folder(subFolderName);
  subFolderContent.children.forEach((child) => {
    const requestedChild = `${subFolderPath}/${child.name}`;
    if (child.type === 'directory') {
      loadAndCompressSubFolderContent(requestedChild, child.name, child);
    } else {
      getFile(requestedChild, params)
        .then((payload) => {
          const fileContent = payload;
          // Create blob link to download
          const content = new Blob([fileContent], { type: 'text/plain;charset=utf-8' });
          subFolder.file(`${child.name}`, content);
        })
        .catch((err) => {
          console.error(err);
        })
        .finally(() => {
          zip.generateAsync({ type: 'blob' }).then(function () {
            // Compressing content
          });
        });
    }
  });
}

export function downloadArchiveFromJson(json, agentId, agentVersion) {
  for (let key of Object.keys(json)) {
    let content = json[key];
    let mimetype = 'text/plain;charset=utf-8';
    let fileExtension = '.txt';

    switch (key) {
      case 'currentConfig':
        mimetype = 'text/x-yaml;charset=utf-8';
        fileExtension = '.yml';
        break;
      case 'logs':
        fileExtension = '.log';
        break;
    }

    if (typeof content === 'object') {
      content = JSON.stringify(content, null, 2);
    }
    const file = new Blob([content], { type: mimetype });
    zip.file(`${key}${fileExtension}`, file);
  }
  zip
    .generateAsync({ type: 'blob' })
    .then((archive) => saveAs(archive, `${agentId}_${agentVersion}.zip`))
    .finally((zip = new JSZip()));
}
