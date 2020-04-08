import React from 'react';
import { connect } from 'react-redux';
import { notificationActions } from '../../../../redux/ducks/notification';

import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';
import KeyValueEditor from '../editComponents/KeyValueEditor';
import ConfigurationDownload from '../ConfigurationDownload';

/**
 * Dialog for entering key/value pairs and downloading a configuration file.
 */
class DownloadDialog extends React.Component {
  downloadButton = React.createRef();
  configDownload = React.createRef();

  state = {
    attributes: [],
  };

  handleChangeAttributes = (newAttributes) => {
    this.setState({ attributes: newAttributes });
  };

  render() {
    return (
      <Dialog
        header={'Download Configuration File'}
        modal={true}
        visible={this.props.visible}
        onHide={this.props.onHide}
        style={{ maxWidth: '900px' }}
        footer={
          <div>
            <Button label="Download" ref={this.downloadButton} className="p-button-primary" onClick={this.handleDownload} />
            <Button label="Cancel" className="p-button-secondary" onClick={this.props.onHide} />
          </div>
        }
      >
        <p style={{ marginBottom: '1em' }}>
          Enter key/value pairs to download the correlating agent configuration.
          <br /> You will get a different result depending on the mapping that fits your input.
        </p>
        <KeyValueEditor onChange={this.handleChangeAttributes} keyValueArray={this.state.attributes} maxHeight={`300px`} />
        {/** creating reference of DownloadLink, to be able to call download within this.handleDownload */}
        <ConfigurationDownload onRef={(ref) => (this.configDownload = ref)} />
      </Dialog>
    );
  }

  /**
   * triggers download of config file and onHide
   */
  handleDownload = () => {
    const sanitizedAttributes = {};

    let showWarning;
    for (const pair of this.state.attributes) {
      if (sanitizedAttributes[pair.key]) {
        showWarning = true;
      } else if (pair.key) {
        sanitizedAttributes[pair.key] = pair.value || '';
      } else {
        showWarning = true;
      }
    }

    if (showWarning) {
      this.props.showWarningMessage('Invalid Input', 'Some attribute keys were double or empty and have been discarded');
    }

    this.props.onHide();
    this.configDownload.download(sanitizedAttributes);
  };

  componentDidUpdate(prevProps) {
    if (!prevProps.visible && this.props.visible) {
      this.downloadButton.current.element.focus();
    }
  }
}

const mapDispatchToProps = {
  showWarningMessage: notificationActions.showWarningMessage,
};

export default connect(null, mapDispatchToProps)(DownloadDialog);
