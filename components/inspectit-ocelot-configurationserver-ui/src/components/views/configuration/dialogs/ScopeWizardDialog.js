import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';
import { InputText } from 'primereact/inputtext';
import { Dropdown } from 'primereact/dropdown';
import { Fieldset } from 'primereact/fieldset';
import { Checkbox } from 'primereact/checkbox';
import { RadioButton } from 'primereact/radiobutton';
import React from 'react';

/**
 * The scope wizard dialog itself.
 */
class ScopeWizardDialog extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      classMatcher: '',
      classMatcherType: '',
      methodMatcherType: '',
      selectedMethodMatchers: [],
      isConstructor: 'false',
      isSelectedParameter: false,
      parameterList: [],
    };
  }

  onMethodMatcherChange(e) {
    let selectedMethodMatchers = this.state.selectedMethodMatchers;

    if (e.checked) {
      selectedMethodMatchers.push(e.value);
    } else {
      for (let i = 0; i < selectedMethodMatchers.length; i++) {
        const selectedMethodMatcher = selectedMethodMatchers[i];

        if (selectedMethodMatcher.key === e.value.key) {
          selectedMethodMatchers.splice(i, 1);
          break;
        }
      }
    }

    this.setState({ selectedMethodMatchers });
  }

  // handle parameter change
  handleParameterChange = (e, index) => {
    const parameterList = this.state.parameterList;
    parameterList[index].parameter = e.target.value;
    this.setState(parameterList);
  };

  removeParameter = (index) => {
    const parameterList = this.state.parameterList;
    parameterList.splice(index, 1);
    this.setState(parameterList);
  };

  addParameter = () => {
    this.setState({ parameterList: [...this.state.parameterList, { parameter: '' }] });
  };

  render() {
    const heightClassFieldset = window.innerHeight * 0.15;
    const heightMethodFieldset = window.innerHeight * 0.55;

    const tooltipOptions = {
      showDelay: 500,
      position: 'top',
    };

    const classMatchers = [
      { label: 'Class', value: 'class' },
      { label: 'Superclass', value: 'superClass' },
      { label: 'Interface', value: 'interface' },
    ];
    const matcherTypes = [
      { label: 'EQUALS_FULLY', value: 'equalsFully' },
      { label: 'MATCHES', value: 'matches' },
      { label: 'STARTS_WITH', value: 'startsWith' },
      { label: 'STARTS_WITH_IGNORE_CASE', value: 'startsWithIgnoreCase' },
      { label: 'CONTAINS', value: 'contains' },
      { label: 'CONTAINS_IGNORE_CASE', value: 'containsIgnoreCase' },
      { label: 'ENDS_WITH', value: 'endsWith' },
      { label: 'ENDS_WITH_IGNORE_CASE', value: 'endsWithIgnoreCase' },
    ];
    const methodVisibility = [
      { name: 'public', key: 'public' },
      { name: 'protected', key: 'protected' },
      { name: 'default', key: 'default' },
      { name: 'private', key: 'private' },
    ];

    // the dialogs footer
    const footer = (
      <div>
        <Button label="Add" />
        <Button label="Cancel" className="p-button-secondary" onClick={this.props.onHide} />
      </div>
    );

    return (
      <>
        <style jsx>{`
          .this :global(.p-dialog-content) {
            height: 75vh;
            border-left: 1px solid #ddd;
            border-right: 1px solid #ddd;
          }
          .row-center {
            display: flex;
            align-items: center;
          }
          .fill {
            flex-grow: 1;
          }
          .meta-row label {
            margin-right: 2rem;
          }
          .meta-row label:not(:first-child) {
            margin-left: 0.5rem;
          }
          .row-margin {
            margin-top: 1rem;
          }
          .this :global(.dd-branch) {
            width: 10rem;
          }
          .this :global(.in-name) {
            width: 100%;
          }
        `}</style>

        <div className="this">
          <Dialog
            className="scope-wizard-dialog"
            header="Define Selection"
            visible={this.props.visible}
            style={{ width: '50vw', minWidth: '50rem' }}
            onHide={this.props.onHide}
            blockScroll
            footer={footer}
            focusOnShow={false}
          >
            <Fieldset legend="Class Matcher" style={{ paddingTop: 0, height: heightClassFieldset, overflow: 'hidden' }}>
              <div className="row-center meta-row fill">
                <Dropdown
                  className="class-matcher-dropdown"
                  value={this.state.matcher}
                  options={classMatchers}
                  onChange={(e) => this.setState({ matcher: e.value })}
                  placeholder="Select a Class Matcher"
                />
                <span className="p-text">which</span>
                <Dropdown
                  className="class-matcher-type-dropdown"
                  value={this.state.classMatcherType}
                  options={matcherTypes}
                  onChange={(e) => this.setState({ classMatcherType: e.value })}
                  placeholder="Select a Matcher Type"
                />
                <InputText className="class-matcher-input" />
              </div>
            </Fieldset>

            <Fieldset legend="Method Matcher" style={{ paddingTop: 0, height: heightMethodFieldset }}>
              <div className="row-margin row-center meta-row">
                {methodVisibility.map((methodMatcher) => {
                  return (
                    <div key={methodMatcher.key} className="p-field-checkbox">
                      <Checkbox
                        inputId={methodMatcher.key}
                        name="methodMatcher"
                        value={methodMatcher}
                        onChange={(e) => this.onMethodMatcherChange(e)}
                        checked={this.state.selectedMethodMatchers.some((item) => item.key === methodMatcher.key)}
                      />
                      <label htmlFor={methodMatcher.key}>{methodMatcher.name}</label>
                    </div>
                  );
                })}
              </div>
              <div className="method-matcher-radio row-center meta-row">
                <div className="p-field-radiobutton">
                  <RadioButton
                    inputId="methodName"
                    name="methodName"
                    value="false"
                    onChange={(e) => this.setState({ isConstructor: e.value })}
                    checked={this.state.isConstructor === 'false'}
                  />
                  <label htmlFor="methodName">Method name</label>
                </div>
                <div className="p-field-radiobutton">
                  <RadioButton
                    inputId="constructor"
                    name="constructor"
                    value="true"
                    onChange={(e) => this.setState({ isConstructor: e.value })}
                    checked={this.state.isConstructor === 'true'}
                  />
                  <label htmlFor="constructor">Constructor</label>
                </div>
              </div>
              <div className="row-center meta-row fill">
                <Dropdown
                  className="method-matcher-dropdown"
                  value={this.state.methodMatcherType}
                  options={matcherTypes}
                  onChange={(e) => this.setState({ methodMatcherType: e.value })}
                  placeholder="Select a Matcher Type"
                />
                <InputText className="method-matcher-input" />
              </div>
              <div className="p-field-checkbox row-center row-margin fill">
                <Checkbox
                  inputId="selected-parameters"
                  name="selectedParameters"
                  value={this.state.isSelectedParameter}
                  onChange={(e) => this.setState({ isSelectedParameter: e.checked })}
                  checked={this.state.isSelectedParameter}
                />
                <label htmlFor={'onlyWithSelectedParameters'}>Only with selected Parameters</label>
              </div>
              <div className="parameterInputFields">
                <Fieldset legend="Method Arguments" style={{ paddingTop: 0, overflow: 'hidden' }}>
                  {this.state.parameterList.map((parameter, i) => {
                    return (
                      <div key={i} className="box row-margin">
                        <InputText
                          name="parameter"
                          placeholder="Enter a parameter"
                          value={parameter.parameter}
                          onChange={(e) => this.handleParameterChange(e, i)}
                        />
                        <Button
                          tooltip="Remove Parameter"
                          icon="pi pi-fw pi-trash"
                          tooltipOptions={tooltipOptions}
                          onClick={() => this.removeParameter(i)}
                        />
                      </div>
                    );
                  })}
                  <div className="addButton row-margin">
                    <Button
                      tooltip="Add Parameter"
                      icon="pi pi-plus"
                      tooltipOptions={tooltipOptions}
                      onClick={(e) => this.addParameter(e)}
                    />
                  </div>
                </Fieldset>
              </div>
            </Fieldset>
          </Dialog>
        </div>
      </>
    );
  }
}

export default ScopeWizardDialog;
