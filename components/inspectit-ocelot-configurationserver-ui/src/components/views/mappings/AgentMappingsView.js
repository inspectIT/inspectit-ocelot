import React from 'react';
import { connect } from 'react-redux'
import { mappingsActions, mappingsSelectors } from '../../../redux/ducks/mappings';
import { notificationActions } from '../../../redux/ducks/notification';
import yaml from 'js-yaml';

import EditorView from "../../editor/EditorView";

/**
 * The view for managing the agent mappings.
 */
class AgentMappingsView extends React.Component {

    componentDidMount() {
        this.props.fetchMappings();
    }

    onSave = (content) => {
        try {
            const mappings = yaml.safeLoad(content);
            this.props.putMappings(mappings, true);
        } catch (error) {
            if (error.name && error.name === "YAMLException") {
                const { message } = error;
                this.props.showWarning("YAML Syntax Error", "Error: " + message);
            }
        }
    }

    onRefresh = () => {
        this.props.fetchMappings();
        this.props.editorContentChanged(null);
    }

    render = () => {
        const { loading, content, yamlError, hasUnsavedChanges } = this.props;
        return (
            <>
                <style jsx>{`
                .this {
                    height: 100%;
                    display: flex;
                }
                .header {
                    font-size: 1rem;
                    display: flex;
                    align-items: center;
                    height: 2rem;
                }
                .header :global(.pi) {
                    font-size: 1.75rem;
                    color: #aaa;
                    margin-right: 1rem;
                }
                .dirtyStateMarker {
                    margin-left: .25rem;
                    color: #999;
                }
                `}</style>
                <div className="this">
                    <EditorView
                        value={content}
                        onSave={this.onSave}
                        onRefresh={this.onRefresh}
                        enableButtons={!loading}
                        onChange={this.props.editorContentChanged}
                        isErrorNotification={true}
                        canSave={!yamlError && hasUnsavedChanges}
                        notificationIcon="pi-exclamation-triangle"
                        notificationText={yamlError}>
                        <div className="header">
                            <i className="pi pi-sitemap"></i>
                            <div>Agent Mappings</div>
                            {hasUnsavedChanges && <div className="dirtyStateMarker">*</div>}
                        </div>
                    </EditorView>
                </div>
            </>
        );
    }
};

const getYamlError = (content) => {
    try {
        yaml.safeLoad(content);
        return null;
    } catch (error) {
        if (error.message) {
            return "YAML Syntax Error: " + error.message;
        } else {
            return "YAML cannot be parsed.";
        }
    }
}

function mapStateToProps(state) {
    const { pendingRequests, updateDate, editorContent } = state.mappings;
    let content = editorContent == null ? mappingsSelectors.getMappingsAsYaml(state) : editorContent
    return {
        loading: pendingRequests > 0,
        content,
        hasUnsavedChanges: editorContent != null,
        yamlError: getYamlError(content),
        updateDate
    }
}

const mapDispatchToProps = {
    fetchMappings: mappingsActions.fetchMappings,
    putMappings: mappingsActions.putMappings,
    showWarning: notificationActions.showWarningMessage,
    editorContentChanged: mappingsActions.editorContentChanged
}

export default connect(mapStateToProps, mapDispatchToProps)(AgentMappingsView);