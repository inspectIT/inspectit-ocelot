import React from 'react';
import DocsActionElement from './DocsActionElement';
import DocsElementTypes from './DocsElementTypes';
import DocsRuleElement from './DocsRuleElement';

const BaseDocsElement = ({ data, type, registerRef, scrollTo }) => {
  // render specific content based on element type
  const renderSpecificContent = (element) => {
    switch (type) {
      case DocsElementTypes.ACTION:
        return <DocsActionElement action={element} />;
      case DocsElementTypes.RULE:
        return <DocsRuleElement rule={element} scrollTo={scrollTo} />;
      case DocsElementTypes.METRIC:
      case DocsElementTypes.SCOPE:
      default:
        return null;
    }
  };

  return (
    <>
      <style jsx>{`
        .element {
          padding: 0.5rem;
          transition: background-color 0.2s;
          border-bottom: solid 1px #eeeeee;
        }
        .element:hover {
          background: #eaeaea;
        }
        .title-row {
          display: flex;
        }
        .title {
          flex: 1;
          font-family: monospace;
          border-left: 3px solid #e8a034;
          padding-left: 0.25rem;
          font-weight: bold;
        }
        .since {
          background-color: #007ad9;
          padding: 0rem 0.25rem;
          border-radius: 0.25rem;
          color: white;
        }
        .description {
          padding-top: 0.25rem;
          color: #333;
          white-space: pre-wrap;
        }
      `}</style>
      {data.map((element) => (
        <div
          key={element.name}
          className="element"
          ref={(ref) => {
            registerRef(element.name, ref);
          }}
        >
          <div className="title-row">
            <span className="title">{element.name}</span>
            {element.since && (
              <span className="since" title="This element exists since..">
                {element.since}
              </span>
            )}
          </div>
          <div>
            {element.description && <div className="description">{element.description}</div>}
            {renderSpecificContent(element)}
          </div>
        </div>
      ))}
    </>
  );
};

export default BaseDocsElement;
