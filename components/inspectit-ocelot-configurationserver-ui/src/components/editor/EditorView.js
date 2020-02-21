import React from 'react'
import PropTypes from 'prop-types';
import EditorToolbar from './EditorToolbar';

import dynamic from 'next/dynamic'
const AceEditor = dynamic(() => import('./AceEditor'), { ssr: false });
const TreeTableEditor = dynamic(() => import('./TreeTableEditor'), { ssr: false });

import editorConfig from '../../data/yaml-editor-config.json'
import Notificationbar from './Notificationbar';

/**
 * Editor view consisting of the AceEditor and a toolbar.
 * 
 */
class EditorView extends React.Component {

    handleSave = () => {
        this.props.onSave(this.editor.getValue());
    }

    render() {
        const { value, schema, showEditor, hint, onRefresh, onChange, onCreate, isRefreshing, enableButtons, isErrorNotification, notificationIcon, notificationText, canSave, loading, children, readOnly, propsSplit, onPropsSplit } = this.props;

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
                .editor-container {
                    position: relative;
                }
                .loading-overlay {
                    position: absolute;
                    left: 0;
                    top: 0;
                    right: 0;
                    bottom: 0;
                    background-color: #00000080;
                    color: white;
                    z-index: 100;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                }
                `}</style>
                <div className="p-col-fixed">
                    <EditorToolbar
                        enableButtons={enableButtons}
                        canSave={canSave}
                        onRefresh={onRefresh}
                        isRefreshing={isRefreshing}
                        onSave={this.handleSave}
                        onSearch={() => this.editor.executeCommand("find")}
                        onHelp={() => this.editor.showShortcuts()}
                        propsSplit={propsSplit}
                        onPropsSplit={onPropsSplit}
                    >
                        {children}
                    </EditorToolbar>
                </div>
                <div className="p-col editor-container">
                    {showEditor &&
                        <AceEditor editorRef={(editor) => this.editor = editor} onCreate={onCreate} mode="yaml" theme="cobalt" options={editorConfig} value={value} onChange={onChange} canSave={canSave} onSave={this.handleSave} readOnly={readOnly} />
                    }
                    {!showEditor &&
                        <div className="selection-information">
                            <div>{hint}</div>
                        </div>
                    }
                    {loading &&
                        <div className="loading-overlay">
                            <i className="pi pi-spin pi-spinner" style={{ 'fontSize': '2em' }}></i>
                        </div>
                    }
                </div>
                {
                    showEditor && propsSplit && 
                    <div className="p-col">
                        <TreeTableEditor value={value} schema={schema} loading={loading}/>
                    </div>
                }
                <div className="p-col-fixed">
                    {notificationText ? <Notificationbar text={notificationText} isError={isErrorNotification} icon={notificationIcon} /> : null}
                </div>
            </div>
        );
    }
}

EditorView.propTypes = {
    /** The value of the editor */
    value: PropTypes.string,
    /** The configuration schema */
    schema: PropTypes.object,
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
    /** Whether the toolbar buttons should be enabled or disabled. */
    enableButtons: PropTypes.bool,
    /** The children will be shown in the toolbar. Can be used e.g. to show additional information. */
    children: PropTypes.element,
    /** Whether the save button is enabled or not. The save button is enabled only if the `enableButtons` is true.  */
    canSave: PropTypes.bool,
    /** Whether the notification bar is showing an error or not. */
    isErrorNotification: PropTypes.bool,
    /** The icon class to show in the notification bar. */
    notificationIcon: PropTypes.string,
    /** The text to show in the notification bar. */
    notificationText: PropTypes.string,
    /** Whether the editor should show an loading indicator */
    loading: PropTypes.bool,
    /** Wheter the editor should be in read-only mode */
    readOnly: PropTypes.bool,
    /** Weather a split view is active showing config properties in a tree */
    propsSplit: PropTypes.bool,
    /** Function to react on the change of the properties split view */
    onPropsSplit: PropTypes.func,
}

EditorView.defaultProps = {
    showEditor: true,
    enableButtons: true,
    canSave: true,
    loading: false,
    propsSplit: true,
};

export default EditorView;


