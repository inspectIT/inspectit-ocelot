import React from 'react'
import { connect } from 'react-redux'
import { uniqueId } from 'lodash';

import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';
import { Message } from 'primereact/message';
import { InputText } from 'primereact/inputtext';
import { configurationQueries, configurationActions, configurationSelectors } from '../../../../redux/ducks/configuration';


/**
 * Dialog for creating a new file or directory.
 * The file/folder is placed relative to the currently selected one.
 * If the selection is empty, the file/folder will be placed under the root.
 * If a folder is selected, the created file/folder will be placed within it.
 * If a file is selected, the created file/folder will be placed next to it.
 */
class CreateDialog extends React.Component {

    constructor(props) {
        super(props)
        this.state = {
            /**
             * In case the input name is invalid, a meaningful error message is placed here
             */
            error: null,
            filename: props.directoryMode ? "" : ".yml",
            inputId: uniqueId("create-dialog-input")
        }
    }

    render() {
        const type = this.props.directoryMode ? "Directory" : "File";

        return (
            <Dialog
                style={{width: '400px'}}
                header={"Create " + type}
                modal={true}
                visible={this.props.visible}
                onHide={this.onHide}
                footer={(
                    <div>
                        <Button label="Create" disabled={!this.canCreateFileOrFolder()}onClick={this.createFileOrFolder} />
                        <Button label="Cancel" className="p-button-secondary" onClick={this.onHide} />
                    </div>
                )}
            >
                <span className="p-float-label" style={{marginTop : "0.5em", marginBottom : "0.5em"}}>
                    <InputText 
                        id={this.state.inputId} 
                        style={{width: '100%'}}
                        onKeyPress={this.onKeyPress} 
                        value={this.state.filename} 
                        onChange={(e) => this.filenameChanged(e.target.value)} 
                    />
                    <label htmlFor={this.state.inputId}>{type + " Name"}</label>
                </span>
                {this.state.error &&
                    <Message style={{width: '100%'}} severity="error" text={ this.state.error}></Message>
                }
            </Dialog>
        )
    }

    onHide = () => {
        this.props.onHide();
        this.reset();
    }

    componentDidUpdate(prevProps) {
        if(!prevProps.visible && this.props.visible) {
            const input = document.getElementById(this.state.inputId);
            input.focus();
            input.setSelectionRange(0,0);
        }
    }

    onKeyPress = (e) => {
        if (e.key === 'Enter' && this.canCreateFileOrFolder()) {
            this.createFileOrFolder()
        }
    }

    reset() {
        if(this.props.directoryMode) {
            this.filenameChanged("");
        } else {
            this.filenameChanged(".yml");
        }
    }

    filenameChanged = (name) => {
        let error = null;
        const existingFile = configurationQueries.getFile(this.props.files, this.getBasePath() + name);
        if(existingFile) {
            if(configurationQueries.isDirectory(existingFile)) {
                error = "A directory with the given name already exists";
            } else {
                error = "A file with the given name already exists";
            }
        }
        this.setState({
            filename : name,
            error : error,
        });
    }

    canCreateFileOrFolder = () => {
        if(this.state.error) {
            return false;
        } if (this.props.directoryMode) {
            return this.state.filename;
        } else {
            return this.state.filename && !this.state.filename.startsWith(".");
        }
    }

    createFileOrFolder = () => {
        const fullPath = this.getBasePath() + this.state.filename;
        if(this.props.directoryMode) {
            this.props.createDirectory(fullPath, true)
        } else {
            this.props.writeFile(fullPath,"",true)
        }
        this.props.onHide();
    }

    getBasePath = () => {
        const {selection, isDirectorySelected} = this.props;
        if(!selection) {
            return "";
        } else if (isDirectorySelected) {
            return selection + "/";
        } else {
            const lastSlash = selection.lastIndexOf("/");
            return selection.substring(0, lastSlash+1);
        }
    }

}

function mapStateToProps(state) {
    const { selection, files } = state.configuration;
    return {
        isDirectorySelected: configurationSelectors.isSelectionDirectory(state),
        files,
        selection
    }
}

const mapDispatchToProps = {
    writeFile: configurationActions.writeFile,
    createDirectory: configurationActions.createDirectory,
}

export default connect(mapStateToProps, mapDispatchToProps)(CreateDialog);