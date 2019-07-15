import React, { Component } from 'react'
import { Card } from 'primereact/card';
import { InputText } from "primereact/inputtext";
import { Password } from 'primereact/password';
import { Button } from 'primereact/button';
import { Message } from 'primereact/message';

/**
 * The used header in the login card.
 */
const LoginCardHeader = (
    <div className="this">
        <style jsx>{`
        .this {
            text-align: center;
            padding: 1rem 0rem;
        }
        .ocelot-head {
            height: 9rem;
        }
        .text-ocelot {
            font-size: 1rem;
            margin-top: 1.25rem;
            font-weight: bold;
            color: #e8a034;
        }
        .text-server {
            font-size: 1.5rem;
        }
        `}</style>
        <img className="ocelot-head" src="/static/images/inspectit-ocelot.svg" />
        <div className="text-ocelot">inspectIT Ocelot</div>
        <div className="text-server">Configuration Server</div>
    </div>
)

/**
 * The login card which wrapes and handles the interaction in order to log in into the application.
 */
class LoginCard extends Component {

    state = {
        buttonDisabled: false,
        showSpinner: false,
        errorMessage: ""
    }

    doLogin = () => {
        this.setState({
            buttonDisabled: true,
            showSpinner: true
        });
    }

    onKeyPress = (e) => {
        if (!this.state.buttonDisabled && e.key === 'Enter') {
            this.doLogin();
            e.target.blur();
        }
    }

    render() {
        return (
            <Card className="loggin-card" header={LoginCardHeader}>
                <style global jsx>{`
                .loggin-card.p-card {
                    width: 25rem;
                    position: relative;
                    box-shadow: none;
                }
                .loggin-card .p-button, .loggin-card .p-inputtext, .loggin-card .p-message {
                    width: 100%;
                }
                `}</style>
                <style jsx>{`
                .input {
                    margin-top: 1rem;
                }
                .pi-spinner {
                    position: absolute;
                    top: 0.5rem;
                    right: 0.5rem;
                }
                `}</style>
                <div className="p-inputgroup input">
                    <span className="p-inputgroup-addon">
                        <i className="pi pi-user"></i>
                    </span>
                    <InputText placeholder="Username" onKeyPress={this.onKeyPress} />
                </div>
                <div className="p-inputgroup input">
                    <span className="p-inputgroup-addon">
                        <i className="pi pi-lock"></i>
                    </span>
                    <Password placeholder="Password" feedback={false} onKeyPress={this.onKeyPress} />
                </div>

                {this.state.errorMessage === "" ?
                    null
                    :
                    <div className="input">
                        <Message severity="error" text={this.state.errorMessage}></Message>
                    </div>
                }

                <div className="input">
                    <Button onClick={this.doLogin} disabled={this.state.buttonDisabled} label="Login" />
                </div>

                {this.state.showSpinner ? <i className="pi pi-spin pi-spinner"></i> : null}
            </Card>
        )
    }
}

export default LoginCard;