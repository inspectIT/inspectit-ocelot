import React from 'react';
import { useSelector, useDispatch, shallowEqual } from 'react-redux';
import { promotionSelectors } from '../../../redux/ducks/promotion';
import { ToggleButton } from 'primereact/togglebutton';
import { promotionActions } from '../../../redux/ducks/promotion';

/** Toolbar for mappingsView for changing mappings filter, downloading config files, reloading & adding mappings */
const PromotionFileApproval = () => {
  const dispatch = useDispatch();

  const currentSelection = useSelector(promotionSelectors.getCurrentSelectionFile);
  const approved = currentSelection.approved;

  const updateFileApproval = (approved) => {
    if (approved) {
      dispatch(promotionActions.approveFile(currentSelection.file));
    } else {
      dispatch(promotionActions.disapproveFile(currentSelection.file));
    }
  };

  return (
    <>
      <style jsx>
        {`
          .this {
            border-top: 1px solid #ddd;
            display: flex;
            flex-direction: row-reverse;
            padding: 0.5rem 1rem;
          }
        `}
      </style>

      <div className="this">
        <ToggleButton
          onLabel="Approved"
          offLabel="Not Approved"
          onIcon="pi pi-check"
          offIcon="pi pi-times"
          checked={approved}
          onChange={(e) => updateFileApproval(e.value)}
        />
      </div>
    </>
  );
};

export default PromotionFileApproval;
