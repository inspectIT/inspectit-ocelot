import { Toolbar } from 'primereact/toolbar';
import { Button } from 'primereact/button';

/**
 * The toolbar used within the editor view.
 */
const EditorToolbar = ({ enableButtons, onSave, onSearch, onHelp, children }) => (
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
        `}</style>
        <Toolbar>
            <div className="p-toolbar-group-left">
                {children}
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