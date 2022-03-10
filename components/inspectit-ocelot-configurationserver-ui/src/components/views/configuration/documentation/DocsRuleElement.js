import React from 'react';
import _ from 'lodash';
import ReactTooltip from 'react-tooltip';

const KeyValueItem = ({ keyString, value, valueOrigin, ruleOrigin, scrollTo }) => {
  return (
    <>
      <style jsx>{`
        a {
          cursor: pointer;
        }
        a:hover {
          color: #e8a034;
        }
        .items {
          padding-left: 0.5rem;
          display: flex;
          flex-direction: column;
          align-items: flex-start;
        }
        .key,
        .value {
          font-family: monospace;
        }
        .value {
          color: #1c6e5c;
        }
        .origin {
          font-size: 1rem;
          color: #666;
          cursor: pointer;
        }
        .this {
        }
      `}</style>
      <div className="this">
        <span className="key">{keyString}</span>:{' '}
        {valueOrigin ? (
          <a className="value" onClick={() => scrollTo(value)}>
            {_.toString(value)}
          </a>
        ) : (
          <span className="value">{_.toString(value)}</span>
        )}{' '}
        {ruleOrigin && (
          <i className="pi pi-external-link origin" data-tip={'included from rule: ' + ruleOrigin} onClick={() => scrollTo(ruleOrigin)}></i>
        )}
      </div>
    </>
  );
};

const DocsRuleElement = ({ rule, scrollTo }) => {
  const { include, scopes, metricsDocs, tracingDoc, actionCallsMap } = rule;
  const linkItems = (items) => {
    return (
      <>
        <style jsx>{`
          a {
            cursor: pointer;
          }
          a:hover {
            color: #e8a034;
          }
          .items {
            padding-left: 0.5rem;
            display: flex;
            flex-direction: column;
            align-items: flex-start;
          }
        `}</style>
        <div className="items">
          {items.map((item) => {
            return (
              <a key={item} onClick={() => scrollTo(item)}>
                {item}
              </a>
            );
          })}
        </div>
      </>
    );
  };

  return (
    <>
      <style jsx>{`
        .headline {
          margin-top: 0.5rem;
          margin-bottom: 0.25rem;
          font-weight: bold;
          color: #4e4e4e;
        }
        .indented {
          padding-left: 0.5rem;
        }
        .attributes {
          display: flex;
          flex-direction: column;
        }
        .clickable {
          color: #51a4e5;
          cursor: pointer;
        }
        .clickable:hover {
          color: #e8a034;
        }
      `}</style>

      <ReactTooltip effect="solid" />

      <div>
        {include.length > 0 && (
          <>
            <div className="headline">Included Rules</div>
            {linkItems(include)}
          </>
        )}

        {scopes.length > 0 && (
          <>
            <div className="headline">Scopes</div>
            {linkItems(scopes)}
          </>
        )}

        {tracingDoc && (
          <>
            <div className="headline">Tracing</div>
            <div className="indented">
              {tracingDoc.startSpan && <KeyValueItem keyString="start-span" value={tracingDoc.startSpan} scrollTo={scrollTo} />}
              {tracingDoc.startSpanConditions && (
                <>
                  <div className="headline">Start-Span Conditions</div>
                  <div className="indented">
                    {_.map(tracingDoc.startSpanConditions, (value, condition) => (
                      <KeyValueItem key={condition} keyString={condition} value={value} scrollTo={scrollTo} />
                    ))}
                  </div>
                </>
              )}
            </div>
          </>
        )}

        {actionCallsMap && (
          <>
            <div className="headline">Attributes</div>
            {_.map(actionCallsMap, (attributes, attributeKey) => {
              if (_.isEmpty(attributes)) {
                return null;
              }
              return (
                <div key={attributeKey} className="indented">
                  <div className="headline">{attributeKey}</div>
                  <div className="indented attributes">
                    {_.map(attributes, ({ name, actionName, inheritedFrom }) => (
                      <KeyValueItem
                        key={name}
                        keyString={name}
                        value={actionName}
                        valueOrigin={actionName}
                        ruleOrigin={inheritedFrom}
                        scrollTo={scrollTo}
                      />
                    ))}
                  </div>
                </div>
              );
            })}
          </>
        )}

        {metricsDocs.length > 0 && (
          <>
            <div className="headline">Metrics</div>
            {metricsDocs.map((metric) => {
              const { name, value, constantTags, dataTags } = metric;
              return (
                <div key={name} className="indented">
                  <div className="headline clickable" onClick={() => scrollTo(name)}>
                    {name}
                  </div>
                  <div className="indented">
                    <KeyValueItem keyString="Value" value={value} />
                    <div className="headline">Constant Tags</div>
                    <div className="indented">
                      {_.map(constantTags, (tagValue, tagName) => (
                        <KeyValueItem key={tagName} keyString={tagName} value={tagValue} />
                      ))}
                    </div>
                    <div className="headline">Data Tags</div>
                    <div className="indented">
                      {_.map(dataTags, (tagValue, tagName) => (
                        <KeyValueItem key={tagName} keyString={tagName} value={tagValue} />
                      ))}
                    </div>
                  </div>
                </div>
              );
            })}
          </>
        )}
      </div>
    </>
  );
};

export default DocsRuleElement;
