import { CONFIGURATION_TYPES } from '../data/constants';
import _ from 'lodash';

/**
 * Returns the type of the specified configuration. In case the configuration is null or empty
 * YAML will be returned.
 *
 * Example: the following configuration will result in type "METHOD_CONFIGURATION"
 * > # {"type": "METHOD_CONFIGURATION"}
 * > inspectit:
 * >   config:
 * >     ...
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
    return _.find(CONFIGURATION_TYPES, { name: type }, CONFIGURATION_TYPES.YAML);
  } catch {
    // YAML type in case the first comment is no JSON object
    return CONFIGURATION_TYPES.YAML;
  }
};
