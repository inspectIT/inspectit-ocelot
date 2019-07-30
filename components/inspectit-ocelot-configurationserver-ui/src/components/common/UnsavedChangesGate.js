import React from 'react'
import { connect } from 'react-redux'
import { configurationSelectors } from '../../redux/ducks/configuration'

class UnsavedChangesGate extends React.Component {

    componentDidMount() {
        window.onbeforeunload = () => {
            if(this.anyUnsavedChanges()) {
                return "unsaved changes";
            }
        }
    }

    componentWillUnmount() {
        window.onbeforeunload = null;
    }

    render() {
        return (
            <>
                {this.props.children}
            </>
        );
    }

    anyUnsavedChanges = () => {
        return this.props.unsavedConfigChanges || this.props.unsavedMappingChanges;
    }
}


function mapStateToProps(state) {
    return {
        unsavedConfigChanges: configurationSelectors.anyUnsavedChanges(state),
        unsavedMappingChanges: state.mappings.editorContent != null,
    }
}

export default connect(mapStateToProps)(UnsavedChangesGate);