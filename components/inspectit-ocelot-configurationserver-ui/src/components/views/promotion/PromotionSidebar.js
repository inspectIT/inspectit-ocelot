import React from 'react';
import { ListBox } from 'primereact/listbox';
import classnames from 'classnames';

/**
 * Item template for a promotion file in the sidebar.
 *
 * @param {object} option current list option
 */
const selectionTemplate = ({ file, type, authors, isApproved, canSelfApprove, currentUser, canPromote }) => {
  const iconClassNames = classnames('pi', {
    green: type === 'ADD',
    yellow: type === 'MODIFY',
    red: type === 'DELETE',
    'pi-plus': type === 'ADD',
    'pi-pencil': type === 'MODIFY',
    'pi-minus': type === 'DELETE',
  });

  const itemClassNames = classnames('p-clearfix', 'item', {
    approved: isApproved,
  });

  const preventApproval = !canPromote || (!canSelfApprove && authors.includes(currentUser));

  return (
    <>
      <style jsx>
        {`
          .green {
            color: #38ad38;
          }
          .yellow {
            color: #ff9900;
          }
          .red {
            color: #ff0000;
          }
          .item {
            padding: 0.5rem 0.75rem;
          }
          .item-wrapper {
            display: flex;
            height: 1.5rem;
            align-items: center;
          }
          .label {
            margin-left: 1rem;
            flex-grow: 1;
          }
          .approved {
            background-color: #dff1df;
          }
          :global(.p-highlight) .approved {
            background-color: inherit;
          }
          .pi-lock {
            color: #9e9e9e;
          }
        `}
      </style>

      <div className={itemClassNames} key={file}>
        <div className="item-wrapper">
          <i className={iconClassNames}></i>
          <span className="label">{file}</span>
          {isApproved && <i className="pi pi-check-circle green"></i>}
          {preventApproval && <i className="pi pi-lock"></i>}
        </div>
      </div>
    </>
  );
};

/**
 * Sidebar for showing existing configuration promotion files.
 */
const PromotionSidebar = ({ selection, onSelectionChange, promotionFiles, updateDate }) => {
  return (
    <>
      <style jsx>
        {`
          .this {
            height: 100%;
            overflow-y: auto;
            display: flex;
            flex-direction: column;
            border-right: 1px solid #dddddd;
          }
          .this :global(.p-listbox) {
            width: 22rem;
            height: 100%;
            border: none;
          }
          .this :global(.p-listbox-list-wrapper) {
            border-radius: 0;
          }
          .this :global(.p-listbox .p-listbox-list .p-listbox-item) {
            padding: 0;
          }
          .information {
            color: #aaa;
            font-size: 0.75rem;
            text-align: center;
            padding: 0.25rem 0;
          }
          .title {
            font-family: monospace;
            color: #212529;
            font-size: 1rem;
            padding: 1rem;
            border-bottom: 1px solid #eeeeee;
            line-height: 1rem; /** In order to match the DiffView */
            padding: 0.9rem;
          }
        `}
      </style>

      <div className="this">
        <div className="title">Modified Configurations</div>
        <ListBox
          value={selection}
          options={promotionFiles}
          onChange={(e) => onSelectionChange(e.value ? e.value.file : null)}
          itemTemplate={selectionTemplate}
          optionLabel="file"
          dataKey="file"
        />
        <div className="information">Last Refresh: {updateDate ? new Date(updateDate).toLocaleString() : '-'}</div>
      </div>
    </>
  );
};

export default PromotionSidebar;
