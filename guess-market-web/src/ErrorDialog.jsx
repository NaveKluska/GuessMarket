import { useEffect, useRef } from 'react';

/** Same look as the JavaFX client's error window: red "!" badge, red header, message, OK. */
export default function ErrorDialog({ message, onClose }) {
    const okRef = useRef(null);

    useEffect(() => {
        okRef.current?.focus();
        const onKey = event => {
            if (event.key === 'Escape' || event.key === 'Enter') onClose();
        };
        window.addEventListener('keydown', onKey);
        return () => window.removeEventListener('keydown', onKey);
    }, [onClose]);

    return (
        <div className="modal-backdrop" onClick={onClose}>
            <div className="error-dialog" role="alertdialog" aria-labelledby="error-title" onClick={event => event.stopPropagation()}>
                <div className="error-dialog-top">
                    <div className="error-dialog-icon" aria-hidden="true">!</div>
                    <div>
                        <div id="error-title" className="error-dialog-header">Something went wrong</div>
                        <div className="error-dialog-message">{message}</div>
                    </div>
                </div>
                <hr className="separator"/>
                <div className="error-dialog-buttons">
                    <button ref={okRef} className="primary-button" onClick={onClose}>OK</button>
                </div>
            </div>
        </div>
    );
}
