import { Card } from 'primereact/card';
import { InputText } from "primereact/inputtext";
import { Password } from 'primereact/password';
import { Button } from 'primereact/button';
import { Message } from 'primereact/message';

const LoginCardHeader = (
    <div className="this">
        <style jsx>{`
        .this {
            text-align: center;
            padding: 1rem 0rem;
        }
        .ocelot-head {
            width: 50%;
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

const LoginCard = () => {
    return (
        <Card className="loggin-card" header={LoginCardHeader}>
            <style global jsx>{`
            .loggin-card.p-card {
                width: 20rem;
                position: relative;
            }
            .loggin-card .p-button, .loggin-card .p-inputtext, .loggin-card .p-message {
                width: 100%;
            }
            `}</style>
            <style jsx>{`
            .input {
                margin-top: 0.5rem;
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
                <InputText placeholder="Username" />
            </div>
            <div className="p-inputgroup input">
                <span className="p-inputgroup-addon">
                    <i className="pi pi-lock"></i>
                </span>
                <Password placeholder="Password" feedback={false} />
            </div>

            <div className="input">
                <Message severity="error" text="Wrong password."></Message>
            </div>

            <div className="input">
                <Button label="Login" />
            </div>

            <i className="pi pi-spin pi-spinner"></i>
        </Card>
    )
}

export default LoginCard;