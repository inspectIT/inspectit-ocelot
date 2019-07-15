import ace from 'ace-builds/src-noconflict/ace';
import 'ace-builds/webpack-resolver';
import React from 'react';

import 'ace-builds/src-noconflict/ext-language_tools';
import 'ace-builds/src-noconflict/ext-searchbox';
import 'ace-builds/src-noconflict/ext-keybinding_menu';
//include supported themes and modes here
import 'ace-builds/src-noconflict/mode-yaml';
import 'ace-builds/src-noconflict/theme-cobalt';


class AceEditor extends React.Component {

    constructor(props) {
        super(props);
        this.divRef = React.createRef();
    }

    render() {
        let style= {
            width: "100%",
            height: "100%"
        }
        return (
            <div ref={this.divRef} style={style}/>
        )
    }
    configureEditor() {
        this.editor.setTheme("ace/theme/" + this.props.theme)
        this.editor.getSession().setMode("ace/mode/" + this.props.mode);
        if(!!this.props.options) {
            this.editor.setOptions(this.props.options);
        }
    }

    componentDidMount() {
        this.editor = ace.edit(this.divRef.current)
        var editorRef = this.editor;
        ace.config.loadModule("ace/ext/keybinding_menu", function(module) {
            module.init(editorRef);
        })

        this.configureEditor();
        this.props.initEditor(this.editor);
    }
    
    componentDidUpdate() {
        this.configureEditor();
    }

}

export default AceEditor;