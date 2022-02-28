import React from 'react';

const DocsActionElement = ({ action }) => {
  const { inputs, returnDescription } = action;

  return (
    <>
      <style jsx>{`
        .headline {
          margin-top: 0.5rem;
          margin-bottom: 0.25rem;
          font-weight: bold;
          color: #4e4e4e;
        }
        .item {
          padding-left: 0.5rem;
          display: flex;
        }
        .item-meta {
          white-space: nowrap;
          margin-right: 0.5rem;
        }
        .item-type {
          font-family: monospace;
        }
        .item-name {
          font-family: monospace;
          font-weight: bold;
          color: #1c6e5c;
        }
        .item-description {
        }
      `}</style>
      <div>
        {inputs.length > 0 && (
          <>
            <div className="headline">Inputs</div>
            {inputs.map(({ type, name, description }) => {
              return (
                <div className="item" key={name}>
                  <div className="item-meta">
                    <span className="item-type">{type}</span> <span className="item-name">{name}</span>
                    {description && ' - '}
                  </div>
                  {description && <div className="item-description">{description}</div>}
                </div>
              );
            })}
          </>
        )}
        {returnDescription && (
          <>
            <div className="headline">Return Value</div>
            <div className="item">{returnDescription}</div>
          </>
        )}
      </div>
    </>
  );
};

export default DocsActionElement;
