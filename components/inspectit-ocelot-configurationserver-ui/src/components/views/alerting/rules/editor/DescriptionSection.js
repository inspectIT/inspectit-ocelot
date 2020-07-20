import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { Inplace, InplaceDisplay, InplaceContent } from 'primereact/inplace';
import TextAreaEditor from '../../../../common/value-editors/TextAreaEditor';
import Section from '../../Section';
import classnames from 'classnames';

/**
 * This component combines displaying and editing of a description text.
 */
const DescriptionSection = ({ value, readOnly, updateValue }) => {
  // state variables
  const [inplaceActive, setInplaceState] = useState(false);
  const [inplaceHeight, setInplaceHeight] = useState(80);

  const descriptionRef = React.createRef();

  const toggleInplace = (event) => {
    setInplaceState(event.value);
    setInplaceHeight(Math.max(descriptionRef.current.clientHeight, 80));
  };

  const updateDescription = (value) => {
    updateValue(value);
    setInplaceState(false);
  };

  const content = (
    <div className={classnames('inplace', { placeholder: !value })} ref={descriptionRef}>
      {value || 'no description'}
    </div>
  );

  return (
    <>
      <style jsx>
        {`
          .this :global(.inplace) {
            white-space: pre-wrap;
          }
          .this :global(.placeholder) {
            color: #9e9e9e;
            font-style: italic;
          }
          .this :global(.p-inplace-display) {
            display: block;
          }
          .simple-content {
            margin-left: 0.5rem;
          }
        `}
      </style>

      <div className="this">
        <Section title="Description">
          {readOnly ? (
            <div className="simple-content">content</div>
          ) : (
            <Inplace active={inplaceActive} onToggle={toggleInplace}>
              <InplaceDisplay>{content}</InplaceDisplay>
              <InplaceContent>
                <TextAreaEditor height={inplaceHeight} value={value} updateValue={updateDescription} />
              </InplaceContent>
            </Inplace>
          )}
        </Section>
      </div>
    </>
  );
};

DescriptionSection.propTypes = {
  /** The description value to edit */
  value: PropTypes.string.isRequired,
  /** Whether the string is read only */
  readOnly: PropTypes.bool,
  /** Callback on value update */
  updateValue: PropTypes.func,
};

DescriptionSection.defaultProps = {
  readOnly: false,
  updateValue: () => {},
};

export default DescriptionSection;
