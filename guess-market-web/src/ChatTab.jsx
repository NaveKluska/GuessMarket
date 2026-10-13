import { Fragment, useEffect, useRef, useState } from 'react';
import { postChatMessage } from './api.js';
import { useApp } from './AppContext.js';
import { dateKey, dateLabel, timeOfDay } from './format.js';
import { nameColor } from './ui.jsx';

/** Everyone sees the same stream; a date chip separates days, WhatsApp-style, as in JavaFX. */
export default function ChatTab({ messages, refreshChat, active }) {
    const { userName, runAction } = useApp();
    const [draft, setDraft] = useState('');
    const listRef = useRef(null);
    const count = messages?.length ?? 0;

    // Messages are only ever appended, so a new count means something arrived - jump to it. Also
    // on opening the tab: a hidden list has no height, so a jump made while hidden goes nowhere.
    useEffect(() => {
        const list = listRef.current;
        if (list && active) {
            list.scrollTop = list.scrollHeight;
        }
    }, [count, active]);

    function handleSend(event) {
        event.preventDefault();
        const message = draft.trim();
        if (!message) {
            return;
        }
        runAction(() => postChatMessage(userName, message), () => {
            setDraft('');
            refreshChat();
        });
    }

    return (
        <div className="tab-body">
            <div className="chat-list" ref={listRef}>
                {messages === null && <div className="list-empty">Loading…</div>}
                {messages?.length === 0 && <div className="list-empty">No messages yet. Say hello!</div>}
                {messages?.map((message, index) => (
                    <Fragment key={index}>
                        {(index === 0 || dateKey(messages[index - 1].at) !== dateKey(message.at)) && (
                            <div className="date-divider"><span>{dateLabel(message.at)}</span></div>
                        )}
                        <div className="chat-row">
                            <span className="chat-time">{timeOfDay(message.at)}</span>
                            <span className="chat-user" style={{ color: nameColor(message.userName) }}>{message.userName}:</span>
                            <span className="chat-message-text">{message.message}</span>
                        </div>
                    </Fragment>
                ))}
            </div>
            <form className="chat-input-bar" onSubmit={handleSend}>
                <input id="chat-message" className="text-input chat-field" placeholder="Type a message..."
                       aria-label="Chat message" value={draft} onChange={event => setDraft(event.target.value)}/>
                <button type="submit" className="primary-button">Send</button>
            </form>
        </div>
    );
}
