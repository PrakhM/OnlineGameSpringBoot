const TILE = 32;
const MAP_W = 20;
const MAP_H = 15;
const SCALE = 1.5;
const STILE = TILE * SCALE;
const playerImg = new Image();
playerImg.src = '/images/player.png';
const playerLinesImg = new Image();
playerLinesImg.src = '/images/playerLines.png';

const MAP = [
    [1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1],
    [1,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,1],
    [1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1],
    [1,0,0,1,1,0,0,0,0,1,1,0,0,0,1,0,0,0,0,1],
    [1,0,0,1,1,0,0,0,0,1,1,0,0,0,1,0,0,0,0,1],
    [1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,0,1],
    [1,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,2,2,0,1],
    [1,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,2,2,0,1],
    [1,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,1],
    [1,0,0,1,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,1],
    [1,0,0,1,0,0,0,0,0,0,0,0,0,1,1,1,0,0,0,1],
    [1,0,0,0,0,0,0,0,0,0,0,0,0,1,0,1,0,0,0,1],
    [1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1],
    [1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1],
    [1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1],
];

const canvas = document.getElementById('gameCanvas');
canvas.width  = MAP_W * STILE;
canvas.height = MAP_H * STILE;
const ctx = canvas.getContext('2d');
ctx.imageSmoothingEnabled = false;

const offscreen = document.createElement('canvas');
offscreen.width = STILE;
offscreen.height = STILE;
const off = offscreen.getContext('2d');
off.imageSmoothingEnabled = false;

let stompClient = null;
let username = null;
let players = {};

const TILE_IMGS = { 0: new Image(), 1: new Image(), 2: new Image() };
TILE_IMGS[0].src = '/images/grass.png';
TILE_IMGS[1].src = '/images/tree.png';
TILE_IMGS[2].src = '/images/water.png';

function drawMap() {
    for (let row = 0; row < MAP_H; row++) {
        for (let col = 0; col < MAP_W; col++) {
            const t = MAP[row][col];
            ctx.drawImage(TILE_IMGS[t], col * STILE, row * STILE, STILE, STILE);
        }
    }
}

function drawPlayers() {
    Object.values(players).forEach(p => {
        const px = p.x * STILE;
        const py = p.y * STILE;

        off.drawImage(playerImg, 0, 0, STILE, STILE);

        off.globalCompositeOperation = 'source-atop';
        off.globalAlpha = 0.8;
        off.fillStyle = p.colour;
        off.fillRect(0, 0, STILE, STILE);

        ctx.drawImage(offscreen, px, py);
        ctx.drawImage(playerLinesImg, px, py, STILE, STILE);

        ctx.font = 'bold 12px "Press Start 2P"';
        ctx.textAlign = 'center';
        const nameWidth = ctx.measureText(p.tag).width;
        ctx.fillStyle = 'rgba(0,0,0,0.6)';
        ctx.fillRect(px + STILE/2 - nameWidth/2 - 3, py - STILE*0.4, nameWidth + 6, 13);
        ctx.fillStyle = p.colour;
        ctx.fillText(p.tag, px + STILE/2, py - STILE*0.12);
    });
}

function render() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    drawMap();
    drawPlayers();
}

function updatePlayerList() {
    const list = document.getElementById('player-list');
    list.innerHTML = Object.values(players).map(p =>
        `<div class="player-entry" style="color:${p.colour}">${p.tag}</div>`
    ).join('');
}

function appendChat(msg) {
    const box = document.getElementById('chat-messages');
    const div = document.createElement('div');

    if (msg.type === 'JOIN' || msg.type === 'LEAVE') {
        div.className = msg.type === 'JOIN' ? 'msg-join' : 'msg-leave';
        div.textContent = msg.content;
    } else {
        div.className = 'msg-chat';
        const senderColor = players[msg.sender]?.colour || '#f39c12';
        div.innerHTML = `<span class="sender" style="color:${senderColor}">${msg.sender}:</span> ${msg.content}`;
    }

    box.appendChild(div);
    box.scrollTop = box.scrollHeight;
}

function sendChat() {
    const input = document.getElementById('chat-input');
    const text = input.value.trim();
    if (!text || !stompClient) return;
    stompClient.send('/app/chat.send', {}, JSON.stringify({
        sender: username,
        content: text,
        type: 'CHAT'
    }));
    input.value = '';
}

function connect() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null;

    stompClient.connect({}, function() {
        // Subscribe to player updates
        stompClient.subscribe('/topic/players', function(payload) {
            const list = JSON.parse(payload.body);
            players = {};
            list.forEach(p => players[p.tag] = p);
            render();
            updatePlayerList();
        });

        // Subscribe to chat
        stompClient.subscribe('/topic/chat', function(payload) {
            appendChat(JSON.parse(payload.body));
        });

        // Register this player
        stompClient.send('/app/player.addPlayer', {}, JSON.stringify({
            tag: username,
            x: 2, y: 2
        }));

        // Announce join in chat
        stompClient.send('/app/chat.join', {}, JSON.stringify({
            sender: username,
            content: '',
            type: 'JOIN'
        }));
    });
}

const KEY_DIR = {
    'ArrowUp': 0, 'w': 0, 'W': 0,
    'ArrowLeft': 1, 'a': 1, 'A': 1,
    'ArrowDown': 2, 's': 2, 'S': 2,
    'ArrowRight': 3, 'd': 3, 'D': 3,
};

let lastMove = 0;
const MOVE_DELAY = 150;
lastMoves = {0 : 0, 1 : 0, 2 : 0, 3 : 0};

document.addEventListener('keydown', function(e) {
    if (document.activeElement === document.getElementById('chat-input')) return;
    if (document.activeElement === document.getElementById('username-input')) return;

    const dir = KEY_DIR[e.key];
    if (dir !== undefined && stompClient) {
        e.preventDefault();
        const now = Date.now();
        if (now - lastMoves[dir] >= MOVE_DELAY) {
            lastMoves[dir] = now;
            stompClient.send('/app/player.movePlayer', {}, dir);
        }
    }
});

document.getElementById('chat-input').addEventListener('keydown', function(e) {
    if (e.key === 'Enter') sendChat();
});

document.getElementById('chat-send').addEventListener('click', sendChat);

function joinGame() {
    const val = document.getElementById('username-input').value.trim();
    if (!val) return;
    username = val;
    document.getElementById('login-overlay').style.display = 'none';
    render(); // draw empty map while connecting
    connect();
}

document.getElementById('join-btn').addEventListener('click', joinGame);
document.getElementById('username-input').addEventListener('keydown', function(e) {
    if (e.key === 'Enter') joinGame();
});

render();
