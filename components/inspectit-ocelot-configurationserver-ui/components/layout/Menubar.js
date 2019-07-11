import { Toolbar } from 'primereact/toolbar';
import { Button } from 'primereact/button';

/**
 * The application's menu bar.
 */
const Menubar = () => {
    return (
        <Toolbar id="toolbar">
            <style global jsx>{`
            #toolbar {
                height: 4rem;
                padding: 0.5rem;
                border-radius: 0;
                border: 0;
                border-bottom: 1px solid #ccc;
            }
            `}</style>
            <style jsx>{`
            .flex-v-center {
                display: flex;
                align-items: center;
                height: 3rem;
            }
            .ocelot-head {
                height: 3rem;
                margin-right: 1rem;
            }
            .ocelot-text {
                font-size: 1rem;
                font-weight: bold;
                color: #eee;
            }
            `}</style>
            <div className="p-toolbar-group-left flex-v-center">
                <img className="ocelot-head" src="/static/images/inspectit-ocelot-head.svg" />
                <div className="ocelot-text">inspectIT Ocelot</div>
            </div>
            <div className="p-toolbar-group-right flex-v-center">
                <Button label="Logout" icon="pi pi-power-off" style={{ marginLeft: 4 }} />
            </div>
        </Toolbar>
    )
}

export default Menubar;