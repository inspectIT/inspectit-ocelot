import React from 'react'
import { Tree } from 'primereact/tree';
import { ContextMenu } from 'primereact/contextmenu';
import { connect } from 'react-redux'
import { configurationActions, configurationSelectors, configurationUtils } from '../../../redux/ducks/configuration'
import { linkPrefix } from '../../../lib/configuration';

import { DEFAULT_CONFIG_TREE_KEY } from '../../../data/constants';

const menu = [
    {
        label: 'Add Folder',
        icon: 'pi pi-folder',
        command: () => {
            console.log('add folder');
        }
    },
    {
        label: 'Add File',
        icon: 'pi pi-file',
        command: () => {
            console.log('add file')
        }
    },
    {
        label: 'Rename',
        icon: 'pi pi-pencil',
        command: () => {
            console.log('rename')
        }
    },
    {
        label: 'Delete',
        icon: 'pi pi-trash',
        command: () => {
            console.log('delete')
        }
    }
]

/**
 * The file tree used in the configuration view.
 */
class FileTree extends React.Component {

    /**
     * Fetch the files initially.
     */
    componentDidMount = () => {
        const { loading, defaultConfig } = this.props;
        if (!loading) {
            this.props.fetchFiles();
        }

        if (Object.entries(defaultConfig).length === 0) {
            this.props.fetchDefaultConfig();
        }
    }

    /**
     * Handle tree selection changes.
     */
    onSelectionChange = (event) => {
        const { selection, selectedDefaultConfigFile, rawFiles } = this.props;
        const newSelection = event.value;
        if (newSelection) {
            if (newSelection !== selection && newSelection !== selectedDefaultConfigFile) {
                this.props.selectFile(newSelection);
            }
        } else {
            if (selection || selectedDefaultConfigFile) {
                this.props.selectFile(null);
            }
        }
    }

    /**
     * might be better to introduce a new prop for contextMenuSelection which will be changed as an action along with the state (with state in case it is too slow otherwise)
     * that way the file won't be fetched and loaded and there is no need to select
     * dialogs need to favour the contextmenuselection over selection in this case
     */
    // onContextMenuSelectionChange = (event) => {
    //     const { selection } = this.props;
    //     const newSelection = event.value;
    //     if (!selectedNodeKey.startsWith(DEFAULT_CONFIG_TREE_KEY) && selection !== event.value) {
    //         this.props.selectFile(event.value);
    //     }
    // }

    onContextMenu = (event) => {
        console.log(event)
        // event contains the node! I don't need onContextMenuSelectionChange where the event will give me nothing but the key
        const { selection, loading } = this.props;
        if (selection && !loading) {
            this.cm.show(event.originalEvent)
        }
    }

    render() {
        return (
            <div className='this'>
                <style jsx>{`
                    .this {
                        overflow: auto;
                        flex-grow: 1;
                    }
                    .this :global(.cm-tree-icon) {
                        width: 1.3rem;
                        height: 1.3rem;
                    }
                    .this :global(.cm-tree-label) {
                        color: #aaa;
                    }
                    .this :global(.ocelot-tree-head-orange) {
                        background: url("${linkPrefix}/static/images/inspectit-ocelot-head_orange.svg") center no-repeat;
                        background-size: 1rem 1rem;
                    }
                    .this :global(.ocelot-tree-head-white) {
                        background: url("${linkPrefix}/static/images/inspectit-ocelot-head_white.svg") center no-repeat;
                        background-size: 1rem 1rem;
                    }
				`}</style>
                <ContextMenu model={menu} ref={el => this.cm = el} />
                <Tree
                    className={this.props.className}
                    filter={true}
                    filterBy="label"
                    value={this.props.defaultTree.concat(this.props.files)}
                    selectionMode="single"
                    selectionKeys={this.props.selection || this.props.selectedDefaultConfigFile}
                    onSelectionChange={this.onSelectionChange}
                    onContextMenuSelectionChange={this.onSelectionChange}
                    onContextMenu={this.onContextMenu} />
                />
            </div>

        );
    }
}

function mapStateToProps(state) {
    const { pendingRequests, selection, files, defaultConfig, selectedDefaultConfigFile } = state.configuration;
    return {
        files: configurationSelectors.getFileTree(state),
        loading: pendingRequests > 0,
        selection,
        defaultConfig: defaultConfig,
        defaultTree: configurationSelectors.getDefaultConfigTree(state),
        selectedDefaultConfigFile
    }
}

const mapDispatchToProps = {
    fetchDefaultConfig: configurationActions.fetchDefaultConfig,
    fetchFiles: configurationActions.fetchFiles,
    selectFile: configurationActions.selectFile,
}

export default connect(mapStateToProps, mapDispatchToProps)(FileTree);