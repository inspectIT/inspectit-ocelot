import React from 'react';
import UserSettingsMenu from './UserSettingsMenu';

/**
 * The User Settings View Wrapper, includes the Tab Menu.
 */
const UserSettingsView = (props) => {
    return (
        <div>
            <style jsx>{`
                .content {
                margin-top: 3rem;
                overflow: auto auto;
                }
            `}</style>

            <UserSettingsMenu />

            <div className='content'>
                {props.children}
            </div>
        </div>
    )
}

export default UserSettingsView;