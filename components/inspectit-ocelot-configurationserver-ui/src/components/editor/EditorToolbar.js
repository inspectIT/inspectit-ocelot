import { Toolbar } from 'primereact/toolbar';
import { Button } from 'primereact/button';

/**
 * The toolbar used within the editor view.
 */
const EditorToolbar = ({ path, filename, icon, enableButtons, onSave, onSearch, onHelp }) => (
    <div className="this">
        <style jsx>{`
        .this :global(.p-toolbar) {
            background: 0;
            border: 0;
            border-radius: 0;
            background-color: #eee;
            border-bottom: 1px solid #ddd;
        }
        .p-toolbar-group-right > :global(*) {
            margin-left: .25rem;
        }
        .p-toolbar-group-left {
            font-size: 1rem;
            display: flex;
            align-items: center;
            height: 2rem;
        }
        .p-toolbar-group-left :global(.pi) {
            font-size: 1.75rem;
            color: #aaa;
            margin-right: 1rem;
        }
        .path {
            color: #999;
        }
        `}</style>
        <Toolbar>
            <div className="p-toolbar-group-left">
                {filename &&
                    <>
                        <i className={"pi " + icon}></i>
                        <div className="path">{path}</div>
                        <div className="name">{filename}</div>
                    </>
                }
            </div>
            <div className="p-toolbar-group-right">
                <Button disabled={!enableButtons} icon="pi pi-question" onClick={onHelp} />
                <Button disabled={!enableButtons} icon="pi pi-search" onClick={onSearch} />
                <Button disabled={!enableButtons} onClick={onSave} label="Save" icon="pi pi-save" />
            </div>
        </Toolbar>
    </div>
);

export default EditorToolbar;