import React, { useEffect } from 'react';
import PropTypes from 'prop-types';
import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';

/**
 * A generic Dialog for deleting the given element.
 */
const ConvertDialog = ({ name, visible, onHide, text, onSuccess }) => {
  const proceedButton = React.createRef();

  useEffect(() => {
    if (proceedButton?.current?.element) {
      proceedButton.current.element.focus();
    }
  });

  return (
    <Dialog
      header={text}
      modal={true}
      visible={visible}
      onHide={onHide}
      style={{ maxWidth: '45rem' }}
      footer={
        <div>
          <Button
            label="Proceed"
            ref={proceedButton}
            className="p-button-danger"
            onClick={() => {
              onSuccess();
              onHide();
            }}
          />
          <Button label="Cancel" className="p-button-secondary" onClick={onHide} />
        </div>
      }
    >
      <p>
        The UI-based editor will be removed from file <b>&quot;{name}&quot;</b> and it will be converted into a YAML configuration file!
        This process <b>cannot</b> be reverted!
      </p>
    </Dialog>
  );
};

ConvertDialog.propTypes = {
  /** The name of the element to convert */
  name: PropTypes.string,
  /** The text to show in the dialog */
  text: PropTypes.string,
  /** Whether the dialog is visible */
  visible: PropTypes.bool,
  /** Callback on conversion success */
  onSuccess: PropTypes.func,
  /** Callback on dialog hide */
  onHide: PropTypes.func,
};

ConvertDialog.defaultProps = {
  text: 'Warning',
  visible: true,
  onSuccess: () => {},
  onHide: () => {},
};

export default ConvertDialog;
