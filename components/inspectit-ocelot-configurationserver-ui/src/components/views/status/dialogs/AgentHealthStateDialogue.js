import React from 'react';
import { Dialog } from 'primereact/dialog';
import { Button } from 'primereact/button';
import PropTypes from 'prop-types';

/**
 * Dialog that shows the given content, applying syntax highlighting if available, and offering a download option
 */
const AgentHealthStateDialogue = ({ visible, onHide, error, contentValue, contextName }) => {

  const { health,  source, message, history } = contentValue
  const { healthInfo, iconColor } =  resolveHealthState(health);

  let latestIncidentElement = <>There are no incidents.</>

  if(source && message) {
    latestIncidentElement = <>
      <div>
        <h3>
          Latest Incident:
        </h3>
      </div>
      <div>
              <span style={{fontFamily: "monospace"
              }}>{source}:</span> <span className="italic">{message}</span>
      </div>
      <div className="line"/>
    </>
  }

  const historyElements = [];
  if(history && history.length > 0) {
    historyElements.push(
        <div className="bold">
          <h3>
            History:
          </h3>
        </div>
    )
    history.forEach(value => {
      const { time, source, health, message } = value;
      const { healthInfo, iconColor } = resolveHealthState(health);
      historyElements.push(
          <div>
            <b>{time}</b> <span style={{ color: iconColor,  fontWeight: "bold"}}>{healthInfo}</span> <span style={{fontFamily: "monospace"
          }}>{source}:</span> <br/> <span style={{fontStyle: "italic"}}>{message}</span>
            <div style={{borderBottom: "0.05rem solid rgba(228, 228, 228, 0.8)",
              width: "95%",
              margin: "auto",
              padding: "0.5rem",
              paddingBottom: "0.5rem",}}/>
          </div>)
    })
  }
  return (
    <>
      <style jsx>{`
        .bold {
          font-weight: bold;
        }
        
        .italic {
          font-style: italic;
        }
        
        .line {
          border-bottom: 0.15rem solid rgba(228, 228, 228, 0.8);
          width: 95%;
          margin: auto;
          padding-top: 0.5rem;
          padding-bottom: 0.5rem;
        }

      `}</style>
      <Dialog
        style={{ width: '50vw', overflow: 'auto' }}
        header={"Health State of " + contextName}
        modal={true}
        visible={visible}
        onHide={onHide}
        footer={
          <div>
            <Button label="Close" className="p-button-secondary" onClick={onHide} />
          </div>
        }
      >
        { error ? (
          <div>

          </div>
        ) : (
          <>
            <div>
                <h2>The agent is in an
              <span style={{ color: iconColor }}>{healthInfo}</span>
                   state
                </h2>
            </div>
              {latestIncidentElement}
            <div>
              {historyElements}
            </div>
          </>
        )}
      </Dialog>
    </>
  );
};

AgentHealthStateDialogue.propTypes = {
  /** Whether a error is thrown */
  error: PropTypes.bool,
  /** Whether the dialog is visible */
  visible: PropTypes.bool,
  /** Callback on dialog hide */
  onHide: PropTypes.func,
  /** The string value being displayed. E.g. the logs.*/
  contentValue: PropTypes.string,
  /** The type of content. E.g. config or log.*/
  contentType: PropTypes.string,
  /** The name of the data's context. E.g. the agent whose logs are being shown.*/
  contextName: PropTypes.string,
};

AgentHealthStateDialogue.defaultProps = {
  error: false,
  visible: true,
  onHide: () => {},
  contentValue: 'No content found',
  contentType: '',
};


const resolveHealthState = (health) => {
  switch (health) {
    case 'OK':
      return {
        healthInfo: ' OK ',
        iconClass: 'pi-check-circle',
        iconColor: '#0abd04',
      };
    case 'WARNING':
      return {
        healthInfo: ' Warning ',
        iconClass: 'pi-minus-circle',
        iconColor: '#e8c413',
      };
  }
  return {
    healthInfo: ' Error ',
    iconClass: 'pi-times-circle',
    iconColor: 'red',
  };
}

export default AgentHealthStateDialogue;
