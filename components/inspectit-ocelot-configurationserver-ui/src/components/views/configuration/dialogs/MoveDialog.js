import React from 'react';
import { connect } from 'react-redux';

import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';
import { InputText } from 'primereact/inputtext';
import { configurationUtils, configurationActions, configurationSelectors } from '../../../../redux/ducks/configuration';

/**
 * Dialog for moving/renaming the selected file or directory.
 */
class MoveDialog extends React.Component {

    state = {
        /** In case the input name is invalid, a meaningful error message is placed here*/
        error: "",
        /** The name of the target file / folder without leading slash and ending */
        targetPath: "",
        /** The ending of the file, e.g. ".yml" or empty in case of a folder */
        targetPathEnding: "",
    };

    input = React.createRef();

    render() {
        const { onHide } = this.props;
        const { targetPath, targetPathEnding, selectedFile, selectionName, isDir } = this.state;

        const type = isDir ? "Directory" : "File";
        const originalParent = selectedFile && configurationUtils.getParentDirectoryPath(selectedFile);
        const newParent = configurationUtils.getParentDirectoryPath(this.getAbsoluteTargetPath());
        const isRename = originalParent == newParent;

        return (
            <Dialog
                style={{ width: '600px' }}
                header={"Move " + type}
                modal={true}
                visible={this.props.visible}
                onHide={onHide}
                onShow={this.onShow}
                footer={(
                    <div>
                        <Button label={isRename ? "Rename" : "Move"} onClick={this.performMove} />
                        <Button label="Cancel" className="p-button-secondary" onClick={onHide} />
                    </div>
                )}
            >
                <div>
                    Move / Rename <b>"{selectionName}"</b> to the following path:
                </div>
                <div className="p-inputgroup" style={{ width: '100%', marginTop: "0.5em" }}>
                    <span className="p-inputgroup-addon">/</span>
                    <InputText
                        ref={this.input}
                        style={{ width: '100%' }}
                        placeholder="Target Path"
                        onKeyPress={this.onKeyPress}
                        value={targetPath}
                        onChange={(e) => this.setState({ targetPath: e.target.value })}
                    />
                    {targetPathEnding &&
                        <span className="p-inputgroup-addon">{targetPathEnding}</span>
                    }
                </div>
            </Dialog>
        )
    }

    componentDidUpdate() {
        if (this.state.resetSelection) {
            const { targetPath } = this.state;
            const selStart = targetPath.lastIndexOf("/") + 1;
            const selEnd = targetPath.length;
            const inputElem = this.input.current.element;
            inputElem.focus();
            inputElem.setSelectionRange(selStart, selEnd);
            this.setState({ resetSelection: false });
        }
    }

    onKeyPress = (e) => {
        if (e.key === 'Enter') {
            this.performMove();
        }
    }

    performMove = () => {
        const source = this.state.selectedFile;
        const target = this.getAbsoluteTargetPath();
        if (source != target) {
            this.props.move(source, target, true);
        }
        this.props.onHide();
    }

    onShow = () => {
        /** Pick selection between redux state selection and incoming property selection. */
        const { selection, filePath } = this.props;

        const selectedFile = filePath || selection;
        const selectionName = selectedFile ? selectedFile.split("/").slice(-1)[0] : "";
        const fileObj = configurationUtils.getFile(this.props.files, selectedFile);
        const isDir = configurationUtils.isDirectory(fileObj);

        /** Set target path from selection. */
        //remove leading slash
        let targetPath = selectedFile ? selectedFile.substring(1) : "";
        let targetPathEnding = "";
        if (!isDir) {
            const lastDot = targetPath.lastIndexOf(".");
            const lastSlash = targetPath.lastIndexOf("/");
            if (lastDot != -1 && lastDot > lastSlash) {
                targetPathEnding = targetPath.substring(lastDot);
                targetPath = targetPath.substring(0, lastDot);
            }
        }

        this.setState({
            targetPathEnding,
            targetPath,
            resetSelection: true,
            selectedFile,
            selectionName,
            isDir
        });
    }

    getAbsoluteTargetPath = () => {
        const { targetPath, targetPathEnding } = this.state;
        let path = "/" + targetPath + targetPathEnding;
        //sanitzie the path by removing doubled slashes
        while (path.indexOf("//") != -1) {
            path = path.replace("//", "/");
        }
        return path;
    }
}

function mapStateToProps(state) {
    const { selection } = state.configuration;
    return {
        selection,
    }
}

const mapDispatchToProps = {
    move: configurationActions.move
}

export default connect(mapStateToProps, mapDispatchToProps)(MoveDialog);