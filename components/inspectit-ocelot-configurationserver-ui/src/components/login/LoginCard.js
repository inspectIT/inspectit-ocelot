import React, { Component } from 'react';
import LoginCardHeader from './LoginCardHeader';
import { InputText } from "primereact/inputtext";
import { Password } from 'primereact/password';
import { Button } from 'primereact/button';
import { Message } from 'primereact/message';

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
            showSpinner: true,
            errorMessage: "error-message"
        });
    }

    onKeyPress = (e) => {
        if (!this.state.buttonDisabled && e.key === 'Enter') {
            this.doLogin();
            e.target.blur();
        }
    }

    render() {
        const fullWidthStyle = { width: "100%" }

        return (
            <div className="this">
                <style jsx>{`
                .this {
                    width: 25rem;
                    position: relative;
                    box-shadow: none;
                }
                .input {
                    margin-top: 1rem;
                }
                .pi-spinner {
                    position: absolute;
                    top: 0.5rem;
                    right: 0.5rem;
                }
                `}</style>
                <LoginCardHeader />
                <div className="p-inputgroup input">
                    <span className="p-inputgroup-addon">
                        <i className="pi pi-user"></i>
                    </span>
                    <InputText style={fullWidthStyle} placeholder="Username" onKeyPress={this.onKeyPress} />
                </div>
                <div className="p-inputgroup input">
                    <span className="p-inputgroup-addon">
                        <i className="pi pi-lock"></i>
                    </span>
                    <Password style={fullWidthStyle} placeholder="Password" feedback={false} onKeyPress={this.onKeyPress} />
                </div>

                {this.state.errorMessage && 
                    <div className="input">
                        <Message style={fullWidthStyle} severity="error" text={this.state.errorMessage}></Message>
                    </div>
                }

                <div className="input">
                    <Button style={fullWidthStyle} onClick={this.doLogin} disabled={this.state.buttonDisabled} label="Login" />
                </div>

                {this.state.showSpinner && <i className="pi pi-spin pi-spinner"></i>}
            </div>
        )
    }
}

export default LoginCard;