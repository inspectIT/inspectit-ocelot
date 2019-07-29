import React from 'react'
import PropTypes from 'prop-types';
import EditorToolbar from './EditorToolbar';

import dynamic from 'next/dynamic'
const AceEditor = dynamic(() => import('./AceEditor'), { ssr: false });

import editorConfig from '../../data/yaml-editor-config.json'
import Notificationbar from './Notificationbar';

/**
 * Editor view consisting of the AceEditor and a toolbar.
 * 
 */
class EditorView extends React.Component {

    render() {
        const { value, showEditor, hint, onSave, onRefresh, onChange, isRefreshing, enableButtons, isErrorNotification, notificationIcon, notificationText, canSave, children } = this.props;

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
                        enableButtons={enableButtons}
                        canSave={canSave}
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
                        <AceEditor editorRef={(editor) => this.editor = editor} mode="yaml" theme="cobalt" options={editorConfig} value={value} onChange={onChange} />
                        :
                        <div className="selection-information">
                            <div>{hint}</div>
                        </div>
                    }
                </div>
                <div className="p-col-fixed">
                    {notificationText ? <Notificationbar text={notificationText} isError={isErrorNotification} icon={notificationIcon} /> : null}
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
}

EditorView.defaultProps = {
    showEditor: true,
    enableButtons: true,
    canSave: true
};

export default EditorView;


