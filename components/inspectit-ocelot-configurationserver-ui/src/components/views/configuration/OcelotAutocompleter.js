import { getYamlPath } from '../../../lib/yaml-utils';

import axios from '../../../lib/axios-api';

/**
 * Implementation of an ACE-autocompleter.
 * This autocompleter uses a heuristic to derive the current JSON-Path in the current YAML file.
 * This path is then used to get autocompletion suggestions from the configuration server.
 */
const completer = {
  getCompletions: function (editor, session, pos, prefix, callback) {
    if (editor.configAutoCompleteEnabled) {
      const lines = session.getDocument().getLines(0, pos.row).slice();
      lines[lines.length - 1] = lines[lines.length - 1].substring(0, pos.column);
      const path = getYamlPath(lines);
      if (path && path[path.length - 1] === prefix) {
        if (path.length === 1 && 'inspectit'.startsWith(path[0])) {
          callback(null, [{ value: 'inspectit', meta: 'Ocelot', score: 1000 }]);
        } else {
          const pathString = concatPath(path);
          fetchAutocompletions(prefix, pathString, callback);
        }
      }
    }
  },
};

/**
 * Takes an array of path segments and concats them in an array-access based form.
 * E.g. [first,second,third] wil lresult in "first[second][third]".
 *
 * @param {*} segments the path segments
 */
const concatPath = (segments) => {
  if (segments.length === 1) {
    return segments[0];
  } else if (segments.length >= 1) {
    const pathWithoutFirstSegment = segments.slice(1, segments.length - 1);
    return segments[0] + pathWithoutFirstSegment.reduce((accumulator, segment) => accumulator + '[' + segment + ']', '');
  }
};

/**
 * Issues a request to the config server for providing autocompletion suggestions.
 *
 * @param {*} prefix the word which is being autocompleted, e.g. "file-ba"
 * @param {*} configPath the path under which the autocompletion is taking place, e.g. "inspectit.config"
 * @param {*} callback the autocompletion callback to report suggestions
 */
const fetchAutocompletions = (prefix, configPath, callback) => {
  axios
    .post('/autocomplete', {
      path: configPath,
    })
    .then((response) => {
      const suggestions = response.data;
      if (suggestions) {
        const result = suggestions
          .filter((suggestion) => suggestion.startsWith(prefix))
          .map((suggestion) => {
            return {
              value: suggestion,
              meta: 'Ocelot',
              score: 1000,
            };
          });
        callback(null, result);
      }
    })
    .catch((error) => {
      console.warn('Could not fetch autocompletion results.', error);
    });
};

const initialized = false;

/**
 * Enables the Ocelot autocompletion on the given editor.
 *
 * @param {*} aceEditor editor to enable the ocelot autocompleter
 */
export const enableOcelotAutocompletion = (aceEditor) => {
  if (!initialized) {
    const langTools = ace.require('ace/ext/language_tools'); // eslint-disable-line no-undef
    langTools.addCompleter(completer);
  }
  aceEditor.configAutoCompleteEnabled = true;
};
