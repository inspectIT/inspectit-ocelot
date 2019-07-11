import React, { Component } from 'react'
import LoginCard from '../login/LoginCard'

const LoginView = (props) => {
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
            {props.children}
            <LoginCard />
        </div>
    )
}

export default LoginView;