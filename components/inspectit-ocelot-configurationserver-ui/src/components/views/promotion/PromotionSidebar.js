import React, { useState } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { ListBox } from 'primereact/listbox';
import {Checkbox} from 'primereact/checkbox';
import classnames from 'classnames';
import { promotionActions } from '../../../redux/ducks/promotion';

const selectionTemplate = (option) => {
    const { file, type } = option;

    let classNames = classnames({
        'pi': true,
        'add': type === 'ADD',
        'modify': type === 'MODIFY',
        'remove': type === 'DELETE'
    });

    if (type === 'ADD') {
        classNames += ' pi-plus';
    } else if (type === 'MODIFY') {
        classNames += ' pi-pencil';
    } else if (type === 'DELETE') {
        classNames += ' pi-minus';
    } else {
        classNames += ' pi-question';
    }

    return (
        <>
        <style jsx>
        {`
        .add {
            color: #38ad38;
        }
        .modify {
            color: #ff9900;
        }
        .remove {
            color: #ff0000;
        }
        .item {
            display: flex;
            height: 1.5rem;
            align-items: center;
        }
        .label {
            margin-left: 1rem;
        }
        `}
        </style>

        <div className="p-clearfix item" key={file}>
            <i className={classNames }></i>
            <span className="label">{file}</span>
        </div>
        </>
    );
};

const PromotionSidebar = () => {
    const dispatch = useDispatch();

    const promotionFiles = useSelector(state => state.promotion.files);
    const currentSelection = useSelector(state => state.promotion.currentSelection);

    const setCurrentSelection = (file) => {
        dispatch(promotionActions.setCurrentSelection(file));
    }

    return (
        <>
            <style jsx>
            {`
            .this {
                height: 100%;
                overflow-y: auto;
            }
            .this :global(.p-listbox) {
                width: 20rem;
                height: 100%;
                border: none;
                border-right: 1px solid #dddddd;
            }
            .this :global(.p-listbox-list-wrapper) {
                border-radius: 0;
            }
            `}
            </style>

            <div className="this">
                <ListBox value={currentSelection} options={promotionFiles} onChange={(e) => setCurrentSelection(e.value)} itemTemplate={selectionTemplate} optionLabel="file" />
            </div>
        </>
    );
};

export default PromotionSidebar;