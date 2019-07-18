import React from 'react'
import { connect } from 'react-redux'

import { Growl } from 'primereact/growl';

/** ID of the last show notification */
let lastNotification = -1;

/**
 * Handles showing of notification messages.
 */
class NotificationHandler extends React.Component {

    componentDidUpdate = () => {
        const { notifications } = this.props;

        if (notifications && notifications.length > 0) {
            let notification = notifications[0];            
            if (notification.id != lastNotification) {
                lastNotification = notification.id;
                this.growl.show(notifications);
            }
        }
    }

    render() {
        return (
            <>
                <Growl ref={(el) => { this.growl = el; }} />
                {this.props.children}
            </>
        );
    }
}

function mapStateToProps(state) {
    const { notifications } = state.notification;
    return {
        notifications
    }
}

export default connect(mapStateToProps, null)(NotificationHandler);