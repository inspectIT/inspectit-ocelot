import React from 'react';
import { connect } from 'react-redux';

import { Growl } from 'primereact/growl';

/** ID of the last show notification */
let lastNotificationId = -1;

/**
 * Handles showing of notification messages.
 */
class NotificationHandler extends React.Component {
  growl = React.createRef();

  componentDidMount = () => {
    this.showNotifications();
  };

  componentDidUpdate = () => {
    this.showNotifications();
  };

  showNotifications = () => {
    const { notification } = this.props;

    if (notification && notification.id !== lastNotificationId) {
      lastNotificationId = notification.id;
      this.growl.show(notification);
    }
  };

  render() {
    return (
      <>
        <Growl
          ref={(el) => {
            this.growl = el;
          }}
        />
        {this.props.children}
      </>
    );
  }
}

function mapStateToProps(state) {
  const { lastNotification } = state.notification;
  return {
    notification: lastNotification,
  };
}

export default connect(mapStateToProps, null)(NotificationHandler);
