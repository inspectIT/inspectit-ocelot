import React from 'react';
import { connect } from 'react-redux'
import { mappingsActions } from '../../../redux/ducks/mappings';
import { notificationActions } from '../../../redux/ducks/notification';
import yaml from 'js-yaml';

import EditorView from "../../editor/EditorView";

/**
 * The view for managing the agent mappings.
 */
class AgentMappingsView extends React.Component {

    static getDerivedStateFromProps(props, state) {
        const hasUpdated = props.updateDate != state.lastUpdateDate;

        if (hasUpdated) {
            const yamlMappings = props.mappings ? yaml.safeDump(props.mappings) : "";

            return {
                lastUpdateDate: props.updateDate,
                editorValue: yamlMappings
            }
        }

        return null;
    }

    state = {
        lastUpdateDate: null,
        editorValue: ""
    }

    componentDidMount() {
        this.onRefresh();
    }

    onSave = (content) => {
        try {
            const mappings = yaml.safeLoad(content);

            // to prevent editor resets
            this.setState({
                editorContent: content
            });

            this.props.putMappings(mappings);
        } catch (error) {
            if (error.name && error.name === "YAMLException") {
                const { message } = error;
                this.props.showWarning("YAML Syntax Error", "Error: " + message);
            }
        }
    }

    onRefresh = () => {
        this.props.fetchMappings();
    }

    onChange = (value) => {
        this.setState({
            editorValue: value
        });
    }

    render = () => {
        const { loading } = this.props;
        const { editorValue } = this.state;
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
                `}</style>
                <div className="this">
                    <EditorView value={editorValue} onSave={this.onSave} onRefresh={this.onRefresh} enableButtons={!loading} onChange={this.onChange}>
                        <div className="header">
                            <i className="pi pi-sitemap"></i>
                            <div>Agent Mappings</div>
                        </div>
                    </EditorView>
                </div>
            </>
        );
    }
};

function mapStateToProps(state) {
    const { loading, mappings, updateDate } = state.mappings;
    return {
        loading,
        mappings,
        updateDate
    }
}

const mapDispatchToProps = {
    fetchMappings: mappingsActions.fetchMappings,
    putMappings: mappingsActions.putMappings,
    showWarning: notificationActions.showWarningMessage
}

export default connect(mapStateToProps, mapDispatchToProps)(AgentMappingsView);