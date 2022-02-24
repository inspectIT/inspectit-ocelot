import React from 'react';

class ConfigDocumentation extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      docElementRefs: {},
    };
  }

  scrollToDocElement(docElementName) {
    this.state.docElementRefs[docElementName].parentNode.parentNode.scroll(0, this.state.docElementRefs[docElementName].offsetTop - 75);
  }

  baseHtml(docObject, type) {
    return (
      <div
        className={'doc-element'}
        ref={(element) => {
          this.state.docElementRefs[docObject.name] = element;
        }}
      >
        <h3 className={'element-heading'}>{docObject.name}</h3>
        <div className={'doc-element-content'}>
          <p>{docObject.description}</p>
          {this.specificHtml(docObject, type)}
        </div>
      </div>
    );
  }

  specificHtml(docObject, type) {
    switch (type) {
      case 'action':
        return this.actionHtml(docObject);
      case 'rule':
        return this.ruleHtml(docObject);
      case 'metric':
        return this.metricHtml(docObject);
      default:
        return null;
    }
  }

  metricHtml(metric) {
    return <p>{metric.unit}</p>;
  }

  actionHtml(action) {
    return (
      <dl>
        {action.inputs.length > 0 && this.actionInputListHtml(action.inputs)}
        {action.returnDescription !== null && this.actionReturnDescriptionHtml(action.returnDescription)}
      </dl>
    );
  }

  actionInputListHtml(inputs) {
    return (
      <>
        <dt>
          <h4>Inputs:</h4>
        </dt>
        {inputs.map((input) => {
          return (
            <dd>
              <span className={'actionInputType'}>{input.type} </span>
              <span className={'actionInputName'}>{input.name} </span>
              <span className={'actionInputDescription'}>{input.description}</span>
            </dd>
          );
        })}
      </>
    );
  }

  actionReturnDescriptionHtml(returnDesc) {
    return (
      <dt>
        <dt>
          <h4>Return value:</h4>
        </dt>
        <dd>{returnDesc}</dd>
      </dt>
    );
  }

  ruleHtml(rule) {
    return (
      <>
        {rule.include.length > 0 && this.ruleIncludeHtml(rule.include)}
        {rule.scopes.length > 0 && this.ruleScopesHtml(rule.scopes)}
        {rule.tracingDoc !== null && this.ruleTracingHtml(rule.tracingDoc)}
        {Object.keys(rule.actionCallsMap)
          .map((key) => {
            return Object.keys(rule.actionCallsMap[key]).length;
          })
          .reduce((x, y) => {
            return x + y;
          }) > 0 && this.ruleEntryExitHtml(rule.actionCallsMap)}
        {rule.metricsDocs.length > 0 && this.ruleMetricsHtml(rule.metricsDocs)}
      </>
    );
  }

  ruleIncludeHtml(include) {
    return (
      <>
        <dt>
          <h4>Include:</h4>
        </dt>
        {include.map((singleInclude) => {
          return (
            <dd>
              <button className={'doc-element-link'} onClick={() => this.scrollToDocElement(singleInclude)}>
                {singleInclude}
              </button>
            </dd>
          );
        })}
      </>
    );
  }

  ruleScopesHtml(scopes) {
    return (
      <>
        <dt>
          <h4>Scopes:</h4>
        </dt>
        {scopes.map((singleScope) => {
          return (
            <dd>
              <button className={'doc-element-link'} onClick={() => this.scrollToDocElement(singleScope)}>
                {singleScope}
              </button>
            </dd>
          );
        })}
      </>
    );
  }

  ruleTracingHtml(tracing) {
    return (
      <>
        <dt>
          <h4>Tracing:</h4>
        </dt>
        <dd>start-span: {`${tracing.startSpan}`}</dd>
        <dd>{this.keyValueListHtml('Start-Span-Condtions', tracing.startSpanConditions)}</dd>
        <dd>{this.keyValueListHtml('Attributes', tracing.attributes)}</dd>
      </>
    );
  }

  ruleEntryExitHtml(actionCallsMap) {
    return (
      <>
        <dt>
          <h4>Attributes:</h4>
        </dt>
        {Object.keys(actionCallsMap).map((key) => {
          if (Object.keys(actionCallsMap[key]).length > 0) {
            return (
              <dd>
                <dl>
                  <dt>
                    <strong>{key}: </strong>
                  </dt>
                  {Object.keys(actionCallsMap[key]).map((attributeKey) => {
                    let actionCall = actionCallsMap[key][attributeKey];
                    return (
                      <dd>
                        {actionCall.name}:
                        <button className={'doc-element-link'} onClick={() => this.scrollToDocElement(actionCall.action)}>
                          {' '}
                          {actionCall.action}{' '}
                        </button>
                        {actionCall.inheritedFrom !== null && (
                          <>
                            <i>( from included rule </i>
                            <button className={'doc-element-link'} onClick={() => this.scrollToDocElement(actionCall.inheritedFrom)}>
                              {actionCall.inheritedFrom}{' '}
                            </button>
                            <i>)</i>
                          </>
                        )}
                      </dd>
                    );
                  })}
                </dl>
              </dd>
            );
          } else {
            return null;
          }
        })}
      </>
    );
  }

  ruleMetricsHtml(metrics) {
    return (
      <>
        <dt>
          <h4>Metrics:</h4>
        </dt>
        {metrics.map((singleMetric) => {
          return (
            <dd className={'metric'}>
              <button className={'doc-element-link'} onClick={() => this.scrollToDocElement(singleMetric.name)}>
                {singleMetric.name}
              </button>
              <dl>
                <dt>Value:</dt>
                <dd>singleMetric.value</dd>
                {this.keyValueListHtml('Constant Tags', singleMetric.constantTags)}
                {this.keyValueListHtml('Data Tags', singleMetric.dataTags)}
              </dl>
            </dd>
          );
        })}
      </>
    );
  }

  keyValueListHtml(title, entriesMap) {
    if (Object.keys(entriesMap).length > 0) {
      return (
        <dl>
          <dt>{title}:</dt>
          {Object.keys(entriesMap).map((key) => {
            return (
              <dd>
                {key}: {entriesMap[key]}
              </dd>
            );
          })}
        </dl>
      );
    } else {
      return null;
    }
  }

  docElements(docObjects, type) {
    return docObjects.map((docObject) => this.baseHtml(docObject, type));
  }

  render() {
    return (
      <>
        <style>
          {`
              h1 {
                  background-color: #de6f00;
                  color: #ffffff;
                  font-family: "Segoe UI Semibold";
                  padding: 12px;
                  margin: 2px;
              }
              
              .fullDoc {
                font-family: "Segoe UI";
                white-space: pre-wrap;
                width: 35rem;
                overflow: auto;
                max-height: calc(100% - 75px);
                bottom: 0;
              }
              
              .doc-section{
                  background-color: #e0e0e0;
                  padding: 1px 2px;
                  border: 1px solid grey;
                  margin: 5px;
              }
              
              .section-heading {
                  padding-left: 5px;
                  margin-bottom: 15px;
                  margin-top: 5px;
              }
              
              .doc-element{
                  margin: 8px;
              }
              
              .doc-element-content{
                  background-color: white;
                  padding: 1px 10px;
              }
              
              .element-heading{
                  padding: 2px 8px;
                  margin: 0;
                  background-color: #ffcd9c; /*#ffc387*/
                  word-wrap: break-word;
              }
              
              button.doc-element-link{
                background: none;
                border: none;
                padding: 0;
                color: #007ad9;
                cursor: pointer;
                font-size: 1rem;
              }
              
              .actionInputDescription{
                  color: #545454;
                  font-family: "";
              }
            `}
        </style>

        <div className="fullDoc">
          <h1>inspectIT Ocelot Configuration Documentation</h1>
          <div className="doc-section">
            <h2 className="section-heading">Scopes</h2>
            {this.props.configDocumentation !== null && this.docElements(this.props.configDocumentation.scopes, 'scope')}
          </div>
          <div className="doc-section">
            <h2 className="section-heading">Actions</h2>
            {this.props.configDocumentation !== null && this.docElements(this.props.configDocumentation.actions, 'action')}
          </div>
          <div className="doc-section">
            <h2 className="section-heading">Rules</h2>
            {this.props.configDocumentation !== null && this.docElements(this.props.configDocumentation.rules, 'rule')}
          </div>
          <div className="doc-section">
            <h2 className="section-heading">Metrics</h2>
            {this.props.configDocumentation !== null && this.docElements(this.props.configDocumentation.metrics, 'metric')}
          </div>
        </div>
      </>
    );
  }
}

export default ConfigDocumentation;
