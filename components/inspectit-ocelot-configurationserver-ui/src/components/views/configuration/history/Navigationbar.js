import React from 'react';

class Navigationbar extends React.Component {
  render() {
    const { showHistoryView } = this.props;
    return (
      <>
        <style jsx>
          {`
            .button {
              border: none;
            }
            .button-selected {
              border: none;
              background-color: #cccccc;
            }
            .conatiner {
              display: flex;
              flex-direction: column;
            }

            .label {
              writing-mode: vertical-rl;
              font-size: 1.3em;
              padding-top: 1em;
              padding-bottom: 1em;
              color: #333333;
            }
          `}
        </style>

        <div>
          <button className={this.props.show ? 'button-selected' : 'button'} onClick={showHistoryView}>
            <div className="container">
              <div>
                <i className="pi pi-chevron-left" style={{ paddingTop: '1em' }}></i>
              </div>
              <div>
                <label className="label">Versioning</label>
              </div>
            </div>
          </button>
        </div>
      </>
    );
  }
}

export default Navigationbar;
