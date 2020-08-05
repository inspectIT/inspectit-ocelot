import React, { useEffect, useState, useRef } from 'react';
import { Dialog } from 'primereact/dialog';
import { Button } from 'primereact/button';
import { ProgressBar } from 'primereact/progressbar';
import { InputText } from 'primereact/inputtext';
import _ from 'lodash';

/**
 * Dialog for showing the currently approved files before promoting them.
 */
const PromotionApprovalDialog = ({ visible, onHide, onPromote, isLoading, approvedFiles = [] }) => {
  // state variables
  const [message, setMessage] = useState('');

  // ref variables
  const messageInput = useRef(null);

  // derived variables
  const isValidMessage = !_.isEmpty(message);

  // clear message when dialog gets visible
  useEffect(() => {
    if (visible) {
      setMessage('');
    }
  }, [visible]);

  // invoke the promotion
  const promote = () => onPromote(message);

  // promote when enter is pressed
  const onKeyPress = (event) => {
    if (event.key === 'Enter' && !isLoading && isValidMessage) {
      promote();
    }
  };

  // set the focus to the input field after the dialog is shown
  const onShow = () => {
    messageInput.current.element.focus();
  };

  const footer = (
    <div>
      <Button label="Promote" onClick={promote} disabled={isLoading || !isValidMessage} />
      <Button label="Cancel" className="p-button-secondary" onClick={onHide} disabled={isLoading} />
    </div>
  );

  return (
    <>
      <style jsx>
        {`
          .list li {
            font-family: monospace;
          }
          .content :global(.p-progressbar) {
            height: 0.5rem;
          }
          .content :global(.message-input) {
            width: 100%;
            margin: 0.5rem 0 1rem;
          }
        `}
      </style>

      <Dialog
        header="Promote Configurations"
        focusOnShow={false}
        visible={visible}
        style={{ width: '50vw' }}
        modal={true}
        onHide={onHide}
        onShow={onShow}
        footer={footer}
      >
        <div className="content">
          <span>The following files have been approved and will be promoted:</span>
          <ul className="list">
            {approvedFiles.map((file) => (
              <li key={file}>{file}</li>
            ))}
          </ul>

          <span>Please add a message to describe the configuration promotion:</span>

          <InputText
            ref={messageInput}
            className="message-input"
            placeholder="Promotion message..."
            onKeyPress={onKeyPress}
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            disabled={isLoading}
          />

          {isLoading && <ProgressBar mode="indeterminate" />}
        </div>
      </Dialog>
    </>
  );
};

export default PromotionApprovalDialog;
