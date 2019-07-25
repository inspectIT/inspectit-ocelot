import React from 'react';
import { connect } from 'react-redux'
import { mappingsActions } from '../../../redux/ducks/mappings';
import yaml from 'js-yaml';
import { isEqual } from 'lodash';

import EditorView from "../../editor/EditorView";

class AgentMappingsView extends React.Component {

    static getDerivedStateFromProps(props, state) {
        if (!isEqual(props.mappings, state.currentMappings)) {
            console.log("update");
            
            return {
                currentMappings: props.mappings
            }
        }
        return null;
    }

    state = {
        currentMappings: null,
        editorContent: "asd"
    }

    componentDidMount() {
        this.props.fetchMappings();
    }

    onSave = (content) => {
        const mappings = yaml.safeLoad(content);
        this.props.putMappings(mappings);
    }

    onChange = (value) => {
        console.log(value);
        
        this.setState({
            editorContent: value
        });
    }

    render = () => {
        const { currentMappings, editorContent } = this.state;
        const yamlMappings = yaml.safeDump(currentMappings);

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
                    <EditorView content={editorContent} showEditor={true} onSave={this.onSave} onChange={this.onChange}>
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