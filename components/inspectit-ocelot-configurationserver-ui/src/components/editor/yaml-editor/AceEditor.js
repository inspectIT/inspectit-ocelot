import React from 'react';
import ace from 'ace-builds/src-noconflict/ace';

import 'ace-builds/webpack-resolver';
import 'ace-builds/src-noconflict/ext-language_tools';
import 'ace-builds/src-noconflict/ext-searchbox';
import 'ace-builds/src-noconflict/ext-keybinding_menu';

//include supported themes and modes here
import 'ace-builds/src-noconflict/mode-yaml';
import 'ace-builds/src-noconflict/theme-cobalt';
import InspectitOcelotMode from './InspectitOcelotMode';
import axios from '../../../lib/axios-api';

let rulesToGenerate;

const saveCommand = (doSave) => {
  return {
    name: 'saveFile',
    bindKey: {
      win: 'Ctrl-S',
      mac: 'Command-S',
    },
    exec: doSave,
  };
};

/**
 * Component which wraps the AceEditor.
 */
class AceEditor extends React.Component {
  constructor(props) {
    super(props);

    this.props.editorRef(this);
    this.divRef = React.createRef();
  }

  render() {
    return <div ref={this.divRef} style={{ width: '100%', height: '100%' }} />;
  }

  configureEditor() {
    const { theme, options, readOnly } = this.props;
    this.editor.setTheme('ace/theme/' + theme);

    this.editor.session.off('change', this.onChange);
    this.editor.session.on('change', this.onChange);
    this.editor.commands.addCommand(saveCommand(this.doSave));
    this.editor.setReadOnly(readOnly);
    // the following deactivates the cursor and line selection in read only mode.
    // See: https://stackoverflow.com/questions/32806060/is-there-a-programmatic-way-to-hide-the-cursor-in-ace-editor
    this.editor.setHighlightGutterLine(!readOnly);
    this.editor.setHighlightActiveLine(!readOnly);
    this.editor.renderer.$cursorLayer.element.style.display = readOnly ? 'none' : '';

    if (options) {
      this.editor.setOptions(options);
    }
  }

  componentDidMount() {
    this.editor = ace.edit(this.divRef.current);
    const editorRef = this.editor;

    ace.config.loadModule('ace/ext/keybinding_menu', function (module) {
      module.init(editorRef);
    });
    if (this.props.onCreate) {
      this.props.onCreate(this.editor);
    }

    this.configureEditor();

    // rulesToGenerate is a map whose contents determine what the highlighting rules need to be. The contents are
    // retrieved over the config-server's REST API. Until the results are returned the standard YAML highlighting mode
    // is used. When the results have been returned, the mode is set to InspectitOcelotMode.
    this.editor.getSession().setMode('ace/mode/yaml');
    axios
      .get('highlight-rules')
      .then((response) => {
        rulesToGenerate = response.data;
        this.editor.getSession().setMode(new InspectitOcelotMode());
      })
      .catch((error) => {
        console.log('Encountered error when retrieving Map to create custom Highlighting mode:');
        console.log(error);
      });

    this.updateValue();
  }

  componentDidUpdate() {
    this.configureEditor();
    this.updateValue();
  }

  onChange = () => {
    const value = this.getValue();
    const sanitizedValue = this.sanitizeValue(value);

    if (value !== sanitizedValue) {
      this.editor.setValue(sanitizedValue);
    } else if (this.props.onChange) {
      this.props.onChange(this.getValue());
    }
  };

  doSave = () => {
    if (this.props.canSave) {
      this.props.onSave();
    }
  };

  /**
   * Updates the editor content using the `value` props and sets the cursor to the beginning of the content.
   */
  updateValue = () => {
    const { value } = this.props;
    const currentValue = this.getValue();
    if (value !== currentValue) {
      this.editor.setValue(value ? value : '', -1);
    }
  };

  executeCommand = (command) => {
    this.editor.execCommand(command);
  };

  showShortcuts = () => {
    this.editor.showKeyboardShortcuts();
  };

  getValue = () => {
    return this.editor.getSession().getValue();
  };

  sanitizeValue = (value) => {
    return value.replace(/\t/g, '  ');
  };
}

// needed for getting the contents of rulesToGenerate to InspectitOcelotHighlightingRules
export { rulesToGenerate };

export default AceEditor;
