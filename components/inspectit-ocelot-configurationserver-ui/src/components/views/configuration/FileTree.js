import React from 'react'
import { Tree } from 'primereact/tree';
import { connect } from 'react-redux'
import { configurationActions, configurationSelectors, configurationUtils } from '../../../redux/ducks/configuration'

/**
 * The file tree used in the configuration view.
 */
class FileTree extends React.Component {

    /**
     * Fetch the files initially.
     */
    componentDidMount = () => {
        const { loading } = this.props;
        if (!loading) {
            this.props.fetchFiles();
        }
    }

    /**
     * Handle tree selection changes.
     */
    onSelectionChange = (event) => {
        const { selection, rawFiles } = this.props;

        if (event.value !== selection) {
            this.props.selectFile(event.value);
        }
    }

    render() {
        return (
            <Tree
                className={this.props.className}
                filter={true}
                filterBy="label"
                value={this.props.files}
                selectionMode="single"
                selectionKeys={this.props.selection}
                onSelectionChange={this.onSelectionChange}
            />
        );
    }
}

function mapStateToProps(state) {
    const { pendingRequests, selection, files } = state.configuration;
    return {
        files: configurationSelectors.getFileTree(state),
        loading: pendingRequests > 0,
        selection
    }
}

const mapDispatchToProps = {
    fetchFiles: configurationActions.fetchFiles,
    selectFile: configurationActions.selectFile
}

export default connect(mapStateToProps, mapDispatchToProps)(FileTree);