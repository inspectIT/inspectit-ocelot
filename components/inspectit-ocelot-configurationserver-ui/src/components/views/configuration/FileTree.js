import React from 'react'
import { Tree } from 'primereact/tree';
import { connect } from 'react-redux'
import { configurationActions, configurationSelectors } from '../../../redux/ducks/configuration'

/**
 * The file tree used in the configuration view.
 */
class FileTree extends React.Component {

    /**
     * Fetch the files initially.
     */
    componentDidMount = () => {
        const { loading, files } = this.props;
        if (!loading && (files && files.length <= 0)) {
            this.props.fetchFiles();
        }
    }

    /**
     * Handle tree selection changes.
     */
    onSelectionChange = (event) => {
        this.props.selectFile(event.value);
    }

    render() {
        return (
            <Tree
                className={this.props.className}
                filter={true}
                filterBy="label"
                value={this.props.files}
                selectionMode="single"
                onSelectionChange={e => this.onSelectionChange(e)} />
        );
    }
}

function mapStateToProps(state) {
    const { loading } = state.configuration;
    return {
        files: configurationSelectors.getFileTree(state),
        loading
    }
}

const mapDispatchToProps = {
    fetchFiles: configurationActions.fetchFiles,
    selectFile: configurationActions.selectFile
}

export default connect(mapStateToProps, mapDispatchToProps)(FileTree);