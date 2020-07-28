import React from 'react';
import { connect } from 'react-redux';
import { configurationActions, configurationUtils } from '../../../../redux/ducks/configuration';

import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';

/**
 * Dialog for deleting the currently selected file or folder.
 */
class DeleteDialog extends React.Component {
  state = {};

  deleteButton = React.createRef();

  render() {
    const { fileName, type } = this.state;

    return (
      <Dialog
        header={'Delete ' + type}
        focusOnShow={false}
        modal={true}
        visible={this.props.visible}
        onHide={this.props.onHide}
        footer={
          <div>
            <Button label="Delete" ref={this.deleteButton} className="p-button-danger" onClick={this.deleteSelectedFile} />
            <Button label="Cancel" className="p-button-secondary" onClick={this.props.onHide} />
          </div>
        }
      >
        Are you sure you want to delete <b>&quot;{fileName}&quot;</b> ? This cannot be undone!
      </Dialog>
    );
  }

  deleteSelectedFile = () => {
    this.props.deleteSelection(true, this.props.filePath);
    this.props.onHide();
  };

  componentDidUpdate(prevProps) {
    if (!prevProps.visible && this.props.visible) {
      /**Timeout is needed for .focus() to be triggered correctly. */
      setTimeout(() => {
        this.deleteButton.current.element.focus();
      }, 0);

      const { filePath } = this.props;

      const fileName = filePath ? filePath.split('/').slice(-1)[0] : '';

      const fileObj = configurationUtils.getFile(this.props.files, filePath);
      const type = configurationUtils.isDirectory(fileObj) ? 'Directory' : 'File';

      this.setState({ fileName, type });
    }
  }
}

function mapStateToProps(state) {
  const { selection, files } = state.configuration;
  return {
    selection,
    files,
  };
}

const mapDispatchToProps = {
  deleteSelection: configurationActions.deleteSelection,
};

export default connect(mapStateToProps, mapDispatchToProps)(DeleteDialog);
