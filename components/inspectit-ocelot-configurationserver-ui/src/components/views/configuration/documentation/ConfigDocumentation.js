import React, { useRef } from 'react';
import BaseDocsElement from './BaseDocsElement';
import DocsElementTypes from './DocsElementTypes';
import { Growl } from 'primereact/growl';
import _ from 'lodash';

const ConfigDocumentation = ({ configurationDocs }) => {
  // growl for showing notifications
  const growl = useRef(null);

  // not managed by state
  const elementRefs = {};

  const registerRef = (name, ref) => {
    if (!elementRefs.hasOwnProperty(name)) {
      elementRefs[name] = ref;
    }
  };

  // scrolls to the given element
  const scrollTo = (elementName) => {
    if (elementRefs.hasOwnProperty(elementName)) {
      elementRefs[elementName].scrollIntoView();
    } else {
      growl.current.show({
        severity: 'warn',
        summary: 'Element Does Not Exist',
        detail: (
          <>
            'The element "<i>{elementName}</i>" does not exist in the current documentation.'
          </>
        ),
      });
    }
  };

  return (
    <>
      <style jsx>
        {`
          .docs-containger :global(.p-growl) {
            widht: 25rem;
          }
          .docs-container {
            flex: 1;
            background-color: #fff;
            overflow: auto;
          }
          .doc-section {
          }
          .section-heading {
            background-color: #eee;
            font-size: 1rem;
            font-weight: bold;
            padding: 1rem 0.5rem;
            border-bottom: 1px solid #dddddd;
            border-top: 1px solid #dddddd;
          }

          .doc-element {
            margin: 8px;
          }

          .doc-element-content {
            background-color: white;
            padding: 1px 10px;
          }

          .element-heading {
            padding: 2px 8px;
            margin: 0;
            background-color: #ffcd9c; /*#ffc387*/
            word-wrap: break-word;
          }

          button.doc-element-link {
            background: none;
            border: none;
            padding: 0;
            color: #007ad9;
            cursor: pointer;
            font-size: 1rem;
          }

          .actionInputDescription {
            color: #545454;
            font-family: '';
          }

          .placeholder {
            color: #777;
            padding: 1rem 0.5rem;
          }
        `}
      </style>

      <div className="docs-container">
        <Growl ref={growl} />

        <div className="doc-section">
          <div className="section-heading">Scopes</div>
          {_.isEmpty(configurationDocs.scopes) ? (
            <div className="placeholder">No scopes defined.</div>
          ) : (
            <BaseDocsElement data={configurationDocs.scopes} type={DocsElementTypes.SCOPE} registerRef={registerRef} />
          )}
        </div>

        <div className="doc-section">
          <div className="section-heading">Actions</div>
          {_.isEmpty(configurationDocs.actions) ? (
            <div className="placeholder">No actions defined.</div>
          ) : (
            <BaseDocsElement data={configurationDocs.actions} type={DocsElementTypes.ACTION} registerRef={registerRef} />
          )}
        </div>

        <div className="doc-section">
          <div className="section-heading">Rules</div>
          {_.isEmpty(configurationDocs.rules) ? (
            <div className="placeholder">No rules defined.</div>
          ) : (
            <BaseDocsElement data={configurationDocs.rules} type={DocsElementTypes.RULE} registerRef={registerRef} scrollTo={scrollTo} />
          )}
        </div>

        <div className="doc-section">
          <div className="section-heading">Metrics</div>
          {_.isEmpty(configurationDocs.metrics) ? (
            <div className="placeholder">No metrics defined.</div>
          ) : (
            <BaseDocsElement data={configurationDocs.metrics} type={DocsElementTypes.METRIC} registerRef={registerRef} />
          )}
        </div>
      </div>
    </>
  );
};

export default ConfigDocumentation;
