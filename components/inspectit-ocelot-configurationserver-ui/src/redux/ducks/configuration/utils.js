import { find } from 'lodash';

/**
 * Returns the file object for the given path.
 * If the file does not ecist, null is returned.
 * 
 * @param {*} rootFiles the list of the files within the root directory
 * @param {*} path the path of the file to get
 */
export function getFile(rootFiles, path) {
    if (path.startsWith("/")) {
        path = path.substring(1);
    }
    const segmentNames = path.split("/");
    let filesList = rootFiles;
    let foundFile = null;

    for (let idx in segmentNames) {
        const segmentName = segmentNames[idx];

        foundFile = find(filesList, { name: segmentName });
        if (!foundFile) {
            return null;
        } else {
            filesList = foundFile.children || [];
        }
    }
    return foundFile;
}

/**
 * Reutrns the path of the parent folder of the given file.
 * If the given file is on root level, an empty string is returned.
 * 
 * @param {*} path the path of the file
 */
export function getParentDirectoryPath(path) {
    const lastSlash = path.lastIndexOf("/");
    return lastSlash == -1 ? "" : path.substring(0,lastSlash);
}

/**
 * true if the given file object is a directory.
 */
export function isDirectory(file) {
    return file && file.type === "directory";
}