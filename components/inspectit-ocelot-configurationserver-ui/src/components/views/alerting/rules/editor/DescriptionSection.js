import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { Inplace, InplaceDisplay, InplaceContent } from 'primereact/inplace';
import TextAreaEditor from '../../../../common/value-editors/TextAreaEditor';

/**
 * This component combines displaying and editing of a description text.
 */
const DescriptionSection = ({ value, readOnly, updateValue }) => {
  const [inplaceActive, setInplaceState] = useState(false);
  const descriptionRef = React.createRef();

  return (
    <>
      <style jsx>
        {`
          .this :global(.descr-content) {
            max-height: 5rem;
            margin: 0 1rem 1rem;
            padding: 0.5rem 1rem 0.5rem;
            border: 1px solid #c8c8c8;
          }
          .this :global(.descr-scrolled-content) {
            max-height: 3.8rem;
            overflow-y: auto;
          }
          .this :global(.descr-header) {
            margin: 0 1rem 0;
            padding: 0.5rem 1rem 0.5rem;
            background-color: #eee;
            border: 1px solid #c8c8c8;
            font-weight: bold;
          }
          .this :global(.p-inplace-display) {
            padding: 0;
          }
        `}
      </style>
      <div className="this">
        <div className="descr-header">Description</div>
        <div className="descr-content">
          <div className="descr-scrolled-content">
            {readOnly ? (
              <div className="descriptionContent" ref={descriptionRef}>
                {value}
              </div>
            ) : (
              <Inplace active={inplaceActive} onToggle={() => setInplaceState(true)}>
                <InplaceDisplay>
                  <div className="descriptionContent" ref={descriptionRef}>
                    {value}
                  </div>
                </InplaceDisplay>
                <InplaceContent>
                  <TextAreaEditor
                    height={descriptionRef.current ? descriptionRef.current.clientHeight + 20 : 0}
                    value={value}
                    updateValue={(value) => {
                      updateValue(value);
                      setInplaceState(false);
                    }}
                  />
                </InplaceContent>
              </Inplace>
            )}
          </div>
        </div>
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
