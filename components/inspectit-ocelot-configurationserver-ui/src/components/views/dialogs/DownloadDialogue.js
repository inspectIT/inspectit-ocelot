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
const DownloadDialogue = ({
  visible,
  onHide,
  error,
  loading,
  contentValue,
  contentType,
  contextName,
  isDownloadDialogFooterHidden,
  onCancel,
}) => {
  const dialogueSettings = ContentTypeMapper(contentType, contextName);

  const downloadFilename =
    contentType + (contextName ? '_' + contextName.replace(/[^a-z0-9-]/gi, '_').toLowerCase() : '') + dialogueSettings.fileExtension;

  const download = () => {
    const blob = new Blob([contentValue], { type: dialogueSettings.mimeType });
    return window.URL.createObjectURL(blob);
  };

  const renderError = (errorType) => {
    switch (errorType) {
      case 'log':
        return `${dialogueSettings.header} could not be loaded due to an unexpected error.\n
                Ensure that both agent-commands and log-preloading are enabled and the agent-commands URL is correct in your agent
                configuration.\nThis feature is only available for agent versions 1.15.0 and higher`;
      case 'archive':
        return `Downloading the Support Archive for ${contextName} failed. Make sure agent-commands are enabled and the agent-commands URL is correct in your agent configuration.\n
                This feature is only available for agent versions 2.2.0 and higher`;
      default:
        return `${dialogueSettings.header} could not be loaded due to an unexpected error.`;
    }
  };

  return (
    <>
      <style jsx>{`
        .downloadDialogFooter {
          display: none;
        }
      `}</style>
      <Dialog
        style={{ width: '50vw', overflow: 'auto' }}
        header={dialogueSettings.header}
        modal={true}
        visible={visible}
        onHide={onHide}
        footer={
          <div className={isDownloadDialogFooterHidden ? 'downloadDialogFooter' : null}>
            <a href={error || loading ? null : download()} download={downloadFilename}>
              <Button icon="pi pi-download" label="Download" className="p-button-primary" disabled={loading || error} />
            </a>
            <Button label="Cancel" className="p-button-secondary" onClick={onCancel} />
          </div>
        }
      >
        {loading ? (
          <ProgressBar mode="indeterminate" />
        ) : error ? (
          renderError(contentType)
        ) : (
          <SyntaxHighlighter
            customStyle={{ maxHeight: '50vh' }}
            showLineNumbers={true}
            language={dialogueSettings.language}
            style={tomorrowNightBlue}
          >
            {contentValue}
          </SyntaxHighlighter>
        )}
      </Dialog>
    </>
  );
};

DownloadDialogue.propTypes = {
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
  /** Whether the cancel Button is disabled */
  disableDownloadCancelButton: PropTypes.bool,
  /** Callback on dialog cancel */
  onCancel: PropTypes.func,
};

DownloadDialogue.defaultProps = {
  error: false,
  visible: true,
  onHide: () => {},
  contentValue: 'No content found',
  contentType: '',
};

export default DownloadDialogue;
