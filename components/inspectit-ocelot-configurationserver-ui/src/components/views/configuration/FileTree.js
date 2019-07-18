import React from 'react'
import { Tree } from 'primereact/tree';
import { connect } from 'react-redux'
import { configurationActions, configurationSelectors } from '../../../redux/ducks/configuration'

class FileTree extends React.Component {

    state = {
        selectedNodeKey: ""
    }

    componentDidMount = () => {
        this.props.fetchFiles();
    }

    onSelectionChange = (event) => {
        console.log(event, event.value);
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
                selectionKeys={this.state.selectedNodeKey}
                onSelectionChange={e => this.onSelectionChange(e)} />
        );
    }
}

function mapStateToProps(state) {
    return {
        files: configurationSelectors.fileTree(state)
    }
}

const mapDispatchToProps = {
    fetchFiles: configurationActions.fetchFiles,
    selectFile: configurationActions.selectFile
}

export default connect(mapStateToProps, mapDispatchToProps)(FileTree);