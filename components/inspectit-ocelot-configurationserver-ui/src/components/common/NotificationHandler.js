import React from 'react';
import { connect } from 'react-redux';

import { Toast } from 'primereact/toast';

/** ID of the last show notification */
let lastNotificationId = -1;

/**
 * Handles showing of notification messages.
 */
class NotificationHandler extends React.Component {
  toast = React.createRef();

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
      this.toast.show(notification);
    }
  };

  render() {
    return (
      <>
        <Toast
          ref={(el) => {
            this.toast = el;
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
