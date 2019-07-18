import React from 'react'
import { connect } from 'react-redux'

import { Growl } from 'primereact/growl';

let lastNotification = -1;

class NotificationHandler extends React.Component {

    componentDidUpdate = () => {
        const { notifications } = this.props;

        if (notifications && notifications.length > 0) {
            let notification = notifications[0];
            console.log(notification.id, lastNotification);
            
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