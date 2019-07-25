import React from 'react';
import { connect } from 'react-redux'
import { mappingsActions } from '../../../redux/ducks/mappings';
import yaml from 'js-yaml';
import { isEqual } from 'lodash';

import EditorView from "../../editor/EditorView";

/**
 * The view for managing the agent mappings.
 */
class AgentMappingsView extends React.Component {

    static getDerivedStateFromProps(props, state) {
        if (!isEqual(props.mappings, state.currentMappings)) {
            const yamlMappings = props.mappings ? yaml.safeDump(props.mappings) : "";

            return {
                currentMappings: props.mappings,
                editorContent: yamlMappings
            }
        }
        return null;
    }

    state = {
        currentMappings: null,
        editorContent: ""
    }

    componentDidMount() {
        this.onRefresh();
    }

    onSave = (content) => {
        // to prevent editor resets
        this.setState({
            editorContent: content
        });

        const mappings = yaml.safeLoad(content);
        this.props.putMappings(mappings);
    }

    onRefresh = () => {
        this.props.fetchMappings();
    }

    render = () => {
        const { loading } = this.props;
        const { editorContent } = this.state;

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
                    <EditorView content={editorContent} onSave={this.onSave} onRefresh={this.onRefresh} enableButtons={!loading}>
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
    const { loading, mappings } = state.mappings;
    return {
        loading,
        mappings
    }
}

const mapDispatchToProps = {
    fetchMappings: mappingsActions.fetchMappings,
    putMappings: mappingsActions.putMappings
}

export default connect(mapStateToProps, mapDispatchToProps)(AgentMappingsView);