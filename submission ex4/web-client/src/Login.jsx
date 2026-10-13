import { useState } from 'react';
import { registerUser } from './api.js';

export default function Login({ onLoggedIn }) {
    const [name, setName] = useState('');
    const [error, setError] = useState('');
    const [busy, setBusy] = useState(false);

    async function handleSubmit(event) {
        event.preventDefault();
        const trimmed = name.trim();
        if (!trimmed) {
            setError('Enter a name.');
            return;
        }
        setBusy(true);
        setError('');
        try {
            await registerUser(trimmed);
            onLoggedIn(trimmed);
        } catch (ex) {
            setError(ex.message);
            setBusy(false);
        }
    }

    return (
        <div className="login-root">
            <form className="login-card" onSubmit={handleSubmit}>
                <h1 className="login-title">Guess Market</h1>
                <p className="login-subtitle">Enter a name to continue.</p>
                <input
                    id="login-name"
                    className="text-input"
                    placeholder="Your name"
                    value={name}
                    onChange={event => setName(event.target.value)}
                    autoFocus
                />
                <button className="primary-button" type="submit" disabled={busy}>Continue</button>
                <p className="login-error">{error}</p>
            </form>
        </div>
    );
}
