import React, { Component } from 'react'
import LoginCard from '../login/LoginCard'

class LoginView extends Component {
    render() {
        return (
            <div className="this">
                <style jsx>{`
                .this {
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    height: 100vh;
                }
                `}</style>
                {this.props.children}
                <LoginCard />
            </div>
        )
    }
}

export default LoginView;