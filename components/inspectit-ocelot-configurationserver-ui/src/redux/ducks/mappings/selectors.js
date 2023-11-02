import { createSelector } from 'reselect';
import yaml from 'js-yaml';

const mappingsSelector = (state) => state.mappings;

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

/**
 * Returns whether the currently selected version is the latest one. The front-end assumes, that
 * the latest version is on index 0 in the versions array provided by the backend.
 */
export const isLatestVersion = createSelector(mappingsSelector, (configuration) => {
  const { versions, selectedVersion } = configuration;

  return _isLatestVersion(versions, selectedVersion);
});

/**
 * The logic to determine whether the given version is the latest one. The front-end assumes, that
 * the latest version is on index 0 in the versions array provided by the backend.
 */
const _isLatestVersion = (versions, selectedVersion) => {
  return selectedVersion === null || versions.length === 0 || selectedVersion === versions[0].id;
};
