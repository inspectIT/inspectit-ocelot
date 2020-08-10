import { fetch } from './common';

/**
 * Fetches all existing alerting templates.
 */
export const fetchTemplates = () => {
  return fetch('/alert/kapacitor/templates');
};

/**
 * Fetches the content of the given template.
 */
export const fetchTemplate = (templateName) => {
  return fetch(`/alert/kapacitor/templates/${templateName}`);
};
