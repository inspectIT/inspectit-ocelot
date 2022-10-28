import React from 'react';
import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';
import Dropzone from 'react-dropzone';
import { configurationActions } from '../../../../redux/ducks/configuration';
import PropTypes from 'prop-types';
import { isSelectionDirectory } from '../../../../redux/ducks/configuration/selectors';
import { connect } from 'react-redux';
import { notificationActions } from '../../../../redux/ducks/notification';
import _ from 'lodash';

var initialState = { files: [] };

class UploadDialog extends React.Component {
  duplicateWarning = (
    <p style={{ color: 'red' }}>
      <i style={{ verticalAlign: 'middle', color: 'red', fontSize: '2rem' }} className="pi pi-info-circle" />
      Some of your filenames already exist. Existing files will be overwritten.
    </p>
  );

  constructor() {
    super();
    this.onDrop = (files) => {
      // concat files with previously selected files if applicable.
      if (this.state.files && this.state.files.length > 0) {
        // remove duplicates
        let newFiles = files.filter((file) => !this.state.files.some((f) => f.name === file.name));
        // concat with previously selected files
        files = this.state.files.concat(newFiles);
      }

      this.setState({ files });
    };
    this.state = initialState;
  }

  removeFileByName(fileName) {
    var files = this.state.files;
    _.remove(files, (file) => {
      return file.name === fileName;
    });
    this.setState({
      files: files,
    });
  }

  render() {
    var { files, selection } = this.props;
    var duplicateInfo = <></>;

    const filesToUpload = this.state.files.map((file) => {
      if (this.isDuplicate(files, file)) {
        duplicateInfo = this.duplicateWarning;
      }

      var fileName = file.name;
      if (selection && !(_.endsWith(selection, '.yml') || _.endsWith(selection, '.yaml'))) {
        fileName = selection + '/' + file.name;
      }
      return (
        <div key={fileName}>
          <p>
            <i className="pi pi-file" style={{ color: 'lightgray', fontSize: '2rem', verticalAlign: 'middle', paddingRight: '0.75rem' }} />
            {fileName}
            <i
              className="pi pi-times"
              title="Click to remove file"
              onClick={() => this.removeFileByName(file.name)}
              style={{ color: 'red', fontSize: '2rem', verticalAlign: 'middle', float: 'right', cursor: 'pointer' }}
            />
          </p>
        </div>
      );
    });
    const baseStyle = {
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      padding: '20px',
      borderWidth: 2,
      borderRadius: 2,
      borderColor: '#2196f3',
      borderStyle: 'dashed',
      backgroundColor: '#fafafa',
      color: '#bdbdbd',
      transition: 'border .3s ease-in-out',
    };
    return (
      <Dialog
        header={'Upload Configuration Files'}
        modal={true}
        visible={this.props.visible}
        onHide={() => this.hideAndCleanState(this)}
        footer={
          <div>
            <Button label="Upload" onClick={this.uploadFiles} disabled={this.state.files.length < 1} />
            <Button label="Cancel" className="p-button-secondary" onClick={this.props.onHide} />
          </div>
        }
      >
        <div>{filesToUpload}</div>

        <Dropzone
          onDrop={this.onDrop}
          accept={{
            'text/yml': ['.yml', '.yaml'],
          }}
        >
          {({ getRootProps, getInputProps }) => (
            <section className="container" style={{ cursor: 'pointer' }}>
              <div style={baseStyle} {...getRootProps({ className: 'dropzone' })}>
                <input ref={this.input} {...getInputProps()} />
                <p>Drag and drop your configuration files here, or click to select files</p>
                <p>Only yaml files are being accepted!</p>
              </div>
            </section>
          )}
        </Dropzone>
        {duplicateInfo}
      </Dialog>
    );
  }

  hideAndCleanState = (that) => {
    that.setState(initialState);
    that.props.onHide();
  };

  isDuplicate = (persistedFiles, file) => {
    if (persistedFiles) {
      for (var persistedFile of persistedFiles) {
        if (file !== undefined && file.name === persistedFile.name) {
          return true;
        }
      }
    }
    return false;
  };

  uploadFiles = () => {
    const { isDirectory } = this.props;

    let fileNamePrefix = '/';
    if (isDirectory) {
      fileNamePrefix = this.props.selection + '/';
    }

    this.state.files.forEach((file) => {
      const fileName = fileNamePrefix + file.name;

      const fileReader = new FileReader();
      fileReader.readAsText(file);
      fileReader.onload = (e) => {
        const content = e.target.result;
        this.props.writeFile(fileName, content, true, true);
      };
    });

    this.props.onHide();
  };
}

function mapStateToProps(state) {
  const { selection, files } = state.configuration;

  const isDirectory = isSelectionDirectory(state);

  return {
    isDirectory,
    files,
    selection,
  };
}

const mapDispatchToProps = {
  writeFile: configurationActions.writeFile,
  showErrorMessage: notificationActions.showErrorMessage,
  showSuccessMessage: notificationActions.showSuccessMessage,
};

UploadDialog.props = {
  visible: PropTypes.bool,
  onHide: PropTypes.bool,
  writeFile: PropTypes.func,
};

UploadDialog.defaultProperties = {
  visible: true,
  onHide: () => {},
  writeFile: () => {},
};

export default connect(mapStateToProps, mapDispatchToProps)(UploadDialog);
