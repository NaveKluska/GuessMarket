import { useCallback, useState } from 'react';
import Login from './Login.jsx';
import EventsTab from './EventsTab.jsx';
import AccountTab from './AccountTab.jsx';
import ChatTab from './ChatTab.jsx';
import ErrorDialog from './ErrorDialog.jsx';
import { AppContext } from './AppContext.js';
import { getAllEvents, getAllUsers, getChatMessages } from './api.js';
import { usePoll } from './usePoll.js';

const SESSION_KEY = 'guessmarket.userName';
const TABS = ['Events', 'Account', 'Chat'];

export default function App() {
    // Kept in sessionStorage so a page refresh doesn't log the user out: re-registering the same
    // name would be rejected as already taken, leaving no way back in.
    const [userName, setUserName] = useState(() => sessionStorage.getItem(SESSION_KEY));

    function handleLoggedIn(name) {
        sessionStorage.setItem(SESSION_KEY, name);
        setUserName(name);
    }

    if (!userName) {
        return <Login onLoggedIn={handleLoggedIn}/>;
    }
    return <MainScreen userName={userName}/>;
}

function MainScreen({ userName }) {
    const [tab, setTab] = useState('Events');
    const [errorMessage, setErrorMessage] = useState(null);
    const events = usePoll(getAllEvents, []);
    const users = usePoll(getAllUsers, []);
    const chat = usePoll(getChatMessages, []);

    // An action changes balances and events everywhere, so refresh both right away instead of
    // leaving up to a second of stale numbers until the next poll tick.
    const refreshShared = () => {
        events.refresh();
        users.refresh();
    };

    async function runAction(work, afterSuccess) {
        try {
            await work();
            afterSuccess?.();
        } catch (ex) {
            setErrorMessage(ex.message);
        }
    }

    const closeError = useCallback(() => setErrorMessage(null), []);
    const serverDown = events.error || users.error || chat.error;

    return (
        <AppContext.Provider value={{ userName, runAction }}>
            <div className="app-root">
                <header className="toolbar">
                    <span className="welcome">Logged in as <strong>{userName}</strong></span>
                    <span className="brand-title">Guess Market</span>
                    <span className={'connection' + (serverDown ? ' connection-lost' : '')}>
                        {serverDown ? 'Server unreachable - retrying…' : ''}
                    </span>
                </header>

                <nav className="tabs" role="tablist">
                    {TABS.map(name => (
                        <button
                            key={name}
                            role="tab"
                            aria-selected={tab === name}
                            className={'tab' + (tab === name ? ' tab-selected' : '')}
                            onClick={() => setTab(name)}
                        >
                            {name}
                        </button>
                    ))}
                </nav>

                {/* Both tabs stay mounted and are only hidden, so each keeps its own selection and
                    scroll position when you switch away and back - same as JavaFX's TabPane. */}
                <div className="tab-panel" hidden={tab !== 'Events'}>
                    <EventsTab events={events.data} active={tab === 'Events'}/>
                </div>
                <div className="tab-panel" hidden={tab !== 'Account'}>
                    <AccountTab users={users.data} events={events.data} refreshShared={refreshShared} active={tab === 'Account'}/>
                </div>
                <div className="tab-panel" hidden={tab !== 'Chat'}>
                    <ChatTab messages={chat.data} refreshChat={chat.refresh} active={tab === 'Chat'}/>
                </div>
            </div>
            {errorMessage && <ErrorDialog message={errorMessage} onClose={closeError}/>}
        </AppContext.Provider>
    );
}
