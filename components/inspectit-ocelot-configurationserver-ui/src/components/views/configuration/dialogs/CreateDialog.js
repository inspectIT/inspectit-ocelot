import React from 'react';
import { connect } from 'react-redux';

import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';
import { Message } from 'primereact/message';
import { InputText } from 'primereact/inputtext';
import { configurationUtils, configurationActions } from '../../../../redux/ducks/configuration';

/**
 * Dialog for creating a new file or directory.
 * The file/folder is placed relative to the currently selected one.
 * If the selection is empty, the file/folder will be placed under the root.
 * If a folder is selected, the created file/folder will be placed within it.
 * If a file is selected, the created file/folder will be placed next to it.
 */
class CreateDialog extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      /** In case the input name is invalid, a meaningful error message is placed here */
      error: null,
      filename: '',
    };
    this.input = React.createRef();
  }

  render() {
    const type = this.props.directoryMode ? 'Folder' : 'File';

    return (
      <Dialog
        focusOnShow={false}
        style={{ width: '400px' }}
        header={'Create ' + type}
        modal={true}
        visible={this.props.visible}
        onHide={this.props.onHide}
        onShow={() => this.filenameChanged('')}
        footer={
          <div>
            <Button label="Create" disabled={!this.canCreateFileOrFolder()} onClick={this.createFileOrFolder} />
            <Button label="Cancel" className="p-button-secondary" onClick={this.props.onHide} />
          </div>
        }
      >
        <div style={{ width: '100%', paddingBottom: '0.5em' }}>
          Create a {type.toLowerCase()} in <b>{this.state.parentDirectoryName}</b>:
        </div>
        <div className="p-inputgroup" style={{ width: '100%' }}>
          <InputText
            ref={this.input}
            style={{ width: '100%' }}
            onKeyPress={this.onKeyPress}
            value={this.state.filename}
            placeholder={type + ' Name'}
            onChange={(e) => this.filenameChanged(e.target.value)}
          />
          {!this.props.directoryMode && <span className="p-inputgroup-addon">.yml</span>}
        </div>
        {this.state.error && (
          <div style={{ width: '100%', paddingTop: '0.5em' }}>
            <Message style={{ width: '100%' }} severity="error" text={this.state.error}></Message>
          </div>
        )}
      </Dialog>
    );
  }

  componentDidUpdate(prevProps) {
    if (!prevProps.visible && this.props.visible) {
      /**Timeout is needed for .focus() to be triggered correctly. */
      setTimeout(() => {
        this.input.current.element.focus();
      }, 0);

      const { filePath } = this.props;

      const fileObj = configurationUtils.getFile(this.props.files, filePath);
      const isDirectory = configurationUtils.isDirectory(fileObj);

      let parentDirectoryName = 'root';
      if (filePath && isDirectory) {
        parentDirectoryName = '"' + filePath.split('/').slice(-1)[0] + '"';
      } else if (filePath) {
        let parent = filePath.split('/').slice(-2)[0];
        parentDirectoryName = parent ? '"' + parent + '"' : 'root';
      }

      this.setState({
        isDirectory,
        parentDirectoryName,
      });
    }
  }

  onKeyPress = (e) => {
    if (e.key === 'Enter' && this.canCreateFileOrFolder()) {
      this.createFileOrFolder();
    }
  };

  filenameChanged = (name) => {
    let error = null;
    const existingFile = configurationUtils.getFile(this.props.files, this.getAbsolutePath(name));
    if (existingFile) {
      if (configurationUtils.isDirectory(existingFile)) {
        error = 'A directory with the given name already exists';
      } else {
        error = 'A file with the given name already exists';
      }
    }
    this.setState({
      filename: name,
      error: error,
    });
  };

  canCreateFileOrFolder = () => {
    return !this.state.error && !!this.state.filename;
  };

  createFileOrFolder = () => {
    const fullPath = this.getAbsolutePath(this.state.filename);
    if (this.props.directoryMode) {
      this.props.createDirectory(fullPath, true, true);
    } else {
      this.props.writeFile(fullPath, '', true, true);
    }
    this.props.onHide();
  };

  /**
   * Returns the absolute path of the current filename relative to the selection
   */
  getAbsolutePath = (filename) => {
    const { directoryMode, filePath } = this.props;
    const { isDirectory } = this.state;

    const suffix = directoryMode ? '' : '.yml';
    if (!filePath) {
      return '/' + filename + suffix;
    } else if (isDirectory) {
      return filePath + '/' + filename + suffix;
    } else {
      const lastSlash = filePath.lastIndexOf('/');
      return filePath.substring(0, lastSlash + 1) + filename + suffix;
    }
  };
}

function mapStateToProps(state) {
  const { selection, files } = state.configuration;
  return {
    files,
    selection,
  };
}

const mapDispatchToProps = {
  writeFile: configurationActions.writeFile,
  createDirectory: configurationActions.createDirectory,
};

export default connect(mapStateToProps, mapDispatchToProps)(CreateDialog);
