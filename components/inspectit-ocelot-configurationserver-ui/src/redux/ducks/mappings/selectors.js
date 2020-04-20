import { createSelector } from 'reselect';
import yaml from 'js-yaml';

/**
 * Returns the fetched mappigns as YAML.
 */
export const getMappingsAsYaml = createSelector(
  (state) => state.mappings.mappings,
  (mappings) => (mappings ? yaml.safeDump(mappings) : '')
);

/**
 * Returns the fetched mappigns as YAML given the mappings config section.
 */
export const getMappingsAsYamlFromMappingsState = createSelector(
  (state) => state.mappings,
  (mappings) => (mappings ? yaml.safeDump(mappings) : '')
);
