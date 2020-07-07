import React, { useState } from 'react';
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
      <style jsx>{`
        .this :global(.descr-content){
          max-height: 5rem;
          margin: 0 1rem 1rem;
          padding: 0.5rem 1rem 0.5rem;
          border: 1px solid #c8c8c8;
        }
        .this :global(.descr-scrolled-content){
          max-height: 3.8rem;
          overflow-y: auto;
        }
        .this :global(.descr-header){
          margin: 0 1rem 0;
          padding: 0.5rem 1rem 0.5rem;
          background-color: #eee;
          border: 1px solid #c8c8c8;
          font-weight: bold;
        }
        .this :global(.p-inplace-display){
          padding: 0;
        }
      `}
      </style>
      <div className="this">
        <div className="descr-header">Description</div>
        <div className="descr-content">
          <div className="descr-scrolled-content">
            {readOnly ? <SimpleDataView value={value} descriptionRef={descriptionRef} /> :
              <Inplace
                active={inplaceActive}
                onToggle={() => setInplaceState(true)}
              >
                <InplaceDisplay>
                  <SimpleDataView value={value} descriptionRef={descriptionRef} />
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
            }
          </div>
        </div>
      </div>
    </>
  );
};

const SimpleDataView = ({value, descriptionRef}) => {
  return (<div className="descriptionContent" ref={descriptionRef}>{value}</div>);
};

export default DescriptionSection;
