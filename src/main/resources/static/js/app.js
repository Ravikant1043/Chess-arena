"use strict";

/**
 * Chess Arena front-end.
 *
 * Responsibilities:
 *  - Auth (register/login) against the REST API; JWT stored in localStorage.
 *  - One STOMP-over-WebSocket connection authenticated with that JWT.
 *  - Lobby: online players, "Find Opponent", incoming challenges.
 *  - Game: render the server's board, click-to-move, live updates, game over.
 *
 * The server is authoritative — the client sends move intents and renders whatever
 * board state the server broadcasts back.
 */

const PIECE_GLYPH = { K: "♚", Q: "♛", R: "♜", B: "♝", N: "♞", P: "♟" };

const state = {
    token: null,
    username: null,
    stomp: null,
    gameId: null,
    myColor: null,      // "WHITE" | "BLACK"
    orientation: "WHITE",
    selected: null,     // algebraic square currently selected
    lastMove: null,     // {from, to}
    gameSub: null,
    boardCells: {},     // algebraic square -> cell element
    whiteName: null,
    blackName: null,
    opponent: null,
};

// ------------------------------------------------------------------ DOM helpers
const $ = (id) => document.getElementById(id);
const show = (el) => el.classList.remove("hidden");
const hide = (el) => el.classList.add("hidden");

// ================================================================== AUTH

let authMode = "login";

function setAuthMode(mode) {
    authMode = mode;
    $("tab-login").classList.toggle("active", mode === "login");
    $("tab-register").classList.toggle("active", mode === "register");
    $("auth-submit").textContent = mode === "login" ? "Log in" : "Register";
    $("auth-msg").innerHTML = "";
}

async function submitAuth(e) {
    e.preventDefault();
    const username = $("username").value.trim();
    const password = $("password").value;
    const endpoint = authMode === "login" ? "/api/auth/login" : "/api/auth/register";

    try {
        const res = await fetch(endpoint, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username, password }),
        });
        const data = await res.json();
        if (!res.ok) {
            showAuthMsg(data.error || "Something went wrong", "error");
            return;
        }
        onAuthenticated(data.token, data.username);
    } catch (err) {
        showAuthMsg("Network error: " + err.message, "error");
    }
}

function showAuthMsg(text, kind) {
    $("auth-msg").innerHTML = `<div class="msg ${kind}">${text}</div>`;
}

function onAuthenticated(token, username) {
    state.token = token;
    state.username = username;
    localStorage.setItem("chess.token", token);
    localStorage.setItem("chess.username", username);
    enterApp();
}

function logout() {
    if (state.stomp) state.stomp.deactivate();
    localStorage.removeItem("chess.token");
    localStorage.removeItem("chess.username");
    location.reload();
}

// ================================================================== APP / WS

function enterApp() {
    hide($("auth-view"));
    show($("app-view"));
    $("me-name").textContent = state.username;
    connectWebSocket();
    refreshProfile();
    refreshLeaderboard();
}

function connectWebSocket() {
    const client = new StompJs.Client({
        webSocketFactory: () => new SockJS("/ws"),
        connectHeaders: { Authorization: "Bearer " + state.token },
        reconnectDelay: 3000,
        onConnect: () => {
            state.stomp = client;
            client.subscribe("/topic/lobby", (msg) => handleLobbyMessage(JSON.parse(msg.body)));
            client.subscribe("/user/queue/game", (msg) => handlePersonalMessage(JSON.parse(msg.body)));
            $("lobby-status").textContent = "Connected. Click \"Find Opponent\" to start.";
        },
        onStompError: (frame) => {
            $("lobby-status").textContent = "Connection error: " + frame.headers["message"];
        },
    });
    state.stomp = client;
    client.activate();
}

// ------------------------------------------------------------- lobby messages

function handleLobbyMessage(msg) {
    switch (msg.type) {
        case "ONLINE_PLAYERS": renderOnlinePlayers(msg.players); break;
        case "CHALLENGE": addChallenge(msg); break;
        case "CHALLENGE_CLOSED": removeChallenge(msg.challengeId); break;
    }
}

function renderOnlinePlayers(players) {
    const ul = $("online-list");
    ul.innerHTML = "";
    players.forEach((p) => {
        const li = document.createElement("li");
        const tag = p.username === state.username ? ' <span class="tag">(you)</span>'
                  : p.inGame ? ' <span class="tag">in game</span>' : "";
        li.innerHTML = `<span>${p.username}${tag}</span><span class="score">${p.score}</span>`;
        ul.appendChild(li);
    });
}

function addChallenge(msg) {
    if (msg.challenger === state.username) return; // don't show my own challenge to me
    if ($("challenge-" + msg.challengeId)) return;
    const div = document.createElement("div");
    div.className = "challenge-card";
    div.id = "challenge-" + msg.challengeId;
    div.innerHTML = `<span><b>${msg.challenger}</b> wants to play</span>`;
    const btn = document.createElement("button");
    btn.textContent = "Accept";
    btn.onclick = () => acceptChallenge(msg.challengeId);
    div.appendChild(btn);
    $("challenges").appendChild(div);
}

function removeChallenge(challengeId) {
    const el = $("challenge-" + challengeId);
    if (el) el.remove();
}

function findOpponent() {
    state.stomp.publish({ destination: "/app/lobby/find-opponent" });
    $("lobby-status").textContent = "Searching for an opponent… waiting for someone to accept.";
    $("find-btn").disabled = true;
}

function acceptChallenge(challengeId) {
    state.stomp.publish({
        destination: "/app/lobby/accept",
        body: JSON.stringify({ challengeId }),
    });
}

// ---------------------------------------------------------- personal messages

function handlePersonalMessage(msg) {
    switch (msg.type) {
        case "GAME_START": startGame(msg); break;
        case "TARGETS": showTargets(msg.square, msg.targets); break;
        case "ERROR": $("lobby-status").textContent = "⚠ " + msg.message; $("find-btn").disabled = false; break;
    }
}

// ================================================================== GAME

function startGame(msg) {
    state.gameId = msg.gameId;
    state.myColor = msg.yourColor;
    state.orientation = msg.yourColor;
    state.selected = null;
    state.lastMove = null;
    state.whiteName = msg.white;
    state.blackName = msg.black;
    state.opponent = msg.opponent;

    hide($("lobby-view"));
    show($("game-view"));
    hide($("back-lobby-btn"));
    show($("resign-btn"));
    $("game-banner").innerHTML = "";
    $("chat-messages").innerHTML = "";
    appendSystemMsg(`Game started — <b>${msg.white}</b> (White) vs <b>${msg.black}</b> (Black). Good luck!`);
    $("game-players").textContent = `♔ ${msg.white}  vs  ♚ ${msg.black}  —  you are ${msg.yourColor}`;

    // Subscribe to this game's broadcast channel.
    if (state.gameSub) state.gameSub.unsubscribe();
    state.gameSub = state.stomp.subscribe("/topic/game/" + msg.gameId,
        (m) => handleGameMessage(JSON.parse(m.body)));

    // Render the initial position straight from GAME_START — the first GAME_STATE
    // broadcast may have fired before our subscription was established.
    buildEmptyBoard();
    if (msg.board) renderBoard(msg.board);
    $("game-turn").textContent = `${msg.sideToMove || "WHITE"} to move`;
}

function handleGameMessage(msg) {
    switch (msg.type) {
        case "GAME_STATE":
            renderBoard(msg.board);
            updateTurn(msg);
            if (msg.lastMove) highlightLastMove(msg.lastMove);
            if (msg.lastMoveText) announceMove(msg);
            break;
        case "CHAT": appendChatMsg(msg.sender, msg.text); break;
        case "GAME_OVER": renderBoard(msg.board); showGameOver(msg); break;
    }
}

/** Posts "♞ bob: Knight to c3" style move announcements into the chat. */
function announceMove(msg) {
    // The mover is whoever is NOT the side to move now.
    const mover = msg.sideToMove === "WHITE" ? state.blackName : state.whiteName;
    appendSystemMsg(`<b>${mover}</b>: ${msg.lastMoveText}`);
}

function updateTurn(msg) {
    if (msg.status === "CHECK") {
        $("game-turn").textContent = `${msg.sideToMove} to move — CHECK!`;
    } else {
        $("game-turn").textContent = `${msg.sideToMove} to move`;
    }
}

// ------------------------------------------------------------- board rendering

function orderedRanks() {
    // board[0] is rank 8 ... board[7] is rank 1. White sees rank 8 at top;
    // Black sees it flipped.
    return state.orientation === "WHITE"
        ? [0, 1, 2, 3, 4, 5, 6, 7]
        : [7, 6, 5, 4, 3, 2, 1, 0];
}

function orderedFiles() {
    return state.orientation === "WHITE"
        ? [0, 1, 2, 3, 4, 5, 6, 7]
        : [7, 6, 5, 4, 3, 2, 1, 0];
}

function squareName(rowIdx, fileIdx) {
    // rowIdx 0 => rank 8. fileIdx 0 => file a.
    return String.fromCharCode(97 + fileIdx) + (8 - rowIdx);
}

function buildEmptyBoard() {
    const board = $("board");
    board.innerHTML = "";
    state.boardCells = {};
    for (const r of orderedRanks()) {
        for (const f of orderedFiles()) {
            const sq = squareName(r, f);
            const cell = document.createElement("div");
            const dark = (r + f) % 2 === 1;
            cell.className = "square " + (dark ? "dark" : "light");
            cell.dataset.square = sq;
            cell.onclick = () => onSquareClick(sq);
            board.appendChild(cell);
            state.boardCells[sq] = cell;
        }
    }
}

function renderBoard(grid) {
    for (const r of [0, 1, 2, 3, 4, 5, 6, 7]) {
        for (const f of [0, 1, 2, 3, 4, 5, 6, 7]) {
            const sq = squareName(r, f);
            const cell = state.boardCells[sq];
            if (!cell) continue;
            const code = grid[r][f]; // e.g. "wP" or null
            if (code) {
                const color = code[0] === "w" ? "white" : "black";
                cell.innerHTML = `<span class="piece ${color}">${PIECE_GLYPH[code[1]]}</span>`;
            } else {
                cell.innerHTML = "";
            }
            cell.classList.remove("selected");
        }
    }
}

function highlightLastMove(uci) {
    Object.values(state.boardCells).forEach((c) => c.classList.remove("last-move"));
    const from = uci.substring(0, 2), to = uci.substring(2, 4);
    if (state.boardCells[from]) state.boardCells[from].classList.add("last-move");
    if (state.boardCells[to]) state.boardCells[to].classList.add("last-move");
}

function onSquareClick(sq) {
    if (!state.gameId) return;

    if (state.selected === null) {
        // First click: must land on a piece of my color.
        const cell = state.boardCells[sq];
        const piece = cell.querySelector(".piece");
        if (!piece) return;
        const isWhite = piece.classList.contains("white");
        if ((state.myColor === "WHITE") !== isWhite) return; // not my piece
        state.selected = sq;
        cell.classList.add("selected");
        requestTargets(sq); // ask the server which squares this piece can go to
    } else {
        if (sq === state.selected) { // deselect
            state.boardCells[sq].classList.remove("selected");
            state.selected = null;
            clearTargets();
            return;
        }
        // Clicking another of my own pieces switches the selection.
        const cell = state.boardCells[sq];
        const piece = cell.querySelector(".piece");
        if (piece) {
            const isWhite = piece.classList.contains("white");
            if ((state.myColor === "WHITE") === isWhite) {
                state.boardCells[state.selected].classList.remove("selected");
                clearTargets();
                state.selected = sq;
                cell.classList.add("selected");
                requestTargets(sq);
                return;
            }
        }
        sendMove(state.selected, sq);
        if (state.boardCells[state.selected]) state.boardCells[state.selected].classList.remove("selected");
        state.selected = null;
        clearTargets();
    }
}

// ------------------------------------------------------- move highlighting

function requestTargets(square) {
    state.stomp.publish({
        destination: "/app/game/targets",
        body: JSON.stringify({ gameId: state.gameId, square }),
    });
}

function showTargets(square, targets) {
    if (square !== state.selected) return; // stale reply — selection changed
    clearTargets();
    targets.forEach((sq) => {
        if (state.boardCells[sq]) state.boardCells[sq].classList.add("target");
    });
}

function clearTargets() {
    Object.values(state.boardCells).forEach((c) => c.classList.remove("target"));
}

function sendMove(from, to) {
    let uci = from + to;
    // Auto-queen: if a pawn reaches the last rank, append promotion piece.
    const cell = state.boardCells[from];
    const piece = cell && cell.querySelector(".piece");
    const toRank = to[1];
    if (piece && piece.textContent === PIECE_GLYPH.P && (toRank === "8" || toRank === "1")) {
        uci += "q";
    }
    state.stomp.publish({
        destination: "/app/game/move",
        body: JSON.stringify({ gameId: state.gameId, uci }),
    });
}

// ------------------------------------------------------------------- chat

const escapeHtml = (s) => s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");

/** A player chat bubble: me on the LEFT, opponent on the RIGHT (with avatar). */
function appendChatMsg(sender, text) {
    const mine = sender === state.username;
    const row = document.createElement("div");
    row.className = "chat-row " + (mine ? "mine" : "theirs");

    const avatar = document.createElement("div");
    avatar.className = "chat-avatar";
    avatar.textContent = sender.charAt(0).toUpperCase();
    avatar.title = sender;

    const bubble = document.createElement("div");
    bubble.className = "chat-bubble";
    bubble.innerHTML = `<span class="sender">${escapeHtml(sender)}</span>${escapeHtml(text)}`;

    row.appendChild(avatar);
    row.appendChild(bubble);
    $("chat-messages").appendChild(row);
    scrollChat();
}

/** A centered system line (move announcements, game events). Accepts trusted HTML. */
function appendSystemMsg(html) {
    const div = document.createElement("div");
    div.className = "chat-system";
    div.innerHTML = html;
    $("chat-messages").appendChild(div);
    scrollChat();
}

function scrollChat() {
    const box = $("chat-messages");
    box.scrollTop = box.scrollHeight;
}

function sendChat(e) {
    e.preventDefault();
    const input = $("chat-input");
    const text = input.value.trim();
    if (!text || !state.gameId || !state.stomp) return;
    state.stomp.publish({
        destination: "/app/game/chat",
        body: JSON.stringify({ gameId: state.gameId, text }),
    });
    input.value = "";
    input.focus();
}

function showGameOver(msg) {
    let text, cls = "banner";
    if (msg.endReason === "STALEMATE" || msg.endReason === "DRAW") {
        text = "Draw — " + msg.endReason.toLowerCase();
    } else if (msg.winner === state.username) {
        text = "You won! (" + msg.endReason.toLowerCase() + ")";
    } else {
        text = "You lost — " + msg.endReason.toLowerCase();
        cls = "banner loss";
    }
    $("game-banner").innerHTML = `<div class="${cls}">${text}</div>`;
    appendSystemMsg(`🏁 Game over — ${escapeHtml(text)}`);
    hide($("resign-btn"));
    show($("back-lobby-btn"));
    refreshProfile();
    refreshLeaderboard();
}

function resign() {
    if (!confirm("Resign this game?")) return;
    state.stomp.publish({
        destination: "/app/game/resign",
        body: JSON.stringify({ gameId: state.gameId }),
    });
}

function backToLobby() {
    if (state.gameSub) { state.gameSub.unsubscribe(); state.gameSub = null; }
    state.gameId = null;
    hide($("game-view"));
    show($("lobby-view"));
    $("find-btn").disabled = false;
    $("lobby-status").textContent = "Click \"Find Opponent\" to start another game.";
}

// ================================================================== REST helpers

async function apiGet(path) {
    const res = await fetch(path, { headers: { Authorization: "Bearer " + state.token } });
    if (!res.ok) return null;
    return res.json();
}

async function refreshProfile() {
    const me = await apiGet("/api/me");
    if (me) $("me-score").textContent = me.score;
}

async function refreshLeaderboard() {
    const board = await apiGet("/api/leaderboard");
    if (!board) return;
    const ul = $("leaderboard");
    ul.innerHTML = "";
    board.forEach((e) => {
        const li = document.createElement("li");
        li.innerHTML = `<span>${e.rank}. ${e.username}</span><span class="score">${e.score}</span>`;
        ul.appendChild(li);
    });
}

// ================================================================== bootstrap

function init() {
    $("tab-login").onclick = () => setAuthMode("login");
    $("tab-register").onclick = () => setAuthMode("register");
    $("auth-form").onsubmit = submitAuth;
    $("logout-btn").onclick = logout;
    $("find-btn").onclick = findOpponent;
    $("resign-btn").onclick = resign;
    $("back-lobby-btn").onclick = backToLobby;
    $("chat-form").onsubmit = sendChat;

    // Auto-login if a token is already stored.
    const token = localStorage.getItem("chess.token");
    const username = localStorage.getItem("chess.username");
    if (token && username) {
        state.token = token;
        state.username = username;
        enterApp();
    }
}

document.addEventListener("DOMContentLoaded", init);
