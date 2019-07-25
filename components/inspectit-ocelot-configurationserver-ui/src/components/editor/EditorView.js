import React from 'react'
import EditorToolbar from './EditorToolbar';

import dynamic from 'next/dynamic'
const AceEditor = dynamic(() => import('./AceEditor'), { ssr: false });

import editorConfig from '../../data/yaml-editor-config.json'

/**
 * Editor view consisting of the AceEditor and a toolbar.
 */
class EditorView extends React.Component {

    render() {
        const { content, showEditor, hint, onSave, children } = this.props;

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


export default EditorView;


