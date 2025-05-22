/**
 * mapping function that returns all values needed to set up the download of a file for a given content-type
 * explicitly mapped contentTypes are 'config', 'log', 'instrumentation-feedback' and 'archive',
 * everything else will default to a plain .txt configuration
 * @param contentType
 * @param contextName
 * @returns {{fileExtension: string, header: string, language: string, mimeType: string}}
 * @constructor
 */
export const ContentTypeMapper = (contentType, contextName) => {
  switch (contentType) {
    case 'config':
      return {
        language: 'yaml',
        fileExtension: '.yml',
        mimeType: 'text/x-yaml',
        header: 'Configuration of ' + contextName,
      };
    case 'log':
      return {
        language: 'plaintext',
        fileExtension: '.log',
        mimeType: 'text/plain',
        header: 'Logs of ' + contextName,
      };
    case 'archive':
      return {
        language: '',
        fileExtension: '',
        mimeType: '',
        header: 'Preparing Download of the Support Archive for ' + contextName,
      };
    case 'instrumentation-feedback':
      return {
        language: 'json',
        fileExtension: '.json',
        mimeType: 'text/json',
        header: 'Current Instrumentation of ' + contextName,
      };
    default:
      return {
        language: 'plaintext',
        fileExtension: '.txt',
        mimeType: 'text/plain',
        header: 'Content of ' + contextName,
      };
  }
};
