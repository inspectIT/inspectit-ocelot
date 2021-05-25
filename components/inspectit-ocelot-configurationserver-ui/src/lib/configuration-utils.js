import { CONFIGURATION_TYPES } from '../data/constants';

/**
 * Returns the type of the specified configuration. In case the configuration is null or empty
 * YAML will be returned.
 *
 * @param {string} yamlConfiguration
 */
export const getConfigurationType = (yamlConfiguration) => {
  if (!yamlConfiguration) {
    // empty files will also result in YAML types.
    return CONFIGURATION_TYPES.YAML;
  }

  const lines = yamlConfiguration.split('\n');
  const headerLine = lines[0].trim();

  // special files must define a header comment
  if (!headerLine.startsWith('#')) {
    return CONFIGURATION_TYPES.YAML;
  }

  const jsonString = headerLine.substring(1);
  try {
    const { type } = JSON.parse(jsonString);
    const fileType = CONFIGURATION_TYPES[type];

    return fileType ? fileType : CONFIGURATION_TYPES.YAML;
  } catch (error) {
    // YAML type in case the first comment is no JSON object
    return CONFIGURATION_TYPES.YAML;
  }
};
