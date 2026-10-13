const BASE = '/guessmarket';

const UNREACHABLE = 'Cannot reach the Guess Market server. Make sure Tomcat is running on localhost:8080 with guessmarket.war deployed.';

// The server answers every request with JSON, errors included ({"error": "..."}). Anything else -
// no answer at all, an empty proxy error while Tomcat is down, or Tomcat's own HTML 404 page when
// the WAR isn't deployed - means the server itself isn't there, so say that instead of a raw code.
async function request(path, options) {
    let response;
    try {
        response = await fetch(BASE + path, options);
    } catch {
        throw new Error(UNREACHABLE);
    }
    const text = await response.text();
    let body;
    try {
        body = text ? JSON.parse(text) : null;
    } catch {
        throw new Error(UNREACHABLE);
    }
    if (!response.ok) {
        throw new Error(body?.error ?? UNREACHABLE);
    }
    return body;
}

// The browser allows only six connections to the server at once. Big background refreshes are
// marked low priority so the event you are looking at, and your own actions, always go first.
function get(path, priority = 'high') {
    return request(path, { priority });
}

function post(path, payload) {
    return request(path, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });
}

export function registerUser(name) {
    return post('/users/register', { name });
}

export function getAllUsers() {
    return get('/users', 'low');
}

export function getAllEvents() {
    return get('/events');
}

export function getEventDetails(eventName, priority = 'high') {
    return get('/events?name=' + encodeURIComponent(eventName), priority);
}

export function depositCash(userName, amount) {
    return post('/users/deposit', { userName, amount });
}

export function buyShares(memberName, eventName, optionIndex, quantity) {
    return post('/events/buy', { memberName, eventName, optionIndex, quantity });
}

export function submitOrder(userName, eventName, optionIndex, side, price, quantity) {
    return post('/events/order', { userName, eventName, optionIndex, side, price, quantity });
}

export function openEvent(mmName, eventName) {
    return post('/events/open', { mmName, eventName });
}

export function closeEvent(mmName, eventName, winningOptionIndex) {
    return post('/events/close', { mmName, eventName, winningOptionIndex });
}

export function createLmsrEvent(creatorName, name, description, commission, commissionType, optionNames, b) {
    return post('/events/create-lmsr', { creatorName, name, description, commission, commissionType, optionNames, b });
}

export function createOrderBookEvent(creatorName, name, description, commission, commissionType, optionNames, allowMint, initial, d) {
    return post('/events/create-orderbook', { creatorName, name, description, commission, commissionType, optionNames, allowMint, initial, d });
}

export function getChatMessages() {
    return get('/chat', 'low');
}

export function postChatMessage(userName, message) {
    return post('/chat', { userName, message });
}
