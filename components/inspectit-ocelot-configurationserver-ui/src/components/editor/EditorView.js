import React from 'react'
import PropTypes from 'prop-types';
import EditorToolbar from './EditorToolbar';

import dynamic from 'next/dynamic'
const AceEditor = dynamic(() => import('./AceEditor'), { ssr: false });

import editorConfig from '../../data/yaml-editor-config.json'

/**
 * Editor view consisting of the AceEditor and a toolbar.
 * 
 */
class EditorView extends React.Component {

    render() {
        const { content, showEditor, hint, onSave, onRefresh, isRefreshing, children } = this.props;

        return (
            <div className="this p-grid p-dir-col p-nogutter">
                <style jsx>{`
                .this {
                    flex: 1;
                }
                .selection-information {
                    display: flex;
                    height: 100%;
                    align-items: center;
                    justify-content: center;
                    color: #bbb;
                }
                `}</style>
                <div className="p-col-fixed">
                    <EditorToolbar
                        enableButtons={showEditor}
                        onRefresh={onRefresh}
                        isRefreshing={isRefreshing}
                        onSave={() => onSave(this.editor.getValue())}
                        onSearch={() => this.editor.executeCommand("find")}
                        onHelp={() => this.editor.showShortcuts()}>
                        {children}
                    </EditorToolbar>
                </div>
                <div className="p-col">
                    {showEditor ?
                        <AceEditor editorRef={(editor) => this.editor = editor} mode="yaml" theme="cobalt" options={editorConfig} value={content} />
                        :
                        <div className="selection-information">
                            <div>{hint}</div>
                        </div>
                    }
                </div>
            </div>
        );
    }
}

EditorView.propTypes = {
    /** The content of the editor */
    content: PropTypes.string,
    /** Whether the editor should be shown or hidden. */
    showEditor: PropTypes.bool,
    /** The hint which will be shown if the editor is hidden. */
    hint: PropTypes.string,
    /** Callback which is triggered when the save button is pressed. */
    onSave: PropTypes.func,
    /** Callback which is executed when the refresh button is pressed. The refresh button is only shown if this callback is specified. */
    onRefresh: PropTypes.func,
    /** If true, the refresh button is disabled and showing a spinner. */
    isRefreshing: PropTypes.bool,
    /** The children will be shown in the toolbar. Can be used e.g. to show additional information. */
    children: PropTypes.element
}

EditorView.defaultProps = {
    showEditor: true
};

export default EditorView;


