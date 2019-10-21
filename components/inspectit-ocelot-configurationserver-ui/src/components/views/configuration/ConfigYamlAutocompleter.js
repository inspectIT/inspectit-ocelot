import {getYamlPath} from '../../../lib/yaml-utils'

import axios from '../../../lib/axios-api';


const completer = {

    getCompletions: function(editor, session, pos, prefix, callback) {
        if(editor.configAutoCompleteEnabled) {
            const lines = session.getDocument().getLines(0,pos.row).slice();
            lines[lines.length - 1] = lines[lines.length - 1].substring(0, pos.column);
            let path = getYamlPath(lines);
            if(path && path[path.length -1] == prefix) {
                if(path.length == 1 && "inspectit".startsWith(path[0])) {
                    callback(null, [{value: "inspectit", score: 1000}]);
                } else {
                    let pathString = path[0] + path.slice(1, path.length - 1).reduce((acc,seg) => acc + "["+seg+"]", "");
                    fetchAutocompletions(prefix, pathString, callback);
                }
            }
        }
    }
}

/**
 * Issues a request to the config server for providing autocompletion suggestions.
 * 
 * @param {*} prefix the word which is being autocompleted, e.g. "file-ba"
 * @param {*} configPath the path under which the autocompletion is taking place, e.g. "inspectit.config"
 * @param {*} callback the autocompletion callback to report suggestions
 */
const fetchAutocompletions = (prefix, configPath, callback) => {
    axios
    .post("/autocomplete" , {
        path: configPath
    })
    .then(response => {
        const suggestions = response.data;
        if(suggestions) {
            let result = suggestions
                .filter(sug => sug.startsWith(prefix))
                .map(sug => {
                    return {value : sug, score : 1000}
                });
            callback(null, result);
        }
    })
    .catch(error => {});
}

let langTools;
const init = () => {
    if(!langTools) {
        langTools = require('ace-builds/src-noconflict/ext-language_tools');
        langTools.addCompleter(completer);
    }
}

export const enableConfigAutoCompletion = (aceEditor) => {
    init();
    aceEditor.configAutoCompleteEnabled = true;
};
