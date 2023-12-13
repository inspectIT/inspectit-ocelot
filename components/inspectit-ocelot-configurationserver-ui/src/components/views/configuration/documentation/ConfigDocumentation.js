import React, { useRef } from 'react';
import BaseDocsElement from './BaseDocsElement';
import DocsElementTypes from './DocsElementTypes';
import { Toast } from 'primereact/toast';
import _ from 'lodash';

const ConfigDocumentation = React.memo(({ configurationDocs, selectedAgentMapping }) => {
  // toast for showing notifications
  const toast = useRef(null);

  // not managed by state
  const elementRefs = {};

  const registerRef = (name, ref) => {
    if (!_.has(elementRefs, name)) {
      elementRefs[name] = ref;
    }
  };

  // scrolls to the given element
  const scrollTo = (elementName) => {
    if (_.has(elementRefs, elementName)) {
      elementRefs[elementName].scrollIntoView();
    } else {
      toast.current.show({
        severity: 'warn',
        summary: 'Element Does Not Exist',
        detail: (
          <>
            The element &quot;<i>{elementName}</i>&quot; does not exist in the current documentation.
          </>
        ),
      });
    }
  };

  return (
    <>
      <style jsx>
        {`
          .docs-containger :global(.p-toast) {
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
            padding: 1rem;
            border-bottom: 1px solid #dddddd;
            border-top: 1px solid #dddddd;
          }
          .placeholder {
            color: #777;
            padding: 1rem 0.5rem;
          }
        `}
      </style>

      <div className="docs-container">
        <Toast ref={toast} />

        <div className="doc-section">
          <div className="section-heading">Scopes</div>
          {_.isEmpty(configurationDocs.scopes) ? (
            <div className="placeholder">No scopes defined.</div>
          ) : (
            <BaseDocsElement
              data={configurationDocs.scopes}
              type={DocsElementTypes.SCOPE}
              registerRef={registerRef}
              selectedAgentMapping={selectedAgentMapping}
            />
          )}
        </div>

        <div className="doc-section">
          <div className="section-heading">Actions</div>
          {_.isEmpty(configurationDocs.actions) ? (
            <div className="placeholder">No actions defined.</div>
          ) : (
            <BaseDocsElement
              data={configurationDocs.actions}
              type={DocsElementTypes.ACTION}
              registerRef={registerRef}
              selectedAgentMapping={selectedAgentMapping}
            />
          )}
        </div>

        <div className="doc-section">
          <div className="section-heading">Rules</div>
          {_.isEmpty(configurationDocs.rules) ? (
            <div className="placeholder">No rules defined.</div>
          ) : (
            <BaseDocsElement
              data={configurationDocs.rules}
              type={DocsElementTypes.RULE}
              registerRef={registerRef}
              scrollTo={scrollTo}
              selectedAgentMapping={selectedAgentMapping}
            />
          )}
        </div>

        <div className="doc-section">
          <div className="section-heading">Metrics</div>
          {_.isEmpty(configurationDocs.metrics) ? (
            <div className="placeholder">No metrics defined.</div>
          ) : (
            <BaseDocsElement
              data={configurationDocs.metrics}
              type={DocsElementTypes.METRIC}
              registerRef={registerRef}
              selectedAgentMapping={selectedAgentMapping}
            />
          )}
        </div>
      </div>
    </>
  );
}, _.isEqual);

ConfigDocumentation.displayName = 'ConfigDocumentation';

export default ConfigDocumentation;
