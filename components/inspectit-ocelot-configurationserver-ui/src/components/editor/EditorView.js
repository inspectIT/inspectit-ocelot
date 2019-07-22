import React from 'react'
import { Tree } from 'primereact/tree';
import { connect } from 'react-redux'
import { configurationActions, configurationSelectors } from '../../redux/ducks/configuration'
import { notificationActions } from '../../redux/ducks/notification'

import dynamic from 'next/dynamic'
import { Toolbar } from 'primereact/toolbar';
import { Button } from 'primereact/button';
import { SplitButton } from 'primereact/splitbutton';
import { ScrollPanel } from 'primereact/scrollpanel';

import yamlEditorConfig from '../../data/yaml-editor-config.json'


const AceEditor = dynamic(() => import('./AceEditor'), {
    ssr: false
});

class EditorView extends React.Component {

    items = [
        {
            label: 'Save as..',
            icon: 'pi pi-save',
            command: (e) => {
                console.log("save as");
            }
        }
    ];

    initEditor = (editor) => {
        this.editor = editor;
    }

    save = () => {
        this.props.showInfo("File saved", null);
    }

    render() {
        const { selection, isDirectory } = this.props;

        let path = "";
        let name = "";

        if (selection) {
            const lastIndex = selection.lastIndexOf("/") + 1;
            path = selection.slice(0, lastIndex);
            name = selection.slice(lastIndex);
        }

        return (
            <div className="this p-grid p-dir-col p-nogutter">
                <style jsx>{`
                .this {
                    flex: 1;
                }
                .this :global(.p-toolbar) {
                    background: 0;
                    border: 0;
                    border-radius: 0;
                    background-color: #eee;
                    border-bottom: 1px solid #ddd;
                }
                .this :global(.p-toolbar-group-right) > :global(*) {
                    margin-left: .25rem;
                }
                .selection-information {
                    display: flex;
                    height: 100%;
                    align-items: center;
                    justify-content: center;
                    color: #bbb;
                }
                .this :global(.p-toolbar-group-left) {
                    font-size: 1rem;
                    display: flex;
                    align-items: center;
                    height: 2rem;
                }
                .this :global(.p-toolbar-group-left) :global(.pi) {
                    font-size: 1.75rem;
                    color: #aaa;
                    margin-right: 1rem;
                }
                .path {
                    color: #999;
                }
                `}</style>
                <div className="p-col-fixed">
                    <Toolbar>
                        <div className="p-toolbar-group-left">
                            {selection &&
                                <>
                                    <i className={"pi pi-" + (isDirectory ? "inbox" : "file")}></i>
                                    <div className="path">{path}</div>
                                    <div className="name">{name}</div>
                                </>
                            }
                        </div>
                        <div className="p-toolbar-group-right">
                            <Button disabled={!selection || isDirectory} icon="pi pi-question" onClick={() => this.editor.showKeyboardShortcuts()} />
                            <Button disabled={!selection || isDirectory} icon="pi pi-search" onClick={() => this.editor.execCommand("find")} />
                            <SplitButton disabled={!selection || isDirectory} onClick={this.save} label="Save" icon="pi pi-save" model={this.items} />
                        </div>
                    </Toolbar>
                </div>
                <div className="p-col">
                    {!selection || isDirectory ?
                        <div className="selection-information">
                            <div>Select a file to start editing.</div>
                        </div>
                        :
                        <AceEditor mode="yaml" theme="cobalt" initEditor={this.initEditor} options={yamlEditorConfig} value={this.props.selection} />
                    }
                </div>
            </div>
        );
    }
}

function mapStateToProps(state) {
    const { selection } = state.configuration;
    return {
        selection,
        isDirectory: configurationSelectors.isSelectionDirectory(state)
    }
}

const mapDispatchToProps = {
    showInfo: notificationActions.showInfoMessage
}

export default connect(mapStateToProps, mapDispatchToProps)(EditorView);


