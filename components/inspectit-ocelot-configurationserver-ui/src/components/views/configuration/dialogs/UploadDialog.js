import React from 'react';
import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';
import Dropzone from 'react-dropzone';
import { configurationActions } from '../../../../redux/ducks/configuration';
import PropTypes from 'prop-types';
import { isSelectionDirectory } from '../../../../redux/ducks/configuration/selectors';
import { connect } from 'react-redux';
import { notificationActions } from '../../../../redux/ducks/notification';

class UploadDialog extends React.Component {
  constructor() {
    super();
    this.handleClick = this.handleClick.bind(this);
    this.onDrop = (files) => {
      this.setState({ files });
    };
    this.state = {
      files: [],
    };
  }

  render() {
    const files = this.state.files.map((file) => <p key={file.name}> {file.name}</p>);
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
        header={'Upload one or more files'}
        modal={true}
        visible={this.props.visible}
        onHide={this.props.onHide}
        footer={
          <div>
            <Button label="Upload" onClick={this.handleClick} />
            <Button label="Cancel" className="p-button-secondary" onClick={this.props.onHide} />
          </div>
        }
      >
        <Dropzone onDrop={this.onDrop}>
          {({ getRootProps, getInputProps }) => (
            <section className="container">
              <div style={baseStyle} {...getRootProps({ className: 'dropzone' })}>
                <input ref={this.input} {...getInputProps()} />
                <p>Drag and drop some files here, or click to select files</p>
              </div>
              <div>{files}</div>
            </section>
          )}
        </Dropzone>
      </Dialog>
    );
  }

  handleClick = () => {
    const { showErrorMessage, showSuccessMessage } = this.props;
    if (this.props.isDirectory) {
      this.state.files.forEach((file) => {
        const fileName = this.props.selection + '/' + file.name;
        if (file.type === 'application/x-yaml') {
          let duplicate = false;
          this.props.files[0].children.forEach((persistedFile) => {
            if (file.name === persistedFile.name) {
              showErrorMessage('The file name must be unique');
              duplicate = true;
            }
          });
          if (!duplicate) {
            const fileReader = new FileReader();
            fileReader.readAsText(file);
            fileReader.onload = (e) => {
              const content = e.target.result;
              this.props.writeFile(fileName, content, true, true);
              showSuccessMessage('The upload was successful');
            };
          }
        } else {
          showErrorMessage('Wrong file type');
        }
      });
    } else {
      showErrorMessage('You have to select a directory');
    }
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
