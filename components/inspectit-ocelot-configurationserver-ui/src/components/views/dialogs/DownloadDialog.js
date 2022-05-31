import React from 'react';
import { Dialog } from 'primereact/dialog';
import { Button } from 'primereact/button';
import SyntaxHighlighter from 'react-syntax-highlighter';
import { tomorrowNightBlue } from 'react-syntax-highlighter/dist/cjs/styles/hljs';
import { ProgressBar } from 'primereact/progressbar';
import PropTypes from 'prop-types';
import { ContentTypeMapper } from '../mappings/ContentTypeMapper';

/**
 * Dialog that shows the given content, applying syntax highlighting if available, and offering a download option
 */
const DownloadDialog = ({ visible, onHide, error, loading, contentValue, contentType, contextName }) => {
  const fileConfig = ContentTypeMapper(contentType, contextName);

  const downloadFilename =
    contentType + (contextName ? '_' + contextName.replace(/[^a-z0-9-]/gi, '_').toLowerCase() : '') + fileConfig.fileExtension;

  const download = () => {
    const blob = new Blob([contentValue], { type: fileConfig.mimeType });
    return window.URL.createObjectURL(blob);
  };

  return (
    <>
      <Dialog
        style={{ width: '50vw', overflow: 'auto' }}
        header={fileConfig.header}
        modal={true}
        visible={visible}
        onHide={onHide}
        footer={
          <div>
            <a href={error || loading ? null : download()} download={downloadFilename}>
              <Button icon="pi pi-download" label="Download" className="p-button-primary" disabled={loading || error} />
            </a>
            <Button label="Cancel" className="p-button-secondary" onClick={onHide} />
          </div>
        }
      >
        {loading ? (
          <ProgressBar mode="indeterminate" />
        ) : error ? (
          <div>The agent configuration could not been loaded due to an unexpected error.</div>
        ) : (
          <SyntaxHighlighter
            customStyle={{ maxHeight: '50vh' }}
            showLineNumbers={true}
            language={fileConfig.language}
            style={tomorrowNightBlue}
          >
            {contentValue}
          </SyntaxHighlighter>
        )}
      </Dialog>
    </>
  );
};

DownloadDialog.propTypes = {
  /** Whether a error is thrown */
  error: PropTypes.bool,
  /** Whether the dialog is visible */
  visible: PropTypes.bool,
  /** Whether the file is loading */
  loading: PropTypes.bool,
  /** Callback on dialog hide */
  onHide: PropTypes.func,
  /** The string value being displayed. E.g. the logs.*/
  contentValue: PropTypes.string,
  /** The type of content. E.g. config or log.*/
  contentType: PropTypes.string,
  /** The name of the data's context. E.g. the agent whose logs are being shown.*/
  contextName: PropTypes.string,
};

DownloadDialog.defaultProps = {
  error: false,
  visible: true,
  onHide: () => {},
  contentValue: 'No content found',
};

export default DownloadDialog;
