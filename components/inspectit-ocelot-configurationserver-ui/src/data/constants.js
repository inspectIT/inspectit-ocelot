/** Templates */
import methodConfigurationTemplate from './templates/method-configuration.json';

/**
 * Various constants used by the application.
 */

/**
 * Default <title> content
 */
export const BASE_PAGE_TITLE = 'inspectIT Ocelot Configuration Server';

/**
 * Interval in which will be checked, whether the token will be expiring.
 */
export const RENEW_TOKEN_TIME_INTERVAL = 60000;

/**
 * Minimum expiration time
 * If the expiration time of the current token is shorter than the MIN_TOKEN_EXPIRATION_IIME, the token will be renewed.
 */
export const MIN_TOKEN_EXPIRATION_TIME = 300000;

/**
 * Default configuration tree key.
 * Used by the configuration selector to create a unique tree node key
 * and by the configuration action to decide which type of tree node (normal vs. default) is selected.
 */
export const DEFAULT_CONFIG_TREE_KEY = '/$%$%$%$%Ocelot-default-key';

/**
 * The maximum amount of version elements fetched from the config server.
 */
export const VERSION_LIMIT = 100;

/**
 * The supported configuration types.
 */
export const CONFIGURATION_TYPES = {
  YAML: {
    name: '',
    template: '',
  },
  METHOD_CONFIGURATION: {
    name: 'Method-Configuration',
    template: methodConfigurationTemplate,
  },
};

/**
 * Shared options for all tooltips.
 */
export const TOOLTIP_OPTIONS = {
  showDelay: 1000,
  position: 'top',
};

/**
 * Name pattern for hidden files.
 * @type {string}
 */
export const HIDDEN_FILES_NAME_PATTERN = '^\\.';
