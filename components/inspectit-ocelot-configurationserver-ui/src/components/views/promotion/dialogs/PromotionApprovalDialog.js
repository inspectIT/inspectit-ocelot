import React from 'react';
import { Dialog } from 'primereact/dialog';
import { Button } from 'primereact/button';
import { ProgressBar } from 'primereact/progressbar';
import { InputText } from 'primereact/inputtext';

/**
 * Dialog for showing the currently approved files before promoting them.
 */
class PromotionApprovalDialog extends React.Component {
  state = {
    /** The promotion message to be send to the backend. */
    promotionMessage: '',
    /** If true, the promotion button is disabled. Is true when either the promotion message is empty or isLoading is true. */
    promotionDisabled: true,
  };
  input = React.createRef();
  render() {
    const { visible, onHide, isLoading, approvedFiles = [] } = this.props;
    const { promotionMessage } = this.state;
    const footer = (
      <div>
        <Button label="Promote" onClick={this.promote} disabled={this.state.promotionDisabled} />
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
          `}
        </style>

        <Dialog header="Promote Configurations" visible={visible} style={{ width: '50vw' }} modal={true} onHide={onHide} footer={footer}>
          <div className="content">
            <span>The following files have been approved and will be promoted:</span>
            <ul className="list">
              {approvedFiles.map((file) => (
                <li key={file}>{file}</li>
              ))}
            </ul>

            {isLoading && <ProgressBar mode="indeterminate" />}
          </div>
          <InputText
            ref={this.input}
            style={{ width: '100%' }}
            placeholder="Promotion Message"
            onKeyPress={this.onKeyPress}
            value={promotionMessage}
            onChange={(e) => this.setState({ promotionMessage: e.target.value })}
            onKeyUp={() => this.setState({ promotionDisabled: promotionMessage >= 0 && !this.props.isLoading })}
          />
        </Dialog>
      </>
    );
  }

  componentDidUpdate() {
    if (this.state.resetSelection) {
      const { promotionMessage } = this.state;
      const inputElem = this.input.current.element;
      inputElem.focus();
      this.setState({ resetSelection: false });
    }
  }

  onKeyPress = (e) => {
    if (e.key === 'Enter' && !this.state.promotionDisabled) {
      this.promote();
    }
  };

  promote = () => {
    this.props.onPromote(this.state.promotionMessage);
  };

  onShow = () => {
    const promotionMessage = '';

    this.setState({
      promotionMessage,
      resetSelection: true,
    });
  };
}

export default PromotionApprovalDialog;
